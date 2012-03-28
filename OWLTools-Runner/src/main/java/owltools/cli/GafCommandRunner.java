package owltools.cli;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.cli.tools.CLIMethod;
import owltools.gaf.GafDocument;
import owltools.gaf.GafObjectsBuilder;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.inference.AnnotationPredictor;
import owltools.gaf.inference.CompositionalClassPredictor;
import owltools.gaf.inference.Prediction;
import owltools.gaf.owl.GAFOWLBridge;
import owltools.gaf.rules.AnnotationRuleViolation;
import owltools.gaf.rules.AnnotationRulesEngine;
import owltools.gaf.rules.AnnotationRulesEngine.AnnotationRuleCheckException;
import owltools.gaf.rules.AnnotationRulesFactory;
import owltools.gaf.rules.go.GoAnnotationRulesFactoryImpl;
import owltools.io.OWLPrettyPrinter;

/**
 * GAF tools for command-line, includes validation of GAF files.
 * 
 * TODO implement a filtering mechanism for GAF files
 */
public class GafCommandRunner extends CommandRunner {

	public GafDocument gafdoc = null;
	
	private String gafReportFile = null;
	
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
		gafdoc = builder.buildDocument(opts.nextOpt());				
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
				iri = "http://purl.obolibrary.org/obo/"+iri;
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
		bridge.translate(gafdoc);
		if (out != null) {
			pw.saveOWL(bridge.getTargetOntology(),out);
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
		opts.info("LABEL", "list edges in graph closure to root nodes");
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
	public void runGAFChecks(Opts opts) throws AnnotationRuleCheckException, IOException {
		if (g != null && gafdoc != null && gafReportFile != null) {
			AnnotationRulesFactory rulesFactory = new GoAnnotationRulesFactoryImpl(g);
			AnnotationRulesEngine ruleEngine = new AnnotationRulesEngine(-1, rulesFactory );
			Map<String, List<AnnotationRuleViolation>> allViolations = ruleEngine.validateAnnotations(gafdoc);
			File reportFile = new File(gafReportFile);
			
			// no violations found, delete previous error file (if it exists)
			if (allViolations.isEmpty()) {
				System.out.println("No violations found for gaf.");
				FileUtils.deleteQuietly(reportFile);
				return;
			}
			
			// write violations
			PrintWriter writer = null;
			try {
				// TODO make this a more detailed report
				int allViolationsCount = 0;
				writer = new PrintWriter(reportFile);
				writer.println("------------");
				List<String> ruleIds = new ArrayList<String>(allViolations.keySet());
				Collections.sort(ruleIds);
				for (String ruleId : ruleIds) {
					List<AnnotationRuleViolation> violationList = allViolations.get(ruleId);
					writer.println(ruleId + "  count: "+ violationList.size());
					for (AnnotationRuleViolation violation : violationList) {
						writer.print("Line ");
						writer.print(violation.getLineNumber());
						writer.print(": ");
						writer.println(violation.getMessage());
						allViolationsCount++;
					}
					writer.println("------------");
				}
				System.err.println(allViolationsCount+" GAF violations found, reportfile: "+gafReportFile);
			} finally {
				IOUtils.closeQuietly(writer);
			}
			exit(-1); // end with an error code to indicate to Jenkins, that it was not successful
		}
	}
	
	@CLIMethod("--gaf-report-file")
	public void setGAFReportFile(Opts opts) {
		if (opts.hasArgs()) {
			gafReportFile = opts.nextOpt();
		}
	}

}
