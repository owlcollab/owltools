package owltools.sim2;

import owltools.sim2.SimpleOwlSim.Metric;

/**
 * Will keep track of the overall stats of analysis in data sets. This can be
 * used for both an entire set of data, or even the individual pairs of data
 * within a dataset. This can be used to generate a report at whatever level of
 * granularity is necessary, as multiple simstats instances can be instantiated.
 * Note that mins are included here, but aren't really used yet... we can 
 * probably assume that the minimum is zero for most cases.
 * 
 * @author Nicole
 * 
 */
public class SimStats {
	public double minAnalysisBMAJ;

	public double maxAnalysisBMAJ;

	public double minAnalysisBMAIC;

	public double maxAnalysisBMAIC;

	public double minAnalysisMaxIC;

	public double maxAnalysisMaxIC;

	public double minAnalysisSimJ;

	public double maxAnalysisSimJ;

	public double minAnalysisLCSIC;

	public double maxAnalysisLCSIC;

	public int classPairCount;

	public int uniqueClassPairCount;

	public int individualPairCount;

	public int uniqueClassCount;

	public SimStats() {
		init();
	}

	public void init() {
		this.minAnalysisBMAJ = 0.0;
		this.maxAnalysisBMAJ = 0.0;
		this.minAnalysisBMAIC = 0.0;
		this.maxAnalysisBMAIC = 0.0;
		this.minAnalysisMaxIC = 0.0;
		this.maxAnalysisMaxIC = 0.0;
		this.minAnalysisSimJ = 0.0;
		this.maxAnalysisSimJ = 0.0;
		this.minAnalysisLCSIC = 0.0;
		this.maxAnalysisLCSIC = 0.0;
		this.classPairCount = 0;
		this.uniqueClassPairCount = 0;
		this.individualPairCount = 0;
		this.uniqueClassCount = 0;
	}

	public void incrementClassPairCount(int n) {
		this.classPairCount += n;
	}

	public void incrementUniqueClassPairCount() {
		this.uniqueClassPairCount += 1;
	}

	public void incrementIndividualPairCount() {
		this.individualPairCount += 1;
	}

	public void setMin(Metric m, double s) {
		switch (m) {
		case IC_MCS:
			this.minAnalysisBMAIC = s;
		case JACCARD:
			this.minAnalysisBMAJ = s;
		case MAXIC:
			this.minAnalysisMaxIC = s;
		case SIMJ:
			this.minAnalysisSimJ = s;
		case LCSIC:
			this.minAnalysisLCSIC = s;
		}
	}

	public void setMax(Metric m, double s) {
		switch (m) {
		case IC_MCS:
			if (s > this.maxAnalysisBMAIC) {
				this.maxAnalysisBMAIC = s;
			}
			;
		case JACCARD:
			if (s > this.maxAnalysisBMAJ) {
				this.maxAnalysisBMAJ = s;
			}
			;
		case MAXIC:
			if (s > this.maxAnalysisMaxIC) {
				this.maxAnalysisMaxIC = s;
			}
			;
		case SIMJ:
			if (s > this.maxAnalysisSimJ) {
				this.maxAnalysisSimJ = s;
			}
			;
		case LCSIC:
			if (s > this.maxAnalysisLCSIC) {
				this.maxAnalysisLCSIC = s;
			}
			;
		}
	}

	public double getMax(Metric m) {
		double s = 0.0;
		switch (m) {
		case IC_MCS:
			s = this.maxAnalysisBMAIC;
		case JACCARD:
			s = this.maxAnalysisBMAJ;
		case MAXIC:
			s = this.maxAnalysisMaxIC;
		case SIMJ:
			s = this.maxAnalysisSimJ;
		case LCSIC:
			s = this.maxAnalysisLCSIC;
		}
		return s;
	}
}