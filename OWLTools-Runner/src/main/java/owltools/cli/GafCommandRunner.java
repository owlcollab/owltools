package owltools.cli;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.obolibrary.obo2owl.Obo2OWLConstants;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.cli.tools.CLIMethod;
import owltools.gaf.Bioentity;
import owltools.gaf.BioentityDocument;
import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.eco.EcoMapperFactory;
import owltools.gaf.eco.SimpleEcoMapper;
import owltools.gaf.eco.TraversingEcoMapper;
import owltools.gaf.inference.AnnotationPredictor;
import owltools.gaf.inference.CompositionalClassPredictor;
import owltools.gaf.inference.FoldBasedPredictor;
import owltools.gaf.inference.Prediction;
import owltools.gaf.io.GafWriter;
import owltools.gaf.io.GpadWriter;
import owltools.gaf.io.GpiWriter;
import owltools.gaf.io.OpenAnnotationRDFWriter;
import owltools.gaf.io.PseudoRdfXmlWriter;
import owltools.gaf.io.PseudoRdfXmlWriter.ProgressReporter;
import owltools.gaf.io.XgmmlWriter;
import owltools.gaf.metadata.AnnotationDocumentMetadata;
import owltools.gaf.owl.AnnotationExtensionFolder;
import owltools.gaf.owl.AnnotationExtensionUnfolder;
import owltools.gaf.owl.GAFOWLBridge;
import owltools.gaf.owl.GAFOWLBridge.BioentityMapping;
import owltools.gaf.owl.mapping.BasicABox;
import owltools.gaf.parser.CommentListener;
import owltools.gaf.parser.DefaultAspectProvider;
import owltools.gaf.parser.GAFParser;
import owltools.gaf.parser.GafObjectsBuilder;
import owltools.gaf.parser.GpadGpiObjectsBuilder;
import owltools.gaf.parser.GpadGpiObjectsBuilder.AspectProvider;
import owltools.gaf.parser.IssueListener;
import owltools.gaf.parser.LineFilter;
import owltools.gaf.parser.ParserListener;
import owltools.gaf.rules.AnnotationRuleViolation.ViolationType;
import owltools.gaf.rules.AnnotationRulesEngine;
import owltools.gaf.rules.AnnotationRulesEngine.AnnotationRulesEngineResult;
import owltools.gaf.rules.AnnotationRulesFactory;
import owltools.gaf.rules.AnnotationRulesReportWriter;
import owltools.gaf.rules.go.GoAnnotationExperimentalPredictionRule;
import owltools.gaf.rules.go.GoAnnotationRulesFactoryImpl;
import owltools.graph.OWLGraphEdge;
import owltools.io.OWLPrettyPrinter;
import owltools.mooncat.Mooncat;
import uk.ac.manchester.cs.owlapi.modularity.ModuleType;
import uk.ac.manchester.cs.owlapi.modularity.SyntacticLocalityModuleExtractor;

import com.google.common.collect.Ordering;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * GAF tools for command-line, includes validation of GAF files.
 * 
 * TODO implement a filtering mechanism for GAF files
 */
public class GafCommandRunner extends CommandRunner {

	private static final Logger LOG = Logger.getLogger(GafCommandRunner.class);

	public GafDocument gafdoc = null;
	public BioentityDocument bioentityDocument = null;
	private GafParserReport parserReport = null;

	private String gafReportSummaryFile = null;
	private String gafReportFile = null;
	private String gafPredictionFile = null;
	private String gafPredictionReportFile = null;
	private String gafTaxonModule = "go-taxon-rule-unsatisfiable-module.owl";
	private String experimentalGafPredictionFile = null;
	private String experimentalGafPredictionReportFile = null;

	public TraversingEcoMapper eco = null;

	/**
	 * Used for loading GAFs into memory
	 * 
	 * @param opts
	 * @throws Exception
	 */
	@CLIMethod("--gaf")
	public void gaf(Opts opts) throws Exception {
		opts.info("GAF-FILE [--createReport] [--no-iea]", "parses GAF and makes this the current GAF document");
		final String input = opts.nextOpt();
		boolean createReport = false;
		boolean noIEA = false;
		int reportLineCounts = -1;
		while (opts.hasOpts()) {
			if (opts.nextEq("--createReport"))
				createReport = true;
			else if (opts.nextEq("--no-iea")) {
				noIEA = true;
			}
			else if (opts.nextEq("--report-line-count")) {
				reportLineCounts = 100000;
			}
			else
				break;

		}
		if (opts.hasOpts() && opts.nextEq("--createReport")) {
			createReport = true;
		}

		GafObjectsBuilder builder = new GafObjectsBuilder();
		if (createReport) {
			parserReport = new GafParserReport();
			builder.getParser().addParserListener(new ParserListener() {

				@Override
				public boolean reportWarnings() {
					return true;
				}

				@Override
				public void parsing(String line, int lineNumber) {
					// intentionally empty
				}

				@Override
				public void parserWarning(String message, String line, int lineNumber) {
					parserReport.warnings.add(new GafParserMessages(message, line, lineNumber));
				}

				@Override
				public void parserError(String errorMessage, String line, int lineNumber) {
					parserReport.errors.add(new GafParserMessages(errorMessage, line, lineNumber));
				}
			});
		}
		if (noIEA) {
			LOG.info("Using no-IEA filter on GAF.");
			builder.addFilter(new LineFilter<GAFParser>() {

				@Override
				public boolean accept(String line, int pos, GAFParser parser) {
					String evidence = parser.getEvidence();
					evidence = StringUtils.trimToEmpty(evidence);
					if ("IEA".equals(evidence)) {
						return false;
					}
					return true;
				}
			});
		}
		if (reportLineCounts > 0) {
			final int finalReportLineCounts = reportLineCounts;
			builder.getParser().addParserListener(new ParserListener() {

				@Override
				public boolean reportWarnings() {
					return false;
				}

				@Override
				public void parsing(String line, int lineNumber) {
					if (lineNumber % finalReportLineCounts == 0) {
						LOG.info("Parsing line count: "+lineNumber);
					}
				}

				@Override
				public void parserWarning(String message, String line, int lineNumber) {
					// do nothing
				}

				@Override
				public void parserError(String errorMessage, String line, int lineNumber) {
					// do nothing
				}
			});
		}
		LOG.info("Start loading GAF from: "+input);
		gafdoc = builder.buildDocument(input);
		if (parserReport != null) {
			parserReport.lineCount = builder.getParser().getLineNumber();
		}
		if (gafdoc == null) {
			LOG.error("The GAF parsing finished with an empty result.");
			exit(-1);
			return;
		}
		else {
			LOG.info("Finished loading GAF.");
		}
	}

	@CLIMethod("--write-gaf")
	public void writeGaf(Opts opts) throws Exception {
		String ofn = opts.nextOpt();
		GafWriter gw = new GafWriter();
		try {
			gw.setStream(ofn);
			gw.write(gafdoc);
		}
		finally {
			IOUtils.closeQuietly(gw);
		}
	}

	@CLIMethod("--gaf-fold-extensions")
	public void foldGafExtensions(Opts opts) throws Exception {
		opts.info("", "takes a set of pre-loaded annotations and transforms this set such that any annotation with c16 extensions is replaced by a new term. See http://code.google.com/p/owltools/wiki/AnnotationExtensionFolding");
		AnnotationExtensionFolder aef = new AnnotationExtensionFolder(g);
		aef.fold(gafdoc);
		AssertInferenceTool.assertInferences(g, false, false, false, false, false, false, false, null, null);
	}

	@CLIMethod("--gaf-unfold-extensions")
	public void unfoldGafExtensions(Opts opts) throws Exception {
		opts.info("", "takes a set of pre-loaded annotations and transforms this set adding info to c16 extensions. See http://code.google.com/p/owltools/wiki/AnnotationExtensionFolding");
		boolean isReplaceGenus = true;
		boolean isThrowOnMultipleExpressions = true;
		while (opts.hasOpts()) {
			if (opts.nextEq("-n|--no-replace")) {
				isReplaceGenus = false;
			}
			else if (opts.nextEq("-a|--allow-multiple-expressions")) {
				isThrowOnMultipleExpressions = false;
			}
			else {
				break;
			}
		}
		AnnotationExtensionUnfolder aef = new AnnotationExtensionUnfolder(g);
		aef.isReplaceGenus = isReplaceGenus;
		aef.isThrowOnMultipleExpressions = isThrowOnMultipleExpressions;
		aef.unfold(gafdoc);
	}

	@CLIMethod("--gaf-fold-inferences")
	public void gafFoldInferences(Opts opts) throws Exception {
		opts.info("", "inferences. See: http://code.google.com/p/owltools/wiki/AnnotationExtensionFolding");
		FoldBasedPredictor fbp = new FoldBasedPredictor(gafdoc, g, false);
		if (fbp.isInitialized()) {
			OWLPrettyPrinter owlpp = new OWLPrettyPrinter(g);
			for (Prediction pred : fbp.getAllPredictions()) {
				System.out.println(pred.render(owlpp));
			}
		}
		else {
			LOG.error("Could not create fold based predictions, due to problems with the initialization");
			exit(2);
		}
		fbp.dispose();
	}

	@CLIMethod("--gaf-remove-redundant")
	public void gafRemoveRedundant(Opts opts) throws Exception {
		opts.info("", "Removes any annotation (G,T) if exists (G,T') and T' descendant-of T. ");
		// TODO
		Map<String,Set<String>> rmap = new HashMap<String,Set<String>>();
		List<GeneAnnotation> anns = gafdoc.getGeneAnnotations();
		for (GeneAnnotation ann : anns) {
			String eid = ann.getBioentity();
			String cls = ann.getCls();
			Set<OWLObject> cs = g.getAncestors(g.getOWLObjectByIdentifier(cls));
			if (!rmap.containsKey(eid))
				rmap.put(eid, new HashSet<String>());
			Set<String> ids = new HashSet<String>();
			for (OWLObject c : cs) {
				ids.add(g.getIdentifier(c));
			}
			rmap.get(eid).addAll(ids);
		}
		List<GeneAnnotation> filteredAnns = new ArrayList<GeneAnnotation>();
		for (GeneAnnotation ann : anns) {
			String eid = ann.getBioentity();
			String cls = ann.getCls();
			if (rmap.get(eid).contains(cls)) {
				LOG.info("SKIPPING_REDUNDANT: "+eid+" --> "+cls);
			}
			else {
				filteredAnns.add(ann);
			}
		}
		gafdoc.setGeneAnnotations(filteredAnns);

	}

	@CLIMethod("--gaf-compare-basic")
	public void gafCompareBasic(Opts opts) throws Exception {
		opts.info("TARGET-GAF", 
					"Compares annotations between any loaded annotations and the specified GAF\n"+
				"Ignores all metadata and only attempts to see for each G-T in source if G-T' can be found\n"+
				"in target, where T' is identical, anctesor or descendant of T");
		// TODO
		File gafFile = opts.nextFile();
		GafObjectsBuilder builder = new GafObjectsBuilder();
		GafDocument gafdoc2 = builder.buildDocument(gafFile);
		Map<String,Set<String>> rmap = new HashMap<String,Set<String>>();
		List<GeneAnnotation> anns = gafdoc.getGeneAnnotations();
		Collection<Bioentity> bioentities = gafdoc.getBioentities();

		for (Bioentity e : bioentities) {
			String  eid = e.getId();
			Bioentity e2 = gafdoc2.getBioentity(eid);
			Collection<GeneAnnotation> anns2 = null;
			if (e2 != null) {
				anns2 = gafdoc2.getGeneAnnotations(e2.getId());
			}
			for (GeneAnnotation ann : gafdoc.getGeneAnnotations(eid)) {
				String outcome = null;
				String outcomeInfo = "";
				if (e2 == null) {
					outcome = "ENTITY_NOT_FOUND";
					outcomeInfo = eid;
					break;
				}
				String clsId = ann.getCls();
				OWLClass cls = g.getOWLClassByIdentifier(clsId);
				if (cls == null) {
					LOG.warn(clsId+" not found");
					outcome = "CLASS_NOT_FOUND";
					outcomeInfo = clsId;
				}
				else if (g.isObsolete(cls)) {
					outcome = "CLASS_OBSOLETED";
					outcomeInfo = clsId;
				}
				else {
					for (GeneAnnotation ann2 : anns2) {
						String clsId2 = ann2.getCls();
						if (clsId.equals(clsId2)) {
							// match
							outcome = "EXACT_MATCH";
							outcomeInfo = clsId;
							break;
						}
						else {
							OWLClass cls2 = g.getOWLClassByIdentifier(clsId2);
							Set<OWLObject> ancs2 = g.getAncestors(cls2);
							if (ancs2.contains(cls)) {
								outcome = "MATCHES_MORE_SPECIFIC";
								outcomeInfo = clsId2;
							}
							else {
								Set<OWLObject> ancs = g.getAncestors(cls);
								if (ancs.contains(cls2)) {
									if (outcome == null) {
										outcome = "MATCHES_MORE_GENERAL";
										outcomeInfo = clsId2;
									}
								}
							}
							
							// todo - altIds, etc

						}
					}
				}
				if (outcome == null) {
					outcome = "NO_MATCH";
				}
				System.out.println(outcome+"\t"+outcomeInfo+"\t"+ann.toString());
			}
		}


	}

	/**
	 * Created to mimic the translation to OWL for GAF checks
	 * 
	 * @param opts
	 * @throws Exception
	 */
	@CLIMethod("--gaf2owl-simple")
	public void gaf2OwlSimple(Opts opts) throws Exception {
		opts.info("-o FILE", "translates previously loaded GAF document into OWL, requires a loaded ontology to lookup ids");
		String out = null;
		while (opts.hasOpts()) {
			if (opts.nextEq("-o")) {
				out = opts.nextOpt();
			}
			else
				break;

		}
		if (g == null) {
			LOG.error("An ontology is required.");
			exit(-1);
		}
		else if (gafdoc == null) {
			LOG.error("A GAF document is required.");
			exit(-1);
		}
		else if (out == null) {
			LOG.error("An output file is required.");
			exit(-1);
		}
		LOG.info("Creating OWL represenation of annotations.");
		GAFOWLBridge bridge = new GAFOWLBridge(g);
		bridge.setGenerateIndividuals(false);
		bridge.setBasicAboxMapping(false);
		bridge.setBioentityMapping(BioentityMapping.CLASS_EXPRESSION);
		bridge.setSkipNotAnnotations(true);
		OWLOntology translated = bridge.translate(gafdoc);
		File outputFile = new File(out);
		OWLOntologyManager manager = translated.getOWLOntologyManager();
		OWLDocumentFormat ontologyFormat= new FunctionalSyntaxDocumentFormat();
		manager.saveOntology(translated, ontologyFormat, IRI.create(outputFile));
	}

	@CLIMethod("--gaf2owl")
	public void gaf2Owl(Opts opts) throws OWLException {
		opts.info("[-n TARGET-IRI] [--named-class] [--none] [--class-exxpression] [--individual] [--basic] [-o FILE]", 
				"translates previously loaded GAF document into OWL");
		GAFOWLBridge bridge;
		String iri = null;
		String out = null;
		boolean isSkipIndividuals = false;
		boolean isBasic = false;
		BioentityMapping bioentityMapping = null;
		boolean makeMinimalModel = false;
		boolean isAddAsSupport = false;
		boolean isAddAsMain = false;
		while (opts.hasOpts()) {
			if (opts.nextEq("-n"))
				iri = opts.nextOpt();
			else if (opts.nextEq("-o")) {
				out = opts.nextOpt();
			}
			else if (opts.nextEq("-c") || opts.nextEq("--skip-individuals")) {
				isSkipIndividuals = true;
			}
			else if (opts.nextEq("--named-class")) {
				isSkipIndividuals = true;
				bioentityMapping = BioentityMapping.NAMED_CLASS;
			}
			else if (opts.nextEq("--none")) {
				bioentityMapping = BioentityMapping.NONE;
			}
			else if (opts.nextEq("--class-expression")) {
				isSkipIndividuals = true;
				bioentityMapping = BioentityMapping.CLASS_EXPRESSION;
			}
			else if (opts.nextEq("--individual")) {
				bioentityMapping = BioentityMapping.INDIVIDUAL;
			}
			else if (opts.nextEq("--basic")) {
				isBasic = true;
			}
			else if (opts.nextEq("-a|--add-as-support")) {
				isAddAsSupport = true;
			}
			else if (opts.nextEq("-m|--add-as-main")) {
				isAddAsMain = true;
			}
			else if (opts.nextEq("--make-minimal-model")) {
				makeMinimalModel = true;
			}
			else
				break;
		}
		if (isBasic) {
			bridge = new BasicABox(g);
		}	
		else {
			bridge = new GAFOWLBridge(g);
		}
		if (iri != null) {
			if (!iri.startsWith("http:")) {
				iri = Obo2OWLConstants.DEFAULT_IRI_PREFIX+iri;
				if (!iri.endsWith(".owl"))
					iri = iri + ".owl";
			}
			// todo - save tgtOnt
			OWLOntology tgtOnt = g.getManager().createOntology(IRI.create(iri));

			bridge.setTargetOntology(tgtOnt);
		}
		bridge.setGenerateIndividuals(!isSkipIndividuals);
		if (bioentityMapping != null) {
			bridge.setBioentityMapping(bioentityMapping);
			// bioentity mapping setting are only evaluated for non basic a box translation
			bridge.setBasicAboxMapping(false); 
		}
		LOG.info("Start converting GAF to OWL");
		bridge.translate(gafdoc);
		LOG.info("Finished converting GAF to OWL");

		OWLOntology ontology = bridge.getTargetOntology();
		if (makeMinimalModel) {
			OWLOntologyManager manager = ontology.getOWLOntologyManager();
			OWLDataFactory fac = manager.getOWLDataFactory();
			Set<OWLEntity> entities = new HashSet<OWLEntity>();
			if (bridge.isGenerateIndividuals()) {
				LOG.info("Generating minimal model based on individuals");

				Set<OWLNamedIndividual> individuals = ontology.getIndividualsInSignature(Imports.INCLUDED);
				if (individuals.isEmpty()) {
					LOG.info("No individuals found skipping minimization step.");
				}
				else {
					entities.addAll(individuals);
				}
			}
			else if (bridge.getBioentityMapping() == BioentityMapping.NAMED_CLASS) {
				LOG.info("Generating minimal model using named classes of annotations");
				// find all classes with a line number annotation
				OWLAnnotationProperty property = fac.getOWLAnnotationProperty(GAFOWLBridge.GAF_LINE_NUMBER_ANNOTATION_PROPERTY_IRI);
				Set<OWLClass> allClasses = ontology.getClassesInSignature(Imports.INCLUDED);
				for (OWLClass cls : allClasses) {
					Set<OWLAnnotationAssertionAxiom> axioms = ontology.getAnnotationAssertionAxioms(cls.getIRI());
					for (OWLAnnotationAssertionAxiom axiom : axioms) {
						if (property.equals(axiom.getProperty())) {
							entities.add(cls);
						}
					}
				}
				if (entities.isEmpty()) {
					LOG.info("No classes found, skipping minimization step.");
				}
			}
			else {
				LOG.info("Cann't extract entities for minimal model generation, skipping minimzation step.");
			}
			if (entities.isEmpty() == false) {
				LOG.info("Start module extraction");
				SyntacticLocalityModuleExtractor sme = new SyntacticLocalityModuleExtractor(manager, ontology, ModuleType.BOT);
				Set<OWLAxiom> moduleAxioms = sme.extract(entities);

				OWLOntology modOnt = manager.createOntology();
				manager.addAxioms(modOnt, moduleAxioms);
				ontology = modOnt;
				LOG.info("Finished module extraction");
			}
		}

		if (out != null) {
			pw.saveOWL(ontology,out);
		}
		if (isAddAsSupport) {
			g.addSupportOntology(ontology);
		}
		if (isAddAsMain) {
			g.setSourceOntology(ontology);
		}
	}

	@CLIMethod("--gaf-xp-predict")
	public void gafXpPredict(Opts opts) {
		OWLPrettyPrinter owlpp = getPrettyPrinter();
		if (gafdoc == null) {
			System.err.println("No gaf document (use '--gaf GAF-FILE') ");
			exit(1);
			return;
		}
		AnnotationPredictor ap = new CompositionalClassPredictor(gafdoc, g);
		List<Prediction> predictions = ap.getAllPredictions();
		System.out.println("Predictions:"+predictions.size());
		for (Prediction p : predictions) {
			System.out.println(p.render(owlpp));
		}
	}

	@CLIMethod("--gaf-term-IC-values")
	public void gafTermICValues(Opts opts) {
		opts.info("", "Calculate IC for every term based on an input GAF. Ensure relations are filtered accordingly first");
		// TODO - ensure has_part and other relations are excluded
		Map<OWLObject,Set<String>> aMap = new HashMap<OWLObject,Set<String>>();
		double corpusSize = gafdoc.getBioentities().size();
		for (GeneAnnotation a : gafdoc.getGeneAnnotations()) {
			OWLObject c = g.getOWLObjectByIdentifier(a.getCls());
			for (OWLObject x : g.getAncestorsReflexive(c)) {
				if (!aMap.containsKey(x))
					aMap.put(x, new HashSet<String>());
				aMap.get(x).add(a.getBioentity());
			}
		}
		for (OWLObject c : g.getAllOWLObjects()) {
			if (c instanceof OWLClass) {
				if (g.isObsolete(c))
					continue;
				if (!aMap.containsKey(c))
					continue;
				int n = aMap.get(c).size();
				double ic = - (Math.log( n / corpusSize) / Math.log(2));
				System.out.println(g.getIdentifier(c)+"\t"+g.getLabel(c)+"\t"+ ic);
			}
		}
	}

	@CLIMethod("--gaf-calculate-specificity")
	public void gaCalculateSpecificity(Opts opts) {
		opts.info("", "Calculate IC for every term based on an input GAF. Ensure relations are filtered accordingly first");
		// TODO - ensure has_part and other relations are excluded
		Map<OWLObject,Set<String>> aMap = new HashMap<OWLObject,Set<String>>();
		double corpusSize = gafdoc.getBioentities().size();
		int nA = 0;
		int sumA = 0;
		int nG = 0;
		int sumG = 0;
		for (GeneAnnotation a : gafdoc.getGeneAnnotations()) {
			if (a.hasQualifiers()) {
				continue;
			}
			nA++;
			OWLObject c = g.getOWLObjectByIdentifier(a.getCls());
			int maxDist = 0;
			for (OWLGraphEdge edge : g.getOutgoingEdgesClosure(c)) {
				// TODO - make more efficient
				int dist = edge.getDistance();
				if (dist > maxDist) {
					maxDist = dist;
				}
			}
			System.out.println(maxDist + "\t" + a.toString());
			sumA += maxDist;
		}
		// per-gene
		// TODO - don't repeat yourself
		for (Bioentity e : gafdoc.getBioentities()) {
			int maxDist = 0;
			nG++;
			for ( GeneAnnotation a : gafdoc.getGeneAnnotations(e.getId())) {
				if (a.hasQualifiers()) {
					continue;
				}
				OWLObject c = g.getOWLObjectByIdentifier(a.getCls());
				for (OWLGraphEdge edge : g.getOutgoingEdgesClosure(c)) {
					// TODO - make more efficient
					int dist = edge.getDistance();
					if (dist > maxDist) {
						maxDist = dist;
					}
				}
			}
			sumG += maxDist;
		}
		System.out.println("Avg dist per annotation: "+ sumA / (float) nA);
		System.out.println("Avg dist per gene: "+ sumG / (float) nG);
	}

	@CLIMethod("--gaf-term-counts")
	public void gafTermCounts(Opts opts) {
		opts.info("", "Calculate and print the annotation count for each class for the current ontology and annotations");
		// TODO - ensure has_part and other relations are excluded
		Map<OWLObject,Set<String>> aMap = new HashMap<OWLObject,Set<String>>();
		for (GeneAnnotation a : gafdoc.getGeneAnnotations()) {
			OWLObject c = g.getOWLObjectByIdentifier(a.getCls());
			for (OWLObject x : g.getAncestorsReflexive(c)) {
				if (!aMap.containsKey(x))
					aMap.put(x, new HashSet<String>());
				aMap.get(x).add(a.getBioentity());
			}
		}
		for (OWLObject c : g.getAllOWLObjects()) {
			if (c instanceof OWLClass) {
				if (g.isObsolete(c))
					continue;
				System.out.println(g.getIdentifier(c)+"\t"+g.getLabel(c)+"\t"+
						(aMap.containsKey(c) ? aMap.get(c).size() : "0"));
			}
		}
	}

	@CLIMethod("--gaf-query")
	public void gafQuery(Opts opts) {
		opts.info("LABEL", "extracts lines from a GAF file where the ontology term is a reflexive descendant of the query");
		OWLObject obj = resolveEntity(opts);
		// TODO - allow selection of relations
		Set<OWLObject> descs = g.getDescendantsReflexive(obj);
		List<GeneAnnotation> filtered = new ArrayList<GeneAnnotation>();
		for (GeneAnnotation a : gafdoc.getGeneAnnotations()) {
			OWLObject c = g.getOWLObjectByIdentifier(a.getCls());
			if (descs.contains(c)) {
				filtered.add(a);
				// TODO: option to write entire GAF line
				//System.out.println(a.getBioentityObject()+"\t"+a.getBioentityObject().getSymbol()+"\t"+g.getIdentifier(c)+"\t"+g.getLabel(c));
			}
		}
		gafdoc.setGeneAnnotations(filtered);
	}

	@CLIMethod("--extract-ontology-subset-by-gaf")
	public void extractOntologySubsetByGaf(Opts opts) throws OWLOntologyCreationException {
		opts.info("", "makes an ontology subset using closure of all terms used in GAF");
		IRI subOntIRI = IRI.create("http://purl.obolibrary.org/obo/"+g.getOntologyId()+"-gaf-subset");
		while (opts.hasOpts()) {
			if (opts.nextEq("-u|--uri|--iri")) {
				subOntIRI = IRI.create(opts.nextOpt());
			}
			else {
				break;
			}
		}
		Mooncat m = new Mooncat(g);
		Set<OWLClass> cs = new HashSet<OWLClass>();
		LOG.info("Annotations: "+gafdoc.getGeneAnnotations().size());
		Set<String> unmatchedIds = new HashSet<String>();
		for (GeneAnnotation a : gafdoc.getGeneAnnotations()) {
			OWLClass c = g.getOWLClassByIdentifier(a.getCls());
			//LOG.info(" C:"+c);
			if (c == null) {
				unmatchedIds.add(a.getCls());
				continue;
			}
			cs.add(c);
		}
		if (unmatchedIds.size() > 0) {
			LOG.error("GAF contains "+unmatchedIds.size()+" unmatched IDs");
			for (String id : unmatchedIds) {
				LOG.error("UNMATCHED: "+id);
			}
		}
		LOG.info("Making subset ontology seeded from "+cs.size()+" classes");
		g.setSourceOntology(m.makeMinimalSubsetOntology(cs, subOntIRI, true));
		LOG.info("Made subset ontology; # classes = "+cs.size());
	}

	@CLIMethod("--map2slim")
	public void mapToSlim(Opts opts) throws OWLOntologyCreationException, IOException {
		opts.info("[-s SUBSET-NAME] [--idfile FILE] [-u UNMAPPED-FILE] [-w GAF-OUTPUT]", "Maps annotations in a GAF to an ontology subset, e.g. goslim_pombe");
		String subsetId = null;
		String ofn = null;
		String uofn = null;
		Set<OWLObject> subsetObjs = new HashSet<OWLObject>();
		while (opts.hasOpts()) {
			if (opts.nextEq("-s|--subset")) {
				opts.info("SUBSET-NAME", "id/name of subset. Must be in the loaded ontology. E.g. gosubset_prok");
				subsetId = opts.nextOpt();
			}
			else if (opts.nextEq("--idfile")) {
				List<String> lines = FileUtils.readLines(opts.nextFile());
				for (String line : lines) {
					String id = line;
					id = id.replaceAll(" .*", "");
					id = id.replaceAll("\\t.*", "");
					//LOG.info("ID:"+id);
					OWLObject obj = g.getOWLObjectByIdentifier(id);
					if (obj == null) {
						LOG.error("Cannot find: "+id);
					}
					else {
						subsetObjs.add(obj);
					}
				}
			}
			else if (opts.nextEq("--use-support-ontology")) {
				for (OWLClass c : g.getSupportOntologySet().iterator().next().getClassesInSignature(Imports.INCLUDED)) {
					subsetObjs.add(c);
				}
			}
			else if (opts.nextEq("-w|--write-gaf")) {
				opts.info("FILENAME", "writes mapped GAF here");
				ofn = opts.nextOpt();
			}
			else if (opts.nextEq("-u|--write-unmapped-gaf")) {
				opts.info("FILENAME", "writes unmapped associations as GAF to this file");
				uofn = opts.nextOpt();
			}
			else {
				break;
			}
		}

		gafdoc.addComment("This GAF has been mapped to a subset:");

		Mooncat m = new Mooncat(g);
		Map<OWLObject, Set<OWLObject>> ssm;
		if (subsetId != null) {
			LOG.info("Creating subset map for: "+subsetId);
			gafdoc.addComment("Subset: "+subsetId);
			ssm = m.createSubsetMap(subsetId);
		}
		else {
			LOG.info("Creating subset map objects: "+subsetObjs.size());
			gafdoc.addComment("Subset: user supplied list, size = "+subsetObjs.size());
			ssm = m.createSubsetMap(subsetObjs);
		}
		LOG.info("Input set: "+ssm.keySet().size());
		LOG.info("Annotations: "+gafdoc.getGeneAnnotations().size());
		Set<String> unmatchedIds = new HashSet<String>();
		List<GeneAnnotation> mappedAnns = new ArrayList<GeneAnnotation>();
		int nmapped = 0;
		gafdoc.addComment("Number of annotation in input set: "+gafdoc.getGeneAnnotations().size());

		LOG.info("iterating through all annotations");
		List<GeneAnnotation> unmappedAnns = new ArrayList<GeneAnnotation>();
		int tp = 0;
		int num = 0;
		for (GeneAnnotation a : gafdoc.getGeneAnnotations()) {
			num++;
			OWLClass c = g.getOWLClassByIdentifier(a.getCls());
			if (ssm.containsKey(c) && ssm.get(c).size() > 0) {
				nmapped++;
				Set<OWLObject> mapped = ssm.get(c);
				LOG.debug("Mapping : "+c+" ---> "+mapped);
				for (OWLObject mc : mapped) {
					GeneAnnotation a2 = new GeneAnnotation(a);
					a2.setCls(g.getIdentifier(mc));
					mappedAnns.add(a2);
				}
				tp += mapped.size(); 
			}
			else {
				System.out.println("UNMAPPED: "+a);
				unmappedAnns.add(a);
			}
		}
		LOG.info("# annotations mapped: "+nmapped);
		LOG.info("# annotations in new set: "+mappedAnns.size());
		LOG.info("avg mappings / ann: "+tp / (float)num);
		LOG.info("avg mappings / mapped ann: "+tp / (float)nmapped);
		LOG.info("# annotations unmapped: "+unmappedAnns.size());
		gafdoc.addComment("Number of annotations rewritten: "+nmapped);
		gafdoc.setGeneAnnotations(mappedAnns);
		if (ofn != null) {
			GafWriter gw = new GafWriter();
			gw.setStream(ofn);
			gw.write(gafdoc);
		}
		if (uofn != null) {
			GafWriter gw = new GafWriter();
			gw.setStream(uofn);
			GafDocument ugd = new GafDocument(null, null);
			ugd.setGeneAnnotations(unmappedAnns);
			gw.write(ugd);
		}

	}

	private static class GafParserReport {

		int lineCount = 0;

		final List<GafParserMessages> errors = new ArrayList<GafParserMessages>();
		final List<GafParserMessages> warnings = new ArrayList<GafParserMessages>();

		boolean hasWarningsOrErrors() {
			return !warnings.isEmpty() || !errors.isEmpty();
		}

		boolean hasWarnings() {
			return !warnings.isEmpty();
		}

		boolean hasErrors() {
			return !errors.isEmpty();
		}

		boolean hasNothingToReport() {
			return warnings.isEmpty() && errors.isEmpty();
		}
	}

	private static class GafParserMessages {

		String errorMessage;
		String line;
		int lineNumber;

		GafParserMessages(String errorMessage, String line, int lineNumber) {
			this.errorMessage = errorMessage;
			this.line = line;
			this.lineNumber = lineNumber;
		}
	}

	/**
	 * Similar method to '--run-reasoner', but not only checks for unsatisfiable
	 * classes, but also checks first for a consistent ontology.
	 * 
	 * @param opts
	 * @throws Exception
	 */
	@CLIMethod("--ontology-pre-check")
	public void runOntologyPreCheck(Opts opts) throws Exception {
		String unsatisfiableModule = null;
		while (opts.hasOpts()) {
			if (opts.nextEq("-m|--unsatisfiable-module")) {
				opts.info("", "create a module for the unsatisfiable classes.");
				unsatisfiableModule = opts.nextOpt();
			}
			else {
				break;
			}
		}
		if (g == null) {
			LOG.error("The ontology needs to be loaded first.");
			exit(-1);
			return;
		}
		int code = preCheckOntology("The ontology is inconsistent.", "There are unsatisfiable classes.", unsatisfiableModule);
		if (code != 0) {
			exit(-1);
			return;
		}
	}
	
	protected int preCheckOntology(String inConsistentMsg, String unsatisfiableMsg, String unsatisfiableModule) throws OWLException, IOException {
		// pre-check: only try to load ontology iff:
		// * ontology is consistent 
		// * no unsatisfiable classes
		OWLReasoner currentReasoner = reasoner;
		boolean disposeReasoner = false;
		try {
			if (currentReasoner == null) {
				disposeReasoner = true;
				currentReasoner = new ElkReasonerFactory().createReasoner(g.getSourceOntology());
			}
			boolean consistent = currentReasoner.isConsistent();
			if (consistent == false) {
				LOG.error(inConsistentMsg);
				return -1;
			}
			Set<OWLClass> unsatisfiable = currentReasoner.getUnsatisfiableClasses().getEntitiesMinusBottom();
			if (unsatisfiable.isEmpty() == false) {
				LOG.error(unsatisfiableMsg);
				OWLPrettyPrinter prettyPrinter = getPrettyPrinter();
				for (OWLClass owlClass : unsatisfiable) {
					LOG.error("Unsatisfiable: "+prettyPrinter.render(owlClass));
				}
				LOG.info("Creating module for unsatisfiable classes in file: "+unsatisfiableModule);
				ModuleType mtype = ModuleType.BOT;
				OWLOntologyManager m = g.getManager();
				SyntacticLocalityModuleExtractor sme = new SyntacticLocalityModuleExtractor(m, g.getSourceOntology(), mtype );
				Set<OWLEntity> seeds = new HashSet<OWLEntity>(unsatisfiable);
				Set<OWLAxiom> axioms = sme.extract(seeds);
				OWLOntology module = m.createOntology();
				m.addAxioms(module, axioms);
				File moduleFile = new File(unsatisfiableModule).getCanonicalFile();
				m.saveOntology(module, IRI.create(moduleFile));
				return -1;
			}
		}
		finally {
			if (disposeReasoner && currentReasoner != null) {
				currentReasoner.dispose();
			}
		}
		return 0;
	}
	
	@CLIMethod("--gaf-run-checks")
	public void runGAFChecks(Opts opts) throws Exception {
		boolean predictAnnotations = gafPredictionFile != null;
		boolean experimentalPredictAnnotations = experimentalGafPredictionFile != null;
		if (g != null && gafdoc != null && gafReportFile != null) {
			AnnotationRulesEngine ruleEngine = null;
			AnnotationRulesEngineResult result;
			ExtendAnnotationRulesReportWriter reportWriter = null;
			Level elkLogLevel = null;
			Logger elkLogger = null;
			try {
				elkLogger = Logger.getLogger("org.semanticweb.elk");
				elkLogLevel = elkLogger.getLevel();
				elkLogger.setLevel(Level.ERROR);
				
				// pre-check ontology
				int code = preCheckOntology("Can't validate with an inconsistent ontology", 
						"Can't validate with an ontology with unsatisfiable classes", null);
				if (code != 0) {
					exit(code);
					return;
				}
				
				if (eco == null) {
					eco = EcoMapperFactory.createTraversingEcoMapper(pw).getMapper();
				}
				LOG.info("Start validating GAF");
				AnnotationRulesFactory rulesFactory = new GoAnnotationRulesFactoryImpl(g, eco, gafTaxonModule);
				ruleEngine = new AnnotationRulesEngine(rulesFactory, predictAnnotations, experimentalPredictAnnotations);

				result = ruleEngine.validateAnnotations(gafdoc);
				LOG.info("Finished validating GAF");

				reportWriter = createReportWriter();
				reportWriter.renderEngineResult(parserReport, result, ruleEngine);
			}
			finally {
				if (eco != null) {
					eco.dispose();
					eco = null;
				}
				IOUtils.closeQuietly(reportWriter);
				if (ruleEngine != null) {
					ruleEngine = null;
				}
				if (elkLogLevel != null && elkLogger != null) {
					elkLogger.setLevel(elkLogLevel);
					elkLogger = null;
					elkLogLevel = null;
				}
			}

			// no violations found, delete previous error file (if it exists)
			if ((parserReport == null || parserReport.hasNothingToReport()) && result.isEmpty()) {
				System.out.println("No violations found for gaf.");
				return;
			}

			if (parserReport != null && parserReport.hasWarningsOrErrors()) {
				System.err.print("Parser summary Error count: ");
				System.err.print(parserReport.errors.size());
				System.err.print(" Warning count: ");
				System.err.println(parserReport.warnings.size());
			}

			System.err.print("Rule summary:");
			for(ViolationType type : result.getTypes()) {
				System.err.print(" ");
				System.err.print(type.name());
				System.err.print(" ");
				System.err.print(result.countViolations(type));
			}
			System.err.print(" GAF violations found, reportfile: "+gafReportFile);
			System.err.println();


			// handle error vs warnings
			if (parserReport != null && parserReport.hasErrors()) {
				System.out.println("GAF Validation NOT successful. There is at least one PARSER ERROR.");
				exit(-1); // end with an error code to indicate to Jenkins, that it has errors
			}
			else  if (result.hasErrors()) {
				System.out.println("GAF Validation NOT successful. There is at least one ERROR.");
				exit(-1); // end with an error code to indicate to Jenkins, that it has errors
			}
			else if (result.hasWarnings() || (parserReport != null && parserReport.hasWarnings())){
				System.out.println("GAF Validation NOT successful. There is at least one WARNING.");
				// print magic string for Jenkins (Text-finder Plug-in) to indicate an unstable build.
			}
			else if (result.hasRecommendations()) {
				System.out.println("GAF Validation NOT successful. There is at least one RECOMMENDATION.");
				// print magic string for Jenkins (Text-finder Plug-in) to indicate an unstable build.
			}
		}
		else {
			if (g == null) {
				System.err.println("No graph available for gaf-run-check.");
			}
			if (gafdoc == null) {
				System.err.println("No loaded gaf available for gaf-run-check.");
			}
			if (gafReportFile == null) {
				System.err.println("No report file available for gaf-run-check.");
			}
			exit(-1);
			return;
		}
	}

	private ExtendAnnotationRulesReportWriter createReportWriter() throws IOException {
		return new ExtendAnnotationRulesReportWriter(gafReportFile, gafReportSummaryFile, gafPredictionFile, gafPredictionReportFile, experimentalGafPredictionFile, experimentalGafPredictionReportFile);
	}

	private static class ExtendAnnotationRulesReportWriter extends AnnotationRulesReportWriter {

		/**
		 * @param reportFile
		 * @param summaryFile
		 * @param predictionFile
		 * @param predictionReportFile
		 * @param experimentalPredictionFile
		 * @param experimentalPredictionReportFile
		 * @throws IOException
		 */
		ExtendAnnotationRulesReportWriter(String reportFile,
				String summaryFile, String predictionFile,
				String predictionReportFile, String experimentalPredictionFile,
				String experimentalPredictionReportFile) throws IOException
				{
			super(reportFile, summaryFile, predictionFile, predictionReportFile,
					experimentalPredictionFile, experimentalPredictionReportFile);
				}

		public void renderEngineResult(GafParserReport parserReport, AnnotationRulesEngineResult result, AnnotationRulesEngine engine) {
			if (writer != null) {
				// write header
				writer.println("#Line number\tRuleID\tViolationType\tMessage\tLine");
				writer.println("#------------");
			}
			writeParseErrorsOrWarnings(parserReport, writer, summaryWriter);
			if (writer != null) {
				writer.println("#------------");
				writer.print("# Validation for #");
				writer.print(result.getAnnotationCount());
				writer.print(" annotations");
				if (parserReport != null) {
					writer.print(" in ");
					writer.print(parserReport.lineCount);
					writer.print(" lines");
				}
				writer.println();
				writer.println("#------------");
			}
			if (summaryWriter != null) {
				// Print GAF statistics
				summaryWriter.println("*GAF summary*");
				summaryWriter.println();
				summaryWriter.print("Found ");
				summaryWriter.print(result.getAnnotationCount());
				summaryWriter.print(" annotations");
				if (parserReport != null) {
					summaryWriter.print(" in ");
					summaryWriter.print(parserReport.lineCount);
					summaryWriter.print(" lines");
				}
				summaryWriter.println(".");
				summaryWriter.println();
			}
			renderEngineResult(result, engine);
		}

		private void writeParseErrorsOrWarnings(GafParserReport report, PrintWriter writer, PrintWriter summaryWriter) {
			if (report == null || report.hasWarningsOrErrors() == false) {
				return;
			}
			if (summaryWriter != null) {
				summaryWriter.println("*GAF Parser Summary*");
				summaryWriter.println();
				if (report.hasErrors()) {
					summaryWriter.println("There are "+report.errors.size()+" GAF parser errors.");
					summaryWriter.println();
				}
				if (report.hasWarnings()) {
					summaryWriter.println("There are "+report.warnings.size()+" GAF parser warnings.");
					summaryWriter.println();
				}
			}
			if (writer != null) {
				writer.print("# ");
				writer.print('\t');
				writer.print("GAF Parser");
				writer.print('\t');
				writer.print("ERROR");
				writer.print("\tcount:\t");
				writer.print(report.errors.size());
				writer.println();
				for (GafParserMessages gafParserError : report.errors) {
					writer.print(gafParserError.lineNumber);
					writer.print('\t');
					writer.print('\t');
					writer.print("PARSER ERROR");
					writer.print('\t');
					writer.print(gafParserError.errorMessage);
					writer.print('\t');
					writer.print(gafParserError.line);
					writer.println();
				}

				writer.println("#------------");
				writer.print("# ");
				writer.print('\t');
				writer.print("GAF Parser");
				writer.print('\t');
				writer.print("WARNING");
				writer.print("\tcount:\t");
				writer.print(report.warnings.size());
				writer.println();
				for (GafParserMessages gafParserError : report.warnings) {
					writer.print(gafParserError.lineNumber);
					writer.print('\t');
					writer.print('\t');
					writer.print("WARNING");
					writer.print('\t');
					writer.print(gafParserError.errorMessage);
					writer.print('\t');
					writer.print(gafParserError.line);
					writer.println();
				}
				writer.println("#------------");
			}
		}
	}

	@CLIMethod("--gaf-report-file")
	public void setGAFReportFile(Opts opts) {
		if (opts.hasArgs()) {
			gafReportFile = opts.nextOpt();
		}
	}

	@CLIMethod("--gaf-report-summary-file")
	public void setGAFReportSummaryFile(Opts opts) {
		if (opts.hasArgs()) {
			gafReportSummaryFile = opts.nextOpt();
		}
	}

	@CLIMethod("--gaf-prediction-file")
	public void setGAFPredictionFile(Opts opts) {
		if (opts.hasArgs()) {
			gafPredictionFile = opts.nextOpt();
		}
	}

	@CLIMethod("--gaf-prediction-report-file")
	public void setGAFPredictionReportFile(Opts opts) {
		if (opts.hasArgs()) {
			gafPredictionReportFile = opts.nextOpt();
		}
	}
	
	@CLIMethod("--gaf-validation-unsatisfiable-module")
	public void setGAFTaxonModule(Opts opts) {
		if (opts.hasArgs()) {
			gafTaxonModule = opts.nextOpt();
		}
	}

	@CLIMethod("--experimental-gaf-prediction-file")
	public void setExperimentalGAFPredictionFile(Opts opts) {
		if (opts.hasArgs()) {
			experimentalGafPredictionFile = opts.nextOpt();
		}
	}

	@CLIMethod("--experimental-gaf-prediction-report-file")
	public void setExperimentalGAFPredictionReportFile(Opts opts) {
		if (opts.hasArgs()) {
			experimentalGafPredictionReportFile = opts.nextOpt();
		}
	}
	
	@CLIMethod("--experimental-gaf-prediction")
	public void createExperimentalPredictions(Opts opts) throws Exception {
		GoAnnotationExperimentalPredictionRule experimentalPrediction = new GoAnnotationExperimentalPredictionRule();
		List<Prediction> predictions = experimentalPrediction.getPredictedAnnotations(gafdoc, g);
		System.out.println("Predictions: "+predictions.size());
		AnnotationRulesEngineResult result = new AnnotationRulesEngineResult();
		result.addExperimentalInferences(predictions);
		ExtendAnnotationRulesReportWriter reportWriter = null;
		try {
			reportWriter = createReportWriter();
			reportWriter.renderEngineResult(result, null);
		}
		finally {
			IOUtils.closeQuietly(reportWriter);
		}
	}

	@CLIMethod("--pseudo-rdf-xml")
	public void createRdfXml(Opts opts) throws Exception {
		opts.info("-o OUTPUTFILE", "create an RDF XML file in legacy format.");
		String outputFileName = null;
		List<String> gafSources = null;
		boolean printMemory = false;
		boolean gzipped = false;
		while (opts.hasArgs()) {
			if (opts.nextEq("-o")) {
				outputFileName = opts.nextOpt();
			}
			else if (opts.nextEq("-m|--print-memory")) {
				printMemory = true;
			}
			else if (opts.nextEq("-z|--gz")) {
				gzipped = true;
			}
			else {
				gafSources = opts.nextList();
				break;
			}
		}
		if (g == null) {
			System.err.println("ERROR: No ontology available.");
			exit(-1);
			return;
		}
		if(gafdoc == null && (gafSources == null || gafSources.isEmpty())) {
			System.err.println("ERROR: No GAF(s) available.");
			exit(-1);
			return;
		}
		if (outputFileName == null) {
			System.err.println("ERROR: No output file available.");
			exit(-1);
			return;
		}

		List<GafDocument> gafDocs;
		if (gafSources == null || gafSources.isEmpty()) {
			gafDocs = Collections.singletonList(gafdoc);
		}
		else {
			gafDocs = new ArrayList<GafDocument>();
			if (gafdoc != null) {
				gafDocs.add(gafdoc);
				if (printMemory) {
					printMemory();
				}
			}
			LOG.info("Preparing to load "+gafSources.size()+" GAF documents");
			GafObjectsBuilder builder = new GafObjectsBuilder();
			for(String gafSource : gafSources) {
				LOG.info("Start loading GAF from: "+gafSource);
				GafDocument gafDoc = builder.buildDocument(gafSource);
				gafDocs.add(gafDoc);
				LOG.info("Finished loading GAF from: "+gafSource);
				printMemory();
			}
		}

		OutputStream stream = null;
		try {
			LOG.info("Start writing Pseudo RDF XML to file: "+outputFileName);
			LOG.info("Using "+gafDocs.size()+" GAF documents.");
			PseudoRdfXmlWriter w = new PseudoRdfXmlWriter();
			final long startTime = System.currentTimeMillis();
			w.setProgressReporter(new ProgressReporter() {

				@Override
				public void report(int count, int total) {
					if (count == total) {
						LOG.info("Done writing terms.");
					}
					else if (count % 100 == 0) {
						final long currentTime = System.currentTimeMillis();
						final long elapsed = currentTime - startTime;
						int remainingCount = total - count;
						double perTerm = ((double) elapsed) /  ((double) count);
						long remainingTime = Math.round(remainingCount * perTerm);
						long remainingMinutes = remainingTime / (1000 * 60);
						LOG.info("Written "+count+" of "+total+" terms, remaining: "+remainingMinutes+" minutes");
					}
				}
			});
			stream = new FileOutputStream(new File(outputFileName));
			if (gzipped) {
				stream = new GZIPOutputStream(stream);
			}
			w.write(stream, g, gafDocs);
			LOG.info("Finished writing Pseudo RDF XML");
		}
		catch (Exception e) {
			LOG.error("Error during the creation of the Pseudo RDF XML.", e);
			throw e;
		}
		finally {
			IOUtils.closeQuietly(stream);
		}
	}

	private void printMemory() {
		// run a gc to get a proper memory consumption profile
		System.gc();
		Runtime runtime = Runtime.getRuntime();
		long used = runtime.totalMemory() - runtime.freeMemory();
		LOG.info("Memory "+(used / (1024*1024))+"MB");
	}

	@CLIMethod("--write-xgmml")
	public void writeXgmml(Opts opts) throws IOException {
		opts.info("OUTPUTFILE", "create an XGMML file in legacy format.");
		if (g == null) {
			System.err.println("ERROR: No ontology available.");
			exit(-1);
			return;
		}
		if (!opts.hasArgs()) {
			System.err.println("ERROR: No output file available.");
			exit(-1);
			return;
		}
		String outputFileName = opts.nextOpt();
		XgmmlWriter w = new XgmmlWriter();
		OutputStream stream = new FileOutputStream(new File(outputFileName));
		w.write(stream, g, Arrays.asList(gafdoc));
		stream.close();
	}
	
	@CLIMethod("--gaf2oa")
	public void gaf2oa(Opts opts) throws Exception {
		opts.info("[-o OUTFILE][-f JENA-FORMAT]", 
				"generates RDF (default turtle) following Open Annotation schema");
		String output = null;
		boolean minimize = false;
		String jenaFmt = "TURTLE";
		String mode = null;
		while (opts.hasOpts()) {
			if (opts.nextEq("-o|--output")) {
				output = opts.nextOpt();
			}
			else if (opts.nextEq("-f|--format")) {
				jenaFmt = opts.nextOpt();
			}
			else if (opts.nextEq("-m|--mode")) {
				mode = opts.nextOpt().toLowerCase();
			}
			else {
				break;
			}
		}
		OpenAnnotationRDFWriter w = new OpenAnnotationRDFWriter(jenaFmt);
		if (mode != null) {
			if (mode.equals("gpi"))
				w.setGpifMode();
			else if (mode.equals("gpad"))
				w.setGpadfMode();
			else if (mode.equals("gaf"))
				w.setGafMode();
			else {
				LOG.error("No such mode:" +mode);
			}
		}
		if (output == null) {
			w.write(gafdoc, System.out);
		}
		else {
			w.write(gafdoc, output);
		}

	}



//	@Deprecated
//	@CLIMethod("--visualize-lego")
//	public void visualizeLego(Opts opts) throws Exception {
//		opts.info("[--owl OWLFILE] [-o OUTFOTFILE]", "");
//		LOG.error("The '--visualize-lego' method is deprecated. The dot writer is not compatible with the all-individual approach.");
//		// TODO
//		OWLOntology ont = null;
//		String dotOutputFile = null;
//		String name = null;
//		while (opts.hasOpts()) {
//			if (opts.nextEq("-o|--dot")) {
//				dotOutputFile = opts.nextOpt();
//			}
//			else if (opts.nextEq("--owl")) {
//				ont = pw.parseOWL(opts.nextOpt());
//			}
//			else if (opts.nextEq("-n|--name")) {
//				name = opts.nextOpt();
//			}
//			else {
//				break;
//			}
//		}
//		if (ont == null)
//			ont = g.getSourceOntology();
//
//		if (name == null)
//			name = ont.getOntologyID().toString();
//		if (dotOutputFile != null) {
//			writeLego(ont, dotOutputFile, name);
//		}
//	}
//
//	@Deprecated
//	public void writeLego(OWLOntology ontology, final String output, String name) throws Exception {
//		final OWLGraphWrapper g = new OWLGraphWrapper(ontology);
//
//		Set<OWLNamedIndividual> individuals = ontology.getIndividualsInSignature(true);
//
//		OWLReasonerFactory factory = new ElkReasonerFactory();
//
//		final OWLReasoner reasoner = factory.createReasoner(ontology);
//		try {
//			LegoRenderer renderer = new LegoDotWriter(g, reasoner) {
//
//				BufferedWriter fileWriter = null;
//
//				@Override
//				protected void open() throws IOException {
//					fileWriter = new BufferedWriter(new FileWriter(new File(output)));
//				}
//
//				@Override
//				protected void close() {
//					IOUtils.closeQuietly(fileWriter);
//				}
//
//				@Override
//				protected void appendLine(CharSequence line) throws IOException {
//					//System.out.println(line);
//					fileWriter.append(line).append('\n');
//				}
//			};
//			renderer.render(individuals, name, true);
//		}
//		finally {
//			reasoner.dispose();
//		}
//	}

	@CLIMethod("--gaf-statistics-json")
	public void generateJsonGafStatistics(Opts opts) throws Exception {
		String inputFileString = null;
		String inputFolderString = null;
		String outputFolderString = null;
		String externalCacheFolderString = "external";
		final Map<String, String> externalMap = new HashMap<String, String>();
		while (opts.hasOpts()) {
			if (opts.nextEq("-o")) {
				outputFolderString = opts.nextOpt();
			}
			else if (opts.nextEq("-i|--input-file")) {
				inputFileString = opts.nextOpt();
			}
			else if (opts.nextEq("--gaf-folder")) {
				inputFolderString = opts.nextOpt();
			}
			else if (opts.nextEq("-e|--external-cache")) {
				externalCacheFolderString = opts.nextOpt();
			}
			else if (opts.nextEq("-m|--external-map")) {
				String external = opts.nextOpt();
				String local = opts.nextOpt();
				externalMap.put(external, local);
			}
			else {
				break;
			}
		}
		if (inputFileString == null) {
			System.err.println("No input file defined.");
			exit(-1);
			return;
		}

		final File inputFile = new File(inputFileString).getCanonicalFile();
		final File inputFolder;
		if (inputFolderString != null) {
			inputFolder = new File(inputFolderString).getCanonicalFile();
		}
		else {
			inputFolder = inputFile.getParentFile();
		}
		final File outputFolder;
		if (outputFolderString != null) {
			outputFolder = new File(outputFolderString).getCanonicalFile();
		}
		else {
			outputFolder = inputFile.getParentFile();
		}
		final File externalCacheFolder = new File(externalCacheFolderString);
		String jsonSource = FileUtils.readFileToString(inputFile, "UTF-8");

		JsonParser parser = new JsonParser();
		JsonElement source = parser.parse(jsonSource);
		if (source.isJsonObject()) {
			JsonObject sourceObj = source.getAsJsonObject();
			JsonElement resources = sourceObj.get("resources");
			if (resources != null && resources.isJsonArray()) {
				for(JsonElement listElem : resources.getAsJsonArray()) {
					if (listElem.isJsonObject()) {
						JsonObject dbObj = listElem.getAsJsonObject();
						String id = getValue(dbObj, "id");
						String gafFilename = getValue(dbObj, "gaf_filename");
						String external = getValue(dbObj, "external");
						try {
							File gafFile;
							if (external != null) {
								String local = externalMap.get(id);
								if (local != null) {
									gafFile = new File(local).getCanonicalFile();
								}
								else {
									gafFile = downloadExternal(external, gafFilename, externalCacheFolder);
								}
							}
							else {
								gafFile = new File(inputFolder, gafFilename);
							}
							AnnotationDocumentMetadata metadata = getMetaDataFromGAF(gafFile, id);

							String outputFileName;
							if (gafFilename.endsWith(".gz")) {
								outputFileName = gafFilename.substring(0, (gafFilename.length()-3)) + ".json";
							}
							else {
								outputFileName = gafFilename + ".json";
							}
							File outputFile = new File(outputFolder, outputFileName);
							writeJsonMetadata(metadata, id, outputFile);
						}
						catch (Exception e) {
							LOG.warn("Error during the processing of: "+id+". Metedata might be in-complete.", e);
						}
					}
				}
			}
		}
	}

	private AnnotationDocumentMetadata getMetaDataFromGAF(File gaf, final String id) throws Exception {
		LOG.info("Extracting metadata for "+id+" from file: "+gaf.getAbsolutePath());
		final AnnotationDocumentMetadata metadata = new AnnotationDocumentMetadata();
		metadata.dbname = id;
		metadata.gafDocumentSizeInBytes = gaf.length();

		final GAFParser parser = new GAFParser();
		// also read the comments, they contain the date information
		parser.addCommentListener(new CommentListener() {

			@Override
			public void readingComment(String comment, String line, int lineNumber) {
				comment = StringUtils.trimToNull(comment);
				if (comment != null && comment.startsWith("Submission Date:")) {
					String dateString = comment.substring("Submission Date:".length());
					metadata.submissionDate = StringUtils.trimToNull(dateString);
				}
			}
		});
		try {
			Set<String> bioentities = new HashSet<String>();
			parser.parse(gaf);
			while (parser.next()) {
				// log info every ten million line.
				int lineNumber = parser.getLineNumber();
				if (lineNumber > 0 && (lineNumber % 10000000) == 0) {
					LOG.info("GAF "+id+", reading line number: "+lineNumber);
				}
				metadata.annotationCount += 1L;
				String evidence = parser.getEvidence();
				if ("IEA".equals(evidence) == false) {
					metadata.annotationCountExcludingIEA += 1L;
				}
				String bioentity = parser.getDb() + ":" + parser.getDbObjectId();
				bioentities.add(bioentity);
			}
			metadata.annotatedEntityCount = bioentities.size();
		}
		finally {
			parser.dispose();
		}
		return metadata;
	}

	private File downloadExternal(String external, String name, File cache) throws Exception {
		InputStream input = null;
		OutputStream output = null;
		try {
			File cacheFile = new File(cache, name);
			LOG.info("Start downloading "+name+" to cache: "+cacheFile.getAbsolutePath());

			// WARNING: This will fail for FTP file larger than 2GB
			// This is a bug in Java 5 and 6, but is fixed in Java 7
			input = new URL(external).openStream();
			output = new FileOutputStream(cacheFile);
			IOUtils.copyLarge(input, output);
			LOG.info("Finished downloading "+name+" to cache: "+cacheFile.getAbsolutePath());
			return cacheFile;
		}
		finally {
			IOUtils.closeQuietly(input);
			IOUtils.closeQuietly(output);
		}
	}

	private void writeJsonMetadata(AnnotationDocumentMetadata metadata, String id, File outputFile) throws Exception {
		LOG.info("Writing metadata for "+id+" to JSON file: "+outputFile.getAbsolutePath());
		final GsonBuilder gsonBuilder = new GsonBuilder();
		final Gson gson = gsonBuilder.setPrettyPrinting().create();
		String json = gson.toJson(metadata);
		FileUtils.write(outputFile, json, "UTF-8");
	}

	private String getValue(JsonObject obj, String name) {
		String value = null;
		JsonElement jsonElement = obj.get(name);
		if (jsonElement != null && jsonElement.isJsonPrimitive()) {
			value = jsonElement.getAsString();
		}
		return value;
	}

	@CLIMethod("--gpad-gpi")
	public void loadGpadGpi(Opts opts) throws Exception {
		String gpadFileName = null;
		String gpiFileName = null;
		String ecoMappingSource = null;
		String reportFileName = null;
		boolean includeUnknownBioentities = false;
		boolean includeUnmappedECO = true;
		boolean clearComments = false;

		AspectProvider aspectProvider = null;

		while (opts.hasOpts()) {
			if (opts.nextEq("-a|--gpad|--gpa"))
				gpadFileName = opts.nextOpt();
			else if (opts.nextEq("-i|--gpi")) {
				gpiFileName = opts.nextOpt();
			}
			else if (opts.nextEq("--eco")) {
				ecoMappingSource = opts.nextOpt();
			}
			else if (opts.nextEq("--include-unknown-bioentities")) {
				includeUnknownBioentities = true;
			}
			else if (opts.nextEq("--exclude-unknown-bioentities")) {
				includeUnknownBioentities = false;
			}
			else if (opts.nextEq("--exclude-unmapped-eco")) {
				includeUnmappedECO = false;
			}
			else if (opts.nextEq("--include-unmapped-eco")) {
				includeUnmappedECO = true;
			}
			else if (opts.nextEq("-r|--report-file")) {
				reportFileName = opts.nextOpt();
			}
			else if (opts.nextEq("--go-aspects")) {
				Map<String, String> mappings = new HashMap<String, String>();
				mappings.put("GO:0008150", "P");
				mappings.put("GO:0003674", "F");
				mappings.put("GO:0005575", "C");
				aspectProvider = DefaultAspectProvider.createAspectProvider(g, mappings , reasoner);
			}
			else if (opts.nextEq("-c|--clear-comments")) {
				clearComments = true;
			}
			else {
				break;
			}
		}
		if (gpadFileName == null) {
			System.err.println("ERROR: No gpad file specified.");
			exit(-1);
			return;
		}
		if (gpiFileName == null) {
			System.err.println("ERROR: No gpi file specified.");
			exit(-1);
			return;
		}
		File reportFile = null;
		if (reportFileName != null) {
			reportFile = new File(reportFileName).getCanonicalFile();
			// delete previous report
			FileUtils.deleteQuietly(reportFile);
		}

		SimpleEcoMapper ecoMapper;
		if (ecoMappingSource == null) {
			ecoMapper = EcoMapperFactory.createSimple();
		}
		else {
			ecoMapper = EcoMapperFactory.createSimple(ecoMappingSource);
		}
		GpadGpiObjectsBuilder builder = new GpadGpiObjectsBuilder(ecoMapper);
		builder.setAspectProvider(aspectProvider);
		builder.setGpadIncludeUnknowBioentities(includeUnknownBioentities);
		builder.setGpadIncludeUnmappedECO(includeUnmappedECO);
		IssueListener.DefaultIssueListener listener = null;
		if (reportFile != null) {
			listener = new IssueListener.DefaultIssueListener();
			builder.addIssueListener(listener);
		}

		File gpad = new File(gpadFileName).getCanonicalFile();
		File gpi = new File(gpiFileName).getCanonicalFile();
		Pair<BioentityDocument,GafDocument> pair = builder.loadGpadGpi(gpad, gpi);
		gafdoc = pair.getRight();
		bioentityDocument = pair.getLeft();

		if (clearComments) {
			gafdoc.getComments().clear();
			bioentityDocument.getComments().clear();
		}

		if (reportFile != null) {
			BufferedWriter writer = null;
			try {
				writer = new BufferedWriter(new FileWriter(reportFile));
				// report statistics

				// Write mapped errors
				Map<String, String> mappedErrors = listener.getMappedErrors();
				if (mappedErrors != null) {
					List<String> keys = Ordering.natural().sortedCopy(mappedErrors.keySet());
					for (String key : keys) {
						writer.append("ERROR").append('\t');
						writer.append(key).append('\t');
						writer.append(mappedErrors.get(key));
						writer.append('\n');
					}
				}

				// write other errors
				List<String> errors = listener.getErrors();
				if (errors != null) {
					for (String error : errors) {
						writer.append("ERROR").append('\t').append('\t');
						writer.append(error);
						writer.append('\n');
					}
				}

				// write mapped warnings
				Map<String, String> mappedWarnings = listener.getMappedWarnings();
				if (mappedWarnings != null) {
					List<String> keys = Ordering.natural().sortedCopy(mappedWarnings.keySet());
					for (String key : keys) {
						writer.append("WARNING").append('\t');
						writer.append(key).append('\t');
						writer.append(mappedWarnings.get(key));
						writer.append('\n');
					}
				}

				// write other warnings
				List<String> warnings = listener.getWarnings();
				if (warnings != null) {
					for (String error : warnings) {
						writer.append("WARNING").append('\t').append('\t');
						writer.append(error);
						writer.append('\n');
					}
				}
			}
			finally {
				IOUtils.closeQuietly(writer);
			}
		}
	}

	@CLIMethod("--write-gpad")
	public void writeGpad(Opts opts) throws Exception {
		String outputFileName = null;
		double version = 1.2d;
		while (opts.hasOpts()) {
			if (opts.nextEq("-o|--output")) {
				outputFileName = opts.nextOpt();
			}
			else if (opts.nextEq("--v-11")) {
				version = 1.1d;
			}
			else if (opts.nextEq("--v-12")) {
				version = 1.2d;
			}
			else {
				break;
			}
		}
		if (outputFileName == null) {
			System.err.println("ERROR: No output file specified.");
			exit(-1);
			return;
		}
		if (gafdoc == null) {
			System.err.println("ERROR: No annotations loaded.");
			exit(-1);
			return;
		}
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new File(outputFileName));
			GpadWriter writer = new GpadWriter(pw, version);
			writer.write(gafdoc);
		}
		finally {
			IOUtils.closeQuietly(pw);
		}
	}

	@CLIMethod("--write-gpi")
	public void writeGpi(Opts opts) throws Exception {
		String outputFileName = null;
		double version = 1.2d;
		while (opts.hasOpts()) {
			if (opts.nextEq("-o|--output")) {
				outputFileName = opts.nextOpt();
			}
			else if (opts.nextEq("--v-11")) {
				version = 1.1d;
			}
			else if (opts.nextEq("--v-12")) {
				version = 1.2d;
			}
			else {
				break;
			}
		}
		if (outputFileName == null) {
			System.err.println("ERROR: No output file specified.");
			exit(-1);
			return;
		}
		if (bioentityDocument == null && gafdoc == null) {
			System.err.println("ERROR: No bioentities loaded.");
			exit(-1);
			return;
		}
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new File(outputFileName));
			GpiWriter writer = new GpiWriter(pw, version);
			if (bioentityDocument != null) {
				writer.write(bioentityDocument);
			}
			else {
				// TODO create proper document with all isoforms used in the annotations
				writer.write(gafdoc.getBioentities());
			}

		}
		finally {
			IOUtils.closeQuietly(pw);
		}
	}
}
