package owltools.cli;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.obolibrary.obo2owl.Owl2Obo;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.writer.OBOFormatWriter;
import org.semanticweb.owlapi.io.OWLFunctionalSyntaxOntologyFormat;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.RemoveImport;

import owltools.InferenceBuilder;
import owltools.graph.OWLGraphWrapper;
import owltools.io.CatalogXmlIRIMapper;
import owltools.io.ParserWrapper;

/**
 * Simple command-line tool to assert inferences and (optional) remove redundant relations
 */
public class AssertInferenceTool {
	
	public static void main(String[] args) throws Exception {
		Opts opts = new Opts(args);
		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper graph = null;
		boolean removeRedundant = true;
		List<String> inputs = new ArrayList<String>();
		String outputFileName = null;
		String outputFileFormat = null;
		
		// parse command line parameters
		while (opts.hasArgs()) {
			
			if (opts.nextArgIsHelp()) {
				help();
				opts.setHelpMode(true);
			}
			else if (opts.nextEq("--removeRedundant")) {
				removeRedundant = true;
			}
			else if (opts.nextEq("--keepRedundant")) {
				removeRedundant = false;
			}
			else if (opts.nextEq("-o|--output")) {
				opts.info("OUTPUT-FILE", "specify an output file");
				outputFileName = opts.nextOpt();
			}
			else if (opts.nextEq("-f|--output-format")) {
				opts.info("OUTPUT-FILE-FORMAT", "specify an output file format: obo, owl, ofn, or owx");
				outputFileFormat = opts.nextOpt();
			}
			else if (opts.nextEq("--use-catalog") || opts.nextEq("--use-catalog-xml")) {
				opts.info("", "uses default catalog-v001.xml");
				pw.getManager().addIRIMapper(new CatalogXmlIRIMapper("catalog-v001.xml"));
			}
			else if (opts.nextEq("--catalog-xml")) {
				opts.info("CATALOG-FILE", "uses the specified file as a catalog");
				pw.getManager().addIRIMapper(new CatalogXmlIRIMapper(opts.nextOpt()));
			}
			else {
				inputs.add(opts.nextOpt());
			}
		}
		if (inputs.isEmpty()) {
			error("No input file found. Please specify at least one input.");
		}
		
		// load the first one as main ontology, the rest are used as support ontologies
		for(String input : inputs) {
			if (graph == null) {
				graph = pw.parseToOWLGraph(input);
			}
			else {
				graph.addSupportOntology(pw.parse(input));
			}
		}
		
		boolean useTemp = false;
		if (outputFileName == null) {
			outputFileName = inputs.get(0);
			useTemp = true;
		}
		
		// if no output was specified, guess format from input suffix
		if (outputFileFormat == null) {
			String primaryInput = inputs.get(0).toLowerCase();
			if (primaryInput.endsWith(".obo")) {
				outputFileFormat = "obo";
			}
			else if (primaryInput.endsWith(".owx")) {
				outputFileFormat = "owx";
			}
			else if (primaryInput.endsWith(".ofn")) {
				outputFileFormat = "ofn";
			}
			else {
				outputFileFormat = "owl";
			}
		}
		else {
			outputFileFormat = outputFileFormat.toLowerCase();
		}
		
		// assert inferences
		assertInferences(graph, removeRedundant);
		
		// write ontology
		writeOntology(graph.getSourceOntology(), outputFileName, outputFileFormat, useTemp);
		
	}
	
	private static void error(String message) {
		System.err.println(message);
		help();
		System.exit(-1); // exit with a non-zero error code
	}
	
	private static void help() {
		System.out.println("Loads an ontology (and supports if required), use a reasoner to find and assert inferred + redundant relationships, and write out the ontology.\n" +
				"Parameters: INPUT [-o OUTPUT] [-f OUTPUT-FORMAT] [SUPPORT]\n" +
				"            Allows multiple supports and catalog xml files");
	}

	static void writeOntology(OWLOntology ontology, String outputFileName, 
			String outputFileFormat, boolean useTemp)
			throws Exception 
	{
		// handle writes via a temp file or direct output
		File outputFile;
		if (useTemp) {
			outputFile = File.createTempFile("assert-inference-tool", ".temp");
		}
		else {
			outputFile = new File(outputFileName);
		}
		try {
			writeOntologyFile(ontology, outputFileFormat, outputFile);
			if (useTemp) {
				File target = new File(outputFileName);
				FileUtils.copyFile(outputFile, target);
			}
		}
		finally {
			if (useTemp) {
				// delete temp file
				FileUtils.deleteQuietly(outputFile);
			}
		}
	}

	static void writeOntologyFile(OWLOntology ontology, String outputFileFormat, File outputFile) throws Exception {
		if ("obo".equals(outputFileFormat)) {
			BufferedWriter bufferedWriter = null;
			try {
				Owl2Obo owl2Obo = new Owl2Obo();
				OBODoc oboDoc = owl2Obo.convert(ontology);
				OBOFormatWriter oboWriter = new OBOFormatWriter();
				bufferedWriter = new BufferedWriter(new FileWriter(outputFile));
				oboWriter.write(oboDoc, bufferedWriter);
			}
			finally {
				IOUtils.closeQuietly(bufferedWriter);
			}
		}
		else {
			OWLOntologyFormat format = new RDFXMLOntologyFormat();
			if ("owx".equals(outputFileFormat)) {
				format = new OWLXMLOntologyFormat();
			}
			else if ("ofn".equals(outputFileFormat)) {
				format = new OWLFunctionalSyntaxOntologyFormat(); 
			}
			FileOutputStream outputStream = null;
			try {
				OWLOntologyManager manager = ontology.getOWLOntologyManager();
				outputStream = new FileOutputStream(outputFile);
				manager.saveOntology(ontology, format, outputStream);
			}
			finally {
				IOUtils.closeQuietly(outputStream);
			}
		}
	}
	
	/**
	 * Assert inferred super class relationships and (optional) remove redundant ones.
	 * 
	 * @param graph
	 * @param removeRedundant set to false to not remove redundant super class relations
	 */
	public static void assertInferences(OWLGraphWrapper graph, boolean removeRedundant) {
		
		OWLOntology ontology = graph.getSourceOntology();
		OWLOntologyManager manager = ontology.getOWLOntologyManager();
		OWLDataFactory factory = manager.getOWLDataFactory();
		
		// create ontology with imports and create set of changes to removed the additional imports
		List<OWLOntologyChange> removeImportChanges = new ArrayList<OWLOntologyChange>();
		Set<OWLOntology> supportOntologySet = graph.getSupportOntologySet();
		for (OWLOntology support : supportOntologySet) {
			IRI ontologyIRI = support.getOntologyID().getOntologyIRI();
			OWLImportsDeclaration importDeclaration = factory.getOWLImportsDeclaration(ontologyIRI);
			List<OWLOntologyChange> change = manager.applyChange(new AddImport(ontology, importDeclaration));
			if (!change.isEmpty()) {
				// the change was successful, create remove import for later
				removeImportChanges.add(new RemoveImport(ontology, importDeclaration));
			}
		}
		
		// Inference builder
		InferenceBuilder builder = new InferenceBuilder(graph, InferenceBuilder.REASONER_ELK);
		try {
			// assert inferences
			List<OWLAxiom> inferences = builder.buildInferences(false);
			manager.addAxioms(ontology, new HashSet<OWLAxiom>(inferences));
			
			// optional
			// remove redundant
			if (removeRedundant) {
				Collection<OWLAxiom> redundantAxioms = builder.getRedundantAxioms();
				if (redundantAxioms != null && !redundantAxioms.isEmpty()) {
					manager.removeAxioms(ontology, new HashSet<OWLAxiom>(redundantAxioms));
				}
			}
		}
		finally {
			builder.dispose();
		}
		
		// remove additional import axioms
		manager.applyChanges(removeImportChanges);
	}

}
