package owltools.graph.shunt;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

/**
 * A simple (and easily serializable through GSON) graph model to move data from the
 * OWL model out to more standard graph resources. This is intended to be used with
 * OWLTools-Solr for loading ontology graph segements for eventual use with (and
 * modelled after) the BBOP JavaScript graph model.
 * 
 * TODO: To completely mirror the JS, predicates would have to be added as well.
 * 
 * The graph is not operational--merely a structured data store.
 * 
 * See: {@link owltools.graph.shunt.OWLShuntNode} and {@link owltools.graph.shunt.OWLShuntEdge}.
 * 
 */
public class OWLShuntGraph {
	
	public String id = null;
	public Set<OWLShuntNode> nodes = new HashSet<OWLShuntNode>();
	public Set<OWLShuntEdge> edges = new HashSet<OWLShuntEdge>();
	
	@SerializedName("meta")
	public Map<String, Object> metadata = new HashMap<String, Object>();

	private transient Map<String,Set<String>> childToParents = new HashMap<String,Set<String>>();
	private transient Map<String,Set<String>> parentToChildren = new HashMap<String,Set<String>>();
	//private Set<String> roots = null;
	
	/**
	 * Empty constructor.
	 */
	public OWLShuntGraph() {	
	}
	
	/**
	 * Check to see if a node is in the graph.
	 * 
	 * @param n
	 * @return boolean
	 */
	public boolean hasNode(OWLShuntNode n){
		return nodes.contains(n);
	}

	/**
	 * Check to see if an edge is in the graph.
	 * 
	 * @param e
	 * @return boolean
	 */
	public boolean hasEdge(OWLShuntEdge e){
		return edges.contains(e);
	}

	/**
	 * Simply "add" a node to the graph.
	 * 
	 * @param n
	 * @return boolean, true if new elt added
	 */
	public boolean addNode(OWLShuntNode n){
		return nodes.add(n);
	}

	/**
	 * Simply "add" an edge to the graph.
	 * 
	 * @param e
	 * @return boolean, true if new elt added
	 */
	public boolean addEdge(OWLShuntEdge e){

		String cid = e.sub;
		String pid = e.obj;

		// First, mark the child to parents relationship.
		// First, make sure there is a set there.
		if( ! childToParents.containsKey(pid) ){
			childToParents.put(pid, new HashSet<String>());
		}
		// Then add the relation if it is not in the set.
		Set<String> oya = childToParents.get(pid);
		if( ! oya.contains(cid) ){
			oya.add(cid);
		}
		
		// Now mark the parent to children relationships.
		// First, make sure there is a set there.
		if( ! parentToChildren.containsKey(cid) ){
			parentToChildren.put(cid, new HashSet<String>());
		}
		// Then add the relation if it is not in the set.
		Set<String> kids = parentToChildren.get(cid);
		if( ! kids.contains(pid) ){
			kids.add(pid);
		}
		
		// Standard return.
		return edges.add(e);
	}

	/**
	 * Return the set of parents of a node as IDs.
	 * 
	 * @param nodeID
	 * @return Set of strings
	 */
	public Set<String> getParents(String nodeID){
		Set<String> retset = new HashSet<String>();

		if( childToParents.containsKey(nodeID) ){
			retset = childToParents.get(nodeID);
		}
		
		return retset;
	}
	
	/**
	 * Return the set of children of a node as IDs.
	 * 
	 * @param nodeID
	 * @return Set of strings
	 */
	public Set<String> getChildren(String nodeID){
		Set<String> retset = new HashSet<String>();

		if( parentToChildren.containsKey(nodeID) ){
			retset = parentToChildren.get(nodeID);
		}
		
		return retset;
	}
	
	/**
	 * Return the set of graph roots (no parents) as IDs.
	 * 
	 * @return Set of strings
	 */
	public Set<String> getRoots(){
		
		Set<String> roots = new HashSet<String>();

		// Cycle through the nodes and see who has a parent.
		for( OWLShuntNode node : nodes ){
			Set<String> parents = getParents(node.id);
			if( parents == null || parents.isEmpty() ){
				roots.add(node.id);
			}
		}
		
		return roots;
	}
	
	/**
	 * Return the set of graph leaves (no children) as IDs.
	 * 
	 * @return Set of strings
	 */
	public Set<String> getLeaves(){
		
		Set<String> leaves = new HashSet<String>();

		// Cycle through the nodes and see who has a child.
		for( OWLShuntNode node : nodes ){
			Set<String> kids = getChildren(node.id);
			if( kids == null || kids.isEmpty() ){
				leaves.add(node.id);
			}
		}
		
		return leaves;
	}
	
	/**
	 * Get a depth-first iterator for the graph.
	 * 
	 * @return JSON form of the shunt graph structure
	 */
	public Iterator<String> iteratorDF(){
		return new OWLShuntGraphDFIterator(this);
	}

	/**
	 * 
	 * @return JSON form of the shunt graph structure
	 */
	public String toJSON(){
		
		Gson gson = new Gson();
		return gson.toJson(this);
	}
	
	/**
	 * 
	 * @return JSON form of the shunt graph structure
	 */
	public String unsafeToJSON(){
		
		//Gson gson = new Gson();
		//Gson gson = new GsonBuilder().serializeNulls().create();
		Gson gson = new GsonBuilder().disableHtmlEscaping().create();
		return gson.toJson(this);
	}
	
	/**
	 * Mark this graph as incomplete in the metadata and indicate the complete
	 * size of the graph.
	 * 
	 * @param realNodeCount
	 * @param realEdgeCount
	 */
	public void setIncomplete(int realNodeCount, int realEdgeCount) {
		metadata.put("incomplete-p", Boolean.TRUE);
		metadata.put("complete-node-count", Integer.valueOf(realNodeCount));
		metadata.put("complete-edge-count", Integer.valueOf(realEdgeCount));
	}
}
