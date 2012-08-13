package owltools.ontologyverification;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLObject;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;
import owltools.ontologyverification.annotations.AfterLoading;
import owltools.ontologyverification.annotations.AfterMireot;
import owltools.ontologyverification.annotations.AfterReasoning;
import owltools.ontologyverification.impl.AbstractCheck;

/**
 * Tests for {@link OntologyCheckRunner}.
 */
public class OntologyCheckRunnerTest extends OWLToolsTestBasics {

	@AfterLoading
	public static class OntologyCheckRunnerTestChecks1 extends AbstractCheck {

		public OntologyCheckRunnerTestChecks1() {
			super(null, null, false);
		}

		@Override
		public Collection<CheckWarning> check(OWLGraphWrapper graph, Collection<OWLObject> allOwlObjects) {
			return null;
		}
	}
	
	@AfterLoading
	@AfterMireot
	@AfterReasoning
	static class OntologyCheckRunnerTestChecks2 extends AbstractCheck {
		
		public OntologyCheckRunnerTestChecks2() {
			super(null, null, false);
		}
		
		@Override
		public Collection<CheckWarning> check(OWLGraphWrapper graph, Collection<OWLObject> allOwlObjects) {
			return null;
		}
		
	}
	
	// no annotation -> AfterLoading
	static class OntologyCheckRunnerTestChecks3 extends AbstractCheck {

		
		public OntologyCheckRunnerTestChecks3() {
			super(null, null, false);
		}
		
		@Override
		public Collection<CheckWarning> check(OWLGraphWrapper graph, Collection<OWLObject> allOwlObjects) {
			return null;
		}
	}
	
	@Test
	public void testOntologyCheckRunner() throws Exception {
		Collection<Class<? extends OntologyCheck>> checks = new ArrayList<Class<? extends OntologyCheck>>();
		checks.add(OntologyCheckRunnerTestChecks1.class);
		checks.add(OntologyCheckRunnerTestChecks2.class);
		checks.add(OntologyCheckRunnerTestChecks3.class);
		
		OntologyCheckRunner runner = new OntologyCheckRunner(checks);
		assertNotNull(runner);
		Map<OntologyCheck, Collection<CheckWarning>> results1 = runner.verify(null, null, AfterLoading.class);
		assertEquals(3, results1.size());
		
		Map<OntologyCheck, Collection<CheckWarning>> results2 = runner.verify(null, null, AfterMireot.class);
		assertEquals(1, results2.size());
		
		Map<OntologyCheck, Collection<CheckWarning>> results3 = runner.verify(null, null, AfterReasoning.class);
		assertEquals(1, results3.size());
	}


}
