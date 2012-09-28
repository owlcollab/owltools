package org.bbop.graph;

import org.semanticweb.owlapi.model.OWLObject;

public interface NodeLabelProvider {
	public String getLabel(OWLObject lo);
}
