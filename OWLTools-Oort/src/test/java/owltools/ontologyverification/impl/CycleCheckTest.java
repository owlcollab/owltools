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
 * Tests for {@link CycleCheck}.
 */
public class CycleCheckTest extends OWLToolsTestBasics {

	@Test
	public void testCycle1() throws Exception {
		ParserWrapper parser = new ParserWrapper();
		IRI iri = IRI.create(getResource("verification/cycle.obo").getAbsoluteFile()) ;
		OWLGraphWrapper graph = parser.parseToOWLGraph(iri.toString());
		
		OntologyCheck check = new CycleCheck();
		
		Collection<CheckWarning> warnings = check.check(graph, graph.getAllOWLObjects());
		assertEquals(3, warnings.size());
	}
	
	@Test
	public void testCycle2() throws Exception {
		ParserWrapper parser = new ParserWrapper();
		IRI iri = IRI.create(getResource("verification/self_references.obo").getAbsoluteFile()) ;
		OWLGraphWrapper graph = parser.parseToOWLGraph(iri.toString());
		
		OntologyCheck check = new CycleCheck();
		
		Collection<CheckWarning> warnings = check.check(graph, graph.getAllOWLObjects());
		assertEquals(1, warnings.size());
		CheckWarning warning = warnings.iterator().next();
		assertTrue(warning.getIris().contains(IRI.create("http://purl.obolibrary.org/obo/FOO_0004")));
	}
	
	@Test
	public void testNonCycle1() throws Exception {
		ParserWrapper parser = new ParserWrapper();
		IRI iri = IRI.create(getResource("verification/dangling_references.obo").getAbsoluteFile()) ;
		OWLGraphWrapper graph = parser.parseToOWLGraph(iri.toString());
		
		OntologyCheck check = new CycleCheck();
		
		Collection<CheckWarning> warnings = check.check(graph, graph.getAllOWLObjects());
		assertTrue(warnings.isEmpty());
	}
	
	@Test
	public void testNonCycle2() throws Exception {
		ParserWrapper parser = new ParserWrapper();
		IRI iri = IRI.create(getResource("verification/name_redundancy.obo").getAbsoluteFile()) ;
		OWLGraphWrapper graph = parser.parseToOWLGraph(iri.toString());
		
		OntologyCheck check = new CycleCheck();
		
		Collection<CheckWarning> warnings = check.check(graph, graph.getAllOWLObjects());
		assertTrue(warnings.isEmpty());
	}
	
}
