package owltools.ncbi;

import java.util.Set;
import java.util.HashSet;

import org.apache.log4j.Logger;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLOntology;

import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

/**
 * Provides common methods for working with NCBI.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class NCBIConverter extends OWLConverter {
	/**
	 * Create a logger.
	 */
	protected final static Logger logger =
		Logger.getLogger(OWLConverter.class);

	/**
	 * Create an NCBI taxon IRI from an NCBI ID string.
	 *
	 * @param id the ID string
	 * @return an IRI of the form http://purl.obolibrary.org/obo/NCBITaxon_1
	 */
	protected static IRI createNCBIIRI(String id) {
		// Handle some special cases.
		if(id.equals("taxonomic_rank") ||
			id.equals("species_group") ||
			id.equals("species_subgroup")) {
			return IRI.create(OBO + "NCBITaxon#_" + id);
		} else {
			return IRI.create(NCBI + id);
		}
	}

	/**
	 * Create an NCBI taxon IRI from an NCBI ID integer.
	 *
	 * @param id the numeric ID
	 * @return an IRI of the form http://purl.obolibrary.org/obo/NCBITaxon_1
	 */
	protected static IRI createNCBIIRI(int id) {
		return IRI.create(NCBI + id);
	}

	/**
	 * Convenience method for asserting a subClass relation between
	 * a parent and child class in an ontology.
	 *
	 * @param ontology the current ontology
	 * @param child the child class
	 * @param parentID the NCBI ID of the parent class
	 * @return the axiom
	 */
	protected static OWLSubClassOfAxiom assertSubClass(
			OWLOntology ontology, OWLClass child, String parentID) {
		OWLDataFactory dataFactory = ontology.getOWLOntologyManager().
			getOWLDataFactory();
		IRI iri = createNCBIIRI(parentID);
		OWLClass parent = dataFactory.getOWLClass(iri);
		return assertSubClass(ontology, child, parent);
	}

	/**
	 * Get the NCBI Taxonomy ID of an OWL Class.
	 *
	 * @param taxon the class
	 * @return null or the NCBI Taxonomy ID as a string
	 */
	public static String getTaxonID(OWLClass taxon) {
		IRI iri = taxon.getIRI();
		String iriString = iri.toString();
		if (iriString.startsWith(NCBI)) {
			return iriString.replaceFirst("^" + NCBI, "");
		} else {
			return null;
		}
	}

	/**
	 * Create a class for an NCBI Taxonomy ID and add it to the ontology.
	 *
	 * @param ontology the current ontology
	 * @param id the NCBI Taxonomy ID, will be expanded into an IRI of the
	 *	form http://purl.obolibrary.org/obo/NCBITaxon_1
	 * @return the new class
	 */
	public static OWLClass createTaxon(OWLOntology ontology, String id) {
		IRI iri = createNCBIIRI(id);
		OWLDataFactory dataFactory = ontology.getOWLOntologyManager().
			getOWLDataFactory();
		OWLClass taxon = dataFactory.getOWLClass(iri);
		declare(ontology, taxon);
		annotate(ontology, taxon, "oio:hasOBONamespace",
			"ncbi_taxonomy");
		return taxon;
	}

	/**
	 * Check to make sure that this taxon has the required properties.
	 *
	 * <ul>
	 * <li>label exists</li>
	 * <li>exactly one superClass</li>
	 * </ul>
	 *
	 * @param ontology the current ontology
	 * @param taxon the subject
	 * @return true if the check succeeds, otherwise false.
	 */
	public static Boolean checkTaxon(OWLOntology ontology, 
			OWLClass taxon) {
		String id = getTaxonID(taxon);

		String label = getFirstLiteral(ontology, taxon,
			"rdfs:label");
		if (label == null || label.trim().length() == 0) {
			logger.error("No SCIENTIFIC NAME provided for " + id);
			return false;
		}

		Set<OWLClassExpression> superClasses =
			taxon.getSuperClasses(ontology);
		if (superClasses.size() < 1 && !id.equals("1")) {
			logger.error("No PARENT ID for " + id);
			return false;
		} else if (superClasses.size() > 1) {
			logger.error("Multiple PARENT IDs for " + id);
			return false;
		}

		return true;
	}
}
