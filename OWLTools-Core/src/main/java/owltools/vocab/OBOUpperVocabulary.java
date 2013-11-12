package owltools.vocab;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.vocab.Namespaces;


public enum OBOUpperVocabulary {
	
	/**
	 * 
	 */
	GO_molecular_function(OBONamespaces.GO, "0003674"),
	GO_biological_process(OBONamespaces.GO, "0008150"),
	GO_cellular_process(OBONamespaces.GO, "0009987"),
	BFO_part_of(OBONamespaces.BFO, "0000050"),
	BFO_has_part(OBONamespaces.BFO, "0000051"),
	BFO_occurs_in(OBONamespaces.BFO, "0000066"),
	RO_regulates(OBONamespaces.RO, "0002211"),
	RO_negatively_regulates(OBONamespaces.RO, "0002212"),
	RO_positively_regulates(OBONamespaces.RO, "0002213"),
	RO_starts(OBONamespaces.RO, "0002223"),
	RO_ends(OBONamespaces.RO, "0002229"),
	GOREL_enabled_by(OBONamespaces.GOREL, "enabled_by"),
	GOREL_provides_input_for(OBONamespaces.GOREL, "provides_input_for");
	

	

	final IRI iri;
	final OBONamespaces namespace;
	final String id;
	
	public static final String OBO = "http://purl.obolibrary.org/obo/";
	
	OBOUpperVocabulary(OBONamespaces ns, String id) {
		this.namespace = ns;
		this.id = id;
		iri = IRI.create(OBO + ns + "_" + id);
	}
	
	

	public IRI getIRI() {
		return iri;
	}

	public OWLObjectProperty getObjectProperty(OWLDataFactory f) {
		return f.getOWLObjectProperty(iri);
	}
	public OWLObjectProperty getObjectProperty(OWLOntology o) {
		return getObjectProperty(o.getOWLOntologyManager().getOWLDataFactory());
	}

	@Override
	public String toString() {
		return iri.toString();
	}
}
