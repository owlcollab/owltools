package owltools.gaf.test;

import static junit.framework.Assert.*;

import java.io.IOException;
import java.util.Set;

import org.junit.Test;

import owltools.gaf.GafDocument;
import owltools.gaf.GafObjectsBuilder;
import owltools.gaf.GeneAnnotation;
import owltools.test.OWLToolsTestBasics;

public class GafObjectsBuilderTest extends OWLToolsTestBasics {

	@Test
	public void testBuildDocument() throws IOException{
		GafObjectsBuilder builder = new GafObjectsBuilder();
		
		GafDocument doc = builder.buildDocument(getResource("test_gene_association_mgi.gaf"));
		
		assertNotNull(doc);
		
		assertFalse(doc.getBioentities().isEmpty());
		
		assertFalse(doc.getGeneAnnotations().isEmpty());
		
		Set<GeneAnnotation> anns = doc.getGeneAnnotations("MGI:MGI:1916529");
		for (GeneAnnotation ann : anns) {
			System.out.println("ANN: "+ann);
		}
		assertTrue(anns.size() == 3);
	}
	
	@Test
	public void testSplitBuildDocument() throws IOException{
		GafObjectsBuilder builder = new GafObjectsBuilder(50);
		
		GafDocument doc = builder.buildDocument(getResource("test_gene_association_mgi.gaf"));
		
		assertNotNull(doc);
		
		doc = builder.getNextSplitDocument();
		
		assertFalse(doc.getBioentities().isEmpty());
		
		assertFalse(doc.getGeneAnnotations().isEmpty());


	}
	
}
