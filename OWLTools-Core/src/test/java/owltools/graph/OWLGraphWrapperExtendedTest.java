package owltools.graph;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.semanticweb.owlapi.model.IRI;

import owltools.OWLToolsTestBasics;

public class OWLGraphWrapperExtendedTest  extends OWLToolsTestBasics {
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testGetIRIByIdentifier() throws Exception {
		// Load test ontology (It's a part of go-gaf.owl)
		OWLGraphWrapper wrapper = getGraph("graph/sub-go-gaf-with-dummy-prop.owl");

		// case 1: there is only one matching BFO/RO property.
		IRI iri1 = wrapper.getIRIByIdentifier("results_in_formation_of", false);
		assertEquals("results_in_formation_of is supposed to be mapped to RO_0002297 but it is not.", iri1.toString(), "http://purl.obolibrary.org/obo/RO_0002297");

		// case 2: there is only one matching property (non-BFO/RO).
		IRI iri2 = wrapper.getIRIByIdentifier("dummy1", false);
		assertEquals("dummy1 is supposed to be mapped to so#dummy1 but it is not.", iri2.toString(), "http://purl.obolibrary.org/obo/so#dummy1");

		// case 3: there are multiple matching properties (one is BFO/RO and another is SO).
		IRI iri3 = wrapper.getIRIByIdentifier("part_of", false);
		assertEquals("part_of is supposed to be mapped to BFO_0000050 but it is not.", iri3.toString(), "http://purl.obolibrary.org/obo/BFO_0000050");

		// case 4: there are multiple matching properties (everyones are non BFO/RO).
		thrown.expect(RuntimeException.class);
		thrown.expectMessage("Multiple candidate IRIs are found for id: dummy2. None of them are from BFO or RO.");
		wrapper.getIRIByIdentifier("dummy2", false);
	}

	@Test
	public void testGetIdentifier() throws Exception {
		OWLGraphWrapper wrapper = getGraph("graph/dummy-ontology.owl");

		String id1 = wrapper.getIdentifier(IRI.create("http://purl.obolibrary.org/obo/GO_0000001"));
		String id2 = wrapper.getIdentifier(IRI.create("http://purl.obolibrary.org/obo/GO_0000002"));
		String id3 = wrapper.getIdentifier(IRI.create("http://example.com/X_005"));
		String id4 = wrapper.getIdentifier(IRI.create("http://example.com/X_010"));
		String id5 = wrapper.getIdentifier(IRI.create("http://purl.obolibrary.org/obo/GR_protein_Q6K4D1"));

		assertEquals(id1, "GO:0000001");
		assertEquals(id2, "GO:0000002");
		assertEquals(id3, "X:5");
		assertEquals(id4, "http://example.com/X_010");
	}
}