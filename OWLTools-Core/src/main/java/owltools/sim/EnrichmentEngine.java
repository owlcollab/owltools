package owltools.sim;

import org.apache.commons.math.distribution.HypergeometricDistributionImpl;
import org.semanticweb.owlapi.model.OWLClass;

import owltools.sim2.SimpleOwlSim;


public class EnrichmentEngine {
	SimpleOwlSim sim;
	OWLClass populationClass;
	
	public double calculateEnrichment(OWLClass geneSetClass, OWLClass enrichedClass) {
		 // int populationSize, int numberOfSuccesses, int sampleSize
		HypergeometricDistributionImpl hg = 
			new HypergeometricDistributionImpl(
					sim.getNumElementsForAttribute(populationClass),
					sim.getNumElementsForAttribute(geneSetClass),
					sim.getNumElementsForAttribute(enrichedClass)
					);
		
		int enrichedClassSize = sim.getNumElementsForAttribute(enrichedClass);
		return hg.cumulativeProbability(enrichedClassSize);
	}
    
}
