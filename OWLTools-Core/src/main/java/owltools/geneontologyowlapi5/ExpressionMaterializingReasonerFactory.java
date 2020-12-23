package owltools.geneontologyowlapi5;

import org.geneontology.reasoner.OWLExtendedReasonerFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.BufferingMode;
import org.semanticweb.owlapi.reasoner.IllegalConfigurationException;
import org.semanticweb.owlapi.reasoner.OWLReasonerConfiguration;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;

/**
 * Simple OWL-API {@link OWLReasonerFactory} for instantiating an
 * {@link ExpressionMaterializingReasoner}.
 */
public class ExpressionMaterializingReasonerFactory implements OWLExtendedReasonerFactory<ExpressionMaterializingReasoner> {

	private final OWLReasonerFactory factory;
    public ExpressionMaterializingReasonerFactory(OWLReasonerFactory factory) {
		super();
		this.factory = factory;
	}

	public String getReasonerName() {
        return "ExpressionMaterializingReasoner";
    }

    public ExpressionMaterializingReasoner createNonBufferingReasoner(OWLOntology ontology) {
        return createNonBufferingReasoner(ontology, new SimpleConfiguration());
    }

    public ExpressionMaterializingReasoner createReasoner(OWLOntology ontology) {
        return createReasoner(ontology, new SimpleConfiguration());
    }

    public ExpressionMaterializingReasoner createNonBufferingReasoner(OWLOntology ontology, OWLReasonerConfiguration config) throws IllegalConfigurationException {
        ExpressionMaterializingReasoner r = new ExpressionMaterializingReasoner(ontology, factory, config, BufferingMode.NON_BUFFERING);
        return r;
    }

    public ExpressionMaterializingReasoner createReasoner(OWLOntology ontology, OWLReasonerConfiguration config) throws IllegalConfigurationException {
    	ExpressionMaterializingReasoner r = new ExpressionMaterializingReasoner(ontology, factory, config, BufferingMode.BUFFERING);
        return r;
    }
}
