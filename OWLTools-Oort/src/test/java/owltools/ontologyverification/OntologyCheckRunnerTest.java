package owltools.ontologyverification;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLObject;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;
import owltools.ontologyverification.OntologyCheckRunner.TimePoint;
import owltools.ontologyverification.impl.AbstractCheck;

/**
 * Tests for {@link OntologyCheckRunner}.
 */
public class OntologyCheckRunnerTest extends OWLToolsTestBasics {

	public static class OntologyCheckRunnerTestChecks extends AbstractCheck {

		public OntologyCheckRunnerTestChecks(TimePoint timePoint) {
			super(null, null, false, timePoint);
		}

		@Override
		public Collection<CheckWarning> check(OWLGraphWrapper graph, Collection<OWLObject> allOwlObjects) {
			return null;
		}
	}
	
	@Test
	public void testOntologyCheckRunner() throws Exception {
		Collection<OntologyCheck> checks = new ArrayList<OntologyCheck>();
		checks.add(new OntologyCheckRunnerTestChecks(TimePoint.AfterLoad));
		checks.add(new OntologyCheckRunnerTestChecks(null));
		
		checks.add(new OntologyCheckRunnerTestChecks(TimePoint.AfterMireot));
		
		checks.add(new OntologyCheckRunnerTestChecks(TimePoint.AfterReasoning));
		
		OntologyCheckRunner runner = new OntologyCheckRunner(checks);
		assertNotNull(runner);
		Map<OntologyCheck, Collection<CheckWarning>> results1 = runner.verify(null, null, TimePoint.AfterLoad);
		assertEquals(2, results1.size());
		
		Map<OntologyCheck, Collection<CheckWarning>> results2 = runner.verify(null, null, TimePoint.AfterMireot);
		assertEquals(1, results2.size());
		
		Map<OntologyCheck, Collection<CheckWarning>> results3 = runner.verify(null, null, TimePoint.AfterReasoning);
		assertEquals(1, results3.size());
	}


}
