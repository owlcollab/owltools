package owltools.sim2.kb;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class ICDistribution {

	private double samplingRate;
	
	private List<ICDistributionValue> referenceDistribution;
	private List<ICDistributionValue> candidateDistribution;
	
	private BasicStatistics basicReferenceStatistics;
	private BasicStatistics basicCandidateStatistics;
	
	public ICDistribution(List<Double> icList,
			DescriptiveStatistics referenceStats, double samplingRate) {
		DescriptiveStatistics candidateStats = new DescriptiveStatistics();
		for (double d : icList) {
			candidateStats.addValue(d);
		}
		this.samplingRate = samplingRate;
		
		referenceDistribution = new ArrayList<ICDistributionValue>();
		candidateDistribution = new ArrayList<ICDistributionValue>();
		
		basicReferenceStatistics = new BasicStatistics(referenceStats);
		basicCandidateStatistics = new BasicStatistics(candidateStats);
		
		generateDistribution(referenceStats.getSortedValues(), referenceDistribution);
		generateDistribution(candidateStats.getSortedValues(), candidateDistribution);
	}

	private void generateDistribution(double[] sortedDistroValues, List<ICDistributionValue> distribution) {
		double current = 0;
		int prevI = 0;
		
		while (current <= sortedDistroValues[sortedDistroValues.length - 1]) {
			double rate = current + samplingRate;
			
			int count = 0;
			for (int i = prevI ;i < sortedDistroValues.length ;i++) {
				if (sortedDistroValues[i] >= current && sortedDistroValues[i] < rate) {
					count++;
				} else {
					if (sortedDistroValues[i] >= rate) {
						prevI = i;
						break;
					}
				}
			}
			
			double perc = (double) count / sortedDistroValues.length;
			distribution.add(new ICDistributionValue(rate / 2, perc));
			current += samplingRate;
		}
	}

	public List<ICDistributionValue> getReferenceDistribution() {
		return referenceDistribution;
	}

	public List<ICDistributionValue> getCandidateDistribution() {
		return candidateDistribution;
	}

	public BasicStatistics getReferenceBasicStatistics() {
		return basicReferenceStatistics;
	}

	public BasicStatistics getCandidateBasicStatistics() {
		return basicCandidateStatistics;
	}

}
