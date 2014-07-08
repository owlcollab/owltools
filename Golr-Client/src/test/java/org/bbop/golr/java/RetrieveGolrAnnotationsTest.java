package org.bbop.golr.java;

import static org.junit.Assert.*;

import java.util.List;

import org.bbop.golr.java.RetrieveGolrAnnotations.GolrDocument;
import org.junit.Test;

public class RetrieveGolrAnnotationsTest {

	@Test
	public void testGetAnnotationsForGene() throws Exception {
		RetrieveGolrAnnotations.JSON_INDENT_FLAG = true;
		RetrieveGolrAnnotations retriever = new RetrieveGolrAnnotations("http://golr.berkeleybop.org");
		List<GolrDocument> annotations = retriever.getGolrAnnotationsForGene("MGI:MGI:97290");
		assertNotNull(annotations);
		for (GolrDocument document : annotations) {
			System.out.println(document.bioentity+"  "+document.annotation_class);
		}
		System.out.println(annotations.size());
		assertTrue(annotations.size() > 10);
	}

}
