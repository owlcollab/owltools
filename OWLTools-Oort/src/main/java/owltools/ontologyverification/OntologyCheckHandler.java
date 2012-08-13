package owltools.ontologyverification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import owltools.graph.OWLGraphWrapper;
import owltools.ontologyverification.annotations.AfterLoading;
import owltools.ontologyverification.annotations.AfterMireot;
import owltools.ontologyverification.annotations.AfterReasoning;

/**
 * Handle the ontology checks for an ontology and its reporting 
 * via a {@link Logger}.
 *
 */
public class OntologyCheckHandler {

	private static final Logger logger = Logger.getLogger(OntologyCheckHandler.class);
	
	private final OntologyCheckRunner runner;
	private final boolean isWarningFatal;
	
	/**
	 * Create a new instance.
	 * 
	 * @param isWarningFatal if true all warnings are treated as errors.
	 * @param classes list of classes containing ontology checks
	 */
	public OntologyCheckHandler(boolean isWarningFatal, List<Class<? extends OntologyCheck>> classes) {
		super();
		this.isWarningFatal = isWarningFatal;
		runner = new OntologyCheckRunner(classes);
	}
	
	/**
	 * Run tests for the ontology after loading it. 
	 * 
	 * @param owlGraphWrapper ontology
	 */
	public void afterLoading(OWLGraphWrapper owlGraphWrapper) {
		run(owlGraphWrapper, AfterLoading.class);
	}
	
	/**
	 * Run tests for the ontology after mireoting. 
	 * 
	 * @param owlGraphWrapper ontology
	 */
	public void afterMireot(OWLGraphWrapper owlGraphWrapper) {
		run(owlGraphWrapper, AfterMireot.class);
	}
	
	/**
	 * Run tests for the ontology after reasoning. 
	 * 
	 * @param owlGraphWrapper ontology
	 */
	public void afterReasoning(OWLGraphWrapper owlGraphWrapper) {
		run(owlGraphWrapper, AfterReasoning.class);
	}
	
	void run(OWLGraphWrapper owlGraphWrapper, Class<?> annotation) {
		Map<OntologyCheck, Collection<CheckWarning>> results = runner.verify(owlGraphWrapper, annotation);
		if (results == null || results.isEmpty()) {
			// do nothing
			return;
		}
		String ontologyId = owlGraphWrapper.getOntologyId();
		int successCount = 0;
		int warningCount = 0;
		int errorCount = 0;
		int internalErrorCount = 0;
		StringBuilder sb = new StringBuilder("OntologyChecks for ").append(ontologyId).append(':');
		reportHeader(sb);
		for (OntologyCheck check : results.keySet()) {
			Collection<CheckWarning> allWarnings = results.get(check);
			if (allWarnings == null || allWarnings.isEmpty()) {
				successCount += 1;
				report(sb, check, allWarnings, "Success");
			}
			else {
				List<CheckWarning> fatalOnly = new ArrayList<CheckWarning>();
				List<CheckWarning> warningsOnly = new ArrayList<CheckWarning>();
				for (CheckWarning warning : allWarnings) {
					if (warning.isFatal() || isWarningFatal) {
						fatalOnly.add(warning);
					}
					else {
						warningsOnly.add(warning);
					}
				}
				warningCount += warningsOnly.size();
				errorCount += fatalOnly.size();
				if (!fatalOnly.isEmpty()) {
					report(sb, check, fatalOnly, "Error");
				}
				if (!warningsOnly.isEmpty()) {
					report(sb, check, warningsOnly, "Warning");
				}
			}
		}
		sb.append('\n');
		sb.append(summary(results.size(), successCount, warningCount, errorCount, internalErrorCount));
		Level level = Level.INFO;
		if (warningCount > 0) {
			level = Level.WARN;
		}
		boolean hasErrors = errorCount > 0 || internalErrorCount > 0;
		if (hasErrors) {
			level = Level.ERROR;
		}
		log(sb, level);
		if (hasErrors) {
			throw new RuntimeException(createExceptionMessage(ontologyId, errorCount, internalErrorCount));
		}
		
	}

	protected void log(StringBuilder sb, Level level) {
		logger.log(level, sb.toString());
	}

	private String createExceptionMessage(String ontologyId, int errorCount, int internalErrorCount) {
		StringBuilder message = new StringBuilder("OntologyCheck for ");
		message.append(ontologyId);
		message.append(" found ");
		if (errorCount > 0) {
			message.append(errorCount);
			message.append(" error");
			if (errorCount > 1) {
				message.append('s');
			}
		}
		if (errorCount > 0 && internalErrorCount > 0) {
			message.append(" and ");
		}
		if (internalErrorCount > 0) {
			message.append(internalErrorCount);
			message.append(" internal error");
			if (internalErrorCount > 1) {
				message.append('s');
			}
		}
		return message.toString();
	}
	
	protected void reportHeader(StringBuilder sb) {
		sb.append('\n');
		sb.append("Name \t Status \t Message");
	}
	
	protected void report(StringBuilder sb, OntologyCheck check, Collection<CheckWarning> warnings, String status) {
		sb.append('\n');
		sb.append(check.getLabel());
		sb.append('\t');
		sb.append(status);
		sb.append('\t');
		if (warnings != null && !warnings.isEmpty()) {
			for (CheckWarning warning : warnings) {
				sb.append("\n\t\t");
				sb.append(warning.getMessage());
			}
		}
	}
	
	protected StringBuilder summary(int totalCount, int successCount, int warningCount, int errorCount, int internalErrorCount) {
		StringBuilder sb = new StringBuilder();
		sb.append("Summary (Total, Success, Warning, Error):  (");
		sb.append(totalCount);
		sb.append(", ");
		sb.append(successCount);
		sb.append(", ");
		sb.append(warningCount);
		sb.append(", ");
		sb.append(errorCount+internalErrorCount);
		sb.append(')');
		return sb;
	}
}
