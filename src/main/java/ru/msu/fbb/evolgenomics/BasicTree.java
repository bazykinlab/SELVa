import net.sourceforge.olduvai.treejuxtaposer.drawer.*;
import net.sourceforge.olduvai.treejuxtaposer.*;
import java.util.*;
import java.io.*;

/**
 * A pretty minimalistic tree implementation based on BasicNode.
 * @see BasicNode
 */
public class BasicTree{

    //BasicTree fields
    private Map<String, BasicNode > name2node; //mapping from node name to node
    private final BasicNode root;
    //    private int numNodes;
    private ArrayList<BasicNode> leaves = new ArrayList<BasicNode>(); 

    //access methods
    /**
     * @return Collection of the tree's leaves
     */
    public Collection<BasicNode> getLeaves(){ return leaves;}
    public BasicNode getRoot(){return root;}

    /**
     * @return the numer of  tree nodes (internal and leaves) 
     */
    public int getNumNodes(){return name2node.size();}

    /**
     * @return the tree height (longest path from root to a leaf)
     */
    public double getTreeHeight(){return root.getLongestPathDown();}

    /**
     * @return the names of all tree nodes (internal and leaves)
     */
    public Set<String> getNodeNames(){return name2node.keySet();}

    /**
     * @return  all tree nodes (internal and leaves)
     */
    public Collection<BasicNode> getNodes(){return name2node.values();}

    /**
     * Return the BasicNode correspoding to the given name
     * @param name The node's name
     * @return The BasicNode corresponding to name or null if no such node exists
     */
    public BasicNode getNodeByName(String name){
	return name2node.get(name);
    }

    
    /**
     * Build tree from given the file
     * @param fileName name of the file (including path, if not in current directory)
     */
    public BasicTree(String fileName){
	//read the tree using the libnewicktree parser
	Tree tjTree = null; //start with the libnewicktree representation
	try{
	    BufferedReader r = new BufferedReader(new FileReader(fileName));
	    TreeParser tp = new TreeParser(r);
	    tjTree = tp.tokenize(fileName);

	}catch(FileNotFoundException e){
	    System.err.println("Error: cannot open the tree file " + fileName);
	    System.exit(-1);
	}	
	//now build "our" tree from the one created by the parser for ease of use
	name2node = new HashMap<String, BasicNode> (tjTree.nodes.size());
	TreeNode tjRoot = tjTree.getRoot();
	root = buildTreeFromParserRec(tjTree, tjRoot, null);
	//	numNodes = name2node.size();  
    }

    /**
     * Recursively build "our" tree from the one created by the parser
     * @parem tjTree  the tree built by the libnewicktree parser
     * @param tjNode  the current node of tjTree
     * @param myPrent the parent of the node in "our" tree
     * @return the root of the subtree we just built
     */
    private BasicNode buildTreeFromParserRec(Tree tjTree, TreeNode tjNode, BasicNode myParent){
	int numChildren = tjNode.numberChildren();
	BasicNode myNode = new BasicNode(tjNode.getName(), myParent, tjNode.getWeight(), numChildren);
	name2node.put(tjNode.getName(), myNode);
	double longestPathDown = 0;
	for (int i = 0; i < numChildren; i++) {
	    TreeNode tjChild = tjNode.getChild(i);
	    BasicNode child = buildTreeFromParserRec(tjTree, tjChild, myNode);
	    myNode.addChild(child);
	    double pathDownThroughChild = child.getLength() + child.getLongestPathDown();
	    if (pathDownThroughChild > longestPathDown)
		longestPathDown = pathDownThroughChild;
	}
	
	if (numChildren == 0){
	    leaves.add(myNode);
	}else{
	    myNode.setLongestPathDown(longestPathDown);
	}
	return myNode;
    }

    public void print(){
	System.out.println("the tree has: " + getNumNodes() + " nodes");
	System.out.println("they are: " + getNodes()); 
	System.out.println("their names are: " + getNodeNames());
	printTree(root);
	System.out.println("the leaves are: " + getLeaves());
				   
    }


    /**
     * recursive printing function
     * @param root root of the subtree to print
     */
    private  void printTree(BasicNode root){
	System.out.println("this is:" + root + " at depth " + root.getDepth() +
			   " and \"height\": " + root.getLongestPathDown());
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
    
    public static void main (String[] args){
	BasicTree tree = new BasicTree(args[0]);
	tree.print();
    }

}
