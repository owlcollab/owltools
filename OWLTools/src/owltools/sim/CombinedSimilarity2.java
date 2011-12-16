package owltools.sim;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObject;

public class CombinedSimilarity2 extends Similarity {

	double jaccardScore;
	int overlapScore;
	int unionScore;
	double overlapOverSetAScore;
	double overlapOverSetBScore;
	double overlapOverSetAPlusBScore;

	public CombinedSimilarity2() {
		super();
		minScore = 0.2; // default
	}


	@Override
	public void calculate(SimEngine simEngine, OWLObject a, OWLObject b) {
		this.simEngine = simEngine;
		Set<OWLObject> setA = simEngine.getAttributeClosureFor(a);
		Set<OWLObject> setB = simEngine.getAttributeClosureFor(b);
		Set<OWLObject> setU = new HashSet<OWLObject>();
		setU.addAll(setA);
		setU.addAll(setB);
		Set<OWLObject> setI = new HashSet<OWLObject>();
		setI.addAll(setA);
		setI.retainAll(setB);

		overlapScore = setI.size();
		unionScore = setU.size();
		jaccardScore = ((double)overlapScore) / unionScore;
		overlapOverSetAScore = ((double)overlapScore) / setA.size();
		overlapOverSetBScore = ((double)overlapScore) / setB.size();
		overlapOverSetAPlusBScore = ((double)overlapScore) / (setA.size()+setB.size());
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
		s.println("JACCARD/B: "+overlapOverSetBScore);
		s.println("JACCARD/A+B: "+overlapOverSetAPlusBScore);
	}

}
