package owltools.sim;

import java.io.PrintStream;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObject;

public class CombinedSimilarity extends Similarity {

	double jaccardScore;
	int overlapScore;
	int unionScore;
	double overlapOverSetAScore;
	double overlapOverSetBScore;

	public CombinedSimilarity() {
		super();
		minScore = 0.2; // default
	}


	@Override
	public void calculate(SimEngine simEngine, OWLObject a, OWLObject b) {
		this.simEngine = simEngine;
		Set<OWLObject> setA = simEngine.getGraph().getAncestorsReflexive(a);
		Set<OWLObject> setB = simEngine.getGraph().getAncestorsReflexive(b);
		overlapScore = simEngine.getCommonSubsumersSize(a, b);
		unionScore = simEngine.getUnionSubsumersSize(a, b);
		jaccardScore = ((double)overlapScore) / unionScore;
		overlapOverSetAScore = ((double)overlapScore) / setA.size();
		overlapOverSetBScore = ((double)overlapScore) / setB.size();
	}

	@Override
	protected void translateResultsToOWLAxioms(String id,
			OWLNamedIndividual result, Set<OWLAxiom> axioms) {
		// TODO Auto-generated method stub

	}

	public void print(PrintStream s) {
		s.println("OVERLAP: "+overlapScore);
		s.println("UNION: "+unionScore);
		s.println("JACCARD: "+jaccardScore);
		s.println("JACCARD/A: "+overlapOverSetAScore);
		s.println("JACCARD/A: "+overlapOverSetBScore);
	}

}
