package owltools.gaf.rules.go;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import owltools.gaf.GeneAnnotation;
import owltools.gaf.eco.TraversingEcoMapper;
import owltools.gaf.rules.AbstractAnnotationRule;
import owltools.gaf.rules.AnnotationRuleViolation;
import owltools.gaf.rules.AnnotationRuleViolation.ViolationType;

/**
 * GO_AR:0000017
 */
public class GoIDAAnnotationRule extends AbstractAnnotationRule {
	
	/**
	 * The string to identify this class in the annotation_qc.xml and related factories.
	 * This is not supposed to be changed. 
	 */
	public static final String PERMANENT_JAVA_ID = "org.geneontology.rules.GO_AR_0000017";

	private static final String MESSAGE = "IDA annotations must not have a With/From entry";
	
	private final String message;
	private final ViolationType violationType;
	
	private final Set<String> evidences;
	
	public GoIDAAnnotationRule(TraversingEcoMapper eco) {
		this.message = MESSAGE;
		this.violationType = ViolationType.Warning;
		evidences = eco.getAllValidEvidenceIds("IDA", true);
	}
	
	@Override
	public Set<AnnotationRuleViolation> getRuleViolations(GeneAnnotation a) {
		String evidence = a.getShortEvidence();
		if (evidence != null && evidences.contains(evidence)) {
			Collection<String> withInfos = a.getWithInfos();
			if (withInfos != null && !withInfos.isEmpty()) {
				AnnotationRuleViolation violation = new AnnotationRuleViolation(getRuleId(), message, a, violationType);
				return Collections.singleton(violation);
			}
		}
		return null;
	}

}
