package owltools.ontologyverification.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.obolibrary.oboformat.parser.OBOFormatConstants.OboFormatTag;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;
import owltools.mooncat.Mooncat;
import owltools.ontologyverification.CheckWarning;

/**
 * Check for dangling references in equivalence axioms. A reference is assumed to be 
 * dangling, if there is no declaration axiom for an OWLClass and has no annotation assertions
 * 
 * This currently limited to OBO style ontologies.
 */
public class DanglingReferenceCheck extends AbstractCheck {

	public static final String SHORT_HAND = "dangling-ref";
	
	public DanglingReferenceCheck() {
		super("DANGLING_REFERENCE_CHECK", "Dangling Reference Check", false, null);
	}
	
	@Override
	public Collection<CheckWarning> check(OWLGraphWrapper graph, Collection<OWLObject> allOwlObjects) {
		OWLPrettyPrinter pp = new OWLPrettyPrinter(graph);
		List<CheckWarning> out = new ArrayList<CheckWarning>();
		for (OWLObject owlObject : allOwlObjects) {
			if (owlObject instanceof OWLClass) {
				OWLClass owlClass = (OWLClass) owlObject;
				check(owlClass, graph, out, pp);
			}
		}
		return out;
	}

	protected void check(OWLClass owlClass, OWLGraphWrapper graph, List<CheckWarning> warnings, OWLPrettyPrinter pp) {
		final Set<OWLOntology> allOntologies = graph.getAllOntologies();
		for(OWLOntology ontology : allOntologies){
			Set<OWLEquivalentClassesAxiom> axioms = ontology.getEquivalentClassesAxioms(owlClass);
			if (axioms != null && !axioms.isEmpty()) {
				// check axioms 
				for (OWLEquivalentClassesAxiom axiom : axioms) {
					// get the relevant part of the equivalence axiom
					Set<OWLClassExpression> ces = axiom.getClassExpressionsMinus(owlClass);
					if (ces.size() == 1) {
						OWLClassExpression ce = ces.iterator().next();
						if(!ce.isAnonymous()) {
							// OboFormatTag.TAG_EQUIVALENT_TO
							handleEquivalentTo(warnings, allOntologies, axiom, ce.asOWLClass(), pp);
						}
						else if (ce instanceof OWLObjectIntersectionOf) {
							// OboFormatTag.TAG_INTERSECTION_OF
							handleIntersection(warnings, allOntologies, axiom, (OWLObjectIntersectionOf) ce, pp);
						}
						else if (ce instanceof OWLObjectUnionOf) {
							// OboFormatTag.TAG_UNION_OF
							handleUnionOf(warnings, allOntologies, axiom, (OWLObjectUnionOf) ce, pp);
						}
						else {
							// not translatable to OBO
							handleGeneric(warnings, allOntologies, axiom, ce, pp);
						}
					}
					else {
						// not translatable to OBO
						for (OWLClassExpression ce : ces) {
							handleGeneric(warnings, allOntologies, axiom, ce, pp);
						}
					}
				}
			}
		}
	}

	private void handleEquivalentTo(List<CheckWarning> warnings, Set<OWLOntology> allOntologies,
			OWLEquivalentClassesAxiom axiom, OWLClass cls, OWLPrettyPrinter pp)
	{
		if (isDangling(cls, allOntologies)) {
			final IRI iri = cls.getIRI();
			String message = "Dangling reference "+iri+" in EQUIVALENT_TO axiom: "+pp.render(axiom);
			warnings.add(new CheckWarning(getID(), message , isFatal(), iri, OboFormatTag.TAG_EQUIVALENT_TO.getTag()));
		}
	}
	
	private void handleIntersection(List<CheckWarning> warnings, Set<OWLOntology> allOntologies,
			OWLEquivalentClassesAxiom axiom, OWLObjectIntersectionOf intersection, OWLPrettyPrinter pp) 
	{
		for(OWLClassExpression operand : intersection.getOperandsAsList()) {
			OWLClass operandCls = null;
			if (!operand.isAnonymous()) {
				operandCls = operand.asOWLClass();
			}
			else if (operand instanceof OWLObjectSomeValuesFrom) {
				OWLObjectSomeValuesFrom ristriction = (OWLObjectSomeValuesFrom) operand;
				OWLClassExpression filler = ristriction.getFiller();
				if (!filler.isAnonymous()) {
					operandCls = filler.asOWLClass();
				}
			}
			else {
				// not translatable to OBO
				handleGeneric(warnings, allOntologies, axiom, operand, pp);
			}
			if (operandCls != null && isDangling(operandCls, allOntologies)) {
				final IRI iri = operandCls.getIRI();
				String message = "Dangling reference "+iri+" in INTERSECTION_OF axiom: "+pp.render(axiom);
				warnings.add(new CheckWarning(getID(), message , isFatal(), iri, OboFormatTag.TAG_INTERSECTION_OF.getTag()));
			}
		}
	}
	
	private void handleUnionOf(List<CheckWarning> warnings, Set<OWLOntology> allOntologies, 
			OWLEquivalentClassesAxiom axiom, OWLObjectUnionOf union, OWLPrettyPrinter pp) 
	{
		List<OWLClassExpression> operands = union.getOperandsAsList();
		for(OWLClassExpression operand : operands) {
			if (!operand.isAnonymous()) {
				OWLClass operandCls = operand.asOWLClass();
				if (isDangling(operandCls, allOntologies)) {
					final IRI iri = operandCls.getIRI();
					String message = "Dangling reference "+iri+" in UNION_OF axiom: "+pp.render(axiom);
					warnings.add(new CheckWarning(getID(), message , isFatal(), iri, OboFormatTag.TAG_UNION_OF.getTag()));
				}
			}
			else {
				// not translatable to OBO
				handleGeneric(warnings, allOntologies, axiom, operand, pp);
			}
		}
	}

	private void handleGeneric(List<CheckWarning> warnings, Set<OWLOntology> allOntologies, 
			OWLEquivalentClassesAxiom axiom, OWLClassExpression ce, OWLPrettyPrinter pp) 
	{
		Set<OWLClass> classes = ce.getClassesInSignature();
		for (OWLClass cls : classes) {
			if (isDangling(cls, allOntologies)) {
				final IRI iri = cls.getIRI();
				String message = "Dangling reference "+iri+" in axiom: "+pp.render(axiom);
				warnings.add(new CheckWarning(getID(), message , isFatal(), iri, null));
			}
		}
	}

	/**
	 * Test an class is dangling.
	 * 
	 * Here a dangling entity is one that has no annotation assertions;
	 * 
	 * This currently limited to OBO style ontologies
	 * 
	 * @param cls
	 * @param ontologies
	 * @return true if the class is dangling
	 * 
	 * @see Mooncat#isDangling(OWLOntology, org.semanticweb.owlapi.model.OWLEntity)
	 */
	private boolean isDangling(OWLClass cls, Collection<OWLOntology> ontologies) {
		// check for declaration axioms
		// this usually won't hit, as obo2owl always creates an declaration axiom
		boolean hasDeclaration = false;
		
		for (OWLOntology ontology : ontologies) {
			Set<OWLDeclarationAxiom> axioms = ontology.getDeclarationAxioms(cls);
			if (axioms != null && !axioms.isEmpty()) {
				hasDeclaration = true;
				break;
			}
		}
		
		if (!hasDeclaration) {
			return true;
		}
		
		// check that each class has either at least one least one annotation
		for (OWLOntology ontology : ontologies) {
			Set<OWLAnnotationAssertionAxiom> axioms = ontology.getAnnotationAssertionAxioms(cls.getIRI());
			if (axioms != null && !axioms.isEmpty()) {
				return false;
			}
		}
		return true;
	}

}
