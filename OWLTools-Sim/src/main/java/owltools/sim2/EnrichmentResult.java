package owltools.sim2;

import org.semanticweb.owlapi.model.OWLClass;

public class EnrichmentResult implements Comparable<EnrichmentResult> {
	public OWLClass enrichedClass; // attribute being tested

	public OWLClass sampleSetClass; // e.g. gene set

	public Double pValue;

	public Double pValueCorrected;

	public EnrichmentResult(OWLClass sampleSetClass, OWLClass enrichedClass,
			double pValue, double pValueCorrected) {
		super();
		this.sampleSetClass = sampleSetClass;
		this.enrichedClass = enrichedClass;
		this.pValue = pValue;
		this.pValueCorrected = pValueCorrected;
	}

	@Override
	public int compareTo(EnrichmentResult result2) {
		return this.pValue.compareTo((result2).pValue);
	}

	@Override
	public String toString() {
		return sampleSetClass + " " + enrichedClass + " " + pValue + " "
				+ pValueCorrected;
	}

}