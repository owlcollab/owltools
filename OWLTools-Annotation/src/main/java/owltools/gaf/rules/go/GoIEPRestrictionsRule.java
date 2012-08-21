package owltools.gaf.rules.go;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import owltools.gaf.EcoTools;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.rules.AbstractAnnotationRule;
import owltools.gaf.rules.AnnotationRuleViolation;
import owltools.gaf.rules.AnnotationRuleViolation.ViolationType;
import owltools.graph.OWLGraphWrapper;

/**
 * GO_AR:0000006
 */
public class GoIEPRestrictionsRule extends AbstractAnnotationRule {
	
	/**
	 * The string to identify this class in the annotation_qc.xml and related factories.
	 * This is not supposed to be changed. 
	 */
	public static final String PERMANENT_JAVA_ID = "org.geneontology.rules.GO_AR_0000006";

	private static final String MESSAGE = "IEP usage is restricted to terms from the Biological Process ontology";
	
	private final String message;
	private final ViolationType violationType;
	
	private final Set<String> evidences;
	private final Set<String> classSubSet;

	public GoIEPRestrictionsRule(OWLGraphWrapper graph, OWLGraphWrapper eco) {
		this.message = MESSAGE;
		this.violationType = ViolationType.Warning;
		
		Set<OWLClass> ecoClasses = EcoTools.getClassesForGoCodes(eco, "IEP");
		evidences = EcoTools.getCodes(ecoClasses, eco, true);
		
		classSubSet = new HashSet<String>();
		
		OWLClass rootClass = graph.getOWLClassByIdentifier("GO:0008150");
		OWLReasonerFactory factory = new ElkReasonerFactory();
		OWLReasoner reasoner = factory.createReasoner(graph.getSourceOntology());
		try {
			NodeSet<OWLClass> nodeSet = reasoner.getSubClasses(rootClass, false);
			for(OWLClass cls : nodeSet.getFlattened()) {
				if (cls.isBottomEntity() || cls.isTopEntity()) {
					continue;
				}
				String oboId = graph.getIdentifier(cls);
				if (oboId != null) {
					classSubSet.add(oboId);
				}
			}
		}
		finally {
			reasoner.dispose();
		}
	}
	
	@Override
	public Set<AnnotationRuleViolation> getRuleViolations(GeneAnnotation a) {
		String evidenceCls = a.getEvidenceCls();
		if (evidenceCls != null && evidences.contains(evidenceCls)) {
			String cls = a.getCls();
			if (cls != null) {
				if (!classSubSet.contains(cls)) {
					AnnotationRuleViolation violation = new AnnotationRuleViolation(getRuleId(), message, a, violationType);
					return Collections.singleton(violation);
				}
			}
		}
		return null;
	}
}
