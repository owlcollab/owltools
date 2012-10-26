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
public class PANTHERTools {
	
	private final String treeName;
	private final String treeStr;
	private final String[] treeAnns;
	
	/**
	 * Create an instance for the given path
	 * 
	 * @param path
	 * @throws IOException 
	 */
	public PANTHERTools (File pFile) throws IOException {

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
	
//	/**
//	 * Return the complete OWL shunt graph repesentation of the phylogenic tree.
//	 */
//	public Map<String,> getOWLShuntGraph(){
//		
//		OWLShuntGraph g = new OWLShuntGraph();
//		
//		// Parse the Newick tree down to something usable.
//		NHXParser p = new NHXParser();
//		Phylogeny[] phys = null;
//
//	}		
		
//	/**
//	 * Wrapper method for the reasoner.
//	 * 
//	 * @param taxonClass
//	 * @param reflexive
//	 * @return set of super classes
//	 */
//	public Set<OWLClass> getAncestors(OWLClass taxonClass, boolean reflexive) {
//		if (taxonClass == null) {
//			return Collections.emptySet();
//		}
//		Set<OWLClass> result = new HashSet<OWLClass>();
//		Set<OWLClass> set = reasoner.getSuperClasses(taxonClass, false).getFlattened();
//		for (OWLClass cls : set) {
//			if (cls.isBuiltIn() == false) {
//				result.add(cls);
//			}
//		}
//		if (reflexive) {
//			result.add(taxonClass);
//		}
//		if (result.isEmpty()) {
//			return Collections.emptySet();
//		}
//		return result;
//		
//	}
//	
//	/**
//	 * Wrapper method for the reasoner
//	 * 
//	 * @param sources
//	 * @param reflexive
//	 * @return set of sub classes
//	 */
//	public Set<OWLClass> getDescendents(Set<OWLClass> sources, boolean reflexive) {
//		if (sources == null || sources.isEmpty()) {
//			return Collections.emptySet();
//		}
//		Set<OWLClass> result = new HashSet<OWLClass>();
//		for (OWLClass source : sources) {
//			Set<OWLClass> set = reasoner.getSubClasses(source, false).getFlattened();
//			for (OWLClass cls : set) {
//				if (cls.isBuiltIn() == false) {
//					result.add(cls);
//				}
//			}
//		}
//		if (reflexive) {
//			result.addAll(sources);
//		}
//		if (result.isEmpty()) {
//			return Collections.emptySet();
//		}
//		return result;
//	}
//	
//	/**
//	 * Clean up the internal data structures, usually done as last operation.
//	 */
//	public void dispose() {
//		mappingCache.clear();
//		if (disposeReasonerP) {
//			reasoner.dispose();
//		}
//	}

}
