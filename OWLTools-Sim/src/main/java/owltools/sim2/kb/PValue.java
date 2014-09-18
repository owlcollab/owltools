package owltools.sim2.kb;

public class PValue {

	private double simplePValue;
	private double anovaPValue;
	private double distributionBasedPValue;
	
	public PValue(double simplePValue, double anovaPValue, double distributionBasedPValue) {
		this.simplePValue = simplePValue;
		this.anovaPValue = anovaPValue;
		this.distributionBasedPValue = distributionBasedPValue;
	}

	public double getSimplePValue() {
		return simplePValue;
	}

	public double getAnovaPValue() {
		return anovaPValue;
	}

	public double getDistributionBasedPValue() {
		return distributionBasedPValue;
	}
}
