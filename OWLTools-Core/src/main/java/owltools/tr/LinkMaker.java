package owltools.tr;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.graph.OWLGraphWrapper;

/**
 * Hard coded version to create relationships based on matching genus
 * differentia patterns, such as:
 * 
 * <pre>
 *   SUB_GENUS and REL some X --NEW_RELATION--> SUPER_GENUS and REL some X
 * </pre>
 * 
 * A reasoner is used the minimize the number of matching candidates, the actual
 * matching uses the equivalent class definitions.<br>
 * <br>
 * <b>WARNING</b>: At some point this might be replaced with a more generic
 * pattern marching approach.
 */
public class LinkMaker {

	private final Set<OWLOntology> allOntologies;
	private final OWLDataFactory f;
	private final OWLReasoner r;

	/**
	 * @param g
	 * @param r 
	 */
	public LinkMaker(OWLGraphWrapper g, OWLReasoner r) {
		this.r = r;
		allOntologies = g.getAllOntologies();
		f = g.getDataFactory();
	}
	
	/**
	 * Set of variables for a pattern.
	 */
	public static class LinkPattern {

		final OWLClass subGenus;
		final OWLClass superGenus;
		final OWLObjectProperty differentiaRelation;
		final OWLObjectProperty newRelation;
		
		public LinkPattern(OWLClass subGenus, OWLClass superGenus,
				OWLObjectProperty differentiaRelation,
				OWLObjectProperty newRelation) {
			this.subGenus = subGenus;
			this.superGenus = superGenus;
			this.differentiaRelation = differentiaRelation;
			this.newRelation = newRelation;
		}

		/**
		 * @return the subGenus
		 */
		public OWLClass getSubGenus() {
			return subGenus;
		}

		/**
		 * @return the superGenus
		 */
		public OWLClass getSuperGenus() {
			return superGenus;
		}

		/**
		 * @return the differentiaRelation
		 */
		public OWLObjectProperty getDifferentiaRelation() {
			return differentiaRelation;
		}

		/**
		 * @return the newRelation
		 */
		public OWLObjectProperty getNewRelation() {
			return newRelation;
		}
	}
	
	public class LinkMakerResult {
		
		final Set<OWLAxiom> predictions;
		final Set<OWLAxiom> exisiting;
		final Set<OWLAxiom> modified;

		/**
		 * @param predictions
		 * @param exisiting
		 * @param modified
		 */
		public LinkMakerResult(Set<OWLAxiom> predictions, Set<OWLAxiom> exisiting, Set<OWLAxiom> modified) {
			this.predictions = predictions;
			this.exisiting = exisiting;
			this.modified = modified;
		}

		/**
		 * @return the predictions
		 */
		public Set<OWLAxiom> getPredictions() {
			return predictions;
		}

		/**
		 * @return the exisiting
		 */
		public Set<OWLAxiom> getExisiting() {
			return exisiting;
		}

		/**
		 * @return the modified
		 */
		public Set<OWLAxiom> getModified() {
			return modified;
		}
	}
	
	/**
	 * Start search for matches of the given patterns. Annotate new axioms with
	 * the given source annotation. Furthermore create a new axiom for existing
	 * annotations, if they do not have a matching source annotation.<br>
	 * <br>
	 * This method does not modify the ontology, it only returns the axioms. 
	 * 
	 * @param patterns
	 * @param sourceAnnotation
	 * @return result
	 */
	public LinkMakerResult makeLinks(List<LinkPattern> patterns, OWLAnnotation sourceAnnotation, boolean updateExisting) {
		final Set<OWLAxiom> resultAxioms = new HashSet<OWLAxiom>();
		final Set<OWLAxiom> existingAxioms = new HashSet<OWLAxiom>();
		final Set<OWLAxiom> modified = new HashSet<OWLAxiom>();
		for (LinkPattern linkPattern : patterns) {
			for (OWLClass currentSubClass : getRelevantClasses(linkPattern.subGenus)) {
				OWLClass differentiaCls = hasMatchingIntersection(currentSubClass, linkPattern.subGenus, linkPattern.differentiaRelation);
				if (differentiaCls != null) {
					// found a matching class, now search for the corresponding one.
					for(OWLClass currentSuperClass : getRelevantClasses(linkPattern.superGenus)) {
						OWLClass potentialMatch = hasMatchingIntersection(currentSuperClass, linkPattern.superGenus, linkPattern.differentiaRelation);
						if (differentiaCls.equals(potentialMatch)) {
							// the class has the required xp
							// now check that the link does not already exist
							OWLAxiom existing = hasLinks(currentSubClass, linkPattern.newRelation, currentSuperClass);
							OWLObjectSomeValuesFrom someValuesFrom = f.getOWLObjectSomeValuesFrom(linkPattern.newRelation, currentSuperClass);
							if (existing == null) {
								OWLSubClassOfAxiom a = f.getOWLSubClassOfAxiom(currentSubClass, someValuesFrom, Collections.singleton(sourceAnnotation));
								resultAxioms.add(a);
							}
							else {
								if (updateExisting) {
									existingAxioms.add(existing);
									Set<OWLAnnotation> existingAnnotations = existing.getAnnotations();
									if (existingAnnotations.contains(sourceAnnotation)) {
										modified.add(existing);
									}
									else {
										Set<OWLAnnotation> mergedAnnotations = new HashSet<OWLAnnotation>();
										mergedAnnotations.add(sourceAnnotation);
										mergedAnnotations.addAll(existingAnnotations);
										OWLSubClassOfAxiom mod = f.getOWLSubClassOfAxiom(currentSubClass, someValuesFrom, mergedAnnotations);
										modified.add(mod);
									}
								}
							}
						}
					}
				}
			}
		}
		return new LinkMakerResult(resultAxioms, existingAxioms, modified);
	}
	
	/**
	 * Use reasoner to minimize the search space. We make use of the fact that
	 * all classes for a given genus are inferred to be subClasses of the genus.
	 * 
	 * @param genus
	 * @return set of classes
	 */
	private Set<OWLClass> getRelevantClasses(OWLClass genus) {
		return r.getSubClasses(genus, false).getFlattened();
	}
	
	/**
	 * Search for the first class with a matching equivalent class definition. 
	 * 
	 * @param c
	 * @param genus
	 * @param relation
	 * @return match or null
	 */
	private OWLClass hasMatchingIntersection(OWLClass c, OWLClass genus, OWLObjectProperty relation) {
		for(OWLOntology o : allOntologies) {
			Set<OWLEquivalentClassesAxiom> eqAxioms = o.getEquivalentClassesAxioms(c);
			for (OWLEquivalentClassesAxiom eqAxiom : eqAxioms) {
				Set<OWLClassExpression> expressions = eqAxiom.getClassExpressionsMinus(c);
				for (OWLClassExpression expression : expressions) {
					if (expression instanceof OWLObjectIntersectionOf) {
						OWLObjectIntersectionOf intersection = (OWLObjectIntersectionOf) expression;
						OWLClass differentiaCls = null;
						boolean matchesGenus = false;
						boolean matchesRelation = false;
						Set<OWLClassExpression> operands = intersection.getOperands();
						if (operands.size() == 2) {
							for (OWLClassExpression operand : operands) {
								if (operand.isAnonymous() == false) {
									OWLClass currentGenus = operand.asOWLClass();
									if (genus.equals(currentGenus)) {
										matchesGenus = true;
									}
								}
								else if (operand instanceof OWLObjectSomeValuesFrom) {
									OWLObjectSomeValuesFrom differentia = (OWLObjectSomeValuesFrom) operand;
									if (relation.equals(differentia.getProperty())) {
										matchesRelation = true;
										OWLClassExpression filler = differentia.getFiller();
										if (!filler.isAnonymous() && !filler.isOWLNothing() && !filler.isOWLThing()) {
											differentiaCls = filler.asOWLClass();
										}
									}
								}
							}
							if (matchesGenus && matchesRelation ) {
								 return differentiaCls;
							}
						}
					}
				}
			}
		}
		return null;
	}
	
	
	/**
	 * Check that the given subClass does not already has a matching subClass axiom.
	 * 
	 * @param subCls
	 * @param p
	 * @param superCls
	 * @return existing axiom or null
	 */
	private OWLAxiom hasLinks(OWLClass subCls, OWLObjectProperty p, OWLClass superCls) {
		for(OWLOntology o : allOntologies) {
			Set<OWLSubClassOfAxiom> subClsAxioms = o.getSubClassAxiomsForSubClass(subCls);
			for (OWLSubClassOfAxiom subClsAxiom : subClsAxioms) {
				OWLClassExpression ce = subClsAxiom.getSuperClass();
				if (ce instanceof OWLObjectSomeValuesFrom) {
					OWLObjectSomeValuesFrom someValuesFrom = (OWLObjectSomeValuesFrom) ce;
					OWLObjectPropertyExpression property = someValuesFrom.getProperty();
					if (p.equals(property)) {
						OWLClassExpression filler = someValuesFrom.getFiller();
						if (superCls.equals(filler)) {
							return subClsAxiom;
						}
					}
				}
			}
		}
		return null;
	}
	
}
