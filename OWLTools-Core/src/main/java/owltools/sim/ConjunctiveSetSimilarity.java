package owltools.sim;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObject;

/**
 * ConjunctiveSetSimilarity(a,b) = IC( LCA_1(a,b) ^ LCA_2(a,b) ... LCA_n(a,b) )
 * 
 * Each LCA is an ancestor class or class expression that can be reached over any properties from a and b.
 * Note that no new class expressions are generated when calculating LCA - however, existing class expressions
 * used in axioms are used.
 * 
 * 
 * it's recommended this used as a sub-method of a MultiSimilarity check
 * 
 * @author cjm
 *
 */
public class ConjunctiveSetSimilarity extends Similarity {

	private static Logger LOG = Logger.getLogger(ConjunctiveSetSimilarity.class);
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
		Double ic = simEngine.getInformationContent(objs);
		if (ic == null)
			ic = 0.0;
		setScore(ic);
		this.bestSubsumers = objs;

	}

	@Override
	protected void translateResultsToOWLAxioms(String id,
			OWLNamedIndividual result, Set<OWLAxiom> axioms) {
		// TODO Auto-generated method stub
		
	}
	
	public void print(PrintStream s) {
		s.println("IntersectionIC: "+toString()+"\n");
		for (OWLObject obj : bestSubsumers) {
			s.print("  subsumer:");
			print(s,obj);
		}

	}

}
