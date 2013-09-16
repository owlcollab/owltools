package owltools.gaf.rules;

import java.util.List;

import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.graph.OWLGraphWrapper;

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
	public List<AnnotationRule> getGafDocumentRules();
	
	
	/**
	 * Get the list of rules, which require a global view of the GAF ({@link GafDocument}) and OWL.
	 * 
	 * @return rules
	 */
	public List<AnnotationRule> getOwlRules();
	
	
	/**
	 * Get the list of rules, which check for inferred annotations. These also
	 * need the global view of the GAF ({@link GafDocument}) and the OWL.
	 * 
	 * @return rules
	 */
	public List<AnnotationRule> getInferenceRules();
	
	/**
	 * Get the list of rules, which check for inferred annotations. These also
	 * need the global view of the GAF ({@link GafDocument}) and the OWL.
	 * 
	 * @return rules
	 */
	public List<AnnotationRule> getExperimentalInferenceRules();
	
	/**
	 * Get the underlying ontology graph for this rule factory.
	 * 
	 * @return graph or null
	 */
	public OWLGraphWrapper getGraph();

}