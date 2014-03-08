package owltools.gaf.bioentities;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
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

	/**
	 * Create protein ontologies from the qfo files.
	 * 
	 * @param ids map of taxon ids to output filenames
	 * @param inputFolder the folder for the input qfo seq xml file
	 * @param outputFolder folder for the owl files
	 * @param catalogXML
	 * @throws Exception
	 */
	public static void createProteinOntologies(Map<String, String> ids, String inputFolder, String outputFolder, String catalogXML) throws Exception {
		final OWLOntologyFormat format = new ManchesterOWLSyntaxOntologyFormat();
		final OWLOntologyManager m = OWLManager.createOWLOntologyManager();
		StringBuilder catalogBuilder = new StringBuilder();
		catalogBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>").append('\n');
		catalogBuilder.append("<catalog>").append('\n');
		for(Entry<String, String> entry : ids.entrySet()) {
			String taxonId = entry.getKey();
			String outputFileName;
			String outputName = entry.getValue();
			if (outputName == null) {
				outputFileName = taxonId+".owl";
			}
			else {
				outputFileName = outputName+".owl";
			}
			File inputFile = new File(inputFolder, taxonId+".xml.gz");
			if (inputFile.exists() == false) {
				inputFile = new File(inputFolder, taxonId+".xml");
			}

			IRI ontologyId = createProteinOntologyIRI(taxonId);
			OWLOntology ont = createProteinLabelOntology(m, ontologyId, inputFile);
			File outputFile = new File(outputFolder, outputFileName);
			
			m.saveOntology(ont, format, IRI.create(outputFile));
			
			catalogBuilder.append(" <uri name=\""+ontologyId.toString()+"\" uri=\""+outputFileName+"\"/>").append('\n');
			if (outputName != null) {
				IRI alternateOntologyId = createProteinOntologyIRI(outputName);
				catalogBuilder.append(" <uri name=\""+alternateOntologyId.toString()+"\" uri=\""+outputFileName+"\"/>").append('\n');
			}
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
