package owltools.phenolog;

import java.util.HashSet;
import java.util.Set;

//import org.semanticweb.owlapi.model.OWLObject;

/**
 * represents an attribute-bearing entity; for example, a gene.
 */
public class Individual implements Comparable<Individual>{

    private String id;
    private String label;
    private int orthologs = 0;
    //private OWLObject owlObject; // for future use
    private Set<Attribute> attributes = new HashSet<Attribute>();

    // @Override
    public boolean equals(Object oind) {
    	if (oind == null) {
			return false;
		}
    	if (oind instanceof Individual == false) {
    		return false;
    	}
        Individual ind = (Individual)oind;
        return getId().equals(ind.getId());
    }

     @Override
    public int hashCode() {
        return id.hashCode();
    }

     public int compareTo(Individual ind) {
        return id.compareTo(ind.getId());
    }

    public Individual(String id, String label, Set<Attribute> attributes) {
        this.id = id;
        this.label = label;
        this.attributes = attributes;
    }

    public Individual(String id, Set<Attribute> attributes) {
        this.id = id;
        this.label = label;
        this.attributes = attributes;
    }

    public Individual(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public Individual(String id, String label, int orthologs) {
        this.id = id;
        this.label = label;
        this.orthologs = orthologs;
    }

    public Individual(String id) {
        this.id = id;
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

    public int getOrthologs() {
        return orthologs;
    }

    public void setOrthologs(int orthologs) {
        this.orthologs = orthologs;
    }

    public Set<Attribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(Set<Attribute> attributes) {
        this.attributes = attributes;
    }
}
