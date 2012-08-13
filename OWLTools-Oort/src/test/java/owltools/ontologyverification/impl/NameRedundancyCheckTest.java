package owltools.ontologyverification.impl;

import static org.junit.Assert.*;

import java.util.Collection;

import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;
import owltools.ontologyverification.CheckWarning;
import owltools.ontologyverification.OntologyCheck;

/**
 * Tests for {@link NameRedundancyCheck}.
 */
public class NameRedundancyCheckTest extends OWLToolsTestBasics {

	@Test
	public void testNameRedundancy() throws Exception {
		ParserWrapper parser = new ParserWrapper();
		IRI iri = IRI.create(getResource("verification/name_redundancy.obo").getAbsoluteFile()) ;
		OWLGraphWrapper graph = parser.parseToOWLGraph(iri.toString());
		
		OntologyCheck check = new NameRedundancyCheck();
		
		Collection<CheckWarning> warnings = check.check(graph, graph.getAllOWLObjects());
		assertEquals(2, warnings.size());
	}

}
