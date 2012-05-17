package owltools.graph.shunt;

import java.util.HashSet;
import java.util.Set;

import com.google.gson.Gson;

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

	/**
	 * Empty constructor.
	 */
	public OWLShuntGraph() {	
	}
	
	/**
	 * Simply "add" a node to the graph.
	 * 
	 * @param n
	 */
	public void addNode(OWLShuntNode n){
		nodes.add(n);
	}

	/**
	 * Simply "add" an edge to the graph.
	 * 
	 * @param e
	 */
	public void addEdge(OWLShuntEdge e){
		edges.add(e);
	}
	
	/**
	 * 
	 * @return JSON form of the shunt graph structure
	 */
	public String toJSON(){
		
		Gson gson = new Gson();
		return gson.toJson(this);
	}
}
