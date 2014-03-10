package owltools.gaf.bioentities;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxOntologyFormat;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import owltools.gaf.bioentities.QuestForOrthologsSeqXMLParser.ProteinListener;
import owltools.vocab.OBOUpperVocabulary;

public class ProteinTools {
	
	private static final Logger logger = Logger.getLogger(ProteinTools.class);
	
	/**
	 * Retrieve the default mapping from db name to the numeric part of the NCBI
	 * taxon id.
	 * 
	 * @return map
	 */
	public static Map<String, String> getDefaultDbToTaxon() {
		Map<String, String> dbToTaxon = new HashMap<String, String>();
		dbToTaxon.put("goa_human", "9606");
		dbToTaxon.put("cgd", "237561");
		dbToTaxon.put("dictyBase", "44689");
		dbToTaxon.put("ecocyc", "83333");
		dbToTaxon.put("fb", "7227");
		dbToTaxon.put("goa_chicken", "9031");
		dbToTaxon.put("goa_cow", "9913");
		dbToTaxon.put("goa_dog", "9615");
		dbToTaxon.put("goa_pig", "9823");
		dbToTaxon.put("gramene_oryza", "39947");
		dbToTaxon.put("mgi", "10090");
		dbToTaxon.put("pombase", "284812");
		dbToTaxon.put("pseudocap", "208964");
		dbToTaxon.put("rgd", "10116");
		dbToTaxon.put("sgd", "559292");
		dbToTaxon.put("tair", "3702");
		dbToTaxon.put("wb", "6239");
		dbToTaxon.put("zfin", "7955");
		dbToTaxon = Collections.unmodifiableMap(dbToTaxon);
		return dbToTaxon;
	}
	
	/**
	 * Create protein ontologies from the qfo files.
	 * 
	 * @param ids set of taxon ids
	 * @param inputFolder the folder for the input qfo seq xml file
	 * @param outputFolder folder for the owl files
	 * @param catalogXML
	 * @throws Exception
	 */
	public static void createProteinOntologies(Set<String> ids, String inputFolder, String outputFolder, String catalogXML) throws Exception {
		final OWLOntologyFormat format = new ManchesterOWLSyntaxOntologyFormat();
		final OWLOntologyManager m = OWLManager.createOWLOntologyManager();
		StringBuilder catalogBuilder = new StringBuilder();
		catalogBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>").append('\n');
		catalogBuilder.append("<catalog>").append('\n');
		for(String taxonId : ids) {
			String outputName = taxonId+".owl";
			File inputFile = new File(inputFolder, taxonId+".xml.gz");
			if (inputFile.exists() == false) {
				inputFile = new File(inputFolder, taxonId+".xml");
			}
			logger.info("Load from file: "+inputFile);

			IRI ontologyId = createProteinOntologyIRI(taxonId);
			OWLOntology ont = createProteinLabelOntology(m, ontologyId, inputFile);
			File outputFile = new File(outputFolder, outputName);
			
			logger.info("Save to file: "+outputFile.getCanonicalPath());
			m.saveOntology(ont, format, IRI.create(outputFile));
			m.removeOntology(ont);
			
			catalogBuilder.append(" <uri name=\""+ontologyId.toString()+"\" uri=\""+outputName+"\"/>").append('\n');
		}
		catalogBuilder.append("</catalog>").append('\n');
		if (catalogXML != null) {
			FileUtils.write(new File(outputFolder, catalogXML), catalogBuilder);
		}
	}
	
	/**
	 * Create an protein ontology IRI for the given subset name.
	 * 
	 * @param name subset
	 * @return IRI
	 */
	public static IRI createProteinOntologyIRI(String name) {
		String id = OBOUpperVocabulary.OBO+"go/protein/subset/"+name+".owl";
		return IRI.create(id);
	}
	
	/**
	 * Create a new ontology (with the given ID) for all proteins in the given
	 * Quest for Orthlogs seq XML file. It will create a named class for each
	 * protein.
	 * 
	 * @param manager
	 * @param ontologyId
	 *            ontology id
	 * @param file
	 *            qfo file
	 * @return ontology
	 * @throws IOException
	 * @throws XMLStreamException
	 * @throws OWLOntologyCreationException
	 */
	public static OWLOntology createProteinLabelOntology(OWLOntologyManager manager, IRI ontologyId, File file) 
			throws IOException, XMLStreamException, OWLOntologyCreationException {
		OWLOntology ontology = manager.createOntology(ontologyId);
		InputStream inputStream = null;
		try {
			if (file.getName().toLowerCase().endsWith(".gz")) {
				inputStream = new GZIPInputStream(new FileInputStream(file));
			}
			else {
				inputStream = new FileInputStream(file);
			}
			// create superClass
			OWLClass proRoot = manager.getOWLDataFactory().getOWLClass(IRI.create(OBOUpperVocabulary.OBO+"PR_000000001"));
			
			createProteinClassesFromQuestForOrthologs(ontology, inputStream, proRoot);
			return ontology;
		}
		finally {
			IOUtils.closeQuietly(inputStream);
		}
	}
	
	
	/**
	 * Add a named class for each protein extracted from the {@link InputStream}.
	 * Expects to stream to be an seqXML from Quest for Orthologs.
	 * 
	 * @param ontology
	 * @param inputStream
	 * @param proRoot
	 * @throws XMLStreamException
	 */
	public static void createProteinClassesFromQuestForOrthologs(final OWLOntology ontology, InputStream inputStream, final OWLClass proRoot) throws XMLStreamException {
		final OWLOntologyManager manager = ontology.getOWLOntologyManager();
		final OWLDataFactory factory = manager.getOWLDataFactory();
		final OWLAnnotationProperty rdfsLabel = factory.getRDFSLabel();
		final OWLAnnotationProperty rdfsComment = factory.getRDFSComment();
		final OWLAnnotationProperty uniqueLabel = factory.getOWLAnnotationProperty(IRI.create("http://purl.obolibrary.org/obo/IAO_0000589"));
		
		QuestForOrthologsSeqXMLParser p = new QuestForOrthologsSeqXMLParser();
		p.addListener(new ProteinListener() {
			
			@Override
			public void handleProtein(String db, String id, String name,
					String uniqueName, String comment, String taxonId) {
				IRI iri = IRI.create(OBOUpperVocabulary.OBO+db+"_"+id);
				OWLClass cls = factory.getOWLClass(iri);
				Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
				// declare class
				axioms.add(factory.getOWLDeclarationAxiom(cls));
				// set names
				if (name == null) {
					name = uniqueName;
				}
				else {
					axioms.add(factory.getOWLAnnotationAssertionAxiom(uniqueLabel, iri, factory.getOWLLiteral(uniqueName)));
				}
				axioms.add(factory.getOWLAnnotationAssertionAxiom(rdfsLabel, iri, factory.getOWLLiteral(name)));
				// add comment
				if (comment != null) {
					axioms.add(factory.getOWLAnnotationAssertionAxiom(rdfsComment, iri, factory.getOWLLiteral(comment)));
				}
				// add this protein as subClass of protein root class
				axioms.add(factory.getOWLSubClassOfAxiom(cls, proRoot));
				
				manager.addAxioms(ontology, axioms);
			}
		});
		p.parse(inputStream);
	}
}
