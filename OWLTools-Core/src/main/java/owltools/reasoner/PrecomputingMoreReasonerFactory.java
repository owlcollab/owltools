package owltools.reasoner;

import org.semanticweb.more.MOReReasonerFactory;
import org.semanticweb.more.OWL2ReasonerManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.IllegalConfigurationException;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerConfiguration;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;

public class PrecomputingMoreReasonerFactory implements OWLReasonerFactory {
	
	private final int reasonerMode;
	private final MOReReasonerFactory factory;
	
	private PrecomputingMoreReasonerFactory(int reasonerMode) {
		this.reasonerMode = reasonerMode;
		factory = new MOReReasonerFactory(reasonerMode);
	}

	public static PrecomputingMoreReasonerFactory getMoreHermitFactory() {
		return new PrecomputingMoreReasonerFactory(OWL2ReasonerManager.HERMIT);
	}
	
	public static PrecomputingMoreReasonerFactory getMoreJFactFactory() {
		return new PrecomputingMoreReasonerFactory(OWL2ReasonerManager.JFACT);
	}
	
	public String getReasonerName() {
		if (reasonerMode == OWL2ReasonerManager.JFACT) {
			return "MORe JFact Reasoner";
		}
		return "MORe HermiT Reasoner";
	}

	public OWLReasoner createNonBufferingReasoner(OWLOntology ontology) {
		return createNonBufferingReasoner(ontology, new SimpleConfiguration());
	}

	public OWLReasoner createReasoner(OWLOntology ontology) {
		return createReasoner(ontology, new SimpleConfiguration());
	}

	// note that currently buffering and non-buffering make no difference for now
	public OWLReasoner createNonBufferingReasoner(OWLOntology ontology, OWLReasonerConfiguration config) throws IllegalConfigurationException {
		OWLReasoner reasoner = factory.createNonBufferingReasoner(ontology, config);
		reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
		return reasoner;
	}

	public OWLReasoner createReasoner(OWLOntology ontology, OWLReasonerConfiguration config) throws IllegalConfigurationException {
		OWLReasoner reasoner = factory.createReasoner(ontology, config);
		reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
		return reasoner;
	}
}
