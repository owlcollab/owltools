package owltools.ontologyrelease;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.obolibrary.macro.MacroExpansionGCIVisitor;
import org.obolibrary.macro.MacroExpansionVisitor;
import org.obolibrary.obo2owl.OWLAPIOwl2Obo;
import org.obolibrary.obo2owl.Obo2OWLConstants;
import org.obolibrary.obo2owl.Obo2Owl;
import org.obolibrary.obo2owl.OboInOwlCardinalityTools;
import org.obolibrary.obo2owl.OboInOwlCardinalityTools.AnnotationCardinalityException;
import org.obolibrary.obo2owl.Owl2Obo;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.parser.InvalidXrefMapException;
import org.obolibrary.oboformat.parser.OBOFormatConstants.OboFormatTag;
import org.obolibrary.oboformat.parser.OBOFormatParserException;
import org.obolibrary.oboformat.parser.XrefExpander;
import org.obolibrary.oboformat.writer.OBOFormatWriter;
import org.obolibrary.owl.LabelFunctionalSyntaxStorerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.RemoveAxiom;
import org.semanticweb.owlapi.model.RemoveImport;
import org.semanticweb.owlapi.model.SetOntologyID;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.util.OWLEntityRenamer;

import com.google.common.base.Optional;

import owltools.InferenceBuilder;
import owltools.InferenceBuilder.ConsistencyReport;
import owltools.InferenceBuilder.PotentialRedundant;
import owltools.JustifyAssertionsTool;
import owltools.JustifyAssertionsTool.JustifyResult;
import owltools.cli.Opts;
import owltools.gaf.GafDocument;
import owltools.gaf.owl.GAFOWLBridge;
import owltools.gaf.parser.GafObjectsBuilder;
import owltools.graph.AxiomAnnotationTools;
import owltools.graph.OWLGraphWrapper;
import owltools.io.CatalogXmlIRIMapper;
import owltools.io.OWLOboGraphsFormat;
import owltools.io.OWLPrettyPrinter;
import owltools.io.ParserWrapper;
import owltools.mooncat.Mooncat;
import owltools.mooncat.PropertyViewOntologyBuilder;
import owltools.mooncat.QuerySubsetGenerator;
import owltools.ontologyrelease.OortConfiguration.MacroStrategy;
import owltools.ontologyrelease.logging.ErrorReportFileHandler;
import owltools.ontologyrelease.logging.ExplicitReportFileHandler;
import owltools.ontologyrelease.logging.Log4jHandler;
import owltools.ontologyrelease.logging.LogHandler;
import owltools.ontologyrelease.logging.TraceReportFileHandler;
import owltools.ontologyverification.OntologyCheck;
import owltools.ontologyverification.OntologyCheckHandler;
import owltools.ontologyverification.OntologyCheckHandler.CheckSummary;
import owltools.util.OwlHelper;
import uk.ac.manchester.cs.owl.owlapi.OWLImportsDeclarationImpl;
import uk.ac.manchester.cs.owlapi.modularity.ModuleType;
import uk.ac.manchester.cs.owlapi.modularity.SyntacticLocalityModuleExtractor;

/**
 * This class is a command line utility which builds an ontology release. The
 * command line argument --h or --help provides usage documentation of this
 * utility. This tool called through bin/ontology-release-runner.
 * 
 * @author Shahid Manzoor
 * 
 */
public class OboOntologyReleaseRunner extends ReleaseRunnerFileTools {

	final OntologyCheckHandler ontologyChecks;
	ParserWrapper parser;
	Mooncat mooncat;
	OWLPrettyPrinter owlpp;
	OortConfiguration oortConfig;

	public OboOntologyReleaseRunner(OortConfiguration oortConfig, File base, List<LogHandler> handlers) throws IOException {
		super(base, oortConfig.isUseReleaseFolder(), oortConfig.isIgnoreLockFile(), addDefaultHandlers(handlers, oortConfig));
		this.oortConfig = oortConfig;
		this.ontologyChecks = new OntologyCheckHandler(false, oortConfig.getOntologyChecks(), handlers);
	}
	
	static List<LogHandler> addDefaultHandlers(List<LogHandler> handlers, OortConfiguration config) {
		// setup default report files
		Set<String> suffixes = new HashSet<String>();
		suffixes.add("-reasoner-report.txt");
		handlers.add(ExplicitReportFileHandler.createSuffixFiltered(suffixes, config));
		return handlers;
	}

	/**
	 * Check whether the file is new. Throw an {@link IOException}, 
	 * if the file already exists and {@link OortConfiguration#isAllowFileOverWrite} 
	 * is not set to true.
	 * 
	 * @param file
	 * @return file return the same file to allow chaining with other operations
	 * @throws IOException
	 */
	@Override
	protected File checkNew(File file) throws IOException {
		if (!oortConfig.isAllowFileOverWrite() && file.exists() && file.isFile()) {
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

	public static void main(String[] args) {
		// default log handler
		final Log4jHandler log4jHandler = new Log4jHandler(Logger.getLogger(OboOntologyReleaseRunner.class), true);
		
		int exitCode = 0;
		OboOntologyReleaseRunner oorr = null;
		try {
			OortConfiguration oortConfig = new OortConfiguration();
			
			boolean isHelp = parseOortCommandLineOptions(args, oortConfig);
			if (isHelp) {
				System.exit(exitCode);
			}
			
			final List<LogHandler> handlers = new ArrayList<LogHandler>();
			handlers.add(log4jHandler);
	
			final File base = oortConfig.getBase().getCanonicalFile();
			log4jHandler.logInfo("Base directory path " + base.getAbsolutePath());
			
			// setup additional log handlers
			if (oortConfig.getErrorReportFile() != null) {
				handlers.add(new ErrorReportFileHandler(base, oortConfig.getErrorReportFile()));
			}
			if (oortConfig.getTraceReportFile() != null) {
				handlers.add(new TraceReportFileHandler(base, oortConfig.getTraceReportFile()));
			}
			
			oorr = new OboOntologyReleaseRunner(oortConfig, base, handlers);
			
			boolean success = oorr.createRelease();
			String message;
			if (success) {
				message = "Finished release manager process";
			}
			else {
				message = "Finished release manager process, but no release was created.";
			}
			log4jHandler.logInfo(message);
			log4jHandler.logInfo("Done!");
		} catch (OboOntologyReleaseRunnerCheckException e) {
			StringBuilder sb = new StringBuilder();
			sb.append("Stopped Release process. Hint: ");
			sb.append(e.getHint());
			List<String> reasons = e.getReasons();
			if (reasons != null) {
				sb.append(" Reasons: ");
				for (String reason : reasons) {
					sb.append('\n').append(reason);
				}
			}
			log4jHandler.logError(sb.toString(), e);
			exitCode = -1;
		} catch (Throwable e) {
			log4jHandler.logError("Stopped Release process. Reason: "+e.getMessage(), e);
			exitCode = -1;
		} finally {
			if (oorr != null) {
				log4jHandler.logInfo("deleting lock file");
				oorr.deleteLockFile();
			}
		}
		System.exit(exitCode);
	}

	/**
	 * Parse the CLI parameters into the configuration object.
	 * 
	 * @param args
	 * @param oortConfig
	 * @return true, if the parameters contain the help flag.
	 * @throws IOException
	 */
	static boolean parseOortCommandLineOptions(String[] args, OortConfiguration oortConfig) throws IOException {
		
		Opts opts = new Opts(args);
		while (opts.hasArgs()) {


			if (opts.nextEq("--h|--help|-h")) {
				// Do not return after finding the help flag.
				// Go through all checks first, to print the options on system out.
				help();
				opts.setHelpMode(true);
			}
			
			if (opts.nextEq("--outdir|-outdir")) { 
				oortConfig.setBase(new File(opts.nextOpt())); 
			}
			else if (opts.nextEq("--reasoner|-reasoner")) {
				// TODO - deprecate "-reasoner"
				oortConfig.setReasonerName(opts.nextOpt());
			}
			else if (opts.nextEq("--no-reasoner")) {
				oortConfig.setReasonerName(null);
			}
			else if (opts.nextEq("--skip-format")) {
				oortConfig.addToSkipFormatSet(opts.nextOpt());
			}
			else if (opts.nextEq("--prefix")) {
				oortConfig.addSourceOntologyPrefix(opts.nextOpt());
			}
			else if (opts.nextEq("--enforceEL")) {
				// If this option is active, the ontology is 
				// restricted to EL before reasoning!
				oortConfig.setEnforceEL(true);
			}
			else if (opts.nextEq("--makeEL")) {
				// If this option is active, an EL restricted ontology 
				// is written after reasoning.
				oortConfig.setWriteELOntology(true);
			}
			else if (opts.nextEq("--no-subsets")) {
				oortConfig.setWriteSubsets(false);
			}
			else if (opts.nextEq("--force")) {
				oortConfig.setForceRelease(true);
			}
			else if (opts.nextEq("--ignoreLock")) {
				oortConfig.setIgnoreLockFile(true);
			}
			else if (opts.nextEq("--asserted")) {
				oortConfig.setAsserted(true);
			}
			else if (opts.nextEq("--simple")) {
				oortConfig.setSimple(true);
			}
			else if (opts.nextEq("--simple-filtered")) {
				oortConfig.setSimple(true);
				List<String> properties = new ArrayList<String>();
				oortConfig.setFilterSimpleProperties(properties);
				while (opts.hasOpts() == false) {
					properties.add(opts.nextOpt());
				}
			}
			else if (opts.nextEq("--relaxed")) {
				oortConfig.setRelaxed(true);
			}
			else if (opts.nextEq("--expand-xrefs")) {
				oortConfig.setExpandXrefs(true);
			}
			else if (opts.nextEq("--re-mireot")) {
				oortConfig.setRecreateMireot(true);
			}
			else if (opts.nextEq("--repair-cardinality")) {
				oortConfig.setRepairAnnotationCardinality(true);
			}
			else if (opts.nextEq("--justify")) {
				oortConfig.setJustifyAssertedSubclasses(true);
			}
			else if (opts.nextEq("--justify-from")) {
				oortConfig.setJustifyAssertedSubclasses(true);
				oortConfig.setJustifyAssertedSubclassesFrom(opts.nextOpt());
			}
			else if (opts.nextEq("--useIsInferred")) {
				oortConfig.setUseIsInferred(true);
			}
			else if (opts.nextEq("--remove-trailing-qualifiers")) {
				oortConfig.setRemoveTrailingQualifiers(true);
			}
			else if (opts.nextEq("--allow-equivalent-pairs")) {
				oortConfig.setAllowEquivalentNamedClassPairs(true);
			}
			else if (opts.nextEq("--expand-macros")) {
				oortConfig.setExpandMacros(true);
				oortConfig.setMacroStrategy(MacroStrategy.GCI);
			}
			else if (opts.nextEq("--expand-macros-inplace")) {
				oortConfig.setExpandMacros(true);
				oortConfig.setMacroStrategy(MacroStrategy.INPLACE);
			}
			else if (opts.nextEq("--allow-overwrite")) {
				oortConfig.setAllowFileOverWrite(true);
			}
			else if (opts.nextEq("--remove-dangling-before-reasoning")) {
				oortConfig.setRemoveDanglingBeforeReasoning(true);
			}
			else if (opts.nextEq("--add-support-from-imports")) {
				oortConfig.setAddSupportFromImports(true);
			}
			else if (opts.nextEq("--add-imports-from-supports")) {
				oortConfig.setAddImportsFromSupports(true);
			}
			else if (opts.nextEq("--translate-disjoints-to-equivalents")) {
				oortConfig.setTranslateDisjointsToEquivalents(true);
			}
			else if (opts.nextEq("--skip-ontology-checks")) {
				oortConfig.setExecuteOntologyChecks(false);
			}
			else if (opts.nextEq("--skip-release-folder")) {
				oortConfig.setUseReleaseFolder(false);
			}
			else if (opts.nextEq("--bridge-ontology|-b")) {
				oortConfig.addBridgeOntology(opts.nextOpt());
			}
			else if (opts.nextEq("--config-file")) {
				File file = new File(opts.nextOpt());
				OortConfiguration.loadConfig(file , oortConfig);
			}
			else if (opts.nextEq("--rename-entity")) {
				oortConfig.addRewriteIRIMap(IRI.create(opts.nextOpt()), 
						IRI.create(opts.nextOpt()));
			}
			else if (opts.nextEq("--catalog-xml")) {
				oortConfig.setCatalogXML(opts.nextOpt());
			}
			else if (opts.nextEq("--check-for-gaf")) {
				oortConfig.setGafToOwl(true);
			}
			else if (opts.nextEq("--query-ontology")) {
				oortConfig.setUseQueryOntology(true);
				oortConfig.setQueryOntology(opts.nextOpt());
			}
			else if (opts.nextEq("--query-ontology-iri")) {
				oortConfig.setQueryOntologyReferenceIsIRI(true);
				oortConfig.setQueryOntologyReference(opts.nextOpt());
			}
			else if (opts.nextEq("--query-ontology-label")) {
				oortConfig.setQueryOntologyReferenceIsIRI(false);
				oortConfig.setQueryOntologyReference(opts.nextOpt());
			}
			else if (opts.nextEq("--query-ontology-remove-query")) {
				oortConfig.setQueryOntologyReferenceIsIRI(true);
			}
			else if (opts.nextEq("--write-label-owl")) {
				oortConfig.setWriteLabelOWL(true);
			}
			else if (opts.nextEq("--threads")) {
				oortConfig.setThreads(Integer.parseInt(opts.nextOpt()));
			}
			else if (opts.nextEq("--run-obo-basic-dag-check")) {
				oortConfig.setRunOboBasicDagCheck(true);
			}
			else if (opts.nextEq("--skip-remove-redundant")) {
				oortConfig.setRemoveRedunantAxioms(false);
			}
			else if (opts.nextEq("--ignore-potential-redundant")) {
				oortConfig.setCheckPotentialRedundant(false);
			}
			else if (opts.nextEq("--version-report-files")) {
				oortConfig.setVersionReportFiles(true);
			}
			else if (opts.nextEq("--skip-error-modules")) {
				oortConfig.setCreateErrorModules(false);
			}
			else if (opts.nextEq("--error-report")) {
				String errorReportFile = "error-report.txt";
				if (opts.hasArgs() && !opts.hasOpts()) {
					errorReportFile = opts.nextOpt();
				}
				oortConfig.setErrorReportFile(errorReportFile);
			}
			else if (opts.nextEq("--trace-report")) {
				String traceReportFile = "trace-report.txt";
				if (opts.hasArgs() && !opts.hasOpts()) {
					traceReportFile = opts.nextOpt();
				}
				oortConfig.setTraceReportFile(traceReportFile);
			}
			else if (opts.nextEq("--ignore-selected-equivalent-pairs")) {
				Set<String> ignoreSelectedEquivalentPairSet = new HashSet<String>(opts.nextList());
				oortConfig.setIgnoreSelectedEquivalentPairSet(ignoreSelectedEquivalentPairSet);
			}
			else if (opts.nextEq("--ontology-checks")) {
				Set<String> addFlags = new HashSet<String>(); 
				Set<String> removeFlags = new HashSet<String>();
				boolean clear = false;
				while (opts.hasOpts()) {
					if (opts.nextEq("-a")) { // add
						addFlags.add(opts.nextOpt());
					}
					else if (opts.nextEq("-r")) { // remove
						removeFlags.add(opts.nextOpt());
					}
					else if (opts.nextEq("-c|--clear")) {
						clear = true;
					}
					else
						break;
				}
				List<OntologyCheck> checks = oortConfig.getOntologyChecks();
				if (checks == null) {
					checks = new ArrayList<OntologyCheck>();
				}
				if (clear) {
					checks.clear();
				}
				oortConfig.setOntologyChecks(checks);
				for(String shortName : addFlags) {
					OntologyCheck check = OortConfiguration.getOntologyCheck(shortName);
					if (check != null) {
						checks.add(check);
					}
				}
				for(String shortName : removeFlags) {
					OntologyCheck check = OortConfiguration.getOntologyCheck(shortName);
					if (check != null) {
						checks.remove(check);
					}
				}
				
			}
			else if (opts.hasArgs()) {
				oortConfig.addPath(opts.nextOpt());
			}
			else {
				if (opts.isHelpMode()) {
					return true; // has help
				}
			}
		}
		return false; // no help
	}

	/**
	 * Create a release, use the {@link OortConfiguration} instance specified the in constructor. 
	 * 
	 * @return true if the release was successful
	 * @throws IOException
	 * @throws OWLOntologyCreationException
	 * @throws FileNotFoundException
	 * @throws OWLOntologyStorageException
	 * @throws OboOntologyReleaseRunnerCheckException
	 * @throws AnnotationCardinalityException
	 * @throws OBOFormatParserException
	 */
	public boolean createRelease() throws IOException, 
	OWLOntologyCreationException, FileNotFoundException, OWLOntologyStorageException,
	OboOntologyReleaseRunnerCheckException, AnnotationCardinalityException,
	OBOFormatParserException
	{
		return createRelease(oortConfig.getPaths());
	}
	
	private boolean createRelease(Vector<String> allPaths) throws IOException, 
		OWLOntologyCreationException, FileNotFoundException, OWLOntologyStorageException,
		OboOntologyReleaseRunnerCheckException, AnnotationCardinalityException,
		OBOFormatParserException
	{
		if (allPaths.isEmpty()) {
			logError("No files to load found, please specify at least one ontology file.");
			return false;
		}
		List<String> paths;
		List<String> gafs = null;
		if (oortConfig.isGafToOwl()) {
			gafs = new ArrayList<String>();
			paths = new ArrayList<String>();
			for(String path : allPaths) {
                                // TODO - be a bit more sophisticated about this
				if (path.endsWith(".obo") || path.endsWith(".owl") || path.endsWith(".ofn") || path.endsWith(".owx") || path.endsWith(".omn")) {
					paths.add(path);
				}
				else {
					gafs.add(path);
				}
			}
			if (gafs.isEmpty()) {
				logError("No gaf files found, please specify at least one gaf file or disable 'check-for-gaf' mode.");
				return false;
			}
		}
		else {
			if (oortConfig.isUseQueryOntology()) {
				paths = new ArrayList<String>(allPaths.size() + 1);
				paths.add(oortConfig.getQueryOntology());
				paths.addAll(allPaths);
			}
			else {
				paths = allPaths;
			}
		}
		logInfo("Using the following ontologies: " + paths);
		if (gafs != null) {
			logInfo("Using the following gaf files: " +gafs);
		}
		parser = new ParserWrapper();
		String catalogXML = oortConfig.getCatalogXML();
		if (catalogXML != null) {
			parser.addIRIMapper(new CatalogXmlIRIMapper(catalogXML));
		}
		OWLGraphWrapper graph = parser.parseToOWLGraph(paths.get(0));
		if (oortConfig.isAddSupportFromImports()) {
			// add imports to support
			graph.addSupportOntologiesFromImportsClosure(true);
			
			OWLOntology sourceOntology = graph.getSourceOntology();
			Set<OWLImportsDeclaration> importsDeclarations = sourceOntology.getImportsDeclarations();
			OWLOntologyManager manager = sourceOntology.getOWLOntologyManager();
			for (OWLImportsDeclaration owlImportsDeclaration : importsDeclarations) {
				manager.applyChange(new RemoveImport(sourceOntology, owlImportsDeclaration));
			}
		}
		
		// rewrite IRIs
		//  used if two ontologies do not use the same IRI for the same concept
		if (oortConfig.getRewriteIRIMap().size() > 0) {
			OWLEntityRenamer oer = new OWLEntityRenamer(graph.getManager(), graph.getAllOntologies());
			List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange> ();
			for (IRI fromIRI : oortConfig.getRewriteIRIMap().keySet()) {
				changes.addAll( oer.changeIRI(fromIRI, oortConfig.getRewriteIRIMap().get(fromIRI)) );
			}
			logInfo("IRI rewrites: "+changes.size());
			graph.getManager().applyChanges(changes);
			
		}

		mooncat = new Mooncat(graph);
		owlpp = new OWLPrettyPrinter(mooncat.getGraph());

		// A bridge ontology contains axioms connecting classes from different ontologies,
		// but no class declarations or class metadata.
		// Bridge ontologies are commonly used (e.g. GO, phenotype ontologies) to store
		// logical definitions such that the core ontology includes no dangling references.
		// Here we merge in the bridge ontologies into the core ontology
		for (String f : oortConfig.getBridgeOntologies()) {
			OWLOntology ont = parser.parse(f);
			logInfo("Merging "+ont+" into main ontology [loaded from "+f+"]");
			mooncat.getGraph().mergeOntology(ont);
		}

		for (int k = 1; k < paths.size(); k++) {
			String p = paths.get(k);
			OWLOntology ont = parser.parse(p);
			logInfo("Loaded "+ont+" from "+p);
			if (oortConfig.isAutoDetectBridgingOntology() && isBridgingOntology(ont))
				mooncat.mergeIntoReferenceOntology(ont);
			else
				mooncat.addReferencedOntology(ont);
		}
		
		if (oortConfig.isAddImportsFromSupports()) {
			logInfo("Adding imports from supports");
			graph.addImportsFromSupportOntologies();
		}
		
		// load gafs
		if (oortConfig.isGafToOwl()) {
			// prepare an empty ontology for the GAFs to be loaded later
			// use the first gaf file name as ontology id
			String gafResource = gafs.get(0);
			IRI gafIRI;
			if (gafResource.indexOf(':') > 0) {
				// if it contains a colon, assume its an IRI
				gafIRI = IRI.create(gafResource);
			}
			else {
				// assume it is a file, use the filename as id
				gafIRI = IRI.create(new File(gafResource).getName());
			}
			// create ontology with gaf IRI
			OWLOntology gafOntology = graph.getManager().createOntology(gafIRI);
			
			// create the GAF bridge
			GAFOWLBridge gafBridge = new GAFOWLBridge(graph, gafOntology);
			// Do not generate individuals, use a prototype instead
			// This is required for efficient reasoning
			gafBridge.setGenerateIndividuals(false);
			
			// load gaf files
			for(String gaf : gafs) {
				try {
					GafObjectsBuilder builder = new GafObjectsBuilder();
					GafDocument gafdoc = builder.buildDocument(gaf);
					gafBridge.translate(gafdoc);
				} catch (URISyntaxException e) {
					throw new IOException(e);
				}
			}
			
			// update the owl graph wrapper, mooncat, and pretty printer with the new gaf data
			OWLGraphWrapper gafGraph = new OWLGraphWrapper(gafOntology);
			for(OWLOntology ontology : graph.getAllOntologies()) {
				gafGraph.addSupportOntology(ontology);
			}
			mooncat = new Mooncat(gafGraph);
			owlpp = new OWLPrettyPrinter(gafGraph);
		}
		
		if (oortConfig.getSourceOntologyPrefixes() != null) {
			logInfo("The following prefixes will be used to determine "+
					"which classes belong in source:"+oortConfig.getSourceOntologyPrefixes());
			mooncat.setSourceOntologyPrefixes(oortConfig.getSourceOntologyPrefixes());
		}
		
		if (oortConfig.isRepairAnnotationCardinality()) {
			logInfo("Checking and repair annotation cardinality constrains");
			OboInOwlCardinalityTools.checkAnnotationCardinality(mooncat.getOntology());
		}

		if (oortConfig.isExecuteOntologyChecks()) {
			CheckSummary summary = ontologyChecks.afterLoading(mooncat.getGraph());
			if (summary.success == false) {
				if (!oortConfig.isForceRelease()) {
					throw new OboOntologyReleaseRunnerCheckException(summary.message);
				}
				else {
					logWarn("Force Release: ignore "+summary.errorCount+" errors from ontology check, error message: "+summary.message);
				}
			}
		}
		
		final String ontologyId = handleOntologyId();
		final String version = handleVersion(ontologyId);

		if (oortConfig.isWriteLabelOWL()) {
			mooncat.getManager().getOntologyStorers().add(new LabelFunctionalSyntaxStorerFactory());
		}
		
		// ----------------------------------------
		// Macro expansion
		// ----------------------------------------
		// sets gciOntology, if there are macros and the strategy is GCI
		OWLOntology gciOntology = null;
		if (oortConfig.isExpandMacros()) {
			logInfo("expanding macros");
			if (oortConfig.getMacroStrategy() == MacroStrategy.GCI) {
				MacroExpansionGCIVisitor gciVisitor = 
					new MacroExpansionGCIVisitor(mooncat.getOntology(), mooncat.getManager(), false);
				gciOntology = gciVisitor.createGCIOntology();
				logInfo("GCI Ontology has "+gciOntology.getAxiomCount()+" axioms");
				gciVisitor.dispose();
			}
			else {
				OWLOntology ont = mooncat.getOntology();
				MacroExpansionVisitor mev = 
					new MacroExpansionVisitor(ont);
				ont = mev.expandAll();		
				mooncat.setOntology(ont);
				mev.dispose();
				logInfo("Expanded in place; Ontology has "+ont.getAxiomCount()+" axioms");
			}

		}

		// ----------------------------------------
		// Generate bridge ontologies from xref expansion
		// ----------------------------------------
		if (oortConfig.isExpandXrefs()) {
			logInfo("Creating Bridge Ontologies by expanding Xrefs");

			// Note that this introduces a dependency on the oboformat-specific portion
			// of the oboformat code. Ideally we would like to make everything run
			// independent of obo
			XrefExpander xe;
			try {
				// TODO - make this configurable.
				// currently uses the name "MAIN-bridge-to-EXT" for all
				final OBODoc obodoc = parser.getOBOdoc();
				if (obodoc == null) {
					final String message = "Creating Bridge Ontologies is only applicable for OBO ontologies as source.";
					if (!oortConfig.isForceRelease()) {
						throw new OboOntologyReleaseRunnerCheckException(message);
					}
					else {
						logWarn("Force Release: ignore "+message);
					}
				}
				else {
					xe = new XrefExpander(obodoc, ontologyId+"-bridge-to");
					xe.expandXrefs(); // generate imported obo docs from xrefs
					for (OBODoc tdoc : parser.getOBOdoc().getImportedOBODocs()) {
						String tOntId = tdoc.getHeaderFrame().getClause(OboFormatTag.TAG_ONTOLOGY).getValue().toString();
						logInfo("Generating bridge ontology:"+tOntId);
						Obo2Owl obo2owl = new Obo2Owl();
						OWLOntology tOnt = obo2owl.convert(tdoc);
						saveOntologyInAllFormats(ontologyId, tOntId, version, tOnt, null, true);
					}
				}
			} catch (InvalidXrefMapException e) {
				logInfo("Problem during Xref expansion: "+e.getMessage());
			}

			// TODO - option to generate imports
		}
		
		if (oortConfig.isTranslateDisjointsToEquivalents()) {
			mooncat.translateDisjointsToEquivalents();
		}
			
		// ----------------------------------------
		// Asserted (non-classified)
		// ----------------------------------------

		if (oortConfig.isAsserted()) {
			logInfo("Creating Asserted Ontology (copy of original)");
			saveInAllFormats(ontologyId, "non-classified", version, gciOntology);
			logInfo("Asserted Ontology Creation Completed");
		}
		
		// ----------------------------------------
		// Create query from named query (non-classified)
		// ----------------------------------------		

		if (oortConfig.isUseQueryOntology()) {
			logInfo("Use named query to build ontology.");
			String queryReference = oortConfig.getQueryOntologyReference();
			if (queryReference == null || queryReference.isEmpty()) {
				logError("Could not find a named query reference. This is required for the QueryOntology feature.");
				return false;
			}
			
			OWLClass namedQuery;
			if (oortConfig.isQueryOntologyReferenceIsIRI()) {
				IRI iri = IRI.create(queryReference);
				namedQuery = mooncat.getGraph().getOWLClass(iri);
				if (namedQuery == null) {
					logError("Could not find an OWLClass with the IRI: "+iri);
					return false;
				}
			}
			else {
				OWLObject owlObject = mooncat.getGraph().getOWLObjectByLabel(queryReference);
				if (owlObject != null && owlObject instanceof OWLClass) {
					namedQuery = (OWLClass) owlObject;
				}
				else {
					logError("Could not find an OWLClass with the label: "+queryReference);
					return false;
				}
			}
			final String reasonerName = oortConfig.getReasonerName();
			if (reasonerName == null) {
				logError("While using a query ontology a reasoner is required.");
				return false;
			}
			final OWLReasonerFactory reasonerFactory = InferenceBuilder.getFactory(reasonerName);
			
			QuerySubsetGenerator subsetGenerator = new QuerySubsetGenerator();
			Set<OWLOntology> toMerge = mooncat.getGraph().getSupportOntologySet();
			subsetGenerator.createSubOntologyFromDLQuery(namedQuery, mooncat.getGraph(), mooncat.getGraph(), reasonerFactory, toMerge);
			
			if (oortConfig.isRemoveQueryOntologyReference()) {
				logInfo("Removing query term from ontology: "+namedQuery);
				OWLOntology owlOntology = mooncat.getGraph().getSourceOntology();
				Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
				axioms.addAll(owlOntology.getAxioms(namedQuery, Imports.EXCLUDED));
				axioms.addAll(owlOntology.getDeclarationAxioms(namedQuery));
				OWLOntologyManager manager = owlOntology.getOWLOntologyManager();
				manager.removeAxioms(owlOntology, axioms);
				logInfo("Finished removing query term, removed axiom count: "+axioms.size());
			}
			
			logInfo("Finished building ontology from query.");
		}

		// ----------------------------------------
		// Merge in subsets of external ontologies
		// ----------------------------------------
		// only do this if --re-mireot is set
		//
		// note this is done *prior* to reasoning - part of the rationale
		// is that by bringing in a smaller subset we make the reasoning
		// more tractable (though this is less relevant for Elk)
		//
		// This is a mandatory step for checking GAFs, otherwise 
		// the reasoner does not use the loaded support ontologies.
		if ((oortConfig.isRecreateMireot() || oortConfig.isGafToOwl()) && 
				!oortConfig.isUseQueryOntology() &&
				graph.getSupportOntologySet().size() > 0) {
			logInfo("Number of dangling classes in source: "+mooncat.getDanglingClasses().size());
			logInfo("Merging Ontologies (only has effect if multiple ontologies are specified)");
			mooncat.mergeOntologies();
			if (oortConfig.isRepairAnnotationCardinality()) {
				logInfo("Checking and repair annotation cardinality constrains");
				OboInOwlCardinalityTools.checkAnnotationCardinality(mooncat.getOntology());
			}
			saveInAllFormats(ontologyId, "merged", version, gciOntology);

			logInfo("Number of dangling classes in source (post-merge): "+mooncat.getDanglingClasses().size());

			// TODO: option to save as imports
		}
		else if (oortConfig.isRepairAnnotationCardinality()) {
			logInfo("Checking and repair annotation cardinality constrains");
			OboInOwlCardinalityTools.checkAnnotationCardinality(mooncat.getOntology());
		}

		if (oortConfig.isExecuteOntologyChecks()) {
			CheckSummary summary = ontologyChecks.afterMireot(mooncat.getGraph());
			if (summary.success == false) {
				if (!oortConfig.isForceRelease()) {
					throw new OboOntologyReleaseRunnerCheckException(summary.message);
				}
				else {
					logWarn("Force Release: ignore "+summary.errorCount+" errors from ontology check, error message: "+summary.message);
				}
			}
		}

		if (oortConfig.isRemoveDanglingBeforeReasoning()) {
			mooncat.removeDanglingAxioms();
		}

		// ----------------------------------------
		// Main (asserted plus inference of non-redundant links)
		// ----------------------------------------
		// this is the same as ASSERTED, with certain axioms ADDED based on reasoner results

		// this is always on by default
		//  at some point we may wish to make this optional,
		//  but a user would rarely choose to omit the main ontology
		if (true) {

			logInfo("Creating main ontology");
			
			if (oortConfig.getReasonerName() != null) {
				// cache all lines to go into reasoner report
				List<String> reasonerReportLines = new ArrayList<String>();
				
				InferenceBuilder infBuilder = null;
				try {
					infBuilder = handleInferences(ontologyId, version, reasonerReportLines, gciOntology);

					// TEST FOR EQUIVALENT NAMED CLASS PAIRS
					if (true) {
						final List<OWLEquivalentClassesAxiom> equivalentNamedClassPairs = infBuilder.getEquivalentNamedClassPairs();
						if (equivalentNamedClassPairs.size() > 0) {
							List<OWLEquivalentClassesAxiom> filteredAxioms = equivalentNamedClassPairs;
							Set<String> ignoreSelectedEquivalentPairSet = oortConfig.getIgnoreSelectedEquivalentPairSet();
							if (ignoreSelectedEquivalentPairSet != null && !ignoreSelectedEquivalentPairSet.isEmpty()) {
								filteredAxioms = filterEquivalentNamedClassPairs(equivalentNamedClassPairs, ignoreSelectedEquivalentPairSet, graph);
							}
							if (filteredAxioms.size() > 0) {
								logWarn("Found equivalencies between named classes");
								List<String> reasons = new ArrayList<String>();
								for (OWLEquivalentClassesAxiom eca : filteredAxioms) {
									String axiomString = owlpp.render(eca);
									reasons.add(axiomString);
									String message = "EQUIVALENT_CLASS_PAIR\t"+axiomString;
									reasonerReportLines.add(message);
								}
								if (oortConfig.isCreateErrorModules()) {
									createEquivModule(ontologyId, filteredAxioms);
									
								}
								if (oortConfig.isAllowEquivalentNamedClassPairs() == false) {
									// TODO: proper exception mechanism - delay until end?
									if (!oortConfig.isForceRelease()) {
										saveReasonerReport(ontologyId, reasonerReportLines);
										throw new OboOntologyReleaseRunnerCheckException("Found equivalencies between named classes.", reasons, "Use ForceRelease option to ignore this warning.");
									}
								}
							}
							else {
								cleanupEquivModule(ontologyId);
							}

						}
						else {
							cleanupEquivModule(ontologyId);
						}
					}

					// REDUNDANT AXIOMS
					if (oortConfig.isRemoveRedunantAxioms()) {
						logInfo("Finding redundant axioms");
						for (OWLAxiom ax : infBuilder.getRedundantAxioms()) {
							// TODO - in future do not remove axioms that are annotated
							logInfo("Removing redundant axiom:"+ax+" // " + owlpp.render(ax));
							reasonerReportLines.add("REDUNDANT\t"+owlpp.render(ax));
							// note that the actual axiom in the ontology may be different, but with the same
							// structure; i.e. with annotations
							for (OWLAxiom axInOnt : mooncat.getOntology().getAxiomsIgnoreAnnotations(ax)) {
								logInfo("  Actual axiom: "+axInOnt);
								mooncat.getManager().applyChange(new RemoveAxiom(mooncat.getOntology(), axInOnt));	
							}
	
						}
	
						logInfo("Redundant axioms removed");
					}
					else {
						logInfo("Skipping removal of redundant axioms");
					}
					
					if (oortConfig.isCheckPotentialRedundant()) {
						logInfo("Check for potential redundant subClass axioms");
						List<PotentialRedundant> potentialRedundants = infBuilder.checkPotentialRedundantSubClassAxioms();
						if (potentialRedundants != null && !potentialRedundants.isEmpty()) {
							logWarn("Found potential redundant subClass axioms");
							List<String> reasons = new ArrayList<String>();
							
							if (oortConfig.isCreateErrorModules()) {
								createPotentialRedundantModule(ontologyId, potentialRedundants);
							}
							// before printing group by relationship and sort by class A
							Collections.sort(potentialRedundants, PotentialRedundant.PRINT_COMPARATOR);
							
							for (PotentialRedundant redundant : potentialRedundants) {
								StringBuilder sb = new StringBuilder();
								sb.append(owlpp.render(redundant.getClassA())).append(" ");
								sb.append(owlpp.render(redundant.getProperty())).append(" ");
								sb.append(owlpp.render(redundant.getClassB()));
								sb.append(" is also a simple SubClassOf.");
								final String reason = sb.toString();
								reasons.add(reason);
								String message = "POTENTIAL_REDUNDANT\t"+reason;
								reasonerReportLines.add(message);
							}
						}
						else {
							cleanupPotentialRedundantModule(ontologyId);
						}
					}
					
					saveReasonerReport(ontologyId, reasonerReportLines);
				}
				finally {
					if (infBuilder != null) {
						infBuilder.dispose();
					}
				}
			}
			if (oortConfig.isExecuteOntologyChecks()) {
				CheckSummary summary = ontologyChecks.afterReasoning(mooncat.getGraph());
				if (summary.success == false) {
					if (!oortConfig.isForceRelease()) {
						throw new OboOntologyReleaseRunnerCheckException(summary.message);
					}
					else {
						logWarn("Force Release: ignore "+summary.errorCount+" errors from ontology check, error message: "+summary.message);
					}
				}
			}
			if (oortConfig.isRemoveTrailingQualifiers()) {
				// remove all axiom annotations which translate to trailing qualifiers
				AxiomAnnotationTools.reduceAxiomAnnotationsToOboBasic(mooncat.getOntology());
				
			}
			saveInAllFormats(ontologyId, null, version, gciOntology);
		} // --end of building main ontology

		// TODO
		for (PropertyView pv : oortConfig.getPropertyViews()) {
			PropertyViewOntologyBuilder pvob = 
				new PropertyViewOntologyBuilder(mooncat.getGraph().getDataFactory(),
						mooncat.getManager(),
						mooncat.getOntology(),
						mooncat.getOntology(),
						pv.property);

		}

		// ----------------------------------------
		// SUBSETS
		// ----------------------------------------
		// including: named subsets, profile subsets (e.g. EL), simple subsets

		if (oortConfig.isWriteSubsets()) {
			// named subsets
			logInfo("writing named subsets");
			Set<String> subsets = mooncat.getGraph().getAllUsedSubsets();
			for (String subset : subsets) {
				Set<OWLClass> objs = mooncat.getGraph().getOWLClassesInSubset(subset);
				logInfo("subset:"+subset+" #classes:"+objs.size());
				String fn = "subsets/"+subset;

				IRI iri = IRI.create(Obo2OWLConstants.DEFAULT_IRI_PREFIX+ontologyId+"/"+fn+".owl");
				OWLOntology subOnt = mooncat.makeMinimalSubsetOntology(objs, iri);
				logInfo("subOnt:"+subOnt+" #axioms:"+subOnt.getAxiomCount());
				saveOntologyInAllFormats(ontologyId, fn, version, subOnt, gciOntology, true);
			}

		}

		// write EL version
		if(oortConfig.isWriteELOntology()) {
			logInfo("Creating EL ontology");
			OWLGraphWrapper elGraph = InferenceBuilder.enforceEL(mooncat.getGraph());
			saveInAllFormats(ontologyId, "el", version, elGraph.getSourceOntology(), gciOntology);
			logInfo("Finished Creating EL ontology");
		}

		// ----------------------------------------
		// Relaxed (assert inferred subclasses and remove equivalence axioms)
		// ----------------------------------------
		
		if (oortConfig.isRelaxed()) {
			
			logInfo("Creating relaxed ontology");
			
			Set<OWLEquivalentClassesAxiom> rmAxs = mooncat.getOntology().getAxioms(AxiomType.EQUIVALENT_CLASSES);
			logInfo("Removing "+rmAxs.size()+" EquivalentClasses axioms from simple");
			mooncat.getManager().removeAxioms(mooncat.getOntology(), rmAxs);
			
			saveInAllFormats(ontologyId, "relaxed", version, gciOntology);
			logInfo("Creating relaxed ontology completed");
		}
		
		// ----------------------------------------
		// Simple/Basic (no MIREOTs, no imports)
		// ----------------------------------------
		// this is the same as MAIN, with certain axiom REMOVED
		if (oortConfig.isSimple()) {
			handleSimpleOntology(graph, ontologyId, version, gciOntology);
		}		


		// ----------------------------------------
		// End of export file creation
		// ----------------------------------------

		boolean success = commit(version);
		return success;
	}

	// ----------------------------------------
	// Methods for creating and deleting module files.
	// The modules are only create, if the appropriate flag is set and one of the following conditions is meet:
	// 1) Unsatisfiable class
	// 2) Equivalences between names classes
	// 3) potential redundant axioms
	// 
	// The modules are create using the OWL-API modularization strategy BottomUp (BOT).
	// ----------------------------------------
	
	private void createEquivModule(String ontologyId, List<OWLEquivalentClassesAxiom> equivalentNamedClassPairs)
			throws OWLOntologyCreationException, IOException, OWLOntologyStorageException
	{
		Set<OWLEntity> signature = new HashSet<OWLEntity>();
		for(OWLEquivalentClassesAxiom ax : equivalentNamedClassPairs) {
			signature.addAll(ax.getClassesInSignature());
		}
		final String moduleName = "equivalent-classes";
		createModule(ontologyId, moduleName, signature);
	}
	
	private void createUnsatisfiableModule(String ontologyId, Collection<OWLEntity> unsatisfiable)
			throws OWLOntologyCreationException, IOException, OWLOntologyStorageException
	{
		Set<OWLEntity> signature = new HashSet<OWLEntity>(unsatisfiable);
		
		final String moduleName = "unsatisfiable";
		createModule(ontologyId, moduleName, signature);
	}
	
	private void createPotentialRedundantModule(String ontologyId, Collection<PotentialRedundant> redundants)
			throws OWLOntologyCreationException, IOException, OWLOntologyStorageException
	{
		Set<OWLEntity> signature = new HashSet<OWLEntity>();
		for (PotentialRedundant redundant : redundants) {
			signature.addAll(redundant.getAxiomOne().getSignature());
			signature.addAll(redundant.getAxiomTwo().getSignature());
		}
		
		final String moduleName = "potential-redundant";
		createModule(ontologyId, moduleName, signature);
	}

	private void createModule(String ontologyId, String moduleName, Set<OWLEntity> signature)
			throws OWLOntologyCreationException, IOException, OWLOntologyStorageException 
	{
		// create a new manager, avoid unnecessary change events
		final OWLOntologyManager m = OWLManager.createOWLOntologyManager();
		
		// extract module
		SyntacticLocalityModuleExtractor sme = new SyntacticLocalityModuleExtractor(m, mooncat.getOntology(), ModuleType.BOT);
		Set<OWLAxiom> moduleAxioms = sme.extract(signature);
		
		OWLOntology module = m.createOntology(IRI.generateDocumentIRI());
		m.addAxioms(module, moduleAxioms);
		
		// save module
		OutputStream moduleOutputStream = null;
		try {
			moduleOutputStream = getOutputSteam(getModuleFileName(ontologyId, moduleName));
			m.saveOntology(module, moduleOutputStream);
		}
		finally {
			IOUtils.closeQuietly(moduleOutputStream);
		}
	}

	private String getModuleFileName(String ontologyId, String moduleName) {
		return ontologyId+"-"+moduleName+"-module.owl";
	}

	private void cleanupEquivModule(String ontologyId) throws IOException {
		cleanupFile(getModuleFileName(ontologyId, "equivalent-classes"));
	}
	
	private void cleanupUnsatisfiableModule(String ontologyId) throws IOException {
		cleanupFile(getModuleFileName(ontologyId, "unsatisfiable"));
	}
	
	private void cleanupPotentialRedundantModule(String ontologyId) throws IOException {
		cleanupFile(getModuleFileName(ontologyId, "potential-redundant"));
	}

	// ----------------------------------------
	// Other Helper methods
	// ----------------------------------------
	
	/**
	 * Given a list of equivalent classes axiom, remove all axioms which use a
	 * class from the ignore set. Maps the class to it's identifier via the
	 * given graph.
	 * 
	 * @param all
	 * @param ignores
	 * @param graph
	 * @return filtered list
	 */
	private List<OWLEquivalentClassesAxiom> filterEquivalentNamedClassPairs(
			List<OWLEquivalentClassesAxiom> all, Set<String> ignores, OWLGraphWrapper graph)
	{
		List<OWLEquivalentClassesAxiom> filtered = new ArrayList<OWLEquivalentClassesAxiom>(all.size());
		for (OWLEquivalentClassesAxiom axiom : all) {
			Set<OWLClass> namedClasses = axiom.getNamedClasses();
			boolean add = true;
			for (OWLClass owlClass : namedClasses) {
				String id = graph.getIdentifier(owlClass);
				if (ignores.contains(id)) {
					add = false;
					break;
				}
			}
			if (add) {
				filtered.add(axiom);
			}
		}
		return filtered;
	}
	
	private void handleSimpleOntology(OWLGraphWrapper graph, String ontologyId, 
			String version, OWLOntology gciOntology) throws OboOntologyReleaseRunnerCheckException,
			OWLOntologyStorageException, IOException, OWLOntologyCreationException
	{
		logInfo("Creating simple ontology");

		Set<RemoveImport> ris = new HashSet<RemoveImport>();
		for (OWLImportsDeclaration oid : mooncat.getOntology().getImportsDeclarations()) {
			ris.add( new RemoveImport(mooncat.getOntology(), oid) );
		}
		for (RemoveImport ri : ris) {
			mooncat.getManager().applyChange(ri);
		}

		List<String> filterProperties = oortConfig.getFilterSimpleProperties();
		if (filterProperties != null) {
			logInfo("Using a property filter for simple ontology.");
			Set<OWLObjectProperty> filterProps = new HashSet<OWLObjectProperty>();
			for (String s : filterProperties) {
				OWLObjectProperty property = graph.getOWLObjectProperty(s);
				if (property == null) {
					property = graph.getOWLObjectPropertyByIdentifier(s);
				}
				if (property == null) {
					final OWLObject owlObject = graph.getOWLObjectByLabel(s);
					if (owlObject instanceof OWLObjectProperty) {
						property = (OWLObjectProperty) owlObject;
					}
				}
				if (property == null) {
					logError("Could not find OWLObjectProperty for: "+s);
				}
				else {
					filterProps.add(property);
				}
			}
			if (filterProps.isEmpty()) {
				logInfo("Property filter will remove all relations, except subClassOf/is_a.");
			}
			else {
				logInfo("Property filter will retain subClassOf/is_a and the following relationships: "+filterProps);
			}
			Mooncat.retainAxiomsInPropertySubset(mooncat.getOntology(), filterProps);
			logInfo("");
		}

		Set<OWLClass> coreSubset = new HashSet<OWLClass>();
		Set<String> sourcePrefixes = oortConfig.getSourceOntologyPrefixes();
		if (sourcePrefixes == null || sourcePrefixes.isEmpty()) {
			logInfo("Guessing core ontology idspace from ontology id: "+ontologyId.toLowerCase());
			sourcePrefixes = Collections.singleton(ontologyId.toLowerCase());
		}
		else {
			logInfo("Setting core ontology idspace: "+sourcePrefixes);
		}
		
		for (OWLClass c : mooncat.getOntology().getClassesInSignature()) {
			String idSpace = getIdSpace(c);
			if (sourcePrefixes.contains(idSpace)) {
				coreSubset.add(c);
			}
		}

		logInfo("Estimated core ontology number of classes: "+coreSubset.size());
		if (coreSubset.size() == 0) {
			// TODO - make the core subset configurable
			logError("cannot determine core subset - simple file will include everything");
		}
		else {
			mooncat.removeSubsetComplementClasses(coreSubset, true);
		}

		if (!oortConfig.isRelaxed()) {
			// if relaxed was created, than the equivalence axioms, have already been removed
			Set<OWLEquivalentClassesAxiom> rmAxs = mooncat.getOntology().getAxioms(AxiomType.EQUIVALENT_CLASSES);
			logInfo("Removing "+rmAxs.size()+" EquivalentClasses axioms from simple");
			mooncat.getManager().removeAxioms(mooncat.getOntology(), rmAxs);
		}
		mooncat.removeDanglingAxioms();

		/*
		 * before saving as simple ontology remove certain axiom annotations 
		 * to comply with OBO-Basic level.
		 */
		logInfo("Removing axiom annotations which are equivalent to trailing qualifiers");
		AxiomAnnotationTools.reduceAxiomAnnotationsToOboBasic(mooncat.getOntology());
		
		if (oortConfig.isRunOboBasicDagCheck()) {
			logInfo("Start - Verifying DAG requirement for OBO Basic.");
			List<List<OWLObject>> cycles = OboBasicDagCheck.findCycles(mooncat.getGraph());
			if (cycles != null && !cycles.isEmpty()) {
				StringBuilder sb = new StringBuilder();
				for (List<OWLObject> cycle : cycles) {
					sb.append("Cycle[");
					for (OWLObject owlObject : cycle) {
						sb.append(' ');
						sb.append(owlpp.render(owlObject));
					}
					sb.append("]\n");

				}
				if (!oortConfig.isForceRelease()) {
					sb.insert(0, "OBO Basic is not a DAG, found the following cycles:\n");
					throw new OboOntologyReleaseRunnerCheckException(sb.toString());
				}
				else {
					logWarn("Force Release: ignore "+cycles.size()+" cycle(s) in basic ontology, cycles: "+sb.toString());
				}
			}
			logInfo("Finished - Verifying DAG requirement for OBO Basic.");
		}
		
		saveInAllFormats(ontologyId, "simple", version, gciOntology);
		logInfo("Creating simple ontology completed");
	}

	private String getIdSpace(OWLClass c) {
		// warning this is a hack. It assumes that the class IRIs are converted into OBO style IDs!
		return OWLAPIOwl2Obo.getIdentifier(c.getIRI()).replaceAll(":.*", "").toLowerCase();
	}

	private String handleOntologyId() {
		String ontologyId = Owl2Obo.getOntologyId(mooncat.getOntology());
		ontologyId = ontologyId.replaceAll(".obo$", ""); // TODO temp workaround
		return ontologyId;
	}

	private String handleVersion(String ontologyId) {
		// TODO add an option to set/overwrite the version manually via command-line
		// TODO re-use/create a method in obo2owl for creating an version IRI
		String version;
		OWLOntology ontology = mooncat.getOntology();
		OWLOntologyID owlOntologyId = ontology.getOntologyID();
		Optional<IRI> versionIRI = owlOntologyId.getVersionIRI();
		if (versionIRI.isPresent() == false) {
			// set a new version IRI using the current date
			version = OntologyVersionTools.format(new Date());
			versionIRI = Optional.of(IRI.create(Obo2OWLConstants.DEFAULT_IRI_PREFIX+ontologyId+"/"+oortConfig.getVersionSubdirectory()+"/"+version+"/"+ontologyId+".owl"));
			
			OWLOntologyManager m = mooncat.getManager();
			m.applyChange(new SetOntologyID(ontology, new OWLOntologyID(owlOntologyId.getOntologyIRI(), versionIRI)));
		}
		else {
			String versionIRIString = versionIRI.get().toString();
			version = OntologyVersionTools.parseVersion(versionIRIString);
			if (version == null) {
				// use the whole IRI? escape?
				logError("Could not parse a version from ontolgy version IRI: "+versionIRIString);
				version = versionIRIString;
			}
		}
		return version;
	}
	
	/**
	 * Handle all the inference and optional justification steps for the main ontology.
	 * Adds all findings to the reasoner report.
	 * 
	 * @param ontologyId
	 * @param version
	 * @param reasonerReportLines
	 * @param gciOntology
	 * @return infBuilder
	 * 
	 * @throws OWLOntologyStorageException
	 * @throws IOException
	 * @throws OWLOntologyCreationException
	 * @throws OboOntologyReleaseRunnerCheckException
	 */
	private InferenceBuilder handleInferences(String ontologyId, String version, List<String> reasonerReportLines, OWLOntology gciOntology)
			throws OWLOntologyStorageException, IOException, OWLOntologyCreationException, OboOntologyReleaseRunnerCheckException
	{
		logInfo("Using reasoner to add/retract links in main ontology");
		OWLGraphWrapper g = mooncat.getGraph();
		final OWLOntology ont = g.getSourceOntology();
		final OWLOntologyManager manager = ont.getOWLOntologyManager();
		final OWLDataFactory factory = manager.getOWLDataFactory();
		final Set<OWLSubClassOfAxiom> removedSubClassOfAxioms = new HashSet<OWLSubClassOfAxiom>();
		final Set<RemoveAxiom> removedSubClassOfAxiomChanges = new HashSet<RemoveAxiom>();
		final InferenceBuilder infBuilder = new InferenceBuilder(g, oortConfig.getReasonerName(), oortConfig.isEnforceEL()) {

			@Override
			protected void logInfo(String msg) {
				OboOntologyReleaseRunner.this.logInfo(msg);
			}

			@Override
			protected boolean isDebug() {
				return false;
			}
			
		};

		// CONSISTENCY CHECK
		// A consistent ontology is a primary for sensible reasoning results. 
		if (oortConfig.isCheckConsistency()) {
			logInfo("Checking consistency");
			ConsistencyReport consistencyReport = infBuilder.performConsistencyChecks();
			if (consistencyReport.errors.size() > 0) {
				for (String inc : consistencyReport.errors) {
					String message = "PROBLEM\t" + inc;
					reasonerReportLines.add(message);
				}
				if (oortConfig.isCreateErrorModules() && consistencyReport.unsatisfiable != null && consistencyReport.unsatisfiable.isEmpty() == false) {
					createUnsatisfiableModule(ontologyId, consistencyReport.unsatisfiable);
				}
				// TODO: proper exception mechanism - delay until end?
				if (!oortConfig.isForceRelease()) {
					saveReasonerReport(ontologyId, reasonerReportLines);
					throw new OboOntologyReleaseRunnerCheckException("Found problems during intial checks.", 
							consistencyReport.errors, "Use ForceRelease option to ignore this warning.");
				}
			}
			else {
				cleanupUnsatisfiableModule(ontologyId);
			}
			logInfo("Checking consistency completed");
		}
		
		// optionally remove a subset of the axioms we want to attempt to recapitulate
		if (oortConfig.isJustifyAssertedSubclasses()) {
			if (oortConfig.isUseIsInferred()) {
				removeInferredAxioms(removedSubClassOfAxioms, removedSubClassOfAxiomChanges);
			}
			else {
				removeInferredOld(infBuilder, removedSubClassOfAxioms, removedSubClassOfAxiomChanges);
			}
			
			logInfo("Removing "+removedSubClassOfAxiomChanges.size()+" axioms");
			for (RemoveAxiom rmax : removedSubClassOfAxiomChanges) {
				manager.applyChange(rmax);
			}
			saveInAllFormats(ontologyId, "minimal", version, gciOntology);
		}

		logInfo("Creating inferences");				
		List<OWLAxiom> inferredAxioms = infBuilder.buildInferences();

		if (oortConfig.isJustifyAssertedSubclasses()) {
			OWLReasoner reasoner = infBuilder.getReasoner(ont);
			JustifyResult result = JustifyAssertionsTool.justifySubClasses(ont, reasoner, removedSubClassOfAxioms, inferredAxioms);
			for (OWLAxiom ax : result.getExistsEntailed()) {
				// add to ontology and report
				addAxiom("EXISTS, ENTAILED", ax, ont, manager, factory, reasonerReportLines);
			}
			for (OWLAxiom ax : result.getNewInferred()) {
				// add to ontology and report
				addAxiom("NEW, INFERRED", ax, ont, manager, factory, reasonerReportLines);
			}
			for (OWLAxiom ax : result.getExistsRedundant()) {
				// report only
				String rptLine = "EXISTS, REDUNDANT\t"+owlpp.render(ax);
				reasonerReportLines.add(rptLine);
			}
			for (OWLAxiom ax : result.getExistsNotEntailed()) {
				// add to ontology and report
				manager.applyChange(new AddAxiom(ont, ax));
				String rptLine = "EXISTS, NOT-ENTAILED\t"+owlpp.render(ax);
				reasonerReportLines.add(rptLine);
			}
		}
		else {
			// default for non-justify mode
			for(OWLAxiom ax: inferredAxioms) {
				if (ax instanceof OWLSubClassOfAxiom && 
						((OWLSubClassOfAxiom)ax).getSuperClass().isOWLThing()) {
					// ignore owlThing as superClass
					continue;
				}
				String info = "NEW, INFERRED";
				if (ax instanceof OWLSubClassOfAxiom && 
						!(((OWLSubClassOfAxiom)ax).getSuperClass() instanceof OWLClass)) {
					// because the reasoner API can only generated subclass axioms with named superclasses,
					// we assume that any that have anonymous expressions as superclasses were generated
					// by the inference builder in the process of translating equivalence axioms
					// to weaker subclass axioms
					info = "NEW, TRANSLATED";
				}
				addAxiom(info, ax, ont, manager, factory, reasonerReportLines);
			}
		}
		logInfo("Inferences creation completed");
		
		return infBuilder;
	}
	
	private void addAxiom(String info, OWLAxiom ax, OWLOntology ont, OWLOntologyManager manager, OWLDataFactory factory, List<String> reasonerReportLines) {
		if (oortConfig.isUseIsInferred()) {
			ax = AxiomAnnotationTools.markAsInferredAxiom(ax, factory);
		}
		manager.applyChange(new AddAxiom(ont, ax));
		String ppax = owlpp.render(ax);
		String rptLine = info+"\t"+ppax;
		reasonerReportLines.add(rptLine);
	}

	/**
	 * Use a heuristic to guess which links can be inferred from logic
	 * definitions.<br>
	 * This method will be replaced by
	 * {@link #removeInferredAxioms(Set, Set)}, which relies on axiom
	 * annotations to identify axioms marked as inferred.
	 * 
	 * @param infBuilder 
	 * @param removedSubClassOfAxioms
	 * @param removedSubClassOfAxiomChanges
	 * 
	 * @throws OWLOntologyStorageException
	 * @throws IOException
	 * @throws OWLOntologyCreationException
	 * 
	 * @deprecated use #removeInferredNew(String, Set, Set) to replace this method
	 */
	@Deprecated
	private void removeInferredOld(InferenceBuilder infBuilder, Set<OWLSubClassOfAxiom> removedSubClassOfAxioms, Set<RemoveAxiom> removedSubClassOfAxiomChanges)
			throws OWLOntologyStorageException, IOException, OWLOntologyCreationException 
	{
		final OWLGraphWrapper g = mooncat.getGraph();
		final OWLOntology ont = g.getSourceOntology();
		String from = oortConfig.getJustifyAssertedSubclassesFrom();
		final Set<OWLClass> markedClasses;
		OWLClass fromClass = from == null ? null: g.getOWLClassByIdentifier(from);
		if (fromClass == null) {
			logInfo("Removing asserted subclasses between defined class pairs");
			markedClasses = null;
		}
		else {
			OWLReasoner reasoner = infBuilder.getReasoner(ont);
			NodeSet<OWLClass> nodeSet = reasoner.getSubClasses(fromClass, false);
			
			if (nodeSet == null || nodeSet.isEmpty() || nodeSet.isBottomSingleton()) {
				logWarn("No subclasses found for class: "+owlpp.render(fromClass));
				markedClasses = Collections.singleton(fromClass);
			}
			else {
				markedClasses = new HashSet<OWLClass>(nodeSet.getFlattened());
				markedClasses.add(fromClass);
			}
			infBuilder.setReasoner(null); // reset reasoner
		}
		for (OWLSubClassOfAxiom a : ont.getAxioms(AxiomType.SUBCLASS_OF)) {
			OWLClassExpression subClassExpression = a.getSubClass();
			if (subClassExpression.isAnonymous()) {
				continue;
			}
			OWLClassExpression superClassExpression = a.getSuperClass();
			if (superClassExpression.isAnonymous()) {
				continue;
			}
			OWLClass subClass = subClassExpression.asOWLClass();
			OWLClass superClass = superClassExpression.asOWLClass();
			
			if (OwlHelper.getEquivalentClasses(subClass, ont).isEmpty()) {
				continue;
			}
			if (OwlHelper.getEquivalentClasses(superClass, ont).isEmpty()) {
				continue;
			}
			if (markedClasses != null) {
				boolean usesMarkedAxiomSubClass = false;
				boolean usesMarkedAxiomSuperClass = false;
				Set<OWLEquivalentClassesAxiom> subClassEqAxioms = ont.getEquivalentClassesAxioms(subClass);
				for (OWLEquivalentClassesAxiom equivalentClassesAxiom : subClassEqAxioms) {
					Set<OWLClass> classesInSignature = equivalentClassesAxiom.getClassesInSignature();
					for (OWLClass owlClass : classesInSignature) {
						if (markedClasses.contains(owlClass)) {
							usesMarkedAxiomSubClass = true;
							break;
						}
					}
				}
				Set<OWLEquivalentClassesAxiom> superClassEqAxioms = ont.getEquivalentClassesAxioms(superClass);
				for (OWLEquivalentClassesAxiom equivalentClassesAxiom : superClassEqAxioms) {
					Set<OWLClass> classesInSignature = equivalentClassesAxiom.getClassesInSignature();
					for (OWLClass owlClass : classesInSignature) {
						if (markedClasses.contains(owlClass)) {
							usesMarkedAxiomSuperClass = true;
							break;
						}
					}
				}
				
				if (!usesMarkedAxiomSubClass || !usesMarkedAxiomSuperClass) {
					continue;
				}
			}
			RemoveAxiom rmax = new RemoveAxiom(ont, a);
			removedSubClassOfAxiomChanges.add(rmax);
			removedSubClassOfAxioms.add(a);
		}
	}
	
	/**
	 * Remove inferred axioms, which are marked by the appropriate axiom annotation. 
	 * 
	 * @param removedSubClassOfAxioms
	 * @param removedSubClassOfAxiomChanges
	 * 
	 * @see AxiomAnnotationTools#isMarkedAsInferredAxiom(OWLAxiom)
	 */
	private void removeInferredAxioms(Set<OWLSubClassOfAxiom> removedSubClassOfAxioms, Set<RemoveAxiom> removedSubClassOfAxiomChanges)
	{
		final OWLOntology ont = mooncat.getGraph().getSourceOntology();
		for (OWLSubClassOfAxiom a : ont.getAxioms(AxiomType.SUBCLASS_OF)) {
			if (AxiomAnnotationTools.isMarkedAsInferredAxiom(a)) {
				RemoveAxiom rmax = new RemoveAxiom(ont, a);
				removedSubClassOfAxiomChanges.add(rmax);
				removedSubClassOfAxioms.add(a);
			}
		}
	}

	/**
	 * @param ontologyId
	 * @param ext
	 * @param version
	 * @param gciOntology
	 * @throws OWLOntologyStorageException
	 * @throws IOException
	 * @throws OWLOntologyCreationException
	 */
	private void saveInAllFormats(String ontologyId, String ext, String version, OWLOntology gciOntology) throws OWLOntologyStorageException, IOException, OWLOntologyCreationException {
		saveInAllFormats(ontologyId, ext, version, mooncat.getOntology(), gciOntology);
	}

	private void saveInAllFormats(String ontologyId, String ext, String version, OWLOntology ontologyToSave, OWLOntology gciOntology) throws OWLOntologyStorageException, IOException, OWLOntologyCreationException {
		if (ext == null || ext.isEmpty()) {
			saveOntologyInAllFormats(ontologyId, ontologyId, version, ontologyToSave, gciOntology, false);
		}
		else {
			saveOntologyInAllFormats(ontologyId, ontologyId + "-" + ext, version, ontologyToSave, gciOntology, true);
		}
	}

	private void saveOntologyInAllFormats(String idspace, String fileNameBase, String version, OWLOntology ontologyToSave, OWLOntology gciOntology, boolean changeOntologyId) throws OWLOntologyStorageException, IOException, OWLOntologyCreationException {

		logInfo("Saving: "+fileNameBase);

		final OWLOntologyManager manager = mooncat.getManager();

		// if we add a new ontology id, remember the change, to restore the original 
		// ontology id after writing into a file.
		SetOntologyID reset = null;

		boolean writeOWL = !oortConfig.isSkipFormat("owl");
        boolean writeOWX = !oortConfig.isSkipFormat("owx");
        boolean writeJSON = !oortConfig.isSkipFormat("json");
		boolean writeOFN = oortConfig.isWriteLabelOWL();
		
		if (changeOntologyId && (writeOWL || writeOWX)) {
			final OWLOntologyID owlOntologyID = ontologyToSave.getOntologyID();
			
			// create temporary id using the file name base to distinguish between the different release types
			// pattern: OBO_PREFIX / ID-SPACE / NAME .owl
			final IRI newOntologyIRI = IRI.create(Obo2OWLConstants.DEFAULT_IRI_PREFIX+idspace+"/"+fileNameBase+".owl");
			
			// create temporary version IRI
			// pattern: OBO_PREFIX / ID-SPACE / VERSION / NAME .owl
			final IRI newVersionIRI = IRI.create(Obo2OWLConstants.DEFAULT_IRI_PREFIX+idspace+"/"+version+"/"+fileNameBase+".owl");
			final OWLOntologyID newOWLOntologyID = new OWLOntologyID(Optional.of(newOntologyIRI), Optional.of(newVersionIRI));
			manager.applyChange(new SetOntologyID(ontologyToSave, newOWLOntologyID));
			
			// create change axiom with original id
			reset = new SetOntologyID(ontologyToSave, owlOntologyID);
		}

		if (writeOWL) {
			OutputStream os = getOutputSteam(fileNameBase +".owl");
			write(manager, ontologyToSave, oortConfig.getDefaultFormat(), os);
		}

		
		if (writeOWX) {
			OutputStream osxml = getOutputSteam(fileNameBase +".owx");
			write(manager, ontologyToSave, oortConfig.getOwlXMLFormat(), osxml);
		}
		
		if (writeOFN) {
			OutputStream os = getOutputSteam(fileNameBase +".ofn");
			write(manager, ontologyToSave, oortConfig.getOwlOfnFormat(), os);
		}

		if (reset != null) {
			// reset versionIRI
			// the reset is required, because each owl file 
			// has its corresponding file name in the version IRI.
			manager.applyChange(reset);
		}

		if (gciOntology != null && (writeOWL || writeOWX || writeOFN)) {
			OWLOntologyManager gciManager = gciOntology.getOWLOntologyManager();

			// create specific import for the generated owl ontology
			OWLImportsDeclaration importDeclaration = new OWLImportsDeclarationImpl(IRI.create(fileNameBase +".owl"));
			AddImport addImport = new AddImport(gciOntology, importDeclaration);
			RemoveImport removeImport = new RemoveImport(gciOntology, importDeclaration);

			gciManager.applyChange(addImport);
			try {
				if (writeOWL) {
					OutputStream gciOS = getOutputSteam(fileNameBase +"-aux.owl");
					write(gciManager, gciOntology, oortConfig.getDefaultFormat(), gciOS);
				}

				if (writeOWX) {
					OutputStream gciOSxml = getOutputSteam(fileNameBase +"-aux.owx");
					write(gciManager, gciOntology, oortConfig.getOwlXMLFormat(), gciOSxml);
				}

				if (writeOFN) {
					OutputStream gciOS = getOutputSteam(fileNameBase +"-aux.ofn");
					write(gciManager, gciOntology, oortConfig.getOwlOfnFormat(), gciOS);
				}
			}
			finally {
				gciManager.applyChange(removeImport);
			}
		}

		if (!oortConfig.isSkipFormat("obo")) {

			Owl2Obo owl2obo = new Owl2Obo();
			OBODoc doc = owl2obo.convert(ontologyToSave);

			OBOFormatWriter writer = new OBOFormatWriter();

			BufferedWriter bwriter = getWriter(fileNameBase +".obo");

			writer.write(doc, bwriter);

			bwriter.close();
		}

	      if (!oortConfig.isSkipFormat("json")) {
	          String ofn = fileNameBase +".json";
              logInfo("exporting json to "+ofn);

              
	          ParserWrapper pw = new ParserWrapper();
	          pw.saveOWL(ontologyToSave, new OWLOboGraphsFormat(), getOutputSteam(ofn));
	      }
	      else {
	          logInfo("skipping json");
	      }

		if (!oortConfig.isSkipFormat("metadata")) {
			if (oortConfig.isWriteMetadata()) {
				saveMetadata(fileNameBase, mooncat.getGraph());
			}
		}
	}
	
	private void write(OWLOntologyManager manager, OWLOntology ont, OWLDocumentFormat format, OutputStream out) throws OWLOntologyStorageException {
		try {
			manager.saveOntology(ont, format, out);
		} finally {
			try {
				out.close();
			} catch (IOException e) {
				logWarn("Could not close stream.", e);
			} 
		}
		
	}

	private void saveReasonerReport(String ontologyId,
			List<String> reasonerReportLines) {
		
		Collections.sort(reasonerReportLines);
		StringBuilder sb = new StringBuilder();
		for (String s : reasonerReportLines) {
			sb.append(s);
			sb.append('\n');
		}
		report(ontologyId + "-reasoner-report.txt", sb);
	}

	private void saveMetadata(String ontologyId,
			OWLGraphWrapper graph) {
		String fn = ontologyId + "-metadata.txt";
		OutputStream fos;
		try {
			fos = getOutputSteam(fn);
			PrintWriter pw = new PrintWriter(fos);
			OntologyMetadata omd = new OntologyMetadata(pw);
			omd.generate(graph);
			pw.close();
			fos.close();
		} catch (IOException e) {
			logWarn("Could not print reasoner report for ontolog: "+ontologyId, e);
		}
	}


	private boolean isBridgingOntology(OWLOntology ont) {
		for (OWLClass c : ont.getClassesInSignature(Imports.INCLUDED)) {

			if (ont.getDeclarationAxioms(c).size() > 0) {
				if (mooncat.getOntology().getDeclarationAxioms(c).size() >0) {
					// class already declared in main ontology - a 2ary ontology MUST
					// declare at least one of its own classes if it is a bone-fide non-bridging ontology
				}
				else if (mooncat.isDangling(ont, c)) {
					// a dangling class has no OWL annotations.
					// E.g. bp_xp_cl contains CL classes as dangling
				}
				else {
					logInfo(c+" has declaration axioms, is not in main, and is not dangling, therefore "+ont+" is NOT a bridging ontology");
					return false;
				}
			}
		}
		logInfo(ont+" is a bridging ontology");
		return true;
	}

	private static void help() {
		System.out.println("This utility builds an ontology release. This tool is supposed to be run " +
		"from the location where a particular ontology releases are to be maintained.");
		System.out.println();
		System.out.println("bin/ontology-release-runner [OPTIONAL OPTIONS] ONTOLOGIES-FILES");
		System.out.println("Multiple obo or owl files are separated by a space character in the place of the ONTOLOGIES-FILES arguments.");
		System.out.println();
		System.out.println("OPTIONS:");
	}
}
