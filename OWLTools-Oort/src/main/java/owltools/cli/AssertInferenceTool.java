package owltools.cli;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.obolibrary.obo2owl.Owl2Obo;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.writer.OBOFormatWriter;
import org.obolibrary.oboformat.writer.OBOFormatWriter.NameProvider;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLFunctionalSyntaxOntologyFormat;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.RemoveImport;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.util.OWLAxiomVisitorAdapter;

import owltools.InferenceBuilder;
import owltools.InferenceBuilder.ConsistencyReport;
import owltools.InferenceBuilder.OWLClassFilter;
import owltools.InferenceBuilder.PotentialRedundant;
import owltools.graph.AxiomAnnotationTools;
import owltools.graph.OWLGraphWrapper;
import owltools.io.CatalogXmlIRIMapper;
import owltools.io.OWLPrettyPrinter;
import owltools.io.ParserWrapper;
import owltools.io.ParserWrapper.OboAndOwlNameProvider;
import uk.ac.manchester.cs.owlapi.modularity.ModuleType;
import uk.ac.manchester.cs.owlapi.modularity.SyntacticLocalityModuleExtractor;

/**
 * Simple command-line tool to assert inferences and (optional) remove redundant relations
 */
public class AssertInferenceTool {
	
	private static final Logger logger = Logger.getLogger(AssertInferenceTool.class);
	
	public static void main(String[] args) throws Exception {
		Opts opts = new Opts(args);
		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper graph = null;
		boolean removeRedundant = true;
		boolean checkConsistency = true; // TODO provide a command-line option to override this.
		boolean checkForPotentialRedundant = true;
		boolean dryRun = false;
		boolean useIsInferred = false;
		boolean ignoreNonInferredForRemove = false;
		boolean verifyExistingInferences = false;
		boolean runInferences = true;
		boolean removeUnsupportedInferences = false;
		boolean removeUnsupportedRegulationInferences = false; 
		boolean alwaysAssertSuperClasses = false;
		List<String> inputs = new ArrayList<String>();
		String outputFileName = null;
		String outputFileFormat = null;
		String reportFile = null;
		String idFilterPrefix = null;
		
		boolean all = false;
		String idsInputFile = null;
		
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
			else if (opts.nextEq("--dryRun")) {
				dryRun = true;
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
			else if (opts.nextEq("--markIsInferred")) {
				useIsInferred = true;
			}
			else if (opts.nextEq("--useIsInferred")) {
				useIsInferred = true;
				ignoreNonInferredForRemove = true;
			}
			else if (opts.nextEq("--idFilterPrefix")) {
				idFilterPrefix = opts.nextOpt();
			}
			else if (opts.nextEq("--ignoreNonInferredForRemove")) {
				ignoreNonInferredForRemove = true;
			}
			else if (opts.nextEq("--reportFile")) {
				reportFile = opts.nextOpt();
			}
			else if (opts.nextEq("--all")) {
				// check all classes for un-marked inferred links
				all = true;
				Logger.getLogger("org.semanticweb.elk").setLevel(Level.ERROR);
			}
			else if (opts.nextEq("--all-ids-input-file")) {
				// check all classes (from the id file, one per line)
				// for un-marked inferred links
				idsInputFile = opts.nextOpt();
				all = true;
				Logger.getLogger("org.semanticweb.elk").setLevel(Level.ERROR);
			}
			else if (opts.nextEq("--ignorePotentialRedundant")) {
				checkForPotentialRedundant = false;
			}
			else if (opts.nextEq("--verifyExistingInferences")) {
				verifyExistingInferences = true;
			}
			else if (opts.nextEq("--verifyExistingInferencesOnly")) {
				verifyExistingInferences = true;
				runInferences = false;
			}
			else if (opts.nextEq("--removeUnsupportedInferences")) {
				removeUnsupportedInferences = true;
			}
			else if (opts.nextEq("--removeUnsupportedRegulationInferences")) {
				removeUnsupportedRegulationInferences = true;
			}
			else if (opts.nextEq("--catalog-xml")) {
				String catalog = opts.nextOpt();
				pw.addIRIMapper(new CatalogXmlIRIMapper(catalog));
			}
			else if (opts.nextEq("--always-assert-super-classes")) {
				alwaysAssertSuperClasses = true;
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
		graph.addImportsFromSupportOntologies();
		
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
		
		BufferedWriter reportWriter = null;
		if (reportFile != null) {
			reportWriter = new BufferedWriter(new FileWriter(reportFile));
		}
		try {
			// create ontology with imports and create set of changes to removed the additional imports
			final List<OWLOntologyChange> removeImportChanges = handleSupportOntologies(graph);
			try {
				if (all == true) {
					assertAllInferences(graph, idsInputFile);
				}else {
					if (runInferences) {
						OWLClassFilter filter = null;
						if (idFilterPrefix != null) {
							final String prefix = idFilterPrefix;
							filter = new OWLClassFilter() {
								
								@Override
								public boolean useOWLClass(OWLClass cls, OWLOntology ont) {
									String id = Owl2Obo.getIdentifierFromObject(cls, ont, null);
									boolean use = id != null && id.startsWith(prefix);
									return use;
								}
							};
						}
						// assert inferences
						assertInferences(graph, removeRedundant, checkConsistency, useIsInferred,
								ignoreNonInferredForRemove, checkConsistency, checkForPotentialRedundant,
								alwaysAssertSuperClasses, filter, reportWriter);
					}
					
					if (verifyExistingInferences) {
						// check existing inference
						verifyExistingInferences(graph, reportWriter, removeUnsupportedInferences, removeUnsupportedRegulationInferences);
					}
				}
			}finally {
				// remove additional import axioms
				cleanupSupportOntologies(graph, removeImportChanges);
			}
		}
		finally {
			if (reportWriter != null) {
				reportWriter.close();
			}
		}
		
		if (dryRun == false) {
			// write ontology
			writeOntology(graph.getSourceOntology(), graph, outputFileName, outputFileFormat, useTemp);
		}
		
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

	static void writeOntology(OWLOntology ontology, OWLGraphWrapper graph, String outputFileName, 
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
			writeOntologyFile(ontology, graph, outputFileFormat, outputFile);
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

	static void writeOntologyFile(OWLOntology ontology, OWLGraphWrapper graph, String outputFileFormat, File outputFile) throws Exception {
		if ("obo".equals(outputFileFormat)) {
			BufferedWriter bufferedWriter = null;
			try {
				Owl2Obo owl2Obo = new Owl2Obo();
				OBODoc oboDoc = owl2Obo.convert(ontology);
				OBOFormatWriter oboWriter = new OBOFormatWriter();
				bufferedWriter = new BufferedWriter(new FileWriter(outputFile));
				NameProvider nameprovider = new OboAndOwlNameProvider(oboDoc, graph);
				oboWriter.write(oboDoc, bufferedWriter, nameprovider );
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
	
	private static boolean isRegulation(OWLClass cls, OWLGraphWrapper g) {
		String label = g.getLabel(cls);
		if (label != null) {
			return label.contains("regulation");
		}
		return false;
	}
	
	public static void verifyExistingInferences(OWLGraphWrapper graph, BufferedWriter reportWriter, boolean removeUnsupported, boolean removeRegulation) throws InconsistentOntologyException, IOException {
		OWLOntology ontology = graph.getSourceOntology();
		OWLOntologyManager manager = ontology.getOWLOntologyManager();
		
		logger.info("Start verification of existing inferences");
		
		Set<OWLSubClassOfAxiom> allSubClassAxioms = ontology.getAxioms(AxiomType.SUBCLASS_OF);
		
		Set<OWLSubClassOfAxiom> filteredAllSubClassAxioms = new HashSet<OWLSubClassOfAxiom>();
		Set<OWLSubClassOfAxiom> allRegulationSubClassAxioms = new HashSet<OWLSubClassOfAxiom>();
		for (OWLSubClassOfAxiom owlSubClassOfAxiom : allSubClassAxioms) {
			if (AxiomAnnotationTools.isMarkedAsInferredAxiom(owlSubClassOfAxiom)) {
				OWLClassExpression superClassCE = owlSubClassOfAxiom.getSuperClass();
				OWLClassExpression subClassCE = owlSubClassOfAxiom.getSubClass();
				if (!superClassCE.isAnonymous() && !subClassCE.isAnonymous()) {
					filteredAllSubClassAxioms.add(owlSubClassOfAxiom);
					if (removeRegulation) {
						if (isRegulation(subClassCE.asOWLClass(), graph) 
							&& isRegulation(superClassCE.asOWLClass(), graph)) {
							allRegulationSubClassAxioms.add(owlSubClassOfAxiom);
						}
					}
				}
			}
		}
		logger.info("Total SubClassOf axioms: "+allSubClassAxioms.size());
		if (filteredAllSubClassAxioms.isEmpty()) {
			logger.info("NO Inferred SubClassOf axioms. Verification Stopped.");
			return;
		}
		logger.info("Inferred SubClassOf axioms: "+filteredAllSubClassAxioms.size());
		final Set<OWLSubClassOfAxiom> existsNotEntailed = new HashSet<OWLSubClassOfAxiom>();
		final Set<OWLSubClassOfAxiom> regulationExistsNotEntailed = new HashSet<OWLSubClassOfAxiom>();
		manager.removeAxioms(ontology, filteredAllSubClassAxioms);
		try {
			OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();
			OWLReasoner reasoner = null;
			try {
				reasoner = reasonerFactory.createReasoner(ontology);
				
				List<OWLSubClassOfAxiom> sortedAxioms = new ArrayList<OWLSubClassOfAxiom>(filteredAllSubClassAxioms);
				Collections.sort(sortedAxioms);
				
				for (OWLSubClassOfAxiom owlSubClassOfAxiom : sortedAxioms) {
					OWLClass subClass = owlSubClassOfAxiom.getSubClass().asOWLClass();
					OWLClass orginalSuperClass = owlSubClassOfAxiom.getSuperClass().asOWLClass();
					
					Set<OWLClass> superClasses = reasoner.getSuperClasses(subClass, false).getFlattened();
					if (superClasses.contains(orginalSuperClass) == false) {
						existsNotEntailed.add(owlSubClassOfAxiom);
						boolean isRemoveRegulation = false;
						if (removeRegulation && allRegulationSubClassAxioms.contains(owlSubClassOfAxiom)) {
							isRemoveRegulation = true;
							regulationExistsNotEntailed.add(owlSubClassOfAxiom);
						}
						if (reportWriter != null) {
							OWLPrettyPrinter owlpp = new OWLPrettyPrinter(graph);
							StringBuilder sb = new StringBuilder();
							if (removeUnsupported || isRemoveRegulation) {
								sb.append("REMOVED ");
							}
							sb.append("EXISTS, TAGGED-INFERRED, NOT-ENTAILED\t");
							sb.append(owlpp.render(subClass));
							sb.append(" ");
							sb.append(owlpp.render(orginalSuperClass));
							sb.append("\t ! Direct SuperClasses for ");
							sb.append(graph.getIdentifier(subClass));
							sb.append(":");
							Set<OWLClass> directSuperClasses = reasoner.getSuperClasses(subClass, true).getFlattened();
							for (OWLClass directSuperClass : directSuperClasses) {
								sb.append(' ');
								if (directSuperClass.isOWLThing()) {
									if (directSuperClasses.size() == 1) {
										// only print OWLThing, if it is the only super class
										sb.append("owl:Thing");
									}
								}
								else {
									sb.append(owlpp.render(directSuperClass));
								}
							}

							reportWriter.append(sb).append('\n');
						}
					}
				}
				if (removeRegulation && !regulationExistsNotEntailed.isEmpty()) {
					logger.info("Found "+regulationExistsNotEntailed.size()+" unsupported regulation inferences");
				}
				if (!existsNotEntailed.isEmpty()) {
					logger.info("Found "+existsNotEntailed.size()+" unsupported inferences");
				}
				else {
					logger.info("NO unsupported inferences found.");
				}
			} finally {
				if (reasoner != null) {
					reasoner.dispose();
				}
				reasonerFactory = null;
			}
		}
		finally {
			if (removeRegulation) {
				logger.info("Removing "+regulationExistsNotEntailed.size()+" unsupported regulation inferences.");
				// remove the unsupported axioms
				filteredAllSubClassAxioms.removeAll(regulationExistsNotEntailed);
				// add the supported axioms back into the ontology.
				manager.addAxioms(ontology, filteredAllSubClassAxioms);
			}
			else if (removeUnsupported && !existsNotEntailed.isEmpty()) {
				logger.info("Removing "+existsNotEntailed.size()+" unsupported inferences.");
				// remove the unsupported axioms
				filteredAllSubClassAxioms.removeAll(existsNotEntailed);
				// add the supported axioms back into the ontology.
				manager.addAxioms(ontology, filteredAllSubClassAxioms);
			}
			else {
				// revert changes
				manager.addAxioms(ontology, filteredAllSubClassAxioms);
			}
		}
	}
	
	/**
	 * Assert inferred super class relationships and (optional) remove redundant ones.
	 * 
	 * @param graph
	 * @param removeRedundant set to false to not remove redundant super class relations
	 * @param checkConsistency
	 * @param useIsInferred 
	 * @param ignoreNonInferredForRemove
	 * @param alwaysAssertSuperClasses
	 * @param filter
	 * @param reportWriter (optional)
	 * @throws InconsistentOntologyException 
	 * @throws IOException 
	 * @throws OWLOntologyStorageException 
	 * @throws OWLOntologyCreationException 
	 */
	public static void assertInferences(OWLGraphWrapper graph, boolean removeRedundant, 
			boolean checkConsistency, boolean useIsInferred, boolean ignoreNonInferredForRemove,
			boolean alwaysAssertSuperClasses,
			OWLClassFilter filter, BufferedWriter reportWriter) 
			throws InconsistentOntologyException, IOException, OWLOntologyCreationException, OWLOntologyStorageException
	{
		assertInferences(graph,removeRedundant,checkConsistency,useIsInferred,ignoreNonInferredForRemove, true, true, alwaysAssertSuperClasses, filter, reportWriter);
	}
	
	public static void assertInferences(OWLGraphWrapper graph, boolean removeRedundant, 
			boolean checkConsistency, boolean useIsInferred, boolean ignoreNonInferredForRemove,
			boolean checkForNamedClassEquivalencies, boolean checkForPotentialRedundant,
			boolean alwaysAssertSuperClasses,
			OWLClassFilter filter, BufferedWriter reportWriter) 
			throws InconsistentOntologyException, IOException, OWLOntologyCreationException, OWLOntologyStorageException
	{
		OWLOntology ontology = graph.getSourceOntology();
		OWLOntologyManager manager = ontology.getOWLOntologyManager();
		OWLDataFactory factory = manager.getOWLDataFactory();
		
		DefaultAssertInferenceReport report = new DefaultAssertInferenceReport(graph);
		
		Set<OWLAxiom> newAxioms;
		
		// Inference builder
		InferenceBuilder builder = new InferenceBuilder(graph, InferenceBuilder.REASONER_ELK);
		builder.addFilter(filter);
		try {
			logger.info("Start building inferences");
			// assert inferences
			List<OWLAxiom> inferences = builder.buildInferences(alwaysAssertSuperClasses);
			
			logger.info("Finished building inferences");
			
			// add inferences
			logger.info("Start adding inferred axioms, count: " + inferences.size());
			newAxioms = new HashSet<OWLAxiom>(inferences);
			if (useIsInferred) {
				newAxioms = AxiomAnnotationTools.markAsInferredAxiom(newAxioms, factory);
			}
			manager.addAxioms(ontology, newAxioms);
			logger.info("Finished adding inferred axioms");
			
			final OWLPrettyPrinter owlpp = new OWLPrettyPrinter(graph);
			if (reportWriter != null) {
				report.putAxioms(newAxioms, "ADD");
				reportWriter.append("Added axioms (count "+newAxioms.size()+")\n");
			}
			
			
			// optional
			// remove redundant
			if (removeRedundant) {
				Collection<OWLAxiom> redundantAxioms = builder.getRedundantAxioms();
				if (redundantAxioms != null && !redundantAxioms.isEmpty()) {
					logger.info("Start removing redundant axioms, count: "+redundantAxioms.size());
					Set<OWLAxiom> redundantAxiomSet = new HashSet<OWLAxiom>(redundantAxioms);
					if (useIsInferred) {
						Iterator<OWLAxiom> iterator = redundantAxiomSet.iterator();
						while (iterator.hasNext()) {
							OWLAxiom axiom = iterator.next();
							boolean wasInferred = AxiomAnnotationTools.isMarkedAsInferredAxiom(axiom);
							if (wasInferred == false && ignoreNonInferredForRemove == true) {
								logger.info("Ignoring redundant axiom, as axiom wasn't marked as inferred: "+owlpp.render(axiom));
								iterator.remove();
							}
						}
					}
					manager.removeAxioms(ontology, redundantAxiomSet);
					logger.info("Finished removing redundant axioms");
				}
				
				if (reportWriter != null) {
					reportWriter.append("Removed axioms (count "+redundantAxioms.size()+")\n");
					report.putAxioms(redundantAxioms, "REMOVE");
				}
			}
			
			List<PotentialRedundant> potentialRedundants = null;
			
			if (checkForPotentialRedundant) {
				logger.info("Running additional checks");
				potentialRedundants = builder.checkPotentialRedundantSubClassAxioms(newAxioms);
				if (potentialRedundants != null) {
					// group by relationship and sort by class A
					Collections.sort(potentialRedundants, PotentialRedundant.PRINT_COMPARATOR);
					
					if (reportWriter != null) {
						report.setRedundants(potentialRedundants);
					}
				}
				logger.info("Finished running additional checks");
			}
			
			if (reportWriter != null) {
				report.printReport(reportWriter);
			}
			
			// checks
			if (checkConsistency) {
				logger.info("Start checking consistency");
				// logic checks
				ConsistencyReport consistencyReport = builder.performConsistencyChecks();
				final int incCount = consistencyReport.errors.size();
				if (incCount > 0) {
					if (consistencyReport.unsatisfiable != null && !consistencyReport.unsatisfiable.isEmpty()) {
						createUnsatisfiableModule(consistencyReport.unsatisfiable, ontology);
					}
					for (String inc  : consistencyReport.errors) {
						logger.error("PROBLEM: " + inc);
					}
					throw new InconsistentOntologyException("Logic inconsistencies found, count: "+incCount);
				}
				else {
					cleanupUnsatisfiableModule();
				}

				// equivalent named class pairs
				final List<OWLEquivalentClassesAxiom> equivalentNamedClassPairs = builder.getEquivalentNamedClassPairs();
				final int eqCount = equivalentNamedClassPairs.size();
				if (eqCount > 0) {
					logger.error("Found equivalencies between named classes");
					createEquivModule(equivalentNamedClassPairs, ontology);
					for (OWLEquivalentClassesAxiom eca : equivalentNamedClassPairs) {
						logger.error("EQUIVALENT_CLASS_PAIR: "+owlpp.render(eca));
					}
					if (checkForNamedClassEquivalencies) {
						throw new InconsistentOntologyException("Found equivalencies between named classes, count: " + eqCount);
					}
				}
				else {
					cleanupEquivModule();
				}
				logger.info("Finished checking consistency");
			}
			if (potentialRedundants != null) {
				logger.error("Found potential problems");
				createPotentialRedundantModule(potentialRedundants, ontology);
				for (PotentialRedundant redundant : potentialRedundants) {
					StringBuilder sb = new StringBuilder("POTENTIAL REDUNDANT AXIOMS: ");
					sb.append(owlpp.render(redundant.getClassA())).append(" ");
					sb.append(owlpp.render(redundant.getProperty())).append(" ");
					sb.append(owlpp.render(redundant.getClassB()));
					sb.append(" is also a simple SubClassOf.");
					logger.error(sb.toString());
				}
				throw new InconsistentOntologyException("Found potential redundant subClass axioms, count: " + potentialRedundants.size());
			}
			else {
				cleanupPotentialRedundantModule();
			}
		}
		finally {
			builder.dispose();
		}
	}

	private static void createEquivModule(List<OWLEquivalentClassesAxiom> equivalentNamedClassPairs, OWLOntology ont)
			throws OWLOntologyCreationException, IOException, OWLOntologyStorageException
	{
		Set<OWLEntity> signature = new HashSet<OWLEntity>();
		for(OWLEquivalentClassesAxiom axiom : equivalentNamedClassPairs) {
			signature.addAll(axiom.getSignature());
		}
		final String moduleName = "equivalent-classes";
		createModule(moduleName, signature, ont);
	}
	
	private static void createUnsatisfiableModule(Collection<OWLEntity> unsatisfiable, OWLOntology ont)
			throws OWLOntologyCreationException, IOException, OWLOntologyStorageException
	{
		Set<OWLEntity> signature = new HashSet<OWLEntity>(unsatisfiable);
		final String moduleName = "unsatisfiable";
		createModule(moduleName, signature, ont);
	}
	
	private static void createPotentialRedundantModule(Collection<PotentialRedundant> redundants, OWLOntology ont)
			throws OWLOntologyCreationException, IOException, OWLOntologyStorageException
	{
		Set<OWLEntity> signature = new HashSet<OWLEntity>();
		for (PotentialRedundant redundant : redundants) {
			signature.addAll(redundant.getAxiomOne().getSignature());
			signature.addAll(redundant.getAxiomTwo().getSignature());
		}
		
		final String moduleName = "potential-redundant";
		createModule(moduleName, signature, ont);
	}

	private static void createModule(String moduleName, Set<OWLEntity> signature, OWLOntology ont)
			throws OWLOntologyCreationException, IOException, OWLOntologyStorageException 
	{
		// create a new manager, re-use factory
		// avoid unnecessary change events
		final OWLOntologyManager m = OWLManager.createOWLOntologyManager(ont.getOWLOntologyManager().getOWLDataFactory());
		
		// extract module
		SyntacticLocalityModuleExtractor sme = new SyntacticLocalityModuleExtractor(m, ont, ModuleType.BOT);
		Set<OWLAxiom> moduleAxioms = sme.extract(signature);
		
		OWLOntology module = m.createOntology(IRI.generateDocumentIRI());
		m.addAxioms(module, moduleAxioms);
		
		// save module
		OutputStream moduleOutputStream = null;
		try {
			moduleOutputStream = new FileOutputStream(getModuleFile(moduleName));
			m.saveOntology(module, moduleOutputStream);
		}
		finally {
			IOUtils.closeQuietly(moduleOutputStream);
		}
	}

	private static File getModuleFile(String moduleName) throws IOException {
		return new File("assert-"+moduleName+"-module.owl").getCanonicalFile();
	}
	
	private static void cleanupEquivModule() throws IOException {
		cleanupFile(getModuleFile("equivalent-classes"));
	}
	
	private static void cleanupUnsatisfiableModule() throws IOException {
		cleanupFile(getModuleFile("unsatisfiable"));
	}
	
	private static void cleanupPotentialRedundantModule() throws IOException {
		cleanupFile(getModuleFile("potential-redundant"));
	}
	
	static void cleanupFile(File file) throws IOException {
		// try to delete the file, do nothing if the file does not exist
		// fail if the file exists, but could not be deleted.
		if (file.exists()) {
			boolean delete = file.delete();
			if (delete == false) {
				throw new IOException("Could not delete file: "+file.getAbsolutePath());
			}
		}
	}
	
	private static List<OWLOntologyChange> handleSupportOntologies(OWLGraphWrapper graph)
	{
		OWLOntology ontology = graph.getSourceOntology();
		OWLOntologyManager manager = ontology.getOWLOntologyManager();
		OWLDataFactory factory = manager.getOWLDataFactory();
		
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
		return removeImportChanges;
	}
	
	private static void  cleanupSupportOntologies(OWLGraphWrapper graph, List<OWLOntologyChange> remove)
	{
		OWLOntology ontology = graph.getSourceOntology();
		OWLOntologyManager manager = ontology.getOWLOntologyManager();
		manager.applyChanges(remove);
	}
	
	static class DefaultAssertInferenceReport {
		
		private final OWLGraphWrapper graph;
		OWLPrettyPrinter owlpp;
		private Map<OWLClassExpression, List<Line>> lines = new HashMap<OWLClassExpression, List<Line>>();
		Collection<PotentialRedundant> redundants = null;
		private List<String> others = new ArrayList<String>();

		DefaultAssertInferenceReport(OWLGraphWrapper graph) {
			this.graph = graph;
			owlpp = new OWLPrettyPrinter(graph);
		}

		static class Line {
			String type;
			String msg;
			
			Line(String type, String msg) {
				this.type = type;
				this.msg = msg;
			}
		}
		
		public void putAxiom(OWLAxiom ax, final String type) {
			ax.accept(new OWLAxiomVisitorAdapter(){

				@Override
				public void visit(OWLEquivalentClassesAxiom axiom) {
					StringBuilder sb = new StringBuilder();
					sb.append(type);
					sb.append('\t');
					sb.append("EQ:");
					for(OWLClassExpression expr : axiom.getClassExpressions()) {
						sb.append(' ');
						if (expr.isAnonymous()) {
							sb.append(owlpp.render(expr));
						}
						else {
							final OWLClass sCls = expr.asOWLClass();
							sb.append(graph.getIdentifier(sCls));
							final String label = graph.getLabel(sCls);
							if (label != null) {
								sb.append(" '");
								sb.append(label);
								sb.append('\'');
							}
						}
					}
					
					others.add(sb.toString());
				}

				@Override
				public void visit(OWLSubClassOfAxiom axiom) {
					OWLClassExpression subClass = axiom.getSubClass();
					List<Line> current = lines.get(subClass);
					if (current == null) {
						current = new ArrayList<Line>();
						lines.put(subClass, current);
					}
					StringBuilder sb = new StringBuilder();
					if (subClass.isAnonymous()) {
						sb.append(owlpp.render(subClass));
					}
					else {
						final OWLClass sCls = subClass.asOWLClass();
						sb.append(graph.getIdentifier(sCls));
						final String label = graph.getLabel(sCls);
						if (label != null) {
							sb.append(" '");
							sb.append(label);
							sb.append('\'');
						}
					}
					sb.append(' ');
					OWLClassExpression superClass = axiom.getSuperClass();
					if (superClass.isAnonymous()) {
						sb.append(owlpp.render(superClass));
					}
					else {
						final OWLClass sCls = superClass.asOWLClass();
						sb.append(graph.getIdentifier(sCls));
						final String label = graph.getLabel(sCls);
						if (label != null) {
							sb.append(" '");
							sb.append(label);
							sb.append('\'');
						}
					}
					current.add(new Line(type, sb.toString()));
				}
				
			});
		}
		
		public void setRedundants(Collection<PotentialRedundant> redundants) {
			this.redundants = redundants;
		}
		
		public void putAxioms(Collection<OWLAxiom> axioms, String type) {
			for (OWLAxiom owlAxiom : axioms) {
				putAxiom(owlAxiom, type);
			}
		}
		
		public void printReport(BufferedWriter writer) throws IOException {
			List<OWLClassExpression> keys = new ArrayList<OWLClassExpression>(lines.keySet());
			Collections.sort(keys);
			for (OWLClassExpression owlClass : keys) {
				List<Line> current = lines.get(owlClass);
				for (Line line : current) {
					writer.append(line.type);
					writer.append('\t');
					writer.append(line.msg);
					writer.append('\n');
				}
			}
			if (redundants != null) {
				for (PotentialRedundant redundant : redundants) {
					writer.append("POTENTIAL REDUNDANT AXIOMS\t");
					writer.append(owlpp.render(redundant.getClassA())).append(" ");
					writer.append(owlpp.render(redundant.getProperty())).append(" ");
					writer.append(owlpp.render(redundant.getClassB()));
					writer.append(" is also a simple SubClassOf.\n");
				}
			}
		}
	}
	
	private static class InconsistentOntologyException extends Exception {

		// generated
		private static final long serialVersionUID = -1075657686336672286L;
		
		InconsistentOntologyException(String message) {
			super(message);
		}
	}

	public static void assertAllInferences(OWLGraphWrapper graph, String idsInputFile) {
		final OWLOntology ontology = graph.getSourceOntology();
		final OWLOntologyManager manager = ontology.getOWLOntologyManager();
		final OWLDataFactory factory = manager.getOWLDataFactory();
		
		Set<String> ids = loadIdsInputFile(idsInputFile);
		
		final OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();
		final OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);
		try {
			logger.info("Start check all");
			// check all classes from the main ontology
			AllInferenceReport report = new AllInferenceReport();
			Set<OWLClass> classes = ontology.getClassesInSignature(false);
			int count = 0;
			int total = ids != null ? ids.size() : classes.size();
			int step = 100;
			for (final OWLClass owlClass : classes) {
				if (ids != null) {
					String id = graph.getIdentifier(owlClass);
					if (ids.contains(id) == false) {
						continue;
					}
				}
				count += 1;
				// get axioms for the current class
				Set<OWLClassAxiom> axioms = ontology.getAxioms(owlClass);
				
				handleAxioms(owlClass, axioms, ontology, manager, factory, reasoner, report);
//				handleAxioms2(owlClass, axioms, ontology, manager, factory, reasoner, report);
				if (count % step == 0) {
					logger.info("Current count "+count+" of "+total);
				}
			}
			PrintWriter writer = new PrintWriter(System.out);
			report.printReport(writer);
			writer.close();
		}
		finally {
			reasoner.dispose();
		}
	}
	
	private static Set<String> loadIdsInputFile(String input) {
		if (input != null) {
			File inputFile = new File(input);
			if (inputFile.exists() && inputFile.isFile() && inputFile.canRead()) {
				Set<String> ids = new HashSet<String>();
				LineIterator iterator = null;
				try {
					iterator = FileUtils.lineIterator(inputFile);
					while (iterator.hasNext()) {
						String line = iterator.next();
						line = StringUtils.trimToNull(line);
						if (line != null) {
							ids.add(line);
						}
					}
					logger.info("Finished loading input file: "+input+"\n id count: "+ids.size());
					return ids;
				} catch (IOException e) {
					logger.warn("Could not load ids file: "+input, e);
				}
				finally {
					LineIterator.closeQuietly(iterator);
				}
			}
			else {
				logger.warn("Could not load ids file: "+input);
			}
		}
		return null;
	}

	@SuppressWarnings("unused")
	private static void handleAxioms2(final OWLClass owlClass,
			Set<OWLClassAxiom> axioms, final OWLOntology ontology,
			final OWLOntologyManager manager, final OWLDataFactory factory,
			final OWLReasoner reasoner, AllInferenceReport report)
	{
		for (OWLClassAxiom axiom : axioms) {
			// only check the axiom if it isn't marked
			if (AxiomAnnotationTools.isMarkedAsInferredAxiom(axiom) == false) {
				axiom.accept(new OWLAxiomVisitorAdapter(){

					@Override
					public void visit(OWLSubClassOfAxiom axiom) {
						// check sub class axiom
						if (axiom.getSubClass().equals(owlClass)) {
							// only update the axiom if the sub class is the current class
							OWLClassExpression ce = axiom.getSuperClass();
							if (ce.isAnonymous() == false) {
								// ignore anonymous super classes
								final OWLClass superClass = ce.asOWLClass();
								
								// remove axiom and flush reasoner
								manager.removeAxiom(ontology, axiom);
								reasoner.flush();
								
								// ask reasoner
								NodeSet<OWLClass> superClasses = reasoner.getSuperClasses(owlClass, true);
								if (superClasses.containsEntity(superClass)) {
									// is inferred
									// add markup and add to ontology
									OWLAxiom markedAxiom = AxiomAnnotationTools.markAsInferredAxiom(axiom, factory);
									manager.addAxiom(ontology, markedAxiom);
								}
								else {
									// not inferred 
									// add original axiom back into ontology
									manager.addAxiom(ontology, axiom);
								}
							}
						}
					}
				});
			}
		}
	}

	private static void handleAxioms(final OWLClass owlClass,
			Set<OWLClassAxiom> axioms, final OWLOntology ontology,
			final OWLOntologyManager manager, final OWLDataFactory factory,
			final OWLReasoner reasoner, AllInferenceReport report)
	{
		report.classes += 1;
		// only look at sub class of axioms with a named superClass and subClass is currentClass
		Set<OWLSubClassOfAxiom> subClassAxioms = new HashSet<OWLSubClassOfAxiom>();
		for (OWLClassAxiom axiom : axioms) {
			if (axiom instanceof OWLSubClassOfAxiom) {
				OWLSubClassOfAxiom subClassAxiom = (OWLSubClassOfAxiom) axiom;
				if (owlClass.equals(subClassAxiom.getSubClass())) {
					OWLClassExpression ce = subClassAxiom.getSuperClass();
					if (ce.isAnonymous() == false) {
						subClassAxioms.add(subClassAxiom);
					}
				}
			}
		}
		// check that the axioms are not already inferred
		boolean requireUpdate = false;
		if (subClassAxioms.isEmpty() == false) {
			for (OWLSubClassOfAxiom owlSubClassOfAxiom : subClassAxioms) {
				if (AxiomAnnotationTools.isMarkedAsInferredAxiom(owlSubClassOfAxiom) == false) {
					requireUpdate = true;
				}
			}
		}
		if (requireUpdate) {
			report.handledClasses += 1;
			report.checkedAxioms += subClassAxioms.size();
			manager.removeAxioms(ontology, subClassAxioms);
			reasoner.flush();
			NodeSet<OWLClass> superClasses = reasoner.getSuperClasses(owlClass, false);
			Set<OWLAxiom> addSet = new HashSet<OWLAxiom>();
			for (OWLSubClassOfAxiom subClassAxiom : subClassAxioms) {
				if (superClasses.containsEntity(subClassAxiom.getSuperClass().asOWLClass())) {
					// is inferred
					// add markup and add to ontology
					OWLAxiom markedAxiom = AxiomAnnotationTools.markAsInferredAxiom(subClassAxiom, factory);
					report.annotatedAxioms += 1;
					addSet.add(markedAxiom);
				}
				else {
					// not inferred 
					// add original axiom back into ontology
					addSet.add(subClassAxiom);
				}
			}
			manager.addAxioms(ontology, addSet);
		}
	}
	
	static class AllInferenceReport {
		
		int classes = 0;
		int handledClasses = 0;
		
		int checkedAxioms = 0;
		int annotatedAxioms = 0;
		
		void printReport(PrintWriter writer) {
			writer.println("Classes:           "+classes);
			writer.println("Handled Classes:   "+handledClasses);
			writer.println("Axioms:            "+checkedAxioms);
			writer.println("Annotated Axioms:  "+annotatedAxioms);
			writer.flush();
		}
	}
}
