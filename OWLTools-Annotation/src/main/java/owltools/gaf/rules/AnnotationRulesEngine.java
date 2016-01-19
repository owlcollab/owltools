package owltools.gaf.rules;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
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
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.inference.Prediction;
import owltools.gaf.owl.GAFOWLBridge;
import owltools.gaf.owl.GAFOWLBridge.BioentityMapping;
import owltools.gaf.rules.AnnotationRuleViolation.ViolationType;
import owltools.gaf.rules.go.BasicChecksRule;
import owltools.graph.OWLGraphWrapper;

public class AnnotationRulesEngine {

	private static Logger LOG = Logger.getLogger(AnnotationRulesEngine.class);
	
	private final AnnotationRulesFactory rulesFactory;
	private final boolean createInferences;
	private final boolean useExperimentalInferences;
	
	public AnnotationRulesEngine(AnnotationRulesFactory rulesFactory, boolean createInferences, boolean useExperimental){
		this.rulesFactory = rulesFactory;
		this.createInferences = createInferences;
		this.useExperimentalInferences = useExperimental;
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
		List<AnnotationRule> experimentalInferenceRules = rulesFactory.getExperimentalInferenceRules();
		if (inferenceRules != null) {
			for (AnnotationRule annotationRule : experimentalInferenceRules) {
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
		List<AnnotationRule> inferenceRules = null;
		if (createInferences) {
			inferenceRules = rulesFactory.getInferenceRules();
		}
		List<AnnotationRule> experimentalInferenceRules = null;
		if (useExperimentalInferences) {
			experimentalInferenceRules = rulesFactory.getExperimentalInferenceRules();
		}
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
			final boolean hasExperimentalInferenceRules = useExperimentalInferences && experimentalInferenceRules != null && !experimentalInferenceRules.isEmpty();
			boolean buildTranslatedGraph = graph != null && (hasOwlRules || hasInferenceRules);
			if (graph != null) {
				result.setOntologyVersions(graph.getVersions());
			}
			
			OWLGraphWrapper translatedGraph = null;
			if (buildTranslatedGraph) {
				LOG.info("Creating OWL represenation of annotations.");
				// create empty ontology
				final OWLOntologyManager m = graph.getManager();
				
				OWLOntology translated = m.createOntology(IRI.generateDocumentIRI());
				
				// either try to import source or copy
				final OWLOntology source = graph.getSourceOntology();
				final OWLOntologyID sourceId = source.getOntologyID();
				if (sourceId != null && sourceId.getDefaultDocumentIRI() != null) {
					// use import
					OWLDataFactory f = m.getOWLDataFactory();
					m.applyChange(new AddImport(translated, f.getOWLImportsDeclaration(sourceId.getDefaultDocumentIRI().get())));
					
				}
				else {
					// copy top level ontology and imports
					m.addAxioms(translated, source.getAxioms());
					Set<OWLImportsDeclaration> importsDeclarations = source.getImportsDeclarations();
					for (OWLImportsDeclaration importsDeclaration : importsDeclarations) {
						m.applyChange(new AddImport(translated, importsDeclaration));
					}
				}
				
				GAFOWLBridge bridge = new GAFOWLBridge(graph, translated);
				bridge.setGenerateIndividuals(false);
				bridge.setBasicAboxMapping(false);
				bridge.setBioentityMapping(BioentityMapping.NAMED_CLASS);
				bridge.setSkipNotAnnotations(true);
				translatedGraph = new OWLGraphWrapper(translated);
			}
			
			if (hasOwlRules && translatedGraph != null) {
				LOG.info("Start validation using OWL representation with "+owlRules.size()+" rules.");
				for(AnnotationRule rule : owlRules) {
					result.addViolations(rule.getRuleViolations(doc, translatedGraph));
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
			
			if (hasExperimentalInferenceRules && translatedGraph != null) {
				LOG.info("Start experimental prediction/inference of annotations.");
				for(AnnotationRule rule : experimentalInferenceRules) {
					result.addExperimentalInferences(rule.getPredictedAnnotations(doc, translatedGraph));
				}
				LOG.info("Finished experimental prediction/inference of new annotations. Found: "+result.experimentalPredictions.size());
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
		
		final List<Prediction> predictions;
		
		final List<Prediction> experimentalPredictions;
		
		Map<String, String> ontologyVersions = null;
		
		public AnnotationRulesEngineResult() {
			super();
			typedViolations = new HashMap<ViolationType, Map<String,List<AnnotationRuleViolation>>>();
			predictions = new ArrayList<Prediction>();
			experimentalPredictions = new ArrayList<Prediction>();
		}
		
		public void addInferences(List<Prediction> predictions) {
			if (predictions != null) {
				this.predictions.addAll(predictions);
			}
		}
		
		public void addExperimentalInferences(List<Prediction> predictions) {
			if (predictions != null) {
				this.experimentalPredictions.addAll(predictions);
			}
		}
		
		public void addViolations(Iterable<AnnotationRuleViolation> violations) {
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
		
		void setOntologyVersions(Map<String, String> ontologyVersions) {
			this.ontologyVersions = ontologyVersions;
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
