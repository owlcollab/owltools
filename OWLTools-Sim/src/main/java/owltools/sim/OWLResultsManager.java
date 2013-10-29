package owltools.sim;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.sim2.OwlSim.ScoreAttributeSetPair;


public class OWLResultsManager {
	OWLOntology resultsOntology = null;
	
	public OWLNamedIndividual storeResult(OWLNamedIndividual i, OWLNamedIndividual j,
			ScoreAttributeSetPair sap, String type) {
		OWLNamedIndividual match = 
			getOWLDataFactory().getOWLNamedIndividual(newIRI());
		return match;
	}
	
	private IRI newIRI() {
		// TODO Auto-generated method stub
		return null;
	}

	OWLDataFactory getOWLDataFactory() {
		return resultsOntology.getOWLOntologyManager().getOWLDataFactory();
	}
}
