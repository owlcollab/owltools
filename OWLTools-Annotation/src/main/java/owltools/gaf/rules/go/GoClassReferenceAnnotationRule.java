package owltools.gaf.rules.go;

import java.util.HashSet;
import java.util.Set;

import org.obolibrary.obo2owl.Obo2OWLConstants;
import org.semanticweb.owlapi.model.OWLClass;

import owltools.gaf.GeneAnnotation;
import owltools.gaf.rules.AbstractAnnotationRule;
import owltools.gaf.rules.AnnotationRuleViolation;
import owltools.graph.OWLGraphWrapper;

/**
 * Checks to see if an annotation uses a class that is not in the current ontology, and that
 * the class has not been obsoleted
 */
public class GoClassReferenceAnnotationRule extends AbstractAnnotationRule {

	private final OWLGraphWrapper graph;

	public GoClassReferenceAnnotationRule(OWLGraphWrapper wrapper){
		this.graph = wrapper; 
	}

	@Override
	public Set<AnnotationRuleViolation> getRuleViolations(GeneAnnotation a) {

		HashSet<AnnotationRuleViolation> set = new HashSet<AnnotationRuleViolation>();
		String cls = a.getCls().replace(":", "_");

		OWLClass owlClass = graph.getOWLClass(Obo2OWLConstants.DEFAULT_IRI_PREFIX + cls);

		if (owlClass == null) {
			AnnotationRuleViolation v = new AnnotationRuleViolation(
					"The GO id in the annotation is a dangling reference", a);
			v.setRuleId(getRuleId());
			set.add(v);
		}

		boolean isObsolete = graph.getIsObsolete(owlClass);

		if (isObsolete) {
			AnnotationRuleViolation arv = new AnnotationRuleViolation(
					"The GO id in the annotation is a obsolete class", a);
			arv.setRuleId(getRuleId());
			
			// arv.setSuggestedReplacements(suggestedReplacements)
			set.add(arv);
		}

		return set;
	}

}
