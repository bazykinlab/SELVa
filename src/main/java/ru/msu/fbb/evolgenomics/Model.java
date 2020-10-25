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
    public String toString(){
	return time + ": " + java.util.Arrays.toString(fitness);
    }
}


/**
 * The class for used to read and store the model parameters (provided in the config file).
 */
public class Model{
    private String fitnessFile;
    private NewFitnessRule newFitnessRule;
    private LandscapeChangeTiming landscapeChangeTiming;
    private InitialFitness initialFitnessDefinition;
    private byte[] rootSequence;
    private double sigma = -1;
    private double alpha = -1;
    private double beta = -1;
    private double landscapeChangeRate = 0;
    private double landscapeChangeInterval = Double.POSITIVE_INFINITY;
    private double landscapeChangeParameter;
    private double ageDependenceCoef = Double.NaN;
    //    private boolean fixSumFitness = false;
    private double[] initialFitnessVectorFromFile; //the fitness vectore read from file

    private HashMap<String, double[]> position2fitness; //map a string encoding branch and position
                                                               //to a fitness vector
    private HashMap<String, ChangeTime> changeBranchTimeFitness;

    /** 
     * @return the user-specified root sequence, encoded as indices of the ALPHABET string, or null if it is not provided
     * in the latter case, the root sequence will be generated from the root landscape's stationary distribution
     */
    public byte[] getRootSequenceFromFile(){ return rootSequence;  }
    
    public void setLandscapeChangeRate( double r){ landscapeChangeRate = r;}
    public void setLandscapeChangeInterval(double x){landscapeChangeInterval = x;}
    public double getLandscapeChangeRate() {return landscapeChangeRate;}
    public double getLandscapeChangeInterval(){return landscapeChangeInterval; }

    public double getAlleleAgeDependenceCoef() {return ageDependenceCoef;}
    public void setAgeDependenceCoef(double coef) {ageDependenceCoef = coef;}
    public double getSigma() { return sigma;  }
    public void setSigma(double s) {  sigma = s;  }
    public double getGammaAlpha() { return alpha;  }
    public void setGammaAlpha(double a) { alpha = a;  }
    public double getGammaBeta() { return beta;  }
    public void setGammaBeta(double b) { beta = b;  }
    public String getFitnessFile() { return fitnessFile;}
    public void setFitnessFile(String ff) { fitnessFile = ff;}

    /* getters for the parameters that are always set */
    public int getSequenceLength(){return sequenceLength;};
    private int sequenceLength;
    public void setSequenceLength(int sl){sequenceLength = sl;};
    public double getLandscapeChangeParameter(){ return landscapeChangeParameter;}
    public void setLandscapeChangeParameter(double lcp){ landscapeChangeParameter = lcp;}
    public InitialFitness getInitialFitnessDefinition(){return initialFitnessDefinition;}
    public void setInitialFitnessDefinition(InitialFitness ifd){ initialFitnessDefinition = ifd;}
    public LandscapeChangeTiming getLandscapeChangeTiming(){return landscapeChangeTiming;}
    public void setLandscapeChangeTiming(LandscapeChangeTiming lct){ landscapeChangeTiming = lct;}
    public NewFitnessRule getNewFitnessRule(){return newFitnessRule;}
    public void setNewFitnessRule(NewFitnessRule nfr){ newFitnessRule = nfr;}

    //    public boolean sharedLandscape(){return sharedLandscape;}
    public boolean fixSumFitness(){ return false; }     //not used
    public boolean changeAtSpecifiedBranchAndTime(){
	return landscapeChangeTiming == LandscapeChangeTiming.SPECIFIED_BRANCH_AND_TIME;
    }
        
    private void computeLandscapeChangeInterval(double maxDepth){
	
	int numLandscapeChanges = (int)getLandscapeChangeParameter();
	double interval = maxDepth/(numLandscapeChanges+1);
	//will this cause problems with rounding?  perhaps it's safer to hard-code the "0 changes" case
	if (Parameters.debug())
	    System.out.println("landscape change interval: " + interval);
	setLandscapeChangeInterval(interval);
    }

    public void computeAndSetLandscapeChangeParameters(double maxDepth){
	// compute the parameters of landscape change.
	// wee need to know the tree height for fixed num intervals, so we do it here and not earlier
	if (getLandscapeChangeTiming() == LandscapeChangeTiming.FIXED_NUM_CHANGES){
	    computeLandscapeChangeInterval(maxDepth);
	}else if (getLandscapeChangeTiming() == LandscapeChangeTiming.FIXED_INTERVAL_LENGTH){
	    setLandscapeChangeInterval(getLandscapeChangeParameter());
	}
	else{
	    // we're _not_ doing deterministic change, so set the change interval to infinity
	    setLandscapeChangeInterval(Double.POSITIVE_INFINITY);
	    //interpret the LANDSCAPE_CHANGE_PARAMETER as the stochastic rate
	    setLandscapeChangeRate(getLandscapeChangeParameter());
	}
    }

    
    /**
     * @param node finishing the branch that is queries
     * @return Double.NEGATIVE_INFINITY if no change this branch, time till the end node otherwise
     */
    public double getChangeTimeThisBranch(BasicNode node){
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
    public double[] getNewFitnessThisBranch(BasicNode node){
	String nodeName = node.toString();
	if (changeBranchTimeFitness.containsKey(nodeName))
	    return changeBranchTimeFitness.get(nodeName).fitness;
	else
	    return null;
    }


    /**
     * get the String corresponding to the alphabet index array
     * @param seqArr - sequence as array of bytes
     * @return corresponding sequence as a String
     */
     public String arr2seq(byte [] seqArr){
	String alphabet = Parameters.getAlphabet();
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
    public double[] getFitnessForPosition(String branchName, double position){
	if (landscapeChangeTiming != LandscapeChangeTiming.SPECIFIED_BRANCH_AND_TIME)
	    throw new
		InvalidParameterCombinationException("getFitnessForPosition() only applicable if  LANDSCAPCE_CHANGE_TIMING is set to  specified_branch_and_time");
	String key = branchAndTimeToKey(branchName, position);
	return position2fitness.get(key);
    }

    public double[] getInitialFitnessFromFile(){
	if (initialFitnessVectorFromFile == null)
	    readFitnessFromFile();
	return java.util.Arrays.copyOf(initialFitnessVectorFromFile, initialFitnessVectorFromFile.length);
    }

    public void readFitnessFromFile(){
	initialFitnessVectorFromFile = new double[Parameters.getAlphabet().length()];
 	//alphabetSize = Model.getAlphabet().length();
	// double[] fitness = new double[alphabetSize];
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
    public void setRootSequence(byte[] seq){
	rootSequence = seq;
    }

    
    /**
     * auxiliary function for converting branch name + time to key
     */
    public String branchAndTimeToKey( String branch, double time){
	return branch + " " + time;
    }
    
    /** Read the prespecified coordinates of landscape changes from a file
     * @param changeBranchAndTimeFileStr - name of the file with landscape change coordinates
     */
    public void readChangeBranchAndTimeFile(String changeBranchAndTimeFileStr){
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
		if (fields.length  !=  2 && fields.length != 2 + Parameters.getAlphabetSize() ){
		    System.err.println("error in change branch time file " + changeBranchAndTimeFileStr);
		    System.err.println("line: " + line);
		    System.exit(-1);
		}
		else{
		    try{
			String branch = fields[0];
			double time = Double.parseDouble(fields[1]);
			double [] fitness = null;
			
			if ( fields.length == 2 + Parameters.getAlphabetSize()){//the fitness is specified by the user

			    fitness = new double[Parameters.getAlphabetSize()];
			    for (int i = 0; i < Parameters.getAlphabetSize(); i++){
				fitness[i] = Double.parseDouble(fields[2+i]);
			    }
			    String key = branchAndTimeToKey(branch, time);

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
    
    public void printParams(){
	System.out.println("Model parameters:");
	try{
	    Class <?> c = Model.class;
	    for (Field f : c.getDeclaredFields()){
		String name = f.getName();
		if (!name.equals("fitnessFile")){
		    System.out.print(name + ":");
		    System.out.println(f.get(null));
		}
	    }
	    if (changeBranchTimeFitness != null){
		System.out.println("changeBranchTimeFitness :");
		for (Map.Entry<String, ChangeTime> entries : changeBranchTimeFitness.entrySet()){
		    System.out.println("\t" + entries.getKey() + " : " + entries.getValue());
		}
	    }
	    System.out.println("fitnessFile: " + fitnessFile);
	    System.out.println("initialFitnessVectorFromFile : " + java.util.Arrays.toString(initialFitnessVectorFromFile));
	    System.out.println("rootSequence (as indices of ALPHABET) : " + java.util.Arrays.toString(rootSequence));
	
	}catch(Exception e){
	    e.printStackTrace();
	}
    }

}

  
