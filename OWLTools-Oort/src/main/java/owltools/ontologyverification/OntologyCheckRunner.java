package owltools.ontologyverification;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphWrapper;
import owltools.ontologyverification.annotations.AfterLoading;
import owltools.ontologyverification.annotations.AfterMireot;
import owltools.ontologyverification.annotations.AfterReasoning;

/**
 * Identify and run ontology checks for an ontology ({@link OWLGraphWrapper})
 * via the {@link OntologyCheck} interface. The runner uses annotations to
 * identify the time point when to run the ontology check methods during the
 * release process.<br/>
 * The relevant annotations are {@link AfterLoading}, {@link AfterMireot}, and
 * {@link AfterReasoning}. They indicate the stage for each check. If no stage
 * is specified, default to {@link AfterLoading}. You can add multiple stages to
 * each method.
 */
public class OntologyCheckRunner {
	
	private final Set<OntologyCheck> afterLoadingChecks;
	private final Set<OntologyCheck> afterMireotChecks;
	private final Set<OntologyCheck> afterReasoningChecks;
	
	/**
	 * Create a new runner instance.
	 * 
	 * @param classes check classes
	 */
	OntologyCheckRunner(Collection<Class<? extends OntologyCheck>> classes) {
		super();
		afterLoadingChecks = new HashSet<OntologyCheck>();
		afterMireotChecks = new HashSet<OntologyCheck>();
		afterReasoningChecks = new HashSet<OntologyCheck>();
		Set<Class<?>> done = new HashSet<Class<?>>();
		for (Class<? extends OntologyCheck> cls : classes) {
			if (cls == null) {
				throw new IllegalArgumentException("null is not a valid class");
			}
			if (done.add(cls)) {
				identifyChecks(cls);
			}
		}
	}
	
	/**
	 * Identify when to run a check. Using {@link AfterLoading},
	 * {@link AfterMireot}, and {@link AfterReasoning} as markers.
	 * 
	 * @param cls
	 */
	private void identifyChecks(Class<? extends OntologyCheck> cls) {
		OntologyCheck instance = createInstance(cls);
		
		boolean hasLoading = cls.getAnnotation(AfterLoading.class) != null;
		if (hasLoading) {
			afterLoadingChecks.add(instance);
		}
		boolean hasMireot = cls.getAnnotation(AfterMireot.class) != null;
		if (hasMireot) {
			afterMireotChecks.add(instance);
		}
		boolean hasReasoning = cls.getAnnotation(AfterReasoning.class) != null;
		if (hasReasoning) {
			afterReasoningChecks.add(instance);
		}
		if (!hasReasoning && !hasMireot && !hasLoading) {
			// default: if not target annotation is provided execute after loading
			afterLoadingChecks.add(instance);
		}
	}
	
	/**
	 * Create an instance for a given class. 
	 * Assumes that a default constructor exists.
	 * 
	 * @param cls
	 * @return instance
	 */
	private <T> T createInstance(Class<T> cls) {
		try {
			return cls.newInstance();
		} catch (InstantiationException e) {
			throw new RuntimeException("Could not create instance for class: "+cls.getName(), e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Could not create instance for class: "+cls.getName(), e);
		}
	}

	/**
	 * Run checks for an ontology and given stage
	 * 
	 * @param owlGraphWrapper target ontology
	 * @param annotation stage annotation class
	 * @return map of checks and warnings
	 */
	Map<OntologyCheck, Collection<CheckWarning>> verify (OWLGraphWrapper owlGraphWrapper, Class<?> annotation) {
		Set<OWLObject> allOWLObjects = owlGraphWrapper.getAllOWLObjects();
		return verify(owlGraphWrapper, allOWLObjects, annotation);
	}
	
	/**
	 * Run checks for an ontology and given stage
	 * 
	 * @param owlGraphWrapper target ontology
	 * @param allOWLObjects all {@link OWLObject} collection
	 * @param annotation stage annotation class
	 * @return map of checks and warnings
	 */
	Map<OntologyCheck, Collection<CheckWarning>> verify (OWLGraphWrapper owlGraphWrapper, Set<OWLObject> allOWLObjects, Class<?> annotation) {
		if (AfterLoading.class.equals(annotation)) {
			return verify(afterLoadingChecks, owlGraphWrapper, allOWLObjects, AfterLoading.class.getSimpleName());
		}
		if (AfterMireot.class.equals(annotation)) {
			return verify(afterMireotChecks, owlGraphWrapper, allOWLObjects, AfterMireot.class.getSimpleName());
		}
		if (AfterReasoning.class.equals(annotation)) {
			return verify(afterReasoningChecks, owlGraphWrapper, allOWLObjects, AfterReasoning.class.getSimpleName());
		}
		throw new RuntimeException("Cannot call verify for unknown annotation: "+annotation);
	}
	
	/**
	 * Verify an ontology with a set of check methods.
	 * 
	 * @param checks the checks to execute
	 * @param owlGraphWrapper target ontology
	 * @param allOWLObjects all owl objects in the graph wrapper
	 * @param type string to distinguish between the different test stages
	 * @return map of checks and warnings
	 */
	private Map<OntologyCheck, Collection<CheckWarning>> verify(Set<OntologyCheck> checks, OWLGraphWrapper owlGraphWrapper, Set<OWLObject> allOWLObjects, String type) {
		Map<OntologyCheck, Collection<CheckWarning>> results = new HashMap<OntologyCheck, Collection<CheckWarning>>();
		for(OntologyCheck check : checks) {
			Collection<CheckWarning> warnings = check.check(owlGraphWrapper, allOWLObjects);
			results.put(check, warnings);
		}
		return results;
	}
	
}
