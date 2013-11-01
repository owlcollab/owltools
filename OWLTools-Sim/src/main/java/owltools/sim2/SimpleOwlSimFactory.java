package owltools.sim2;

import org.semanticweb.owlapi.model.OWLOntology;

public class SimpleOwlSimFactory implements OwlSimFactory {

	@Override
	public OwlSim createOwlSim(OWLOntology o) {
		return new SimpleOwlSim(o);
	}
	
}
