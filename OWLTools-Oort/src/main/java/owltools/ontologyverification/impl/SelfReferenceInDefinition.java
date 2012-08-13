package owltools.ontologyverification.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.graph.OWLGraphWrapper;
import owltools.ontologyverification.CheckWarning;

/**
 * Check, whether the class is defined using the class itself. 
 * For example: X = X and R some Y
 */
public class SelfReferenceInDefinition extends AbstractCheck {

	public SelfReferenceInDefinition() {
		super("SELF_REFERENCE_IN_DEFINITION", "Self Reference In Definition", true);
	}
	
	@Override
	public Collection<CheckWarning> check(OWLGraphWrapper graph, Collection<OWLObject> allOwlObjects) {
		OWLOntology ontology = graph.getSourceOntology();
		List<CheckWarning> violations = new ArrayList<CheckWarning>();
		for(OWLClass cls : ontology.getClassesInSignature()) {
			Set<OWLEquivalentClassesAxiom> equivalentClassesAxioms = ontology.getEquivalentClassesAxioms(cls);
			if (equivalentClassesAxioms != null && !equivalentClassesAxioms.isEmpty()) {
				for (OWLEquivalentClassesAxiom owlEquivalentClassesAxiom : equivalentClassesAxioms) {
					for (OWLClassExpression ex : owlEquivalentClassesAxiom.getClassExpressions()) {
						if (ex instanceof OWLClass)
							continue;
						Set<OWLClass> classesInSignature = ex.getClassesInSignature();
						if (classesInSignature != null && classesInSignature.contains(cls)) {
							String id = graph.getIdentifier(cls);
							String message = "Class "+id+" has a self reference in its logical definition: "+owlEquivalentClassesAxiom;
							CheckWarning warning = new CheckWarning("Self_Reference_In_Definition", message , isFatal(), cls.getIRI());
							violations.add(warning);
						}
					}
				}
			}
		}
		if (!violations.isEmpty()) {
			return violations;
		}
		return null;
	}

}
