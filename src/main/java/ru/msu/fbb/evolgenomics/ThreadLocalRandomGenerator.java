import org.apache.commons.math3.random.*;
import java.util.concurrent.*;
import java.util.Random;
/**
 * Implementation of org.apache.commons.math3.random.RandomGenerator interface using java's ThreadLocalRandom.
 */

public class ThreadLocalRandomGenerator extends AbstractRandomGenerator {
    Random random;
    public ThreadLocalRandomGenerator(){
	if (Parameters.seedSet()){
	    random = new Random(Parameters.nextSeed());
	}
    }

    public double nextDouble(){
	if (!Parameters.seedSet())
	    return ThreadLocalRandom.current().nextDouble(1.0);
	else
	    return random.nextDouble();
    }
    /**
     * Can't be done - throw an UnsupportedOperateionException
     */
    public void setSeed (long seed){
	if (!Parameters.seedSet())
	    throw new UnsupportedOperationException("Setting the seed for ThreadLocalRandom not permitted");
	else
	    random.setSeed(seed);
    }
}
