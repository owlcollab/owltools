package owltools.ontologyrelease.reports.go;

import static owltools.ontologyrelease.reports.go.GOTermsIdsReport.getOboNamespaceChar;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import org.semanticweb.owlapi.model.OWLClass;

import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLGraphWrapper.ISynonym;
import owltools.ontologyrelease.reports.OntologyReportGenerator.AbstractReport;
import owltools.ontologyrelease.reports.OntologyReportGenerator.OntologyReport;

/**
 * {@link OntologyReport} for GeneOntology terms with alternative identifiers.
 */
public class GOTermsAltIdsReport extends AbstractReport {

	public static final GOTermsAltIdsReport INSTANCE = new GOTermsAltIdsReport();
	
	GOTermsAltIdsReport() {
		super();
	}
	
	@Override
	public String getReportFileName() {
		return "GO.terms_alt_ids";
	}

	@Override
	protected String getFileHeader() {
		return "! GO IDs (primary and secondary) and text strings\n"
				+ "! GO:0000000 (primary) [tab] GO:0000000 (secondary, separated by space(s) if >1) [tab] text string [tab] F|P|C\n"
				+ "! where F = molecular function, P = biological process, C = cellular component\n"
				+ "! obs = term is obsolete\n!\n";
	}

	@Override
	public void handleTerm(PrintWriter writer, OWLClass owlClass, OWLGraphWrapper graph) throws IOException {
		List<String> altIds = graph.getAltIds(owlClass);
		if (altIds != null && !altIds.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			for (String altId : altIds) {
				if (sb.length() > 0) {
					sb.append(' ');
				}
				sb.append(altId);
			}
			char oboNamespaceChar = getOboNamespaceChar(owlClass, graph);
			if (oboNamespaceChar > 0) {
				String id = graph.getIdentifier(owlClass);
				String label = graph.getLabel(owlClass);
				String altIdsString = sb.toString();
				writeTabs(writer, writer, id, altIdsString, label, oboNamespaceChar); // TODO append obsolete state?
				List<ISynonym> synonyms = graph.getOBOSynonyms(owlClass);
				if (synonyms != null && !synonyms.isEmpty()) {
					for (ISynonym synonym : synonyms) {
						writeTabs(writer, writer, id, altIdsString, synonym.getLabel(), oboNamespaceChar); // TODO append obsolete state?
					}
				}
			}
		}
	}

}
