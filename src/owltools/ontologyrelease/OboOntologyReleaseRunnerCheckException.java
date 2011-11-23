package owltools.ontologyrelease;

import java.util.List;

/**
 * OORT Exception indicating a failed check during the release process. 
 */
public class OboOntologyReleaseRunnerCheckException extends Exception {

	// generated
	private static final long serialVersionUID = 3027646526727891643L;
	
	private final String message;
	private final List<String> reasons;
	private final String hint;

	public OboOntologyReleaseRunnerCheckException(String message) {
		this(message, null, null);
	}
	
	public OboOntologyReleaseRunnerCheckException(String message, List<String> reasons) {
		this(message, reasons, null);
	}
	
	public OboOntologyReleaseRunnerCheckException(String message, List<String> reasons, String hint) {
		super();
		this.message = message;
		this.reasons = reasons;
		this.hint = hint;
	}

	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @return the reasons
	 */
	public List<String> getReasons() {
		return reasons;
	}

	/**
	 * @return the hint
	 */
	public String getHint() {
		return hint;
	}
	
	/**
	 * Render the exception in an human readable form and 
	 * suitable for appending to a logger.
	 * 
	 * @return string
	 */
	public String renderMessageString() {
		StringBuilder sb = new StringBuilder(message);
		if (reasons != null && !reasons.isEmpty()) {
			sb.append("\n\tDetails:\n");
			for (String reason : reasons) {
				sb.append('\t').append(reason).append('\n');
			}
		}
		if (hint != null) {
			sb.append("\n\tHint: ").append(hint).append('\n');
		}
		return sb.toString();
	}
}
