package owltools;

import gnu.trove.set.hash.THashSet;

import java.util.HashMap;
import java.util.HashSet;
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
	
	public static class RedundantAxiom {
		
		final OWLSubClassOfAxiom axiom;
		final Set<OWLClass> moreSpecific;

		/**
		 * @param axiom
		 * @param moreSpecific
		 */
		public RedundantAxiom(OWLSubClassOfAxiom axiom, Set<OWLClass> moreSpecific) {
			this.axiom = axiom;
			this.moreSpecific = moreSpecific;
		}

		/**
		 * @return the axiom
		 */
		public OWLSubClassOfAxiom getAxiom() {
			return axiom;
		}

		/**
		 * @return the moreSpecific
		 */
		public Set<OWLClass> getMoreSpecific() {
			return moreSpecific;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((axiom == null) ? 0 : axiom.hashCode());
			result = prime * result
					+ ((moreSpecific == null) ? 0 : moreSpecific.hashCode());
			return result;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
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
			RedundantAxiom other = (RedundantAxiom) obj;
			if (axiom == null) {
				if (other.axiom != null) {
					return false;
				}
			} else if (!axiom.equals(other.axiom)) {
				return false;
			}
			if (moreSpecific == null) {
				if (other.moreSpecific != null) {
					return false;
				}
			} else if (!moreSpecific.equals(other.moreSpecific)) {
				return false;
			}
			return true;
		}
		
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
	public static Map<OWLClass, Set<RedundantAxiom>> findRedundantSubClassAxioms(Iterable<OWLClass> classes, OWLOntology ontology, OWLReasoner reasoner) {
		int redundantAxiomsCounter = 0;
		Map<OWLClass, Set<RedundantAxiom>> allRedundantAxioms = new HashMap<OWLClass, Set<RedundantAxiom>>();
		for (final OWLClass cls : classes) {
			Set<RedundantAxiom> redundantAxioms = new THashSet<RedundantAxiom>();
			Set<OWLSubClassOfAxiom> subClassAxioms = ontology.getSubClassAxiomsForSubClass(cls);
			
			// use direct super classes to identify redundant axioms
			Set<OWLClass> directSuperClasses = reasoner.getSuperClasses(cls, true).getFlattened();
			Map<OWLClass, Set<OWLClass>> directSuperClassAncestors = new HashMap<OWLClass, Set<OWLClass>>();
			for(OWLClass directSuperClass : directSuperClasses) {
				directSuperClassAncestors.put(directSuperClass, reasoner.getSuperClasses(directSuperClass, false).getFlattened());
			}
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
							// find explanation for why there is a more specific one
							Set<OWLClass> moreSpecific = new HashSet<OWLClass>();
							for(OWLClass directSuperClass : directSuperClasses) {
								Set<OWLClass> ancestors = directSuperClassAncestors.get(directSuperClass);
								if (ancestors.contains(superClass)) {
									moreSpecific.add(directSuperClass);
								}
							}
							redundantAxioms.add(new RedundantAxiom(axiom, moreSpecific));
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
	public static Map<OWLClass, Set<RedundantAxiom>> removeRedundantSubClassAxioms(OWLOntology ontology, OWLReasoner reasoner) {
		Iterable<OWLClass> classes = ontology.getClassesInSignature();
		Map<OWLClass, Set<RedundantAxiom>> axioms = findRedundantSubClassAxioms(classes, ontology, reasoner);
		if (!axioms.isEmpty()) {
			Set<OWLSubClassOfAxiom> allAxioms = new THashSet<OWLSubClassOfAxiom>();
			for(OWLClass cls : axioms.keySet()) {
				for(RedundantAxiom redundantAxiom : axioms.get(cls)) {
					allAxioms.add(redundantAxiom.getAxiom());
				}
			}
			OWLOntologyManager manager = ontology.getOWLOntologyManager();
			List<OWLOntologyChange> changes = manager.removeAxioms(ontology, allAxioms);
			LOG.info("Removed "+changes.size()+" redundant axioms.");
		}
		return axioms;
		
	}
}
