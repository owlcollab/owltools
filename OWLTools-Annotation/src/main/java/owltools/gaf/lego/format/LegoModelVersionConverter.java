package owltools.gaf.lego.format;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
import org.semanticweb.owlapi.model.OWLClass;
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

public class LegoModelVersionConverter {
	
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
	
	public void convertLegoModelToAllIndividuals(final OWLOntology abox, final String modelId) {
		final OWLOntologyManager m = abox.getOWLOntologyManager();
		final OWLDataFactory f = m.getOWLDataFactory();
		final List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
		final Set<OWLNamedIndividual> individuals = abox.getIndividualsInSignature();
		final Set<OWLObjectPropertyAssertionAxiom> propertyAssertionAxioms = abox.getAxioms(AxiomType.OBJECT_PROPERTY_ASSERTION);
		
		// update all relevant axioms pertaining to only one individual
		for (final OWLNamedIndividual individual : individuals) {
			Set<OWLAnnotationAssertionAxiom> annotationAssertionAxioms = getAnnotationAssertionAxioms(abox, individual);
			// convert evidence into individual(s)
			// split into ECO IRI and annotations
			EvidenceTriple triple = EvidenceTriple.createEvidenceTriple(annotationAssertionAxioms, abox, changes);
			
			// create individuals for ECO IRIs and link to existing individual
			Set<IRI> evidenceIndividuals = new HashSet<IRI>();
			for(IRI ecoIRI : triple.ecoIRIs) {
				OWLNamedIndividual evidenceIndividual = createEvidenceIndividual(ecoIRI, triple.evidenceAnnotations, modelId, abox, changes);
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
					
					Pair<OWLNamedIndividual,Set<OWLAxiom>> pair = CoreMolecularModelManager.createIndividual(modelId, abox, filler, triple.contributors);
					OWLNamedIndividual newIndividual = pair.getLeft();
					for(OWLAxiom newAxiom : pair.getRight()) {
						changes.add(new AddAxiom(abox, newAxiom));
					}
					changes.add(new AddAxiom(abox, CoreMolecularModelManager.createFact(f, property, individual, newIndividual, triple.contributors)));
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
					OWLNamedIndividual evidenceIndividual = createEvidenceIndividual(ecoIRI, triple.evidenceAnnotations, modelId, abox, changes);
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
		}
	}

	static OWLNamedIndividual createEvidenceIndividual(IRI ecoIRI, Set<OWLAnnotation> evidenceAnnotations, String modelId, OWLOntology abox, List<OWLOntologyChange> changes) {
		OWLDataFactory f = abox.getOWLOntologyManager().getOWLDataFactory();
		OWLClass c = f.getOWLClass(ecoIRI);
		Pair<OWLNamedIndividual, Set<OWLAxiom>> evidenceIndividualPair = CoreMolecularModelManager.createIndividual(modelId, abox, c, null);
		OWLNamedIndividual individual = evidenceIndividualPair.getLeft();
		for(OWLAxiom newAxiom : evidenceIndividualPair.getRight()) {
			changes.add(new AddAxiom(abox, newAxiom));
		}
		IRI individualIRI = individual.getIRI();
		for(OWLAnnotation evidenceAnnotation : evidenceAnnotations) {
			changes.add(new AddAxiom(abox, f.getOWLAnnotationAssertionAxiom(individualIRI, evidenceAnnotation)));
		}
		return individual;
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
