package owltools.gaf.rules;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.rules.AnnotationRuleViolation.ViolationType;
import owltools.gaf.rules.go.GoAnnotationRulesFactoryImpl;

public class AnnotationRulesEngine {

	private static Logger LOG = Logger.getLogger(AnnotationRulesEngine.class);
	
	private final AnnotationRulesFactory rulesFactory;
	
	public AnnotationRulesEngine(){
		this(new GoAnnotationRulesFactoryImpl());
	}
	
	
	public AnnotationRulesEngine(AnnotationRulesFactory rulesFactory){
		this.rulesFactory = rulesFactory;
		rulesFactory.init();
	}
	
	
	public AnnotationRulesEngineResult validateAnnotations(GafDocument doc) throws AnnotationRuleCheckException{
		
		List<AnnotationRule> rules = rulesFactory.getGeneAnnotationRules();
		List<AnnotationRule> gafRules = rulesFactory.getGafRules();
		if(rules == null || rules.isEmpty()){
			throw new AnnotationRuleCheckException("Rules are not initialized. Please check the annotation_qc.xml file for errors");
		}
		
		AnnotationRulesEngineResult result = new AnnotationRulesEngineResult();
		final int ruleCount = rules.size() + (gafRules != null ? gafRules.size() : 0);
		LOG.info("Start validation of annotations with "+ruleCount+" rules.");
		try{
			for(GeneAnnotation annotation : doc.getGeneAnnotations()){
				for(AnnotationRule rule : rules){
					result.addViolations(rule.getRuleViolations(annotation));
				}
			}
			if (gafRules != null && !gafRules.isEmpty()) {
				for (AnnotationRule rule : gafRules) {
					result.addViolations(rule.getRuleViolations(doc));
				}
			}
			
		}catch(Exception ex){
			LOG.error(ex.getMessage(), ex);
			throw new RuntimeException(ex);
		}
		LOG.info("Finished validation of annotations.");
		return result;
	}
	
	
	/**
	 * Results for a run of the {@link AnnotationRulesEngine}.
	 */
	public static class AnnotationRulesEngineResult {
		
		private final Map<ViolationType, Map<String, List<AnnotationRuleViolation>>> typedViolations;
		
		AnnotationRulesEngineResult() {
			super();
			typedViolations = new HashMap<ViolationType, Map<String,List<AnnotationRuleViolation>>>();
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
		 * @param writer
		 */
		public static void renderViolations(AnnotationRulesEngineResult result, PrintWriter writer) {
			List<ViolationType> types = result.getTypes();
			if (types.isEmpty()) {
				// do nothing
				return;
			}
			writer.println("#Line\tRuleID\tViolationType\tMessage");
			writer.println("#------------");
			for(ViolationType type : types) {
				Map<String, List<AnnotationRuleViolation>> violations = result.getViolations(type);
				List<String> ruleIds = new ArrayList<String>(violations.keySet());
				Collections.sort(ruleIds);
				for (String ruleId : ruleIds) {
					List<AnnotationRuleViolation> violationList = violations.get(ruleId);
					writer.print("# ");
					writer.print(ruleId);
					writer.print(' ');
					writer.print(type.name());
					writer.print("  count: ");
					writer.print(violationList.size());
					writer.println();
					for (AnnotationRuleViolation violation : violationList) {
						writer.print(violation.getLineNumber());
						writer.print('\t');
						writer.print(ruleId);
						writer.print('\t');
						writer.print(type.name());
						writer.print('\t');
						final String message = violation.getMessage();
						for (int i = 0; i < message.length(); i++) {
							char c = message.charAt(i);
							if (c == '\t') {
								writer.print("\\t"); // escape tabs
							}
							if (c == '\n') {
								writer.print("\\n"); // escape new lines
							}
							else if (Character.isWhitespace(c)) {
								// normalize other white spaces
								writer.print(' ');
							}
							else {
								writer.print(c);
							}
							
						}
						writer.println();
					}
					writer.println("#------------");
				}
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
