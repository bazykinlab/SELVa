// import edu.berkeley.compbio.phyloutils.*;
// import com.davidsoergel.trees.*;
// import com.davidsoergel.dsutils.*;
import java.util.*;
class Branch implements Comparable<Branch>{
    int [] arr;
    BasicNode node;
    Branch(BasicNode node, int seqLength){
	arr = new int[seqLength];
	this.node = node;
    }
    double getLength(){
	return node.getLength();
    }
    static String getHeader(){
	return "avg_total_hits\tavg_observed_hits\tnum_nonobserved_hits\tnum_multihit_positions";
    }
    String getStats(){
	int allCount = 0;
	int uniqueCount = 0;
	int multiCount = 0;
	for (int i = 0; i < arr.length; i++){
	    allCount+=arr[i];
	    if (arr[i] > 1)
		multiCount++;
	    if (arr[i] > 0)
		uniqueCount++;
	}
	return ""+ (double)allCount/(double)arr.length + "\t" + (double)uniqueCount/(double)arr.length
	    + "\t" + (allCount - uniqueCount) + "\t"+ multiCount ;
    }
    
    double getAverageSubstitutions(){
	int count = 0;
	for (int i = 0; i < arr.length; i++){
	    count+=arr[i];
	}
	return (double)count/(double)arr.length;
	}
    int getNumDoubleSubstitutions(){
	int count = 0;
	for (int i = 0; i < arr.length; i++){
	    if (arr[i] > 1)
		count++;
	}
	return count;
    }
    public int compareTo(Branch other){
	double diff = this.node.getLength()-other.node.getLength();
	if (diff < 0)
	    return -1;
	else if (diff > 0)
	    return 1;
	else
	    return 0;
    }
}

public class SubstitutionAnalyzer{

    private static HashMap <String, Branch >  node2branch;
    private static int seqLength;
    private static int[] totalCounter;
    private static int sumSubstitutions=0;
    
    public static void init(BasicTree  tree, int seqLength){	
	node2branch = new  HashMap<String, Branch>(tree.getNumNodes()*2);
	for (BasicNode node2 : tree.getNodes() ){
	    BasicNode node = (BasicNode)node2;
	    node2branch.put(node.toString(), new Branch(node,seqLength));
	}
	totalCounter = new int[seqLength];
	SubstitutionAnalyzer.seqLength = seqLength;
	sumSubstitutions=0;
    }
    
    public static void registerSubstitution(String nodeName, int position){
	Branch branch = node2branch.get(nodeName);
	branch.arr[position]++;
	node2branch.put(nodeName, branch);
	totalCounter[position]++;
	sumSubstitutions++;
    }
    public static void substitutionStats(){
	System.err.println("average substitutions per tree: " + ((double)sumSubstitutions/(double) seqLength));
	Branch[] branchArr = node2branch.values().toArray(new Branch[node2branch.size()] );
	Arrays.sort(branchArr);
	System.out.println("branch\tlength\t" + Branch.getHeader());

	for (Branch branch: branchArr){
	    System.out.println(branch.node.toString()+"\t"+branch.getLength() + "\t" + branch.getStats() );
	}
	//see the number of multiple hits per branch, compare with branch length
    }
}
