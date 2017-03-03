package owltools.mooncat;

import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.reasoner.Node;

public class IncoherentOntologyException extends Exception {

    public IncoherentOntologyException(Set<OWLClass> unsats) {
        super("unsatisfiable: "+unsats);
    }

}
