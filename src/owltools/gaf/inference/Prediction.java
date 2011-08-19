package owltools.gaf.inference;

import java.util.HashSet;
import java.util.Set;
import owltools.gaf.GeneAnnotation;
import owltools.io.OWLPrettyPrinter;

public class Prediction {
	private GeneAnnotation geneAnnotation;
	private boolean isRedundantWithExistingAnnotations;
	private boolean isRedundantWithOtherPredictions;
	
	public Prediction() {
		super();
	}

	public GeneAnnotation getGeneAnnotation() {
		return geneAnnotation;
	}

	public void setGeneAnnotation(GeneAnnotation geneAnnotation) {
		this.geneAnnotation = geneAnnotation;
	}

	public String toString() {
		return isRedundantWithExistingAnnotations + "/" + isRedundantWithOtherPredictions + "/" + geneAnnotation.toString();
	}

	public boolean isRedundantWithExistingAnnotations() {
		return isRedundantWithExistingAnnotations;
	}

	public void setRedundantWithExistingAnnotations(
			boolean isRedundantWithExistingAnnotations) {
		this.isRedundantWithExistingAnnotations = isRedundantWithExistingAnnotations;
	}

	public boolean isRedundantWithOtherPredictions() {
		return isRedundantWithOtherPredictions;
	}

	public void setRedundantWithOtherPredictions(
			boolean isRedundantWithOtherPredictions) {
		this.isRedundantWithOtherPredictions = isRedundantWithOtherPredictions;
	}
	
	public String render(OWLPrettyPrinter pp) {
		return "Prediction="+pp.renderId(geneAnnotation.getCls())+" // "+geneAnnotation.toString();
	}


	
}
