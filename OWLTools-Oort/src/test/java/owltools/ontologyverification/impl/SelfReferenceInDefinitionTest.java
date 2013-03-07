package owltools.ontologyverification.impl;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Iterator;

import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;
import owltools.ontologyverification.CheckWarning;
import owltools.ontologyverification.OntologyCheck;

/**
 * Test for {@link SelfReferenceInDefinition}.
 */
public class SelfReferenceInDefinitionTest extends OWLToolsTestBasics {

	@Test
	public void testSelfReferences() throws Exception {
		ParserWrapper parser = new ParserWrapper();
		IRI iri = IRI.create(getResource("verification/self_references.obo").getAbsoluteFile()) ;
		OWLGraphWrapper graph = parser.parseToOWLGraph(iri.toString());
		
		OntologyCheck check = new SelfReferenceInDefinition();
		Collection<CheckWarning> warnings = check.check(graph, graph.getAllOWLObjects());
		assertEquals(2, warnings.size());
		final Iterator<CheckWarning> iterator = warnings.iterator();
		CheckWarning warning1 = iterator.next();
		assertTrue(warning1.getIris().contains(IRI.create("http://purl.obolibrary.org/obo/FOO_0004")));
		
		CheckWarning warning2 = iterator.next();
		assertTrue(warning2.getIris().contains(IRI.create("http://purl.obolibrary.org/obo/FOO_0006")));
	}

}
