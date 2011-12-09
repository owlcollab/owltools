package owltools.ontologyrelease.reports;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphWrapper;
import owltools.ontologyrelease.reports.OntologyReportGenerator.AbstractReport;
import owltools.ontologyrelease.reports.OntologyReportGenerator.OntologyReport;

/**
 * {@link OntologyReport} for obsolete terms with a direct or possible replacement 
 */
public class ObsoletesAllReport extends AbstractReport {

	private final String filePrefix;

	/**
	 * @param graph
	 */
	public ObsoletesAllReport(OWLGraphWrapper graph) {
		this(graph.getOntologyId().toUpperCase()+".");
	}
	
	/**
	 * @param filePrefix
	 */
	public ObsoletesAllReport(String filePrefix) {
		super();
		this.filePrefix = filePrefix;
	}

	@Override
	public String getReportFileName() {
		return filePrefix+"obsoletes-all";
	}

	@Override
	protected String getFileHeader() {
		return "! Obsolete terms and alternatives, marked as either direct [replaced-by] or possible [consider]\n!\n" +
				"!Obsolete [tab] Alternative\n";
	}

	@Override
	public void handleTerm(PrintWriter writer, OWLObject owlObject, OWLGraphWrapper graph) throws IOException {
		if (graph.isObsolete(owlObject)) {
			List<String> considerList = graph.getConsider(owlObject);
			if (considerList != null && !considerList.isEmpty()) {
				String id = graph.getIdentifier(owlObject);
				for (String consider : considerList) {
					writeTabs(writer, writer, id, consider);
				}
			}
			List<String> replacedBys = graph.getReplacedBy(owlObject);
			if (replacedBys != null && !replacedBys.isEmpty()) {
				String id = graph.getIdentifier(owlObject);
				for (String replacedBy : replacedBys) {
					writeTabs(writer, writer, id, replacedBy);
				}
			}
		}
	}

}
