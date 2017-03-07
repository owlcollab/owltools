package owltools.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.obolibrary.obo2owl.Obo2OWLConstants;
import org.obolibrary.obo2owl.OWLAPIObo2Owl;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAsymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLAxiomVisitorEx;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLDatatypeDefinitionAxiom;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLDifferentIndividualsAxiom;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointUnionAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalDataPropertyAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLHasKeyAxiom;
import org.semanticweb.owlapi.model.OWLInverseFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLInverseObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLIrreflexiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNegativeDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLNegativeObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLReflexiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLSameIndividualAxiom;
import org.semanticweb.owlapi.model.OWLSubAnnotationPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubDataPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubPropertyChainOfAxiom;
import org.semanticweb.owlapi.model.OWLSymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLTransitiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.SWRLRule;

/**
 * These are helper methods to check and modify axiom annotations. 
 * The main focus is the axiom annotation marking axioms as inferred by. Currently, the 
 * only supported annotation for this use-case is: is_inferred="true"^xsd:string
 */
public class AxiomAnnotationTools {

	/**
	 * IRI for the default annotation marking an axiom as inferred.
	 * Axiom annotation with such annotations are also recognized 
	 * and translated as OBO annotations in obo2owl and owl2obo conversions.
	 */
	public static final IRI ANNOTATION_IRI_IS_INFERRED = IRI.create(Obo2OWLConstants.OIOVOCAB_IRI_PREFIX, "is_inferred");

	/**
	 * Check if there as an annotation with is_inferred="true" for the given axiom.
	 * The method will return true, as soon as it finds an annotation value "true"^xsd:string.
	 * 
	 * @param axiom
	 * @return boolean
	 */
	public static boolean isMarkedAsInferredAxiom(OWLAxiom axiom) {
		List<String> values = getAxiomAnnotationValues(ANNOTATION_IRI_IS_INFERRED, axiom);
		if (values != null && !values.isEmpty()) {
			for (String value : values) {
				if (value != null && "true".equalsIgnoreCase(value)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Mark the given axiom as inferred.<br>
	 * <b>Side effect</b>: Removes the axiom in the given ontology and creates a
	 * new axiom with the changed annotations. Returns the new changed axiom.
	 * 
	 * @param axiom
	 * @param ontology
	 * @return changed axiom
	 */
	public static OWLAxiom markAsInferredAxiom(OWLAxiom axiom, OWLOntology ontology) {
		
		return modifyInferredAxiom(axiom, ontology, true);
	}
	
	/**
	 * Mark the given axiom as inferred. Uses the factory to re-create the axiom
	 * with the updated annotations.
	 * 
	 * @param axiom
	 * @param factory
	 * @return changed axiom
	 */
	public static OWLAxiom markAsInferredAxiom(OWLAxiom axiom, OWLDataFactory factory) {
		
		return updateInferredAxiom(axiom, factory, true);
	}
	
	/**
	 * Mark the given axioms as inferred.<br>
	 * <b>Side effect</b>: Removes the axiom in the given ontology and creates a
	 * new axiom with the changed annotations. Returns the new changed axiom.
	 * 
	 * @param axioms
	 * @param ontology
	 * @return changed axiom
	 */
	public static Set<OWLAxiom> markAsInferredAxiom(Set<OWLAxiom> axioms, OWLOntology ontology) {
		final OWLOntologyManager manager = ontology.getOWLOntologyManager();
		final OWLDataFactory factory = manager.getOWLDataFactory();
		
		// update axioms
		Set<OWLAxiom> newAxioms = new HashSet<OWLAxiom>();
		for (OWLAxiom axiom : axioms) {
			newAxioms.add(updateInferredAxiom(axiom, factory, true));
		}
		// change ontology
		manager.removeAxioms(ontology, axioms);
		manager.addAxioms(ontology, newAxioms);
		return newAxioms;
	}
	
	/**
	 * Mark the given axioms as inferred. Uses the factory to re-create the
	 * axioms with the updated annotations.
	 * 
	 * @param axioms
	 * @param factory
	 * @return changed axiom
	 */
	public static Set<OWLAxiom> markAsInferredAxiom(Set<OWLAxiom> axioms, OWLDataFactory factory) {
		
		// update axioms
		Set<OWLAxiom> newAxioms = new HashSet<OWLAxiom>();
		for (OWLAxiom axiom : axioms) {
			newAxioms.add(updateInferredAxiom(axiom, factory, true));
		}
		return newAxioms;
	}

	/**
	 * Remove the inferred mark from the axiom.<br>
	 * <b>Side effect</b>: Removes the axiom in the given ontology and creates a
	 * new axiom with the changed annotations. Returns the new changed axiom.
	 * 
	 * @param axiom
	 * @param ontology
	 * @return changed axiom
	 */
	public static OWLAxiom removeMarkAsInferredAxiom(OWLAxiom axiom, OWLOntology ontology) {
		return modifyInferredAxiom(axiom, ontology, false);
	}
	
	private static OWLAxiom modifyInferredAxiom(OWLAxiom axiom, OWLOntology ontology, boolean add) {
		final OWLOntologyManager manager = ontology.getOWLOntologyManager();
		final OWLDataFactory factory = manager.getOWLDataFactory();
		final OWLAxiom newAxiom = updateInferredAxiom(axiom, factory, add);
		
		// update ontology
		manager.removeAxiom(ontology, axiom);
		manager.addAxiom(ontology, newAxiom);
		return newAxiom;
	}

	private static OWLAxiom updateInferredAxiom(OWLAxiom axiom, OWLDataFactory factory, boolean add) {
		// filter existing
		Set<OWLAnnotation> annotations = axiom.getAnnotations();
		Set<OWLAnnotation> newAnnotations = new HashSet<OWLAnnotation>();
		if (annotations != null && !annotations.isEmpty()) {
			for (OWLAnnotation annotation : annotations) {
				OWLAnnotationProperty property = annotation.getProperty();
				if (property.getIRI().equals(ANNOTATION_IRI_IS_INFERRED) == false) {
					newAnnotations.add(annotation);
				}
			}
		}
		if (add) {
			// add new annotation: is_inferred="true"
			OWLAnnotationProperty property = factory.getOWLAnnotationProperty(ANNOTATION_IRI_IS_INFERRED);
			newAnnotations.add(factory.getOWLAnnotation(property, factory.getOWLLiteral("true")));
		}
		// create new axiom with updated annotation
		final OWLAxiom newAxiom = changeAxiomAnnotations(axiom, newAnnotations, factory);
		return newAxiom;
	}

	/**
	 * Retrieve the literal values for the axiom annotations with the given
	 * annotation property IRI.
	 * 
	 * @param iri
	 * @param axiom
	 * @return literal values or null
	 */
	public static List<String> getAxiomAnnotationValues(IRI iri, OWLAxiom axiom) {
		List<String> result = null;
		for(OWLAnnotation annotation : axiom.getAnnotations()) {
			OWLAnnotationProperty property = annotation.getProperty();
			if (property.getIRI().equals(iri)) {
				OWLAnnotationValue value = annotation.getValue();
				if (value instanceof OWLLiteral) {
					String literal = ((OWLLiteral) value).getLiteral();
					if (result == null) {
						result = Collections.singletonList(literal);
					}
					else if (result.size() == 1) {
						result = new ArrayList<String>(result);
						result.add(literal);
					}
					else {
						result.add(literal);
					}
				}
			}
		}
		return result;
	}
	
	
	/**
	 * Update the given axiom to a new set of axiom annotation.<br>
	 * <b>Side effect</b>: This removes the old axiom and adds the new axiom to
	 * the given ontology. The method also returns the new axiom to enable
	 * chaining.
	 * 
	 * @param axiom
	 * @param annotations
	 * @param ontology
	 * @return newAxiom
	 */
	public static OWLAxiom changeAxiomAnnotations(OWLAxiom axiom, Set<OWLAnnotation> annotations, OWLOntology ontology) {
		final OWLOntologyManager manager = ontology.getOWLOntologyManager();
		final OWLDataFactory factory = manager.getOWLDataFactory();
		final OWLAxiom newAxiom = changeAxiomAnnotations(axiom, annotations, factory);
		manager.removeAxiom(ontology, axiom);
		manager.addAxiom(ontology, newAxiom);
		return newAxiom;
	}
	
	/**
	 * Update the given axiom to the new set of axiom annotation. Recreates the
	 * axiom with the new annotations using the given factory.
	 * 
	 * @param axiom
	 * @param annotations
	 * @param factory
	 * @return newAxiom
	 */
	public static OWLAxiom changeAxiomAnnotations(OWLAxiom axiom, Set<OWLAnnotation> annotations, OWLDataFactory factory) {
		final AxiomAnnotationsChanger changer = new AxiomAnnotationsChanger(annotations, factory);
		final OWLAxiom newAxiom = axiom.accept(changer);
		return newAxiom;
	}
	
	/**
	 * Remove axiom annotations, which do not comply with the OBO-Basic level,
	 * i.e. trailing qualifier values in OBO.
	 * 
	 * @param ontology
	 */
	public static void reduceAxiomAnnotationsToOboBasic(OWLOntology ontology) {
		for(OWLAxiom axiom : ontology.getAxioms()) {
			AxiomAnnotationTools.reduceAxiomAnnotationsToOboBasic(axiom, ontology);
		}
	}
	
	/**
	 * Remove axiom annotations, which do not comply with the OBO-Basic level,
	 * i.e. trailing qualifier values in OBO.<br>
	 * <b>Side effect</b>: This removes the old axiom and adds the new axiom to
	 * the given ontology. The method also returns the new axiom to enable
	 * chaining.
	 * 
	 * @param axiom
	 * @param ontology
	 * @return axiom
	 */
	public static OWLAxiom reduceAxiomAnnotationsToOboBasic(OWLAxiom axiom, OWLOntology ontology) {
		Set<OWLAnnotation> annotations = axiom.getAnnotations();
		if (annotations != null && !annotations.isEmpty()) {
			boolean changed = false;
			Set<OWLAnnotation> newAnnotations = new HashSet<OWLAnnotation>();
			for (OWLAnnotation owlAnnotation : annotations) {
				OWLAnnotationProperty p = owlAnnotation.getProperty();
				IRI iri = p.getIRI();
				/*
				 * if the property IRI is not in a predefined annotation property in 
				 * OWLAPIObo2Owl assume that it's not OBO-Basic
				 */
				if (OWLAPIObo2Owl.ANNOTATIONPROPERTYMAP.containsValue(iri) == false) {
					// remove axiom annotation
					changed = true;
				}
				else {
					newAnnotations.add(owlAnnotation);
				}
			}
			if (changed) {
				// only update the axiom if the annotations have been changed
				OWLAxiom newAxiom = AxiomAnnotationTools.changeAxiomAnnotations(axiom, newAnnotations, ontology);
				return newAxiom;
			}
		}
		return axiom;
	}
	
	/**
	 * Visitor which returns a new axiom of the same type with the new annotations.
	 */
	public static class AxiomAnnotationsChanger implements OWLAxiomVisitorEx<OWLAxiom> {
		
		private final Set<OWLAnnotation> annotations;
		private final OWLDataFactory factory;

		public AxiomAnnotationsChanger(Set<OWLAnnotation> annotations, OWLDataFactory factory) {
			this.annotations = annotations;
			this.factory = factory;
		}

		@Override
		public OWLAxiom visit(OWLSubAnnotationPropertyOfAxiom axiom) {
			return factory.getOWLSubAnnotationPropertyOfAxiom(axiom.getSubProperty(), axiom.getSuperProperty(), annotations);
		}

		@Override
		public OWLAxiom visit(OWLAnnotationPropertyDomainAxiom axiom) {
			return factory.getOWLAnnotationPropertyDomainAxiom(axiom.getProperty(), axiom.getDomain(), annotations);
		}

		@Override
		public OWLAxiom visit(OWLAnnotationPropertyRangeAxiom axiom) {
			return factory.getOWLAnnotationPropertyRangeAxiom(axiom.getProperty(), axiom.getRange(), annotations);
		}

		@Override
		public OWLAxiom visit(OWLSubClassOfAxiom axiom) {
			return factory.getOWLSubClassOfAxiom(axiom.getSubClass(), axiom.getSuperClass(), annotations);
		}

		@Override
		public OWLAxiom visit(OWLNegativeObjectPropertyAssertionAxiom axiom) {
			return factory.getOWLNegativeObjectPropertyAssertionAxiom(axiom.getProperty(), axiom.getSubject(), axiom.getObject(), annotations);
		}

		@Override
		public OWLAxiom visit(OWLAsymmetricObjectPropertyAxiom axiom) {
			return factory.getOWLAsymmetricObjectPropertyAxiom(axiom.getProperty(), annotations);
		}

		@Override
		public OWLAxiom visit(OWLReflexiveObjectPropertyAxiom axiom) {
			return factory.getOWLReflexiveObjectPropertyAxiom(axiom.getProperty(), annotations);
		}

		@Override
		public OWLAxiom visit(OWLDisjointClassesAxiom axiom) {
			return factory.getOWLDisjointClassesAxiom(axiom.getClassExpressions(), annotations);
		}

		@Override
		public OWLAxiom visit(OWLDataPropertyDomainAxiom axiom) {
			return factory.getOWLDataPropertyDomainAxiom(axiom.getProperty(), axiom.getDomain(), annotations);
		}

		@Override
		public OWLAxiom visit(OWLObjectPropertyDomainAxiom axiom) {
			return factory.getOWLObjectPropertyDomainAxiom(axiom.getProperty(), axiom.getDomain(), annotations);
		}

		@Override
		public OWLAxiom visit(OWLEquivalentObjectPropertiesAxiom axiom) {
			return factory.getOWLEquivalentObjectPropertiesAxiom(axiom.getProperties(), annotations);
		}

		@Override
		public OWLAxiom visit(OWLNegativeDataPropertyAssertionAxiom axiom) {
			return factory.getOWLNegativeDataPropertyAssertionAxiom(axiom.getProperty(), axiom.getSubject(), axiom.getObject(), annotations);
		}

		@Override
		public OWLAxiom visit(OWLDifferentIndividualsAxiom axiom) {
			return factory.getOWLDifferentIndividualsAxiom(axiom.getIndividuals(), annotations);
		}

		@Override
		public OWLAxiom visit(OWLDisjointDataPropertiesAxiom axiom) {
			return factory.getOWLDisjointDataPropertiesAxiom(axiom.getProperties(), annotations);
		}

		@Override
		public OWLAxiom visit(OWLDisjointObjectPropertiesAxiom axiom) {
			return factory.getOWLDisjointObjectPropertiesAxiom(axiom.getProperties(), annotations);
		}

		@Override
		public OWLAxiom visit(OWLObjectPropertyRangeAxiom axiom) {
			return factory.getOWLObjectPropertyRangeAxiom(axiom.getProperty(), axiom.getRange(), annotations);
		}

		@Override
		public OWLAxiom visit(OWLObjectPropertyAssertionAxiom axiom) {
			return factory.getOWLObjectPropertyAssertionAxiom(axiom.getProperty(), axiom.getSubject(), axiom.getObject(), annotations);
		}

		@Override
		public OWLAxiom visit(OWLFunctionalObjectPropertyAxiom axiom) {
			return factory.getOWLFunctionalObjectPropertyAxiom(axiom.getProperty(), annotations);
		}

		@Override
		public OWLAxiom visit(OWLSubObjectPropertyOfAxiom axiom) {
			return factory.getOWLSubObjectPropertyOfAxiom(axiom.getSubProperty(), axiom.getSuperProperty(), annotations);
		}

		@Override
		public OWLAxiom visit(OWLDisjointUnionAxiom axiom) {
			return factory.getOWLDisjointUnionAxiom(axiom.getOWLClass(), axiom.getClassExpressions(), annotations);
		}

		@Override
		public OWLAxiom visit(OWLDeclarationAxiom axiom) {
			return factory.getOWLDeclarationAxiom(axiom.getEntity(), annotations);
		}

		@Override
		public OWLAxiom visit(OWLAnnotationAssertionAxiom axiom) {
			return factory.getOWLAnnotationAssertionAxiom(axiom.getProperty(), axiom.getSubject(), axiom.getValue(), annotations);
		}

		@Override
		public OWLAxiom visit(OWLSymmetricObjectPropertyAxiom axiom) {
			return factory.getOWLSymmetricObjectPropertyAxiom(axiom.getProperty(), annotations);
		}

		@Override
		public OWLAxiom visit(OWLDataPropertyRangeAxiom axiom) {
			return factory.getOWLDataPropertyRangeAxiom(axiom.getProperty(), axiom.getRange(), annotations);
		}

		@Override
		public OWLAxiom visit(OWLFunctionalDataPropertyAxiom axiom) {
			return factory.getOWLFunctionalDataPropertyAxiom(axiom.getProperty(), annotations);
		}

		@Override
		public OWLAxiom visit(OWLEquivalentDataPropertiesAxiom axiom) {
			return factory.getOWLEquivalentDataPropertiesAxiom(axiom.getProperties(), annotations);
		}

		@Override
		public OWLAxiom visit(OWLClassAssertionAxiom axiom) {
			return factory.getOWLClassAssertionAxiom(axiom.getClassExpression(), axiom.getIndividual(), annotations);
		}

		@Override
		public OWLAxiom visit(OWLEquivalentClassesAxiom axiom) {
			return factory.getOWLEquivalentClassesAxiom(axiom.getClassExpressions(), annotations);
		}

		@Override
		public OWLAxiom visit(OWLDataPropertyAssertionAxiom axiom) {
			return factory.getOWLDataPropertyAssertionAxiom(axiom.getProperty(), axiom.getSubject(), axiom.getObject(), annotations);
		}

		@Override
		public OWLAxiom visit(OWLTransitiveObjectPropertyAxiom axiom) {
			return factory.getOWLTransitiveObjectPropertyAxiom(axiom.getProperty(), annotations);
		}

		@Override
		public OWLAxiom visit(OWLIrreflexiveObjectPropertyAxiom axiom) {
			return factory.getOWLIrreflexiveObjectPropertyAxiom(axiom.getProperty(), annotations);
		}

		@Override
		public OWLAxiom visit(OWLSubDataPropertyOfAxiom axiom) {
			return factory.getOWLSubDataPropertyOfAxiom(axiom.getSubProperty(), axiom.getSuperProperty(), annotations);
		}

		@Override
		public OWLAxiom visit(OWLInverseFunctionalObjectPropertyAxiom axiom) {
			return factory.getOWLInverseFunctionalObjectPropertyAxiom(axiom.getProperty(), annotations);
		}

		@Override
		public OWLAxiom visit(OWLSameIndividualAxiom axiom) {
			return factory.getOWLSameIndividualAxiom(axiom.getIndividuals(), annotations);
		}

		@Override
		public OWLAxiom visit(OWLSubPropertyChainOfAxiom axiom) {
			return factory.getOWLSubPropertyChainOfAxiom(axiom.getPropertyChain(), axiom.getSuperProperty(), annotations);
		}

		@Override
		public OWLAxiom visit(OWLInverseObjectPropertiesAxiom axiom) {
			return factory.getOWLInverseObjectPropertiesAxiom(axiom.getFirstProperty(), axiom.getSecondProperty(), annotations);
		}

		@Override
		public OWLAxiom visit(OWLHasKeyAxiom axiom) {
			return factory.getOWLHasKeyAxiom(axiom.getClassExpression(), axiom.getDataPropertyExpressions(), annotations);
		}

		@Override
		public OWLAxiom visit(OWLDatatypeDefinitionAxiom axiom) {
			return factory.getOWLDatatypeDefinitionAxiom(axiom.getDatatype(), axiom.getDataRange(), annotations);
		}

		@Override
		public OWLAxiom visit(SWRLRule rule) {
			return factory.getSWRLRule(rule.getBody(), rule.getHead(), annotations);
		}

	}
}
