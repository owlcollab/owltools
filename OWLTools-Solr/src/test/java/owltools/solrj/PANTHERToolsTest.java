package owltools.solrj;


import static junit.framework.Assert.*;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import owltools.graph.shunt.OWLShuntGraph;

public class PANTHERToolsTest {

	@Test
	public void testTreeloading() throws IOException{

		// Get the files we need.
		File pDir = getResource(".");
		FileFilter pFileFilter = new WildcardFileFilter("PTHR*.tree");
		File[] pFiles = pDir.listFiles(pFileFilter);
		for( File pFile : pFiles ){
			System.err.println("Processing PANTHER tree: " + pFile.getAbsolutePath());
			PANTHERTools ptool = new PANTHERTools(pFile);

			System.err.println(ptool.getNHXString());
			System.err.println(ptool.getTreeName());

			// Trivial
			OWLShuntGraph g = ptool.getOWLShuntGraph();
			assertNotNull(g);

			System.err.println(g.toJSON());
//			assertEquals("At least a string",
//					g.toJSON().getClass().equals(String.class));
		}
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
