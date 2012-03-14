package owltools.gaf.rules;

import java.util.Set;

import owltools.gaf.GeneAnnotation;

public interface AnnotationRule {
	
	/**
	 * Given an annotation, find the set of violations using the rule
	 * 
	 * @param a annotation
	 * @return set of violations
	 */
	public Set<AnnotationRuleViolation> getRuleViolations(GeneAnnotation a);	

	/**
	 * Set the rule id
	 * 
	 * @param ruleId
	 */
	public void setRuleId(String ruleId);
	
	/**
	 * Get the ruleId.
	 * 
	 * @return ruleId
	 */
	public String getRuleId();
	
}
