import org.apache.commons.math3.random.*;
import java.util.concurrent.*;
/**
 * Implementation of org.apache.commons.math3.random.RandomGenerator interface using java's ThreadLocalRandom.
 */

public class ThreadLocalRandomGenerator extends AbstractRandomGenerator {
    public ThreadLocalRandomGenerator(){
	super();
    }

    public double nextDouble(){
	return ThreadLocalRandom.current().nextDouble(1.0);
    }
    /**
     * Can't be done - throw an UnsupportedOperateionException
     */
    public void setSeed (long seed){
	throw new UnsupportedOperationException("Setting the seed for ThreadLocalRandom not permitted");
    }
}
