// Ana Wu, Pace University, April 2016
/*
 *  Communication topology for neighborhood definition
 */

public abstract class Topology {
  public int particleNumber;

  Topology(int particleNumber){
    this.particleNumber = particleNumber;
  }

  // Return all the neighbors of particle @particleIndex
  public abstract int[] getNeighbors(int particleIndex);
}