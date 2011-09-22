package owltools.ontologyverification;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import owltools.graph.OWLGraphWrapper;
import owltools.ontologyverification.CheckResult.Status;
import owltools.ontologyverification.annotations.AfterLoading;
import owltools.ontologyverification.annotations.AfterMeriot;
import owltools.ontologyverification.annotations.AfterReasoning;
import owltools.ontologyverification.annotations.Check;

/**
 * Identify and run ontology checks for an ontology ({@link OWLGraphWrapper}).
 * The identification uses reflection and annotations to identify suitable 
 * ontology check methods from a list of classes.<br/>
 * The relevant annotations are {@link Check}, {@link AfterLoading}, {@link AfterMeriot}, 
 * and {@link AfterReasoning}. 
 * The {@link Check} annotation identify a methods as an ontology check. 
 * The rest indicate the stage for each check. If no stage is specified, 
 * default to {@link AfterLoading}. You can add multiple stages to each method.
 * 
 * Requirements for check classes and methods:
 * <ul>
 *  <li>Each check class needs an unparameterized constructor</li>
 *  <li>Check methods are public</li>
 *  <li>Check methods have one parameter: {@link OWLGraphWrapper}</li>
 *  <li>Check methods return a {@link CheckResult} or null</li>
 *  <li>Check methods need to be thread safe, state-less</li>
 *  <li>Check methods should not modify the ontology</li>
 * </ul> 
 */
public class OntologyCheckRunner {
	
	private final static Logger logger = Logger.getLogger(OntologyCheckRunner.class);
	
	private final Map<Method, Object> afterLoadingChecks;
	private final Map<Method, Object> afterMeriotChecks;
	private final Map<Method, Object> afterReasoningChecks;
	
	/**
	 * Create a new runner instance.
	 * 
	 * @param classes check classes
	 */
	OntologyCheckRunner(Class<?>...classes) {
		super();
		afterLoadingChecks = new HashMap<Method, Object>();
		afterMeriotChecks = new HashMap<Method, Object>();
		afterReasoningChecks = new HashMap<Method, Object>();
		Set<Class<?>> done = new HashSet<Class<?>>();
		for (Class<?> cls : classes) {
			if (cls == null) {
				throw new IllegalArgumentException("null is not a valid class");
			}
			if (done.add(cls)) {
				identifyChecks(cls);
			}
		}
	}
	
	/**
	 * Identify check methods in a class.
	 * 
	 * @param cls
	 */
	private void identifyChecks(Class<?> cls) {
		int count = 0;
		Object instance = createInstance(cls);
		for(Method method : cls.getMethods()) {
			if (method.getAnnotation(Check.class) != null) {
				checkMethodSignature(method, cls);
				boolean hasLoading = method.getAnnotation(AfterLoading.class) != null;
				if (hasLoading) {
					afterLoadingChecks.put(method, instance);
					count += 1;
				}
				boolean hasMeriot = method.getAnnotation(AfterMeriot.class) != null;
				if (hasMeriot) {
					afterMeriotChecks.put(method, instance);
					count += 1;
				}
				boolean hasReasoning = method.getAnnotation(AfterReasoning.class) != null;
				if (hasReasoning) {
					afterReasoningChecks.put(method, instance);
					count += 1;
				}
				if (!hasReasoning && !hasMeriot && !hasLoading) {
					// default: if not target annotation is provided execute after loading
					afterLoadingChecks.put(method, instance);
					count += 1;
				}
			}
		}
		if (count == 0) {
			throw new RuntimeException("No suitable check methods found in class: "+cls.getName());
		}
	}
	
	/**
	 * Check whether the method conforms to the expected methods 
	 * signature (parameter and return type).
	 * 
	 * @param method
	 * @param cls
	 */
	private void checkMethodSignature(Method method, Class<?> cls) {
		Class<?> returnType = method.getReturnType();
		if (!CheckResult.class.isAssignableFrom(returnType)) {
			throw new RuntimeException("The method: "+method.getName()+" in class: "+cls.getName()+" does not have the expected return type to be a check method: "+returnType.getName());
		}
		
		Class<?>[] parameterTypes = method.getParameterTypes();
		if (parameterTypes.length != 1) {
			throw new RuntimeException("The method: "+method.getName()+" in class: "+cls.getName()+" does not have the expected number of paramters: "+parameterTypes.length);
		}
		if (!OWLGraphWrapper.class.isAssignableFrom(parameterTypes[0])) {
			throw new RuntimeException("The method: "+method.getName()+" in class: "+cls.getName()+" does not have the expected parameter type to be a check method: "+parameterTypes[0].getName());
		}
	}
	
	/**
	 * Create an instance for a given class. 
	 * Assumes that a default constructor exists.
	 * 
	 * @param cls
	 * @return instance
	 */
	private Object createInstance(Class<?> cls) {
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
	 * @return list of check results
	 */
	List<CheckResult> verify (OWLGraphWrapper owlGraphWrapper, Class<?> annotation) {
		if (AfterLoading.class.equals(annotation)) {
			return verify(afterLoadingChecks, owlGraphWrapper, AfterLoading.class.getSimpleName());
		}
		if (AfterMeriot.class.equals(annotation)) {
			return verify(afterMeriotChecks, owlGraphWrapper, AfterMeriot.class.getSimpleName());
		}
		if (AfterReasoning.class.equals(annotation)) {
			return verify(afterReasoningChecks, owlGraphWrapper, AfterReasoning.class.getSimpleName());
		}
		throw new RuntimeException("Cannot call verify for unknown annotation: "+annotation);
	}
	
	/**
	 * Verify an ontology with a set of check methods.
	 * 
	 * @param checks the checks to execute
	 * @param owlGraphWrapper target ontology
	 * @param type string to distinguish between the different test stages
	 * @return list of check results
	 */
	private List<CheckResult> verify(Map<Method, Object> checks, OWLGraphWrapper owlGraphWrapper, String type) {
		List<CheckResult> results = new ArrayList<CheckResult>();
		for(Entry<Method, Object> entry : checks.entrySet()) {
			Method method = entry.getKey();
			Object object = entry.getValue();
			try {
				Object result = method.invoke(object, owlGraphWrapper);
				String checkName = method.getName()+"-"+type;
				if (result == null) {
					// assume success
					results.add(new CheckResult(checkName, Status.Success));
				}
				else {
					if (result instanceof CheckResult) {
						CheckResult checkResult = (CheckResult) result;
						checkResult.setCheckName(checkName);
						results.add(checkResult);
					}
					else {
						results.add(new CheckResult(checkName, Status.InternalError, "The check did not return the expected type"));
					}
				}
			} catch (IllegalArgumentException e) {
				logger.error("Could not execute check method: "+method.getName(), e);
				error(results, method.getName(), e);
			} catch (IllegalAccessException e) {
				logger.error("Could not execute check method: "+method.getName(), e);
				error(results, method.getName(), e);
			} catch (InvocationTargetException e) {
				logger.error("Could not execute check method: "+method.getName(), e);
				error(results, method.getName(), e);
			}
		}
		return results;
	}
	
	private void error(List<CheckResult> results, String checkName, Throwable e) {
		results.add(new CheckResult(checkName, Status.InternalError, "Internal error while exceuting the check: "+e.getMessage()));
	}
}
