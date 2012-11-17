package owltools.panther;


import static junit.framework.Assert.*;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import owltools.graph.shunt.OWLShuntGraph;
import owltools.panther.PANTHERForest;

public class PANTHERForestTest {

	@Test
	public void testSetloading() throws IOException{

		// Get the files we need.
		File pDir = getResource(".");
		FileFilter pFileFilter = new WildcardFileFilter("PTHR*.tree");
		List<File> pTreeFiles = new ArrayList<File>(Arrays.asList(pDir.listFiles(pFileFilter))); // i hate java
		File tcFile = getResource("PANTHER7.2_HMM_classifications");
		
		PANTHERForest pSet = new PANTHERForest(tcFile, pTreeFiles);
		
		// Trivial
		assertNotNull("Hope so", pSet);
		assertEquals("We should have two trees", 3, pSet.getNumberOfFilesInSet());

		// Contents.
		assertNull("Not in (A)", pSet.getAssociatedTrees(""));
		assertNull("Not in (B)", pSet.getAssociatedTrees(null));
		assertNull("Not in (C)", pSet.getAssociatedTrees("GO:0022008"));
		assertEquals("Has in (A)", "PTHR10000",
				pSet.getAssociatedTrees("UniProtKB:Q4Q8D0").iterator().next().getTreeLabel());
		assertEquals("Has in (B)", "PTHR24041",
				pSet.getAssociatedTrees("ENSEMBL:ENSG00000169894").iterator().next().getTreeLabel());
	}
	
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
