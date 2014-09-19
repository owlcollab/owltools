package owltools.sim2.kb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.moment.Variance;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;

import owltools.sim2.UnknownOWLClassException;

/**
 * @author Tudor Groza
 *
 * Computes the variance of a given phenotypic profile against a reference model.
 * Intuitively, the variance should be lower when computed against a close matching
 * reference model than when computed against a random model. This intuition may 
 * not hold because of the possibly fairly uniform IC means of the reference models.
 * It may, however, be useful when computed per abnormality branch.
 * 
 * Idea: Perhaps a p-value computed using a single-sided or paired t-test might 
 * make more sense.
 */
public class OwlSimVariance {

	private OWLSimReferenceBasedStatistics refBasedStats;

	public OwlSimVariance(OWLSimReferenceBasedStatistics refBasedStats) throws UnknownOWLClassException {
		this.refBasedStats = refBasedStats;
	}

	public double getVarianceValue(Set<OWLClass> candidates, IRI referenceEntity) throws OwlSimVarianceEntityReferenceNotFoundException {
		// Reference entity does not exist - this should probably not happen.
		if (!refBasedStats.getReferenceStats().containsKey(referenceEntity)) {
			throw new OwlSimVarianceEntityReferenceNotFoundException(referenceEntity);
		}
		
		// Create IC list for candidates provided
		double[] icData = refBasedStats.retrieveCandidatesIC(candidates);
		// Return variance against the given reference concept
		return new Variance().evaluate(icData, refBasedStats.getReferenceStats().get(referenceEntity).getMean());
	}

	/**
	 * This implementation can be heavily optimized
	 */
	public Map<String, Double> getTopNVarianceValues(Set<OWLClass> candidates, int n) {
		double[] icData = refBasedStats.retrieveCandidatesIC(candidates);

		// Compute variance against all cached reference entities
		Map<IRI, Double> iResult = new LinkedHashMap<IRI, Double>();
		List<Double> list = new ArrayList<Double>();
		for (IRI reference : refBasedStats.getReferenceStats().keySet()) {
			double variance = new Variance().evaluate(icData, refBasedStats.getReferenceStats().get(reference).getMean());
			list.add(variance);
			iResult.put(reference, variance);
		}
		// Sort variances ascendantly
		Collections.sort(list);
		
		// Return top-N variances
		
		int iterSize = n < list.size() ? n : list.size();
		Map<String, Double> result = new LinkedHashMap<String, Double>();
		for (int i = 0 ;i < iterSize ; i++) {
			double varValue = list.get(i);
			List<String> refURIs = findRefURIs(varValue, iResult);
			for (String uri : refURIs) {
				result.put(uri, varValue);
			}
		}
		
		return result;
	}

	private List<String> findRefURIs(double varValue, Map<IRI, Double> iResult) {
		List<String> list = new ArrayList<String>();

		for (IRI iri : iResult.keySet()) {
			if (iResult.get(iri).doubleValue() == varValue) {
				list.add(iri.toString());
			}
		}
		
		return list;
	}

	public Map<IRI, Double> getVarianceValue(Set<OWLClass> candidates,
			IRI referenceEntity, Set<OWLClass> topLevelReferenceEntities) throws OwlSimVarianceEntityReferenceNotFoundException, UnknownOWLClassException {
		Map<IRI, Double> varianceResult = new HashMap<IRI, Double>();
		
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
				double variance = new Variance().evaluate(icData, stats.getMean());
				varianceResult.put(topLevelIRI, variance);
			}
		}
		
		return varianceResult;
	}
}
