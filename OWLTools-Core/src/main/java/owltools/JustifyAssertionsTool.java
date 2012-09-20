package owltools;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import owltools.InferenceBuilder.Inferences;

/**
 * Methods to test/justify, whether certain axioms can be inferred from the ontology.
 * Currently focused on {@link OWLSubClassOfAxiom}.
 */
public class JustifyAssertionsTool {
	
	/**
	 * Generic result for a justify.
	 */
	public static class JustifyResult {
		
		private Set<OWLAxiom> newInferred = null;
		private Set<OWLAxiom> existsEntailed = null;
		private Set<OWLAxiom> existsRedundant = null;
		private Set<OWLAxiom> existsNotEntailed = null;
		
		/**
		 * @return the newInferred
		 */
		public Set<OWLAxiom> getNewInferred() {
			return wrap(newInferred);
		}
		
		/**
		 * @return the existsEntailed
		 */
		public Set<OWLAxiom> getExistsEntailed() {
			return wrap(existsEntailed);
		}
		
		/**
		 * @return the existsRedundant
		 */
		public Set<OWLAxiom> getExistsRedundant() {
			return wrap(existsRedundant);
		}
		
		/**
		 * @return the existsNotEntailed
		 */
		public Set<OWLAxiom> getExistsNotEntailed() {
			return wrap(existsNotEntailed);
		}
		
		private <T> Set<T> wrap(Set<T> set) {
			if (set == null) {
				return Collections.emptySet();
			}
			return Collections.unmodifiableSet(set);
		}

		/**
		 * @param ax the newInferred to add
		 */
		void addNewInferred(OWLAxiom ax) {
			newInferred = add(ax, newInferred);
		}

		/**
		 * @param ax the existsEntailed to add
		 */
		void addExistsEntailed(OWLAxiom ax) {
			existsEntailed = add(ax, existsEntailed);
		}

		/**
		 * @param ax the existsRedundant to add
		 */
		void addExistsRedundant(OWLAxiom ax) {
			existsRedundant = add(ax, existsRedundant);
		}

		/**
		 * @param ax the existsNotEntailed to add
		 */
		void addExistsNotEntailed(OWLAxiom ax) {
			existsNotEntailed = add(ax, existsNotEntailed);
		}
		
		<T> Set<T> add(T value, Set<T> set) {
			if (set == null) {
				return Collections.singleton(value);
			}
			if (set.size() == 1) {
				Set<T> newSet = new HashSet<T>();
				newSet.addAll(set);
				newSet.add(value);
				return newSet;
			}
			set.add(value);
			return set;
		}
	}

	/**
	 * Justify the given set of subClass axioms from the given ontology. Assumes that
	 * the axioms are already removed and the reasoner is up-to-date. <br>
	 * Does not modify the ontology.
	 * 
	 * @param ontology
	 * @param reasoner
	 * @param axioms
	 * @return result
	 */
	public static JustifyResult justifySubClasses(OWLOntology ontology, OWLReasoner reasoner, Set<OWLSubClassOfAxiom> axioms) {
		
		InferenceBuilder infBuilder = new InferenceBuilder(null, (OWLReasonerFactory) null, false);
		Inferences inferences = infBuilder.buildInferences(ontology, reasoner, true);
		List<OWLAxiom> inferredAxioms = inferences.axiomsToAdd;
		return justifySubClasses(ontology, reasoner, axioms, inferredAxioms);
	}
	
	/**
	 * Justify the given set of subClass axioms from the given ontology. Assumes that
	 * the axioms are already removed and the infBuilder is ready for reasoning. <br>
	 * Does not modify the ontology.
	 * 
	 * @param ontology
	 * @param infBuilder 
	 * @param axioms
	 * @return result
	 */
	public static JustifyResult justifySubClasses(OWLOntology ontology, InferenceBuilder infBuilder, Set<OWLSubClassOfAxiom> axioms) {
		
		OWLReasoner reasoner = infBuilder.getReasoner(ontology);
		Inferences inferences = infBuilder.buildInferences(ontology, reasoner, true);
		List<OWLAxiom> inferredAxioms = inferences.axiomsToAdd;
		return justifySubClasses(ontology, reasoner, axioms, inferredAxioms);
	}
	
	/**
	 * Justify the given set of subClass axioms from the given ontology. Assumes
	 * that the axioms are already removed, the reasoner is up-to-date and there
	 * is a collection of new inferred axioms. <br>
	 * Does not modify the ontology.
	 * 
	 * @param ontology
	 * @param reasoner
	 * @param removedAxioms
	 * @param inferredAxioms
	 * @return result
	 */
	public static JustifyResult justifySubClasses(OWLOntology ontology, OWLReasoner reasoner, Set<OWLSubClassOfAxiom> removedAxioms, Collection<OWLAxiom> inferredAxioms) {
		JustifyResult result = new JustifyResult();
		
		/*
		 * Create a set of axioms without their annotations. Reason: The new
		 * axioms won't match (Set#contains), if the corresponding axioms in the
		 * set have annotations.
		 */
		Set<OWLAxiom> noAnnotationsAxioms = new HashSet<OWLAxiom>();
		for (OWLAxiom owlAxiom : removedAxioms) {
			noAnnotationsAxioms.add(owlAxiom.getAxiomWithoutAnnotations());
		}
		
		for(OWLAxiom ax: inferredAxioms) {
			if (ax instanceof OWLSubClassOfAxiom && 
					((OWLSubClassOfAxiom)ax).getSuperClass().isOWLThing()) {
				continue;
			}

			if (noAnnotationsAxioms.contains(ax)) {
				// "EXISTS, ENTAILED"
				result.addExistsEntailed(ax);
			}
			else {
				// "NEW, INFERRED"
				result.addNewInferred(ax);
			}
		}
		for (OWLSubClassOfAxiom ax : removedAxioms) {
			OWLSubClassOfAxiom noAnnotations = ax.getAxiomWithoutAnnotations();
			if (!inferredAxioms.contains(noAnnotations)) {
				OWLClassExpression superClass = ax.getSuperClass();
				boolean entailed = false;
				if (superClass.isAnonymous() == false) {
					NodeSet<OWLClass> superClasses = reasoner.getSuperClasses(ax.getSubClass(), false);
					entailed = superClasses.containsEntity(superClass.asOWLClass());
				}
				if (entailed) {
					// "EXISTS, REDUNDANT"
					result.addExistsRedundant(ax);
				}
				else {
					// "EXISTS, NOT-ENTAILED"
					result.addExistsNotEntailed(ax);
				}
			}
		}
		
		return result;
	}
}
