package owltools.sim2.kb;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class BasicStatistics {

	private DescriptiveStatistics referenceStats;
	
	public BasicStatistics(DescriptiveStatistics referenceStats) {
		this.referenceStats = referenceStats;
	}

	public double getMin() {
		return referenceStats.getMin();
	}

	public double getMax() {
		return referenceStats.getMax();
	}

	public double getMean() {
		return referenceStats.getMean();
	}

	public double getStandardDeviation() {
		return referenceStats.getStandardDeviation();
	}

	public double getKurtosis() {
		return referenceStats.getKurtosis();
	}

	public double getSkewness() {
		return referenceStats.getSkewness();
	}

}
