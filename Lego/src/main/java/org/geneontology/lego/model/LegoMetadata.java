package org.geneontology.lego.model;

import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAnnotationValueVisitorEx;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;

public class LegoMetadata {

	private Set<String> dates = null;
	private Set<String> contributors = null;
	private Set<String> sources = null;
	private Set<String> evidence = null;

	public LegoMetadata() {
		super();
	}

	/**
	 * @return the contributors
	 */
	public Set<String> getContributors() {
		return contributors;
	}

	/**
	 * @param contributors the contributors to set
	 */
	public void setContributors(Set<String> contributors) {
		this.contributors = contributors;
	}

	/**
	 * @return the dates
	 */
	public Set<String> getDates() {
		return dates;
	}

	/**
	 * @param dates the dates to set
	 */
	public void setDates(Set<String> dates) {
		this.dates = dates;
	}

	/**
	 * @return the sources
	 */
	public Set<String> getSources() {
		return sources;
	}

	/**
	 * @param sources the sources to set
	 */
	public void setSources(Set<String> sources) {
		this.sources = sources;
	}

	/**
	 * @return the evidence
	 */
	public Set<String> getEvidence() {
		return evidence;
	}

	/**
	 * @param evidence the evidence to set
	 */
	public void setEvidence(Set<String> evidence) {
		this.evidence = evidence;
	}
	
	private static class LiteralValueVisitor implements OWLAnnotationValueVisitorEx<String> {
		
		static LiteralValueVisitor INSTANCE = new LiteralValueVisitor();
		
		@Override
		public String visit(IRI iri) {
			return null;
		}

		@Override
		public String visit(OWLAnonymousIndividual individual) {
			return null;
		}

		@Override
		public String visit(OWLLiteral literal) {
			return literal.getLiteral();
		}
	}

	public static String getTitle(Iterable<OWLAnnotation> annotations) {
		if (annotations != null) {
			for (OWLAnnotation annotation : annotations) {
				String propertyId = annotation.getProperty().getIRI()
						.toString();
				OWLAnnotationValue annValue = annotation.getValue();
				String value = annValue.accept(LiteralValueVisitor.INSTANCE);
				if (value != null) {
					if ("http://purl.org/dc/elements/1.1/title"
							.equals(propertyId)) {
						return value;
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * @param node
	 * @param axioms
	 */
	public static void extractMetadata(LegoMetadata node, Iterable<? extends OWLAxiom> axioms) {
		for (OWLAxiom axiom : axioms) {
			extractMetadata(node, axiom);
		}
	}
	
	/**
	 * @param node
	 * @param axiom
	 */
	public static void extractMetadata(LegoMetadata node, OWLAxiom axiom) {
		// extract meta data from annotations
		Set<OWLAnnotation> annotations = axiom.getAnnotations();
		for (OWLAnnotation annotation : annotations) {
			String propertyId = annotation.getProperty().getIRI().toString();
			OWLAnnotationValue annValue = annotation.getValue();
			String value = annValue.accept(LiteralValueVisitor.INSTANCE);
			if (value != null) {
				if ("http://geneontology.org/lego/evidence".equals(propertyId)) {
					Set<String> evidence = node.getEvidence();
					if (evidence == null) {
						evidence = new HashSet<String>();
						node.setEvidence(evidence);
					}
					evidence.add(value);
				}
				else if ("http://purl.org/dc/elements/1.1/date".equals(propertyId)) {
					Set<String> dates = node.getDates();
					if (dates == null) {
						dates = new HashSet<String>();
						node.setDates(dates);
					}
					dates.add(value);
				}
				else if ("http://purl.org/dc/elements/1.1/source".equals(propertyId)) {
					Set<String> sources = node.getSources();
					if (sources == null) {
						sources = new HashSet<String>();
						node.setSources(sources);
					}
					sources.add(value);
				}
				else if ("http://purl.org/dc/elements/1.1/contributor".equals(propertyId)) {
						Set<String> contributors = node.getContributors();
						if (contributors == null) {
							contributors = new HashSet<String>();
							node.setContributors(contributors);
						}
						contributors.add(value);
				}
			}
		}
	}
}