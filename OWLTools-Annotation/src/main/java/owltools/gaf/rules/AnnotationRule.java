package owltools.gaf.rules;

import java.util.Date;
import java.util.Set;

import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.graph.OWLGraphWrapper;

public interface AnnotationRule {
	
	/**
	 * Given an annotation, find the set of violations using the rule
	 * 
	 * @param a annotation
	 * @return set of violations
	 * @see #isAnnotationLevel()
	 */
	public Set<AnnotationRuleViolation> getRuleViolations(GeneAnnotation a);	

	/**
	 * Given a whole {@link GafDocument} , find the set of violations using the rule
	 * 
	 * @param gafDoc
	 * @return set of violations
	 * @see #isDocumentLevel()
	 */
	public Set<AnnotationRuleViolation> getRuleViolations(GafDocument gafDoc);
	
	/**
	 * Given a whole {@link GafDocument} in OWL, find the set of violations using the rule
	 * 
	 * @param graph
	 * @return set of violations
	 * @see #isOwlDocumentLevel()
	 */
	public Set<AnnotationRuleViolation> getRuleViolations(OWLGraphWrapper graph);
	
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
	
	/**
	 * Return the name of the rule.
	 * 
	 * @return name
	 */
	public String getName();
	
	/**
	 * Set the name for the rule.
	 * 
	 * @param name
	 */
	public void setName(String name);
	
	/**
	 * Get the status date of this rule.
	 * 
	 * @return status date or null
	 */
	public Date getDate();
	
	/**
	 * Set the status date for this rule.
	 * 
	 * @param date
	 */
	public void setDate(Date date);
	
	/**
	 * Get the status of this rule.
	 * 
	 * @return status
	 */
	public String getStatus();
	
	/**
	 * Set the status for this rule.
	 * 
	 * @param status
	 */
	public void setStatus(String status);
	
	/**
	 * Get the description for a rule. May be null.
	 * 
	 * @return string or null
	 */
	public String getDescription();
	
	/**
	 * Set the description for this rule.
	 * 
	 * @param description
	 */
	public void setDescription(String description);
	
	/**
	 * @return true if the rule has to be applied to a {@link GeneAnnotation}.
	 * @see #getRuleViolations(GeneAnnotation)
	 */
	public boolean isAnnotationLevel();
	
	/**
	 * @return true if the rule has to be applied to the whole GA document.
	 * @see #getRuleViolations(GafDocument)
	 */
	public boolean isDocumentLevel();
	
	
	/**
	 * @return true if the rule has to be applied to the whole OWL document.
	 * @see #getRuleViolations(OWLGraphWrapper)
	 */
	public boolean isOwlDocumentLevel();
	
}
