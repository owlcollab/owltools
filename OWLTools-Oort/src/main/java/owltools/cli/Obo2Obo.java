package owltools.cli;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

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
		
		obo2obo(oboInputFileName, oboOutputFileName, supports, pw);
	}

	private void obo2obo(String oboInputFileName, String oboOutputFileName, 
			List<String> supports, ParserWrapper pw) throws Exception 
	{
		// load OBO source
		OBOFormatParser oboParser = new OBOFormatParser();
		OBODoc oboDoc = oboParser.parse(oboInputFileName);
		
		
		// load optional supports for names
		OWLGraphWrapper graph = null;
		for (String support : supports) {
			if (graph == null) {
				graph = pw.parseToOWLGraph(support);
			}
			else {
				graph.addSupportOntology(pw.parse(support));
			}
		}
		
		// setup name provider
		NameProvider provider;
		if (graph != null) {
			provider = new MergedNameProvider(oboDoc, graph);
		}
		else {
			provider = new OBODocNameProvider(oboDoc);
		}
		
		// write OBO file
		final OBOFormatWriter writer = new OBOFormatWriter();
		final BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(oboOutputFileName));
		writer.write(oboDoc, bufferedWriter, provider);
		bufferedWriter.close();
	}
	
	private void error(String message) {
		System.err.println(message);
		help();
		System.exit(-1);
	}
	
	private void help() {
		System.out.println("Read an OBO file and write it back out as OBO, useful for sorting hand-edited OBO files.\n" +
				"Main feature: Use other ontologies to provide names for the comments during the write.\n\n" +
				"Usage: obo2obo INPUT [-o OUTPUT] [-s SUPPORT]\n" +
				"       Allows multiple supports and catalog xml files");
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
