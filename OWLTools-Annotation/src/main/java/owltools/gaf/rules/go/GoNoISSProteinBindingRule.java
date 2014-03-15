package owltools.gaf.rules.go;

import java.util.Collections;
import java.util.Set;

import owltools.gaf.GeneAnnotation;
import owltools.gaf.eco.TraversingEcoMapper;
import owltools.gaf.rules.AbstractAnnotationRule;
import owltools.gaf.rules.AnnotationRuleViolation;
import owltools.gaf.rules.AnnotationRuleViolation.ViolationType;

/**
 * GO_AR:0000005
 */
public class GoNoISSProteinBindingRule extends AbstractAnnotationRule {
	
	/**
	 * The string to identify this class in the annotation_qc.xml and related factories.
	 * This is not supposed to be changed. 
	 */
	public static final String PERMANENT_JAVA_ID = "org.geneontology.rules.GO_AR_0000005";

	private static final String MESSAGE = "No ISS or ISS-related annotations to 'protein binding ; GO:0005515'";
	
	private final Set<String> evidences;
	private final String message;
	private final ViolationType violationType;
	
	public GoNoISSProteinBindingRule(TraversingEcoMapper eco, ViolationType violationType) {
		super();
		this.message = MESSAGE;
		this.violationType = violationType;
		evidences = eco.getAllValidEvidenceIds("ISS", true);
	}
	
	public GoNoISSProteinBindingRule(TraversingEcoMapper eco) {
		this(eco, ViolationType.Error);
	}
	
	@Override
	public Set<AnnotationRuleViolation> getRuleViolations(GeneAnnotation a) {
		String cls = a.getCls();
		if (cls != null && "GO:0005515".equals(cls)) {
			String evidence = a.getShortEvidence();
			if (evidences.contains(evidence)) {
				AnnotationRuleViolation violation = new AnnotationRuleViolation(getRuleId(), message, a, violationType);
				return Collections.singleton(violation);
			}
			
		}
		return null;
	}

}
