package owltools.ontologyrelease.reports;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphWrapper;

/**
 * Basic tool for generating ontology reports on a term basis. 
 */
public class OntologyReportGenerator {
	
	private final List<OntologyReport> reports;
	private final File reportFolder;

	/**
	 * @param reports
	 * @param reportFolder
	 */
	public OntologyReportGenerator(List<OntologyReport> reports, File reportFolder) {
		super();
		this.reports = reports;
		this.reportFolder = reportFolder;
	}
	
	private List<PrintWriter> createReportFileWriters() throws IOException {
		List<PrintWriter> writers = new ArrayList<PrintWriter>(reports.size());
		for(OntologyReport report : reports) {
			File reportFile;
			String subFolderName = report.getReportSubFolderName();
			if (subFolderName == null || subFolderName.isEmpty()) {
				reportFile = new File(reportFolder, report.getReportFileName());
			}
			else {
				reportFile = new File(new File(reportFolder, subFolderName), report.getReportFileName());
			}
			// TODO check file for overwrite?
			PrintWriter writer = new PrintWriter(reportFile);
			writers.add(writer);
		}
		return writers;
	}
	
	/**
	 * Generate the internal reports for a given ontology.
	 * 
	 * @param graph
	 * @throws IOException
	 */
	public void generateReports(OWLGraphWrapper graph) throws IOException {
		List<PrintWriter> writers = null;
		try {
			writers = createReportFileWriters();
			for (int i = 0; i < reports.size(); i++) {
				reports.get(i).start(writers.get(i), graph);
			}
			for(OWLClass cls : getSortedOWLClasses(graph)) {
				for (int i = 0; i < reports.size(); i++) {
					reports.get(i).handleTerm(writers.get(i), cls, graph);
				}
			}
			for (int i = 0; i < reports.size(); i++) {
				reports.get(i).end(writers.get(i), graph);
			}
		}
		finally {
			if (writers != null) {
				for (PrintWriter writer : writers) {
					IOUtils.closeQuietly(writer);
				}
			}
		}
	}
	
	private List<OWLClass> getSortedOWLClasses(OWLGraphWrapper graph) {
		List<OWLClass> classes = new ArrayList<OWLClass>(graph.getSourceOntology().getClassesInSignature());
		Collections.sort(classes);
		return classes;
	}
	
	/**
	 * Ontology report to be run with the {@link OntologyReportGenerator}.
	 */
	public static interface OntologyReport {
		
		/**
		 * Get the sub folder name for the report
		 * 
		 * @return subFolder name or null
		 */
		public String getReportSubFolderName();
		
		/**
		 * Get the target file name for this report.
		 * 
		 * @return fileName
		 */
		public String getReportFileName();
		
		/**
		 * Start the report.
		 * 
		 * @param writer
		 * @param graph
		 * @throws IOException
		 */
		public void start(PrintWriter writer, OWLGraphWrapper graph) throws IOException;
		
		/**
		 * Handle an {@link OWLObject} during the report.
		 * 
		 * @param writer
		 * @param owlClass
		 * @param graph
		 * @throws IOException
		 */
		public void handleTerm(PrintWriter writer, OWLClass owlClass, OWLGraphWrapper graph) throws IOException;
		
		/**
		 * End the report.
		 * 
		 * @param writer
		 * @param graph
		 * @throws IOException
		 */
		public void end(PrintWriter writer, OWLGraphWrapper graph) throws IOException;
	}
	
	/**
	 * Abstract helper class for {@link OntologyReport}.
	 */
	public static abstract class AbstractReport implements OntologyReport {
		
		@Override
		public String getReportSubFolderName() {
			return "doc";
		}

		public void start(PrintWriter writer, OWLGraphWrapper graph) throws IOException {
			String header = getFileHeader();
			// TODO print ontology information
			if (header != null) {
				writer.println(header);
			}
		}
		
		protected String getFileHeader() {
			return null;
		}
		
		public void end(PrintWriter writer, OWLGraphWrapper graph) throws IOException {
			// intentionally empty
		}
		
		protected void writeTabs(PrintWriter writer, CharSequence... fields) {
			for (int i = 0; i < fields.length; i++) {
				if (i > 0) {
					writer.print('\t');
				}
				writer.print(fields[i]);

			}
			writer.println();
		}
	}
}
