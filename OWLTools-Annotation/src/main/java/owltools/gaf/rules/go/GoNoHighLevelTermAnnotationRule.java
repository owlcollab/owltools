package owltools.gaf.rules.go;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;

import owltools.gaf.GeneAnnotation;
import owltools.gaf.eco.TraversingEcoMapper;
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
	
	private final OWLGraphWrapper go;
	
	private final String gocheck_do_not_annotate = "gocheck_do_not_annotate";
	private final String gocheck_do_not_manually_annotate = "gocheck_do_not_manually_annotate";
	
	private final Set<String> ieaEvidenceCodes;
	
	/**
	 * @param go
	 * @param eco
	 */
	public GoNoHighLevelTermAnnotationRule(OWLGraphWrapper go, TraversingEcoMapper eco) {
		super();
		this.go = go;
		ieaEvidenceCodes = eco.getAllValidEvidenceIds("IEA", true);
	}

	@Override
	public Set<AnnotationRuleViolation> getRuleViolations(GeneAnnotation a) {
		String cls = a.getCls();
		if (cls != null) {
			OWLClass owlClass = go.getOWLClassByIdentifierNoAltIds(cls);
			if (owlClass != null) {
				List<String> subsets = go.getSubsets(owlClass);
				if (subsets != null && !subsets.isEmpty()) {
					for (String subset : subsets) {
						if (gocheck_do_not_annotate.equals(subset)) {
							return createViolation(a, owlClass, subset);
						}
						else if (gocheck_do_not_manually_annotate.equals(subset)) {
							String evidence = a.getShortEvidence();
							if (evidence != null && ieaEvidenceCodes.contains(evidence) == false) {
								return createViolation(a, owlClass, subset);
							}
						}
					}
				}
			}
		}
		return null;
	}

	private Set<AnnotationRuleViolation> createViolation(GeneAnnotation a, OWLClass owlClass, String subset) {
		StringBuilder sb = new StringBuilder();
		sb.append("Do not annotate to: ");
		sb.append(go.getIdentifier(owlClass));
		String label = go.getLabel(owlClass);
		if (label != null) {
			sb.append(" '");
			sb.append(label);
			sb.append("'");
		}
		sb.append(" The term is considered to high level, as marked via the subset tag: ");
		sb.append(subset);
		return Collections.singleton(new AnnotationRuleViolation(getRuleId(), sb.toString() , a, level));
	}

}
