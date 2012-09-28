package org.bbop.graph.collapse;

import java.util.Collection;
import java.util.Collections;
import java.util.EventObject;

import org.semanticweb.owlapi.model.OWLObject;

public class ExpansionEvent extends EventObject {

	// generated
	private static final long serialVersionUID = -383163813445991837L;
	
	private final Collection<OWLObject> shown;
	private final Collection<OWLObject> hidden;	
	
	public ExpansionEvent(Object source, Collection<OWLObject> shown, Collection<OWLObject> hidden) {
		super(source);
		if (shown == null)
			shown = Collections.emptyList();
		if (hidden == null)
			hidden = Collections.emptyList();
		this.shown = shown;
		this.hidden = hidden;
	}

	public Collection<OWLObject> getShown() {
		return shown;
	}

	public Collection<OWLObject> getHidden() {
		return hidden;
	}
}
