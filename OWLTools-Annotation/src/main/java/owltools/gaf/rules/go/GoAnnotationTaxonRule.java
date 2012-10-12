package owltools.gaf.rules.go;

import owltools.gaf.rules.AnnotationTaxonRule;
import owltools.graph.OWLGraphWrapper;

/**
 * Checks if an annotation is valid according to GO taxon constraints.
 * 
 */
public class GoAnnotationTaxonRule extends AnnotationTaxonRule {
	
	/**
	 * The string to identify this class in the annotation_qc.xml and related factories.
	 * This is not supposed to be changed. 
	 */
	public static final String PERMANENT_JAVA_ID = "org.geneontology.gold.rules.AnnotationTaxonRule";

	public GoAnnotationTaxonRule(OWLGraphWrapper graph) {
		super(graph);
	}
	
}

