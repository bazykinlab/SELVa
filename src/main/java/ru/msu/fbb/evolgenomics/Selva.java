// import edu.berkeley.compbio.phyloutils.*;
// import com.davidsoergel.trees.*;
// import com.davidsoergel.dsutils.*;
import java.util.*;
import java.io.*;
import java.util.concurrent.*;

/**
 * The main class  
 */
public class Selva {

    public static void computeLandscapeChangeInterval(BasicTree tree){
	double maxDepth = tree.getTreeHeight();
	
	int numLandscapeChanges = (int)Model.getLandscapeChangeParameter();
	double interval = maxDepth/(numLandscapeChanges+1);
	//will this cause problems with rounding?  perhaps it's safer to hard-code the "0 changes" case
	if (Model.debug())
	    System.out.println("landscape change interval: " + interval);
	Model.setLandscapeChangeInterval(interval);
    }

    public static void main (String args[]){
	try{
	    Model.init(args[0]);
	    double startTime = (double)System.currentTimeMillis();
	    int numLandscapes = Model.getNumRuns();
	    int seqLength = Model.getSequenceLength();

	    //read and build the phylogenetic tree
	    BasicTree tree = new BasicTree(Model.getTreeFile());	    


	    // compute the parameters of landscape change.
	    // wee need to know the tree height for fixed num intervals, so we do it here and not earlier
	    if (Model.getLandscapeChangeTiming() == LandscapeChangeTiming.FIXED_NUM_CHANGES){
		computeLandscapeChangeInterval(tree);
	    }else if (Model.getLandscapeChangeTiming() == LandscapeChangeTiming.FIXED_INTERVAL_LENGTH){
		Model.setLandscapeChangeInterval(Model.getLandscapeChangeParameter());
	    }
	    else{
		// we're _not_ doing deterministic change, so set the change interval to infinity
		Model.setLandscapeChangeInterval(Double.POSITIVE_INFINITY);
		//interpret the LANDSCAPE_CHANGE_PARAMETER as the stochastic rate
		Model.setLandscapeChangeRate(Model.getLandscapeChangeParameter());
	    }

	    //print the values of all model parameters
	    Model.printParams();
	    System.out.println("tree height : " + tree.getTreeHeight());
	    /* start the simulation! */

	    EvolutionaryProcess[] processes = new EvolutionaryProcess[numLandscapes];
	    int numThreads = Model.getNumThreads();
	    int batchSize = numLandscapes / numThreads;
	    //is this correct?
	    if (numLandscapes % numThreads > 0)
		batchSize++;

	    ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

	    for (int i = 0; i < numLandscapes; i++){
		processes[i] = new EvolutionaryProcess(tree, seqLength, i);
		executorService.execute(processes[i]);
	    }
	    
	    executorService.shutdown();
	    if (Model.debug())
		System.err.println("call awaitTermination");
	    executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
	    if (Model.debug())
		System.err.println("all threads completed");

	    double endTime = (double) System.currentTimeMillis();
	    if (Model.debug()){
		System.err.printf("simulations took %.5f seconds\n", (endTime-startTime)/1000.0);
		System.out.printf("simulations took %.5f seconds\n", (endTime-startTime)/1000.0);
	    }
	    //merge sequence and changetime data from different runs
	    PrintWriter seqWriter = new PrintWriter("allnodes.merged.fasta");
            PrintWriter changeTimeWriter;
            PrintWriter fitnessWriter;
            if (Model.printFitnessInfo()){
                changeTimeWriter = new PrintWriter("changetimes.merged.fasta");
                fitnessWriter = new PrintWriter("fitnesses.merged.fasta");
            }else{  
                changeTimeWriter = null;
                fitnessWriter = null;

            }
            String alphabet = Model.getAlphabet();
	    boolean first = true;
	    for (BasicNode node: tree.getNodes()){//		byte[] mergedArr = entry.getValue();
		String nodeName = node.getName();
		seqWriter.println(">"+nodeName);
		if (Model.printFitnessInfo()){
                    changeTimeWriter.println(">"+nodeName); 
                    fitnessWriter.println(">"+nodeName);
		}		
		int k = 0;
		for (int i = 0; i < numLandscapes; i++){
		    //merge sequence info
		    byte[] arr = processes[i].node2seq.get(node);
		    for (int j = 0;  j < arr.length; j++){
		     	seqWriter.append(alphabet.charAt(arr[j]));
		    }
		    //merge fitness change info, but ony if the user asks for it
                    if (Model.printFitnessInfo()){
                        changeTimeWriter.append(processes[i].changeTracker.getChangeTimes(nodeName) + "; ");
                        fitnessWriter.append(processes[i].changeTracker.getFitnesses(nodeName) + "; ");
                    }
		    if (first){
			if (Model.collectStats())
			    System.err.println("landscape " + i + ": " + processes[i].changeTracker.count + " changes");
			first =false;
		    }
                }
		seqWriter.println();
		if (Model.printFitnessInfo()){
                    fitnessWriter.println();
                    changeTimeWriter.println();
                }
	    }
	    seqWriter.close();
	    if (Model.printFitnessInfo()){  
                fitnessWriter.close();
                changeTimeWriter.close();
            }
            endTime = (double) System.currentTimeMillis();
	    //	    System.err.printf("entire computation took %.5f seconds\n", (endTime-startTime)/1000.0);
	    System.out.printf("entire computation took %.5f seconds\n", (endTime-startTime)/1000.0);
	}catch(Exception e){
	    System.err.println(e);
	    e.printStackTrace();
	    System.exit(-1);
	}
    }
 }
