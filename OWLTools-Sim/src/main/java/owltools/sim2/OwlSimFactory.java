package owltools.sim2;

import org.semanticweb.owlapi.model.OWLOntology;

/**
 * Factory for creating OwlSim engines
 *
 */
public interface OwlSimFactory {
	public OwlSim createOwlSim(OWLOntology o);

}
