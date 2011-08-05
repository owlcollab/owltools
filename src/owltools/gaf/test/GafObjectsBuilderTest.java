package owltools.gaf.test;

import java.io.File;
import java.io.IOException;

import owltools.gaf.GafDocument;
import owltools.gaf.GafObjectsBuilder;
import junit.framework.TestCase;

public class GafObjectsBuilderTest extends TestCase {

	public static void testBuildDocument() throws IOException{
		GafObjectsBuilder builder = new GafObjectsBuilder();
		
		GafDocument doc = builder.buildDocument(new File("test_resources/test_gene_association_mgi.gaf"));
		
		assertNotNull(doc);
		
		assertFalse(doc.getBioentities().isEmpty());
		
		assertFalse(doc.getGeneAnnotations().isEmpty());
	}
	
	public static void testSplitBuildDocument() throws IOException{
		GafObjectsBuilder builder = new GafObjectsBuilder(50);
		
		GafDocument doc = builder.buildDocument(new File("test_resources/test_gene_association_mgi.gaf"));
		
		assertNotNull(doc);
		
		doc = builder.getNextSplitDocument();
		
		assertFalse(doc.getBioentities().isEmpty());
		
		assertFalse(doc.getGeneAnnotations().isEmpty());
		
	}
	
}
