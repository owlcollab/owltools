package owltools.gaf.rules;

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
