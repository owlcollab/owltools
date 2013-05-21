package owltools.sim;

import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObject;

public class OverlapSimilarity extends Similarity {

	@Override
	public void calculate(SimEngine simEngine, OWLObject a, OWLObject b) {
		setScore(simEngine.getCommonSubsumersSize(a, b));

	}

	@Override
	protected void translateResultsToOWLAxioms(String id,
			OWLNamedIndividual result, Set<OWLAxiom> axioms) {
		// TODO Auto-generated method stub
		
	}

}
