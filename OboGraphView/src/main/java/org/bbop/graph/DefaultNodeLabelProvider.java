package org.bbop.graph;

import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphWrapper;

public class DefaultNodeLabelProvider implements NodeLabelProvider {
	
	private final OWLGraphWrapper graph;

	public DefaultNodeLabelProvider(OWLGraphWrapper graph) {
		this.graph = graph;
	}

	@Override
	public String getLabel(OWLObject lo) {
		return graph.getLabelOrDisplayId(lo);
	}

}
