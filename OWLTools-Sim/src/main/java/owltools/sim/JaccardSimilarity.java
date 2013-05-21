package owltools.sim;

import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObject;

public class JaccardSimilarity extends Similarity {

	int ci;
	int cu;
	public JaccardSimilarity() {
		super();
		minScore = 0.2; // default
	}


	@Override
	public void calculate(SimEngine simEngine, OWLObject a, OWLObject b) {
		this.simEngine = simEngine;
		ci = simEngine.getCommonSubsumersSize(a, b);
		cu = simEngine.getUnionSubsumersSize(a, b);
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
