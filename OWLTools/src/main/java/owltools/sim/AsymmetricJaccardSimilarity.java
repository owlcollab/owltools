package owltools.sim;

import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObject;

/**
 * 
 * 
 * @author cjm
 *
 */
public class AsymmetricJaccardSimilarity extends Similarity {

	int ci;
	int cu;
	public AsymmetricJaccardSimilarity() {
		super();
		minScore = 0.2; // default
	}


	@Override
	public void calculate(SimEngine simEngine, OWLObject a, OWLObject b) {
		this.simEngine = simEngine;
		ci = simEngine.getCommonSubsumersSize(a, b);
		cu = simEngine.getGraph().getAncestorsReflexive(a).size();
		setScore( ((double)ci) / cu );
	}

	@Override
	protected void translateResultsToOWLAxioms(String id,
			OWLNamedIndividual result, Set<OWLAxiom> axioms) {
		// TODO Auto-generated method stub

	}

	// -------------
	// REPORTING
	// -------------
	public void report(Reporter r) {
		r.report(this,"pair_match_jaccard_overlap_union",a,b,score,ci,cu);
	}

}
