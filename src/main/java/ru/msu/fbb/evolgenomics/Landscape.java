import java.util.*;
import java.io.File;

/**
 * The class represeting a (possibly evolving) fitness landscape.
 * A new landscape object is created if this is the first time a landscape change is made 
 * on the current tree branch, or if landscapes are shared between parallel branches.
 * Otherwise, the landscape change occurs on the same object (to save on object creation)
 */

public class Landscape{
    //link to the next landscape if the landscapes are shared between parallel branches
    private Landscape nextLandscape; 
    private double[][] Q;  //substitution-rate matrix Q
    private double pi[];   // stationary probability vector pi
    private double fitness[]; //fitness vector for the landsacpe
    //    private boolean piComputed = false; //this is set when Q and pi have been computed for the landscape
    private int alphabetSize = Parameters.getAlphabetSize(); 
    //landscape change time will be subtracted as branch time is eaten up.
    private double timeTillLandscapeChange = Double.POSITIVE_INFINITY;     
    private double diagQtimesPi = -1; //let's cache it so we don't recompute

    //cache the memory space to avoid re-allocating it each time we pick new character
    private double[] transitionVect; 

    //the PRNG local to the EvolutionaryProcess creating the Landscape
    RandomNumberGenerator random;

    Model model;
    
    /***** Constructors *****/
    
    /**
     * Create the new landscape object from scratch by querying the Model object
     * @param random the RandomNumberGenerator object providing the RNG to be used
     */
    public Landscape(RandomNumberGenerator random, Model model){//gets the initialization values from Model
	this.alphabetSize = Parameters.getAlphabet().length();
	Q = new double[alphabetSize][alphabetSize];
	pi = new double[alphabetSize];

	fitness = new double[alphabetSize];
	this.random = random;
	this.model = model;
	InitialFitness initialFitnessDefinition = model.getInitialFitnessDefinition();
	switch(initialFitnessDefinition){
	case FILE :
	    fitness = model.getInitialFitnessFromFile();
	    break;
	case FLAT:
	    Fitness.flatFitness(fitness);
	    break;
	case LOGNORM:
	    Fitness.logNormFitness(fitness, random, model);
	    break;
	case GAMMA:
	    Fitness.gammaFitness(fitness, random, model);
	    break;	    
	default: //should not get here, as this is caught in the InitialFitness class
	    throw new RuntimeException(initialFitnessDefinition + " fitness not supported");
	}
	if (Parameters.debug()){
	    System.out.println("initial fitness:");
	    System.out.println(java.util.Arrays.toString(fitness));    
	}
	setQFromFitness();
	computePi(); //not optimized
	// //unset the flags for having computed pi
	// piComputed = false;
	diagQtimesPi = -1; 
	timeTillLandscapeChange = model.getLandscapeChangeInterval();	
    }
    /**
     * Create the new Landsacpe object from the given fitness vector
     * @param fitness the fitness vector
     * @param random the RandomNumberGenerator object providing the RNG to be used
     */    
    public Landscape(double[] fitness, RandomNumberGenerator random, Model model){
	this.fitness = java.util.Arrays.copyOf(fitness, fitness.length);
	this.alphabetSize = fitness.length;
	this.model = model;

	
	setQFromFitness();
	computePi();
	//	piComputed = false;
	this.random = random;
    }

    /**
     * Clone an existing landscape object
     *
     * @param source the landscape to clone
     */
    public Landscape (Landscape source){
	alphabetSize = source.alphabetSize;
	this.fitness = java.util.Arrays.copyOf(source.fitness, source.fitness.length);
	Q = new double[alphabetSize][alphabetSize];
	//	this.piComputed = source.piComputed;
	pi = new double[alphabetSize];
	for (int i = 0; i < alphabetSize; i++){
	    pi[i] = source.pi[i];
	    for (int j = 0; j < alphabetSize; j++){
		Q[i][j] = source.Q[i][j];
	    }
	}
	this.diagQtimesPi = source.diagQtimesPi;
	this.random = source.random;
	this.model = source.model;
	// child Landscape(s) having the same PRNG object as the parent
	//is OK b/c landscapes within a thread are not run in parallel
	//so there is no danger of the landscapes generating the same
	//number
    }

    /**** Methods *****/

    /*** setters and getters ***/
    
    /**
     * Get the value of[i][j]
     * @param i i'th coordinate of Q[i][j]
     * @param j j'th coordinateof Q[i][j]
     * @return Q[i][j]
     */
    public double Qat(int i, int j){
	return Q[i][j];
    }

    /**
     * Get the stationary distribution vector pi for this landscape
     * @return double vectore representing the stationary distribution for this landscape
     */   
    public double[] getPi(){
	computePi();
	return pi;
    }

    
    /**
     * Get the \Sum_i -q_i* pi_i, the expected change rate
     * @return  \Sum_i -q_i* pi_i, the expected change rate
     */
    public double getDiagQtimesPi(){
	//if no value is cached, recompute
	if (diagQtimesPi == -1){
	    computeDiagQtimesPi();
	}
	//otherwise, get the cached value
	return diagQtimesPi;
    }
    /** 
     * Get a copy of the fitness vector for this Landscape
     * @return a new double vector containing a copy of the fitness vector
     */
    public double[] getCopyOfFitness(){
	return java.util.Arrays.copyOf(fitness, fitness.length);
    }
    /**
     * Get the size of the allele alphabet
     */
    //    public int getAlphabetSize(){return alphabetSize;}

    /**
     * setter for the time until landscape change
     * @param t time left
     */
    public void setTimeTillLandscapeChange(double t){
	timeTillLandscapeChange =  t;
    }
    /**
     * getter for the time until landscape change
     * @return t time left
     */
    public double getTimeTillLandscapeChange(){
	return timeTillLandscapeChange;
    }

    /*** Q normalization methods ***/
    
    /** 
     * The expected substitution rate is not fixed, but is scaled to be 1 for flat landscape only
     */
    private void normalizeQToFlat (){
	if (Parameters.debug())
	    System.out.println("normalize to flat");

	double normalizationFactor = alphabetSize -1.0f;
	//why?  if all fitnesses are equal, the q_ij=1 for i!= j, and q_ii = -(n-1), where n is the alphabetSize
	//pi's are: 1/n, so the weighted sum of diagonals = sum_n{-q_ii*p_i} = n * ((n-) * 1/n) = n-1
	for(int i = 0; i < alphabetSize; i++)
	    for(int j = 0; j < alphabetSize; j++)
		Q[i][j] /= normalizationFactor;

	computeDiagQtimesPi();
    }
    
    /**
     * Normalize so that \sum_i -Q[i][i] * pi[i] = 1, and the tree length is interpretable
     * as the expected number of substitutions per site.  The approach of Z. Yang
     */    
    private void normalizeToOne(){
	//we need pi for this, so compute it if not yet
	//	if (!piComputed)
	computePi();
	double sumDiagP = 0;
	for(int i = 0; i < alphabetSize; i++){
	    sumDiagP -= Q[i][i] * pi[i];	    
	}
	for(int i = 0; i < alphabetSize; i++)
	    for(int j = 0; j < alphabetSize; j++)
		Q[i][j] /= sumDiagP;
	computeDiagQtimesPi();
    }
    /**
     * Wrapper function that chooses how to normalize (or scale) Q
     */
    private void normalizeQ(){
	if(Parameters.getQNormalization())
	    normalizeToOne();
	else
	    normalizeQToFlat();
    }
    /**
     * compute \sum -Q[i][i] * pi[i], i.e. the instantaneous probability of _any_ transition
     * we're caching it to save cycles, and computing it on demand b/c to avoid recomputing pi
     */
    private void computeDiagQtimesPi(){
	//	if (!piComputed)
	computePi();
	double sumDiagP = 0;
	for(int i = 0; i < alphabetSize; i++){
	    sumDiagP -= Q[i][i] * pi[i];	    
	}
	diagQtimesPi = sumDiagP;
	if (Parameters.debug()){
	//check what we've got
	    System.out.println("diagQtimesPi = " + diagQtimesPi); 
	}
    }
    /**
     * Set the values of Q and pi from the fitness vector
     */
    private void setQandPiFromFitness(){
	setQFromFitness();
	computePi();
    }
    /**
     * Compute Q from the fitness vector
     */
    private void setQFromFitness(){
	Q = QfromFitness.generateQ(fitness);
	normalizeQ();
    }
    /**
     * Compute pi; if the user-supplied a mutation rate matrix, it is computed from Q; 
     * otherwise, it is computed from the fitness vector directly
     */
    private void computePi(){
	if (Parameters.isMutationRateMatrixDefined())
	    pi = QfromFitness.PiFromQ(Q);
	else
	    pi = QfromFitness.PiFromFitness(fitness);
	//	piComputed = true;
    }

    /**
     * This is where landscape change occurs.  This method determines how exactly it is done
     *
     * @param ls the old landscape
     * @param createNewLandscape should we create a new landscape object? True if landscapes are shared
     * between parallel branches, or this is the first time we change landscape on the current branch
     * @param character the current allele (index).  Relevant for allele-specific landscape change
     * @param newFtness new fitness vector specified by user; null if not specified      
     * @return the "next" landsacpe
     */
    public static Landscape getNewLandscape(Landscape ls, boolean createNewLandscape, byte character, double[] newFitness){
	Landscape currentLS = ls; //prepare for being able to reuse the old Landscape object
	
	//first consider the case where landscapes are shared between paralell branches
	if (Parameters.sharedLandscape()){  //if the landscapes are shared between parallel branches
	    //check if we already have the next landscape (i.e., it's been generated on a
	    //parallel branch that was visited earlier in the course of simulation execution)
	    if (ls.nextLandscape == null){ // if it hasn't been generated before, do it now
		if (Parameters.debug())
		    System.out.println("generate shared landscape on demand");
		ls.nextLandscape = new Landscape(ls); //at first, next landscape is identical to old one
		ls.nextLandscape.changeQ(character, newFitness);  //then immediatley change it
	    }	else{ //if it has been generated before, don't generate it
		if (Parameters.debug())
		    System.out.println("next shared landscape generated before");
	    }
	    //now return the next landscape - whether it's just been generated or saved from
	    //some paralell branch
	    currentLS = ls.nextLandscape; 
	}else {//if landscapes evolve independently on parallel branches,
	       // do branch-specific landscape change
	    if (createNewLandscape) //if we need to create a new object for this branch, do it
		currentLS = new Landscape(ls);
	    //if not, we use the old Java object
	    
	    //whether new Landsacpe object or old, change the landscape now
	    currentLS.changeQ(character, newFitness);
	}
	return currentLS;
    }


    /**
     * Change the landscape (ultimately, changing the Q matrix).  This is the where the 
       * computation of the new landscape takes place
     * @param character the current allele(index) - for allele-specific fitness change
     * @param newFtness new fitness vector specified by user; null if not specified      
     */
    private void changeQ( byte character, double[] newFitness){

	if (Parameters.debug()){
	    System.out.println("old fitness:");
	    System.out.println(java.util.Arrays.toString(fitness));
	}
	if (model.getNewFitnessRule() == NewFitnessRule.USER_SET){
	    if (newFitness != null){
		fitness = Arrays.copyOf(newFitness, newFitness.length);
	    } else{
		throw new InvalidParameterCombinationException("new fitness is USER_SET but the vector is not given (or given incorrectly");
	    }		
	}else{
	    //choose how the new fitness is calculated
	    switch (model.getNewFitnessRule()){
	    case IID:
		if (Parameters.debug())
		    System.out.println("initial fitness: " + model.getInitialFitnessDefinition());
		if (model.getInitialFitnessDefinition() == InitialFitness.LOGNORM)
		    Fitness.logNormFitness(fitness, random, model);
		else if (model.getInitialFitnessDefinition() == InitialFitness.GAMMA)
		    Fitness.gammaFitness(fitness, random, model);
		else
		    throw new InvalidParameterCombinationException(model.getInitialFitnessDefinition() + " not compatible with iid new fitness rule");
		break;
	    case SHUFFLE:
		Fitness.shuffleFitness(fitness, random);
		break;
	    case CURRENT_ALLELE_DEPENDENT:
		Fitness.alleleAgeDependentDiscreteChange(fitness, character, model);
		break;
	    default:
		throw new UnsupportedOperationException("fitness update rule " + model.getNewFitnessRule() + " not supported");
	    }
	}
	if (Parameters.debug()){
	    System.out.println("new fitness:");
	    System.out.println(java.util.Arrays.toString(fitness));
	}
	setQFromFitness(); //compute Q
	computePi();
	//	piComputed = false; //"clear" the cached pi value - we'd need to recompute it for new landscape
    }


    
    /**
     * When a substitution occurs, pick the new allele to replace the current one, 
     * according to the fitness landsacpe
     * @param currentValue  index of the current allele
     * @return index of the new allele
     */    
    public byte pickNewCharacter(byte currentValue){
	if (transitionVect == null)
	    transitionVect = new double[Q.length];
	double qi = -Q[currentValue][currentValue];

	//we don't need to have a separate zeroing-out of transitionVect, bc we reset it here
	for (int i = 0; i < transitionVect.length; i++){
	    if (i == currentValue)
		transitionVect[i] = 0;
	    else
		transitionVect[i] = Q[currentValue][i];
	}
	//because q_i is slightly different from sum(q_ij), let's divide by the actual sum before the sampling
	Utils.normalizeArray(transitionVect);
	byte returnVal = (byte)random.sampleDiscrete(transitionVect);
	return  returnVal;
    }
    

    public void printParams(){
	System.out.println("fitness:");
	System.out.println(Arrays.toString(fitness));
	System.out.println("pi:");
	System.out.println(Arrays.toString(pi));
	System.out.println("Q:");
	for (int i = 0; i < Q.length; i++){
	    System.out.println(Arrays.toString(Q[i]));
	}
	System.out.println("diagQtimesPi: " + getDiagQtimesPi());
	System.out.println();
    }

                                                                  
    /*    public static void main (String args[]){
	double [] fitness = new double[20];
	RandomNumberGenerator rand = new RandomNumberGenerator();
	Fitness.logNormFitness(fitness,rand);
	Landscape landscape = new Landscape( fitness,rand);
	//	landscape.printParams();
	landscape.changeQ((byte)0, null);
	//landscape.printParams();
	}*/
}
