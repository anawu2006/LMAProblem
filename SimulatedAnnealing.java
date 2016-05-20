// Ana Wu, Pace University, April 2016
import java.util.Random;

public class SimulatedAnnealing {
  
  // parameters for adjustment
  private double initialTemp = 30.0;
  private int iterationTimes = 2400;
  
  // Use simulated annealing to find and return the maximum objective value
  // Return the best partition through bestSolution[][]
  // Utilities object u is shared by all algorithms
  public float run(int bestPartition[][], Utilities u) {
    int materialNumber = u.getMaterialNumber();      // Retrieve amount of materials
    int deptNumber = u.getDeptNumber();              // Retrieve amount of departments 
    Random r = u.getRandom();                        // Retrieve Random object
    int p[][] = new int[materialNumber][deptNumber]; // Allocate space for current solution
    u.randomFeasibleSolution(p);                     // Generate random initial solution
    float currObjValue = u.objectiveValue(p);        // Find out its objective value
    float bestObjValue = currObjValue;               // p[][] is the best partition seen so far
    u.copyArray(p, bestPartition);                   // Record it
   
    int neighbor[][] = new int[u.getMaterialNumber()][deptNumber]; // Allocate space for a neighbor solution
    double t = initialTemp;                    // Initial temperature; parameter for adjustment
    while (t > 0.01) {                  // While not frozen; parameter for adjustment
      for (int l = 0; l < iterationTimes; l++) {   // 1000 is parameter for adjustment
        u.copyArray(p, neighbor);
        u.randomSwap(neighbor);         // neighbor[] is now a neighbor of p[]
        float newObjValue = u.objectiveValue(neighbor);
        float delta = newObjValue - currObjValue;
        // Probability to accept a worser neighbor
        double acceptProbability = Math.exp(delta * 80/t);
        // If the neighbor is better, take it as new current solution
        // Otherwise take it with probability acceptProbability
        if ((delta >= 0) || (r.nextDouble() < acceptProbability)) {
          // Accept the neighbor
          u.copyArray(neighbor, p);
          currObjValue = newObjValue;
          // If the new solution is the best seen so far, record it
          if (currObjValue > bestObjValue) {  
            bestObjValue = currObjValue;
            u.copyArray(p, bestPartition);
          }
        }
      }
      t = 0.95*t;   // Reduce temperature
    }
    return bestObjValue;
  }

  public static void main(String args[]) {
    Utilities u = new Utilities();                    // Create a Utilities object
    String fileName = "10.txt";                       // Default data file name
    if (args.length == 1)
      fileName = args[0];                             // Use command-line file name
    u.readGraph(fileName);

    int bestSolution[][] = new int[u.getMaterialNumber()][u.getDeptNumber()]; // Allocate space for best partition
    SimulatedAnnealing sa = new SimulatedAnnealing();
    u.startRun();                                     // Mark the start of run
    float bestObjValue = sa.run(bestSolution, u);     // Run Simulated Annealing
    u.endRun();                                       // Mark the end of run
    // Print out results
    u.reportResult("Simulated annealing", bestObjValue, bestSolution); 
    // Append results in file costs.txt
    u.appendBestPartition("Simulated annealing", bestObjValue, bestSolution);      
  }
}
