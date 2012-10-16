package org.geneontology.lego.model;

import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;

public class LegoLink {

	private final OWLNamedIndividual namedTarget;
	private final OWLObjectPropertyExpression property;
	
	/**
	 * @param namedTarget
	 * @param property
	 */
	public LegoLink(OWLNamedIndividual namedTarget, OWLObjectPropertyExpression property) {
		this.namedTarget = namedTarget;
		this.property = property;
	}

	/**
	 * @return the namedTarget
	 */
	public OWLNamedIndividual getNamedTarget() {
		return namedTarget;
	}

	/**
	 * @return the property
	 */
	public OWLObjectPropertyExpression getProperty() {
		return property;
	}
}
