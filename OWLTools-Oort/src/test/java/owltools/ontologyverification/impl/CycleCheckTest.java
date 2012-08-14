package owltools.ontologyverification.impl;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.List;

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
		assertEquals(1, warnings.size());
		CheckWarning warning = warnings.iterator().next();
		List<IRI> iris = warning.getIris();
		assertEquals(3, iris.size());
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
	
	@Test
	public void testNonCycle3() throws Exception {
		ParserWrapper parser = new ParserWrapper();
		IRI iri = IRI.create(getResource("verification/self_references.obo").getAbsoluteFile()) ;
		OWLGraphWrapper graph = parser.parseToOWLGraph(iri.toString());
		
		OntologyCheck check = new CycleCheck();
		
		Collection<CheckWarning> warnings = check.check(graph, graph.getAllOWLObjects());
		assertTrue(warnings.isEmpty());
	}
	
}
