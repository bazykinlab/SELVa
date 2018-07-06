import java.util.*;
//import java.util.concurrent.*;

/** 
 * The class responsible for the actual simulation.
 * Every simulation instance gets its own EvolutionaryProcess object
 */

public class EvolutionaryProcess implements Runnable{
    /**
     * Class for keeping track of the landscape changes that take place during
     * this simulation instance (if the user chooses to save and print that information)
     */
    class ChangeTracker{
	public int count = 0;  //for debugging purposes
	private class ChangeStruct{ // the stucture for keeping track of changes on one branch
	    ArrayList<Double> changetimes;   //times of changes (on that branch)
	    ArrayList<double[]> fitnesses;   //values of new fitnesses (on this branch)
	    public ChangeStruct(){ 
		changetimes = new ArrayList<Double>();
		fitnesses = new ArrayList<double[]>();
	    }
	    
	}
	private Map<String, ChangeStruct> branch2changes; //map branches to ChangeStructs
	/**
	 * Create new ChangeTracker for this instance; only does something if the
	 * user chooses to save and print fitness change info
	 */
	public ChangeTracker(){
	    if (Model.printFitnessInfo())       
		branch2changes = new HashMap<String, ChangeStruct> ();
	}
	
	/**
	 * Register a landscape change 
	 * @param branchName name of the node identifying the branch where the change occurs (its end)
	 * @param time the time on that branch when the change occurs
	 * @param fitness the new fitness vector
	 */
	void registerChange(String branchName, double time, double[] fitness){
	    count++;
	    if (!Model.printFitnessInfo())
		return;
	    ChangeStruct str =  branch2changes.get(branchName);
	    if (str == null){
		str = new ChangeStruct();
		branch2changes.put(branchName, str);
	    }
	    str.changetimes.add(time);
	    str.fitnesses.add(fitness);
	}
	/**
	 * Get the landscape change times on the given branch
	 * @param nodeName name of the node that is the endpoing of the branch
	 * @return the changetimes encoded as string (inside []) or null if this info is not requested
	 */
	public String getChangeTimes(String nodeName){
	    if (!Model.printFitnessInfo()) 
		return null;
	    ChangeStruct cs  = branch2changes.get(nodeName);
	    if (cs == null)
		return "[]";	
	    ArrayList<Double> arr = cs.changetimes;
	    return arr.toString();
	}
	/**
	 * Get the fitness vectors that were assigned  on the given branch
	 * @param nodeName name of the node that is the endpoing of the branch
	 * @return the changetimes separated by : inside {} or null if this info is not requested
	 * Returns null if this info is not requested
	 */
	public String  getFitnesses(String nodeName){
	    if (!Model.printFitnessInfo()) 
		return null;
	    ChangeStruct cs  = branch2changes.get(nodeName);
	    if (cs == null)
		return "{}";
	else{
	    ArrayList<double[]> arr = cs.fitnesses;
	    StringBuilder str = new StringBuilder(arr.size() * Model.getAlphabetSize()* 10);
	    str.append('{');
	    for (double[] fitness : arr){
		str.append(Arrays.toString(fitness) +":");
	    }
	    str.setCharAt(str.length()-1, '}');
	    return str.toString();
	 
	}
	}
    }

    /**
     * The class for caching sequence aspect of the evolutionary simulation
     * It kepes track of:
     *  - the current sequence (encoded as array of allele indices of length seqLength
     *  - a vector of length seqLength containing the current substitution rate for each
     *  alle in the sequence AND as its last entry the rate of landscape change.
     *  This vector is normalized to sum to 1 in order to be a probability distribution
     *  from which the choice of event is drawn
     *   - sumRates, the total rate of any change occurring
     */
    class Seq{
	byte[] seq; //the sequence encoded as array of allele indices
	int seqLength;   // sequence length, cached for simplicity
	double[] changeRateVect; //the vector of allele substitution rate and landscape change rate
	double sumRates; // the total substitution rate

	/**
	 * Create an empty Seq structure for sequence of length seqLength
	 * @param seqLength the length of the desired sequence
	 */
	public Seq (int seqLength){
	    this.seqLength = seqLength;
	    seq = new byte[seqLength];
	    changeRateVect = new double[seqLength+1];
	}
	
	/**
	 * Create a new Seq structure for sequence of length seqLength and initialize it
	 * according to the stationary distribution of the given landscape
	 * @param seqLength the length of the desired sequence
	 * @param landsacpe current fitness landsacpe (for drawing alleles of the sequence)
	 */
	public Seq (int seqLength, Landscape landscape){
	    this(seqLength);
	    generateSeq(landscape);
	    computeChangeRateVect(landscape);
	}
	/**
	 * Create a new Seq structure by cloning another one
	 * @param orig Seq structure to clone
	 */
	Seq(Seq orig){
	    seq = java.util.Arrays.copyOf(orig.seq, orig.seq.length);
	    changeRateVect = java.util.Arrays.copyOf(orig.changeRateVect, orig.changeRateVect.length);
	    sumRates = orig.sumRates;
	    seqLength = orig.seqLength;
	}
	
	/**
	 * Fill in the vector of substitution rates for each allele in the sequence
	 * Fill in the landsacpe change rate (0 if deterministic)
	 * Noramlize the vector to sum to 1
	 * @param landscape the landscape governing the substitution rates
	 */
	void computeChangeRateVect(Landscape landscape){

	    sumRates = 0;
	    for (int i = 0; i < seq.length; i++){
		int curChar = seq[i];
		changeRateVect[i] = -landscape.Qat(curChar,curChar);
		sumRates += -landscape.Qat(curChar,curChar);
	    }
	    if (Model.getLandscapeChangeTiming() == LandscapeChangeTiming.STOCHASTIC){
		changeRateVect[seq.length] = Model.getLandscapeChangeRate();	 
		if (Model.scaleLandscapeChangeToSubstitutionRate()) 
		    changeRateVect[seq.length] *= landscape.getDiagQtimesPi();
	    }else
		changeRateVect[seq.length] = 0; //for deterministic change

	    sumRates += changeRateVect[seq.length];

	    //normalize the changeRateVect for it to be a probability distribution on change events
	    for (int i = 0; i < changeRateVect.length; i++)
		changeRateVect[i]/=sumRates;
	}
	
	/**
	 * After a substitution, recompute the changeRateVect (minimizing the recomputation)
	 * @param landscape the fitness landscape governing the rates
	 * @parm positionChanged the position in the sequence/changeRateVect that was changed
	 * @param oldChar the allele that was in the position before the substitution
	 */
	private void recomputeChangeRateVect(Landscape landscape, int positionChanged, int oldChar){
	    /* do it more simply, just straightforward, with no tricks */
	    computeChangeRateVect(landscape);
	    /*
	    //compute total rate of change for all positions:
	    double oldSum = sumRates; //the rate of change before the substitution

	    //first, subtract from total change rate the contribution of the old allele's rate
	    sumRates -= -landscape.Qat(oldChar, oldChar); 
	    int curChar = seq[positionChanged]; //the new allele
	    //divide the new allele's scaled rate  by oldSum
	    //so that later we can multiply by oldSum/[new] sumRates
	    changeRateVect[positionChanged] = -landscape.Qat(curChar,curChar)/oldSum; 

	    //now add the contribution of the new allele's rate to the total change rate
	    sumRates += -landscape.Qat(curChar,curChar); 

	    //renormalize the changeRateVect
	    for (int i = 0; i < changeRateVect.length; i++)
		changeRateVect[i]*=(oldSum/sumRates);


	    */
	    
	}
	/**
	 * Generate the sequence according to the landscape's stationary distribution
	 * @param landscape the landsacpe defining the allele distribution
	 */
	private void  generateSeq(Landscape landscape){
	    for (int i = 0; i < seqLength; i++){
		int index = random.sampleDiscrete(landscape.getPi());
		seq[i] = (byte)index;
	    }
	}
	/**
	 * Update the sequence after a substitution event
	 * @param whichEvent the index of sequence character to update
	 * @param landscape the current landscape
	 */
	private  void updateSeq( int whichEvent, Landscape landscape){
	    byte oldChar = seq[whichEvent];

	    seq[whichEvent] = landscape.pickNewCharacter( seq[whichEvent]);
	    recomputeChangeRateVect(landscape, whichEvent, oldChar);
	}
    }
    //members of the EvolutionaryProcess class    
    private BasicTree tree; //the phylogenetic tree 
    Map<BasicNode, byte[]> node2seq; //mapping from tree nodes to sequences at those nodes
    ChangeTracker changeTracker; //keep track of the changes
    private int seqLength; 
    int id; //process id
    private RandomNumberGenerator random; //this evolutionary process's separate RNG
    
    /**
     * Create a new EvolutionaryProcess 
     * @param tree the phylognetic tree that the simulation takes place on
     * @param seqLength the sequence length
     * @param id the process id
     */
    public EvolutionaryProcess (BasicTree tree, int seqLength, int id){
	this.id = id;
	this.tree = tree;
	node2seq = new HashMap<BasicNode, byte[]>(tree.getNumNodes());
	this.seqLength = seqLength;
	
    }
    /**
     * Sample the next event based on the [substitution rates, landscape change rate] vector
     * @param changeRateVect the vector from which to sample the change
     */
    private  int sampleNextEvent(double [] changeRateVect){
	return random.sampleDiscrete(changeRateVect);
    }
    
    private  void printTree(BasicNode root){
	System.out.println("this is:" + root + " at depth " + root.getDepth() );
	byte[] seq = node2seq.get(root);
	
	System.out.println("its children are: ");
	List<BasicNode > children = root.getChildren();
	for (BasicNode child : children){
	    System.out.println(child + " " + child.getLength());
	}
	System.out.println();
	for (BasicNode child : children){
	    printTree(child);
	}
    }
    /**
     * Do the things that need to be done when we change the landscape
     * @param ls the current landsacpe
     * @param seqStr the sequence structure
     * @param changeTracker structure for registering the landscape change
     * @param nodeName name of the tree node on the branch to which the change occurs
     * @param branchLeft the branch time until that node
     * @param firstChangeInBranch is this the first time on this branch that the landscape changes?
     * @return the new landscape (may actually be the old Java object ls)
     */
	private Landscape changeLandscape(Landscape ls, Seq seqStr, ChangeTracker changeTracker, String nodeName, double branchLeft, boolean firstChangeInBranch){

	    //if this is the first time we change landscape on this branch,
	    //we need to create a new landscape, so that sister branches won't be affected.
	    ls = Landscape.getNewLandscape(ls, firstChangeInBranch, seqStr.seq[0]);
	    //compute new changeRateVect based on the new landscape
	    seqStr.computeChangeRateVect(ls);
	    changeTracker.registerChange(nodeName, branchLeft, ls.getCopyOfFitness());
	    if (Model.debug())
		ls.printParams();

	    return ls;
	}

    /*** The simulation happens in the BFS order on the tree. Here's the infrastructure for it ***/
    
    /**
     * The (tree)node-associated data that is used in the BFS queue
     */
    class BFSNode{	
	BasicNode node; //the relevant tree nodey
	double parentTimeTillDeterministicLandscapeChange; //the time till deterministic landscape change
                                                           //remaining from the parent
	Seq origSeqStr; //the inherited sequence structure
	Landscape inheritedLandscape; //the inherited landscape
	BFSNode(BasicNode node, Landscape inheritedLandscape, double parentTimeTillDeterministicLandscapeChange, Seq origSeqStr){
	    this.node = node;
	    this.inheritedLandscape=inheritedLandscape;
	    this.parentTimeTillDeterministicLandscapeChange = parentTimeTillDeterministicLandscapeChange;
	    this.origSeqStr = origSeqStr;
	}
    }

    LinkedList<BFSNode> queue = new LinkedList<BFSNode>(); //the BFS queue

    /**
     * Do the evolution simulation as BFS on the tree
     */
    private void evolveBFS(){
	while (!queue.isEmpty()) {
            BFSNode bfsNode = queue.remove();
	    try{ //just to catch and trace our mess-up runtime exceptions

		List<BasicNode > children = bfsNode.node.getChildren(); //get the tree node's children
		//go down each branch
		for (BasicNode child : children){
		    boolean landscapeChangedThisBranch = false; //has landscape changed this branch?
		    
		    Seq seqStr = new Seq(bfsNode.origSeqStr);	 //create a copy of seq
		    Landscape localLS = bfsNode.inheritedLandscape;
		    double branchLeft = (double)child.getLength(); //the length of the branch remaining
		    int positionChanged = -1;

		    //time until next deterministic landscape change left over from the parent node
		    double timeTillDeterministicLandscapeChange = bfsNode.parentTimeTillDeterministicLandscapeChange; 
		    
		    while (branchLeft > 0){//this condition will always be true, i think

			//get the time till next stochastic event
			//(substitution and/or stochastic landscape change)
			double timeTillNextStochasticEvent = random.sampleExponential(seqStr.sumRates);

			if (timeTillDeterministicLandscapeChange < branchLeft  &&
			    timeTillDeterministicLandscapeChange < timeTillNextStochasticEvent){
			// if the next thing that's going to happen is deterministic landscape change...

			    branchLeft -= timeTillDeterministicLandscapeChange;

			    if (Model.debug()){
				System.out.println(id + ": change landscape deterministically at time "+ branchLeft +" before " + child + " whose depth is " + child.getDepth());
				//				System.out.println("seq: " + java.util.Arrays.toString(seqStr.seq));
			    }

			    //if the landscape hasn't changed before on this branch,
			    //this is the first landscape change on this branch,
			    //so we'll need a new landscape object
			    localLS = changeLandscape(localLS, seqStr, changeTracker, child.toString(), branchLeft, !landscapeChangedThisBranch);
			    landscapeChangedThisBranch = true;

			    timeTillDeterministicLandscapeChange = Model.getLandscapeChangeInterval();

			}else if (timeTillNextStochasticEvent < branchLeft  ){
			   //now consider the case that the next event is stochastic
			    //(substitution or stochastic landscape change)
			    
			    branchLeft-=timeTillNextStochasticEvent;

			    //what _is_ the next event?
			    //changeRateVect is of length seqLength+1, with the last element
			    //being the relative probability of landascpe change
			    int whichEvent =  sampleNextEvent(seqStr.changeRateVect);

			    if (whichEvent < seqStr.seqLength){//the next event changes the sequence	          
				positionChanged = whichEvent;
				if (Model.collectStats())
				    System.out.println("### " + child + "\t" + branchLeft);
				
				seqStr.updateSeq( whichEvent, localLS);
				if (Model.collectStats())
				    SubstitutionAnalyzer.registerSubstitution(child.toString(), whichEvent);
				
			    }else{//the next event is probabilistic landscape change 
				if (Model.debug()){
				    System.out.println(id + ": change landscape probabilistically at time "+ branchLeft +" before " + child.toString());
				    System.out.println("seqStr: " + seqStr);
				}
				//if landscape hasn't changed yet on this branch,
				//then this is the first change, so need new landscape
				localLS = changeLandscape(localLS, seqStr, changeTracker, child.toString(),
							  branchLeft, !landscapeChangedThisBranch);
				landscapeChangedThisBranch = true;
			    }
			    //subtract the elapsed time from time till next landscape change
			    timeTillDeterministicLandscapeChange -=timeTillNextStochasticEvent;
			    
			}else{
			    //the branch will run out before anything is scheduled to happen
			    //subtract the remainder of the branch length from the remaining
			    //determininstic interval
			    timeTillDeterministicLandscapeChange-=branchLeft;
			    break;
			}
		    }
		    //by now we've computed the sequence for the child node, so save it
		    node2seq.put(child, seqStr.seq);

		    //and queue the child for BFS
		    queue.add(new BFSNode(child, localLS, timeTillDeterministicLandscapeChange, seqStr));
		}
	    }catch(Exception e){
		e.printStackTrace();
	    }
	}
    }

    /**
     * Implementation of the run() method of the Runnable interface
     * This is where the EvolutionaryProcess execution begins
     */    
    public void run(){
	//create the rng
	random = new RandomNumberGenerator();

	//here we need to create the first landscape
	Landscape landscape = new Landscape(random);
        changeTracker = new ChangeTracker();

	
	if (Model.debug()){
	    landscape.printParams();
	}
        //record the starting landscape as taking place at time 0 before the root
        changeTracker.registerChange(tree.getRoot().toString(), 0, landscape.getCopyOfFitness());

	//compute the deterministic landscape change time
 	double landscapeChangeTime = Double.POSITIVE_INFINITY;
	if (Model.getLandscapeChangeTiming() == LandscapeChangeTiming.FIXED_INTERVAL_LENGTH
	    || Model.getLandscapeChangeTiming() == LandscapeChangeTiming.FIXED_NUM_CHANGES){
	    landscapeChangeTime = Model.getLandscapeChangeInterval();
	}else //if landscape change timing is stochastic, set the deterministic interval to infinity
	    landscapeChangeTime = Double.POSITIVE_INFINITY;

	//generate the root sequence and add it to the node2seq map
	Seq rootSeqStr = new Seq(seqLength, landscape);
	node2seq.put(tree.getRoot(), rootSeqStr.seq);
	if (Model.collectStats())
	    SubstitutionAnalyzer.init(tree, seqLength);
	

	//queue the root...
	queue.add(new BFSNode(tree.getRoot(), landscape, landscapeChangeTime, rootSeqStr));
	//...and start evolving!
	evolveBFS();
	if (Model.collectStats())
	    SubstitutionAnalyzer.substitutionStats();

	tree = null;//clear the tree data
    }
}
