package org.bbop.graph;

import java.util.Collection;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;

public interface LinkDatabase {

	public static class Link {
		
		private final OWLObject source;
		private final OWLObject target;
		private final OWLObjectProperty property;
		
		public Link(OWLObject source, OWLObject target, OWLObjectProperty property) {
			super();
			this.source = source;
			this.target = target;
			this.property = property;
		}
	
		public OWLObject getSource() {
			return source;
		}
	
		public OWLObject getTarget() {
			return target;
		}
	
		public OWLObjectProperty getProperty() {
			return property;
		}
	
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((property == null) ? 0 : property.hashCode());
			result = prime * result
					+ ((source == null) ? 0 : source.hashCode());
			result = prime * result
					+ ((target == null) ? 0 : target.hashCode());
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
			Link other = (Link) obj;
			if (property == null) {
				if (other.property != null) {
					return false;
				}
			} else if (!property.equals(other.property)) {
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
		
	}

	public OWLObject getObject(IRI iri);
	
	public Collection<LinkDatabase.Link> getChildren(OWLObject lo);

	public Collection<LinkDatabase.Link> getParents(OWLObject lo);
	
	public Collection<OWLObject> getObjects();

	public boolean hasChildren(OWLObject lo);
	
	public boolean hasParents(OWLObject lo);
	
	public Set<OWLObject> getRoots();
	
	public Set<OWLObject> getDescendants(OWLObject term, boolean includeSelf);
	
	public Set<OWLObject> getAncestors(OWLObject term, boolean includeSelf);
	
	public Set<OWLObject> getAncestors(Set<OWLObject> terms, boolean includeSelf);
}
