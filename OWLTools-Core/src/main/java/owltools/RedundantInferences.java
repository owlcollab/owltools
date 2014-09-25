package owltools;

import gnu.trove.set.hash.THashSet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.graph.AxiomAnnotationTools;

public class RedundantInferences {
	
	private static final Logger LOG = Logger.getLogger(RedundantInferences.class);
	
	private RedundantInferences() {
		// no instances allowed
	}

	/**
	 * Find redundant and marked as inferred axioms for each class in the given
	 * ontology. Uses the reasoner to infer the direct super classes.
	 * 
	 * @param classes
	 * @param ontology
	 * @param reasoner
	 * @return map of class to set of redundant axioms
	 */
	public static Map<OWLClass, Set<OWLSubClassOfAxiom>> findRedundantSubClassAxioms(Iterable<OWLClass> classes, OWLOntology ontology, OWLReasoner reasoner) {
		int redundantAxiomsCounter = 0;
		Map<OWLClass, Set<OWLSubClassOfAxiom>> allRedundantAxioms = new HashMap<OWLClass, Set<OWLSubClassOfAxiom>>();
		for (final OWLClass cls : classes) {
			Set<OWLSubClassOfAxiom> redundantAxioms = new THashSet<OWLSubClassOfAxiom>();
			Set<OWLSubClassOfAxiom> subClassAxioms = ontology.getSubClassAxiomsForSubClass(cls);
			
			// use direct super classes to identify redundant axioms
			Set<OWLClass> directSuperClasses = reasoner.getSuperClasses(cls, true).getFlattened();
			for (OWLSubClassOfAxiom axiom : subClassAxioms) {
				
				// only look at axioms, which are marked as inferred
				if (AxiomAnnotationTools.isMarkedAsInferredAxiom(axiom)) {
					OWLClassExpression superClassCE = axiom.getSuperClass();
					if (superClassCE instanceof OWLClass) {
						// only look at axioms with a named super class
						OWLClass superClass = superClassCE.asOWLClass();
						
						// if the super class is not a direct super class
						// assume it is redundant
						if (directSuperClasses.contains(superClass) == false) {
							redundantAxioms.add(axiom);
						}
					}
				}
			}
			// only add cls, if there is a redundant axiom
			if (!redundantAxioms.isEmpty()) {
				allRedundantAxioms.put(cls, redundantAxioms);
				redundantAxiomsCounter += redundantAxioms.size();
			}
		}
		if (redundantAxiomsCounter > 0) {
			LOG.info("Found "+redundantAxiomsCounter+ " redundant axioms for "+allRedundantAxioms.size()+" classes.");	
		}
		
		return allRedundantAxioms;
	}
	
	/**
	 * Remove the redundant and marked as inferred super class assertions for
	 * each class in the ontology signature. Uses the reasoner to infer the
	 * direct super classes.
	 * 
	 * @param ontology
	 * @param reasoner
	 * @return map of class to set of redundant axioms
	 */
	public static Map<OWLClass, Set<OWLSubClassOfAxiom>> removeRedundantSubClassAxioms(OWLOntology ontology, OWLReasoner reasoner) {
		Iterable<OWLClass> classes = ontology.getClassesInSignature();
		Map<OWLClass, Set<OWLSubClassOfAxiom>> axioms = findRedundantSubClassAxioms(classes, ontology, reasoner);
		if (!axioms.isEmpty()) {
			Set<OWLSubClassOfAxiom> allAxioms = new THashSet<OWLSubClassOfAxiom>();
			for(OWLClass cls : axioms.keySet()) {
				allAxioms.addAll(axioms.get(cls));
			}
			OWLOntologyManager manager = ontology.getOWLOntologyManager();
			List<OWLOntologyChange> changes = manager.removeAxioms(ontology, allAxioms);
			LOG.info("Removed "+changes.size()+" redundant axioms.");
		}
		return axioms;
		
	}
}
