package org.bbop.golr.java;

import static org.junit.Assert.*;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.bbop.golr.java.RetrieveGolrAnnotations.GolrAnnotationDocument;
import org.junit.Ignore;
import org.junit.Test;

import owltools.gaf.Bioentity;
import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;

public class RetrieveGolrAnnotationsTest {

	@Test
	public void testGetGolrAnnotationsForGene() throws Exception {
		RetrieveGolrAnnotations retriever = new RetrieveGolrAnnotations("https://golr.geneontology.org/solr"){

			@Override
			protected void logRequest(URI uri) {
				System.out.println(uri);
			}
			
		};
		List<GolrAnnotationDocument> annotations = retriever.getGolrAnnotationsForGenes(
				Arrays.asList("MGI:MGI:97290"), true);
		assertNotNull(annotations);
		for (GolrAnnotationDocument document : annotations) {
			System.out.println(document.bioentity+"  "+document.annotation_class+"  "+document.evidence_type);
		}
		System.out.println(annotations.size());
		assertTrue(annotations.size() > 10);
	}
	
	@Test
	public void testGetGolrAnnotationsForGeneProduction() throws Exception {
		RetrieveGolrAnnotations retriever = new RetrieveGolrAnnotations("https://golr.geneontology.org/solr");
		List<GolrAnnotationDocument> annotations = retriever.getGolrAnnotationsForGene("MGI:MGI:97290");
		assertNotNull(annotations);
		for (GolrAnnotationDocument document : annotations) {
			System.out.println(document.bioentity+"  "+document.annotation_class);
		}
		System.out.println(annotations.size());
		assertTrue(annotations.size() > 10);
	}
	
	@Ignore
	@Test
	public void testGetGolrAnnotationsForGeneWithQualifierProduction() throws Exception {
		RetrieveGolrAnnotations retriever = new RetrieveGolrAnnotations("https://golr.geneontology.org/solr");
		List<GolrAnnotationDocument> annotations = retriever.getGolrAnnotationsForGene("SGD:S000003676");
		assertNotNull(annotations);
		int qualifierCounter = 0;
		for (GolrAnnotationDocument document : annotations) {
			System.out.println(document.bioentity+"  "+document.annotation_class);
			if (document.qualifier != null) {
				System.out.println(document.qualifier);
				qualifierCounter += 1;
			}
		}
		System.out.println(annotations.size());
		assertTrue(annotations.size() > 10);
		assertTrue(qualifierCounter > 0);
	}
	
	@Test
	public void testGetGolrAnnotationsForGeneWithQualifier() throws Exception {
		RetrieveGolrAnnotations retriever = new RetrieveGolrAnnotations("https://golr.geneontology.org/solr");
		List<GolrAnnotationDocument> annotations = retriever.getGolrAnnotationsForGene("UniProtKB:O95996");
		assertNotNull(annotations);
		int qualifierCounter = 0;
		for (GolrAnnotationDocument document : annotations) {
			System.out.println(document.bioentity+"  "+document.annotation_class);
			if (document.qualifier != null) {
				System.out.println(document.qualifier);
				qualifierCounter += 1;
			}
		}
		System.out.println(annotations.size());
		assertTrue(annotations.size() > 10);
		assertTrue(qualifierCounter > 0);
		
		GafDocument convert = retriever.convert(annotations);
		int qualifierCounterConverted = 0;
		for (GeneAnnotation ann : convert.getGeneAnnotations()) {
			int qualifiers = ann.getQualifiers();
			if (qualifiers != 0) {
				qualifierCounterConverted += 1;
			}
		}
		assertEquals(qualifierCounter, qualifierCounterConverted);
	}
	
	@Test
	public void testGetGolrAnnotationsForGenesProduction() throws Exception {
		RetrieveGolrAnnotations retriever = new RetrieveGolrAnnotations("https://golr.geneontology.org/solr") {

			@Override
			protected void logRequest(URI uri) {
				System.out.println(uri);
			}
			
		};
		List<GolrAnnotationDocument> annotations = retriever.getGolrAnnotationsForGenes(
				Arrays.asList("MGI:MGI:97290", "UniProtKB:Q0IIF6"));
		assertNotNull(annotations);
		for (GolrAnnotationDocument document : annotations) {
			System.out.println(document.bioentity+"  "+document.annotation_class);
		}
		System.out.println(annotations.size());
		assertTrue(annotations.size() > 10);
	}
	
	@Test
	public void testGetGolrAnnotationsForSynonym() throws Exception {
		RetrieveGolrAnnotations retriever = new RetrieveGolrAnnotations("https://golr.geneontology.org/solr") {

			@Override
			protected void logRequest(URI uri) {
				System.out.println(uri);
			}
			
		};
		List<GolrAnnotationDocument> annotations = retriever.getGolrAnnotationsForSynonym(
				"TAIR", Collections.singletonList("AT1G12520"), true);
		assertNotNull(annotations);
		for (GolrAnnotationDocument document : annotations) {
			System.out.println(document.bioentity+"  "+document.annotation_class+"  "+document.evidence_type);
		}
		System.out.println(annotations.size());
	}
	
	@Test
	public void testGetAnnotationsForGene() throws Exception {
		RetrieveGolrAnnotations retriever = new RetrieveGolrAnnotations("https://golr.geneontology.org/solr");
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
