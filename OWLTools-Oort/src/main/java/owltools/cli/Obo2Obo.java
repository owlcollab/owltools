package owltools.cli;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.obolibrary.oboformat.model.FrameStructureException;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.parser.OBOFormatParser;
import org.obolibrary.oboformat.writer.OBOFormatWriter;
import org.obolibrary.oboformat.writer.OBOFormatWriter.NameProvider;
import org.obolibrary.oboformat.writer.OBOFormatWriter.OBODocNameProvider;
import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphWrapper;
import owltools.io.CatalogXmlIRIMapper;
import owltools.io.ParserWrapper;

/**
 * Read an OBO file and write it back out as OBO, useful for sorting hand-edited OBO files.
 * Main feature: Use other ontologies to provide names for the comments during the write.
 */
public class Obo2Obo {
	
	private static final Logger logger = Logger.getLogger(Obo2Obo.class);

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		Opts opts = new Opts(args);
		Obo2Obo obo2Obo = new Obo2Obo();
		obo2Obo.run(opts);
	}

	public void run(Opts opts) throws Exception {
		String oboInputFileName = null;
		String oboOutputFileName = null;
		List<String> supports = new ArrayList<String>();
		ParserWrapper pw = new ParserWrapper();
		boolean skipOBODocChecks = false;
		
		while (opts.hasArgs()) {
			if (opts.nextArgIsHelp()) {
				help();
				opts.setHelpMode(true);
			}
			else if (opts.nextEq("-o|--output")) {
				opts.info("OUTPUT-FILE", "specify an output file");
				oboOutputFileName = opts.nextOpt();
			}
			else if (opts.nextEq("-s|--support")) {
				opts.info("SUPPORT-FILE", "specify an support ontology");
				supports.add(opts.nextOpt());
			}
			else if (opts.nextEq("--use-catalog") || opts.nextEq("--use-catalog-xml")) {
				opts.info("", "uses default catalog-v001.xml");
				pw.getManager().addIRIMapper(new CatalogXmlIRIMapper("catalog-v001.xml"));
			}
			else if (opts.nextEq("--catalog-xml")) {
				opts.info("CATALOG-FILE", "uses the specified file as a catalog");
				pw.getManager().addIRIMapper(new CatalogXmlIRIMapper(opts.nextOpt()));
			}
			else if (opts.nextEq("--skip-obo-checks")) {
				opts.info("CATALOG-FILE", "uses the specified file as a catalog");
				pw.getManager().addIRIMapper(new CatalogXmlIRIMapper(opts.nextOpt()));
			}
			else{
				String string = opts.nextOpt();
				if (oboInputFileName != null) {
					error("Only one input file expected but found two:\n"+oboInputFileName+"\n"+string);
				}
				oboInputFileName = string;
			}
		}
		
		// check if there is an output file
		if (oboOutputFileName == null) {
			// if not defined, overwrite the input file
			oboOutputFileName = oboInputFileName;
		}
		
		if (oboInputFileName == null) {
			error("No input file found. Please specify exactly one INPUT");
		}
		
		obo2obo(oboInputFileName, oboOutputFileName, supports, pw, skipOBODocChecks);
	}

	private void obo2obo(String oboInputFileName, String oboOutputFileName, 
			List<String> supports, ParserWrapper pw, boolean skipOBODocChecks) throws Exception
	{
		// load OBO source
		logger.info("Start loading OBO ontology from file: "+oboInputFileName);
		OBOFormatParser oboParser = new OBOFormatParser();
		OBODoc oboDoc;
		try {
			oboDoc = oboParser.parse(oboInputFileName);
		} catch (IOException e) {
			System.err.println("An error occured during the load of the input file '"+oboInputFileName+"' with the error message:\n"+e.getMessage());
			System.exit(-1);
			return;
		}
		logger.info("Finished loading OBO ontology.");
		
		// check structure
		if (!skipOBODocChecks) {
			logger.info("Check OBO document structure.");
			try {
				oboDoc.check();
			} catch (FrameStructureException e) {
				System.err.println("The loaded obo file has an unexpected structure: "+e.getMessage());
				System.exit(-1);
				return;
			}
		}
		else {
			logger.info("SKIPPING - Check OBO document structure.");
		}
		
		// load optional supports for names
		OWLGraphWrapper graph = null;
		if (supports != null && !supports.isEmpty()) {
			logger.info("Start loading support ontologies.");
			for (String support : supports) {
				try {
					if (graph == null) {
						graph = pw.parseToOWLGraph(support);
					}
					else {
						graph.addSupportOntology(pw.parse(support));
					}
				} catch (Exception e) {
					System.err.println("An error occured during the load of the support file '"+support+"' with the error message:/n"+e.getMessage());
					System.exit(-1);
					return;
				}
			}
			logger.info("Finished loading support ontologies.");
		}
		
		// setup name provider
		NameProvider provider;
		if (graph != null) {
			provider = new MergedNameProvider(oboDoc, graph);
		}
		else {
			provider = new OBODocNameProvider(oboDoc);
		}
		
		if (oboInputFileName.equals(oboOutputFileName)) {
			// try writing to temp-file to avoid clobbering the input file
			File tempFile = null;
			try {
				logger.info("Create temporary output file to avoid overwriting input file with invalid data.");
				// create temp-file
				tempFile = File.createTempFile("obo-2-obo-temp-", ".obo");
				
				// write OBO file
				logger.info("Start writing OBO ontology to temporary output file.");
				writeOboFile(oboDoc, tempFile, provider);
				
				// copy file to intended location
				logger.info("Copy temporary file to intended output location: "+oboOutputFileName);
				FileUtils.copyFile(tempFile, new File(oboOutputFileName));
				logger.info("Finished copying OBO ontology to file.");
			}
			finally {
				// delete temp-file
				logger.info("Delete temporary output file.");
				FileUtils.deleteQuietly(tempFile);
			}
		}
		else {
			// write OBO file
			logger.info("Start writing OBO ontology to file: "+oboOutputFileName);
			File outFile = new File(oboOutputFileName);
			writeOboFile(oboDoc, outFile, provider);
			logger.info("Finished writing OBO ontology to file.");
		}
	}
	
	private void writeOboFile(OBODoc doc, File outputFile, NameProvider provider) throws IOException {
		final OBOFormatWriter writer = new OBOFormatWriter();
		writer.setCheckStructure(false);
		BufferedWriter bufferedWriter = null;
		try {
			bufferedWriter = new BufferedWriter(new FileWriter(outputFile));
			writer.write(doc, bufferedWriter, provider);
		}
		finally {
			IOUtils.closeQuietly(bufferedWriter);
		}
	}
	
	private void error(String message) {
		System.err.println(message);
		help();
		System.exit(-1);
	}
	
	private void help() {
		System.out.println("Read an OBO file and write it back out as OBO, useful for sorting hand-edited OBO files.\n" +
				"Main feature: Use other ontologies to provide names for the comments during the write.\n\n" +
				"Parameters: INPUT [-o OUTPUT] [-s SUPPORT]\n" +
				"            Allows multiple supports and catalog xml files");
	}
	
	static class MergedNameProvider extends OBODocNameProvider {

		private final OWLGraphWrapper graph;
		
		MergedNameProvider(OBODoc oboDoc, OWLGraphWrapper wrapper) {
			super(oboDoc);
			this.graph = wrapper;
		}

		@Override
		public String getName(String id) {
			String name = super.getName(id);
			if (name != null) {
				return name;
			}
			OWLObject owlObject = graph.getOWLObjectByIdentifier(id);
			if (owlObject != null) {
				name = graph.getLabel(owlObject);
			}
			return name;
		}

	}
}
