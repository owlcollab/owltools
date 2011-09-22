package owltools.ontologyverification;

import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import owltools.graph.OWLGraphWrapper;
import owltools.ontologyverification.CheckResult.Status;
import owltools.ontologyverification.annotations.AfterLoading;
import owltools.ontologyverification.annotations.AfterMeriot;
import owltools.ontologyverification.annotations.AfterReasoning;

/**
 * Handle the ontology checks for an ontology and its reporting 
 * via a {@link Logger}.
 *
 */
public class OntologyCheckHandler {

	/**
	 * Default instance for convenient use.
	 */
	public static final OntologyCheckHandler DEFAULT_INSTANCE = new OntologyCheckHandler(false, DefaultOntologyChecks.class);
	
	private static final Logger logger = Logger.getLogger(OntologyCheckHandler.class);
	
	private final OntologyCheckRunner runner;
	private final boolean isWarningFatal;
	
	/**
	 * Create a new instance.
	 * 
	 * @param isWarningFatal if true all warnings are treated as errors.
	 * @param classes list of classes containing ontology checks
	 */
	public OntologyCheckHandler(boolean isWarningFatal, Class<?>...classes) {
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
	 * Run tests for the ontology after merioting. 
	 * 
	 * @param owlGraphWrapper ontology
	 */
	public void afterMeriot(OWLGraphWrapper owlGraphWrapper) {
		run(owlGraphWrapper, AfterMeriot.class);
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
		List<CheckResult> results = runner.verify(owlGraphWrapper, annotation);
		String ontologyId = owlGraphWrapper.getOntologyId();
		int successCount = 0;
		int warningCount = 0;
		int errorCount = 0;
		int internalErrorCount = 0;
		StringBuilder sb = new StringBuilder("OntologyChecks for ").append(ontologyId).append(':');
		reportHeader(sb);
		for (CheckResult checkResult : results) {
			switch (checkResult.getStatus()) {
			case Success:
				successCount += 1;
				report(sb, checkResult, Status.Success);
				break;
			case Warning:
				if (isWarningFatal) {
					errorCount += 1;
					report(sb, checkResult, Status.Error);
				}
				else {
					warningCount += 1;
					report(sb, checkResult, Status.Warning);
				}
				break;
			case Error:
				errorCount += 1;
				report(sb, checkResult, Status.Error);
				break;

			case InternalError:
				internalErrorCount += 1;
				report(sb, checkResult, Status.InternalError);
				break;
			default:
				break;
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
		logger.log(level, sb.toString());
		if (hasErrors) {
			throw new RuntimeException(createExceptionMessage(ontologyId, errorCount, internalErrorCount));
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
	
	protected void report(StringBuilder sb, CheckResult checkResult, Status status) {
		sb.append('\n');
		sb.append(checkResult.getCheckName());
		sb.append('\t');
		sb.append(status.name());
		sb.append('\t');
		List<String> messages = checkResult.getMessages();
		if (messages != null && !messages.isEmpty()) {
			for (int i = 0; i < messages.size(); i++) {
				if (i > 0) {
					sb.append("\n\t\t");
				}
				sb.append(messages.get(i));
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
