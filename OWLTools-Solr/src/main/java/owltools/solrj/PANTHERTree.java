package owltools.solrj;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
import owltools.graph.shunt.OWLShuntNode;
import owltools.io.ParserWrapper;

/**
 * Methods to simplify the work with the Newick-ish output we get from PANTHER. 
 */
public class PANTHERTree {
	
	private static final Logger LOG = Logger.getLogger(PANTHERTree.class);

	private final String treeName;
	private final String treeStr;
	private final String[] treeAnns;
	private final Set<String> groupSet;

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
		
		groupSet = new HashSet<String>();
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
	 * Return the complete OWL shunt graph repesentation of the phylogenic tree.
	 */
	public OWLShuntGraph getOWLShuntGraph(){
		
		OWLShuntGraph g = new OWLShuntGraph();
		
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
				int nid = pNode.getId();
				String id_str = Integer.toString(nid);
				String lbl = pNode.getName();
				OWLShuntNode n = new OWLShuntNode(id_str);
				n.setLabel(lbl);
				g.addNode(n);
				
				// Next, gather the edge information and add it to the graph.
				List<PhylogenyNode> children = pNode.getDescendants();
				for( PhylogenyNode kid : children ){
					int enid = kid.getId();
					String enid_str = Integer.toString(enid);
					OWLShuntEdge e = new OWLShuntEdge(id_str, enid_str);
					e.setMetadata(Double.toString(kid.getDistanceToParent()));
					g.addEdge(e);
				}
			}
		}
		
		return g;
	}
	
	/**
	 * Return a globally "unique" identifier for an internal ID. 
	 */
	public String uuidInternal(String str){
		return treeName + ":" + str;
	}
	
	/**
	 * Return a Set of 
	 */
	private void readyAnnotationDataCache(){
		
		//Map<String,String> idToInit = new HashMap<String,String>();
		//Map<String,Set<String>> initToId = new HashMap<String,Set<String>>();
		
		// Parse down every annotation line.
		for( String aLine : treeAnns ){
			
			// First, get rid of the terminal semicolon.
			String cleanALine = StringUtils.chop(aLine);
			
			// Split out the sections.
			String[] sections = StringUtils.split(cleanALine, "|");
			if( sections.length != 3 ) throw new Error("Expected three sections in " + treeName);

			// // Isolate the initial internal identifier.
			// String initSection = sections[0];
			// String initID = StringUtils.substringBefore(initSection, ":");

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
			groupSet.add(oneID);
			groupSet.add(twoID);
			//LOG.info("groupSet in: " + oneID);
			//LOG.info("groupSet in: " + twoID);
		}
		
	}		

	/**
	 * Return a set of all identifiers associated with this family.
	 */
	public Set<String> associatedIdentifierSet(){
		return groupSet;
	}		
		
}
