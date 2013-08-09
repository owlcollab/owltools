package owltools.gaf.inference;

import owltools.gaf.GeneAnnotation;
import owltools.io.OWLPrettyPrinter;

public class Prediction {
	private final GeneAnnotation geneAnnotation;
	private String reason = null;
	private boolean isRedundantWithExistingAnnotations;
	private boolean isRedundantWithOtherPredictions;
	
	public Prediction(GeneAnnotation geneAnnotation) {
		super();
		this.geneAnnotation = geneAnnotation;
	}

	public GeneAnnotation getGeneAnnotation() {
		return geneAnnotation;
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

	/**
	 * @return the reason
	 */
	public String getReason() {
		return reason;
	}

	/**
	 * @param reason the reason to set
	 */
	public void setReason(String reason) {
		this.reason = reason;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		if (geneAnnotation != null) {
			final String bioentity = geneAnnotation.getBioentity();
			final String cls = geneAnnotation.getCls();
			final String c16 = geneAnnotation.getExtensionExpression();
			result = prime * result	+
					((bioentity == null) ? 0 : bioentity.hashCode());
			result = prime * result + 
					((cls == null) ? 0 : cls.hashCode());
			result = prime * result + 
					((c16 == null) ? 0 : c16.hashCode());
		}
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Prediction other = (Prediction) obj;
		if (geneAnnotation == null) {
			return other.geneAnnotation == null;
		}
		if (other.geneAnnotation == null) {
			return false;
		}
		final String bioentity = geneAnnotation.getBioentity();
		final String cls = geneAnnotation.getCls();
		final String c16 = geneAnnotation.getExtensionExpression();
		final String otherBioentity = other.geneAnnotation.getBioentity();
		final String otherCls = other.geneAnnotation.getCls();
		final String otherC16 = other.geneAnnotation.getExtensionExpression();
		
		if (bioentity == null) {
			if (otherBioentity != null)
				return false;
		} else if (!bioentity.equals(otherBioentity))
			return false;
		if (cls == null) {
			if (otherCls != null)
				return false;
		} else if (!cls.equals(otherCls))
			return false;
		if (c16 == null) {
			if (otherC16 != null)
				return false;
		} else if (!c16.equals(otherC16))
			return false;
		return true;
	}
	
}
