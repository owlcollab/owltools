package owltools.gaf.rules.go;

import java.text.ParseException;
import java.util.Collections;
import java.util.Set;

import org.apache.log4j.Logger;

import owltools.gaf.GeneAnnotation;
import owltools.gaf.eco.TraversingEcoMapper;
import owltools.gaf.rules.AbstractAnnotationRule;
import owltools.gaf.rules.AnnotationRuleViolation;
import owltools.gaf.rules.AnnotationRuleViolation.ViolationType;

/**
 *  GO_AR:0000016
 */
public class GoICAnnotationRule extends AbstractAnnotationRule {
	
	/**
	 * The string to identify this class in the annotation_qc.xml and related factories.
	 * This is not supposed to be changed. 
	 */
	public static final String PERMANENT_JAVA_ID = "org.geneontology.rules.GO_AR_0000016";

	private static final String MESSAGE = "IC annotations require a With/From GO ID";
	
	private final String message;
	private final ViolationType violationType;
	
	private final Set<String> evidences;
	
	public GoICAnnotationRule(TraversingEcoMapper eco) {
		this.message = MESSAGE;
		this.violationType = ViolationType.Warning;
		evidences = eco.getAllValidEvidenceIds("IC", true);
		
		// TODO remove hard coded date once the date is available from the annotation_qc.xml
		final String grandFatheringDateString = "20120101";
		try {
			setGrandFatheringDate(BasicChecksRule.dtFormat.get().parse(grandFatheringDateString));
		} catch (ParseException e) {
			Logger.getLogger(GoICAnnotationRule.class).error("Could not grand fathering date: "+grandFatheringDateString, e);
		}
	}
	
	@Override
	public Set<AnnotationRuleViolation> getRuleViolations(GeneAnnotation a) {
		String evidenceCls = a.getEvidenceCls();
		if (evidenceCls != null && evidences.contains(evidenceCls)) {
			String expression = a.getWithExpression();
			if (expression == null || expression.isEmpty()) {
				AnnotationRuleViolation violation = new AnnotationRuleViolation(getRuleId(), message, a, violationType);
				return Collections.singleton(violation);
			}
		}
		return null;
	}

}
