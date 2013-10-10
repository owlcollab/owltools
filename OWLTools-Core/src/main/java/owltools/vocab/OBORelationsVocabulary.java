package owltools.vocab;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.vocab.Namespaces;


public enum OBORelationsVocabulary {
	
	/**
	 * 
	 */
	BFO_part_of(OBONamespaces.BFO, "0000050"),
	BFO_has_part(OBONamespaces.BFO, "0000051"),
	BFO_occurs_in(OBONamespaces.BFO, "0000066"),
	RO_regulates(OBONamespaces.RO, "0002211"),
	RO_negatively_regulates(OBONamespaces.RO, "0002212"),
	RO_positively_regulates(OBONamespaces.RO, "0002213"),
	GOREL_provides_input_for(OBONamespaces.GOREL, "provides_input_for");
	

	

	final IRI iri;
	final OBONamespaces namespace;
	final String id;
	
	static final String OBO = "http://purl.obolibrary.org/obo/";
	
	OBORelationsVocabulary(OBONamespaces ns, String id) {
		this.namespace = ns;
		this.id = id;
		iri = IRI.create(OBO + ns + "_" + id);
	}

	@Override
	public String toString() {
		return iri.toString();
	}
}
