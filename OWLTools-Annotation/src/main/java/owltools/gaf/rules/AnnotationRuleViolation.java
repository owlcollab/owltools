package owltools.gaf.rules;

import java.util.Set;

import owltools.gaf.GeneAnnotation;

/**
 * Data associated with a rule violation
 */
public class AnnotationRuleViolation {

	private final String message;
	private Set<GeneAnnotation> suggestedReplacements;
	private GeneAnnotation sourceAnnotation;
	private String annotationRow;
	private String gafDoument;
	private String ruleId;
	private int lineNumber;

	/**
	 * Create a simple violation from with a given message.
	 * 
	 * @param msg
	 */
	public AnnotationRuleViolation(String msg) {
		super();
		message = msg;
	}

	/**
	 * Create a violation object with a message and a the corresponding source
	 * annotation.
	 * 
	 * @param message
	 * @param sourceAnnotation
	 */
	public AnnotationRuleViolation(String message, GeneAnnotation sourceAnnotation) {
		this(message);
		setSourceAnnotation(sourceAnnotation);
	}

	/**
	 * Create a violation object with a message and a the corresponding
	 * annotation row.
	 * 
	 * @param message
	 * @param annotationRow
	 */
	public AnnotationRuleViolation(String message, String annotationRow) {
		this(message);
		this.annotationRow = annotationRow;
	}

	public String getRuleId() {
		return ruleId;
	}

	public void setRuleId(String ruleId) {
		this.ruleId = ruleId;
	}

	public String getMessage() {
		return message;
	}

	public Set<GeneAnnotation> getSuggestedReplacements() {
		return suggestedReplacements;
	}

	public void setSuggestedReplacements(Set<GeneAnnotation> suggestedReplacements) {
		this.suggestedReplacements = suggestedReplacements;
	}

	public GeneAnnotation getSourceAnnotation() {
		return sourceAnnotation;
	}

	public void setSourceAnnotation(GeneAnnotation sourceAnnotation) {
		this.sourceAnnotation = sourceAnnotation;
		if (sourceAnnotation != null) {
			this.annotationRow = sourceAnnotation.toString();
			if (sourceAnnotation.getSource() != null) {
				this.annotationRow = sourceAnnotation.getSource().getRow();
				this.lineNumber = sourceAnnotation.getSource().getLineNumber();
				this.gafDoument = sourceAnnotation.getSource().getFileName();
			}
		}
	}

	public String getAnnotationRow() {
		return annotationRow;
	}

	public void setAnnotationRow(String annotationRow) {
		this.annotationRow = annotationRow;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public void setLineNumber(int lineNumber) {
		this.lineNumber = lineNumber;
	}

	public String getGafDoument() {
		if (this.sourceAnnotation != null)
			return this.sourceAnnotation.getGafDocument();

		return gafDoument;
	}

	public void setGafDoument(String gafDoument) {
		this.gafDoument = gafDoument;
	}

}
