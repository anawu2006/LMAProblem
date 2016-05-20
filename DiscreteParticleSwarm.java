// Ana Wu, Pace University, April 2016
import java.util.Random;

/*
 * Discrete Particle Swarm Optimization algorithm
 * 
 * Process:
 * 1.Initialize position and velocity for each particle randomly;
 * 2.Loop
 *   a)For each particle, evaluate the fitness value, then compare the fitness value with its pbest.
 *     If get a better solution, update pbest. 
 *     pbest is the best position found by particle so far. Each partile has a pbest.
 *   b)For each particle, identify the best neighbor, get the nbest, then update the velocity.
 *     nbest is the best solution found by neighbors.
 *   c)For each particle, update the position. Check the cut size of each position. 
 *     If find a better one than current gbest, update gbest.
 *     gbest is the best solution found by swarm.
 *   d)If a criterion is met, such as the maximum number of iterations, exit loop;
 * 3.End loop
 */


public class DiscreteParticleSwarm {

  // parameters for adjustment
  private static int particleNumber = 50;         // Particle numbers
  private static int iterationTimes = 2900;       // Maximum number of iteration time
  private static float maxVelocity = 6.0f;        // Velocity in each dimension is between -maxVelocity to maxVelocity
  private static float weight = 1.0f;             // inertia weight of velocity
  private static float c1 = 2.0f;                 // Cognition learning rate
  private static float c2 = 2.0f;                 // Social learning rate
  private static int topologyType = 0;            // Topology type. Well-known topology includes global topology and ring topology.
                                                  // 0:Global topology. 1:Ring topology.

  // variables
  Random random;
  Topology topology;
  Utilities utilities;
  int materialNum;
  int deptNum;
  int[][][] positions;
  int[][][] pBestPosition;
  int[][] bestSolution;
  float[][][] velocities;
  float[] fitness;
  float bestObjValue;

  // variables

  // Use DPSO to find and return the maximum objective value
  // Return the best partition through bestSolution[][]
  // Utilities object u is shared by all algorithms
  public float run(int bestSolution[][], Utilities u) {
    materialNum = u.getMaterialNumber();   // Retrieve amount of materials
    deptNum = u.getDeptNumber();           // Retrieve amount of departments
    random = u.getRandom();                // Retrieve Random object
    utilities = u;                         // Retrieve Random object
    this.bestSolution = bestSolution;

    positions = new int[particleNumber][materialNum][deptNum];     // Positions for all particles
    pBestPosition = new int[particleNumber][materialNum][deptNum]; // Record pbest for all particles
    velocities = new float[particleNumber][materialNum][deptNum];  // Velocity for all particles
    fitness = new float[particleNumber];                           // Fitness value of pbest for all particles
    bestObjValue = Integer.MIN_VALUE;                              // Record best cut size

    if (0 == topologyType)
      topology = new GlobalTopology(particleNumber); // Communication topology for neighbor definition
    else                                             // Global topology: all particles in one neighborhood.
      topology = new RingTopology(particleNumber);   // Ring topology: particle i in a neighborhood consisting of itself, particle i-1, and particle i+1

    // DPSO step 1: initialization start
    initializePositions();  // Initialize position randomly for each particle
    initializeVelocities(); // Initialize velocity randomly for each particle

    // DPSO step 2: start iteration
    for (int iteration = 0; iteration < iterationTimes; ++iteration) {
      evaluateAllFitness();    // Evaluate the fitness value for each particle
      updateAllVelocities();   // Update velocity for each particle
      updateAllPositions();    // Update position for each particle
    }

    return bestObjValue;
  }

  // Initialize position for each particle randomly
  private void initializePositions() {
    for (int i = 0; i < particleNumber; ++i) {
      utilities.randomFeasibleSolution(positions[i]);           // Generate random initial solution
      utilities.copyArray(positions[i], pBestPosition[i]);      // Record pbest solution
      float currentCost = utilities.fitnessValue(positions[i]); // Find out its cost
      fitness[i] = currentCost;                                 // For feasible solution, fitness value is same as its cost
      if (isPositionFeasible(positions[i]) && isBetter(currentCost, bestObjValue)) {// Record it if find a better solution
        bestObjValue = currentCost;
        utilities.copyArray(positions[i], bestSolution);
      }
    }
  }

  // Initialize velocity for each particle randomly
  private void initializeVelocities() {
    for (int p = 0; p < particleNumber; ++p) {
      for (int i = 0; i < materialNum; ++i) {  // Generate random velocity in each dimension
        for (int j = 0; j < deptNum; ++j)
          // Velocity in each dimension is between -maxVelocity to maxVelocity
          velocities[p][i][j] = random.nextFloat() * (maxVelocity * 2) - maxVelocity;
      }
    }
  }

  // Evaluate fitness value for each particle
  private void evaluateAllFitness() {
    for (int i = 0; i < particleNumber; ++i) {
      float currFitness = utilities.fitnessValue(positions[i]); // Calculate fitness value for current solution
      if (isBetter(currFitness, fitness[i])) {                  // Record it if get a better solution
        fitness[i] = currFitness;
        utilities.copyArray(positions[i], pBestPosition[i]);
      }
    }
  }

  // Update velocity for each particle every iteration
  private void updateAllVelocities() {
    for (int i = 0; i < particleNumber; ++i) {
      int bestNeibor = findBestNeighborIndex(i);             // Find best neighbor for particle i
      int[][] bestNPosition = new int[materialNum][deptNum]; // Record nbest, the position of the best neighbor
      utilities.copyArray(pBestPosition[bestNeibor], bestNPosition);
      calculateNewVelocity(velocities[i], positions[i], pBestPosition[i], bestNPosition); // Calculate new velocity
    }
  }

  // Update position for each particle every iteration
  private void updateAllPositions() {
    for (int i = 0; i < particleNumber; ++i) {
      calculateNewPosition(positions[i], velocities[i]); // Update position
      if (isPositionFeasible(positions[i])) {            // Update bestPosition if get a better feasible solution
        float currObjValue = utilities.objectiveValue(positions[i]);
        if (isBetter(currObjValue, bestObjValue)) {
          bestObjValue = currObjValue;
          utilities.copyArray(positions[i], bestSolution);
        }
      }
    }
  }

  // Find best neighbor for specific particle and return the index
  private int findBestNeighborIndex(int particleIndex) {
    int bestNeighbor = particleIndex;                             // Initialize the best neighbor as itself
    int[] neighborsIndice = topology.getNeighbors(particleIndex); // Record the index of all neighbors
    for (int k = 0; k < neighborsIndice.length; ++k) {            // Find the best neighbor by fitness value
      if (!isBetter(fitness[bestNeighbor], fitness[neighborsIndice[k]]))
        bestNeighbor = neighborsIndice[k];
    }
    return bestNeighbor;
  }

  // Calculate new velocity by dimension
  private void calculateNewVelocity(float[][] velocity, int[][] currPosition, int[][] bestLPosition, int[][] bestNPosition) {
    for (int i = 0; i < velocity.length; ++i) {
      for (int j = 0; j < velocity[0].length; ++j) {
        float newV = (weight * velocity[i][j] + c1 * random.nextFloat()
            * (bestLPosition[i][j] - currPosition[i][j]) + c2
            * random.nextFloat() * (bestNPosition[i][j] - currPosition[i][j])); // Equation for update velocity
        newV = (newV > maxVelocity) ? maxVelocity : newV;   // Make sure velocity <= maxVelocity
        newV = (newV < -maxVelocity) ? -maxVelocity : newV; // Make sure velocity >= -maxVelocity
        velocity[i][j] = newV;
      }
    }
  }

  // Calculate new position by dimension
  private void calculateNewPosition(int[][] position, float[][] velocity) {
    for (int i = 0; i < position.length; ++i) {
      for (int j = 0; j < position[0].length; ++j) {
        float sigmodial = (float) (1 / (1 + Math.exp(-velocity[i][j]))); // Equation for update position
        if (random.nextFloat() < sigmodial) {
          position[i][j] = 1;
        } else {
          position[i][j] = 0;
        }
      }
    }
  }

  // Check is position feasible or not
  private boolean isPositionFeasible(int[][] p) {
    return utilities.penaltyValue(p) == 0.0f;
  }

  // check is @newValue better than @oldValue (maximize objective value)
  private boolean isBetter(float newValue, float oldValue) {
    if (newValue > oldValue)
      return true;
    else
      return false;
  }

  public static void main(String[] args) {
    // TODO Auto-generated method stub
    Utilities u = new Utilities(); // Create a Utilities object
    String fileName = "10.txt";    // Default data file name
    if (args.length == 1)
      fileName = args[0];          // Use command-line file name
    u.readGraph(fileName);

    int bestSolution[][] = new int[u.getMaterialNumber()][u.getDeptNumber()]; // Allocate space for best solution
    DiscreteParticleSwarm pso = new DiscreteParticleSwarm();
    u.startRun();                                  // Mark the start of run
    float bestObjValue = pso.run(bestSolution, u); // Run Particle Swarm Optimization
    u.endRun();                                    // Mark the end of run
    // Print out results
    u.reportResult("Particle Swarm Optimization", bestObjValue, bestSolution);
    // Append results in file costs.txt
    u.appendBestPartition("Particle Swarm Optimization", bestObjValue, bestSolution);
  }
}
