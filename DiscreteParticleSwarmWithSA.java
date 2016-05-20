// Ana Wu, Pace University, April 2016
import java.util.Random;

/*
 * Discrete Particle Swarm Optimization with Simulated Annealing algorithm
 * 
 * Process:
 * 1.Initialize position and velocity for each particle randomly;
 * 2.Loop
 *   a)Check converged status. 
 *     If converged currently:
 *       Launch Simulated Annealing to try to find a better neighborhood solution, 
 *       then dispatch all particles by reset velocities and positions; 
 *     Else
 *       go to step b;
 *   b)For each particle, evaluate the fitness value, then compare the fitness value with its pbest.
 *     If get a better solution, update pbest. 
 *     pbest is the best position found by particle so far. Each partile has a pbest.
 *   c)For each particle, identify the best neighbor, get the nbest, then update the velocity.
 *     nbest is the best solution found by neighbors.
 *   d)For each particle, update the position. Check the cut size of each position. 
 *     If find a better one than current gbest, update gbest.
 *     gbest is the best solution found by swarm.
 *   e)If a criterion is met, such as the maximum number of iterations, exit loop;
 * 3.End loop
 */

public class DiscreteParticleSwarmWithSA {

  // parameters for adjustment
  private static int particleNumber = 50;    // Particle numbers
  private static int iterationTimes = 1850;  // Maximum number of iteration time
  private static float maxVelocity = 6.0f;   // Velocity in each dimension is
                                             // between -maxVelocity to
                                             // maxVelocity
  private static float weight = 1.0f;        // inertia weight of velocity
  private static float c1 = 2.0f;            // Cognition learning rate
  private static float c2 = 2.0f;            // Social learning rate
  private static int topologyType = 0;       // Topology type. Well-known topology
                                             // includes global topology and ring topology.
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

  // Use DPSO and SA to find and return the maximum objective value
  // Return the best partition through bestSolution[][]
  // Utilities object u is shared by all algorithms
  public float run(int bestSolution[][], Utilities u) {
    materialNum = u.getMaterialNumber();                         // Retrieve amount of materials
    deptNum = u.getDeptNumber();                                 // Retrieve amount of departments 
    random = u.getRandom();                                      // Retrieve Random object
    utilities = u;                                               // Retrieve Random object
    this.bestSolution = bestSolution;

    positions = new int[particleNumber][materialNum][deptNum];    // Positions for all particles
    pBestPosition = new int[particleNumber][materialNum][deptNum];// Record pbest for all particles
    velocities = new float[particleNumber][materialNum][deptNum]; // Velocity for all particles
    fitness = new float[particleNumber];                          // Fitness value of pbest for all particles
    bestObjValue = Integer.MIN_VALUE;                             // Record best cut size

    if (0 == topologyType)
      topology = new GlobalTopology(particleNumber);// Communication topology for neighbor definition
    else                                            // Global topology: all particles in one neighborhood.
      topology = new RingTopology(particleNumber);  // Ring topology: particle i in a neighborhood consisting of itself, particle i-1, and particle i+1

    // DPSO step 1: initialization start
    initializePositions();  // Initialize position randomly for each particle
    initializeVelocities(); // Initialize velocity randomly for each particle

    // DPSO step 2: start iteration
    for (int iteration = 0; iteration < iterationTimes; ++iteration) {
      
      // Check if converged currently
      if (checkIsConverged()) {
        System.out.println("convergence!");
        sa();           // Launch SA to try to find a better neighbor solution
        reInitialize(); // Dispatch all particles by reset velocities and positions
      }

      evaluateAllFitness();  // Evaluate the fitness value for each particle
      updateAllVelocities(); // Update velocity for each particle
      updateAllPositions();  // Update position for each particle
    }

    return bestObjValue;
  }

  // Launch Simulated Annealing algorithm
  private void sa() {
    SAForDPSO sa = new SAForDPSO();
    sa.run(bestSolution, utilities);
  }

  // Dispatch all particles by reset velocities and positions
  private void reInitialize() {
    resetPosition();
    initializeVelocities();
    initializePositions();
    utilities.copyArray(bestSolution, positions[0]);
    bestObjValue = utilities.fitnessValue(bestSolution);
  }

  private void resetPosition() {
    for (int p = 0; p < particleNumber; ++p) {
      for (int i = 0; i < utilities.getMaterialNumber(); ++i) {
        for (int j = 0; j < utilities.getDeptNumber(); ++j)
          positions[p][i][j] = 0;
      }
    }
  }

  // check is converged currently
  private boolean checkIsConverged() {
    int count = 0;
    int materialNum = utilities.getMaterialNumber();
    int deptNum = utilities.getDeptNumber();
    for (int p = 0; p < particleNumber; ++p) {
      double sum = 0;
      for (int i = 0; i < materialNum; ++i) {
        for (int j = 0; j < deptNum; ++j) {
          sum += (Math.abs(velocities[p][i][j]));
        }
      }
      if ((sum / (deptNum * materialNum)) > 5.9)
        ++count;
    }
    return (count > (particleNumber) * 0.95);
  }

  // Initialize position for each particle randomly
  private void initializePositions() {
    for (int i = 0; i < particleNumber; ++i) {
      utilities.randomFeasibleSolution(positions[i]);           // Generate random initial solution
      utilities.copyArray(positions[i], pBestPosition[i]);      // Record pbest solution
      float currentCost = utilities.fitnessValue(positions[i]); // Find out its cost
      fitness[i] = currentCost;                                 // For feasible solution, fitness value is same as its cost
      if (isPositionFeasible(positions[i]) && isBetter(currentCost, bestObjValue)) { // Record it if find a better solution
        bestObjValue = currentCost;
        utilities.copyArray(positions[i], bestSolution);
      }
    }
  }

  // Initialize velocity for each particle randomly
  private void initializeVelocities() {
    for (int p = 0; p < particleNumber; ++p) {
      for (int i = 0; i < materialNum; ++i) { // Generate random velocity in each dimension
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
      if (isBetter(currFitness, fitness[i])) {                   // Record it if get a better solution
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
            * (bestLPosition[i][j] - currPosition[i][j]) + c2 * random.nextFloat()
            * (bestNPosition[i][j] - currPosition[i][j]));  // Equation for update velocity
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
    Utilities u = new Utilities();    // Create a Utilities object
    String fileName = "10.txt";       // Default data file name
    if (args.length == 1)
      fileName = args[0];             // Use command-line file name
    u.readGraph(fileName);

    int bestSolution[][] = new int[u.getMaterialNumber()][u.getDeptNumber()]; // Allocate space for best partition
    DiscreteParticleSwarmWithSA pso = new DiscreteParticleSwarmWithSA();
    u.startRun();                              // Mark the start of run
    float bestCost = pso.run(bestSolution, u); // Run Particle Swarm Optimization
    u.endRun();                                // Mark the end of run
    // Print out results
    u.reportResult("Particle Swarm Optimization", bestCost, bestSolution);
    // Append results in file costs.txt
    u.appendBestPartition("Particle Swarm Optimization", bestCost, bestSolution);
  }
}

class SAForDPSO {
  // parameters for adjustment
  private double initialTemp = 10.0;
  private int iterationTimes = 500;
  
  // Use simulated annealing to find a better neighbor for bestSolution[][]
  public float run(int bestSolution[][], Utilities u) {
    int materialNumber = u.getMaterialNumber();       // Retrieve amount of materials
    int deptNumber = u.getDeptNumber();               // Retrieve amount of departments 
    Random r = u.getRandom();                         // Retrieve Random object
    int p[][] = new int[materialNumber][deptNumber];  // Allocate space for current partition
    u.copyArray(bestSolution, p);
    float currObjValue = u.objectiveValue(p);         // Find out its objective value
    float bestObjValue = currObjValue;                // bestSolution[][] is the best partition seen so far
    u.copyArray(p, bestSolution);                     // Record it
   
    int neighbor[][] = new int[u.getMaterialNumber()][deptNumber]; // Allocate space for a neighbor solution
    double t = initialTemp;             // Initial temperature; parameter for adjustment
    while (t > 0.01) {                  // While not frozen; parameter for adjustment
      for (int l = 0; l < iterationTimes; l++) {  // 1000 is parameter for adjustment 
        u.copyArray(p, neighbor);
        u.randomSwap(neighbor);         // neighbor[] is now a neighbor of p[][]
        float newCost = u.objectiveValue(neighbor);
        float delta = newCost - currObjValue;
        // Probability to accept a worsening neighbor
        double acceptProbability = Math.exp(delta * 200/t);
        // If the neighbor is better, take it as new current solution
        // Otherwise take it with probability acceptProbability
        if ((delta >= 0) || (r.nextDouble() < acceptProbability)) {
          // Accept the neighbor
          u.copyArray(neighbor, p);
          currObjValue = newCost;
          // If the new solution is the best seen so far, record it
          if (currObjValue > bestObjValue) {  
            bestObjValue = currObjValue;
            u.copyArray(p, bestSolution);
          }
        }
      }
        t = 0.95*t;   // Reduce temperature
    }
    return bestObjValue;
  }
}