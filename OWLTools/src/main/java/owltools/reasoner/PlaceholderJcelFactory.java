package owltools.reasoner;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.IllegalConfigurationException;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerConfiguration;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;

import de.tudresden.inf.lat.jcel.owlapi.main.JcelReasoner;

public class PlaceholderJcelFactory implements OWLReasonerFactory {

	public String getReasonerName() {
		return "JCEL Reasoner";
	}

	public OWLReasoner createNonBufferingReasoner(OWLOntology ontology) {
		return createNonBufferingReasoner(ontology, new SimpleConfiguration());
	}

	public OWLReasoner createReasoner(OWLOntology ontology) {
		return createReasoner(ontology, new SimpleConfiguration());
	}

	// note that currently buffering and non-buffering make no difference for now
	public OWLReasoner createNonBufferingReasoner(OWLOntology ontology, OWLReasonerConfiguration config) throws IllegalConfigurationException {
		JcelReasoner reasoner = new JcelReasoner(ontology);
		reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
		return reasoner;
	}

	public OWLReasoner createReasoner(OWLOntology ontology, OWLReasonerConfiguration config) throws IllegalConfigurationException {
		JcelReasoner reasoner = new JcelReasoner(ontology);
		reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
		return reasoner;
	}
}
