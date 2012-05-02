package owltools.gaf.rules;

import java.util.List;

import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;

public interface AnnotationRulesFactory {

	/**
	 * Initialize the factory.
	 */
	public void init();
	
	/**
	 * Get the list of rules to be applied for each {@link GeneAnnotation}
	 * 
	 * @return rules
	 */
	public List<AnnotationRule> getGeneAnnotationRules();
	
	/**
	 * Get the list of rules, which require a global view of the GAF ({@link GafDocument}).
	 * 
	 * @return rules
	 */
	public List<AnnotationRule> getGafRules();

}