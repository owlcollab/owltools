package owltools.gaf.lego.server.external;

import static org.junit.Assert.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.bbop.golr.java.RetrieveGolrBioentities;
import org.junit.Test;

import owltools.gaf.lego.server.external.ExternalLookupService.LookupEntry;

public class GolrExternalLookupServiceTest {

	@Test
	public void testLookupString() throws Exception {
		GolrExternalLookupService s = new GolrExternalLookupService("http://golr.berkeleybop.org");
		List<LookupEntry> lookup = s.lookup("SGD:S000004529");
		assertEquals(1, lookup.size());
		assertEquals("TEM1", lookup.get(0).label);
	}
	
	@Test
	public void testCachedGolrLookup() throws Exception {
		final List<URI> requests = new ArrayList<URI>();
		GolrExternalLookupService golr = new GolrExternalLookupService(new RetrieveGolrBioentities("http://golr.berkeleybop.org", 2){

			@Override
			protected void logRequest(URI uri) {
				requests.add(uri);
			}
			
		});
		ExternalLookupService s = new CachingExternalLookupService(golr, 1000);
		
		List<LookupEntry> lookup1 = s.lookup("SGD:S000004529");
		assertEquals(1, lookup1.size());
		assertEquals("TEM1", lookup1.get(0).label);
		int count = requests.size();
		
		List<LookupEntry> lookup2 = s.lookup("SGD:S000004529");
		assertEquals(1, lookup2.size());
		assertEquals("TEM1", lookup2.get(0).label);
		
		// there should be no new request to Golr, that's what the cache is for!
		assertEquals(count, requests.size());
	}

}
