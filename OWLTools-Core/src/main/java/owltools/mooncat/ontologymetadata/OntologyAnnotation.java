package owltools.mooncat.ontologymetadata;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLLiteral;

public class OntologyAnnotation {
	public OntologyAnnotation(OWLAnnotation ann) {
		property = ann.getProperty().getIRI().toString();
		OWLAnnotationValue v = ann.getValue();
		if (v instanceof IRI) {
			value = v.toString();
		}
		else {
			OWLLiteral lit = ((OWLLiteral)v);
			value = lit.getLiteral().toString();
		}
	}
	String property;
	Object value;
}
