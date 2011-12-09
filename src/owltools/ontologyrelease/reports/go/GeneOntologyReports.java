package owltools.ontologyrelease.reports.go;

import java.util.ArrayList;
import java.util.List;

import owltools.ontologyrelease.reports.ObsoletesAllReport;
import owltools.ontologyrelease.reports.ObsoletesExactReport;
import owltools.ontologyrelease.reports.ObsoletesInExactReport;
import owltools.ontologyrelease.reports.OntologyReportGenerator.OntologyReport;

/**
 * Provide all reports for the GeneOntology.
 */
public class GeneOntologyReports {

	private GeneOntologyReports() {
		// no instances
	}
	
	private static volatile List<OntologyReport> goReports = null;
	
	public static List<OntologyReport> getGeneOntologyReports() {
		synchronized (GeneOntologyReports.class) {
			goReports = new ArrayList<OntologyReport>();
			goReports.addAll(External2GoReports.getReports());
			goReports.add(GOTermsAltIdsReport.INSTANCE);
			goReports.add(GOTermsIdsObsoleteReport.INSTANCE);
			goReports.add(GOTermsIdsReport.INSTANCE);
			goReports.add(new ObsoletesAllReport("GO."));
			goReports.add(new ObsoletesExactReport("GO."));
			goReports.add(new ObsoletesInExactReport("GO."));
		}
		return goReports;
	}
}
