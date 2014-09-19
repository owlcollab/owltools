package owltools.sim2.kb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.inference.TestUtils;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;

import owltools.sim2.UnknownOWLClassException;

public class OwlSimPValue {

	private OWLSimReferenceBasedStatistics refBasedStats;

	public OwlSimPValue(OWLSimReferenceBasedStatistics refBasedStats) throws UnknownOWLClassException {
		this.refBasedStats = refBasedStats;
	}

	public PValue getPValue(Set<OWLClass> candidates, IRI referenceEntity) throws OwlSimVarianceEntityReferenceNotFoundException {
		if (!refBasedStats.getReferenceStats().containsKey(referenceEntity)) {
			throw new OwlSimVarianceEntityReferenceNotFoundException(referenceEntity);
		}

		// Create IC list for candidates provided
		double[] icData = refBasedStats.retrieveCandidatesIC(candidates);
		List<double[]> sets = new ArrayList<double[]>();
		sets.add(icData);
		sets.add(refBasedStats.getReferenceStats().get(referenceEntity).getValues());

		return new PValue(TestUtils.tTest(refBasedStats.getReferenceStats().get(referenceEntity).getMean(), icData),
				TestUtils.oneWayAnovaPValue(sets), 
				TestUtils.kolmogorovSmirnovStatistic(icData, refBasedStats.getReferenceStats().get(referenceEntity).getValues()));
	}

	public Map<IRI, PValue> getPValue(Set<OWLClass> candidates,
			IRI referenceEntity, Set<OWLClass> topLevelReferenceEntities) throws OwlSimVarianceEntityReferenceNotFoundException, UnknownOWLClassException {
		Map<IRI, PValue> pValueResult = new HashMap<IRI, PValue>();
		
		if (!refBasedStats.getReferenceStats().containsKey(referenceEntity)) {
			throw new OwlSimVarianceEntityReferenceNotFoundException(referenceEntity);
		}
		Map<IRI, Map<IRI, OWLClass>> subClasses = refBasedStats.getDescendants(topLevelReferenceEntities);
		Set<OWLClass> attributes = refBasedStats.getAttributes(referenceEntity);

		for (IRI topLevelIRI : subClasses.keySet()) {
			Map<IRI, OWLClass> actualSubClasses = subClasses.get(topLevelIRI);
			Set<OWLClass> candidateSubset = new HashSet<OWLClass>();
			for (OWLClass cls : candidates) {
				if (actualSubClasses.containsKey(cls.getIRI())) {
					candidateSubset.add(cls);
				}
			}
			
			Set<OWLClass> referenceSubset = new HashSet<OWLClass>();
			for (OWLClass cls : attributes) {
				if (actualSubClasses.containsKey(cls.getIRI())) {
					referenceSubset.add(cls);
				}
			}
			
			if (!candidateSubset.isEmpty() && !referenceSubset.isEmpty()) {
				double[] icData = refBasedStats.retrieveCandidatesIC(candidateSubset);
				DescriptiveStatistics stats = refBasedStats.computeDescriptiveStatistics(referenceSubset);
				
				List<double[]> sets = new ArrayList<double[]>();
				sets.add(icData);
				sets.add(stats.getValues());

				pValueResult.put(topLevelIRI, new PValue(TestUtils.tTest(stats.getMean(), icData),
						TestUtils.oneWayAnovaPValue(sets), 
						TestUtils.kolmogorovSmirnovStatistic(icData, stats.getValues())));
			}
		}
		
		return pValueResult;
	}

	/*
	public Map<IRI, Double> getSimplePValueTopN(Set<OWLClass> candidates, int n) throws OwlSimVarianceEntityReferenceNotFoundException {
		// Create IC list for candidates provided
		List<Double> icList = this.retrieveCandidatesIC(candidates);
		double[] icData = new double[icList.size()];
		for (int i = 0 ;i < icList.size(); i++) {
			icData[i] = icList.get(i).doubleValue();
		}

		Map<IRI, Double> pValues = new HashMap<IRI, Double>();
		List<Double> list = new ArrayList<Double>();
		for (IRI refEntity : referenceStats.keySet()) {
			double pValue = TestUtils.tTest(referenceStats.get(refEntity).getMean(), icData);
			pValues.put(refEntity, pValue);
			if (!list.contains(pValue)) {
				list.add(pValue);
			}
		}
		
		Collections.sort(list);
		int actualN = n > pValues.size() ? pValues.size() : n;
		Map<IRI, Double> result = new HashMap<IRI, Double>();
		
		int count = 0;
		for (double pValue : list) {
			if (count == actualN) {
				break;
			}
			for (IRI refEntity : pValues.keySet()) {
				if (count == actualN) {
					break;
				}
				
				if (pValues.get(refEntity).doubleValue() == pValue) {
					result.put(refEntity, pValue);
					count++;
				}
			}
		}
		
		return result;
	}
	 */
}
