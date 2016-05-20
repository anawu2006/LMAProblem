// Ana Wu, Pace University, April 2016
/*
 * RingTopology
 * Particle i in a neighborhood consisting of itself, particle i-1, and particle i+1,
 * with arrays wrapped so i=0 is beside i=N-1.
 */

public class RingTopology extends Topology {
  RingTopology(int particleNumber) {
    super(particleNumber);
  }

  // Return the indices of three neighbors
  @Override
  public int[] getNeighbors(int particleIndex) {
    // TODO Auto-generated method stub
    int[] neibor= new int[3];
    neibor[0] = particleIndex - 1;
    neibor[1] = particleIndex;
    neibor[2] = particleIndex + 1;
	
    // Particle i=N-1 is beside particle i=0
    if(particleIndex == 0) { 		
      neibor[0] = particleNumber - 1;
    } else if(particleIndex == particleNumber - 1){
      neibor[2] = 0;
    }
    return neibor;
  }
}
