package owltools.graph.shunt;

import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphWrapper;

/**
 * 
 * A node reduced to nothing; for use with OWLShuntGraph.
 * 
 * See: {@link owltools.graph.shunt.OWLShuntGraph} and {@link owltools.graph.shunt.OWLShuntEdge}.
 */
public class OWLShuntNode {

	public String id = null;
	public String type = null;
	public String label = null;
	public String metadata = null;

	/**
	 * @param id
	 */
	public OWLShuntNode(String id) {
		this.id = id;
	}

	/**
	 * @param id
	 * @param label
	 */
	public OWLShuntNode(String id, String label) {
		this.id = id;
		this.label = label;
	}
	
	/**
	 * @param g
	 * @param o
	 */
	public OWLShuntNode(OWLGraphWrapper g, OWLObject o) {
		this.id = g.getIdentifier(o);
		this.label = g.getLabel(o);
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * @param label the label to set
	 */
	public void setLabel(String label) {
		this.label = label;
	}
	
	/**
	 * @return the metadata
	 */
	public String getMetadata() {
		return metadata;
	}
	
	/**
	 * @param metadata the metadata to set
	 */
	public void setMetadata(String metadata) {
		this.metadata = metadata;
	}
	
}
