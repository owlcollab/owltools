package owltools.sim2;

import org.semanticweb.owlapi.model.OWLClass;

public class UnknownOWLClassException extends Exception {

	public UnknownOWLClassException(OWLClass ac) {
		super("Unknown class: "+ac);
	}

}
