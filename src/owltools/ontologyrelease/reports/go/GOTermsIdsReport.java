package owltools.ontologyrelease.reports.go;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLGraphWrapper.ISynonym;
import owltools.ontologyrelease.reports.OntologyReportGenerator.AbstractReport;
import owltools.ontologyrelease.reports.OntologyReportGenerator.OntologyReport;

/**
 * {@link OntologyReport} for identifiers and labels of all 
 * GeneOntology terms. 
 */
public class GOTermsIdsReport extends AbstractReport {

	public static final GOTermsIdsReport INSTANCE = new GOTermsIdsReport();
	
	GOTermsIdsReport() {
		super();
	}
	
	@Override
	public String getReportFileName() {
		return "GO.terms_and_ids";
	}

	@Override
	protected String getFileHeader() {
		return "! GO IDs (primary only) and name text strings\n"
				+ "! GO:0000000 [tab] text string [tab] F|P|C\n"
				+ "! where F = molecular function, P = biological process, C = cellular component\n"
				+ "!\n";
	}

	@Override
	public void handleTerm(PrintWriter writer, OWLObject owlObject, OWLGraphWrapper graph) throws IOException {
		String id = graph.getIdentifier(owlObject);
		String label = graph.getLabel(owlObject);
		char oboNamespaceChar = getOboNamespaceChar(owlObject, graph);
		if (oboNamespaceChar > 0) {
			writeTabs(writer, writer, id, label, oboNamespaceChar);
			List<ISynonym> synonyms = graph.getOBOSynonyms(owlObject);
			if (synonyms != null && !synonyms.isEmpty()) {
				for (ISynonym synonym : synonyms) {
					writeTabs(writer, writer, id, synonym.getLabel(), oboNamespaceChar);
				}
			}
		}
	}

	static char getOboNamespaceChar(OWLObject owlObject, OWLGraphWrapper graph) {
		String namespace = graph.getNamespace(owlObject);
		if ("biological_process".equals(namespace)) {
			return 'P';
		} else if ("molecular_function".equals(namespace)) {
			return 'F';
		} else if ("cellular_component".equals(namespace)) {
			return 'C';
		}
		return 0;
	}

}
