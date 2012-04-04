package owltools.sim;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObject;

/**
 * filters first based on SimJ, if passes threshold performs full ConjunctiveSetSimilarity
 * 
 * @author cjm
 *
 */
public class CombinedJaccardConjunctiveSetSimilarity extends Similarity {

	private static Logger LOG = Logger.getLogger(CombinedJaccardConjunctiveSetSimilarity.class);
	private JaccardSimilarity jSim;
	private ConjunctiveSetSimilarity csSim;
	Double jSimMinScore = 0.2;
	

	@Override
	public void calculate(SimEngine simEngine, OWLObject a, OWLObject b) {
		this.simEngine = simEngine;
		jSim = new JaccardSimilarity();
		jSim.calculate(simEngine, a, b);
		LOG.info("jSim score ="+jSim.getScore());
		if (jSimMinScore != null && jSim.getScore() < jSimMinScore) {
			csSim = new ConjunctiveSetSimilarity();
			csSim.calculate(simEngine, a, b);
		}
		else {
			LOG.info("jSim score beneath threshold");
		}
	}

	@Override
	protected void translateResultsToOWLAxioms(String id,
			OWLNamedIndividual result, Set<OWLAxiom> axioms) {
		// TODO Auto-generated method stub
		
	}
	
	public void print(PrintStream s) {
		jSim.print(s);
		if (csSim != null) {
			csSim.print(s);
		}
	}

}
