package owltools.gaf.rules.go;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import owltools.gaf.GeneAnnotation;
import owltools.gaf.eco.TraversingEcoMapper;
import owltools.gaf.rules.AbstractAnnotationRule;
import owltools.gaf.rules.AnnotationRuleViolation;
import owltools.gaf.rules.AnnotationRuleViolation.ViolationType;

/**
 * GO_AR:0000003
 */
public class GoBindingCheckWithFieldRule extends AbstractAnnotationRule {

	/**
	 * The string to identify this class in the annotation_qc.xml and related factories.
	 * This is not supposed to be changed. 
	 */
	public static final String PERMANENT_JAVA_ID = "org.geneontology.rules.GO_AR_0000003";
	
	// TODO retrieve message from title in the annotation_qc.xml
	private static final String MESSAGE = "Annotations to 'binding' (GO:0005488) and 'protein binding' (GO:0005515) should be made with an IPI evidence code and the interactor in the 'with' field";
	private static final Set<String> entities = createEntities();
	private final Set<String> evidences;
	
	private static Set<String> createEntities() {
		// binding
		// protein binding
		Set<String> set = new HashSet<String>(Arrays.asList("GO:0005488", "GO:0005515"));
		return Collections.unmodifiableSet(set);
	}

	public GoBindingCheckWithFieldRule(TraversingEcoMapper eco) {
		super();
		Set<String> codes = new HashSet<String>();
		codes.add("IPI");
		codes.add("IEA");
		evidences = eco.getAllValidEvidenceIds(codes, true);
	}
	
	@Override
	public Set<AnnotationRuleViolation> getRuleViolations(GeneAnnotation a) {
		String cls = a.getCls();
		if (cls != null && entities.contains(cls)) {
			String evidence = a.getShortEvidence();
			if (evidences.contains(evidence) == false) {
				AnnotationRuleViolation violation = new AnnotationRuleViolation(getRuleId(), MESSAGE, a, ViolationType.Warning);
				return Collections.singleton(violation);
			}
			// check with field
			String withExpression = a.getWithExpression();
			if (withExpression == null || withExpression.isEmpty()) {
				AnnotationRuleViolation violation = new AnnotationRuleViolation(getRuleId(), MESSAGE, a, ViolationType.Warning);
				return Collections.singleton(violation);
			}
			
		}
		return null;
	}

}
