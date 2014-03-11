package owltools.panther;


import static org.junit.Assert.*;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import owltools.graph.OWLGraphWrapper;
import owltools.graph.shunt.OWLShuntGraph;
import owltools.panther.PANTHERTree;

public class PANTHERTreeTest {

	private static Logger LOG = Logger.getLogger(OWLGraphWrapper.class);

	@Test
	public void testTreeloading() throws IOException{

		// Get the files we need.
		//File pFile = getResource("PTHR10000.orig.tree");
		File pFile = getResource("PTHR10000.arbre");
		PANTHERTree ptree = new PANTHERTree(pFile);
		
//		// Trivial
//		OWLShuntGraph g = ptree.getOWLShuntGraph();
//		assertNotNull(g);

		Set<String> aSet = ptree.associatedIdentifierSet();
			
		assertTrue("Contains A", aSet.contains("TAIR:At1g18640"));
		assertTrue("Contains B", aSet.contains("ENTREZ:3405244"));
		assertTrue("Contains C", aSet.contains("UniProtKB:Q4Q8D0"));
		assertTrue("Contains D", aSet.contains("NCBI:XP_001671160"));
		assertTrue("Contains E", aSet.contains("ZFIN:ZDB-GENE-050809-127"));
		assertTrue("Contains F", aSet.contains("ENSEMBL:AGAP012247"));
		assertFalse("Does not contain A", aSet.contains("TAIR:locus:2033535"));
		assertFalse("Does not contain B", aSet.contains("TAIR=locus=2043535"));
		assertFalse("Does not contain C", aSet.contains("ZFIN=ZDB-GENE-050809-127"));
		assertFalse("Does not contain D", aSet.contains(""));
		assertFalse("Does not contain E", aSet.contains("AN7"));
		assertFalse("Does not contain F", aSet.contains(":"));
		assertFalse("Does not contain G", aSet.contains("GO:0022008"));
		
		// DEBUG: Okay, now let's look at the generated graph a little.
		//OWLShuntGraph sg = ptree.getOWLShuntGraph();
		//String sg_json = sg.toJSON();
		//LOG.info(sg_json.toString());
	}
	
//	@Test
//	public void testTreeClosures() throws IOException{
//
//		// Get the files we need.
//		File pFile = getResource("PTHR31869.orig.tree");
//		PANTHERTree ptree = new PANTHERTree(pFile);
//			
//		// Descendnet closures at leaf.
//		Set<String> desc_an3 = ptree.getDescendants("PTHR31869:AN3");
//		assertEquals("Just self in descendents (size)", 1, desc_an3.size());
//		assertTrue("Just self in descendents", desc_an3.contains("PTHR31869:AN3"));
//
//		// Ancestor closures leaf.
//		Set<String> anc_an3 = ptree.getAncestors("PTHR31869:AN3");
//		assertTrue("A bunch of ancestors list", anc_an3.contains("PTHR31869:AN3"));
//		assertEquals("A bunch of ancestors size", 3, anc_an3.size());
//		
//		// TODO: More...but global cross-platform/cross-run stable IDs would be helpful.
//	}
//	
//	@Test
//	public void testAnnotationClosures() throws IOException{
//
//		// Get the files we need.
//		File pFile = getResource("PTHR31869.orig.tree");
//		PANTHERTree ptree = new PANTHERTree(pFile);
//
//		Set<String> aa = ptree.getAncestorAnnotations("UniProtKB:Q86KM0");
//		assertTrue("ancestor annotations contain self", aa.contains("UniProtKB:Q86KM0"));
//		
//		Set<String> ad = ptree.getAncestorAnnotations("UniProtKB:Q86KM0");
//		assertTrue("descendant annotations contain self", ad.contains("UniProtKB:Q86KM0"));
//		
//		// TODO: More...but global cross-platform/cross-run stable IDs would be helpful.
//		// Or actual manufactured examples for that matter.
//	}
	
	// A little helper from Chris stolen from somewhere else...
	protected static File getResource(String name) {
		assertNotNull(name);
		assertFalse(name.length() == 0);
		// TODO: Replace this with a mechanism not relying on the relative path.
		File file = new File("src/test/resources/" + name);
		assertTrue("Requested resource does not exists: "+file, file.exists());
		return file;
	}
}
