package owltools.gaf.rules.go;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;

import owltools.gaf.GeneAnnotation;
import owltools.gaf.rules.AbstractAnnotationRule;
import owltools.gaf.rules.AnnotationRuleViolation;
import owltools.gaf.rules.AnnotationRuleViolation.ViolationType;
import owltools.graph.OWLGraphWrapper;

/**
 * GO_AR:0000008
 */
public class GoNoHighLevelTermAnnotationRule extends AbstractAnnotationRule {
	
	/**
	 * The string to identify this class in the annotation_qc.xml and related factories.
	 * This is not supposed to be changed. 
	 */
	public static final String PERMANENT_JAVA_ID = "org.geneontology.rules.GO_AR_0000008";
	
	private static final ViolationType level = ViolationType.Warning;
	
	private final String high_level_subset = "high_level_annotation_qc";
	private final OWLGraphWrapper go;

	/**
	 * @param go
	 */
	public GoNoHighLevelTermAnnotationRule(OWLGraphWrapper go) {
		super();
		this.go = go;
	}

	@Override
	public Set<AnnotationRuleViolation> getRuleViolations(GeneAnnotation a) {
		String cls = a.getCls();
		if (cls != null) {
			OWLClass owlClass = go.getOWLClassByIdentifier(cls);
			if (owlClass != null) {
				List<String> subsets = go.getSubsets(owlClass);
				if (subsets != null && !subsets.isEmpty()) {
					for (String subset : subsets) {
						if (high_level_subset.equals(subset)) {
							String message = "Do not annotate to: "+cls+" The term is considered to high level, as marked via the subset tag: "+high_level_subset;
							return Collections.singleton(new AnnotationRuleViolation(getRuleId(), message , a, level));
						}
					}
				}
			}
		}
		return null;
	}

}
