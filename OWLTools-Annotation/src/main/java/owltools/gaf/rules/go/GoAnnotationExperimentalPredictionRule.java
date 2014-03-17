package owltools.gaf.rules.go;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import owltools.gaf.Bioentity;
import owltools.gaf.ExtensionExpression;
import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.inference.AnnotationPredictor;
import owltools.gaf.inference.FoldBasedPredictor;
import owltools.gaf.inference.Prediction;
import owltools.gaf.rules.AbstractAnnotationRule;
import owltools.gaf.rules.AnnotationRuleViolation;
import owltools.graph.OWLGraphWrapper;

public class GoAnnotationExperimentalPredictionRule extends AbstractAnnotationRule {
	
	private static final Logger LOG = Logger.getLogger(GoAnnotationExperimentalPredictionRule.class);

	/**
	 * The string to identify this class in the annotation_qc.xml and related factories.
	 * This is not supposed to be changed. 
	 */
	public static final String PERMANENT_JAVA_ID = "org.geneontology.gold.rules.GoAnnotationExperimentalPredictionRule";
	
	private final OWLGraphWrapper source;

	public GoAnnotationExperimentalPredictionRule(OWLGraphWrapper source) {
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
	public List<Prediction> getPredictedAnnotations(GafDocument gafDoc, OWLGraphWrapper graph) {
		List<Prediction> predictions = new ArrayList<Prediction>();
		
		Map<String, Set<GeneAnnotation>> allAnnotations = new HashMap<String, Set<GeneAnnotation>>();
		boolean hasC16Annotations = false;
		
		for(GeneAnnotation annotation : gafDoc.getGeneAnnotations()) {
			List<List<ExtensionExpression>> expressions = annotation.getExtensionExpressions();
			if (expressions != null && expressions.isEmpty() == false) {
				hasC16Annotations = true;
			}
			Bioentity e = annotation.getBioentityObject();
			String id = e.getId();
			Set<GeneAnnotation> anns = allAnnotations.get(id);
			if (anns == null) {
				anns = new HashSet<GeneAnnotation>();
				allAnnotations.put(id, anns);
			}
			anns.add(annotation);
		}
		
		AnnotationPredictor predictor = null;
		if (hasC16Annotations) {
		LOG.info("Use c16 extension for fold based prediction");
			try {
				predictor = new FoldBasedPredictor(gafDoc, source);
				Set<Prediction> foldBasedPredictions = getPredictedAnnotations(allAnnotations, gafDoc, predictor);
				if (foldBasedPredictions != null) {
					predictions.addAll(foldBasedPredictions);
				}
			}
			finally {
				if (predictor != null) {
					predictor.dispose();
				}
				predictor = null;
			}
		}
		LOG.info("Done creating experimental predictions");
		return predictions;
	}
	
	private Set<Prediction> getPredictedAnnotations(Map<String, Set<GeneAnnotation>> allAnnotations, GafDocument gafDoc, AnnotationPredictor predictor) {
		Set<Prediction> predictions = new HashSet<Prediction>();
		
		for (String id : allAnnotations.keySet()) {
			Collection<GeneAnnotation> anns = allAnnotations.get(id);
			Bioentity e = gafDoc.getBioentity(id);
			predictions.addAll(predictor.predictForBioEntity(e, anns));
		}
		return predictions;
	}

	@Override
	public boolean isInferringAnnotations() {
		return true;
	}

}
