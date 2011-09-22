package owltools.ontologyverification;

import java.util.Collections;
import java.util.List;

public class CheckResult {

	private String checkName;
	private final Status status;
	private final List <String> messages;
	
	public enum Status {
		Success,
		Warning,
		Error,
		InternalError
	}
	
	public static CheckResult createSuccess() {
		return new CheckResult(null, Status.Success);
	}
	
	public static CheckResult createSuccess(String message) {
		return new CheckResult(null, Status.Success, Collections.singletonList(message));
	}
	
	public static CheckResult createWarning(String message) {
		return new CheckResult(null, Status.Warning, Collections.singletonList(message));
	}
	
	public static CheckResult createWarning(List<String> messages) {
		return new CheckResult(null, Status.Warning, messages);
	}
	
	public static CheckResult createError(String message) {
		return new CheckResult(null, Status.Error, Collections.singletonList(message));
	}
	
	public static CheckResult createError(List<String> messages) {
		return new CheckResult(null, Status.Error, messages);
	}
	
	CheckResult(String checkName,  Status status, String message) {
		this(checkName, status, Collections.singletonList(message));
	}
	
	CheckResult(String checkName,  Status status, List<String> messages) {
		super();
		this.checkName = checkName;
		this.status = status;
		this.messages = messages;
	}

	CheckResult(String checkName, Status status) {
		this(checkName, status, Collections.<String>emptyList());
	}

	/**
	 * @return the checkName
	 */
	public String getCheckName() {
		return checkName;
	}

	/**
	 * @param checkName the checkName to set
	 */
	void setCheckName(String checkName) {
		this.checkName = checkName;
	}

	/**
	 * @return the status
	 */
	public Status getStatus() {
		return status;
	}

	/**
	 * @return the messages
	 */
	public List<String> getMessages() {
		return messages;
	}
}
