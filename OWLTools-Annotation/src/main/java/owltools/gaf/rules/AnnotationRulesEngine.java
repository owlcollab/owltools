package owltools.gaf.rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.rules.go.GoAnnotationRulesFactoryImpl;

public class AnnotationRulesEngine {

	private static Logger LOG = Logger.getLogger(AnnotationRulesEngine.class);
	
	private final AnnotationRulesFactory rulesFactory;
	
	private Map<String, List<AnnotationRuleViolation>> annotationRuleViolations;
	private Map<String, Integer> annotationRuleViolationsCounter;
	
	private int annotationVoilationLimit;

	
	public AnnotationRulesEngine(){
		this(-1, new GoAnnotationRulesFactoryImpl());
	}
	
	
	public AnnotationRulesEngine(int annotationVoilationLimit, AnnotationRulesFactory rulesFactory){
		this.rulesFactory = rulesFactory;
		this.annotationVoilationLimit = annotationVoilationLimit;
		annotationRuleViolations = new Hashtable<String, List<AnnotationRuleViolation>>();
		annotationRuleViolationsCounter = new Hashtable<String, Integer>();
		rulesFactory.init();
	}
	
	
	public Map<String, List<AnnotationRuleViolation>> validateAnnotations(GafDocument doc) throws AnnotationRuleCheckException{
		
		List<AnnotationRule> rules = rulesFactory.getRules();
		if(rules == null || rules.isEmpty()){
			throw new AnnotationRuleCheckException("Rules are not initialized. Please check the annotation_qc.xml file for errors");
		}
		
		try{
			Set<String> rulesNotToRun = new HashSet<String>();
			for(GeneAnnotation annotation : doc.getGeneAnnotations()){
				for(AnnotationRule rule : rules){
					
					if(rulesNotToRun.contains(rule.getRuleId()))
						continue;
					
					for(AnnotationRuleViolation av: rule.getRuleViolations(annotation)){
						List<AnnotationRuleViolation> list= annotationRuleViolations.get(av.getRuleId());
						Integer counter = annotationRuleViolationsCounter.get(av.getRuleId());
						if(list == null){
							list = new ArrayList<AnnotationRuleViolation>();
							list = Collections.synchronizedList(list);
							annotationRuleViolations.put(av.getRuleId(), list);
							counter = 0;
						}
						
						if(annotationVoilationLimit != -1 && counter >= annotationVoilationLimit) {
							rulesNotToRun.add(rule.getRuleId());
						}
						else {	
							list.add(av);
						}
						
						annotationRuleViolationsCounter.put(av.getRuleId(), counter+1);
					}
					
				}
			}
		}catch(Exception ex){
			LOG.error(ex.getMessage(), ex);
			throw new RuntimeException(ex);
		}
		return annotationRuleViolations;
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
