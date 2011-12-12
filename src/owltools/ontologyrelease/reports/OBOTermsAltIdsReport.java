package owltools.ontologyrelease.reports;

import static owltools.ontologyrelease.reports.OBOTermsIdsReport.getOboNamespace;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import org.semanticweb.owlapi.model.OWLClass;

import owltools.graph.OWLGraphWrapper;
import owltools.ontologyrelease.reports.OntologyReportGenerator.AbstractReport;
import owltools.ontologyrelease.reports.OntologyReportGenerator.OntologyReport;

/**
 * {@link OntologyReport} for GeneOntology terms with alternative identifiers.
 */
public class OBOTermsAltIdsReport extends AbstractReport {

	private final String filePrefix;
	private final String fileHeader;
	
	public OBOTermsAltIdsReport(OWLGraphWrapper graph) {
		this(graph.getOntologyId().toUpperCase()+".");
	}
	
	public OBOTermsAltIdsReport(String filePrefix) {
		this(filePrefix,
				"! OBO IDs (primary and secondary) and text strings\n"
				+ "! ID:0000000 (primary) [tab] ID:0000000 (secondary, separated by space(s) if >1) [tab] text string [tab] obo namespace\n"
				+ "! obs = term is obsolete\n!\n");
	}
	
	public OBOTermsAltIdsReport(String filePrefix, String fileHeader) {
		super();
		this.filePrefix = filePrefix;
		this.fileHeader = fileHeader;
	}
	
	@Override
	public String getReportFileName() {
		return filePrefix+"terms_alt_ids";
	}

	@Override
	protected String getFileHeader() {
		return fileHeader;
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
			String oboNamespace = getOboNamespace(owlClass, graph);
			String id = graph.getIdentifier(owlClass);
			String label = graph.getLabel(owlClass);
			writeTabs(writer, id, sb, label, oboNamespace); // TODO append obsolete state?
		}
	}

}
