import java.util.*;

/**
 * Class for the custom tree node structure.
 * Pretty much as basic as it gets, with a couple of cached values
 *
 * @see BasicTree
 */
public class BasicNode{
    private BasicNode parent;
    private ArrayList<BasicNode> children;
    private double length; //length of the branch leading to this node
    private String name;
    private double depth ;  //longest from node path to beginning
    private double depthFromRoot ;  //longest from node path to root
    private double longestPathDown; //longest path from node to its furtherst leaf descendant
    /**
     * @return the node name
     */
    public String getName(){return name;}
    /**
     * @return the distance from this node to root
     */
    public double getDepth(){return depth;}
    public double getDepthFromRoot(){return depthFromRoot;}

    /**
     * @return the length of the branch leading to this node (0 for root)
     */
    public double getLength(){return length;}

    /**
     * @return the parent of this node
     */
    public BasicNode getParent(){return parent;}

    /**
     * @return the children of this node
     */    
    public List<BasicNode> getChildren(){return children;}
    
    /**
     * Create a node with space for 2 children (initially)
     * @param name node name
     * @param parent the node's parent
     * @param length the length of the branch leading to the node
     */
    public BasicNode(String name, BasicNode parent, double length){
	this(name, parent, length, 2);
    }
    
    /**
     * Create a node with space for numChildren (initially)
     * @param name node name
     * @param parent the node's parent
     * @param length the length of the branch leading to the node
     * @param numChildren the number of node's children
     */
	
    public BasicNode(String name, BasicNode parent, double length, int numChildren){
	this.name = name;
	this.parent = parent;
	this.length = length;
	
	if (parent != null){
	    depth = length + parent.depth;
	    depthFromRoot = length + parent.depthFromRoot;
	    
	} else{
	    depth = length;
	    depthFromRoot = 0;
	}
	children = new ArrayList<BasicNode>(numChildren);
    }
    public void addChild(BasicNode child){
	children.add(child);
    }
    /**
     * @return longest path from node to its furtherst leaf descendant
     */
    public double getLongestPathDown(){return longestPathDown;}

    /**
     * Sets the longest path from node to its furtherst leaf descendant
     * @param len length of the longest path
     */
    public void setLongestPathDown(double len){longestPathDown = len;}
    public String toString (){ return name;}
}
