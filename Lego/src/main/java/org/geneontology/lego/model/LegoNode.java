package org.geneontology.lego.model;

import java.util.Collection;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

public class LegoNode {

	private final OWLNamedIndividual individual;
	private final OWLClassExpression type;
	
	private OWLClass activeEntity = null;
	private Collection<OWLClassExpression> cellularLocation = null;
	private Collection<OWLClassExpression> unknowns = null;
	
	private Collection<LegoLink> links = null;
	
	private boolean cmf = false;
	private boolean mf = false;
	private boolean bp = false;
	
	/**
	 * @param individual
	 * @param type
	 */
	public LegoNode(OWLNamedIndividual individual, OWLClassExpression type) {
		this.individual = individual;
		this.type = type;
	}

	public IRI getId() {
		return individual.getIRI();
	}
	
	/**
	 * @return the individual
	 */
	public OWLNamedIndividual getIndividual() {
		return individual;
	}
	
	/**
	 * @return the type
	 */
	public OWLClassExpression getType() {
		return type;
	}
	
	/**
	 * @return the activeEntity
	 */
	public OWLClass getActiveEntity() {
		return activeEntity;
	}
	
	/**
	 * @param activeEntity the activeEntity to set
	 */
	public void setActiveEntity(OWLClass activeEntity) {
		this.activeEntity = activeEntity;
	}
	
	/**
	 * @return the cellularLocation
	 */
	public Collection<OWLClassExpression> getCellularLocation() {
		return cellularLocation;
	}
	
	/**
	 * @param cellularLocation the cellularLocation to set
	 */
	public void setCellularLocation(Collection<OWLClassExpression> cellularLocation) {
		this.cellularLocation = cellularLocation;
	}
	
	/**
	 * @return the unknowns
	 */
	public Collection<OWLClassExpression> getUnknowns() {
		return unknowns;
	}
	
	/**
	 * @param unknowns the unknowns to set
	 */
	public void setUnknowns(Collection<OWLClassExpression> unknowns) {
		this.unknowns = unknowns;
	}

	/**
	 * @return the links
	 */
	public Collection<LegoLink> getLinks() {
		return links;
	}

	/**
	 * @param links the links to set
	 */
	public void setLinks(Collection<LegoLink> links) {
		this.links = links;
	}

	/**
	 * @return the cmf
	 */
	public boolean isCmf() {
		return cmf;
	}

	/**
	 * @param cmf the cmf to set
	 */
	public void setCmf(boolean cmf) {
		this.cmf = cmf;
	}

	/**
	 * @return the mf
	 */
	public boolean isMf() {
		return mf;
	}

	/**
	 * @param mf the mf to set
	 */
	public void setMf(boolean mf) {
		this.mf = mf;
	}

	/**
	 * @return the bp
	 */
	public boolean isBp() {
		return bp;
	}

	/**
	 * @param bp the bp to set
	 */
	public void setBp(boolean bp) {
		this.bp = bp;
	}
	
}
