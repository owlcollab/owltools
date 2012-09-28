package owltools.cli;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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

import owltools.cli.tools.CLIMethod;
import owltools.gaf.EcoTools;
import owltools.gaf.GafDocument;
import owltools.gaf.GafObjectsBuilder;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.inference.AnnotationPredictor;
import owltools.gaf.inference.CompositionalClassPredictor;
import owltools.gaf.inference.Prediction;
import owltools.gaf.io.PseudoRdfXmlWriter;
import owltools.gaf.io.XgmmlWriter;
import owltools.gaf.owl.GAFOWLBridge;
import owltools.gaf.rules.AnnotationRuleViolation.ViolationType;
import owltools.gaf.rules.AnnotationRulesEngine;
import owltools.gaf.rules.AnnotationRulesEngine.AnnotationRulesEngineResult;
import owltools.gaf.rules.AnnotationRulesFactory;
import owltools.gaf.rules.go.GoAnnotationRulesFactoryImpl;
import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;

/**
 * GAF tools for command-line, includes validation of GAF files.
 * 
 * TODO implement a filtering mechanism for GAF files
 */
public class GafCommandRunner extends CommandRunner {

	private static final Logger LOG = Logger.getLogger(GafCommandRunner.class);
	
	public GafDocument gafdoc = null;
	
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
		opts.info("GAF-FILE", "parses GAF and makes this the current GAF document");
		GafObjectsBuilder builder = new GafObjectsBuilder();
		final String input = opts.nextOpt();
		LOG.info("Start loading GAF from: "+input);
		gafdoc = builder.buildDocument(input);
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
		while (opts.hasOpts()) {
			if (opts.nextEq("-n"))
				iri = opts.nextOpt();
			else if (opts.nextEq("-o")) {
				out = opts.nextOpt();
			}
			else if (opts.nextEq("-c") || opts.nextEq("--skip-individuals")) {
				isSkipIndividuals = true;
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
		Set<OWLObject> descs = g.getDescendantsReflexive(obj);
		for (GeneAnnotation a : gafdoc.getGeneAnnotations()) {
			OWLObject c = g.getOWLObjectByIdentifier(a.getCls());
			if (descs.contains(c)) {
				System.out.println(g.getIdentifier(c)+"\t"+a.getBioentityObject()+"\t"+a.getBioentityObject().getSymbol());
			}
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
			if (result.isEmpty()) {
				System.out.println("No violations found for gaf.");
				FileUtils.deleteQuietly(reportFile);
				FileUtils.write(reportFile, ""); // create empty file
				if (summaryFile != null) {
					FileUtils.deleteQuietly(summaryFile);
					FileUtils.write(summaryFile, ""); // create empty file
				}
				return;
			}
			
			// write violations
			writeAnnotationRuleViolations(result, ruleEngine, reportFile, summaryFile);
			
			System.err.print("Summary:");
			for(ViolationType type : result.getTypes()) {
				System.err.print(" ");
				System.err.print(type.name());
				System.err.print(" ");
				System.err.print(result.countViolations(type));
			}
			System.err.print(" GAF violations found, reportfile: "+gafReportFile);
			System.err.println();
			
			
			// handle error vs warnings
			if (result.hasErrors()) {
				System.out.println("GAF Validation NOT successful. There is at least one ERROR.");
				exit(-1); // end with an error code to indicate to Jenkins, that it has errors
			}
			else if (result.hasWarnings()){
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
	
	private void writeAnnotationRuleViolations(AnnotationRulesEngineResult result, AnnotationRulesEngine engine, 
			File reportFile, File summaryFile) throws IOException
	{
		LOG.info("Start writing violations to report file: "+gafReportFile);
		PrintWriter writer = null;
		PrintWriter summaryWriter = null;
		try {
			if (summaryFile != null) {
				summaryWriter = new PrintWriter(summaryFile);
			}
			writer = new PrintWriter(reportFile);
			AnnotationRulesEngineResult.renderViolations(result, engine, writer, summaryWriter);
		} finally {
			IOUtils.closeQuietly(summaryWriter);
			IOUtils.closeQuietly(writer);
			LOG.info("Finished writing violations to report file.");
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
