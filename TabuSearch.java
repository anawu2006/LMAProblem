// Ana Wu, Pace University, April 2016
// A simplified Tabu Search algorithm implementation for library problem acquisition problem
public class TabuSearch {
  
  // parameters for adjustment
  private int tabuAmount = 30;
  private int iterationTimes = 1200;

  private Utilities u;                        // Utilities object 
  private int neighbor[][];                   // Generic array for a neighbor of p[]
  int materialNum;                            // Retrieve amount of materials
  int deptNum;                                // Retrieve amount of departments 
  
  // Use tabu search to find and return the maximum objective value
  // Return the best partition through bestSolution[][]
  // Utilities object u is shared by all algorithms
  public float run(int bestPartition[][], Utilities u) {
    this.u = u;                                     // Retrieve Random object
    materialNum = u.getMaterialNumber();            // Retrieve amount of materials
    deptNum = u.getDeptNumber();                    // Retrieve amount of departments 
    int p[][] = new int[materialNum][deptNum];      // Allocate space for current solution
    neighbor = new int[materialNum][deptNum];        // Allocate space for a neighbor of p[]

    u.randomFeasibleSolution(p);                     // Generate random initial solution
    float currObjValue = u.objectiveValue(p);        // Find out its cost
    float bestObjValue = currObjValue;               // p[] is the best solution seen so far
    u.copyArray(p, bestPartition);                   // Record it
    // Create a tabu list recording the most recently moved 10 entries
    // 10 is a parameter for adjustment
    TabuList t = new TabuList(tabuAmount);
    // Stop if there are no improvement for 50 consecutive iterations
    // 50 is a parameter for adjustment
    for (int i = 0; i < iterationTimes; i++) {
      currObjValue = bestQualifiedNeighbor(p, t, currObjValue);
      // If the new solution is the best seen so far, record it
      if (currObjValue > bestObjValue) {  
    	bestObjValue = currObjValue;
        u.copyArray(p, bestPartition);
        // Renew another 50 iterations before considering to quit
        i = 0;
      }
    }
    return bestObjValue;
  }

  // Find the best neighbor that is not tabued, return its objective value as return value,
  // return it through p[][]
  // TabuList t lists recently switched entry in solution matrix that should be prohibited in switching now
  // currentCost is the cut size of p
  private float bestQualifiedNeighbor(int p[][], TabuList t, float currObjValue) {
    int material = 0; 
    int dept = 0;
    float bestObjValue = currObjValue;   // Best objective value seen so far;
    int bestMaterial = -1;   
    int bestDept = -1;
    
    // switch each entry (0 to 1, or 1 to 0) in the solution to get neighbor solution
    for (material = 0; material < materialNum; ++material) {
      for(dept = 0; dept < deptNum; ++dept) {
        if (t.isTabued(material * materialNum + dept))  // If the entry is on the tabu list, skip it
          continue;
        float objValue = objective(p, material, dept); 
        if (objValue > bestObjValue) {
          bestObjValue = objValue;
          bestMaterial = material;
          bestDept = dept;
        }
      }
    }
    
    // record the best neighbor solution
    if(bestMaterial != -1 && bestDept != -1){
    	t.insert(bestMaterial * materialNum + bestDept);
    	p[bestMaterial][bestDept] = (p[bestMaterial][bestDept] + 1) % 2; // switch 0 and 1
    }
    return bestObjValue;
  }

  // Evaluate the objective value resulting from switching p[@material][@dept]
  private float objective(int p[][], int material, int dept) {
    u.copyArray(p, neighbor);
    neighbor[material][dept] = (neighbor[material][dept] + 1) % 2; // switch 0 and 1
    if(u.penaltyValue(neighbor) == 0.0f)
    	return u.objectiveValue(neighbor);
    else
    	return Float.MIN_VALUE;
  }

  public static void main(String args[]) {
    Utilities u = new Utilities();                    // Create a Utilities object
    String fileName = "graph10.txt";                  // Default data file name
    if (args.length == 1)
      fileName = args[0];                             // Use command-line file name
    u.readGraph(fileName);

    int bestSolution[][] = new int[u.getMaterialNumber()][u.getDeptNumber()]; // Allocate space for best partition
    TabuSearch ts = new TabuSearch();
    u.startRun();                                     // Mark the start of run
    float bestObjValue = ts.run(bestSolution, u);     // Run Tabu Search
    u.endRun();                                       // Mark the end of run
    // Print out results
    u.reportResult("Tabu search", bestObjValue, bestSolution); 
    // Append results in file costs.txt
    u.appendBestPartition("Tabu search", bestObjValue, bestSolution);      }  
}

// Tabu list implementation as a circular array
class TabuList {
  private int tabuListSize;              // Tabu list size
  private int tabuList[];                // List of tabued entries
  private int next;                      // Index for the next box for insertion

  public TabuList(int size) {
    tabuListSize = size;
    tabuList = new int [tabuListSize];   // Allocate space for tabu list
    for (int i = 0; i < tabuListSize; i++)
      tabuList[i] = -1;                  // Initialize the list with illegal entry value:
                                         // no entry is tabued now
    next = 0;
  }

  // Return true if entry is on the tabu list
  public boolean isTabued(int entry) {
    for (int i = 0; i < tabuListSize; i++)
      if (tabuList[i] == entry)
        return true;
    return false;
  }

  // Insert entry into the tabu list
  public void insert(int entry) {
    tabuList[next] = entry;        // Insert entry at position next
    next++;                         // Increase value of next circularly: 
    if (next == tabuListSize)       // 0, 1, 2, ..., tabuListSize-1, 0, 1, ...
      next = 0;
  }
}
