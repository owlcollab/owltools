package owltools.mooncat;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.obolibrary.obo2owl.Obo2OWLConstants.Obo2OWLVocabulary;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

/**
 * Tools for checking and managing ontology data.
 */
public class OntologyMetaDataTools {

	private static final Logger LOGGER = Logger.getLogger(OntologyMetaDataTools.class);
	
	/**
	 * Functor for resolving conflicts for an annotation property 
	 * and its cardinality constraint.
	 */
	public static interface AnnotationCardinalityConfictHandler {

		/**
		 * Resolve a conflict for a given annotation property and axioms.
		 * The result is either a list of resolved axioms or an exception 
		 * thrown by this method.
		 * 
		 * @param entity 
		 * @param property
		 * @param axioms
		 * @return list of resolved axioms
		 * @throws AnnotationCardinalityException
		 */
		public List<OWLAnnotationAssertionAxiom> handleConflict(OWLEntity entity, OWLAnnotationProperty property, Collection<OWLAnnotationAssertionAxiom> axioms) throws AnnotationCardinalityException;
	}
	
	/**
	 * Exception indication a non-resolvable conflict for an 
	 * annotation property and its cardinality constraint.
	 */
	public static class AnnotationCardinalityException extends Exception {

		// generated
		private static final long serialVersionUID = 2572209869048758572L;

		/**
		 * Create a new Exception.
		 * 
		 * @param message
		 * @param cause
		 */
		public AnnotationCardinalityException(String message, Throwable cause) {
			super(message, cause);
		}

		/**
		 * Create a new Exception.
		 * 
		 * @param message
		 */
		public AnnotationCardinalityException(String message) {
			super(message);
		}
	}
	
	/**
	 * Check the annotations for cardinality violations. 
	 * Try to resolve conflicts with the given handler.   
	 * 
	 * @param ontology the target ontology
	 * @param handler the conflict handler
	 * @throws AnnotationCardinalityException throws exception in case a conflict cannot be resolved by the handler
	 */
	public static void checkAnnotationCardinality(OWLOntology ontology, AnnotationCardinalityConfictHandler handler) throws AnnotationCardinalityException {
		final OWLOntologyManager manager = ontology.getOWLOntologyManager();
		final OWLDataFactory factory = manager.getOWLDataFactory();
		final OWLAnnotationProperty lap = factory.getOWLAnnotationProperty(Obo2OWLVocabulary.IRI_IAO_0000115.getIRI());
		
		for (OWLClass owlClass : ontology.getClassesInSignature(true)) {
			checkOwlEntity(owlClass, lap, ontology, handler, manager);
		}
		for (OWLObjectProperty owlProperty : ontology.getObjectPropertiesInSignature(true)) {
			checkOwlEntity(owlProperty, lap, ontology, handler, manager);
		}
	}

	private static void checkOwlEntity(OWLEntity owlClass,
			final OWLAnnotationProperty lap, OWLOntology ontology,
			AnnotationCardinalityConfictHandler handler,
			final OWLOntologyManager manager)
			throws AnnotationCardinalityException 
	{
		// check cardinality constraint for definition
		Set<OWLAnnotationAssertionAxiom> axioms = ontology.getAnnotationAssertionAxioms(owlClass.getIRI());
		Set<OWLAnnotationAssertionAxiom> defAxioms = new HashSet<OWLAnnotationAssertionAxiom>();
		for (OWLAnnotationAssertionAxiom axiom : axioms) {
			if (lap.equals(axiom.getProperty())) {
				defAxioms.add(axiom);
			}
		}
		if (defAxioms.size() > 1) {
			// handle conflict
			// if conflict is not resolvable, throws exception
			List<OWLAnnotationAssertionAxiom> changed = handler.handleConflict(owlClass, lap, defAxioms);
			for(OWLAnnotationAssertionAxiom axiom : defAxioms) {
				manager.removeAxiom(ontology, axiom);
			}
			for(OWLAnnotationAssertionAxiom axiom : changed) {
				manager.addAxiom(ontology, axiom);
			}
		}
	}
	
	/**
	 * Check the annotations for cardinality violations. 
	 * Try to resolve conflicts with the default handler.   
	 * 
	 * @param ontology the target ontology
	 * @throws AnnotationCardinalityException throws exception in case a conflict cannot be resolved by the handler
	 * @see #DEFAULT_HANDLER
	 */
	public static void checkAnnotationCardinality(OWLOntology ontology) throws AnnotationCardinalityException {
		checkAnnotationCardinality(ontology, DEFAULT_HANDLER);
	}
	
	public static final AnnotationCardinalityConfictHandler DEFAULT_HANDLER = new AnnotationCardinalityConfictHandler() {
		
		@Override
		public List<OWLAnnotationAssertionAxiom> handleConflict(OWLEntity entity, OWLAnnotationProperty property,
				Collection<OWLAnnotationAssertionAxiom> annotations) throws AnnotationCardinalityException {
			
			// only handle definitions
			if (Obo2OWLVocabulary.IRI_IAO_0000115.getIRI().equals(property.getIRI())) {
				// take the first one in the collection
				// (may be random)
				LOGGER.warn("Fixing multiple definition tags for entity: "+entity.getIRI());
				return Collections.singletonList(annotations.iterator().next());
			}
			throw new AnnotationCardinalityException("Could not resolve conflict for property: "+property);
		}
	}; 
}
