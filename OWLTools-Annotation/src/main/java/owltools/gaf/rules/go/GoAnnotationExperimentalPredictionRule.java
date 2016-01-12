package owltools.gaf.rules.go;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.inference.Prediction;
import owltools.gaf.rules.AbstractAnnotationRule;
import owltools.gaf.rules.AnnotationRuleViolation;
import owltools.graph.OWLGraphWrapper;

public class GoAnnotationExperimentalPredictionRule extends AbstractAnnotationRule {
	
	static final Logger LOG = Logger.getLogger(GoAnnotationExperimentalPredictionRule.class);

	/**
	 * The string to identify this class in the annotation_qc.xml and related factories.
	 * This is not supposed to be changed. 
	 */
	public static final String PERMANENT_JAVA_ID = "org.geneontology.gold.rules.GoAnnotationExperimentalPredictionRule";
	
	public GoAnnotationExperimentalPredictionRule() {
		super();
	}

	@Override
	public Set<AnnotationRuleViolation> getRuleViolations(GeneAnnotation a) {
		// Do nothing
		return Collections.emptySet();
	}

	@Override
	public boolean isAnnotationLevel() {
		// Deactivate annotation level
		return false;
	}

	@Override
	public List<Prediction> getPredictedAnnotations(GafDocument gafDoc, OWLGraphWrapper graph) {
		// currently there are no experimental methods
		return Collections.emptyList();
	}
	
	@Override
	public boolean isInferringAnnotations() {
		return true;
	}

}
