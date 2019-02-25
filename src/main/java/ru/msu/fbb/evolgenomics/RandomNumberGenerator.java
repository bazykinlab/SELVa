import org.apache.commons.math3.distribution.*;
import org.apache.commons.math3.random.*;
import java.util.Random;
import java.math.BigInteger;
import java.security.SecureRandom;
/**
 * Class for  EvolutionaryProcess-local random distributions
 */
public class RandomNumberGenerator{
    private RandomGenerator randomNumberGenerator;

    private ExponentialDistribution exponentialDistribution;
    private double cache_exp_lambda=Double.NaN; //for caching the distribution object
    private GammaDistribution gammaDistribution;
    private double cache_gamma_a = Double.NaN; //for caching the distribution object
    private double cache_gamma_b = Double.NaN; //for caching the distribution object
    private LogNormalDistribution logNormalDistribution;
    private double cache_lognorm_mean = Double.NaN;    //for caching the distribution object
    private double cache_lognorm_stdev = Double.NaN;
    


    public RandomNumberGenerator(){
	initRandomNumberGenerator();
    }
    
    private  void initRandomNumberGenerator(){
	if (randomNumberGenerator == null){
	    //very expensive, but hopefully we do it once per process
	    long seed = (new BigInteger((new SecureRandom()).getSeed(8))).longValue();
	    //	    randomNumberGenerator = new ISAACRandom(seed);
	    randomNumberGenerator = new ThreadLocalRandomGenerator();
	}
    }

    public  double nextDouble(){
	return randomNumberGenerator.nextDouble();
    }
    public  int nextInt( int N){
	return randomNumberGenerator.nextInt(N);
    }

    /** Methods for sampling from probability distributions **/

    /**
     * Sample from the gamma distribution with parameters alpha = beta;
     * @param a alpha=beta
     * @return the sampled value
     */
    public  double sampleGamma (double a, double b)  {
	if (gammaDistribution == null || cache_gamma_a != a || cache_gamma_b != b){

	    //the math3 library uses the shape-scale parametrization of gamma
	    gammaDistribution = new GammaDistribution(randomNumberGenerator, a, 1.0/b, GammaDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY);
	    cache_gamma_a = a;
	    cache_gamma_b = b;
	    // if (Model.debug())
	    // 	System.out.println("new GammaDistribution object created with param " + a);
	}
	
	return gammaDistribution.sample();
    }
    
    /**
     * Sample from the exponential distribution with parameter lambda
     * @param lambda lambda
     * @return the sampled value
     */
    public  double sampleExponential(double lambda){
	if (exponentialDistribution == null || lambda != cache_exp_lambda){
	    exponentialDistribution =
		new ExponentialDistribution(new ThreadLocalRandomGenerator(), 1.0/lambda,  ExponentialDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY);
	    cache_exp_lambda = lambda;
 	    // if (Model.debug())
	    // 	System.out.println("new ExponentialDistribution object created wth lambda " + lambda);
	}
	return exponentialDistribution.sample();
    }

    /**
     * Sample from the lognormal  distribution with given parameters
     * @param norm_mean the mean of the normal distribution
     * @param norm_stdev the standard deviation of the normal distribution
     * @return the sampled value
     */    
    public  double sampleLogNormal(double norm_mean, double norm_stdev){
	if (logNormalDistribution == null &&
	    (norm_mean != cache_lognorm_mean || norm_stdev != cache_lognorm_stdev )){
		logNormalDistribution =
		    new LogNormalDistribution(new ThreadLocalRandomGenerator(), norm_mean, norm_stdev,  LogNormalDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY);
		cache_lognorm_mean = norm_mean;
		cache_lognorm_stdev = norm_stdev;
		// if (Model.debug())
		//     System.out.println("new LogNormalDistribution object created with params " + norm_mean + " and " + norm_stdev);
	}
	return logNormalDistribution.sample();
    }
    
    /**
     * Sample from the discrete distribution 
     * @param arr array defining the distribution of (arr[i] is the probabiity of element i)
     * @return  the sampled value (index)
     */
    public  int sampleDiscrete(double[] arr){
	double PERMITTED_ERROR = 1e-9;
	double sum = 0;
	for (double d : arr){
	    if (d < 0)
		throw new IllegalArgumentException("negative probability: " + d);
	    sum += d;
	}
	try{
	    if (Math.abs(sum-1.0) > PERMITTED_ERROR){
		System.err.println("array does not add up to 1: " + sum);
		System.err.println("arr: " + java.util.Arrays.toString(arr));
		throw new  IllegalArgumentException("array does not add up to 1: " + sum);
	    }
	}catch(Exception e){
	    e.printStackTrace();
	    System.exit(-1);
	}
	
	//double r = ThreadLocalRandom.current().nextDouble(1.0);
	double r = nextDouble();
	//	System.out.println("sample discrete r = " + r);
	double cum = 0.0;
	while (true){
	    //trick from Princeton's IntroCS StdRandom library implementation to deal with roundoff error
	    for (int i = 0; i < arr.length; i++){
		cum+=arr[i];
		if (cum > r)
		    return i;
	    }
	}
    }

    /**
     * Randomly permute the array in place (array is modified)
     * @param arr array to permute
     */
    public  void shuffleArray (double[] arr){
	int N = arr.length;
	for (int i = 0;  i < N; i++){
	    int r = nextInt(N-i);
	    //swap arr[i] with arr[i + r]
	    double temp = arr[i];
	    arr[i] = arr[i + r];
	    arr[i + r ] = temp;
	}
    }

}
