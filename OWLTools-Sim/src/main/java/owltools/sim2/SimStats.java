package owltools.sim2;

import java.util.HashMap;

import owltools.sim2.SimpleOwlSim.Metric;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

/**
 * Will keep track of the overall stats of analysis in data sets. This can be
 * used for both an entire set of data, or even the individual pairs of data
 * within a dataset. This can be used to generate a report at whatever level of
 * granularity is necessary, as multiple simstats instances can be instantiated.
 * Note that mins are included here, but aren't really used yet... we can 
 * probably assume that the minimum is zero for most cases.
 * 
 * For now I'm going to overload this class with stats for all owlsim methods, 
 * but those related to attributes should be refactored in the future.
 * 
 * @author nlw
 * 
 */
public class SimStats {

	public HashMap<SimpleOwlSim.Metric,SummaryStatistics> analysisStats = new HashMap<SimpleOwlSim.Metric,SummaryStatistics>();
	    
  public SummaryStatistics individualsIC;

  public SummaryStatistics attributesIC;
  
	public int classPairCount;

	public int uniqueClassPairCount;

	public int individualPairCount;

	public int uniqueClassCount;

	public SimStats() {
		init();
	}

	public void init() {

		this.classPairCount = 0;
		this.uniqueClassPairCount = 0;
		this.individualPairCount = 0;
		this.uniqueClassCount = 0;
		
		//make a placeholder for each metric
		for (SimpleOwlSim.Metric m : SimpleOwlSim.Metric.values()) {			
			analysisStats.put(m, new SummaryStatistics());
		}
		  		
		individualsIC = new SummaryStatistics();
		attributesIC = new SummaryStatistics();
	  	
	}
	
	public void addIndividualIC(double s) {
		individualsIC.addValue(s);
	}

	public void addAttributeIC(double s) {
		attributesIC.addValue(s);
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

	public void setValue(Metric m, double s) {
		this.analysisStats.get(m).addValue(s);
	}

	public double getMax(Metric m) {
		return this.analysisStats.get(m).getMax();
	}

}