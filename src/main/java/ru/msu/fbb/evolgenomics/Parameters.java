import java.util.*;
import java.io.*;
import java.lang.reflect.*;


/**
 * The class for used to read and store the model parameters (provided in the config file).
 */
public class Parameters{
    //    private static HashMap<String, String> configValues = new HashMap<String, String>();
    private static String alphabet;
    private static boolean debug = false;
    private static boolean collectStats = false;
    private static int numInstances = 1;
    private static int numThreads = 1;
    private static String treeFile;
    private static boolean sharedLandscape = false;
    private static boolean printFitnessInfo = false;
    private static boolean qNormalization = true;
    private static boolean scaleLandscapeChangeToSubstitutionRate = false;
    public static double[][] mutationRateMatrix; // mutation rate matrix (set to all 1's by default)
    private static byte[] rootSequence;

    public static Model[] models;
    private static boolean variableLandscapes = false;

    public static Random seedGenerator;
    public static long seed;
    public static boolean seedSet;
    public static boolean seedSet(){return seedSet;}
    public static long nextSeed(){return seedGenerator.nextLong();}
    public static boolean printFitnessInfo(){ return printFitnessInfo;  }
    
    public static int getNumRuns(){return numInstances;}
    public static int getNumThreads(){return numThreads;}
    public static String  getAlphabet(){return alphabet;};
    public static int  getAlphabetSize(){return alphabet.length();};
    public static String getTreeFile(){return treeFile;}
    public static byte[] getRootSequenceFromFile(){ return rootSequence;  }
    
    public static boolean isMutationRateMatrixDefined(){return mutationRateMatrix!=null;}
    
    public static boolean debug(){return debug;}
    public static boolean collectStats(){return collectStats;}

    public static boolean getQNormalization(){return qNormalization;}
    


    public static boolean scaleLandscapeChangeToSubstitutionRate(){
	return scaleLandscapeChangeToSubstitutionRate;
    }
    public static boolean getIsVariableLandscapes(){
	return variableLandscapes;
    }
    public static boolean sharedLandscape(){return sharedLandscape;}


    //wrapper function that throws a slightly more informative exception when a parameter is missing (or misspelled)
    private static String getRequiredParameter(Map<String, String> configValues, String parameter) throws MissingParameterException {
	  String response = configValues.get(parameter);
	  if (response == null)
	      throw new MissingParameterException(parameter);
	  else
	      return response;
      }

    //wrapper function that throws a slightly more informative exception when a parameter is missing (or misspelled)
    //this version accomodates different names for the parameter that is changed and whose absence is reported
    private static String getRequiredParameter(Map<String, String> configValues, String parameter, String name4msg ) throws MissingParameterException {
	  String response = configValues.get(parameter);
	  if (response == null)
	      throw new MissingParameterException(name4msg);
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
    public static void printParams(){
	System.out.println("Global parameters:");
	try{
	    Class <?> c = Parameters.class;
	    for (Field f : c.getDeclaredFields()){
		String name = f.getName();
		//		System.out.println("field: " + name);
		if (!name.equals("debug") && !name.equals("configValues")
		    && !name.equals("initialFitnessVectorFromFile")
		    && !name.equals("rootSequence")
		    && !name.equals("seedGenerator")
		    && !name.equals("models")
		    && !name.equals("changeBranchTimeFitness"))
		    System.out.println(name + " : " + f.get(null));
	    }
	    /*	    if (changeBranchTimeFitness != null){
		System.out.println("changeBranchTimeFitness :");
		for (Map.Entry<String, ChangeTime> entries : changeBranchTimeFitness.entrySet()){
		    System.out.println("\t" + entries.getKey() + " : " + entries.getValue());
		}
		}*/
	    //	    System.out.println("initialFitnessVectorFromFile : " + java.util.Arrays.toString(initialFitnessVectorFromFile));
	    System.out.println("rootSequence (as indices of ALPHABET) : " + java.util.Arrays.toString(rootSequence));
	    System.out.print("mutationRateMatrix : ");
	    if (mutationRateMatrix == null)
		System.out.println("null");
	    else{
		System.out.println("");
		for (double[] arr : mutationRateMatrix)
		    System.out.println(java.util.Arrays.toString(arr));
	    }
	}catch(Exception e){
	    e.printStackTrace();
	}

    }

    public static void assignModelValues( Model model, Map<String, String> configValues){
	try{ //this is solely for the sake of simplified missing parameter exception handling

	    model.setSequenceLength(Integer.parseInt(getRequiredParameter(configValues, "LENGTH")));
	    model.setInitialFitnessDefinition(InitialFitness.stringToEnum(getRequiredParameter(configValues, "INITIAL_FITNESS")));
	    
	    String newFitnessRuleStr = configValues.get("NEW_FITNESS_RULE");
	    if (newFitnessRuleStr == null)
		newFitnessRuleStr = configValues.get("FITNESS_UPDATE_RULE");//old in-house name, supported for backward compatibility
	    if (newFitnessRuleStr == null)
		throw new MissingParameterException("NEW_FITNESS_RULE");
	    model.setNewFitnessRule(NewFitnessRule.stringToEnum(newFitnessRuleStr));
	    
	    String landscapeChangeTimingStr = configValues.get("LANDSCAPE_CHANGE_TIMING");
	    if (landscapeChangeTimingStr == null)
		landscapeChangeTimingStr = configValues.get("LANDSCAPE_CHANGE_RULE"); //old in-house name, supported for backward compatibility
	    if (landscapeChangeTimingStr == null)
		throw new MissingParameterException("LANDSCAPE_CHANGE_TIMING");
	    model.setLandscapeChangeTiming (LandscapeChangeTiming.stringToEnum(landscapeChangeTimingStr));
	    
	    String sharedLandscapeStr = configValues.get("SHARED_LANDSCAPE");
	    if (sharedLandscapeStr == null)
		sharedLandscape = false;
	    else
		sharedLandscape =  Boolean.parseBoolean(sharedLandscapeStr);

	    if (model.getLandscapeChangeTiming() != LandscapeChangeTiming.SPECIFIED_BRANCH_AND_TIME)
		model.setLandscapeChangeParameter( Double.parseDouble(getRequiredParameter(configValues, "LANDSCAPE_CHANGE_PARAMETER")));
	    else{
		String changeBranchAndTimeFileStr = configValues.get("CHANGE_BRANCH_AND_TIME_FILE");
		model.readChangeBranchAndTimeFile(changeBranchAndTimeFileStr);
	    }
	    
	    if (model.getLandscapeChangeTiming()==LandscapeChangeTiming.SPECIFIED_BRANCH_AND_TIME &&
		sharedLandscape == true)
		throw new InvalidParameterCombinationException("Can't have shared landscape and specified branch and time of change");
	    // String fixSumFitnessStr = configValues.get("FIX_SUM_FITNESS");
	    // if (fixSumFitnessStr == null)
	    // 	fixSumFitness = false;
	    // else		
	    // 	fixSumFitness =  Boolean.parseBoolean(fixSumFitnessStr);

	    //check for invalid combination of inputs
	    //and for context-specific parameters:
	    if (model.getNewFitnessRule() == NewFitnessRule.CURRENT_ALLELE_DEPENDENT){
		if (model.getSequenceLength() != 1)
		    throw new InvalidParameterCombinationException("Allele-dependent landscape change only works for single-site landscapes");
		if (model.getLandscapeChangeTiming() != LandscapeChangeTiming.FIXED_INTERVAL_LENGTH
		    && model.getLandscapeChangeTiming() != LandscapeChangeTiming.FIXED_NUM_CHANGES)
		    throw new InvalidParameterCombinationException("Allele-dependent landscape change only implemented for deterministic landscape change");

		//read the AGE_DEPENDENCE_COEFFICIENT
		String response = configValues.get("AGE_DEPENDENCE_COEFFICIENT");//misspelling fixed
		
		if (response == null)
		    configValues.get("EPISTASIS_COEFFICIENT"); //old in-house name, kept for backward compatibility
		if (response != null)
		    model.setAgeDependenceCoef(Double.parseDouble(response));
		else
		    throw new MissingParameterException ("AGE_DEPENDENCE_COEFFICIENT, required for allele-dependent fitness change");
	    }

	    //in the following, we make some extra motions to support the legacy DIST_PARAM parameter that used to parametrize both distributions
	    if (model.getInitialFitnessDefinition() == InitialFitness.LOGNORM ){
		String sigmaStr = configValues.get("SIGMA");
		if (sigmaStr == null){
		    model.setSigma(Double.parseDouble(getRequiredParameter(configValues, "DIST_PARAM", "SIGMA")));
		}else{
		    model.setSigma(Double.parseDouble(sigmaStr));
		}
		
	    } else if (model.getInitialFitnessDefinition() == InitialFitness.GAMMA){
		//		double alpha, beta;
		String alphaStr = configValues.get("GAMMA_ALPHA");
		String betaStr = configValues.get("GAMMA_BETA");
		if (alphaStr == null || betaStr == null){
		    model.setGammaAlpha(Double.parseDouble(getRequiredParameter(configValues, "DIST_PARAM", "GAMMA_ALPHA and GAMMA_BETA")));
		    model.setGammaBeta(model.getGammaAlpha());
		}else{
		    model.setGammaAlpha(Double.parseDouble(alphaStr));
		    model.setGammaBeta(Double.parseDouble(betaStr));
		}
	    } else   if (model.getInitialFitnessDefinition() == InitialFitness.FILE){
		model.setFitnessFile(getRequiredParameter(configValues, "FITNESS_FILE"));
		if (model.getNewFitnessRule() == NewFitnessRule.IID)
		    throw new InvalidParameterCombinationException("initial fitness definition " + model.getInitialFitnessDefinition() + " not compatible with new fitness rule IID ");
		model.readFitnessFromFile();		//		model.readFitnessFromFile(getRequiredParameter(configValues, "FITNESS_FILE"));
	    }

	}catch(MissingParameterException | InvalidParameterCombinationException  | UnrecognizedValueException e){
	    System.err.println(e.getMessage());
	    System.exit(-1);
	}
	
    }
    

    private static void readRootSequence(Map<String, String> configValues){
	String rootSeqFile = configValues.get("ROOT_SEQUENCE_FILE");
	if (rootSeqFile != null){
	    try{
				
		String rootSeq = "";
		boolean first = true;
		Scanner sc = new Scanner(new File(rootSeqFile));
		while(sc.hasNext()){
		    String nextLine = sc.next();
		    //accomodate fasta format
		    if (nextLine.charAt(0)== '>')
			if (first)
			    continue;
			else
			    break;
		    first = false;//fasta header
		    rootSeq += nextLine;
		}
		rootSequence = new byte[rootSeq.length()];

		//check that the root sequence belongs to the closure of the alphabet
		//		if (rootSeq.length() < sequenceLength)
		//		    throw new IllegalArgumentException("The provided root sequence is shorter ("+ rootSeq.length() + ") than the given sequence Length ("+sequenceLength+").");
		for (int i = 0; i < rootSeq.length(); i++){
		    int index = alphabet.indexOf(rootSeq.charAt(i));
		    if (index== -1){
			throw new IllegalArgumentException("The provided root sequence contains non-alphabet character: " + rootSeq.charAt(i));
		    }else{
			rootSequence[i] = (byte)index;
		    }
		}
	    }
	    catch(FileNotFoundException e){
		System.err.println("Error: cannot open the root sequence file " + rootSeqFile);
		System.exit(-1);
	    }
	}
    }
    /**
     * return the mutation rate from character indexed i to character indexed j
     * if mutation rate matrix has not been provided (is null), return 1.0
     * @param i, j - indices of the characters of interest
     * @return mutation rate from character i to character j
     */

    public static double getMutationRate(int i, int j){
	if (mutationRateMatrix == null)
	    return 1.0;
	else
	    return mutationRateMatrix[i][j];
    }

    /**
     * read the mutation rate matrix from file, if it is provided in the config file
     */
    public static void readMutationRateMatrix(Map<String, String> configValues){
	int alphabetLength = alphabet.length();

	String filename = configValues.get("MUTATION_RATE_MATRIX_FILE");

	if (filename != null){
	    mutationRateMatrix = new double[alphabetLength][alphabetLength];
	
	    try{
		Scanner sc = new Scanner(new File(filename));
		for (int i = 0; i < alphabetLength; i++)
		    for (int j = 0; j < alphabetLength; j++)
			mutationRateMatrix[i][j] = sc.nextDouble();
		sc.close();
	    }catch(FileNotFoundException e){
		System.err.println("Error: cannot open the mutation rate matrix file " + filename);
		System.exit(-1);
	    }catch(InputMismatchException e){
		System.err.println("Error: non-numeric value in your mutation rate file");
		System.exit(-1);
	    }
	    catch(NoSuchElementException e){
		System.err.println("Error: mutation rate matrix dimension seems to be shorter than the alphabet");
		System.exit(-1);
	    }
	    
	}
    }

    private static void assignGlobalValues(Map<String, String> globalConfigValues){
	
	// assign the global parameters based on what's been read
	try{ //this is solely for the sake of simplified missing parameter exception handling
	    String debugStr = globalConfigValues.get("DEBUG");
	    if (debugStr != null)
		debug=Boolean.parseBoolean(debugStr);
	    
	    String statsStr = globalConfigValues.get("COLLECT_STATS");
	    if (statsStr != null)
		collectStats=Boolean.parseBoolean(statsStr);

	    String seedStr = globalConfigValues.get("SEED");
	    if (seedStr != null){
		seed = Long.parseLong(seedStr);;
		seedSet = true;
		seedGenerator = new Random(seed);
		seedGenerator.nextLong();//advance to make it more "random"
				
	    }
	    

	    alphabet = getRequiredParameter(globalConfigValues, "ALPHABET");
	    treeFile = getRequiredParameter(globalConfigValues, "TREE_FILE");
	    
	    readRootSequence(globalConfigValues);
	    readMutationRateMatrix(globalConfigValues);
	    
	    String numRunsStr = globalConfigValues.get("NUM_INSTANCES");
	    if (numRunsStr == null)
		numRunsStr = globalConfigValues.get("NUM_RUNS"); //old in-house name, supported for backward compatibility
	    if (numRunsStr != null)
		numInstances = Integer.parseInt(numRunsStr); //else it defaults to 1
	    
	    String numThreadsStr = globalConfigValues.get("NUM_THREADS");
	    if (numThreadsStr != null){
		numThreads = Integer.parseInt(numThreadsStr);
	    }
	    String printFitnessInfoStr = globalConfigValues.get("PRINT_LANDSCAPE_INFO");
	    if (printFitnessInfoStr == null)
		printFitnessInfo = false;
	    else
		printFitnessInfo =  Boolean.parseBoolean(printFitnessInfoStr);
	    
	    
	    String qNormalizationStr = globalConfigValues.get("CONSTANT_RATE");
	    if (qNormalizationStr==null)
		qNormalization = true;
	    else
		qNormalization = Boolean.parseBoolean(qNormalizationStr);
	    
	    String scaleLandscapeStr = globalConfigValues.get("SCALE_LANDSCAPE_CHANGE_TO_SUBSTITUTION_RATE");
	    if (scaleLandscapeStr == null)
		scaleLandscapeChangeToSubstitutionRate = false;
	    else
		scaleLandscapeChangeToSubstitutionRate = Boolean.parseBoolean(scaleLandscapeStr);
	    
	    
	}catch(MissingParameterException | InvalidParameterCombinationException  | UnrecognizedValueException e){
	    System.err.println(e.getMessage());
	    System.exit(-1);
	}
	
    }
    //parse the config file
    public static void init(String configFile){	
	Map<String, String> globalConfigValues = new HashMap<String, String>();
	Map<String, String> modelConfigValues = new HashMap<String, String>();

	boolean oldFormatConfig = true; //a hack for backwards compatibility with the old single-model config files
	try{
	    Scanner sc;
	    if (configFile.equals("-"))
		sc = new Scanner(System.in);
	    else
		sc = new Scanner(new File(configFile));

	    Model model0 = new Model();
	    boolean readingGlobal = true; // are we reading the global parameters?
	    int landscapeCount = 0;
	    while (sc.hasNextLine()){
		String line = sc.nextLine();
		if (line.trim().length() == 0 || line.charAt(0) == '#')
		    continue;
		if (line.startsWith("[END GLOBAL PARAMETERS]")){
		    readingGlobal = false;
		    assignGlobalValues(globalConfigValues);
		    oldFormatConfig = false;
		    break;
		}

		String[] fields = line.split("\\s+");
		if (fields.length!=2)
		    System.err.println("line: " + line);
		else
		    //		    if (readingGlobal)
		    globalConfigValues.put(fields[0], fields[1]);
	    }
	    //	    String varLandscapeStr = globalConfigValues.get("VARIABLE_LANDSCAPES");
	    int numLandscapes;
	    //	    if (varLandscapeStr != null && varLandscapeStr.equals("true")){
	    String numLandscapeStr = globalConfigValues.get("NUM_LANDSCAPES");
	    if (numLandscapeStr != null){
		variableLandscapes = true;
		numLandscapes = Integer.parseInt(numLandscapeStr);
		numInstances = numLandscapes; //numInstances is a global parameter
	    }else
		numLandscapes = 1;
	    models = new Model[numLandscapes];
	    landscapeCount = 0;
	    Model model = new Model();
	    while (sc.hasNextLine()){
		String line = sc.nextLine();
		if (line.length() == 0 || line.charAt(0) == '#')
		    continue;
		
		if (line.startsWith("[LANDSCAPE")){ //use regexp to get landscape #
		    oldFormatConfig = false;
		    if (modelConfigValues.size() > 0){ //this is not the first;
			assignModelValues(model, modelConfigValues);
		    }
		    modelConfigValues = new HashMap<String, String>();
		    model = new Model(); 
		    models[landscapeCount++] = model;
		}else{
		    String[] fields = line.split("\\s+");
		    if (fields.length!=2)
			System.err.println("line: " + line);
		    else
			//		    if (readingGlobal)
			modelConfigValues.put(fields[0], fields[1]);
		}
	    }
	    if (!oldFormatConfig){
		if (modelConfigValues.size() > 0){ //this is not the first;
		    assignModelValues(model, modelConfigValues);
		}
	    } else{
		assignGlobalValues(globalConfigValues);
		assignModelValues(model, globalConfigValues); //if we didn't see variable-landscape markers,
		//everything is in the global config map
		models[0] = model; // we hadn't added model to the models array
		//		model.printParams();
	    }
	} catch (IOException e){
	    System.err.println("Error opening config file " + configFile);
	    System.exit(-1);
	}
	if (rootSequence != null){
	    //now assign the root sequence to model files
	    int seqUsedUp = 0;
	    int cnt = 0;
	    for (Model model: models){
		int modelSeqLength = model.getSequenceLength();
		int start = seqUsedUp;
		int end = seqUsedUp+modelSeqLength;
		if (start > rootSequence.length || end > rootSequence.length){
		    System.err.println("Error: provided root sequence too short for the sum of desired per-model sequences");
		    System.exit(-1);
		}
		model.setRootSequence(java.util.Arrays.copyOfRange(rootSequence, start, end));
		seqUsedUp+=modelSeqLength;
		cnt++;
	    }
	}
    }
}

  
