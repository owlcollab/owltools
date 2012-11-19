package owltools.cli;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.obolibrary.obo2owl.Obo2OWLConstants;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import owltools.cli.tools.CLIMethod;
import owltools.gaf.EcoTools;
import owltools.gaf.GafDocument;
import owltools.gaf.GafObjectsBuilder;
import owltools.gaf.GafParserListener;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.inference.AnnotationPredictor;
import owltools.gaf.inference.CompositionalClassPredictor;
import owltools.gaf.inference.Prediction;
import owltools.gaf.io.PseudoRdfXmlWriter;
import owltools.gaf.io.XgmmlWriter;
import owltools.gaf.owl.GAFOWLBridge;
import owltools.gaf.owl.GAFOWLBridge.BioentityMapping;
import owltools.gaf.rules.AnnotationRuleViolation.ViolationType;
import owltools.gaf.rules.AnnotationRulesEngine;
import owltools.gaf.rules.AnnotationRulesEngine.AnnotationRulesEngineResult;
import owltools.gaf.rules.AnnotationRulesFactory;
import owltools.gaf.rules.go.GoAnnotationRulesFactoryImpl;
import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;
import owltools.mooncat.Mooncat;

/**
 * GAF tools for command-line, includes validation of GAF files.
 * 
 * TODO implement a filtering mechanism for GAF files
 */
public class GafCommandRunner extends CommandRunner {

	private static final Logger LOG = Logger.getLogger(GafCommandRunner.class);
	
	public GafDocument gafdoc = null;
	private GafParserReport parserReport = null;
	
	private String gafReportSummaryFile = null;
	private String gafReportFile = null;
	
	public OWLGraphWrapper eco = null;
	
	/**
	 * Used for loading GAFs into memory
	 * 
	 * @param opts
	 * @throws Exception
	 */
	@CLIMethod("--gaf")
	public void gaf(Opts opts) throws Exception {
		opts.info("GAF-FILE [--createReport]", "parses GAF and makes this the current GAF document");
		final String input = opts.nextOpt();
		boolean createReport = false;
		if (opts.hasOpts() && opts.nextEq("--createReport")) {
			createReport = true;
		}
		
		GafObjectsBuilder builder = new GafObjectsBuilder();
		if (createReport) {
			parserReport = new GafParserReport();
			builder.getParser().addParserListener(new GafParserListener() {
				
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
		LOG.info("Start loading GAF from: "+input);
		gafdoc = builder.buildDocument(input);
		if (parserReport != null) {
			parserReport.lineCount = builder.getParser().getLineNumber();
		}
		if (gafdoc == null) {
			LOG.error("The GAF parsing finished with an empty result.");
			exit(-1);
		}
		else {
			LOG.info("Finished loading GAF.");
		}
	}
	
	@CLIMethod("--gaf2owl")
	public void gaf2Owl(Opts opts) throws OWLException {
		opts.info("[-n TARGET-IRI] [-o FILE]", "translates previously loaded GAF document into OWL");
		GAFOWLBridge bridge;
		String iri = null;
		String out = null;
		boolean isSkipIndividuals = false;
		BioentityMapping bioentityMapping = null;
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
			else
				break;

		}
		if (iri != null) {
			if (!iri.startsWith("http:")) {
				iri = Obo2OWLConstants.DEFAULT_IRI_PREFIX+iri;
				if (!iri.endsWith(".owl"))
					iri = iri + ".owl";
			}
			// todo - save tgtOnt
			OWLOntology tgtOnt = g.getManager().createOntology(IRI.create(iri));

			bridge = new GAFOWLBridge(g, tgtOnt);
		}
		else {
			// adds gaf axioms back into main ontology
			bridge = new GAFOWLBridge(g);
		}
		bridge.setGenerateIndividuals(!isSkipIndividuals);
		if (bioentityMapping != null) {
			bridge.setBioentityMapping(bioentityMapping);
		}
		LOG.info("Start converting GAF to OWL");
		bridge.translate(gafdoc);
		LOG.info("Finished converting GAF to OWL");
		if (out != null) {
			pw.saveOWL(bridge.getTargetOntology(),out,g);
		}
	}
	
	@CLIMethod("--gaf-xp-predict")
	public void gafXpPredict(Opts opts) {
		owlpp = new OWLPrettyPrinter(g);
		if (gafdoc == null) {
			System.err.println("No gaf document (use '--gaf GAF-FILE') ");
			exit(1);
		}
		AnnotationPredictor ap = new CompositionalClassPredictor(gafdoc, g);
		Set<Prediction> predictions = ap.getAllPredictions();
		System.out.println("Predictions:"+predictions.size());
		for (Prediction p : predictions) {
			System.out.println(p.render(owlpp));
		}
	}
	
	@CLIMethod("--gaf-term-IC-values")
	public void gafTermICValues(Opts opts) {
		// TODO - ensure has_part and other relations are excluded
		owlpp = new OWLPrettyPrinter(g);
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
	
	@CLIMethod("--gaf-term-counts")
	public void gafTermCounts(Opts opts) {
		// TODO - ensure has_part and other relations are excluded
		owlpp = new OWLPrettyPrinter(g);
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
		for (GeneAnnotation a : gafdoc.getGeneAnnotations()) {
			OWLObject c = g.getOWLObjectByIdentifier(a.getCls());
			if (descs.contains(c)) {
				// TODO: option to write entire GAF line
				System.out.println(a.getBioentityObject()+"\t"+a.getBioentityObject().getSymbol()+"\t"+g.getIdentifier(c)+"\t"+g.getLabel(c));
			}
		}
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
	
	@CLIMethod("--gaf-run-checks")
	public void runGAFChecks(Opts opts) throws Exception {
		if (g != null && gafdoc != null && gafReportFile != null) {
			if (eco == null) {
				eco = EcoTools.loadECO(pw);
			}
			LOG.info("Start validating GAF");
			AnnotationRulesFactory rulesFactory = new GoAnnotationRulesFactoryImpl(g, eco);
			AnnotationRulesEngine ruleEngine = new AnnotationRulesEngine(rulesFactory );
			AnnotationRulesEngineResult result = ruleEngine.validateAnnotations(gafdoc);
			LOG.info("Finished validating GAF");
			File reportFile = new File(gafReportFile);
			File summaryFile = null;
			if (gafReportSummaryFile != null) {
				summaryFile = new File(gafReportSummaryFile);
			}
			
			// no violations found, delete previous error file (if it exists)
			if ((parserReport == null || parserReport.hasNothingToReport()) && result.isEmpty()) {
				System.out.println("No violations found for gaf.");
				FileUtils.deleteQuietly(reportFile);
				FileUtils.write(reportFile, ""); // create empty file
				if (summaryFile != null) {
					FileUtils.deleteQuietly(summaryFile);
					FileUtils.write(summaryFile, ""); // create empty file
				}
				return;
			}
			
			// write parse errors and rule violations
			createAllReportFiles(parserReport, result, ruleEngine, reportFile, summaryFile);
			
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
		}
	}
	
	private void createAllReportFiles(GafParserReport parserReport, 
			AnnotationRulesEngineResult result, AnnotationRulesEngine engine, 
			File reportFile, File summaryFile) throws IOException
	{
		LOG.info("Start writing reports to file: "+gafReportFile);
		PrintWriter writer = null;
		PrintWriter summaryWriter = null;
		try {
			if (summaryFile != null) {
				summaryWriter = new PrintWriter(summaryFile);
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
			writer = new PrintWriter(reportFile);
			writer.println("#------------");
			writer.print("# Validation for ");
			writer.print(result.getAnnotationCount());
			writer.print("# annotations");
			if (parserReport != null) {
				writer.print(" in ");
				writer.print(parserReport.lineCount);
				writer.print(" lines");
			}
			writer.println();
			writer.println("#------------");
			if (parserReport != null && parserReport.hasWarningsOrErrors()) {
				writeParseErrors(parserReport, writer, summaryWriter);
			}
			AnnotationRulesEngineResult.renderViolations(result, engine, writer, summaryWriter);
		} finally {
			IOUtils.closeQuietly(summaryWriter);
			IOUtils.closeQuietly(writer);
			LOG.info("Finished writing reports to file.");
		}
	}
	
	private void writeParseErrors(GafParserReport report, PrintWriter writer, PrintWriter summaryWriter) throws IOException {
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
		writer.println("#Line number\tRuleID\tViolationType\tMessage\tLine");
		writer.println("#------------");
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
	
	@CLIMethod("--pseudo-rdf-xml")
	public void createRdfXml(Opts opts) throws IOException {
		opts.info("OUTPUTFILE", "create an RDF XML file in legacy format.");
		if (g == null) {
			System.err.println("ERROR: No ontology available.");
			exit(-1);
			return;
		}
		if(gafdoc == null) {
			System.err.println("ERROR: No GAF available.");
			exit(-1);
			return;
		}
		if (!opts.hasArgs()) {
			System.err.println("ERROR: No output file available.");
			exit(-1);
			return;
		}
		String outputFileName = opts.nextOpt();
		PseudoRdfXmlWriter w = new PseudoRdfXmlWriter();
		OutputStream stream = new FileOutputStream(new File(outputFileName));
		w.write(stream, g, Arrays.asList(gafdoc));
		stream.close();
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


}
