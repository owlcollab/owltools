package owltools.ontologyverification.impl;

import static org.junit.Assert.*;

import java.util.Collection;

import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;
import owltools.ontologyverification.CheckWarning;

public class AltIdInSignatureTest extends OWLToolsTestBasics  {

	@Test
	public void test() throws Exception {
		ParserWrapper parser = new ParserWrapper();
		IRI iri = IRI.create(getResource("verification/altid_in_signature.obo").getAbsoluteFile());
		OWLGraphWrapper graph = parser.parseToOWLGraph(iri.toString());
		
		AltIdInSignature check = new AltIdInSignature();
		
		Collection<CheckWarning> warnings = check.check(graph, graph.getAllOWLObjects());
		assertEquals(2, warnings.size());
		final IRI offendingIRI = IRI.create("http://purl.obolibrary.org/obo/FOO_0003");
		for (CheckWarning warning : warnings) {
			assertTrue(warning.getIris().contains(offendingIRI));
			assertFalse(warning.isFatal());
		}
	}

}
