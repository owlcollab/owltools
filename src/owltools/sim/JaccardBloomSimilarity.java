package owltools.sim;

import java.util.Collection;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObject;

import owltools.util.BloomFilter;

public class JaccardBloomSimilarity extends Similarity {

	public JaccardBloomSimilarity() {
		super();
		minScore = 0.2; // default
	}

	
	@Override
	public void calculate(SimEngine simEngine, OWLObject a, OWLObject b) {
		this.simEngine = simEngine;
		Set<OWLObject> ancs = simEngine.getGraph().getAncestorsReflexive(a);
		BloomFilter bloomFilter = new BloomFilter<OWLObject>(0.05, ancs.size());
		bloomFilter.addAll(ancs);

		Set<OWLObject>  cu = simEngine.getGraph().getAncestorsReflexive(b);
		Collection<OWLObject> iAtts = bloomFilter.intersection(cu);
		setScore( ((double)iAtts.size())  / ancs.size());
	}

	@Override
	protected void translateResultsToOWLAxioms(String id,
			OWLNamedIndividual result, Set<OWLAxiom> axioms) {
		// TODO Auto-generated method stub
		
	}

}
