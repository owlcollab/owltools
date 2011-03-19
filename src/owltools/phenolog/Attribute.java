package owltools.phenolog;

//import org.semanticweb.owlapi.model.OWLObject;

/**
 * Represents a characteristic of an individual.
 * This could be represented using an ID for an ontology class.
 *
 */
public class Attribute {

	private String id;
	private String label;
	// private OWLObject owlObject; // for future use

        public Attribute(String id, String label){
            this.id = id;
            this.label = label;
        }

        public Attribute(String id){
            this.id = id;
            this.label = label;
        }

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}

    /*public OWLObject getOwlObject() {
    return owlObject;
    }
    public void setOwlObject(OWLObject owlObject) {
    this.owlObject = owlObject;
    }*/


}
