package org.geneontology.lego.model;

import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;

public class LegoLink {

	private final OWLNamedIndividual source;
	private final OWLNamedIndividual namedTarget;
	private final OWLObjectPropertyExpression property;
	
	/**
	 * @param source
	 * @param namedTarget
	 * @param property
	 */
	public LegoLink(OWLNamedIndividual source, OWLNamedIndividual namedTarget, OWLObjectPropertyExpression property) {
		this.source = source;
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

	/**
	 * @return the source
	 */
	public OWLNamedIndividual getSource() {
		return source;
	}
}
