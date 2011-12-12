package owltools.ontologyrelease.reports.go;

import java.util.ArrayList;
import java.util.List;

import owltools.ontologyrelease.reports.OBOTermsAltIdsReport;
import owltools.ontologyrelease.reports.OBOTermsIdsObsoleteReport;
import owltools.ontologyrelease.reports.OBOTermsIdsReport;
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
			goReports.add(new OBOTermsAltIdsReport("GO.", 
					"! GO IDs (primary and secondary) and text strings\n"
					+ "! GO:0000000 (primary) [tab] GO:0000000 (secondary, separated by space(s) if >1) [tab] text string [tab] F|P|C\n"
					+ "! where F = molecular function, P = biological process, C = cellular component\n"
					+ "! obs = term is obsolete\n!\n"));
			goReports.add(new OBOTermsIdsObsoleteReport("GO.",
					"! GO IDs and text strings (primary only) and obsolete indicator\n"
					+ "! GO:0000000 [tab] text string [tab] F|P|C [tab] (obs)\n"
					+ "! where F = molecular function, P = biological process, C = cellular component\n"
					+ "! obs = term is obsolete\n!\n"));
			goReports.add(new OBOTermsIdsReport("GO.",
					"! GO IDs (primary only) and name text strings\n"
					+ "! GO:0000000 [tab] text string [tab] F|P|C\n"
					+ "! where F = molecular function, P = biological process, C = cellular component\n"
					+ "!\n"));
			goReports.add(new ObsoletesAllReport("GO."));
			goReports.add(new ObsoletesExactReport("GO."));
			goReports.add(new ObsoletesInExactReport("GO."));
		}
		return goReports;
	}
}
