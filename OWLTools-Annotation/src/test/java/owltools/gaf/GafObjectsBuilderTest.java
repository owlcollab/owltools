package owltools.gaf;

import static junit.framework.Assert.*;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import owltools.OWLToolsTestBasics;
import owltools.gaf.GafDocument;
import owltools.gaf.GafObjectsBuilder;
import owltools.gaf.GeneAnnotation;

public class GafObjectsBuilderTest extends OWLToolsTestBasics {

	@Test
	public void testBuildDocument() throws IOException{
		GafObjectsBuilder builder = new GafObjectsBuilder();
		
		GafDocument doc = builder.buildDocument(getResource("test_gene_association_mgi.gaf"));
		
		assertNotNull(doc);
		
		assertFalse(doc.getBioentities().isEmpty());
		
		assertFalse(doc.getGeneAnnotations().isEmpty());
		
		Set<GeneAnnotation> anns = doc.getGeneAnnotations("MGI:MGI:1916529");
		assertEquals(3, anns.size());
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
	
	@Test
	public void testCompoundWithExpression() throws Exception {
		GafObjectsBuilder builder = new GafObjectsBuilder();
		GafDocument doc = builder.buildDocument(getResource("compound_with_expression.gaf"));
		
		List<GeneAnnotation> geneAnnotations = doc.getGeneAnnotations();
		assertEquals(1, geneAnnotations.size());
		GeneAnnotation ann = geneAnnotations.get(0);
		String withExpression = ann.getWithExpression();
		assertTrue(withExpression.contains("|"));
		Collection<WithInfo> withInfos = ann.getWithInfos();
		assertTrue(withInfos.size() > 1);
	}
	
	@Test
	public void testGafLineFilters() throws Exception {
		GafObjectsBuilder builder = new GafObjectsBuilder();
		builder.addFilter(new GafLineFilter() {
			
			@Override
			public boolean accept(String line, int pos, GAFParser parser) {
				String evidence = parser.getEvidence();
				if ("IEA".equals(evidence)) {
					return false;
				}
				return true;
			}
		});
		
		GafDocument doc = builder.buildDocument(getResource("test_gene_association_mgi.gaf"));
		assertNotNull(doc);
		List<GeneAnnotation> annotations = doc.getGeneAnnotations();
		assertEquals(818, annotations.size());
		for (GeneAnnotation geneAnnotation : annotations) {
			String cls = geneAnnotation.getEvidenceCls().trim();
			assertNotSame("IEA", cls);
		}
	}
	
}
