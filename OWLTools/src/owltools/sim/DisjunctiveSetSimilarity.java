package owltools.sim;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObject;

/**
 * @author cjm
 *
 */
public class DisjunctiveSetSimilarity extends Similarity {

	private static Logger LOG = Logger.getLogger(DisjunctiveSetSimilarity.class);
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
		Set<OWLObject> objs = simEngine.getLeastCommonSubsumers(a, b);
		LOG.info("LCSs:"+objs.size());
		double totalIC = 0;
		for (OWLObject obj : objs) {
			if (!simEngine.hasInformationContent(obj))
				continue;
			double ic = simEngine.getInformationContent(obj);
			
			LOG.info("  IC:"+obj+" is "+ic);

			totalIC += ic;
		}
		setScore(totalIC);
		this.bestSubsumers = bestSubsumers;

	}

	@Override
	protected void translateResultsToOWLAxioms(String id,
			OWLNamedIndividual result, Set<OWLAxiom> axioms) {
		// TODO Auto-generated method stub
		
	}
	
	public void print(PrintStream s) {
		s.println(toString());
	}

}
