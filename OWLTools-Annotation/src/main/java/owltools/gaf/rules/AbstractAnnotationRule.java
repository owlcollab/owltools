package owltools.gaf.rules;

import java.util.Set;

import owltools.gaf.GeneAnnotation;

public abstract class AbstractAnnotationRule implements AnnotationRule {

	private String ruleId;
	
	public abstract Set<AnnotationRuleViolation> getRuleViolations(GeneAnnotation a);

	@Override
	public void setRuleId(String ruleId) {
		this.ruleId = ruleId;
	}

	@Override
	public String getRuleId() {
		return this.ruleId;
	}

}

