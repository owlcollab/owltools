package owltools.sim;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObject;

/**
 * MaximumInformationContentSimilarity(a,b) = max { IC(LCA_1(a,b)) ... IC(LCA_n(a,b)) }
 * 
 * E.g. for GO model, this would check all named LCAs (i.e. GO terms) and use the one with the highest IC
 * 
 * 
 * 
 * @author cjm
 *
 */
public class MaximumInformationContentSimilarity extends Similarity {

	private static Logger LOG = Logger.getLogger(MaximumInformationContentSimilarity.class);
	public Set<OWLObject> bestSubsumers = new HashSet<OWLObject>();
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (OWLObject obj : bestSubsumers) {
			sb.append(obj+"; ");
		}
		return score + " "+sb.toString();
	}

	@Override
	public void calculate(SimEngine simEngine, OWLObject a, OWLObject b) {
		this.simEngine = simEngine;
		Set<OWLObject> objs = simEngine.getCommonSubsumers(a, b);
		double maxIC = 0;
		Set<OWLObject> bestSubsumers = new HashSet<OWLObject>();
		for (OWLObject obj : objs) {
			if (!simEngine.hasInformationContent(obj))
				continue;
			double ic = simEngine.getInformationContent(obj);
			if (ic > maxIC) {
				// warning: FP arithmetic
				bestSubsumers = new HashSet<OWLObject>();
				maxIC = ic;
			}
			if (ic >= maxIC) {
				bestSubsumers.add(obj);
			}
		}
		setScore(maxIC);
		this.bestSubsumers = bestSubsumers;

	}

	@Override
	protected void translateResultsToOWLAxioms(String id,
			OWLNamedIndividual result, Set<OWLAxiom> axioms) {
		// TODO Auto-generated method stub
		
	}
}
