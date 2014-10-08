package owltools.gaf.rules;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import owltools.gaf.inference.Prediction;
import owltools.gaf.io.GafWriter;
import owltools.gaf.rules.AnnotationRuleViolation.ViolationType;
import owltools.gaf.rules.AnnotationRulesEngine.AnnotationRulesEngineResult;

/**
 * Write reports for the results of annotations validation and prediction.
 */
public class AnnotationRulesReportWriter implements Closeable {

	 protected PrintWriter writer;
	 protected PrintWriter summaryWriter;
	 protected PrintStream predictionWriter;
	 protected PrintWriter predictionReportWriter;
	 protected PrintStream experimentalPredictionWriter;
	 protected PrintWriter experimentalPredictionReportWriter;
	
	 
	 public AnnotationRulesReportWriter(String reportFile,
			 String summaryFile,
			 String predictionFile,
			 String predictionReportFile,
			 String experimentalPredictionFile,
			 String experimentalPredictionReportFile) throws IOException
	 {
		this(reportFile != null ? new File(reportFile) : null,
			summaryFile != null ? new File(summaryFile) : null,
			predictionFile != null ? new File(predictionFile) : null,
			predictionReportFile != null ? new File(predictionReportFile) : null,
			experimentalPredictionFile != null ? new File(experimentalPredictionFile) : null,
			experimentalPredictionReportFile != null ? new File(experimentalPredictionReportFile) : null);
	 }
	 
	 public AnnotationRulesReportWriter(File reportFile,
			 File summaryFile,
			 File predictionFile,
			 File predictionReportFile,
			 File experimentalPredictionFile,
			 File experimentalPredictionReportFile) throws IOException
	 {
		this(reportFile != null ? new PrintWriter(reportFile) : null,
			summaryFile != null ? new PrintWriter(summaryFile) : null,
			predictionFile != null ? new PrintStream(predictionFile) : null,
			predictionReportFile != null ? new PrintWriter(predictionReportFile) : null,
			experimentalPredictionFile != null ? new PrintStream(experimentalPredictionFile) : null,
			experimentalPredictionReportFile != null ? new PrintWriter(experimentalPredictionReportFile) : null);
	 }
	 
	protected AnnotationRulesReportWriter(PrintWriter writer,
			PrintWriter summaryWriter,
			PrintStream predictionWriter,
			PrintWriter predictionReportWriter,
			PrintStream experimentalPredictionWriter,
			PrintWriter experimentalPredictionReportWriter) {
		this.writer = writer;
		this.summaryWriter = summaryWriter;
		this.predictionWriter = predictionWriter;
		this.predictionReportWriter = predictionReportWriter;
		this.experimentalPredictionWriter = experimentalPredictionWriter;
		this.experimentalPredictionReportWriter = experimentalPredictionReportWriter;
	}


	/**
	 * A simple tab delimited print-out of the validation results.
	 * 
	 * @param result
	 * @param engine
	 * @param writer
	 */
	public static void renderViolations(AnnotationRulesEngineResult result, AnnotationRulesEngine engine, PrintWriter writer) {
		AnnotationRulesReportWriter reporter = new AnnotationRulesReportWriter(writer, null, null, null, null, null);
		try {
			reporter.renderEngineResult(result, engine);
		}
		finally {
			IOUtils.closeQuietly(reporter);
		}
	}


	private void printOntologySummary(AnnotationRulesEngineResult result, PrintWriter writer) {
		if (result.ontologyVersions != null && !result.ontologyVersions.isEmpty()) {
			writer.println();
			writer.println("*Used Ontology Summary*");
			writer.println();
			List<String> sortedIds = new ArrayList<String>(result.ontologyVersions.keySet());
			Collections.sort(sortedIds);
			for (String oid : sortedIds) {
				writer.print('\t');
				writer.print(oid);
				String version = result.ontologyVersions.get(oid);
				if (version != null) {
					writer.print('\t');
					writer.println(version);
				}
				else {
					writer.println();
				}
			}
			writer.println();
		}
	}
		
	/**
	 * A print-out of the results, summaries, and preditions.
	 * 
	 * @param result
	 * @param engine
	 */
	public void renderEngineResult(AnnotationRulesEngineResult result, AnnotationRulesEngine engine) {
		if (summaryWriter != null) {
			summaryWriter.println("*GAF Validation Summary*");
		}
		if (writer != null) {
			List<ViolationType> types = result.getTypes();
			if (types.isEmpty() && result.predictions.isEmpty()) {
				writer.print("# No errors, warnings, or recommendations to report.");
				if (summaryWriter != null) {
					summaryWriter.println();
					summaryWriter.println("No errors, warnings, recommendations, inferences, or predictions to report.");
					summaryWriter.println();
					printOntologySummary(result, summaryWriter);
				}
				return;
			}
			if (types.isEmpty()) {
				writer.print("# No errors, warnings, or recommendations to report.");
				if (summaryWriter != null) {
					summaryWriter.println();
					summaryWriter.println("No errors, warnings, or recommendations to report.");
					summaryWriter.println();
					printOntologySummary(result, summaryWriter);
				}
			}
			else {
				writer.println("#Line number\tRuleID\tViolationType\tMessage\tLine");
				writer.println("#------------");
				if (summaryWriter != null) {
					summaryWriter.println("Errors are reported first.");
					summaryWriter.println();
				}
				for(ViolationType type : types) {
					Map<String, List<AnnotationRuleViolation>> violations = result.getViolations(type);
					List<String> ruleIds = new ArrayList<String>(violations.keySet());
					Collections.sort(ruleIds);
					for (String ruleId : ruleIds) {
						AnnotationRule rule = engine.getRule(ruleId);
						List<AnnotationRuleViolation> violationList = violations.get(ruleId);
						writer.print("# ");
						writer.print(ruleId);
						writer.print('\t');
						printEscaped(rule.getName(), writer, true);
						writer.print('\t');
						writer.print(type.name());
						writer.print("\tcount:\t");
						writer.print(violationList.size());
						writer.println();

						if (summaryWriter != null) {
							summaryWriter.print("For rule ");
							summaryWriter.print(ruleId);
							summaryWriter.print(" (http://www.geneontology.org/GO.annotation_qc.shtml#");
							summaryWriter.print(ruleId);
							summaryWriter.print(")\n ");
							summaryWriter.print(rule.getName());
							summaryWriter.print(", ");
							if (violationList.size() == 1) {
								summaryWriter.print("there is one violation with type ");
							}
							else {
								summaryWriter.print("there are ");
								summaryWriter.print(violationList.size());
								summaryWriter.print(" violations with type ");
							}
							summaryWriter.print(type.name());
							summaryWriter.print('.');
							summaryWriter.println();
							summaryWriter.println();
						}
						for (AnnotationRuleViolation violation : violationList) {
							writer.print(violation.getLineNumber());
							writer.print('\t');
							writer.print(ruleId);
							writer.print('\t');
							writer.print(type.name());
							writer.print('\t');
							final String message = violation.getMessage();
							printEscaped(message, writer, false);
							writer.print('\t');
							String annotationRow = violation.getAnnotationRow();
							if (annotationRow != null) {
								// do not escape the annotation row
								// try to preserve the tab format to allow import into excel or similar
								writer.print(annotationRow);
							}
							writer.println();
						}
						writer.println("#------------");
					}
				}
			}
		}
		if (result.predictions.isEmpty()) {
			if (summaryWriter != null) {
				// no inferences
				summaryWriter.println();
				summaryWriter.println("*GAF Prediction Summary*");
				summaryWriter.println();
				summaryWriter.println("No inferences or predictions to report.");
			}
			if (predictionWriter != null) {
				// write empty file with GAF header
				GafWriter gafWriter = new GafWriter();
				gafWriter.setStream(predictionWriter);
				List<String> comments = Arrays.asList(""," Generated predictions",""); 
				gafWriter.writeHeader(comments);
			}
		}
		else {
			if (summaryWriter != null) {
				// append prediction count
				summaryWriter.println();
				summaryWriter.println("*GAF Prediction Summary*");
				summaryWriter.println();
				summaryWriter.print("Found ");
				if (result.predictions.size() == 1) {
					summaryWriter.print("one prediction");
				}
				else {
					summaryWriter.print(result.predictions.size());
					summaryWriter.print(" predictions");
				}
				summaryWriter.println(", see prediction file for details.");
			}
			writePredictions(predictionWriter, predictionReportWriter, result.predictions, result.ontologyVersions);
		}
		if (result.experimentalPredictions.isEmpty() == false) {
			writePredictions(experimentalPredictionWriter, experimentalPredictionReportWriter, result.experimentalPredictions, result.ontologyVersions);
		}
		if (summaryWriter != null) {
			printOntologySummary(result, summaryWriter);
		}
	}
	
	private void writePredictions(PrintStream predictionWriter, PrintWriter predictionReportWriter, List<Prediction> predictions, Map<String, String> ontologyVersions) {
		if (predictionReportWriter != null) {
			for (Prediction prediction : predictions) {
				String reason = prediction.getReason();
				if (reason != null) {
					predictionReportWriter.println(reason);
				}
			}
		}
		if (predictionWriter != null) {

			GafWriter.BufferedGafWriter bufferedGafWriter = new GafWriter.BufferedGafWriter();
			// write predictions in GAF format
			// write to buffer
			for (Prediction prediction : predictions) {
				if (prediction.isRedundantWithExistingAnnotations() == false && prediction.isRedundantWithOtherPredictions() == false) {
					bufferedGafWriter.write(prediction.getGeneAnnotation());
				}
			}
			// sort buffer
			List<String> lines = bufferedGafWriter.getLines();
			Collections.sort(lines);


			// Append to writer
			// GAF header
			GafWriter gafWriter = new GafWriter();
			gafWriter.setStream(predictionWriter);
			List<String> comments = new ArrayList<String>();
			comments.add("");
			DateFormat format = new SimpleDateFormat("yyyy/MM/dd");
			comments.add("Date: "+format.format(new Date()));
			if (ontologyVersions != null && !ontologyVersions.isEmpty()) {
				List<String> sortedIds = new ArrayList<String>(ontologyVersions.keySet());
				Collections.sort(sortedIds);
				comments.add("");
				comments.add(" Used ontologies and versions (optional)");
				for (String oid : sortedIds) {
					String version = ontologyVersions.get(oid);
					if (version != null) {
						comments.add("\t"+oid+"\t"+version);
					}
					else {
						comments.add("\t"+oid);
					}
				}
				comments.add("");
			}
			comments.add(" Generated predictions");
			comments.add("");
			gafWriter.writeHeader(comments);
			// append sorted lines
			for (String line : lines) {
				predictionWriter.print(line);
			}
		}
	}
	
	private void printEscaped(String s, PrintWriter writer, boolean useWhitespaces) {
		final int length = s.length();
		for (int i = 0; i < length; i++) {
			char c = s.charAt(i);
			if (c == '\t') {
				if (useWhitespaces) {
					writer.print(' '); // replace tab with whitespace
				}
				else {
					writer.print("\\t"); // escape tabs
				}
			}
			if (c == '\n') {
				if (useWhitespaces) {
					writer.print(' '); // replace new line with whitespace
				}
				else {
					writer.print("\\n"); // escape new lines
				}
			}
			else if (Character.isWhitespace(c)) {
				// normalize other white spaces
				writer.print(' ');
			}
			else {
				writer.print(c);
			}
		}
	}


	@Override
	public void close() throws IOException {
		IOUtils.closeQuietly(writer);
		IOUtils.closeQuietly(summaryWriter);
		IOUtils.closeQuietly(predictionWriter);
		IOUtils.closeQuietly(predictionReportWriter);
		IOUtils.closeQuietly(experimentalPredictionWriter);
		IOUtils.closeQuietly(experimentalPredictionReportWriter);
	}
	
}
