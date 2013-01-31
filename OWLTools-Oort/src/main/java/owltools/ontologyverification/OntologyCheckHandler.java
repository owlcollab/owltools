package owltools.ontologyverification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import owltools.graph.OWLGraphWrapper;
import owltools.ontologyrelease.logging.LogHandler;
import owltools.ontologyverification.OntologyCheckRunner.TimePoint;

/**
 * Handle the ontology checks for an ontology and its reporting to the {@link LogHandler}.
 *
 */
public class OntologyCheckHandler {

	private final OntologyCheckRunner runner;
	private final boolean isWarningFatal;
	private final List<LogHandler> handlers;
	
	/**
	 * Create a new instance.
	 * 
	 * @param isWarningFatal if true all warnings are treated as errors.
	 * @param checks list of ontology checks
	 * @param handlers
	 */
	public OntologyCheckHandler(boolean isWarningFatal, List<OntologyCheck> checks, List<LogHandler> handlers) {
		super();
		this.isWarningFatal = isWarningFatal;
		this.handlers = handlers;
		runner = new OntologyCheckRunner(checks);
	}
	
	public static class CheckSummary {
		
		public final boolean success;
		public final int errorCount;
		public final String message;
		
		/**
		 * @param success
		 * @param errorCount
		 * @param message
		 */
		protected CheckSummary(boolean success, int errorCount, String message) {
			this.success = success;
			this.errorCount = errorCount;
			this.message = message;
		}
		
		static CheckSummary success() {
			return new CheckSummary(true, 0, null);
		}
		
		static CheckSummary error(int count, String message) {
			return new CheckSummary(false, count, message);
		}
	}
	
	/**
	 * Run tests for the ontology after loading it. 
	 * 
	 * @param owlGraphWrapper ontology
	 * @return summary
	 */
	public CheckSummary afterLoading(OWLGraphWrapper owlGraphWrapper) {
		return run(owlGraphWrapper, TimePoint.AfterLoad);
	}
	
	/**
	 * Run tests for the ontology after mireoting. 
	 * 
	 * @param owlGraphWrapper ontology
	 * @return summary
	 */
	public CheckSummary afterMireot(OWLGraphWrapper owlGraphWrapper) {
		return run(owlGraphWrapper, TimePoint.AfterMireot);
	}
	
	/**
	 * Run tests for the ontology after reasoning. 
	 * 
	 * @param owlGraphWrapper ontology
	 * @return summary
	 */
	public CheckSummary afterReasoning(OWLGraphWrapper owlGraphWrapper) {
		return run(owlGraphWrapper, TimePoint.AfterReasoning);
	}
	
	CheckSummary run(OWLGraphWrapper owlGraphWrapper, TimePoint timePoint) {
		Map<OntologyCheck, Collection<CheckWarning>> results = runner.verify(owlGraphWrapper, timePoint);
		if (results == null || results.isEmpty()) {
			// do nothing
			return CheckSummary.success();
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
		boolean hasErrors = errorCount > 0 || internalErrorCount > 0;
		report(sb, timePoint);
		if (hasErrors) {
			return CheckSummary.error(errorCount, createExceptionMessage(ontologyId, errorCount, internalErrorCount));
		}
		return CheckSummary.success();
		
	}

	protected void report(StringBuilder sb, TimePoint timePoint) {
		for (LogHandler handler : handlers) {
			handler.report("OntologyCheck-"+timePoint.name(), sb.toString());
		}
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
