package owltools.reasoner;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.BufferingMode;
import org.semanticweb.owlapi.reasoner.IllegalConfigurationException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerConfiguration;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;

public class ExpressionMaterializingReasonerFactory implements OWLReasonerFactory {

	OWLReasonerFactory factory;
    public ExpressionMaterializingReasonerFactory(OWLReasonerFactory factory) {
		super();
		this.factory = factory;
	}

	public String getReasonerName() {
        return "ExpressionMaterializingReasoner";
    }

    public OWLReasoner createNonBufferingReasoner(OWLOntology ontology) {
        return createNonBufferingReasoner(ontology, new SimpleConfiguration());
    }

    public OWLReasoner createReasoner(OWLOntology ontology) {
        return createReasoner(ontology, new SimpleConfiguration());
    }

    public OWLReasoner createNonBufferingReasoner(OWLOntology ontology, OWLReasonerConfiguration config) throws IllegalConfigurationException {
        ExpressionMaterializingReasoner r = new ExpressionMaterializingReasoner(ontology, factory, config, BufferingMode.NON_BUFFERING);
        return r;
    }

    public OWLReasoner createReasoner(OWLOntology ontology, OWLReasonerConfiguration config) throws IllegalConfigurationException {
    	ExpressionMaterializingReasoner r = new ExpressionMaterializingReasoner(ontology, factory, config, BufferingMode.BUFFERING);
        return r;
    }
}
