package owltools.gaf.rules;

import java.util.Set;

import owltools.gaf.GeneAnnotation;

/**
 * Data associated with a rule violation
 */
public class AnnotationRuleViolation {
	
	public static enum ViolationType {
		Error,
		Warning,
		Recommendation
		// TODO Handle warnings and errors, which can be fixed by the test (e.g., by removing or by inference)
	}

	private final String message;
	private Set<GeneAnnotation> suggestedReplacements;
	private GeneAnnotation sourceAnnotation;
	private String annotationRow;
	private String gafDocument;
	private final String ruleId;
	private int lineNumber = -1;
	private ViolationType type;

	/**
	 * Create a simple violation from with a given message.
	 * 
	 * @param ruleId 
	 * @param msg
	 */
	public AnnotationRuleViolation(String ruleId, String msg) {
		this(ruleId, msg, (String) null);
	}

	/**
	 * Create a violation object with a message and a the corresponding source
	 * annotation.
	 * 
	 * @param ruleId 
	 * @param message
	 * @param sourceAnnotation
	 */
	public AnnotationRuleViolation(String ruleId, String message, GeneAnnotation sourceAnnotation) {
		this(ruleId, message);
		setSourceAnnotation(sourceAnnotation);
	}
	
	/**
	 * Create a violation object with a message and a the corresponding source
	 * annotation.
	 * 
	 * @param ruleId 
	 * @param message
	 * @param sourceAnnotation
	 * @param type
	 */
	public AnnotationRuleViolation(String ruleId, String message, GeneAnnotation sourceAnnotation, ViolationType type) {
		this(ruleId, message, (String) null, type);
		setSourceAnnotation(sourceAnnotation);
	}

	/**
	 * Create a violation object with a message and a the corresponding
	 * annotation row.
	 * 
	 * @param ruleId 
	 * @param message
	 * @param annotationRow
	 */
	public AnnotationRuleViolation(String ruleId, String message, String annotationRow) {
		this(ruleId, message, annotationRow, ViolationType.Error);
	}
	
	/**
	 * Create a violation object with a message and a the corresponding
	 * annotation row and {@link ViolationType}.
	 * 
	 * @param ruleId 
	 * @param message
	 * @param annotationRow
	 * @param type
	 */
	public AnnotationRuleViolation(String ruleId, String message, String annotationRow, ViolationType type) {
		super();
		this.ruleId = ruleId;
		this.message = message;
		this.annotationRow = annotationRow;
		this.type = type;
	}

	public String getRuleId() {
		return ruleId;
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
				this.gafDocument = sourceAnnotation.getSource().getFileName();
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

	public String getGafDocument() {
		return gafDocument;
	}

	/**
	 * @return the type
	 */
	public ViolationType getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(ViolationType type) {
		this.type = type;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AnnotationRuleViolation [");
		if(ruleId != null) {
			builder.append("ruleId=");
			builder.append(ruleId);
		}
		if(type != null) {
			builder.append("type=");
			builder.append(type.name());
		}
		if (lineNumber >= 0) {
			builder.append(", ");
			builder.append("lineNumber=");
			builder.append(lineNumber);
		}
		if (message != null) {
			builder.append(", ");
			builder.append("message=");
			builder.append(message);
		}
		if (suggestedReplacements != null) {
			builder.append(", ");
			builder.append("suggestedReplacements=");
			builder.append(suggestedReplacements);
		}
		if (sourceAnnotation != null) {
			builder.append(", ");
			builder.append("sourceAnnotation=");
			builder.append(sourceAnnotation);
		}
		if (annotationRow != null) {
			builder.append(", ");
			builder.append("annotationRow=");
			builder.append(annotationRow);
		}
		builder.append("]");
		return builder.toString();
	}

}
