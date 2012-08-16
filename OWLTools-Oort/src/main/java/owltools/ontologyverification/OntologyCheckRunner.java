package owltools.ontologyverification;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphWrapper;

/**
 * Identify and run ontology checks for an ontology ({@link OWLGraphWrapper})
 * via the {@link OntologyCheck} interface. The runner uses annotations to
 * identify the time point ({@link TimePoint}) when to run the ontology check
 * methods during the release process.
 */
public class OntologyCheckRunner {
	
	public static enum TimePoint {
		AfterLoad,
		AfterMireot,
		AfterReasoning
	}
	
	private final Map<TimePoint, Set<OntologyCheck>> allChecks;
	
	/**
	 * Create a new runner instance.
	 * 
	 * @param checks
	 */
	OntologyCheckRunner(Collection<OntologyCheck> checks) {
		super();
		allChecks = new HashMap<TimePoint, Set<OntologyCheck>>();
		// init map
		for (TimePoint timePoint : TimePoint.values()) {
			allChecks.put(timePoint, new HashSet<OntologyCheck>());
		}
		
		Set<OntologyCheck> done = new HashSet<OntologyCheck>();
		for (OntologyCheck check : checks) {
			if (check == null) {
				throw new IllegalArgumentException("null is not a valid check");
			}
			if (done.add(check)) {
				identifyChecks(check);
			}
		}
	}
	
	private void identifyChecks(OntologyCheck check) {
		TimePoint timePoint = check.getTimePoint();
		if (timePoint == null) {
			// default: if no time point is provided execute after loading
			timePoint = TimePoint.AfterLoad;
		}
		Set<OntologyCheck> checks = allChecks.get(timePoint);
		checks.add(check);
	}
	
	/**
	 * Run checks for an ontology and given {@link TimePoint}.
	 * 
	 * @param graph target ontology
	 * @param timePoint 
	 * @return map of checks and warnings
	 */
	Map<OntologyCheck, Collection<CheckWarning>> verify (OWLGraphWrapper graph, TimePoint timePoint) {
		Set<OWLObject> allOWLObjects = graph.getAllOWLObjects();
		return verify(graph, allOWLObjects, timePoint);
	}
	
	/**
	 * Run checks for an ontology and given {@link TimePoint}.
	 * 
	 * @param graph target ontology
	 * @param allOWLObjects all {@link OWLObject} collection
	 * @param timePoint
	 * @return map of checks and warnings
	 */
	Map<OntologyCheck, Collection<CheckWarning>> verify (OWLGraphWrapper graph, Set<OWLObject> allOWLObjects, TimePoint timePoint) {
		return verify(allChecks.get(timePoint), graph, allOWLObjects);
	}
	
	/**
	 * Verify an ontology with a set of check methods.
	 * 
	 * @param checks the checks to execute
	 * @param owlGraphWrapper target ontology
	 * @param allOWLObjects all owl objects in the graph wrapper
	 * @return map of checks and warnings
	 */
	private Map<OntologyCheck, Collection<CheckWarning>> verify(Set<OntologyCheck> checks, OWLGraphWrapper owlGraphWrapper, Set<OWLObject> allOWLObjects) {
		Map<OntologyCheck, Collection<CheckWarning>> results = new HashMap<OntologyCheck, Collection<CheckWarning>>();
		for(OntologyCheck check : checks) {
			Collection<CheckWarning> warnings = check.check(owlGraphWrapper, allOWLObjects);
			results.put(check, warnings);
		}
		return results;
	}
	
}
