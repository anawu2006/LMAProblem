// Ana Wu, Pace University, April 2016
import java.io.*;
import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.Random;
import java.util.Vector;

// Provide utility methods used by multiple algorithms
public class Utilities {
  private Random r = null;       // Shared random number generator
  private String fileName;       // Graph data file name

  private long startTime;        // Run start time in milliseconds from 12AM 1970 
  private long endTime;          // Run end time in milliseconds from 12AM 1970 
  
  private int materialNum;       // Amount of materials
  private int deptNum;           // Amount of departments
  private int categoryNum;       // Amount of categories
  private int[] budget;          // Budget for each department
  private float[][] preference;  // Preference value matrix
  private int[] belongs;         // Category index for each material
  private float[] cost;          // Cost of each material
  private int[] categoryUpper;   // Upper bound of the amount of materials in each category
  private int[] categoryLower;   // Lower bound of the amount of materials in each category
  private float p = 0.5f;        // Control the degree of importance between preference average value and budget execution rate

  // Class constructor
  public Utilities() {  
    // Initialize random number generator with current time
    // so each run will use different random numbers
    long randomSeed = System.currentTimeMillis();
    r = new Random(randomSeed);
  }
  
  // Getter for Random object r
  public Random getRandom() {    
    return r;
  }
  
  // Getter for material number
  public int getMaterialNumber() {
    return materialNum;
  }
  
  // Getter for department number
  public int getDeptNumber() {
    return deptNum;
  }

  // Make a time stamp for run start time
  public void startRun() {
    startTime = System.currentTimeMillis();
  }

  // Make a time stamp for run end time
  public void endRun() {
    endTime = System.currentTimeMillis();
  }

  // Return elapsed time for the recent run in milliseconds
  public long elapsedTime() {
    return endTime - startTime;
  }

  // Generic way to report run results
  public void reportResult(String message, float bestObjValue, int bestSolution[][]) {
    System.out.println(message + ":  file = " + fileName + "   objective value = " + bestObjValue);
    System.out.println("Run time = " + elapsedTime() + " milliseconds");
    //printSolution(bestSolution);
    System.out.println("----------------------------------------------");
  }

  // Print the solution in p[]
  public void printSolution(int p[][]) {
    for (int i = 0; i < p.length; i++) {
      System.out.print(i + ": ");
      for (int j = 0; j < p[0].length; j++){
        System.out.print(j + ":" + p[i][j] + "|");
      }
      System.out.println();
    }
    System.out.println();
  }
  
  // fitness value = objective value - penalty value
  public float fitnessValue(int x[][]){
    return objectiveValue(x) - penaltyValue(x);
  }

  // Objective value function
  public float objectiveValue(int x[][]) {
    float averPref = getAvePreference(x);
    float budgetRate = getBudgetExecRate(x);
    float value = p * averPref + (1 - p) * budgetRate;
    return value;
  }
  
  // return how many materials @dept get in the solution
  public int getMaterialNumByDept(int x[][], int dept){
    int result = 0;
    for(int i = 0; i < materialNum; ++i){
      if(x[i][dept] == 1)
        ++ result;
      }
    return result;
  }
  
  // get sum of preference values of acquired materials for department @dept
  public float getTotalPrefByDept(int x[][], int dept){
    float total = 0;
    for(int i = 0; i < materialNum; ++i){
      total += (x[i][dept] * preference[i][dept]);
    }
    return total;
  }
  
  // get average preference value for all department
  public float getAvePreference(int x[][]){
    float total = 0;
    for (int j = 0; j < deptNum; ++j){
      float materialNumByDept = getMaterialNumByDept(x, j);
      if(materialNumByDept != 0) {
        float totalPrefByDept = getTotalPrefByDept(x, j);
        total += (totalPrefByDept / materialNumByDept);
      }
    }
    return total / deptNum;
  }
  
  // get the budget execution rate
  public float getBudgetExecRate(int x[][]){
    float totalBudget = getTotalBudget();
    return (totalBudget == 0 ) ? 0 : (float)getTotalActualCost(x) / totalBudget;
  }
  
  // get total actual cost for all materials
  public int getTotalActualCost(int x[][]){
    int result = 0;
    int[][] actualCost = getActualCost(x);
    for(int i = 0; i < materialNum; ++i){
      int cost = 0;
      for(int j = 0; j < deptNum; ++j){
        cost += x[i][j] * actualCost[i][j];
      }
      result += cost;
    }
    return result;
  }
  
  // get total budget of all departments
  public int getTotalBudget(){
    int total = 0;
    for(int j = 0; j < deptNum; ++j){
      total += budget[j];
    }
    return total;
  }
  
  // get actual cost of each department for each material
  public int[][] getActualCost(int x[][]){
    int[][] result = new int[materialNum][deptNum];
    float[] totalPrefByMaterial = getTotalPrefByMaterial(x);
    for(int i = 0; i < materialNum; ++i){
      for(int j = 0; j < deptNum; ++j){
        if(totalPrefByMaterial[i] != 0)
          result[i][j] = (int) Math.ceil((x[i][j] * preference[i][j])* cost[i] / totalPrefByMaterial[i]);
      }
    }
    return result;
  }
  
  // get actual cost for each department
  public int[] getActualCostByDept(int x[][]){
    int[] result = new int[deptNum];
    int[][] actualCost = getActualCost(x);
    for(int j = 0; j < deptNum; ++j){
      int costOfDept = 0;
      for(int i = 0; i < materialNum; ++i){
        costOfDept += (actualCost[i][j]);
      }
      result[j] = costOfDept;
    }
    return result;
  }
  
  // get sum of preference values for each material
  public float[] getTotalPrefByMaterial(int x[][]){
    float result[] = new float[materialNum];
    for(int i = 0; i < materialNum; ++i){
      float total = 0;
      for(int j = 0; j < deptNum; ++j){
        total += (x[i][j] * preference[i][j]);
      }
      result[i] = total;
    }
    return result;  
  }
  
  // get penalty value for solution x[][]
  public float penaltyValue(int x[][]){
    float budgetPenalty = getBudgetPenalty(x);
    float categoryPenalty = getCategoryPenalty(x);
    float penalty = budgetPenalty + categoryPenalty;
    return penalty;
  }
  
  // get penalty value for budget constrains for all departments
  public float getBudgetPenalty(int x[][]){
    float result = 0;
    int[] actualCostByDept = getActualCostByDept(x);
    for(int j = 0; j < deptNum; ++j){
      result += Math.max(0.0f, ((float)(actualCostByDept[j] - budget[j]) / budget[j]));
    }
    return result;
  }
  
  // get penalty value for category constrains for all departments
  public float getCategoryPenalty(int x[][]){
    float penalty = 0;
    boolean[] acquiredStatus = getAcquiredStatus(x);
    for(int k = 0; k < categoryNum; ++k){
      int acquiredNum = getMaterialNumByCategoryAndStatus(acquiredStatus, k, true);
      if(acquiredNum > categoryUpper[k])
        penalty += Math.max(0.0f, (float)(acquiredNum - categoryUpper[k]) / Math.abs(acquiredNum - categoryLower[k]));
      if(acquiredNum < categoryLower[k])
        penalty += Math.max(0.0f, (float)(categoryLower[k] - acquiredNum) / Math.abs(categoryUpper[k] - acquiredNum));
    }
    return penalty;
  }
  
  // get @isAcquired materials number in category @category
  public int getMaterialNumByCategoryAndStatus(boolean[] acquiredStatus, int category, boolean isAcquired){
    int num = 0;
    for(int i = 0; i < materialNum; ++i){
      if(isAcquired == acquiredStatus[i] && belongs[i] == category)
        ++ num;
      }
    return num;
  }
  
  // get each material is acquired or not
  public boolean[] getAcquiredStatus(int x[][]){
    boolean result[] = new boolean[materialNum];
    for(int i = 0; i < materialNum; ++i){
      result[i] = false;
      for(int j = 0; j < deptNum; ++j){
        if(x[i][j] == 1){
          result[i] = true;
          break;
        }
      }
    }
    return result;
  }
  
  
  /*
   * For generating random feasible solution
   * Process:
   * Loop, for each category k
   *   Loop
   *     1.	Randomly select a material i which is in category k and haven¡¯t been acquired yet;
   *     2. Randomly select a department j, check if current solution meets budget constrains with position[i][j] equals to 1. If yes, set position[i][j] to 1;
   *   End loop
   *   
   * TODO: This method will get struck if there is not feasible solution existent
   */
  public void randomFeasibleSolution(int x[][]){
    for(int k = 0; k < categoryNum; ++k){
      Vector<Integer> materialNoAcquired = getMaterialsByCategory(k);
      int MaterialNumInk = materialNoAcquired.size();
      while(MaterialNumInk - materialNoAcquired.size() < categoryLower[k]){
        if(materialNoAcquired.isEmpty()) {
          break;
        } else {
          int index = r.nextInt(materialNoAcquired.size());
          int material = materialNoAcquired.get(index);
          int limitIteration = deptNum;
          while(limitIteration > 0){ // for department
            int dept = r.nextInt(deptNum);
            x[material][dept] = 1;
            if(getBudgetPenalty(x) == 0) {
              materialNoAcquired.remove(index);
              break;
            } else {
              x[material][dept] = 0;
              -- limitIteration;
            }
          }
        }
      }
    }
	  
    /*System.out.println("----------feasible solution------------");
    printPartition(x);
    System.out.println();*/
  }
  
  // Get all materials by category @category
  public Vector<Integer> getMaterialsByCategory(int category){
    Vector<Integer> result = new Vector<Integer>();
    for(int i = 0; i < materialNum; ++i){
      if(belongs[i] == category){
        result.add(i);
      }
    }
    return result;
  }
  
  // Get amount of materials in category @category
  public int getMaterialNumByCategory(int category){
    int num = 0;
    for(int i = 0; i < materialNum; ++i){
      if(category == belongs[i]){
        ++ num;
      }
    }
    return num;
  }
  
  // Get amount of acquired materials 
  public int getAcquiredMaterialNum(int x[][]){
    int total = 0;
    for(int i = 0; i < materialNum; ++i){
      for(int j = 0; j < deptNum; ++j){
        if(x[i][j] == 1){
          ++ total;
          break;
        }
      }
    }
    return total;
  }
  
  // Randomly switch to get a feasible neighborhood
  public void randomSwap(int p[][]) {
    while (true) {
      int x = r.nextInt(materialNum);   // Randomly choose a material and a department
      int y = r.nextInt(deptNum);
      p[x][y] = (p[x][y] + 1) % 2;  // Change 0 to 1, or 1 to 0
      if(penaltyValue(p) == 0)
        return;
      else
        p[x][y] = (p[x][y] + 1) % 2; // Change back
    }
 }
 
  // Copy from[][] into to[][]
  public void copyArray(int from[][], int to[][]) {
    for (int i = 0; i < from.length; ++i)
      for(int j = 0; j < from[0].length; ++j)
        to[i][j] = from[i][j];
  }

  // Read in graph data from file fileName
  public void readGraph(String fileName) {
    BufferedReader file = null;
    StringTokenizer st;  // A generic reference for a String tokenizer
    String line;         // Current line read from the file
    String token;        // Current token on the current line
    this.fileName = fileName; 
    
    try {
      file = new BufferedReader(new FileReader(fileName));  
        
      // First line, including @materialNum, @deptNum and @categoryNum
      line = file.readLine();
      st = new StringTokenizer(line);
      token = st.nextToken(",").trim();
      materialNum = Integer.parseInt(token);
      token = st.nextToken(",").trim();
      deptNum = Integer.parseInt(token);
      token = st.nextToken(",").trim();
      categoryNum = Integer.parseInt(token);
        
      budget = new int[deptNum];
      preference = new float[materialNum][deptNum];
      belongs = new int[materialNum];
      cost = new float[materialNum];
      categoryUpper = new int[categoryNum];
      categoryLower = new int[categoryNum];
       
      // Second line, indicates the category index of each material
      line = file.readLine();
      st = new StringTokenizer(line);
      for (int i = 0; i < materialNum; i++) {
        token = st.nextToken(",").trim();
        belongs[i] = Integer.parseInt(token);
      }
       
      // Third line, indicates the cost of each material
      line = file.readLine();
      st = new StringTokenizer(line);
      for (int i = 0; i < materialNum; i++) {
        token = st.nextToken(",").trim();
        cost[i] = Float.parseFloat(token);
      }
       
      // Fourth line, indicates the budget of each department
      line = file.readLine();
      st = new StringTokenizer(line);
      for (int i = 0; i < deptNum; i++) {
        token = st.nextToken(",").trim();
        budget[i] = Integer.parseInt(token);
      }
       
      // Fifth line, indicates the lower bound of each category
      line = file.readLine();
      st = new StringTokenizer(line);
      for (int i = 0; i < categoryNum; i++) {
        token = st.nextToken(",").trim();
        categoryLower[i] = Integer.parseInt(token);
      }
       
      // Sixth line, indicates the upper bound of each category
      line = file.readLine();
      st = new StringTokenizer(line);
      for (int i = 0; i < categoryNum; i++) {
        token = st.nextToken(",").trim();
        categoryUpper[i] = Integer.parseInt(token);
      }
       
      // Seventh line to the end, indicates the preference value matrix
      for (int i = 0; i < materialNum; i++) { 
    	line = file.readLine();
        st = new StringTokenizer(line);
        for (int j = 0; j < deptNum; j++) { 
          token = st.nextToken(",").trim();
          preference[i][j] = Float.parseFloat(token);
        }
      }
    } catch (Exception e) {
      System.out.print(e.getMessage());
    } finally {
      try {
        if (file != null) 
          file.close();
      } catch (Exception e) {
    	System.out.print(e.getMessage());
      }
    }
  }

  // Append the best cost to file costs.txt for off-line analysis
  public void appendBestPartition(String message, float bestObjValue, int bestSolution[][]) {
    try {
      FileWriter fw = new FileWriter("costs.txt", true);
      PrintWriter out = new PrintWriter(fw);
      out.println(message + ":   file = " + fileName + "   objective value = " + bestObjValue);
      out.println("Run time = " + elapsedTime() + " milliseconds");
      out.println("Total Budget = " + getTotalBudget() + " Actual Cost = " + getTotalActualCost(bestSolution));
      out.println("Average Preference Value = " + getAvePreference(bestSolution));
      out.println("Total Materials = " + getAcquiredMaterialNum(bestSolution));
      out.println("--------------------------------------------------------------------------");
      out.close();
    }
    catch (Exception e){}
  }
}