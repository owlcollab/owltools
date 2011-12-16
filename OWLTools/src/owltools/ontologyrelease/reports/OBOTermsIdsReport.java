package owltools.ontologyrelease.reports;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import org.semanticweb.owlapi.model.OWLClass;

import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLGraphWrapper.ISynonym;
import owltools.ontologyrelease.reports.OntologyReportGenerator.AbstractReport;
import owltools.ontologyrelease.reports.OntologyReportGenerator.OntologyReport;

/**
 * {@link OntologyReport} for identifiers and labels of all 
 * GeneOntology terms. 
 */
public class OBOTermsIdsReport extends AbstractReport {

	private final String filePrefix;
	private final String fileHeader;
	
	public OBOTermsIdsReport(OWLGraphWrapper graph) {
		this(graph.getOntologyId().toUpperCase()+".");
	}
	
	public OBOTermsIdsReport(String filePrefix) {
		this(filePrefix, "! GO IDs (primary only) and name text strings\n"
				+ "! GO:0000000 [tab] text string [tab] obo namespace\n"
				+ "!\n");
	}
	
	public OBOTermsIdsReport(String filePrefix, String fileHeader) {
		super();
		this.filePrefix = filePrefix;
		this.fileHeader = fileHeader;
	}
	
	@Override
	public String getReportFileName() {
		return filePrefix+"terms_and_ids";
	}

	@Override
	protected String getFileHeader() {
		return fileHeader;
	}

	@Override
	public void handleTerm(PrintWriter writer, OWLClass owlClass, OWLGraphWrapper graph) throws IOException {
		String id = graph.getIdentifier(owlClass);
		String label = graph.getLabel(owlClass);
		String oboNamespace = getOboNamespace(owlClass, graph);
		writeTabs(writer, id, label, oboNamespace);
		List<ISynonym> synonyms = graph.getOBOSynonyms(owlClass);
		if (synonyms != null && !synonyms.isEmpty()) {
			for (ISynonym synonym : synonyms) {
				writeTabs(writer, id, synonym.getLabel(), oboNamespace);
			}
		}
	}

	static String getOboNamespace(OWLClass owlClass, OWLGraphWrapper graph) {
		String namespace = graph.getNamespace(owlClass);
		if (namespace == null) {
			return ""; // empty string
		}
		// special case for GO
		if ("biological_process".equals(namespace)) {
			return "P";
		} else if ("molecular_function".equals(namespace)) {
			return "F";
		} else if ("cellular_component".equals(namespace)) {
			return "C";
		}
		return namespace;
	}

}
