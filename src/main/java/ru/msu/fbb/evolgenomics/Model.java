import java.util.*;
import java.io.*;
import java.lang.reflect.*;
/**
 * class for storing info about time to change landscape and landscape to change to
 */
class ChangeTime{
    public double time;
    public double[] fitness;
    public ChangeTime(double time, double[] fitness){
	this.time=time;
	this.fitness=fitness;
    }
}


/**
 * The class for used to read and store the model parameters (provided in the config file).
 */
public class Model{
    private static HashMap<String, String> configValues = new HashMap<String, String>();
    private static String alphabet;
    private static int sequenceLength = 0;
    private static boolean debug = false;
    private static boolean collectStats = false;
    private static int numInstances = 1;
    private static int numThreads = 1;
    private static String treeFile;
    private static String fitnessFile;
    private static boolean sharedLandscape = false;
    private static NewFitnessRule newFitnessRule;
    private static LandscapeChangeTiming landscapeChangeTiming;
    private static InitialFitness initialFitnessDefinition;
    private static double distParam = -1;
    private static double alpha = -1;
    private static double beta = -1;
    private static double landscapeChangeRate = 0;
    private static double landscapeChangeInterval = Double.POSITIVE_INFINITY;
    private static double landscapeChangeParameter;
    private static double ageDependenceCoef = Double.NaN;
    //    private static boolean fixSumFitness = false;
    private static boolean printFitnessInfo = false;
    private static boolean qNormalization = true;
    private static boolean scaleLandscapeChangeToSubstitutionRate = false;
    private static double[] initialFitnessVectorFromFile; //the fitness vectore read from file
    private static HashMap<String, double[]> position2fitness; //map a string encoding branch and position
                                                               //to a fitness vector
    private static HashMap<String, ChangeTime> changeBranchTimeFitness;

    public static void setLandscapeChangeRate( double r){ landscapeChangeRate = r;}
    public static void setLandscapeChangeInterval(double x){landscapeChangeInterval = x;}
    public static double getLandscapeChangeRate() {return landscapeChangeRate;}
    public static double getLandscapeChangeInterval(){return landscapeChangeInterval; }

    public static double getAlleleAgeDependenceCoef() {return ageDependenceCoef;}
    public static double getDistParam() { return distParam;  }
    public static double getGammaAlpha() { return alpha;  }
    public static double getGammaBeta() { return beta;  }
    public static String getFitnessFile() { return fitnessFile;}
    /* getters for the parameters that are always set */
    public static int getSequenceLength(){return sequenceLength;};
    public static int getNumRuns(){return numInstances;}
    public static int getNumThreads(){return numThreads;}
    public static String  getAlphabet(){return alphabet;};
    public static int  getAlphabetSize(){return alphabet.length();};
    public static double getLandscapeChangeParameter(){ return landscapeChangeParameter;}
    public static String getTreeFile(){return treeFile;} 

    public static boolean debug(){return debug;}
    public static boolean collectStats(){return collectStats;}
    public static InitialFitness getInitialFitnessDefinition(){return initialFitnessDefinition;}
    public static LandscapeChangeTiming getLandscapeChangeTiming(){return landscapeChangeTiming;}
    public static NewFitnessRule getNewFitnessRule(){return newFitnessRule;}
    public static boolean getQNormalization(){return qNormalization;}
    public static boolean scaleLandscapeChangeToSubstitutionRate(){
	return scaleLandscapeChangeToSubstitutionRate;
    }

    public static boolean sharedLandscape(){return sharedLandscape;}
    public static boolean fixSumFitness(){ return false; }     //not used
    public static boolean printFitnessInfo(){ return printFitnessInfo;  }
    public static boolean changeAtSpecifiedBranchAndTime(){
	return landscapeChangeTiming == LandscapeChangeTiming.SPECIFIED_BRANCH_AND_TIME;
    }

    /**
     * @param node finishing the branch that is queries
     * @return Double.NEGATIVE_INFINITY if no change this branch, time till the end node otherwise
     */
    public static double getChangeTimeThisBranch(BasicNode node){
	String nodeName = node.toString();
	if (changeBranchTimeFitness.containsKey(nodeName))
	    return changeBranchTimeFitness.get(nodeName).time;
	else
	    return Double.NEGATIVE_INFINITY;
    }

    /**
     * @param node finishing the branch that is queries
     * @return null if no change this branch,  new fitnessime till the end node otherwise (which itself might be null);
     */
    public static double[] getNewFitnessThisBranch(BasicNode node){
	String nodeName = node.toString();
	if (changeBranchTimeFitness.containsKey(nodeName))
	    return changeBranchTimeFitness.get(nodeName).fitness;
	else
	    return null;
    }

    //wrapper function that throws a slightly more informative exception when a parameter is missing (or misspelled)
    private static String getRequiredParameter(String parameter) throws MissingParameterException {
	  String response = configValues.get(parameter);
	  if (response == null)
	      throw new MissingParameterException(parameter);
	  else
	      return response;
      }


    /**
     * get the String corresponding to the alphabet index array
     * @param seqArr - sequence as array of bytes
     * @return corresponding sequence as a String
     */
     public static String arr2seq(byte [] seqArr){
	String alphabet = getAlphabet();
	char[] charArr = new char[seqArr.length];
	for (int i = 0; i < seqArr.length; i++){
	    charArr[i] = alphabet.charAt(seqArr[i]);
	}
	return new String (charArr);
    }

    /**
     * Return the fitness vector to change to at the provided branch and position (until the end of that branch)
     * only works if LANDSCAPCE_CHANGE_TIMING is  specified_branch_and_time
     */
    public static double[] getFitnessForPosition(String branchName, double position){
	if (landscapeChangeTiming != LandscapeChangeTiming.SPECIFIED_BRANCH_AND_TIME)
	    throw new
		InvalidParameterCombinationException("getFitnessForPosition() only applicable if  LANDSCAPCE_CHANGE_TIMING is set to  specified_branch_and_time");
	String key = branchAndTimeToKey(branchName, position);
	return position2fitness.get(key);
    }

    public static double[] getInitialFitnessFromFile(){
	if (initialFitnessVectorFromFile == null)
	    readFitnessFromFile();
	return java.util.Arrays.copyOf(initialFitnessVectorFromFile, initialFitnessVectorFromFile.length);
    }

    private static void readFitnessFromFile(){
	initialFitnessVectorFromFile = new double[alphabet.length()];
 	//alphabetSize = Model.getAlphabet().length();
	// double[] fitness = new double[alphabetSize];
	String fitnessFile = getRequiredParameter("FITNESS_FILE");
	try{
	    Scanner sc = new Scanner(new File(fitnessFile));
	    for (int i = 0; i < initialFitnessVectorFromFile.length; i++)
		initialFitnessVectorFromFile[i] = sc.nextDouble();
	    sc.close();
	}catch(FileNotFoundException e){
	    System.err.println("Error: cannot open the fitness file " + fitnessFile);
	    System.exit(-1);
	}catch(InputMismatchException e){
	    System.err.println("Error: non-numeric value in your fitness file");
	    System.exit(-1);
	}
	catch(NoSuchElementException e){
	    System.err.println("Error: fitness vector apparently shorter than the alphabet");
	    System.exit(-1);
	}
    }
    /**
     * auxiliary function for converting branch name + time to key
     */
    public static String branchAndTimeToKey( String branch, double time){
	return branch + " " + time;
    }
    
    /** Read the prespecified coordinates of landscape changes from a file
     * @param changeBranchAndTimeFileStr - name of the file with landscape change coordinates
     */
    private static void readChangeBranchAndTimeFile(String changeBranchAndTimeFileStr){
	//TODO: have to do sth about rounding issues with position

	changeBranchTimeFitness = new HashMap<String, ChangeTime> ();
	try{
	    Scanner sc = new Scanner(new File(changeBranchAndTimeFileStr));
	    
	    while (sc.hasNextLine()){
		String line = sc.nextLine();
		if (line.length() == 0 )
		    continue;
		String[] fields = line.split("\\s+");
		//either just branch + time, or branch + time + fitness vector
		if (fields.length  !=  2 && fields.length != 2 + getAlphabetSize() ){
		    System.err.println("error in change branch time file " + changeBranchAndTimeFileStr);
		    System.err.println("line: " + line);
		    System.exit(-1);
		}
		else{
		    try{
			String branch = fields[0];
			double time = Double.parseDouble(fields[1]);
			double [] fitness = null;
			//			System.out.println("add " + branch + " time");
			
			if ( fields.length == 2 + getAlphabetSize()){//the fitness is specified by the user
			    //if haven't created the hashmap yet, do it 
			    // if (position2fitness == null)
			    // 	position2fitness = new HashMap<String, double[]>();
			    fitness = new double[getAlphabetSize()];
			    for (int i = 0; i < getAlphabetSize(); i++){
				fitness[i] = Double.parseDouble(fields[2+i]);
			    }
			    String key = branchAndTimeToKey(branch, time);
			    //			    position2fitness.put(key, fitness);
			}
			changeBranchTimeFitness.put(branch, new ChangeTime(time, fitness));
		    }catch (NumberFormatException ne){
			System.err.println("change branch time file " + changeBranchAndTimeFileStr + ": couldn't parse line " + line );
			System.exit(-1);
		    }
		}
	    }
	}catch (IOException ioe){
	    System.err.println("Error opening change branch time file " + changeBranchAndTimeFileStr);
	    System.exit(-1);
	}
    }
    
    public static void printParams(){
	System.out.println("Parameter values are:");
	try{
	    Class <?> c = Model.class;
	    for (Field f : c.getDeclaredFields()){
		String name = f.getName();
		if (!name.equals("debug") && !name.equals("configValues")
		    && !name.equals("initialFitnessVectorFromFile"))
		    System.out.println(name + " : " + f.get(null));
	    }
	}catch(Exception e){
	    e.printStackTrace();
	}
    }

    //parse the config file
    public static void init(String configFile){	
	try{
	    Scanner sc;
	    if (configFile.equals("-"))
		sc = new Scanner(System.in);
	    else
		sc = new Scanner(new File(configFile));
	    
	    while (sc.hasNextLine()){
		String line = sc.nextLine();
		if (line.length() == 0 || line.charAt(0) == '#')
		    continue;
		String[] fields = line.split("\\s+");
		if (fields.length!=2)
		    System.err.println("line: " + line);
		else
		    configValues.put(fields[0], fields[1]);
	    }
	}catch (IOException e){
	    System.err.println("Error opening config file " + configFile);
	    System.exit(-1);
	}
	try{ //this is solely for the sake of simplified missing parameter exception handling
	    sequenceLength = Integer.parseInt(getRequiredParameter("LENGTH"));
	    String debugStr = configValues.get("DEBUG");
	    if (debugStr != null)
		debug=Boolean.parseBoolean(debugStr);

	    String statsStr = configValues.get("COLLECT_STATS");
	    if (statsStr != null)
		collectStats=Boolean.parseBoolean(statsStr);

	    
	    alphabet = getRequiredParameter("ALPHABET");
	    treeFile = getRequiredParameter("TREE_FILE");
	    
	    String numRunsStr = configValues.get("NUM_INSTANCES");
	    if (numRunsStr == null)
		numRunsStr = configValues.get("NUM_RUNS"); //old in-house name, supported for backward compatibility
	    if (numRunsStr != null)
		numInstances = Integer.parseInt(numRunsStr); //else it defaults to 1
	    
	    String numThreadsStr = configValues.get("NUM_THREADS");
	    if (numThreadsStr != null){
		numThreads = Integer.parseInt(numThreadsStr);
	    }
	    
	    initialFitnessDefinition = InitialFitness.stringToEnum(getRequiredParameter("INITIAL_FITNESS"));
	    
	    String newFitnessRuleStr = configValues.get("NEW_FITNESS_RULE");
	    if (newFitnessRuleStr == null)
		newFitnessRuleStr = configValues.get("FITNESS_UPDATE_RULE");//old in-house name, supported for backward compatibility
	    if (newFitnessRuleStr == null)
		throw new MissingParameterException("NEW_FITNESS_RULE");
	    newFitnessRule = NewFitnessRule.stringToEnum(newFitnessRuleStr);
	    
	    String landscapeChangeTimingStr = configValues.get("LANDSCAPE_CHANGE_TIMING");
	    if (landscapeChangeTimingStr == null)
		landscapeChangeTimingStr = configValues.get("LANDSCAPE_CHANGE_RULE"); //old in-house name, supported for backward compatibility
	    if (landscapeChangeTimingStr == null)
		throw new MissingParameterException("LANDSCAPE_CHANGE_TIMING");
	    landscapeChangeTiming = LandscapeChangeTiming.stringToEnum(landscapeChangeTimingStr);
	    
	    String sharedLandscapeStr = configValues.get("SHARED_LANDSCAPE");
	    if (sharedLandscapeStr == null)
		sharedLandscape = false;
	    else
		sharedLandscape =  Boolean.parseBoolean(sharedLandscapeStr);

	    if (landscapeChangeTiming != LandscapeChangeTiming.SPECIFIED_BRANCH_AND_TIME)
		landscapeChangeParameter = Double.parseDouble(getRequiredParameter("LANDSCAPE_CHANGE_PARAMETER"));
	    else{
		String changeBranchAndTimeFileStr = configValues.get("CHANGE_BRANCH_AND_TIME_FILE");
		readChangeBranchAndTimeFile(changeBranchAndTimeFileStr);
	    }

	    if (landscapeChangeTiming==LandscapeChangeTiming.SPECIFIED_BRANCH_AND_TIME &&
		sharedLandscape == true)
		throw new InvalidParameterCombinationException("Can't have shared landscape and specified branch and time of change");
	    // String fixSumFitnessStr = configValues.get("FIX_SUM_FITNESS");
	    // if (fixSumFitnessStr == null)
	    // 	fixSumFitness = false;
	    // else		
	    // 	fixSumFitness =  Boolean.parseBoolean(fixSumFitnessStr);
		
	    
	    String printFitnessInfoStr = configValues.get("PRINT_LANDSCAPE_INFO");
	    if (printFitnessInfoStr == null)
		printFitnessInfo = false;
	    else
		printFitnessInfo =  Boolean.parseBoolean(printFitnessInfoStr);
	    
	    
	    String qNormalizationStr = configValues.get("CONSTANT_RATE");
	    if (qNormalizationStr==null)
		qNormalization = true;
	    else
		qNormalization = Boolean.parseBoolean(qNormalizationStr);
	    	   
	    String scaleLandscapeStr = configValues.get("SCALE_LANDSCAPE_CHANGE_TO_SUBSTITUTION_RATE");
	    if (scaleLandscapeStr == null)
		scaleLandscapeChangeToSubstitutionRate = false;
	    else
		scaleLandscapeChangeToSubstitutionRate = Boolean.parseBoolean(scaleLandscapeStr);
	
	


	    //check for invalid combination of inputs
	    //and for context-specific parameters:
	    if (newFitnessRule == NewFitnessRule.CURRENT_ALLELE_DEPENDENT){
		if (sequenceLength != 1)
		    throw new InvalidParameterCombinationException("Allele-dependent landscape change only works for single-site landscapes");
		if (landscapeChangeTiming != LandscapeChangeTiming.FIXED_INTERVAL_LENGTH
		    && landscapeChangeTiming != LandscapeChangeTiming.FIXED_NUM_CHANGES)
		    throw new InvalidParameterCombinationException("Allele-dependent landscape change only implemented for deterministic landscape change");

		//read the AGE_DEPENDENCE_COEFFICIENT
		String response = configValues.get("AGE_DEPENDENCE_COEFFICIENT");//misspelling fixed
		
		if (response == null)
		    configValues.get("EPISTASIS_COEFFICIENT"); //old in-house name, kept for backward compatibility
		if (response != null)
		    ageDependenceCoef = Double.parseDouble(response);
		else
		    throw new MissingParameterException ("AGE_DEPENDENCE_COEFFICIENT, required for allele-dependent fitness change");
	    }

	    if (initialFitnessDefinition == InitialFitness.LOGNORM ){
		distParam = Double.parseDouble(getRequiredParameter("DIST_PARAM"));
	    } else if (initialFitnessDefinition == InitialFitness.GAMMA){
		//		double alpha, beta;
		String alphaStr = configValues.get("GAMMA_ALPHA");
		String betaStr = configValues.get("GAMMA_BETA");
		if (alphaStr == null || betaStr == null){
		    alpha = Double.parseDouble(getRequiredParameter("DIST_PARAM"));
		    beta = alpha;
		}else{
		    alpha = Double.parseDouble(alphaStr);
		    beta = Double.parseDouble(betaStr);
		}
	    }
	    if (initialFitnessDefinition == InitialFitness.FILE){
		fitnessFile = getRequiredParameter("FITNESS_FILE");
		if (newFitnessRule == NewFitnessRule.IID)
		    throw new InvalidParameterCombinationException("initial fitness definition " + Model.getInitialFitnessDefinition() + " not compatible with new fitness rule IID ");
	    }
	}catch(MissingParameterException | InvalidParameterCombinationException  | UnrecognizedValueException e){
	    System.err.println(e.getMessage());
	    System.exit(-1);
	}
	
	if (initialFitnessDefinition == InitialFitness.FILE)
	    readFitnessFromFile();
	
    }
}

  
