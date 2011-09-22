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

public class OntologyCheckRunner {
	
	private final static Logger logger = Logger.getLogger(OntologyCheckRunner.class);
	
	private final Map<Method, Object> afterLoadingChecks;
	private final Map<Method, Object> afterMeriotChecks;
	private final Map<Method, Object> afterReasoningChecks;
	
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
	
	private void identifyChecks(Class<?> cls) {
		int count = 0;
		Object instance = createInstance(cls);
		for(Method method : cls.getMethods()) {
			if (method.getAnnotation(Check.class) != null) {
				checkMethodSignature(method, cls);
				if (method.getAnnotation(AfterLoading.class) != null) {
					afterLoadingChecks.put(method, instance);
					count += 1;
				}
				if (method.getAnnotation(AfterMeriot.class) != null) {
					afterMeriotChecks.put(method, instance);
					count += 1;
				}
				if (method.getAnnotation(AfterReasoning.class) != null) {
					afterReasoningChecks.put(method, instance);
					count += 1;
				}
			}
		}
		if (count == 0) {
			throw new RuntimeException("No suitable check methods found in class: "+cls.getName());
		}
	}
	
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
	
	private Object createInstance(Class<?> cls) {
		try {
			return cls.newInstance();
		} catch (InstantiationException e) {
			throw new RuntimeException("Could not create instance for class: "+cls.getName(), e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Could not create instance for class: "+cls.getName(), e);
		}
	}

	List<CheckResult> verify (OWLGraphWrapper owlGraphWrapper, Class<?> annotation) {
		if (AfterLoading.class.equals(annotation)) {
			return afterLoading(owlGraphWrapper);
		}
		if (AfterMeriot.class.equals(annotation)) {
			return afterMeriot(owlGraphWrapper);
		}
		if (AfterReasoning.class.equals(annotation)) {
			return afterReasoning(owlGraphWrapper);
		}
		throw new RuntimeException("Cannot call verify for unknown annotation: "+annotation);
	}
	
	List<CheckResult> afterLoading(OWLGraphWrapper owlGraphWrapper) {
		return verify(afterLoadingChecks, owlGraphWrapper);
	}
	
	List<CheckResult> afterMeriot(OWLGraphWrapper owlGraphWrapper) {
		return verify(afterMeriotChecks, owlGraphWrapper);
	}
	
	List<CheckResult> afterReasoning(OWLGraphWrapper owlGraphWrapper) {
		return verify(afterReasoningChecks, owlGraphWrapper);
	}

	private List<CheckResult> verify(Map<Method, Object> checks, OWLGraphWrapper owlGraphWrapper) {
		List<CheckResult> results = new ArrayList<CheckResult>();
		for(Entry<Method, Object> entry : checks.entrySet()) {
			Method method = entry.getKey();
			Object object = entry.getValue();
			try {
				Object result = method.invoke(object, owlGraphWrapper);
				if (result == null) {
					// assume success
					results.add(CheckResult.createSuccess(method.getName()));
				}
				else {
					if (result instanceof CheckResult) {
						results.add((CheckResult) result);
					}
					else {
						results.add(new CheckResult(method.getName(), Status.InternalError, "The check did not return the expected type"));
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
