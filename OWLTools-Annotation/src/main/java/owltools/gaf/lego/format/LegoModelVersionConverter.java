package owltools.gaf.lego.format;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationSubject;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAnnotationValueVisitor;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividualAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.RemoveAxiom;
import org.semanticweb.owlapi.util.OWLAxiomVisitorAdapter;

import owltools.gaf.lego.CoreMolecularModelManager;
import owltools.gaf.lego.IdStringManager.AnnotationShorthand;
import owltools.util.ModelContainer;

public class LegoModelVersionConverter {
	
	static class EvidenceIndividualCache {
		
		static class EvidenceEntry {
			final IRI evidenceIRI;
			final Set<OWLAnnotation> evidenceAnnotations;
			
			EvidenceEntry(IRI evidenceIRI, Set<OWLAnnotation> evidenceAnnotations) {
				this.evidenceIRI = evidenceIRI;
				this.evidenceAnnotations = evidenceAnnotations;
			}

			@Override
			public int hashCode() {
				final int prime = 31;
				int result = 1;
				result = prime
						* result
						+ ((evidenceAnnotations == null) ? 0
								: evidenceAnnotations.hashCode());
				result = prime * result
						+ ((evidenceIRI == null) ? 0 : evidenceIRI.hashCode());
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
				EvidenceEntry other = (EvidenceEntry) obj;
				if (evidenceAnnotations == null) {
					if (other.evidenceAnnotations != null) {
						return false;
					}
				} else if (!evidenceAnnotations
						.equals(other.evidenceAnnotations)) {
					return false;
				}
				if (evidenceIRI == null) {
					if (other.evidenceIRI != null) {
						return false;
					}
				} else if (!evidenceIRI.equals(other.evidenceIRI)) {
					return false;
				}
				return true;
			}
		}
	
		private final OWLOntology abox;
		private final ModelContainer model;
		private final String modelId;
		private final List<OWLOntologyChange> changes;
		private final OWLDataFactory f;

		private final Map<EvidenceEntry, OWLNamedIndividual> individuals = new HashMap<EvidenceEntry, OWLNamedIndividual>();

		EvidenceIndividualCache(OWLOntology abox, ModelContainer model,
				String modelId, List<OWLOntologyChange> changes) {
			this.abox = abox;
			this.model = model;
			this.modelId = modelId;
			this.changes = changes;
			f = abox.getOWLOntologyManager().getOWLDataFactory();
		}
		
		OWLNamedIndividual getIndividual(IRI ecoIRI, Set<OWLAnnotation> evidenceAnnotations) {
			EvidenceEntry entry = new EvidenceEntry(ecoIRI, evidenceAnnotations);
			OWLNamedIndividual individual = individuals.get(entry);
			if (individual == null) {
				OWLClassExpression ce = f.getOWLClass(ecoIRI);
				Pair<OWLNamedIndividual, Set<OWLAxiom>> evidenceIndividualPair = CoreMolecularModelManager.createIndividual(modelId, model, ce, null);
				individual = evidenceIndividualPair.getLeft();
				for(OWLAxiom newAxiom : evidenceIndividualPair.getRight()) {
					changes.add(new AddAxiom(abox, newAxiom));
				}
				IRI individualIRI = individual.getIRI();
				for(OWLAnnotation evidenceAnnotation : evidenceAnnotations) {
					changes.add(new AddAxiom(abox, f.getOWLAnnotationAssertionAxiom(individualIRI, evidenceAnnotation)));
				}
				individuals.put(entry, individual);
			}
			return individual;
		}
	}
	
	static class EvidenceTriple {
		final Set<IRI> ecoIRIs = new HashSet<IRI>();
		final Set<OWLAnnotation> evidenceAnnotations = new HashSet<OWLAnnotation>();
		final Set<OWLAnnotation> contributors = new HashSet<OWLAnnotation>();
		
		public static EvidenceTriple createEvidenceTriple(Set<OWLAnnotationAssertionAxiom> axioms, OWLOntology abox, List<OWLOntologyChange> changes) {
			final EvidenceTriple triple = new EvidenceTriple();
			for(OWLAnnotationAssertionAxiom axiom : axioms) {
				OWLAnnotationProperty property = axiom.getProperty();
				if (AnnotationShorthand.evidence.getAnnotationProperty().equals(property.getIRI())) {
					OWLAnnotationValue value = axiom.getValue();
					value.accept(new OWLAnnotationValueVisitor() {
						
						@Override
						public void visit(OWLLiteral literal) {
							// ignore
						}
						
						@Override
						public void visit(OWLAnonymousIndividual individual) {
							// ignore
						}
						
						@Override
						public void visit(IRI iri) {
							triple.ecoIRIs.add(iri);
						}
					});
					changes.add(new RemoveAxiom(abox, axiom));
				}
				else if(AnnotationShorthand.contributor.getAnnotationProperty().equals(property.getIRI())) {
					// keep and propagate
					triple.contributors.add(axiom.getAnnotation());
				}
				else if(AnnotationShorthand.date.getAnnotationProperty().equals(property.getIRI())) {
					// keep and propagate
					triple.contributors.add(axiom.getAnnotation());
				}
				else if(AnnotationShorthand.comment.getAnnotationProperty().equals(property.getIRI())) {
					// keep only 
				}
				else {
					// remove and move to evidence individual
					changes.add(new RemoveAxiom(abox, axiom));
					triple.evidenceAnnotations.add(axiom.getAnnotation());
				}
			}
			return triple;
		}
		
		public static EvidenceTriple createEvidenceTriple(Set<OWLAnnotation> annotations) {
			final EvidenceTriple triple = new EvidenceTriple();
			for (OWLAnnotation annotation : annotations) {
				OWLAnnotationProperty property = annotation.getProperty();
				if (AnnotationShorthand.evidence.getAnnotationProperty().equals(property.getIRI())) {
					OWLAnnotationValue value = annotation.getValue();
					value.accept(new OWLAnnotationValueVisitor() {
						
						@Override
						public void visit(OWLLiteral literal) {
							// ignore
						}
						
						@Override
						public void visit(OWLAnonymousIndividual individual) {
							// ignore
						}
						
						@Override
						public void visit(IRI iri) {
							triple.ecoIRIs.add(iri);
						}
					});
				}
				else if(AnnotationShorthand.contributor.getAnnotationProperty().equals(property.getIRI())) {
					triple.contributors.add(annotation);
				}
				else if(AnnotationShorthand.date.getAnnotationProperty().equals(property.getIRI())) {
					triple.contributors.add(annotation);
				}
				else if(AnnotationShorthand.comment.getAnnotationProperty().equals(property.getIRI())) {
					triple.contributors.add(annotation);
				}
				else {
					triple.evidenceAnnotations.add(annotation);
				}
			}
			return triple;
		}
	}
	
	public void convertLegoModelToAllIndividuals(final ModelContainer model, final String modelId) {
		final OWLOntology abox = model.getAboxOntology();
		final OWLOntologyManager m = abox.getOWLOntologyManager();
		final OWLDataFactory f = m.getOWLDataFactory();
		final List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
		final Set<OWLNamedIndividual> individuals = abox.getIndividualsInSignature();
		final Set<OWLObjectPropertyAssertionAxiom> propertyAssertionAxioms = abox.getAxioms(AxiomType.OBJECT_PROPERTY_ASSERTION);
		final EvidenceIndividualCache individualCache = new EvidenceIndividualCache(abox, model, modelId, changes);
		
		// update all relevant axioms pertaining to only one individual
		for (final OWLNamedIndividual individual : individuals) {
			Set<OWLAnnotationAssertionAxiom> annotationAssertionAxioms = getAnnotationAssertionAxioms(abox, individual);
			// convert evidence into individual(s)
			// split into ECO IRI and annotations
			EvidenceTriple triple = EvidenceTriple.createEvidenceTriple(annotationAssertionAxioms, abox, changes);
			
			// create individuals for ECO IRIs and link to existing individual
			Set<IRI> evidenceIndividuals = new HashSet<IRI>();
			for(IRI ecoIRI : triple.ecoIRIs) {
				OWLNamedIndividual evidenceIndividual = individualCache.getIndividual(ecoIRI, triple.evidenceAnnotations);
				evidenceIndividuals.add(evidenceIndividual.getIRI());
				OWLAnnotationValue value = evidenceIndividual.getIRI();
				OWLAnnotation a = f.getOWLAnnotation(
						f.getOWLAnnotationProperty(AnnotationShorthand.evidence.getAnnotationProperty()), 
						value);
				changes.add(new AddAxiom(abox, f.getOWLAnnotationAssertionAxiom(individual.getIRI(), a)));
			}
			
			// convert svf types into individuals
			final Set<OWLClassAssertionAxiom> classAssertionAxioms = getClassAssertionAxioms(abox, individual);
			for(OWLClassAssertionAxiom axiom : classAssertionAxioms) {
				OWLClassExpression ce = axiom.getClassExpression();
				if (ce instanceof OWLObjectSomeValuesFrom) {
					OWLObjectSomeValuesFrom svf = (OWLObjectSomeValuesFrom) ce;
					OWLClassExpression filler = svf.getFiller();
					OWLObjectPropertyExpression property = svf.getProperty();
					
					Pair<OWLNamedIndividual,Set<OWLAxiom>> pair = CoreMolecularModelManager.createIndividual(modelId, model, filler, triple.contributors);
					OWLNamedIndividual newIndividual = pair.getLeft();
					for(OWLAxiom newAxiom : pair.getRight()) {
						changes.add(new AddAxiom(abox, newAxiom));
					}
					changes.add(new AddAxiom(abox, CoreMolecularModelManager.createFact(model, property, individual, newIndividual, triple.contributors)));
					changes.add(new RemoveAxiom(abox, axiom));
				}
			}
		}
		
		// update all (old) axioms for two individuals
		for(OWLObjectPropertyAssertionAxiom oldAxiom : propertyAssertionAxioms) {
			Set<OWLAnnotation> annotations = oldAxiom.getAnnotations();
			EvidenceTriple triple = EvidenceTriple.createEvidenceTriple(annotations);
			if (triple.ecoIRIs.isEmpty() == false) {
				// clean up axiom annotations
				changes.add(new RemoveAxiom(abox, oldAxiom));
				// add annotations for new evidence individuals
				Set<OWLAnnotation> newAnnotations = new HashSet<OWLAnnotation>(triple.contributors);
				for(IRI ecoIRI : triple.ecoIRIs) {
					OWLNamedIndividual evidenceIndividual = individualCache.getIndividual(ecoIRI, triple.evidenceAnnotations);
					newAnnotations.add(f.getOWLAnnotation(
							f.getOWLAnnotationProperty(AnnotationShorthand.evidence.getAnnotationProperty()), 
							evidenceIndividual.getIRI()));
				}
				// create new axiom
				changes.add(new AddAxiom(abox, 
						f.getOWLObjectPropertyAssertionAxiom(
								oldAxiom.getProperty(), 
								oldAxiom.getSubject(), 
								oldAxiom.getObject(), 
								newAnnotations)));
			}
		}
		
		if (changes.isEmpty() == false) {
			m.applyChanges(changes);
			model.getReasoner().flush();
		}
	}

	private Set<OWLAnnotationAssertionAxiom> getAnnotationAssertionAxioms(OWLOntology abox, OWLNamedIndividual individual) {
		Set<OWLAnnotationAssertionAxiom> all = abox.getAxioms(AxiomType.ANNOTATION_ASSERTION);
		Set<OWLAnnotationAssertionAxiom> relevant = new HashSet<OWLAnnotationAssertionAxiom>();
		IRI individualIRI = individual.getIRI();
		for (OWLAnnotationAssertionAxiom axiom : all) {
			OWLAnnotationSubject subject = axiom.getSubject();
			if (individualIRI.equals(subject)) {
				relevant.add(axiom);
			}
		}
		return relevant;
	}

	/**
	 * @param abox
	 * @param individual
	 * @return axioms
	 */
	private Set<OWLClassAssertionAxiom> getClassAssertionAxioms(OWLOntology abox, OWLNamedIndividual individual) {
		Set<OWLIndividualAxiom> axioms = abox.getAxioms(individual);
		final Set<OWLClassAssertionAxiom> classAssertionAxioms = new HashSet<OWLClassAssertionAxiom>();
		
		for (final OWLIndividualAxiom owlIndividualAxiom : axioms) {
			owlIndividualAxiom.accept(new OWLAxiomVisitorAdapter(){

				@Override
				public void visit(OWLClassAssertionAxiom axiom) {
					classAssertionAxioms.add(axiom);
				}
			});
		}
		return classAssertionAxioms;
	}
	
}
