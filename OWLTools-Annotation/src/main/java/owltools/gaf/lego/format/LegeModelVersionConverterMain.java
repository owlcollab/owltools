package owltools.gaf.lego.format;

import java.io.File;
import java.io.FileFilter;

import org.apache.commons.io.FilenameUtils;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.cli.Opts;
import owltools.io.CatalogXmlIRIMapper;
import owltools.io.ParserWrapper;

public class LegeModelVersionConverterMain {

	public static void main(String[] args) throws Exception {
		ParserWrapper pw = new ParserWrapper();
		String inputFolder = null;
		String outputFolder = null;
		Opts opts = new Opts(args);
		while (opts.hasOpts()) {
			if (opts.nextEq("--use-catalog|--use-catalog-xml"))
				pw.addIRIMapper(new CatalogXmlIRIMapper(new File("catalog-v001.xml").getAbsoluteFile()));
			else if (opts.nextEq("--catalog-xml"))
				pw.addIRIMapper(new CatalogXmlIRIMapper(opts.nextOpt()));
			else if (opts.nextEq("-i|--input-folder")) {
				inputFolder = opts.nextOpt();
			}
			else if (opts.nextEq("-o|--output-folder")) {
				outputFolder = opts.nextOpt();
			}
			else
				break;
		}
		if (inputFolder == null) {
			throw new Exception("No input folder specified");
		}
		if (outputFolder == null) {
			throw new Exception("No output folder specified");
		}
		
		File input = new File(inputFolder).getCanonicalFile();
		File output = new File(outputFolder).getCanonicalFile();
		
		File[] owlFiles = input.listFiles(new FileFilter() {
			
			@Override
			public boolean accept(File pathname) {
				String extension = FilenameUtils.getExtension(pathname.getName());
				if (extension != null) {
					return "owl".equalsIgnoreCase(extension);
				}
				return false;
			}
		});
		if (owlFiles.length == 0) {
			throw new Exception("No owl files found in input folder: "+inputFolder);
		}
		
		LegoModelVersionConverter converter = new LegoModelVersionConverter();
		for (File owlFile : owlFiles) {
			final OWLOntology abox = pw.parseOWL(IRI.create(owlFile));
			final String modelId = FilenameUtils.removeExtension(owlFile.getName());
			converter.convertLegoModelToAllIndividuals(abox, modelId);
			
			final File outputFile = new File(output, owlFile.getName());
			abox.getOWLOntologyManager().saveOntology(abox, IRI.create(outputFile));
		}

	}

}
