package org.bbop.graph;

import java.awt.Paint;

import org.semanticweb.owlapi.model.OWLObjectProperty;

public interface TypeColorManager {

	public Paint getColor(OWLObjectProperty type);

}
