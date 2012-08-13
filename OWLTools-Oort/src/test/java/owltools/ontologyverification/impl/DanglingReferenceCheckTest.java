package owltools.ontologyverification.impl;

import static org.junit.Assert.*;

import java.util.Collection;

import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;
import owltools.ontologyverification.CheckWarning;

/**
 * Test for {@link DanglingReferenceCheck}.
 */
public class DanglingReferenceCheckTest extends OWLToolsTestBasics {

	@Test
	public void testDanglingReference() throws Exception {
		ParserWrapper parser = new ParserWrapper();
		IRI iri = IRI.create(getResource("verification/dangling_references.obo").getAbsoluteFile()) ;
		OWLGraphWrapper graph = parser.parseToOWLGraph(iri.toString());
		
		DanglingReferenceCheck check = new DanglingReferenceCheck();
		Collection<CheckWarning> warnings = check.check(graph, graph.getAllOWLObjects());
		assertEquals(1, warnings.size());
		CheckWarning warning = warnings.iterator().next();
		assertTrue(warning.getIris().contains(IRI.create("http://purl.obolibrary.org/obo/BAR_0001")));
	}

}
