package owltools.sim2.kb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.graph.OWLGraphWrapper;
import owltools.sim2.OwlSim;
import owltools.sim2.UnknownOWLClassException;

public class OWLSimReferenceBasedStatistics {

	private Logger LOG = Logger.getLogger(OwlSimVariance.class);

	private OwlSim owlsim;
	private OWLOntology sourceOntology;
	private OWLGraphWrapper ontologyGraph;
	
	private Map<IRI, DescriptiveStatistics> referenceStats;

	/**
	 * Assuming owlsim, ontologyGraph and sourceOntology objects already initialized
	 * @throws UnknownOWLClassException 
	 */
	public OWLSimReferenceBasedStatistics(OwlSim owlsim, OWLOntology sourceOntology, OWLGraphWrapper ontologyGraph) throws UnknownOWLClassException {
		this.owlsim = owlsim;
		this.sourceOntology = sourceOntology;
		this.ontologyGraph = ontologyGraph;
		
		referenceStats = new HashMap<IRI, DescriptiveStatistics>();
		computeReferenceStats();
	}

	private void computeReferenceStats() throws UnknownOWLClassException {
		LOG.info("Computing variance for reference entities ...");

		// Compute and cache raw stats for all possible reference entities / disorders
		for (OWLNamedIndividual reference : sourceOntology.getIndividualsInSignature()) {
			DescriptiveStatistics stats = this.computeDescriptiveStatistics(reference);
			referenceStats.put(reference.getIRI(), stats);
		}
	}

	protected Set<OWLClass> getAttributes(IRI referenceEntity) throws UnknownOWLClassException {
		OWLDataFactory g = sourceOntology.getOWLOntologyManager().getOWLDataFactory();
		return owlsim.getAttributesForElement(g.getOWLNamedIndividual(referenceEntity));
	}
	
	protected DescriptiveStatistics computeDescriptiveStatistics(OWLNamedIndividual referenceEntity) throws UnknownOWLClassException  {
		Set<OWLClass> atts = owlsim.getAttributesForElement(referenceEntity);
		return computeDescriptiveStatistics(atts);
	}	

	protected DescriptiveStatistics computeDescriptiveStatistics(Set<OWLClass> attributes) throws UnknownOWLClassException  {
		DescriptiveStatistics statsPerAttSet = new DescriptiveStatistics();
		OWLDataFactory g = sourceOntology.getOWLOntologyManager().getOWLDataFactory();

		for (OWLClass c : attributes) {
			Double ic;
			try {
				ic = owlsim.getInformationContentForAttribute(c);
				if (ic == null) { 
					if (g.getOWLClass(c.getIRI()) != null) {
						ic = owlsim.getSummaryStatistics().max.getMax();
					} else {
						throw new UnknownOWLClassException(c); }
				}
				if (ic.isInfinite() || ic.isNaN()) {
					ic = owlsim.getSummaryStatistics().max.getMax();
				}
				statsPerAttSet.addValue(ic);	

			} catch (UnknownOWLClassException e) {
				LOG.info("Unknown class "+c.toStringID()+" submitted for summary stats. Removed from calculation.");
				continue;
			}
		}
		return statsPerAttSet;
	}	

	protected Map<IRI, DescriptiveStatistics> getReferenceStats() {
		return referenceStats;
	}

	protected double[] retrieveCandidatesIC(Set<OWLClass> candidates) {
		List<Double> icList = new ArrayList<Double>();

		// Create IC list for candidates provided
		for (OWLClass c : candidates) {
			Double ic;
			try {
				ic = owlsim.getInformationContentForAttribute(c);
				if (ic == null) { 
					if (ontologyGraph.getOWLClass(c.getIRI()) != null) {
						ic = owlsim.getSummaryStatistics().max.getMax();
					} else {
						throw new UnknownOWLClassException(c); }
				}
				if (ic.isInfinite() || ic.isNaN()) {
					ic = owlsim.getSummaryStatistics().max.getMax();
				}
				
				icList.add(ic);
			} catch (UnknownOWLClassException e) {
				LOG.info("Unknown class "+c.toStringID()+" submitted for summary stats. Removed from calculation.");
				continue;
			}
		}
		
		double[] result = new double[icList.size()];
		for (int i = 0 ;i < icList.size(); i++) {
			result[i] = icList.get(i).doubleValue();
		}
		return result;
	}
	
	protected Map<IRI, Map<IRI, OWLClass>> getDescendants(Set<OWLClass> topLevelReferenceEntities) {
		Map<IRI, Map<IRI, OWLClass>> result = new HashMap<IRI, Map<IRI,OWLClass>>();
		for (OWLClass referenceEntity : topLevelReferenceEntities) {
			result.put(referenceEntity.getIRI(), new SubclassRetriever(referenceEntity, sourceOntology).getSubClasses());
		}
		return result;
	}
	
	public double getVariance(Set<OWLClass> candidates, IRI referenceEntity) throws OwlSimVarianceEntityReferenceNotFoundException, UnknownOWLClassException {
		return new OwlSimVariance(this).getVarianceValue(candidates, referenceEntity);
	}
	
	public Map<String, Double> getTopNVariances(Set<OWLClass> candidates, int n) throws UnknownOWLClassException {
		return new OwlSimVariance(this).getTopNVarianceValues(candidates, n);
	}

	public Map<IRI, Double> getVariance(Set<OWLClass> candidates, IRI referenceEntity, Set<OWLClass> topLevelReferenceEntities) throws OwlSimVarianceEntityReferenceNotFoundException, UnknownOWLClassException {
		return new OwlSimVariance(this).getVarianceValue(candidates, referenceEntity, topLevelReferenceEntities);
	}

	public PValue getPValue(Set<OWLClass> candidates, IRI referenceEntity) throws OwlSimVarianceEntityReferenceNotFoundException, UnknownOWLClassException {
		return new OwlSimPValue(this).getPValue(candidates, referenceEntity);
	}

	public Map<IRI, PValue> getPValue(Set<OWLClass> candidates, IRI referenceEntity, Set<OWLClass> topLevelReferenceEntities) throws OwlSimVarianceEntityReferenceNotFoundException, UnknownOWLClassException {
		return new OwlSimPValue(this).getPValue(candidates, referenceEntity, topLevelReferenceEntities);
	}

	//Sampling rate in IC values - e.g., 0.05
	public ICDistribution getICDistribution(Set<OWLClass> candidates, IRI referenceEntity, double samplingRate) throws OwlSimVarianceEntityReferenceNotFoundException {
		if (!referenceStats.containsKey(referenceEntity)) {
			throw new OwlSimVarianceEntityReferenceNotFoundException(referenceEntity);
		}

		// Create IC list for candidates provided
		double[] icData = this.retrieveCandidatesIC(candidates);
		return new ICDistribution(icData, referenceStats.get(referenceEntity), samplingRate);
	}
	
	public Map<IRI, ICDistribution> getICDistribution(Set<OWLClass> candidates, IRI referenceEntity, Set<OWLClass> topLevelReferenceEntities, double samplingRate) throws OwlSimVarianceEntityReferenceNotFoundException, UnknownOWLClassException {
		if (!referenceStats.containsKey(referenceEntity)) {
			throw new OwlSimVarianceEntityReferenceNotFoundException(referenceEntity);
		}
		
		Map<IRI, ICDistribution> result = new HashMap<IRI, ICDistribution>();
		Map<IRI, Map<IRI, OWLClass>> subClasses = this.getDescendants(topLevelReferenceEntities);
		Set<OWLClass> attributes = this.getAttributes(referenceEntity);

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
				double[] icData = this.retrieveCandidatesIC(candidateSubset);
				DescriptiveStatistics stats = this.computeDescriptiveStatistics(referenceSubset);
				
				result.put(topLevelIRI, new ICDistribution(icData, stats, samplingRate));
			}
		}

		return result;
	}
	
}
