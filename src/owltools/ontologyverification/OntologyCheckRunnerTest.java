package owltools.ontologyverification;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import owltools.graph.OWLGraphWrapper;
import owltools.ontologyverification.annotations.AfterLoading;
import owltools.ontologyverification.annotations.AfterMeriot;
import owltools.ontologyverification.annotations.AfterReasoning;
import owltools.ontologyverification.annotations.Check;
import owltools.test.OWLToolsTestBasics;

/**
 * Tests for {@link OntologyCheckRunner}.
 */
public class OntologyCheckRunnerTest extends OWLToolsTestBasics {

	static class OntologyCheckRunnerTestChecks {
		
		@AfterLoading
		@Check
		public CheckResult check1(OWLGraphWrapper owlGraphWrapper) {
			return null;
		}
		
		@AfterLoading
		@AfterMeriot
		@AfterReasoning
		@Check
		public CheckResult check2(OWLGraphWrapper owlGraphWrapper) {
			return null;
		}
		
		@Check
		public CheckResult check3(OWLGraphWrapper owlGraphWrapper) {
			return null;
		}
	}
	
	@Test
	public void testOntologyCheckRunner() throws Exception {
		OntologyCheckRunner runner = new OntologyCheckRunner(OntologyCheckRunnerTestChecks.class);
		assertNotNull(runner);
		OWLGraphWrapper g = null;
		List<CheckResult> results = runner.verify(g, AfterLoading.class);
		assertEquals(3, results.size());
	}


}
