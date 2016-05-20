// Ana Wu, Pace University, April 2016
/*
 * GlobalTopology
 * All particles are in the same neighborhood
 */

public class GlobalTopology extends Topology {
  GlobalTopology(int particleNumber) {
    super(particleNumber);
  }
	
  // Return the indices of all particles since all particles are in the same neighborhood
  @Override
  public int[] getNeighbors(int particleIndex) {
    // TODO Auto-generated method stub
    int[] neibor= new int[particleNumber];
    for(int i = 0; i < particleNumber; ++ i){
      neibor[i] = i;
    }
    return neibor;
  }
}