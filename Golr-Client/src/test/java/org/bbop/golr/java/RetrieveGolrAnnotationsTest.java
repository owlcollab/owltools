package org.bbop.golr.java;

import static org.junit.Assert.*;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.bbop.golr.java.RetrieveGolrAnnotations.GolrAnnotationDocument;
import org.junit.Test;

import owltools.gaf.Bioentity;
import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;

public class RetrieveGolrAnnotationsTest {

	@Test
	public void testGetGolrAnnotationsForGene() throws Exception {
		RetrieveGolrAnnotations retriever = new RetrieveGolrAnnotations("http://golr.berkeleybop.org");
		List<GolrAnnotationDocument> annotations = retriever.getGolrAnnotationsForGene("MGI:MGI:97290");
		assertNotNull(annotations);
		for (GolrAnnotationDocument document : annotations) {
			System.out.println(document.bioentity+"  "+document.annotation_class);
		}
		System.out.println(annotations.size());
		assertTrue(annotations.size() > 10);
	}
	
	@Test
	public void testGetGolrAnnotationsForGeneProduction() throws Exception {
		RetrieveGolrAnnotations retriever = new RetrieveGolrAnnotations("http://golr.geneontology.org/solr");
		List<GolrAnnotationDocument> annotations = retriever.getGolrAnnotationsForGene("MGI:MGI:97290");
		assertNotNull(annotations);
		for (GolrAnnotationDocument document : annotations) {
			System.out.println(document.bioentity+"  "+document.annotation_class);
		}
		System.out.println(annotations.size());
		assertTrue(annotations.size() > 10);
	}
	
	@Test
	public void testGetGolrAnnotationsForGenesProduction() throws Exception {
		RetrieveGolrAnnotations retriever = new RetrieveGolrAnnotations("http://golr.geneontology.org/solr") {

			@Override
			protected void logRequest(URI uri) {
				System.out.println(uri);
			}
			
		};
		List<GolrAnnotationDocument> annotations = retriever.getGolrAnnotationsForGenes(Arrays.asList("MGI:MGI:97290", "UniProtKB:Q0IIF6"));
		assertNotNull(annotations);
		for (GolrAnnotationDocument document : annotations) {
			System.out.println(document.bioentity+"  "+document.annotation_class);
		}
		System.out.println(annotations.size());
		assertTrue(annotations.size() > 10);
	}
	
	@Test
	public void testGetGolrAnnotationsForSynonym() throws Exception {
		RetrieveGolrAnnotations retriever = new RetrieveGolrAnnotations("http://golr.geneontology.org/solr") {

			@Override
			protected void logRequest(URI uri) {
				System.out.println(uri);
			}
			
		};
		List<GolrAnnotationDocument> annotations = retriever.getGolrAnnotationsForSynonym("TAIR", "AT1G12520");
		assertNotNull(annotations);
		for (GolrAnnotationDocument document : annotations) {
			System.out.println(document.bioentity+"  "+document.annotation_class);
		}
		System.out.println(annotations.size());
	}
	
	@Test
	public void testGetAnnotationsForGene() throws Exception {
		RetrieveGolrAnnotations retriever = new RetrieveGolrAnnotations("http://golr.berkeleybop.org");
		List<GolrAnnotationDocument> golrDocuments = retriever.getGolrAnnotationsForGene("MGI:MGI:97290");
		assertNotNull(golrDocuments);
		GafDocument gafDocument = retriever.convert(golrDocuments);
		Collection<Bioentity> bioentities = gafDocument.getBioentities();
		List<GeneAnnotation> annotations = gafDocument.getGeneAnnotations();
		for (GeneAnnotation annotation : annotations) {
			System.out.println(annotation);
		}
		assertTrue(golrDocuments.size() > 10);
		assertEquals(1, bioentities.size());
	}

}
