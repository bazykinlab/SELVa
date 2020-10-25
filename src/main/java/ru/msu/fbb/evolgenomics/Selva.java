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


    public static void main (String args[]){
	try{
	    Parameters.init(args[0]);
	    double startTime = (double)System.currentTimeMillis();
	    //read and build the phylogenetic tree
	    BasicTree tree = new BasicTree(Parameters.getTreeFile());	    
	    //print the values of all model parameters

	    System.out.println("tree height : " + tree.getTreeHeight());
	    Model[] models = Parameters.models;
	    for (Model model: models){
		model.computeAndSetLandscapeChangeParameters(tree.getTreeHeight());
	    }

	    Parameters.printParams();	    
	    
	    int numLandscapes = Parameters.getNumRuns();
	    //	    int seqLength = Model.getSequenceLength();
	    
	    
	    /* start the simulation! */
	    
	    EvolutionaryProcess[] processes = new EvolutionaryProcess[numLandscapes];
	    int numThreads = Parameters.getNumThreads();
	    // int batchSize = numLandscapes / numThreads;
	    // //is this correct?
	    // if (numLandscapes % numThreads > 0)
	    //     batchSize++;
	    
	    
	    ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
	    // this is where the paths for one setup, many instances diverges from variable landscapes
	
	    for (int i = 0; i < numLandscapes; i++){
		if (!Parameters.getIsVariableLandscapes()){	
		    processes[i] = new EvolutionaryProcess(tree, models[0], i);
		}else{
		    processes[i] = new EvolutionaryProcess(tree, models[i], i);
		}
		executorService.execute(processes[i]);
	    }
	    
	    
	    executorService.shutdown();
	    if (Parameters.debug())
		System.err.println("call awaitTermination");
	    executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
	    if (Parameters.debug())
		System.err.println("all threads completed");

	    double endTime = (double) System.currentTimeMillis();
	    if (Parameters.debug()){
		System.err.printf("simulations took %.5f seconds\n", (endTime-startTime)/1000.0);
		System.out.printf("simulations took %.5f seconds\n", (endTime-startTime)/1000.0);
	    }
	    //merge sequence and changetime data from different runs
	    PrintWriter seqWriter = new PrintWriter("allnodes.merged.fasta");
            PrintWriter changeTimeWriter;
            PrintWriter fitnessWriter;
            if (Parameters.printFitnessInfo()){
                changeTimeWriter = new PrintWriter("changetimes.merged.fasta");
                fitnessWriter = new PrintWriter("fitnesses.merged.fasta");
            }else{  
                changeTimeWriter = null;
                fitnessWriter = null;

            }
            String alphabet = Parameters.getAlphabet();
	    boolean first = true;
	    for (BasicNode node: tree.getNodes()){//		byte[] mergedArr = entry.getValue();
		String nodeName = node.getName();
		seqWriter.println(">"+nodeName);
		if (Parameters.printFitnessInfo()){
                    changeTimeWriter.println(">"+nodeName); 
                    fitnessWriter.println(">"+nodeName);
		}		
		int k = 0;
		for (int i = 0; i < numLandscapes; i++){
		    //merge sequence info
		    byte[] arr = processes[i].node2seq.get(node);
		    //		    System.out.println("arr: " + arr);
		    for (int j = 0;  j < arr.length; j++){
		     	seqWriter.append(alphabet.charAt(arr[j]));
		    }
		    //merge fitness change info, but ony if the user asks for it
                    if (Parameters.printFitnessInfo()){
                        changeTimeWriter.append(processes[i].changeTracker.getChangeTimes(nodeName) + "; ");
                        fitnessWriter.append(processes[i].changeTracker.getFitnesses(nodeName) + "; ");
                    }
		    if (first){
			if (Parameters.collectStats())
			    System.err.println("landscape " + i + ": " + processes[i].changeTracker.count + " changes");
			first =false;
		    }
                }
		seqWriter.println();
		if (Parameters.printFitnessInfo()){
                    fitnessWriter.println();
                    changeTimeWriter.println();
                }
	    }
	    seqWriter.close();
	    if (Parameters.printFitnessInfo()){  
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
