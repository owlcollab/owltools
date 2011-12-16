package owltools.ontologyrelease.reports;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import org.semanticweb.owlapi.model.OWLClass;

import owltools.graph.OWLGraphWrapper;
import owltools.ontologyrelease.reports.OntologyReportGenerator.AbstractReport;
import owltools.ontologyrelease.reports.OntologyReportGenerator.OntologyReport;

/**
 * {@link OntologyReport} for obsolete terms with a direct replacement 
 */
public class ObsoletesExactReport extends AbstractReport {

	private final String filePrefix;

	/**
	 * @param graph
	 */
	public ObsoletesExactReport(OWLGraphWrapper graph) {
		this(graph.getOntologyId().toUpperCase()+".");
	}
	
	/**
	 * @param filePrefix
	 */
	public ObsoletesExactReport(String filePrefix) {
		super();
		this.filePrefix = filePrefix;
	}

	@Override
	public String getReportFileName() {
		return filePrefix+"obsoletes-exact";
	}

	@Override
	protected String getFileHeader() {
		return "! Obsolete terms and direct annotation substitutes\n!\n"
				+ "!Obsolete [tab] Alternative\n";
	}

	@Override
	public void handleTerm(PrintWriter writer, OWLClass owlClass, OWLGraphWrapper graph) throws IOException {
		if (graph.isObsolete(owlClass)) {
			List<String> replacedBys = graph.getReplacedBy(owlClass);
			if (replacedBys != null && !replacedBys.isEmpty()) {
				String id = graph.getIdentifier(owlClass);
				for (String replacedBy : replacedBys) {
					writeTabs(writer, id, replacedBy);
				}
			}
		}
	}

}
