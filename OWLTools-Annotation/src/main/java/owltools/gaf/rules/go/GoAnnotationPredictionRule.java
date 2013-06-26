package owltools.gaf.rules.go;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.inference.BasicAnnotationPropagator;
import owltools.gaf.inference.FoldBasedPredictor;
import owltools.gaf.inference.Prediction;
import owltools.gaf.rules.AbstractAnnotationRule;
import owltools.gaf.rules.AnnotationRuleViolation;
import owltools.graph.OWLGraphWrapper;

public class GoAnnotationPredictionRule extends AbstractAnnotationRule {

	/**
	 * The string to identify this class in the annotation_qc.xml and related factories.
	 * This is not supposed to be changed. 
	 */
	public static final String PERMANENT_JAVA_ID = "org.geneontology.gold.rules.GoAnnotationPredictionRule";
	
	private final OWLGraphWrapper source;

	public GoAnnotationPredictionRule(OWLGraphWrapper source) {
		super();
		this.source = source;
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
	public Set<Prediction> getPredictedAnnotations(GafDocument gafDoc, OWLGraphWrapper graph) {
		Set<Prediction> predictions = new HashSet<Prediction>();
		
		BasicAnnotationPropagator propagator = new BasicAnnotationPropagator(gafDoc, source);
		Set<Prediction> basicPredictions = propagator.getAllPredictions();
		if (basicPredictions != null) {
			predictions.addAll(basicPredictions);
		}
		FoldBasedPredictor foldBasedPredictor = new FoldBasedPredictor(gafDoc, source);
		Set<Prediction> foldBasedPredictions = foldBasedPredictor.getAllPredictions();
		if (foldBasedPredictions != null) {
			predictions.addAll(foldBasedPredictions);
		}
		return predictions;
	}

	@Override
	public boolean isInferringAnnotations() {
		return true;
	}

}
