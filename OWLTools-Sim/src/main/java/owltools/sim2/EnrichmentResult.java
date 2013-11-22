package owltools.sim2;

import org.semanticweb.owlapi.model.OWLClass;

public class EnrichmentResult implements Comparable<EnrichmentResult> {
	public OWLClass enrichedClass; // attribute being tested

	public OWLClass sampleSetClass; // e.g. gene set

	public Double pValue;

	public Double pValueCorrected;

	public int populationClassSize;

	public int sampleSetClassSize;

	public int enrichedClassSize;

	public int eiSetSize;
	
	public EnrichmentResult(OWLClass sampleSetClass, OWLClass enrichedClass,
			double pValue, double pValueCorrected) {
		super();
		this.sampleSetClass = sampleSetClass;
		this.enrichedClass = enrichedClass;
		this.pValue = pValue;
		this.pValueCorrected = pValueCorrected;
	}

	public EnrichmentResult(OWLClass sampleSetClass, OWLClass enrichedClass,
			double pValue, double pValueCorrected, int populationClassSize,
			int sampleSetClassSize, int enrichedClassSize, int eiSetSize) {
		super();
		this.sampleSetClass = sampleSetClass;
		this.enrichedClass = enrichedClass;
		this.pValue = pValue;
		this.pValueCorrected = pValueCorrected;
		this.populationClassSize = populationClassSize;
		this.sampleSetClassSize = sampleSetClassSize;
		this.enrichedClassSize = enrichedClassSize;
		this.eiSetSize = eiSetSize;
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((enrichedClass == null) ? 0 : enrichedClass.hashCode());
		result = prime * result + enrichedClassSize;
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
		EnrichmentResult other = (EnrichmentResult) obj;
		if (enrichedClass == null) {
			if (other.enrichedClass != null)
				return false;
		} else if (!enrichedClass.equals(other.enrichedClass))
			return false;
		if (enrichedClassSize != other.enrichedClassSize)
			return false;
		return true;
	}
	
	

}