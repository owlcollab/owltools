package owltools.gaf.rules;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.inference.Prediction;
import owltools.gaf.io.GafWriter;
import owltools.gaf.owl.GAFOWLBridge;
import owltools.gaf.owl.GAFOWLBridge.BioentityMapping;
import owltools.gaf.rules.AnnotationRuleViolation.ViolationType;
import owltools.gaf.rules.go.BasicChecksRule;
import owltools.graph.OWLGraphWrapper;

public class AnnotationRulesEngine {

	private static Logger LOG = Logger.getLogger(AnnotationRulesEngine.class);
	
	private final AnnotationRulesFactory rulesFactory;
	private final boolean createInferences;
	
	public AnnotationRulesEngine(AnnotationRulesFactory rulesFactory, boolean createInferences){
		this.rulesFactory = rulesFactory;
		this.createInferences = createInferences;
		rulesFactory.init();
	}
	
	
	/**
	 * Retrieve the corresponding rule for a given rule id.
	 * 
	 * @param id
	 * @return rule or null
	 */
	public AnnotationRule getRule(String id) {
		if (id == null) {
			return null;
		}
		List<AnnotationRule> annotationRules = rulesFactory.getGeneAnnotationRules();
		if (annotationRules != null) {
			for (AnnotationRule annotationRule : annotationRules) {
				if (id.equals(annotationRule.getRuleId())) {
					return annotationRule;
				}
			}
		}
		List<AnnotationRule> documentRules = rulesFactory.getGafDocumentRules();
		if (documentRules != null) {
			for (AnnotationRule annotationRule : documentRules) {
				if (id.equals(annotationRule.getRuleId())) {
					return annotationRule;
				}
			}
		}
		List<AnnotationRule> owlRules = rulesFactory.getOwlRules();
		if (owlRules != null) {
			for (AnnotationRule annotationRule : owlRules) {
				if (id.equals(annotationRule.getRuleId())) {
					return annotationRule;
				}
			}
		}
		List<AnnotationRule> inferenceRules = rulesFactory.getInferenceRules();
		if (inferenceRules != null) {
			for (AnnotationRule annotationRule : inferenceRules) {
				if (id.equals(annotationRule.getRuleId())) {
					return annotationRule;
				}
			}
		}
		return null;
	}
	
	/**
	 * Validate the given {@link GafDocument}.
	 * 
	 * @param doc
	 * @return result
	 * @throws AnnotationRuleCheckException
	 */
	public AnnotationRulesEngineResult validateAnnotations(GafDocument doc) throws AnnotationRuleCheckException{
		
		List<AnnotationRule> annotationRules = rulesFactory.getGeneAnnotationRules();
		List<AnnotationRule> documentRules = rulesFactory.getGafDocumentRules();
		List<AnnotationRule> owlRules = rulesFactory.getOwlRules();
		List<AnnotationRule> inferenceRules = rulesFactory.getInferenceRules();
		if(annotationRules == null || annotationRules.isEmpty()){
 			throw new AnnotationRuleCheckException("Rules are not initialized. Please check the annotation_qc.xml file for errors");
 		}
		
		AnnotationRulesEngineResult result = new AnnotationRulesEngineResult();
		
		LOG.info("Start validation on annotation level with "+annotationRules.size()+" rules.");
		try{
			final List<GeneAnnotation> geneAnnotations = doc.getGeneAnnotations();
			int count = 0;
			final int length = geneAnnotations.size();
			result.setAnnotationCount(length);
			double size = (double) length; // store as double to avoid repeated conversion
			double last = 0.0d;
			for(GeneAnnotation annotation : geneAnnotations){
				for(AnnotationRule rule : annotationRules){
					if (!isGrandFatheredAnnotation(annotation, rule)) {
						result.addViolations(rule.getRuleViolations(annotation));
					}
				}
				count += 1;
				double current = count / size;
				if (Math.abs(current - last) > 0.05d) {
					NumberFormat percentInstance = DecimalFormat.getPercentInstance();
					LOG.info("Progress: "+percentInstance.format(current));
					last = current;
				}
			}
			if (documentRules != null && !documentRules.isEmpty()) {
				LOG.info("Start validation on document level with "+documentRules.size()+" rules.");
				for (AnnotationRule rule : documentRules) {
					result.addViolations(rule.getRuleViolations(doc));
				}
			}
			OWLGraphWrapper graph = rulesFactory.getGraph();
			final boolean hasOwlRules = owlRules != null && !owlRules.isEmpty();
			final boolean hasInferenceRules = createInferences && inferenceRules != null && !inferenceRules.isEmpty();
			boolean buildTranslatedGraph = graph != null && (hasOwlRules || hasInferenceRules);
			
			OWLGraphWrapper translatedGraph = null;
			if (buildTranslatedGraph) {
				LOG.info("Creating OWL represenation of annotations.");
				GAFOWLBridge bridge = new GAFOWLBridge(graph);
				bridge.setGenerateIndividuals(false);
				bridge.setBioentityMapping(BioentityMapping.NAMED_CLASS);
				OWLOntology translated = bridge.translate(doc);
				translatedGraph = new OWLGraphWrapper(translated);
			}
			
			if (hasOwlRules && translatedGraph != null) {
				LOG.info("Start validation using OWL representation with "+owlRules.size()+" rules.");
				for(AnnotationRule rule : owlRules) {
					result.addViolations(rule.getRuleViolations(translatedGraph));
				}
				LOG.info("Finished validation in OWL.");
			}
			
			if (hasInferenceRules && translatedGraph != null) {
				LOG.info("Start prediction/inference of annotations.");
				for(AnnotationRule rule : inferenceRules) {
					result.addInferences(rule.getPredictedAnnotations(doc, translatedGraph));
				}
				LOG.info("Finished prediction/inference of new annotations. Found: "+result.predictions.size());
			}
			
		}catch(Exception ex){
			LOG.error(ex.getMessage(), ex);
			throw new RuntimeException(ex);
		}
		LOG.info("Finished validation of annotations.");
		return result;
	}
	
	private boolean isGrandFatheredAnnotation(GeneAnnotation annotation, AnnotationRule rule) {
		if (rule.hasGrandFathering()) {
			String dateString = annotation.getLastUpdateDate();
			try {
				Date date = BasicChecksRule.dtFormat.get().parse(dateString);
				if (date.before(rule.getGrandFatheringDate())) {
					// is grand fathered
					return true;
				}
			} catch (ParseException e) {
				// ignore
			}
		}
		
		return false;
	}
	
	
	/**
	 * Results for a run of the {@link AnnotationRulesEngine}.
	 */
	public static class AnnotationRulesEngineResult {
		
		private int annotationCount;
		
		private final Map<ViolationType, Map<String, List<AnnotationRuleViolation>>> typedViolations;
		
		private final List<Prediction> predictions;
		
		AnnotationRulesEngineResult() {
			super();
			typedViolations = new HashMap<ViolationType, Map<String,List<AnnotationRuleViolation>>>();
			predictions = new ArrayList<Prediction>();
		}
		
		void addInferences(List<Prediction> predictions) {
			if (predictions != null) {
				this.predictions.addAll(predictions);
			}
		}
		
		void addViolations(Iterable<AnnotationRuleViolation> violations) {
			if (violations != null) {
				for (AnnotationRuleViolation violation : violations) {
					ViolationType type = violation.getType();
					Map<String, List<AnnotationRuleViolation>> map = typedViolations.get(type);
					if (map == null) {
						map = new HashMap<String, List<AnnotationRuleViolation>>();
						typedViolations.put(type, map);
					}
					final String ruleId = violation.getRuleId();
					List<AnnotationRuleViolation> list = map.get(ruleId);
					if(list == null){
						list = new ArrayList<AnnotationRuleViolation>();
						list = Collections.synchronizedList(list);
						map.put(ruleId, list);
					}
					list.add(violation);
				}
			}
		}
		
		/**
		 * @return true, if there is at least one violation with {@link ViolationType#Error}.
		 */
		public boolean hasErrors() {
			return hasType(ViolationType.Error);
		}
		
		/**
		 * @return true, if there is at least one violation with {@link ViolationType#Warning}.
		 */
		public boolean hasWarnings() {
			return hasType(ViolationType.Warning);
		}
		
		/**
		 * @return true, if there is at least one violation with {@link ViolationType#Recommendation}.
		 */
		public boolean hasRecommendations() {
			return hasType(ViolationType.Recommendation);
		}
		
		/**
		 * @param type
		 * @return true, if there is at least one violation with given type
		 */
		public boolean hasType(ViolationType type) {
			Map<String, List<AnnotationRuleViolation>> map = typedViolations.get(type);
			if (map != null) {
				return map.isEmpty() == false;
			}
			return false;
		}
		
		/**
		 * @return list of used {@link ViolationType} in this result.
		 */
		public List<ViolationType> getTypes() {
			List<ViolationType> types = new ArrayList<AnnotationRuleViolation.ViolationType>(typedViolations.keySet());
			Collections.sort(types, new Comparator<ViolationType>() {

				@Override
				public int compare(ViolationType o1, ViolationType o2) {
					int ord1 = o1.ordinal();
					int ord2 = o2.ordinal();
					return (ord1 < ord2 ? -1 : (ord1 == ord2 ? 0 : 1));
				}
			});
			return types;
		}
		
		/**
		 * @return number of errors
		 */
		public int getErrorCount() {
			return countViolations(ViolationType.Error);
		}
		
		/**
		 * @return number of warnings
		 */
		public int getWarningCount() {
			return countViolations(ViolationType.Warning);
		}

		/**
		 * @return number of recommendations
		 */
		public int getRecommendationCount() {
			return countViolations(ViolationType.Recommendation);
		}
		
		/**
		 * @param type
		 * @return number of violations of given type.
		 */
		public int countViolations(ViolationType type) {
			int count = 0;
			Map<String, List<AnnotationRuleViolation>> map = typedViolations.get(type);
			if (map != null) {
				for(Entry<String, List<AnnotationRuleViolation>> entry : map.entrySet()) {
					count += entry.getValue().size();
				}
			}
			return count;
		}
		
		/**
		 * @return true, if there is no {@link AnnotationRuleViolation} in this result.
		 */
		public boolean isEmpty() {
			return typedViolations.isEmpty();
		}
		
		/**
		 * @return a sorted list of all rule IDs for which there are reports.
		 */
		public List<String> getRules() {
			Set<String> ids = new HashSet<String>();
			for(ViolationType type : typedViolations.keySet()) {
				Map<String, List<AnnotationRuleViolation>> map = typedViolations.get(type);
				if (map != null) {
					ids.addAll(map.keySet());
				}
			}
			List<String> list = new ArrayList<String>(ids);
			Collections.sort(list);
			return list;
		}
		
		/**
		 * Retrieve all violations for a given {@link ViolationType}.
		 * 
		 * @param type
		 * @return map or null
		 */
		public Map<String, List<AnnotationRuleViolation>> getViolations(ViolationType type) {
			return typedViolations.get(type);
		}
		
		
		/**
		 * A simple tab delimited print-out of the result.
		 * 
		 * @param result
		 * @param engine
		 * @param writer
		 */
		public static void renderViolations(AnnotationRulesEngineResult result, AnnotationRulesEngine engine, PrintWriter writer) {
			renderEngineResult(result, engine, writer, null, null);
		}
		
		/**
		 * @return the annotationCount
		 */
		public int getAnnotationCount() {
			return annotationCount;
		}

		/**
		 * @param annotationCount the annotationCount to set
		 */
		public void setAnnotationCount(int annotationCount) {
			this.annotationCount = annotationCount;
		}

		/**
		 * A simple tab delimited print-out of the result and a simple summary.
		 * 
		 * @param result
		 * @param engine
		 * @param writer
		 * @param summaryWriter
		 * @param predictionWriter
		 */
		public static void renderEngineResult(AnnotationRulesEngineResult result, AnnotationRulesEngine engine, PrintWriter writer, PrintWriter summaryWriter, PrintStream predictionWriter) {
			if (summaryWriter != null) {
				summaryWriter.println("*GAF Validation Summary*");
			}
			List<ViolationType> types = result.getTypes();
			if (types.isEmpty() && result.predictions.isEmpty()) {
				writer.print("# No errors, warnings, or recommendations to report.");
				if (summaryWriter != null) {
					summaryWriter.println();
					summaryWriter.println("No errors, warnings, recommendations, inferences, or predictions to report.");
					summaryWriter.println();
				}
				return;
			}
			if (types.isEmpty()) {
				writer.print("# No errors, warnings, or recommendations to report.");
				if (summaryWriter != null) {
					summaryWriter.println();
					summaryWriter.println("No errors, warnings, or recommendations to report.");
					summaryWriter.println();
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
								printEscaped(annotationRow, writer, true);
							}
							writer.println();
						}
						writer.println("#------------");
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
				if (predictionWriter != null) {
					
					GafWriter.BufferedGafWriter bufferedGafWriter = new GafWriter.BufferedGafWriter();
					// write predictions in GAF format
					// write to buffer
					for (Prediction prediction : result.predictions) {
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
					List<String> comments = Arrays.asList(""," Generated predictions",""); 
					gafWriter.writeHeader(comments);
					// append sorted lines
					for (String line : lines) {
						predictionWriter.print(line);
					}
				}
			}
		}
	}
	
	private static void printEscaped(String s, PrintWriter writer, boolean useWhitespaces) {
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
	
	/**
	 * This exception is thrown when an exception occurs during the execution
	 * of annotation rules.
	 * 
	 * @author Shahid Manzoor
	 */
	public static class AnnotationRuleCheckException extends Exception {

		// generated
		private static final long serialVersionUID = 4692086624612112588L;

		public AnnotationRuleCheckException() {
		}

		public AnnotationRuleCheckException(String message) {
			super(message);
		}

		public AnnotationRuleCheckException(Throwable cause) {
			super(cause);
		}

		public AnnotationRuleCheckException(String message, Throwable cause) {
			super(message, cause);
		}

	}
	
}
