package owltools.ncbi;

import java.util.Set;
import java.util.HashSet;

import org.apache.log4j.Logger;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubAnnotationPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.AddOntologyAnnotation;

import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import org.obolibrary.obo2owl.Obo2OWLConstants;
import org.obolibrary.obo2owl.Obo2OWLConstants.Obo2OWLVocabulary;

/**
 * Provides common methods for converting data into 
 * <a href="http://www.w3.org/TR/owl2-overview/">Web Ontology Language (OWL)</a>
 * format using <a href="http://owlapi.sourceforge.net">OWLAPI</a>.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class OWLConverter {
	/**
	 * Define the base IRI for Open Biomedical Ontologies (OBO).
	 *
	 * @see <a href="https://code.google.com/p/oboformat/source/browse/trunk/src/main/java/org/obolibrary/obo2owl/Obo2OWLConstants.java">org.obolibrary.obo2owl.Obo2OWLConstants</a>
	 */
	protected final static String OBO = 
		Obo2OWLConstants.DEFAULT_IRI_PREFIX;

	/**
	 * Define the base IRI for OboInOwl terms.
	 *
	 * @see <a href="https://code.google.com/p/oboformat/source/browse/trunk/src/main/java/org/obolibrary/obo2owl/Obo2OWLConstants.java">org.obolibrary.obo2owl.Obo2OWLConstants</a>
	 */
	protected final static String OIO =
		Obo2OWLConstants.OIOVOCAB_IRI_PREFIX;

	/**
	 * Define the base IRI for Information Artifact Ontology (IAO) terms.
	 *
	 * @see <a href="code.google.com/p/information-artifact-ontology/">code.google.com/p/information-artifact-ontology/</a>
	 */
	protected final static String IAO = OBO + "IAO_";

	/**
	 * Define the base IRI for NCBI Taxonomy terms.
	 * 
	 * TODO: It would be better to put this in NCBIConverter, but this led
	 * to initialization problems.
	 */
	protected final static String NCBI = OBO + "NCBITaxon_";

	/**
	 * Create a logger.
	 */
	protected final static Logger logger =
		Logger.getLogger(OWLConverter.class);

	/**
	 * Create OWLAPI utilities: An ontology format.
	 */
	protected static RDFXMLOntologyFormat format =
		initializeFormat();

	/**
	 * Initialize the format with standard default prefixes
	 * (rdf, rdfs, xsd, owl) and with the prefixes we will use
	 * (obo, oio, iao).
	 *
	 * @return The format with the prefixes set.
	 */
	protected static RDFXMLOntologyFormat initializeFormat() {
		RDFXMLOntologyFormat format = new RDFXMLOntologyFormat();
		format.copyPrefixesFrom(new DefaultPrefixManager());
		format.setPrefix("obo", OBO);
		format.setPrefix("oio", OIO);
		format.setPrefix("iao", IAO);
		format.setPrefix("ncbi", NCBI);
		format.setPrefix("ncbitaxon", OBO + "ncbitaxon#");
		return format;
	}

	/**
	 * Reformat names for use in IRIs.
	 *
	 * @param name the string to reformat
	 * @return name with underscores in place of whitespace and hyphens
	 */
	protected static String reformatName(String name) {
		return name.replaceAll("\\s", "_")
			   .replaceAll("-", "_");
	}
	
	/**
	 * Create an annotation property and add it to the ontology.
	 *
	 * @param ontology the current ontology
	 * @param iri the IRI for the annotation property
	 * @return the annotation property
	 */
	protected static OWLAnnotationProperty createAnnotationProperty(
			OWLOntology ontology, IRI iri) {
		OWLDataFactory dataFactory = ontology.getOWLOntologyManager().
			getOWLDataFactory();
		OWLAnnotationProperty property =
			dataFactory.getOWLAnnotationProperty(iri);
		declare(ontology, property);
		return property;
	}

	/**
	 * Convenience method taking a string CURIE instead of an IRI.
	 *
	 * @param ontology the current ontology
	 * @param curie will be expanded using the format.getIRI method
	 * @return the annotation property
	 */
	protected static OWLAnnotationProperty createAnnotationProperty(
			OWLOntology ontology, String curie) {
		return createAnnotationProperty(ontology, format.getIRI(curie));
	}

	/**
	 * Convenience method for adding and labelling OboInOwl annotation
	 * properties with its rdfs:label.
	 *
	 * @see org.obolibrary.obo2owl.Obo2OWLConstants.Obo2OWLVocabulary
	 * @param ontology the current ontology
	 * @param term the Obo2Owl term to use
	 * @return the annotation property
	 */
	protected static OWLAnnotationProperty createAnnotationProperty(
			OWLOntology ontology, Obo2OWLVocabulary term) {
		OWLAnnotationProperty property = createAnnotationProperty(
				ontology, term.getIRI());
		annotate(ontology, property, "rdfs:label", term.getLabel());
		return property;
	}

	/**
	 * Convenience method for adding a declaration axiom to the ontology.
	 *
	 * @param ontology the current ontology
	 * @param subject the entity for which the declaration will be added
	 * @return the declaration axiom
	 */
	protected static OWLDeclarationAxiom declare(OWLOntology ontology,
			OWLEntity subject) {
		OWLOntologyManager manager = ontology.getOWLOntologyManager();
		OWLDataFactory dataFactory = manager.getOWLDataFactory();
		OWLDeclarationAxiom axiom = 
			dataFactory.getOWLDeclarationAxiom(subject);
		manager.addAxiom(ontology, axiom);
		return axiom;
	}
	
	/**
	 * Convenience method for adding an annotation assertion to the
	 * ontology.
	 *
	 * @param ontology the current ontology
	 * @param subject the subject of the annotation
	 * @param property the annotation property
	 * @param value the annotation value
	 * @return the annotation axiom
	 */
	protected static OWLAnnotationAssertionAxiom annotate(
			OWLOntology ontology, 
			OWLEntity subject,
			OWLAnnotationProperty property, 
			OWLAnnotationValue value) {
		OWLOntologyManager manager = ontology.getOWLOntologyManager();
		OWLDataFactory dataFactory = manager.getOWLDataFactory();
		OWLAnnotationAssertionAxiom axiom =
			dataFactory.getOWLAnnotationAssertionAxiom(
				property, subject.getIRI(), value);
		manager.addAxiom(ontology, axiom);
		return axiom;
	}

	/**
	 * Convenience method for adding an annotation assertion to the
	 * ontology, taking a CURIE for the property.
	 *
	 * @param ontology the current ontology
	 * @param subject the subject of the annotation
	 * @param propertyCURIE will be expanded to the full annotation
	 *	property IRI
	 * @param value the annotation value
	 * @return the annotation axiom
	 */
	protected static OWLAnnotationAssertionAxiom annotate(
			OWLOntology ontology, OWLEntity subject,
			String propertyCURIE, OWLAnnotationValue value) {
		OWLDataFactory dataFactory = ontology.getOWLOntologyManager().
			getOWLDataFactory();
		IRI iri = format.getIRI(propertyCURIE);
		OWLAnnotationProperty property =
			dataFactory.getOWLAnnotationProperty(iri);
		return annotate(ontology, subject, property, value);
	}

	/**
	 * Convenience method for adding an annotation assertion to the
	 * ontology, taking a CURIE for the property and a string literal.
	 *
	 * @param ontology the current ontology
	 * @param subject the subject of the annotation
	 * @param propertyCURIE will be expanded to the full annotation
	 *	property IRI
	 * @param value the literal value of the annotation
	 * @return the annotation axiom
	 */
	protected static OWLAnnotationAssertionAxiom annotate(
			OWLOntology ontology, OWLEntity subject,
			String propertyCURIE, String value) {
		OWLDataFactory dataFactory = ontology.getOWLOntologyManager().
			getOWLDataFactory();
		OWLLiteral literal = dataFactory.getOWLLiteral(value);
		return annotate(ontology, subject, propertyCURIE, literal);
	}

	/**
	 * Convenience method for adding an annotation assertion to the
	 * ontology, taking a CURIE for the property and an Boolean literal.
	 *
	 * @param ontology the current ontology
	 * @param subject the subject of the annotation
	 * @param propertyCURIE will be expanded to the full annotation
	 *	property IRI
	 * @param value the literal value of the annotation
	 * @return the annotation axiom
	 */
	protected static OWLAnnotationAssertionAxiom annotate(
			OWLOntology ontology, OWLEntity subject,
			String propertyCURIE, boolean value) {
		OWLDataFactory dataFactory = ontology.getOWLOntologyManager().
			getOWLDataFactory();
		OWLLiteral literal = dataFactory.getOWLLiteral(value);
		return annotate(ontology, subject, propertyCURIE, literal);
	}
	
	/**
	 * Convenience method for adding an annotation assertion to the
	 * ontology itself, taking a CURIE for the property and an Boolean literal.
	 *
	 * @param ontology the current ontology
	 * @param propertyCURIE will be expanded to the full annotation
	 *	property IRI
	 * @param value the literal value of the annotation
	 * @return the annotation axiom
	 */
	protected static OWLAnnotation annotate(OWLOntology ontology,
			String propertyCURIE, IRI value) {
		OWLOntologyManager manager = ontology.getOWLOntologyManager();
		OWLDataFactory dataFactory = manager.getOWLDataFactory();
		IRI iri = format.getIRI(propertyCURIE);
		OWLAnnotationProperty property =
			dataFactory.getOWLAnnotationProperty(iri);
		OWLAnnotation annotation = dataFactory.getOWLAnnotation(
				property, value);
		manager.applyChange(
			new AddOntologyAnnotation(ontology, annotation));
		return annotation;
	}

	/**
	 * Convenience method for adding an annotation assertion to the
	 * ontology itself, taking a CURIE for the property and an Boolean literal.
	 *
	 * @param ontology the current ontology
	 * @param propertyCURIE will be expanded to the full annotation
	 *	property IRI
	 * @param value the literal value of the annotation
	 * @return the annotation axiom
	 */
	protected static OWLAnnotation annotate(OWLOntology ontology,
			String propertyCURIE, String value) {
		OWLOntologyManager manager = ontology.getOWLOntologyManager();
		OWLDataFactory dataFactory = manager.getOWLDataFactory();
		IRI iri = format.getIRI(propertyCURIE);
		OWLAnnotationProperty property =
			dataFactory.getOWLAnnotationProperty(iri);
		OWLLiteral literal = dataFactory.getOWLLiteral(value);
		OWLAnnotation annotation = dataFactory.getOWLAnnotation(
				property, literal);
		manager.applyChange(
			new AddOntologyAnnotation(ontology, annotation));
		return annotation;
	}

	/**
	 * Add an synonym annotation, plus an annotation on that annotation
	 * that specified the type of synonym. The second annotation has the
	 * property oio:hasSynonymType.
	 *
	 * @param ontology the current ontology
	 * @param subject the subject of the annotation
	 * @param type the IRI of the type of synonym
	 * @param property the IRI of the annotation property.
	 * @param value the literal value of the synonym
	 * @return the synonym annotation axiom
	 */
	protected static OWLAnnotationAssertionAxiom synonym(
			OWLOntology ontology, OWLEntity subject, 
			OWLAnnotationValue type,
			OWLAnnotationProperty property, 
			OWLAnnotationValue value) {
		OWLOntologyManager manager = ontology.getOWLOntologyManager();
		OWLDataFactory dataFactory = manager.getOWLDataFactory();
		OWLAnnotationProperty hasSynonymType =
			dataFactory.getOWLAnnotationProperty(
				format.getIRI("oio:hasSynonymType"));
		OWLAnnotation annotation = 
			dataFactory.getOWLAnnotation(hasSynonymType, type);
		Set<OWLAnnotation> annotations = new HashSet<OWLAnnotation>();
		annotations.add(annotation);
		OWLAnnotationAssertionAxiom axiom =
			dataFactory.getOWLAnnotationAssertionAxiom(
				property, subject.getIRI(), value,
				annotations);
		manager.addAxiom(ontology, axiom);
		return axiom;
	}

	/**
	 * Add an synonym annotation, plus an annotation on that annotation
	 * that specified the type of synonym. The second annotation has the
	 * property oio:hasSynonymType.
	 *
	 * @param ontology the current ontology
	 * @param subject the subject of the annotation
	 * @param typeCURIE a CURIE string for the type of synonym
	 * @param propertyCURIE a CURIE string for the property
	 * @param value the string value of the synonym
	 * @return the axiom
	 */
	protected static OWLAnnotationAssertionAxiom synonym(
			OWLOntology ontology, OWLEntity subject,
			String typeCURIE, String propertyCURIE, String value) {
		OWLDataFactory dataFactory = ontology.getOWLOntologyManager().
			getOWLDataFactory();
		IRI type = format.getIRI(typeCURIE);
		OWLAnnotationProperty property =
			dataFactory.getOWLAnnotationProperty(
				format.getIRI(propertyCURIE));
		OWLLiteral literal = dataFactory.getOWLLiteral(value);
		return synonym(ontology, subject, type, property, literal);
	}

	/**
	 * Convenience method for asserting a subClass relation between
	 * a parent and child class in an ontology.
	 *
	 * @param ontology the current ontology
	 * @param child the child class
	 * @param parent the parent class
	 * @return the axiom
	 */
	protected static OWLSubClassOfAxiom assertSubClass(
			OWLOntology ontology, OWLClass child, OWLClass parent) {
		OWLOntologyManager manager = ontology.getOWLOntologyManager();
		OWLDataFactory dataFactory = manager.getOWLDataFactory();
		OWLSubClassOfAxiom axiom = 
			dataFactory.getOWLSubClassOfAxiom(child, parent);
		ontology.getOWLOntologyManager().addAxiom(ontology, axiom);
		return axiom;
	}

	/**
	 * Convenience method for asserting a subAnnotationProperty relation 
	 * between a parent and child property in an ontology.
	 *
	 * @param ontology the current ontology
	 * @param child the child property
	 * @param parent the parent property
	 * @return the axiom
	 */
	protected static OWLSubAnnotationPropertyOfAxiom
			assertSubAnnotationProperty(
			OWLOntology ontology, OWLAnnotationProperty child,
			OWLAnnotationProperty parent) {
		OWLOntologyManager manager = ontology.getOWLOntologyManager();
		OWLDataFactory dataFactory = manager.getOWLDataFactory();
		OWLSubAnnotationPropertyOfAxiom axiom = 
			dataFactory.getOWLSubAnnotationPropertyOfAxiom(
				child, parent);
		manager.addAxiom(ontology, axiom);
		return axiom;
	}

	/**
	 * Convenience method to get the first string literal of an
	 * annotation property.
	 *
	 * @param ontology the current ontology
	 * @param taxon the subject
	 * @param property the property
	 * @return null or the label content
	 */
	protected static String getFirstLiteral(OWLOntology ontology,
			OWLEntity taxon, OWLAnnotationProperty property) {
		Set<OWLAnnotation> annotations = taxon.getAnnotations(
			ontology, property);
		if (!annotations.isEmpty()) {
			OWLAnnotationValue value =
				annotations.iterator().next().getValue();
			if (value instanceof OWLLiteral) {
				OWLLiteral literal = (OWLLiteral)value;
				return literal.getLiteral();
			}
		}
		return null;
	}

	/**
	 * Convenience method to get the first string literal of an
	 * annotation property.
	 *
	 * @param ontology the current ontology
	 * @param subject the subject
	 * @param propertyIRI the property IRI
	 * @return null or the literal content
	 */
	protected static String getFirstLiteral(OWLOntology ontology,
			OWLEntity subject, IRI propertyIRI) {
		OWLDataFactory dataFactory = ontology.getOWLOntologyManager().
			getOWLDataFactory();
		OWLAnnotationProperty property =
			dataFactory.getOWLAnnotationProperty(propertyIRI);
		return getFirstLiteral(ontology, subject, property);
	}

	/**
	 * Convenience method to get the first string literal of an
	 * annotation property.
	 *
	 * @param ontology the current ontology
	 * @param subject the subject
	 * @param propertyCURIE the property CURIE
	 * @return null or the literal content
	 */
	protected static String getFirstLiteral(OWLOntology ontology,
			OWLEntity subject, String propertyCURIE) {
		IRI propertyIRI = format.getIRI(propertyCURIE);
		return getFirstLiteral(ontology, subject, propertyIRI);
	}
}
