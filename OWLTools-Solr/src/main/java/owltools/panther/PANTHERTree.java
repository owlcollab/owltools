package owltools.panther;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.forester.io.parsers.nhx.NHXFormatException;
import org.forester.io.parsers.nhx.NHXParser;
import org.forester.io.parsers.util.PhylogenyParserException;
import org.forester.phylogeny.Phylogeny;
import org.forester.phylogeny.PhylogenyNode;
import org.forester.phylogeny.iterators.PhylogenyNodeIterator;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.UnknownOWLOntologyException;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLGraphWrapper.ISynonym;
import owltools.graph.shunt.OWLShuntEdge;
import owltools.graph.shunt.OWLShuntGraph;
import owltools.graph.shunt.OWLShuntGraphDFIterator;
import owltools.graph.shunt.OWLShuntNode;
import owltools.io.ParserWrapper;

/**
 * Methods to simplify the work with the Newick-ish output we get from PANTHER. 
 */
public class PANTHERTree {
	
	private static final Logger LOG = Logger.getLogger(PANTHERTree.class);

	private final String treeName;
	private final String treeStr;

	//private int id_int = 0;
	private String[] treeAnns;
	private Set<String> annotationSet = null;
	private OWLShuntGraph g = null;
	private Map<String,Set<String>> ancestorClosureSet = null;
	private Map<String,Set<String>> descendantClosureSet = null;
	private Map<String,String> gpToNodeMap = new HashMap <String,String>();
	private Map<String,Set<String>> nodeToGpMap = new HashMap <String,Set<String>>();

	/**
	 * Create an instance for the given path
	 * 
	 * @param path
	 * @throws IOException 
	 */
	public PANTHERTree (File pFile) throws IOException {

		if( pFile == null ){ throw new Error("No file was specified..."); }
		
		//
		String fileAsStr = FileUtils.readFileToString(pFile);
		String[] lines = StringUtils.split(fileAsStr, "\n");
		
		// The first line is the tree in Newick-ish form, the rest is the annotation info
		// that needs to be parsed later on.
		if( lines.length < 2 ){
			throw new Error("Does not look like a usable PANTHER tree file.");
		}else{
			treeStr = lines[0];
			treeAnns = Arrays.copyOfRange(lines, 1, lines.length);
			
			if( treeAnns == null || treeStr == null || treeStr.equals("") || treeAnns.length < 1 ){
				throw new Error("It looks like a bad PANTHER tree file.");
			}else{
				String filename = pFile.getName();
				treeName = StringUtils.substringBefore(filename, ".");
			}
		}
		
		//LOG.info("Processing: " + getTreeName() + " with " + lines.length + " lines.");
		annotationSet = new HashSet<String>();
		generateGraph(); // this must come before annotation processing
		readyAnnotationDataCache();
	}
	
	/**
	 * Return the raw Newick-type input string.
	 */
	public String getNHXString(){
		return treeStr;
	}

	/**
	 * Return the raw Newick-type input string.
	 */
	public String getTreeName(){
		return treeName;
	}
	
	/**
	 * Generate graph information for the tree as it currently stands.
	 */
	private OWLShuntGraph generateGraph(){
		
		g = new OWLShuntGraph();
		
		// Parse the Newick tree down to something usable.
		NHXParser p = new NHXParser();
		Phylogeny[] phys = null;
		try {
			p.setSource(treeStr);
		} catch (PhylogenyParserException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}		
		try {
			phys = p.parse();
		} catch (NHXFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if( phys == null ){
			throw new Error("error while parsing newick tree");
		}
		
		// Assemble the graph(s, whence the for loop) from the parser.
		for( Phylogeny phy : phys ){

			// First, add all of the nodes to the graph.
			PhylogenyNodeIterator pIter = phy.iteratorLevelOrder();
			while( pIter.hasNext() ){

				// Gather the node information and add it to the graph.
				PhylogenyNode pNode = pIter.next();
				// Make node ids global.
				String id_str = uuidInternal(pNode);
				// Sensible label or use the id.
				String lbl = pNode.getName();
				if( lbl == null || lbl == "" ){
					lbl = id_str;
				}
				OWLShuntNode n = new OWLShuntNode(id_str);
				n.setLabel(lbl);
				g.addNode(n);
				
				// Next, gather the edge information and add it to the graph.
				List<PhylogenyNode> children = pNode.getDescendants();
				for( PhylogenyNode kid : children ){
					String enid_str = uuidInternal(kid);
					OWLShuntEdge e = new OWLShuntEdge(id_str, enid_str);
					// Have the distance as string for the metatdata.
					e.setMetadata(Double.toString(kid.getDistanceToParent()));
					g.addEdge(e);
				}
			}
		}
		// Okay, we have a graph...

		// Generate the ancestor closure information per-node.
		// The trick here is that the iterator is depth-first, so
		// our parents will be populated before we are and we can
		// transfer the info immediately.
		// Note that we're keeping track of the visitation order--
		// we'll use that later.
		ancestorClosureSet = new HashMap<String,Set<String>>(); 
		Iterator<String> i = g.iteratorDF();
		List<String> nodeOrder = new ArrayList<String>();
		while( i.hasNext() ){
			String nid = i.next();
			nodeOrder.add(nid);

			// Ready the closure for ourselves.
			Set<String> close = new HashSet<String>();
			
			// Add the information of our parents to ourselves.
			//LOG.info("node (anc): " + nid);
			Set<String> parents = g.getParents(nid);
			// For each of our parents, get their closure set and add it.
			for( String parent : parents ){
				Set<String> parentsClosure = ancestorClosureSet.get(parent);
				for( String anc : parentsClosure ){
					//LOG.info("\tclosure: " + anc);
					close.add(anc);
				}
			}
			// Add ourselves to the set as well.
			close.add(nid);
			
			// Add the set to the closure.
			ancestorClosureSet.put(nid, close);
		}
		
		// Now play the visitation order backwards and
		// collect the transitive descendant information.
		descendantClosureSet = new HashMap<String,Set<String>>(); 
		for( int in = (nodeOrder.size() -1); in >= 0; in-- ){
			  String nid = nodeOrder.get(in);
			  
			// Ready the closure for ourselves.
			Set<String> close = new HashSet<String>();
				
			// Add the information of our children to ourselves.
			//LOG.info("node (desc): " + nid);
			Set<String> kids = g.getChildren(nid);
			for( String kid : kids ){
				Set<String> childrenClosure = descendantClosureSet.get(kid);
				for( String desc : childrenClosure ){
					//LOG.info("\tclosure: " + desc);
					close.add(desc);
				}
			}
			// Add ourselves to the set.
			close.add(nid);
				
			// Add the set to the closure.
			descendantClosureSet.put(nid, close);
		}
		
		return g;
	}
	
	/**
	 * Return the complete OWL shunt graph repesentation of the phylogenic tree.
	 */
	public OWLShuntGraph getOWLShuntGraph(){		
		return g;
	}

	/**
	 * Return the ancestors of this node as an ID set.
	 * Includes this node.
	 * null if nothing is in the graph.
	 * 
	 * @param node id
	 * @return all ancestors (inclusive)
	 */
	public Set<String> getAncestors(String nodeID){

		Set<String> retset = ancestorClosureSet.get(nodeID);
		
		return retset;
	}
		
	/**
	 * Return the descendants of this node as an ID set.
	 * Includes this node.
	 * null if nothing is in the graph.
	 * 
	 * @param node id
	 * @return all ancestors (inclusive)
	 */
	public Set<String> getDescendants(String nodeID){

		Set<String> retset = descendantClosureSet.get(nodeID);
		
		return retset;
	}
		
	/**
	 * Return all annotations to ancestor nodes.
	 * 
	 * @param gene product id
	 * @return all ancestor annotations (inclusive)
	 */
	public Set<String> getAncestorAnnotations(String baseGpID){

		Set<String> retset = new HashSet<String>();
		
		// Look to see if that is a known node.
		String nodeID = gpToNodeMap.get(baseGpID);
		if( nodeID != null){
			
			// We found a node ID, so now we get the ancestor closure.
			// For each of the ancestors, look at the map to GPs and collect them.
			Set<String> ancs = getAncestors(nodeID);
			for( String ancID : ancs ){
				Set<String> gps = nodeToGpMap.get(ancID);
				if( gps != null && ! gps.isEmpty() ){
					for( String gp : gps ){
						retset.add(gp);						
					}
				}
			}
		}
				
		return retset;
	}

	/**
	 * Return all annotations to descendant nodes.
	 * 
	 * @param gene product id
	 * @return all descendant annotations (inclusive)
	 */
	public Set<String> getDescendantAnnotations(String baseGpID){

		Set<String> retset = new HashSet<String>();
		
		// Look to see if that is a known node.
		String nodeID = gpToNodeMap.get(baseGpID);
		if( nodeID != null){
			//LOG.info("node: " + nodeID);
			
			// We found a node ID, so now we get the descendant closure.
			// For each of the descendants, look at the map to GPs and collect them.
			Set<String> descs = getDescendants(nodeID);
			for( String descID : descs ){
				Set<String> gps = nodeToGpMap.get(descID);
				if( gps != null && ! gps.isEmpty() ){
					for( String gp : gps ){
						retset.add(gp);						
					}
				}
			}
		}
				
		return retset;
	}

	/**
	 * Return a globally "unique" identifier for an internal ID. 
	 * 
	 * Technically, in our case, the labels should be unique if defined.
	 */
	private String uuidInternal(PhylogenyNode pNode){

		int id_int = pNode.getId();
		String id_str = Integer.toString(id_int);
		//String id_str = Integer.toString(id_int);
		//id_int++;
		
		String try_id = pNode.getName();
		if( try_id == null || try_id == "" ){
			try_id = id_str;
		}

		return uuidInternal(try_id);
	}
	
	/**
	 * Return a globally "unique" identifier for an internal ID. 
	 * 
	 * Technically, in our case, the labels should be unique if defined.
	 */
	private String uuidInternal(String nodeIdentifier){
		return getTreeName() + ":" + nodeIdentifier;
	}
	
	/**
	 * Return a Set of 
	 */
	private void readyAnnotationDataCache(){
		
		//Map<String,String> idToInit = new HashMap<String,String>();
		//Map<String,Set<String>> initToId = new HashMap<String,Set<String>>();
		
		// Parse down every annotation line.
		//for( String aLine : treeAnns. ){
		for( int ti = 0; ti < treeAnns.length -1; ti++){

			String aLine = treeAnns[ti];
			
			// First, get rid of the terminal semicolon.
			String cleanALine = StringUtils.chop(aLine);			
			
			// Split out the sections.
			String[] sections = StringUtils.split(cleanALine, "|");
			if( sections.length != 3 ) throw new Error("Expected three sections in " + treeName);

			// Isolate the initial internal identifier and map it to a node.
			String initSection = sections[0];
			String rawNodeID = StringUtils.substringBefore(initSection, ":");
			String nodeID = uuidInternal(rawNodeID);
			
			// Isolate and convert the rest. This is done as individuals
			// And not a loop for now to higlight the fact that I think this will become
			// rather more complicated later on.
			String rawIdentifierOne = sections[1];
			String rawIdentifierTwo = sections[2];
			String oneID = StringUtils.replaceChars(rawIdentifierOne, '=', ':');
			String twoID = StringUtils.replaceChars(rawIdentifierTwo, '=', ':');

			// // Now make a map from the initial identitifer to the other two, the other
			// two to the identifier, and a batch Set for the existence of just the 
			// other two.
			//
			// String finalInitID = uuidInternal(initID);
			// Existence.
			annotationSet.add(oneID);
			annotationSet.add(twoID);
			//LOG.info("groupSet in: " + oneID);
			//LOG.info("groupSet in: " + twoID);
			
			// Create the gp -> node map.
			gpToNodeMap.put(oneID, nodeID);
			gpToNodeMap.put(twoID, nodeID);

			// Create the node -> gp map.
			Set<String> gps = null;
			if( nodeToGpMap.containsKey(nodeID) ){
				gps = nodeToGpMap.get(nodeID);
			}else{
				gps = new HashSet<String>();
			}
			gps.add(oneID);
			gps.add(twoID);
			nodeToGpMap.put(nodeID, gps);
		}
		
		//LOG.info("N->GP" + StringUtils.join(nodeToGpMap, ", "));
		//LOG.info("GP->N" + StringUtils.join(gpToNodeMap, ", "));
	}		

	/**
	 * Return a set of all identifiers associated with this family.
	 */
	public Set<String> associatedIdentifierSet(){
		return annotationSet;
	}		
		
}
