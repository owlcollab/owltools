package owltools.gaf.rules;

import java.util.Collections;
import java.util.Set;

import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;

public abstract class AbstractAnnotationRule implements AnnotationRule {

	private String ruleId;
	
	public abstract Set<AnnotationRuleViolation> getRuleViolations(GeneAnnotation a);
	
	@Override
	public Set<AnnotationRuleViolation> getRuleViolations(GafDocument gafDoc) {
		// per default, do nothing
		return Collections.emptySet();
	}

	@Override
	public void setRuleId(String ruleId) {
		this.ruleId = ruleId;
	}

	@Override
	public String getRuleId() {
		return this.ruleId;
	}

	@Override
	public boolean isDocumentLevel() {
		return false;
	}
	
}

