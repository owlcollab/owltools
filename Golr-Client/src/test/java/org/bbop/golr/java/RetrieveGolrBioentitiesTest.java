package org.bbop.golr.java;

import static org.junit.Assert.*;

import java.net.URI;
import java.util.List;

import org.bbop.golr.java.RetrieveGolrBioentities.GolrBioentityDocument;
import org.junit.Test;

public class RetrieveGolrBioentitiesTest {

	@Test
	public void testGetGolrBioentites() throws Exception {
		RetrieveGolrBioentities golr = new RetrieveGolrBioentities("http://golr.berkeleybop.org", 2){

			@Override
			protected void logRequest(URI uri) {
				System.out.println(uri);
			}
			
		};
		List<GolrBioentityDocument> entities = golr.getGolrBioentites("SGD:S000004529");
		assertEquals(1, entities.size());
	}
	
	@Test
	public void testGetGolrBioentitesProduction() throws Exception {
		RetrieveGolrBioentities golr = new RetrieveGolrBioentities("http://golr.geneontology.org/solr", 2){

			@Override
			protected void logRequest(URI uri) {
				System.out.println(uri);
			}
			
		};
		List<GolrBioentityDocument> entities = golr.getGolrBioentites("SGD:S000004529");
		assertEquals(1, entities.size());
		GolrBioentityDocument document = entities.get(0);
		assertNotNull(document.taxon);
	}
	
	/*
	 * WB:WBGene00001674
	 */
	@Test
	public void testGetGolrBioentitesNoctua() throws Exception {
		RetrieveGolrBioentities golr = new RetrieveGolrBioentities("http://noctua-golr.berkeleybop.org/", 2){

			@Override
			protected void logRequest(URI uri) {
				System.out.println(uri);
			}
			
		};
		List<GolrBioentityDocument> entities = golr.getGolrBioentites("WB:WBGene00001674");
		// This instance GOLR does not contain bio-entities, but only classes
		assertEquals(0, entities.size());
	}
}
