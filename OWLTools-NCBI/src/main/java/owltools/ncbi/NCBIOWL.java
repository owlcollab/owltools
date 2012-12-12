package owltools.ncbi;

import java.io.File;

import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;

import org.apache.log4j.Logger;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import org.semanticweb.owlapi.apibinding.OWLManager;

import org.obolibrary.obo2owl.Obo2Owl;
import org.obolibrary.obo2owl.Obo2OWLConstants;
import org.obolibrary.obo2owl.Obo2OWLConstants.Obo2OWLVocabulary;

import owltools.ncbi.NCBIConverter;

/**
 * Provides static methods for creating and initializing an OWLOntology
 * into which taxa can be inserted.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class NCBIOWL extends NCBIConverter {
	/**
	 * Create a logger.
	 */
	protected final static Logger logger =
		Logger.getLogger(NCBIOWL.class);

	/**
	 * Create OWLAPI utilities: A data factory.
	 */
	protected final static OWLDataFactory dataFactory =
		OWLManager.getOWLDataFactory();

	/**
	 * Create OWLAPI utilities: An ontology manager.
	 */
	protected final static OWLOntologyManager manager =
		OWLManager.createOWLOntologyManager(dataFactory);

	/**
	 * Names of the NCBI ranks.
	 */
	public final static HashSet<String> ranks = initializeRanks();
		
	/**
	 * Initialize the ranks.
	 *
	 * @return a set of rank names.
	 */
	private static HashSet<String> initializeRanks() {
		HashSet<String> ranks = new HashSet<String>();
		ranks.add("class");
		ranks.add("family");
		ranks.add("forma");
		ranks.add("genus");
		ranks.add("infraclass");
		ranks.add("infraorder");
		ranks.add("kingdom");
		ranks.add("order");
		ranks.add("parvorder");
		ranks.add("phylum");
		ranks.add("species group");
		ranks.add("species subgroup");
		ranks.add("species");
		ranks.add("subclass");
		ranks.add("subfamily");
		ranks.add("subgenus");
		ranks.add("subkingdom");
		ranks.add("suborder");
		ranks.add("subphylum");
		ranks.add("subspecies");
		ranks.add("subtribe");
		ranks.add("superclass");
		ranks.add("superfamily");
		ranks.add("superkingdom");
		ranks.add("superorder");
		ranks.add("superphylum");
		ranks.add("tribe");
		ranks.add("varietas");
		return ranks;
	}

	/**
	 * A map from NCBI synonym types to OboInOwl annotation properties.
	 */
	public final static HashMap<String,String> synonymTypes =
		initializeFieldMap();

	/**
	 * Initialize the synonymTypes.
	 *
	 * @return a map from field names to CURIE strings for the annotation
	 * properties.
	 */
	private static HashMap<String,String> initializeFieldMap() {
		HashMap<String,String> map = new HashMap();
		map.put("acronym",             "oio:hasBroadSynonym");
		map.put("anamorph",            "oio:hasRelatedSynonym");
		map.put("blast name",          "oio:hasRelatedSynonym");
		map.put("common name",         "oio:hasExactSynonym");
		map.put("equivalent name",     "oio:hasExactSynonym");
		map.put("genbank acronym",     "oio:hasBroadSynonym");
		map.put("genbank anamorph",    "oio:hasRelatedSynonym");
		map.put("genbank common name", "oio:hasExactSynonym");
		map.put("genbank synonym",     "oio:hasRelatedSynonym");
		map.put("in-part",             "oio:hasRelatedSynonym");
		map.put("misnomer",            "oio:hasRelatedSynonym");
		map.put("misspelling",         "oio:hasRelatedSynonym");
		map.put("synonym",             "oio:hasRelatedSynonym");
		map.put("scientific name",     "oio:hasExactSynonym");
		map.put("teleomorph",          "oio:hasRelatedSynonym");
		return map;
	}

	/**
	 * Create and initialize the ontology. This involves adding several
	 * annotation properties from OboInOwl and has_rank,
	 * adding the ranks themselves, and setting annotation properties
	 * on the ontology itself.
	 *
	 * @see org.obolibrary.obo2owl.Obo2OWLConstants.Obo2OWLVocabulary
	 * @return the ontology, ready for adding taxa
	 */
	public static OWLOntology createOWLOntology()
			throws OWLOntologyCreationException {
		IRI iri = IRI.create(OBO + "ncbitaxon.owl");
		OWLOntology ontology = manager.createOntology(iri);
		
		// Add OBO annotation properties
		Obo2OWLVocabulary[] terms = {
			Obo2OWLVocabulary.IRI_OIO_hasExactSynonym,
			Obo2OWLVocabulary.IRI_OIO_hasRelatedSynonym,
			Obo2OWLVocabulary.IRI_OIO_hasBroadSynonym,
			Obo2OWLVocabulary.IRI_OIO_hasDbXref,
			Obo2OWLVocabulary.IRI_OIO_hasOboNamespace,
			Obo2OWLVocabulary.IRI_OIO_hasScope,
			Obo2OWLVocabulary.IRI_OIO_hasOBOFormatVersion,
			Obo2OWLVocabulary.hasSynonymType,
			Obo2OWLVocabulary.IRI_IAO_0000115 // IAO definition
		};
		for (Obo2OWLVocabulary term : terms) {
			createAnnotationProperty(ontology, term);
		}

		// Add has_rank annotation property
		OWLAnnotationProperty rank = createAnnotationProperty(
			ontology, "obo:ncbitaxon#has_rank");
		annotate(ontology, rank, "rdfs:label", "has_rank");
		annotate(ontology, rank, "iao:0000115", "A metadata relation between a class and its taxonomic rank (eg species, family)");
		annotate(ontology, rank, "rdfs:comment", "This is an abstract class for use with the NCBI taxonomy to name the depth of the node within the tree. The link between the node term and the rank is only visible if you are using an obo 1.3 aware browser/editor; otherwise this can be ignored");
		annotate(ontology, rank, "oio:hasOBONamespace", "ncbi_taxonomy");
		
		// Add ranks
		OWLClass taxonomicRank = createTaxon(ontology, 
			"taxonomic_rank");
		annotate(ontology, taxonomicRank, "rdfs:label",
			"taxonomic rank");
		annotate(ontology, taxonomicRank, "rdfs:comment",
			"This is an abstract class for use with the NCBI taxonomy to name the depth of the node within the tree. The link between the node term and the rank is only visible if you are using an obo 1.3 aware browser/editor; otherwise this can be ignored.");
		for(String rankName : ranks) {
			String rankString = reformatName(rankName);
			OWLClass rankClass = createTaxon(ontology, rankString);
			assertSubClass(ontology, rankClass, taxonomicRank);
			annotate(ontology, rankClass, "rdfs:label", rankName);
		}

		// Add synonym type classes
		OWLAnnotationProperty synonymTypeProperty =
			createAnnotationProperty(ontology,
				"oio:SynonymTypeProperty");
		annotate(ontology, synonymTypeProperty, "rdfs:label",
			"synonym_type_property");
		Set<String> synonymNames = synonymTypes.keySet();
		for(String synonymName : synonymNames) {
			String synonymString = reformatName(
				"ncbitaxon:" + synonymName);
			OWLAnnotationProperty synonymProperty =
				createAnnotationProperty(
					ontology, synonymString);
			annotate(ontology, synonymProperty, "rdfs:label", 
				synonymName);
			annotate(ontology, synonymProperty, "oio:hasScope",
				synonymTypes.get(synonymName));
			assertSubAnnotationProperty(ontology,
				synonymProperty, synonymTypeProperty);
		}

		// Annotate the ontology itself.
		annotate(ontology, "rdfs:comment",
			"Autogenerated by OWLTools-NCBIConverter.");
		
		logger.debug("Initialized ontology. Axioms: " +
			ontology.getAxiomCount());
		return ontology;
	}

	/**
	 * Load an ontology from a file.
	 *
	 * @param inputPath the path to the OWL file
	 * @return the loaded ontology
	 * @throws OWLOntologyCreationException if the ontology can't be loaded
	 */
	public static OWLOntology loadOWLOntology(File inputFile)
			throws OWLOntologyCreationException {
		return manager.loadOntologyFromOntologyDocument(inputFile);
	}
}
