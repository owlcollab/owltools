package owltools.gaf.rules.go;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;

import owltools.gaf.EcoTools;
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

	private final String high_level_subset_manual = "high_level_annotation_manual_qc";
	private final String high_level_subset_iea = "high_level_annotation_iea_qc";
	
	private final Set<String> ieaEvidenceCodes;
	
	/**
	 * @param go
	 * @param eco
	 */
	public GoNoHighLevelTermAnnotationRule(OWLGraphWrapper go, OWLGraphWrapper eco) {
		super();
		this.go = go;
		Set<OWLClass> ieaClasses = EcoTools.getClassesForGoCodes(eco, "IEA");
		ieaEvidenceCodes = EcoTools.getCodes(ieaClasses, eco, true);
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
							StringBuilder sb = new StringBuilder();
							sb.append("Do not annotate to: ");
							sb.append(cls);
							String label = go.getLabel(owlClass);
							if (label != null) {
								sb.append(" '");
								sb.append(label);
								sb.append("'");
							}
							sb.append(" The term is considered to high level, as marked via the subset tag: ");
							sb.append(high_level_subset);
							return Collections.singleton(new AnnotationRuleViolation(getRuleId(), sb.toString() , a, level));
						}
					}
				}
			}
		}
		return null;
	}

	
	Set<AnnotationRuleViolation> getRuleViolationsNew(GeneAnnotation a) {
		String cls = a.getCls();
		if (cls != null) {
			OWLClass owlClass = go.getOWLClassByIdentifier(cls);
			if (owlClass != null) {
				String evidenceCode = a.getEvidenceCls();
				List<String> subsets = go.getSubsets(owlClass);
				if (subsets != null && !subsets.isEmpty()) {
					if (evidenceCode != null) {
						if (isIEA(evidenceCode)) {
							if (isAntiSlimIEA(subsets)) {
								return createViolation(a, owlClass, high_level_subset_iea, " for an inferred electronic annotation (IEA)");
							}
						}
						else {
							if (isAntiSlimManual(subsets)) {
								return createViolation(a, owlClass, high_level_subset_manual, " for manual anotation");
							}
						}
					}
				}
			}
		}
		return null;
	}
	
	private Set<AnnotationRuleViolation> createViolation(GeneAnnotation a, OWLClass owlClass, String subset, String infix) {
		StringBuilder sb = new StringBuilder();
		sb.append("Do not annotate to: ");
		sb.append(go.getIdentifier(owlClass));
		String label = go.getLabel(owlClass);
		if (label != null) {
			sb.append(" '");
			sb.append(label);
			sb.append("'");
		}
		sb.append(" The term is considered to high level");
		sb.append(infix);
		sb.append(", as marked via the subset tag: ");
		sb.append(subset);
		return Collections.singleton(new AnnotationRuleViolation(getRuleId(), sb.toString() , a, level));
	}

	private boolean isIEA(String code) {
		return ieaEvidenceCodes.contains(code);
	}
	
	private boolean isAntiSlimManual(Collection<String> subsets) {
		if (subsets != null) {
			return subsets.contains(high_level_subset_manual);
		}
		return false;
	}
	
	private boolean isAntiSlimIEA(Collection<String> subsets) {
		if (subsets != null) {
			return subsets.contains(high_level_subset_iea);
		}
		return false;
	}
}
