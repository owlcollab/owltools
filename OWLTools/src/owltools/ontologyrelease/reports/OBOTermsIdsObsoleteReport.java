package owltools.ontologyrelease.reports;

import static owltools.ontologyrelease.reports.OBOTermsIdsReport.getOboNamespace;

import java.io.IOException;
import java.io.PrintWriter;

import org.semanticweb.owlapi.model.OWLClass;

import owltools.graph.OWLGraphWrapper;
import owltools.ontologyrelease.reports.OntologyReportGenerator.AbstractReport;
import owltools.ontologyrelease.reports.OntologyReportGenerator.OntologyReport;

/**
 * {@link OntologyReport} for obsolete GeneOntology terms.
 */
public class OBOTermsIdsObsoleteReport extends AbstractReport {

	private final String filePrefix;
	private final String fileHeader;
	
	public OBOTermsIdsObsoleteReport(OWLGraphWrapper graph) {
		this(graph.getOntologyId().toUpperCase()+".");
	}
	
	public OBOTermsIdsObsoleteReport(String filePrefix) {
		this(filePrefix, "! OBO IDs and text strings (primary only) and obsolete indicator\n"
				+ "! ID:0000000 [tab] text string [tab] obo namespace [tab] (obs)\n"
				+ "! obs = term is obsolete\n!\n");
	}
	
	public OBOTermsIdsObsoleteReport(String filePrefix, String fileHeader) {
		super();
		this.filePrefix = filePrefix;
		this.fileHeader = fileHeader;
	}
	
	@Override
	public String getReportFileName() {
		return filePrefix+"terms_ids_obs";
	}

	@Override
	protected String getFileHeader() {
		return fileHeader;
	}

	@Override
	public void handleTerm(PrintWriter writer, OWLClass owlClass, OWLGraphWrapper graph) throws IOException {
		if (graph.isObsolete(owlClass)) {
			String oboNamespace = getOboNamespace(owlClass, graph);
			String id = graph.getIdentifier(owlClass);
			String label = graph.getLabel(owlClass);
			writeTabs(writer, id, label, oboNamespace, "obs");
		}
	}
}
