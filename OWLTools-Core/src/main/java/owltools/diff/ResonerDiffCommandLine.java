package owltools.diff;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.semanticweb.owlapi.model.OWLAxiom;

import owltools.InferenceBuilder;
import owltools.graph.OWLGraphWrapper;
import owltools.io.CatalogXmlIRIMapper;
import owltools.io.OWLPrettyPrinter;
import owltools.io.ParserWrapper;


/**
 * Wrapper for {@link ReasonerDiff} as command-line tool.
 */
public class ResonerDiffCommandLine {

	private static void usage() {
		System.out.println("Available parameters:\n"+
				" --change ONTOLOGY resource to be applied as change\n" +
				" ONTOLOGY resource to be used as base ontology\n" +
				" --report output file with the report (OPTIONAL)\n" +
				" --catalog-xml specify an catalog.xml file for IRI mapping (OPTIONAL)\n" +
				" --resoner ["+InferenceBuilder.REASONER_HERMIT+"|"+
					InferenceBuilder.REASONER_ELK+"|"+
					InferenceBuilder.REASONER_PELLET+"|"+
					InferenceBuilder.REASONER_JCEL+"] (OPTIONAL)\n");
	}

	public static void main(String[] args) throws Exception {
		// input options
		List<String> baseLineSources = new ArrayList<String>();
		List<String> changeSources = new ArrayList<String>();
		String catalogXML = null;
		String reasonerName = InferenceBuilder.REASONER_HERMIT;
		
		// output options
		String reportFile = null;
		
		// parse command-line parameters
		int i = 0;
		while (i < args.length) {
			String opt = args[i];
			i++;

			if (opt.trim().length() == 0)
				continue;

			if (opt.equals("--h") || opt.equals("--help") || opt.equals("-h")) {
				usage();
				System.exit(0);
			}

			else if (opt.equals("--reasoner")) {
				reasonerName = args[i];
				i++;
			}
			else if (opt.equals("--catalog-xml")) {
				catalogXML = args[i];
				i++;
			}
			else if (opt.equals("--change")) {
				changeSources.add(args[i]);
				i++;
			}
			else if (opt.equals("--report")) {
				reportFile = args[i];
				i++;
			}
			else {
				String tokens[] = opt.split(" ");
				for (String token : tokens)
					baseLineSources.add(token);
			}
		}
		
		if (baseLineSources.isEmpty()) {
			System.out.println("No baseline ONTOLOGY found.");
			usage();
			System.exit(0);
		}
		if (changeSources.isEmpty()) {
			System.out.println("No change ONTOLOGY found.");
			usage();
			System.exit(0);
		}
		
		// create graphs
		ParserWrapper pw = new ParserWrapper();
		if (catalogXML != null) {
			pw.addIRIMapper(new CatalogXmlIRIMapper(catalogXML));
		}
		
		OWLGraphWrapper baseLine = parseGraph(baseLineSources, pw);
		OWLGraphWrapper change = parseGraph(changeSources, pw);
		
		// create diff
		ReasonerDiff diff = ReasonerDiff.createReasonerDiff(baseLine, change, reasonerName);
		
		// create diff report
		StringBuilder report = createDiffReport(baseLine, diff);
		
		// write report to file or system.out
		if (reportFile != null) {
			FileUtils.write(new File(reportFile), report);
		}
		else {
			System.out.println(report);
		}
		
	}

	private static OWLGraphWrapper parseGraph(List<String> sources, ParserWrapper pw) throws Exception {
		OWLGraphWrapper graph = pw.parseToOWLGraph(sources.get(0));
		if (sources.size() > 1) {
			for (int j = 1; j < sources.size(); j++) {
				graph.addSupportOntology(pw.parse(sources.get(j)));
			}
		}
		return graph;
	}

	private static StringBuilder createDiffReport(OWLGraphWrapper baseLine, ReasonerDiff diff) {
		OWLPrettyPrinter pp = new OWLPrettyPrinter(baseLine);
		StringBuilder report = new StringBuilder();
		
		List<OWLAxiom> newAxioms = diff.getNewAxioms();
		if (newAxioms != null && !newAxioms.isEmpty()) {
			report.append("# There are "+newAxioms.size()+" new inferred axioms.").append('\n');
			for (OWLAxiom owlAxiom : newAxioms) {
				report.append(pp.render(owlAxiom)).append('\n');
			}
		}
		else {
			report.append("# There are NO new inferred axioms.").append('\n');
		}
		report.append("# ----------------").append('\n');
		
		List<OWLAxiom> removedInferredAxioms = diff.getRemovedInferredAxioms();
		if (removedInferredAxioms != null && !removedInferredAxioms.isEmpty()) {
			report.append("# There are "+removedInferredAxioms.size()+" removed inferred axioms.").append('\n');
			for (OWLAxiom owlAxiom : removedInferredAxioms) {
				report.append(pp.render(owlAxiom)).append('\n');
			}
		}
		else {
			report.append("# There are NO removed inferred axioms.").append('\n');
		}
		return report;
	}

}
