import java.util.*;
import java.io.*;
/**
 * Auxiliary class for the fitness vector infrastructure.
 */
public class Fitness{

    /**
     * Fill the fitness vector by sampling from the gamma distribution with alpha and beta
     * @param fitness vector to fill
     * @param alpha alpha
     * @param beta beta
     * @param random the RandomNumberGenerator object providing the RNG to be used
     */
    public static void gammaFitness(double[] fitness, double alpha, double beta,
				    RandomNumberGenerator random){
	//	System.out.println("gamma");
	for (int i = 0; i < fitness.length; i++)
	    fitness[i] = random.sampleGamma(alpha, beta);
    }
    /**
     * Fill the fitness vector by sampling from the gamma distribution with parameters from config
     * @param fitness vector to fill
     * @param random the RandomNumberGenerator object providing the RNG to be used
     */
    public static void gammaFitness(double[] fitness, RandomNumberGenerator random){
	//	double DIST_PARAM = Model.getDistParam();
	gammaFitness(fitness, Model.getGammaAlpha(), Model.getGammaBeta(), random);
    }


    /**
     * Fill the fitness vector by sampling from the lognorm distribution
     * @param fitness vector to fill
     * @param norm_mean The mean of the normal distribution
     * @param norm_stdev The standard deviation of the normal distribution
     * @param random the RandomNumberGenerator object providing the RNG to be used
     */
    public static void logNormFitness(double[] fitness, double norm_mean, double norm_stdev, RandomNumberGenerator random){
	for (int i = 0; i < fitness.length; i++)
	    fitness[i] = random.sampleLogNormal(norm_mean, norm_stdev);
    }

    /**
     * Fill the fitness vector by sampling from the lognorm distribution with parameters from config
     * @param fitness vector to fill
     * @param random the RandomNumberGenerator object providing the RNG to be used
     */
    public static void logNormFitness(double[] fitness, RandomNumberGenerator random){
	double DIST_PARAM = Model.getDistParam();
	logNormFitness(fitness, 0, DIST_PARAM, random);
    }
    /**
     * Fill the fitness vector with all 1's
     * @param fitness vector to fill
     */
    public static void flatFitness(double [] fitness){
	for (int i = 0; i < fitness.length; i++)
	    fitness[i] = 1;
    }
    /**
     * Randomly permute the fitness vector
     * @param fitness  vector to shuffle (in place) 
     * @param random the RandomNumberGenerator object providing the RNG to be used
     */
    public static void shuffleFitness(double [] fitness,
				      RandomNumberGenerator random){
	
	random.shuffleArray(fitness);
	//	int shuffleCount = 0;
	
	//	if (!Utils.allElementsEqual(fitness)){ //don't try to shuffle flat array
	
	//  double[] fitnessOld = java.util.Arrays.copyOf(fitness, fitness.length);
	//	    do{
	// random.shuffleArray(fitness);
	//	shuffleCount++;
	//	    }while(java.util.Arrays.equals(fitness, fitnessOld));
	
	//}
	// if (Model.debug())
	//     System.out.println(shuffleCount + " shuffles");	
    }
    
    /**
     * Change only the fitness of the current allele in linear age-dependent fashion
     * f_new[current] = f_old[current] + k*t, where k is the AGE_DEPENDENT_COEFFICIENT, and 
     * t is the landscape change interval
     * @param fitness the fitnes vector
     * @param character the current allele
     */
    public static void alleleAgeDependentDiscreteChange(double[] fitness, byte character){
	double sum = 0.0;	    
	double kt = Model.getLandscapeChangeInterval() * Model.getAlleleAgeDependenceCoef();
	fitness[character] += kt;
	System.out.println("increase fitness of allele " + character + " by " + kt);
    }
}
