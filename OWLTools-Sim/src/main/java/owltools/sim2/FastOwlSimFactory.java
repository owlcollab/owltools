package owltools.sim2;

import org.semanticweb.owlapi.model.OWLOntology;

public class FastOwlSimFactory implements OwlSimFactory {

	@Override
	public OwlSim createOwlSim(OWLOntology o) {
		return new FastOwlSim(o);
	}
	
}
