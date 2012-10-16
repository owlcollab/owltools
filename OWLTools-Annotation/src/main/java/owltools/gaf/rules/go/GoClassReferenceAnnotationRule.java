package owltools.gaf.rules.go;

import java.util.HashSet;
import java.util.Set;

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
	
	/**
	 * The string to identify this class in the annotation_qc.xml and related factories.
	 * This is not supposed to be changed. 
	 */
	public static final String PERMANENT_JAVA_ID = "org.geneontology.gold.rules.GoClassReferenceAnnotationRule";

	private final OWLGraphWrapper graph;

	public GoClassReferenceAnnotationRule(OWLGraphWrapper wrapper){
		this.graph = wrapper; 
	}

	@Override
	public Set<AnnotationRuleViolation> getRuleViolations(GeneAnnotation a) {

		HashSet<AnnotationRuleViolation> set = new HashSet<AnnotationRuleViolation>();
		String id = a.getCls();
		OWLClass owlClass = graph.getOWLClassByIdentifier(id);

		if (owlClass == null) {
			set.add(new AnnotationRuleViolation(getRuleId(),
					"The id '"+id+"' in the annotation is a dangling reference", a));
		}
		else {
			boolean isObsolete = graph.isObsolete(owlClass);
			if (isObsolete) {
				set.add(new AnnotationRuleViolation(getRuleId(),
						"The id '"+id+"' in the annotation is an obsolete class", a));
			}
		}
		return set;
	}

}
