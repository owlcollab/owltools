package org.geneontology.lego.model2;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLObjectProperty;

public class LegoLink {

	private final IRI source;
	private final IRI target;
	private final OWLObjectProperty relation;
	
	/**
	 * @param source
	 * @param target
	 * @param relation
	 */
	public LegoLink(IRI source, IRI target, OWLObjectProperty relation) {
		this.source = source;
		this.target = target;
		this.relation = relation;
	}

	/**
	 * @return the source
	 */
	public IRI getSource() {
		return source;
	}

	/**
	 * @return the target
	 */
	public IRI getTarget() {
		return target;
	}

	/**
	 * @return the relation
	 */
	public OWLObjectProperty getRelation() {
		return relation;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((relation == null) ? 0 : relation.hashCode());
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		result = prime * result + ((target == null) ? 0 : target.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		LegoLink other = (LegoLink) obj;
		if (relation == null) {
			if (other.relation != null) {
				return false;
			}
		} else if (!relation.equals(other.relation)) {
			return false;
		}
		if (source == null) {
			if (other.source != null) {
				return false;
			}
		} else if (!source.equals(other.source)) {
			return false;
		}
		if (target == null) {
			if (other.target != null) {
				return false;
			}
		} else if (!target.equals(other.target)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("LegoLink [source=");
		builder.append(source);
		builder.append(", relation=");
		builder.append(relation);
		builder.append(", target=");
		builder.append(target);
		builder.append("]");
		return builder.toString();
	}
}
