package owltools.gaf.lego.server.external;

import static org.junit.Assert.*;

import org.junit.Test;

public class ProteinToolServiceTest {

	@Test
	public void test() throws Exception {
		ProteinToolService service = new ProteinToolService("src/test/resources/ontology/protein/subset");
		assertEquals(1, service.obsoleteIRIs.size());
		assertTrue(service.proteinEntryMap.size() > 16000);
	}

}
