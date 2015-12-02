package org.bbop.golr.java;

import static org.junit.Assert.*;

import java.net.URI;
import java.util.List;

import org.bbop.golr.java.RetrieveGolrOntologyClass.GolrOntologyClassDocument;
import org.junit.Test;

public class RetrieveGolrOntologyClassTest {

	@Test
	public void testGet() throws Exception {
		RetrieveGolrOntologyClass golr = new RetrieveGolrOntologyClass("http://golr.berkeleybop.org", 2){

			@Override
			protected void logRequest(URI uri) {
				System.out.println(uri);
			}
			
		};
		List<GolrOntologyClassDocument> entities = golr.getGolrOntologyCls("PO:0001040");
		assertEquals(1, entities.size());
	}
	
	@Test
	public void testGetProduction() throws Exception {
		RetrieveGolrOntologyClass golr = new RetrieveGolrOntologyClass("http://golr.geneontology.org/solr", 2){

			@Override
			protected void logRequest(URI uri) {
				System.out.println(uri);
			}
			
		};
		List<GolrOntologyClassDocument> entities = golr.getGolrOntologyCls("PO:0001040");
		assertEquals(1, entities.size());
	}
	
	/*
	 * WB:WBGene00001674
	 */
	@Test
	public void testGetGolrBioentitesNoctua() throws Exception {
		RetrieveGolrOntologyClass golr = new RetrieveGolrOntologyClass("http://noctua-golr.berkeleybop.org/", 2){

			@Override
			protected void logRequest(URI uri) {
				System.out.println(uri);
			}
			
		};
		List<GolrOntologyClassDocument> entities = golr.getGolrOntologyCls("WB:WBGene00001674");
		assertEquals(1, entities.size());
	}
}
