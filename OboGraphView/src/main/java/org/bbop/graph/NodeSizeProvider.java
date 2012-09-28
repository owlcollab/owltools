package org.bbop.graph;

import java.awt.Dimension;

import org.semanticweb.owlapi.model.OWLObject;

public interface NodeSizeProvider {
	public Dimension getSize(OWLObject lo);
}
