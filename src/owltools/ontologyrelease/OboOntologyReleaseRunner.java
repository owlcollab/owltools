package owltools.ontologyrelease;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.obolibrary.macro.MacroExpansionVisitor;
import org.obolibrary.obo2owl.Obo2Owl;
import org.obolibrary.obo2owl.Owl2Obo;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.parser.InvalidXrefMapException;
import org.obolibrary.oboformat.parser.OBOFormatConstants.OboFormatTag;
import org.obolibrary.oboformat.parser.OBOFormatDanglingReferenceException;
import org.obolibrary.oboformat.parser.XrefExpander;
import org.obolibrary.oboformat.writer.OBOFormatWriter;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.RemoveAxiom;

import owltools.InferenceBuilder;
import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;
import owltools.io.ParserWrapper;
import owltools.mooncat.Mooncat;

/**
 * This class is a command line utility which builds an ontology release. The
 * command line argument --h or --help provides usage documentation of this
 * utility. This tool called through bin/ontology-release-runner.
 * 
 * @author Shahid Manzoor
 * 
 */
public class OboOntologyReleaseRunner extends ReleaseRunnerFileTools {

	protected final static Logger logger = Logger .getLogger(OboOntologyReleaseRunner.class);

	ParserWrapper parser;
	Mooncat mooncat;
	InferenceBuilder infBuilder;
	OWLPrettyPrinter owlpp;
	OortConfiguration oortConfig;
	
	public OboOntologyReleaseRunner(OortConfiguration oortConfig, File base) throws IOException {
		super(base, logger);
		this.oortConfig = oortConfig; 
	}
	
	public static class OortConfiguration {
		
		public enum MacroStrategy {
			NO_EXPANSION, GCI 
		}
		
		String reasonerName = InferenceBuilder.REASONER_HERMIT;
		boolean asserted = false;
		boolean simple = false;
		boolean allowFileOverWrite = false;
		// TODO - make this an option
		boolean isExpandXrefs = false;
		boolean isRecreateMireot = false;
		boolean isExpandMacros = false;
		boolean isCheckConsistency = true;
		
		public String getReasonerName() {
			return reasonerName;
		}

		public void setReasonerName(String reasonerName) {
			this.reasonerName = reasonerName;
		}

		public boolean isAsserted() {
			return asserted;
		}

		public void setAsserted(boolean asserted) {
			this.asserted = asserted;
		}

		public boolean isSimple() {
			return simple;
		}

		public void setSimple(boolean simple) {
			this.simple = simple;
		}

		public boolean isExpandXrefs() {
			return isExpandXrefs;
		}

		public void setExpandXrefs(boolean isExportBridges) {
			this.isExpandXrefs = isExportBridges;
		}

		public boolean isAllowFileOverWrite() {
			return allowFileOverWrite;
		}

		public void setAllowFileOverWrite(boolean allowFileOverWrite) {
			this.allowFileOverWrite = allowFileOverWrite;
		}
	}

	/**
	 * Check whether the file is new. Throw an {@link IOException}, 
	 * if the file already exists and {@link #allowFileOverWrite} 
	 * is not set to true.
	 * 
	 * @param file
	 * @return file return the same file to allow chaining with other operations
	 * @throws IOException
	 */
	@Override
	protected File checkNew(File file) throws IOException {
		if (!oortConfig.allowFileOverWrite && file.exists() && file.isFile()) {
			boolean allow = allowFileOverwrite(file);
			if (!allow) {
				throw new IOException("Trying to overwrite an existing file: "
						+ file.getAbsolutePath());
			}	
		}
		return file;
	}
	
	/**
	 *  Hook method to handle an unexpected file overwrite request.
	 *  Returns true, if the overwrite is allowed.
	 * 
	 * @param file
	 * @return boolean 
	 * @throws IOException
	 */
	protected boolean allowFileOverwrite(File file) throws IOException {
		/* 
		 * For the command line version this is always false, as no dialog 
		 * with the user is possible. If the user wants to override file 
		 * the command-line flag '--allowOverwrite' has to be used.
		 */
		return false;
	}

	private static String getPathIRI(String path){
		return path;
	}

	public static void main(String[] args) throws IOException,
	OWLOntologyCreationException, OWLOntologyStorageException,
	OBOFormatDanglingReferenceException {

		OWLOntologyFormat format = new RDFXMLOntologyFormat();
		String baseDirectory = ".";

		OortConfiguration oortConfig = new OortConfiguration();
		

		int i = 0;
		Vector<String> paths = new Vector<String>();
		while (i < args.length) {
			String opt = args[i];
			i++;

			if (opt.trim().length() == 0)
				continue;

			logger.info("processing arg: " + opt);
			if (opt.equals("--h") || opt.equals("--help")) {
				usage();
				System.exit(0);
			}

			else if (opt.equals("-outdir")) { baseDirectory = args[i]; i++; }

			/*
			 * else if (opt.equals("-owlversion")) { version = args[i]; i++; }
			 */
			else if (opt.equals("-reasoner")) {
				oortConfig.reasonerName = args[i];
				i++;
			}
			/*
			 * else if (opt.equals("-oboincludes")) { oboIncludes = args[i];
			 * i++; }
			 */
			else if (opt.equals("--asserted")) {
				oortConfig.asserted = true;
			}
			else if (opt.equals("--simple")) {
				oortConfig.simple = true;
			}
			else if (opt.equals("--expand-xrefs")) {
				oortConfig.isExpandXrefs = true;
			}
			else if (opt.equals("--re-mireot")) {
				oortConfig.isRecreateMireot = true;
			}
			else if (opt.equals("--expand-macros")) {
				oortConfig.isExpandMacros = true;
			}
			else if (opt.equals("--allow-overwrite")) {
				oortConfig.allowFileOverWrite = true;
			}
			else {
				String tokens[] = opt.split(" ");
				for (String token : tokens)
					paths.add(token);
			}
		}
		
		File base = new File(baseDirectory);
		logger.info("Base directory path " + base.getAbsolutePath());
		
		OboOntologyReleaseRunner oorr = new OboOntologyReleaseRunner(oortConfig, base);
		
		oorr.createRelease(format, paths);
		boolean success = oorr.createRelease(format, paths);
		String message;
		if (success) {
			message = "Finished release manager process";
		}
		else {
			message = "Finished release manager process, but no release was created.";
		}
		logger.info(message);
	}

	public boolean createRelease(OWLOntologyFormat format, Vector<String> paths) 
			throws IOException, OWLOntologyCreationException, 
				FileNotFoundException, OWLOntologyStorageException 
	{
		String path = null;

		if (paths.size() > 0)
			path = getPathIRI( paths.get(0) );

		logger.info("Processing Ontologies: " + paths);

		parser = new ParserWrapper();
		mooncat = new Mooncat(parser.parseToOWLGraph(path));
		owlpp = new OWLPrettyPrinter(mooncat.getGraph());

		for (int k = 1; k < paths.size(); k++) {
			String p = getPathIRI(paths.get(k));
			mooncat.addReferencedOntology(parser.parseOWL(p));
		}

		// TODO implement a way to specify an individual version or extract from the ontology 
		String version = buildVersionInfo(null);
		if (version != null) {
			addVersion(mooncat.getOntology(), version, mooncat.getManager());
		}

		String ontologyId = Owl2Obo.getOntologyId(mooncat.getOntology());
		ontologyId = ontologyId.replaceAll(".obo$", ""); // temp workaround

		// ----------------------------------------
		// Bridge files
		// ----------------------------------------
		if (oortConfig.isExpandMacros) {
			OWLOntology ont = mooncat.getOntology();
			MacroExpansionVisitor mev = 
				new MacroExpansionVisitor(mooncat.getManager().getOWLDataFactory(), 
						ont, mooncat.getManager());
			ont = mev.expandAll();		
			mooncat.setOntology(ont);

		}
			
		// ----------------------------------------
		// Bridge files
		// ----------------------------------------
		if (oortConfig.isExpandXrefs) {
			logger.info("Creating Bridge Ontologies");

			// Note that this introduces a dependency on the oboformat-specific portion
			// of the oboformat code. Ideally we would like to make everything run
			// independent of obo
			XrefExpander xe;
			try {
				// TODO - make this configurable.
				// currently uses the name "MAIN-bridge-to-EXT" for all
				xe = new XrefExpander(parser.getOBOdoc(), ontologyId+"-bridge-to");
				xe.expandXrefs();
				for (OBODoc tdoc : parser.getOBOdoc().getImportedOBODocs()) {
					String tOntId = tdoc.getHeaderFrame().getClause(OboFormatTag.TAG_ONTOLOGY).getValue().toString();
					logger.info("Generating bridge ontology:"+tOntId);
					Obo2Owl obo2owl = new Obo2Owl();
					OWLOntology tOnt = obo2owl.convert(tdoc);
					saveOntologyInAllFormats(tOntId, format, tOnt);
				}
			} catch (InvalidXrefMapException e) {
				logger.info("Problem during Xref expansion: "+e.getMessage(), e);
			}

			// TODO - macro expansions
		}

		// ----------------------------------------
		// Asserted (non-classified)
		// ----------------------------------------


		if (oortConfig.asserted) {
			logger.info("Creating Asserted Ontology");
			saveInAllFormats(ontologyId, format, "non-classified");
			logger.info("Asserted Ontology Creation Completed");
		}

		// ----------------------------------------
		// Main (asserted plus non-redundant inferred links)
		// ----------------------------------------
		// this is the same as ASSERTED, with certain axiom ADDED

		// this is always on by default
		//  at some point we may wish to make this optional,
		//  but a user would rarely choose to omit the main ontology
		if (true) {

			logger.info("Merging Ontologies (only has effect if multiple ontologies are specified)");
			mooncat.mergeOntologies();

			logger.info("Creating basic ontology");

			if (oortConfig.reasonerName != null) {
				infBuilder = new InferenceBuilder(mooncat.getGraph(), oortConfig.reasonerName);

				logger.info("Creating inferences");
				buildInferences();
				logger.info("Inferences creation completed");
				
				if (oortConfig.isCheckConsistency) {
					logger.info("Checking consistency");
					List<String> incs = infBuilder.performConsistencyChecks();
					if (incs.size() > 0) {
						for (String inc  : incs) {
							logger.error(inc);
						}
					}
					logger.info("Checking consistency completed");
				}
				
				if (true) {
					if (infBuilder.getEquivalentNamedClassPairs().size() > 0) {
						logger.error("WARNING! Found equivalencies between named classes");
						for (OWLEquivalentClassesAxiom eca : infBuilder.getEquivalentNamedClassPairs()) {
							logger.error("Equiv:"+eca);
						}
					}
				}
				
				logger.info("Finding redundant axioms");

				for (OWLAxiom ax : infBuilder.getRedundantAxioms()) {
					// TODO - in future do not remove axioms that are annotated
					logger.info("Removing redundant axiom:"+ax+" // " + owlpp.render(ax));
					mooncat.getManager().applyChange(new RemoveAxiom(mooncat.getOntology(), ax));					
				}

				logger.info("Redundant axioms removed");
			}
			saveInAllFormats(ontologyId, format, null);

		}

		// ----------------------------------------
		// Simple (no MIREOTs, no imports)
		// ----------------------------------------
		// this is the same as MAIN, with certain axiom REMOVED
		if (oortConfig.simple) {

			logger.info("Creating simple ontology");

			/*
			logger.info("Creating Inferences");
			if (reasoner != null) {
				//buildInferredOntology(simpleOnt, manager, reasoner);
				buildInferences(mooncat.getGraph(), mooncat.getManager(), reasoner);

			}
			logger.info("Inferences creation completed");
			 */

			Owl2Obo owl2obo = new Owl2Obo();


			logger.info("Guessing core ontology (in future this can be overridden)");

			Set<OWLClass> coreSubset = new HashSet<OWLClass>();
			for (OWLClass c : mooncat.getOntology().getClassesInSignature()) {
				String idSpace = owl2obo.getIdentifier(c).replaceAll(":.*", "").toLowerCase();
				if (idSpace.equals(ontologyId.toLowerCase())) {
					coreSubset.add(c);
				}
			}

			logger.info("Estimated core ontology number of classes: "+coreSubset.size());
			if (coreSubset.size() == 0) {
				// TODO - make the core subset configurable
				logger.error("cannot determine core subset - simple file will include everything");
			}
			else {
				mooncat.removeSubsetComplementClasses(coreSubset, true);
			}

			saveInAllFormats(ontologyId, format, "simple");
			logger.info("Creating simple ontology completed");

		}		


		// ----------------------------------------
		// End of export file creation
		// ----------------------------------------

		boolean success = commit(version);
		return success;
	}

	/**
	 * Uses reasoner to obtained inferred subclass axioms, and then adds the non-redundant
	 * ones to he ontology
	 * 
	 * @param graph
	 * @param manager
	 * @param reasoner
	 * @return
	 */
	private List<OWLAxiom> buildInferences() {

		OWLGraphWrapper graph = mooncat.getGraph();

		List<OWLAxiom> axioms = infBuilder.buildInferences();

		// TODO: ensure there is a subClassOf axiom for ALL classes that have an equivalence axiom
		for(OWLAxiom ax: axioms){
			logger.info("New axiom:"+ax+" // " + owlpp.render(ax));
			mooncat.getManager().applyChange(new AddAxiom(graph.getSourceOntology(), ax));
		}		
		return axioms;
	}

	private void saveInAllFormats(String ontologyId, OWLOntologyFormat format, String ext) throws OWLOntologyStorageException, IOException, OWLOntologyCreationException {
		String fn = ext == null ? ontologyId :  ontologyId + "-" + ext;
		saveOntologyInAllFormats(fn, format, mooncat.getOntology());
	}

	private void saveOntologyInAllFormats(String fn, OWLOntologyFormat format, OWLOntology ontologyToSave) throws OWLOntologyStorageException, IOException, OWLOntologyCreationException {

		logger.info("Saving: "+fn);

		OutputStream os = getOutputSteam(fn +".owl");
		mooncat.getManager().saveOntology(ontologyToSave, format, os);
		os.close();

		Owl2Obo owl2obo = new Owl2Obo();
		OBODoc doc = owl2obo.convert(ontologyToSave);

		OBOFormatWriter writer = new OBOFormatWriter();

		BufferedWriter bwriter = getWriter(fn +".obo");

		writer.write(doc, bwriter);

		bwriter.close();

	}

	private static void usage() {
		System.out.println("This utility builds an ontology release. This tool is supposed to be run " +
		"from the location where a particular ontology releases are to be maintained.");
		System.out.println("\n");
		System.out.println("bin/ontology-release-runner [OPTIONAL OPTIONS] ONTOLOGIES-FILES");
		System.out
		.println("Multiple obo or owl files are separated by a space character in the place of the ONTOLOGIES-FILES arguments.");
		System.out.println("\n");
		System.out.println("OPTIONS:");
		System.out
		.println("\t\t (-outdir ~/work/myontology) The path where the release will be produced.");
		System.out
		.println("\t\t (-reasoner pellet) This option provides name of reasoner to be used to build inference computation.");
		System.out
		.println("\t\t (--asserted) This unary option produces ontology without inferred assertions");
		System.out
		.println("\t\t (--simple) This unary option produces ontology without included/supported ontologies");
	}

	private static void addVersion(OWLOntology ontology, String version,
			OWLOntologyManager manager) {
		OWLDataFactory fac = manager.getOWLDataFactory();

		OWLAnnotationProperty ap = fac.getOWLAnnotationProperty(Obo2Owl
				.trTagToIRI(OboFormatTag.TAG_REMARK.getTag()));
		OWLAnnotation ann = fac
		.getOWLAnnotation(ap, fac.getOWLLiteral(version));

		if (ontology == null ||
				ontology.getOntologyID() == null ||
				ontology.getOntologyID().getOntologyIRI() == null) {
			// TODO: shahid - can you add a proper error mechanism
			System.err.println("Please set your ontology ID. \n"+
			"In obo-format this should be the same as your ID-space, all in lower case");
		}
		OWLAxiom ax = fac.getOWLAnnotationAssertionAxiom(ontology
				.getOntologyID().getOntologyIRI(), ann);

		manager.applyChange(new AddAxiom(ontology, ax));

	}
}
