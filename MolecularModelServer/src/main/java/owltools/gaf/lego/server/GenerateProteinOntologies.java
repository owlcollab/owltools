package owltools.gaf.lego.server;

import java.util.HashMap;
import java.util.Map;

import owltools.cli.Opts;
import owltools.gaf.bioentities.ProteinTools;

/**
 * Helper to generate the protein ontologies from the QfO xml files.
 */
public class GenerateProteinOntologies {

	public static void main(String[] args) throws Exception {
		Opts opts = new Opts(args);
		String inputFolder = null;
		String outputFolder = null;
		String catalogXML = "catalog-v001.xml";
		Map<String, String> files = new HashMap<String, String>();
		
		while (opts.hasArgs()) {
			if (opts.nextEq("-i|--input")) {
				inputFolder = opts.nextOpt();
			}
			else if (opts.nextEq("-o|--output")) {
				outputFolder = opts.nextOpt();
			}
			else if (opts.nextEq("-m|--map")) {
				String name = opts.nextOpt();
				String source = opts.nextOpt();
				files.put(source, name);
			}
			else {
				break;
			}
		}
		if (inputFolder == null) {
			System.err.println("No input folder available");
			System.exit(-1);
		}
		if (outputFolder == null) {
			System.err.println("No output folder available");
			System.exit(-1);
		}
		if (files.isEmpty()) {
			System.err.println("No protein files specified");
			System.exit(-1);
		}
		ProteinTools.createProteinOntologies(files, inputFolder, outputFolder, catalogXML);
	}

}
