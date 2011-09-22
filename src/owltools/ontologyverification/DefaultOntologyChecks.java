package owltools.ontologyverification;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.graph.OWLGraphWrapper;
import owltools.ontologyverification.annotations.AfterLoading;
import owltools.ontologyverification.annotations.Check;

public class DefaultOntologyChecks {

	/**
	 * Check, whether the class is defined using the class itself. 
	 * For example: X = X and R some Y
	 * 
	 * @param owlGraphWrapper
	 * @return checkResult
	 */
	@AfterLoading
	@Check
	public CheckResult selfReferenceInDefinition(OWLGraphWrapper owlGraphWrapper) {
		OWLOntology ontology = owlGraphWrapper.getSourceOntology();
		List<String> violations = new ArrayList<String>();
		for(OWLClass cls : ontology.getClassesInSignature()) {
			Set<OWLEquivalentClassesAxiom> equivalentClassesAxioms = ontology.getEquivalentClassesAxioms(cls);
			if (equivalentClassesAxioms != null && !equivalentClassesAxioms.isEmpty()) {
				for (OWLEquivalentClassesAxiom owlEquivalentClassesAxiom : equivalentClassesAxioms) {
					Set<OWLClass> classesInSignature = owlEquivalentClassesAxiom.getClassesInSignature();
					if (classesInSignature != null && classesInSignature.contains(cls)) {
						String id = owlGraphWrapper.getIdentifier(cls);
						violations.add("Class "+id+" has a self reference in its logical definition.");
					}
				}
			}
		}
		if (!violations.isEmpty()) {
			return CheckResult.createWarning(violations);
		}
		return null;
	}
	
}
