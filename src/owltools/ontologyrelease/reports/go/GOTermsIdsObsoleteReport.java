package owltools.ontologyrelease.reports.go;

import static owltools.ontologyrelease.reports.go.GOTermsIdsReport.getOboNamespaceChar;

import java.io.IOException;
import java.io.PrintWriter;

import org.semanticweb.owlapi.model.OWLClass;

import owltools.graph.OWLGraphWrapper;
import owltools.ontologyrelease.reports.OntologyReportGenerator.AbstractReport;
import owltools.ontologyrelease.reports.OntologyReportGenerator.OntologyReport;

/**
 * {@link OntologyReport} for obsolete GeneOntology terms.
 */
public class GOTermsIdsObsoleteReport extends AbstractReport {

	public static final GOTermsIdsObsoleteReport INSTANCE = new GOTermsIdsObsoleteReport();
	
	GOTermsIdsObsoleteReport() {
		super();
	}
	
	@Override
	public String getReportFileName() {
		return "GO.terms_ids_obs";
	}

	@Override
	protected String getFileHeader() {
		return "! GO IDs and text strings (primary only) and obsolete indicator\n"
				+ "! GO:0000000 [tab] text string [tab] F|P|C [tab] (obs)\n"
				+ "! where F = molecular function, P = biological process, C = cellular component\n"
				+ "! obs = term is obsolete\n!\n";
	}

	@Override
	public void handleTerm(PrintWriter writer, OWLClass owlClass, OWLGraphWrapper graph) throws IOException {
		if (graph.isObsolete(owlClass)) {
			char oboNamespaceChar = getOboNamespaceChar(owlClass, graph);
			if (oboNamespaceChar > 0) {
				String id = graph.getIdentifier(owlClass);
				String label = graph.getLabel(owlClass);
				writeTabs(writer, writer, id, label, oboNamespaceChar, "obs");
			}
		}
	}
}
