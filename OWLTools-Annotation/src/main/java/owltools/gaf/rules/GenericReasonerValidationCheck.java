package owltools.gaf.rules;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.elk.owlapi.ElkReasoner;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import owltools.gaf.GeneAnnotation;
import owltools.gaf.rules.AnnotationRuleViolation.ViolationType;
import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;

/**
 * This check using the {@link ElkReasoner} will not detect unsatisfiable
 * classes, which result from inverse_of object properties. ELK does not support
 * this at the moment.
 */
public class GenericReasonerValidationCheck extends AbstractAnnotationRule {
	
	/**
	 * The string to identify this class in the annotation_qc.xml and related factories.
	 * This is not supposed to be changed. 
	 */
	public static final String PERMANENT_JAVA_ID = "org.geneontology.gold.rules.GenericReasonerValidationCheck";
	
	private static final Logger logger = Logger.getLogger(GenericReasonerValidationCheck.class);

	private final OWLReasonerFactory factory = new ElkReasonerFactory();

	@Override
	public Set<AnnotationRuleViolation> getRuleViolations(GeneAnnotation a) {
		// Do nothing silently ignore this call.
		return Collections.emptySet();
	}

	@Override
	public boolean isAnnotationLevel() {
		return false;
	}
	
	@Override
	public boolean isOwlDocumentLevel() {
		return true;
	}

	@Override
	public Set<AnnotationRuleViolation> getRuleViolations(OWLGraphWrapper graph) {
		logger.info("Check generic logic violations for gaf");
		
		if (logger.isDebugEnabled()) {
			logger.debug("Create reasoner");
		}
		OWLReasoner reasoner = factory.createReasoner(graph.getSourceOntology());
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Check consistency");
			}
			boolean consistent = reasoner.isConsistent();
			if (!consistent) {
				return Collections.singleton(new AnnotationRuleViolation(getRuleId(), "Logic inconsistency in combined annotations and ontology detected."));
			}

			if (logger.isDebugEnabled()) {
				logger.debug("Start - Check for unsatisfiable classes");
			}
			Node<OWLClass> unsatisfiableClasses = reasoner.getUnsatisfiableClasses();
			if (logger.isDebugEnabled()) {
				logger.debug("Finished - Check for unsatisfiable classes");
			}
			if (unsatisfiableClasses != null) {
				OWLPrettyPrinter pp = new OWLPrettyPrinter(graph);
				Set<OWLClass> entities = unsatisfiableClasses.getEntities();
				Set<AnnotationRuleViolation> violations = new HashSet<AnnotationRuleViolation>();
				for (OWLClass c : entities) {
					if (c.isBottomEntity() || c.isTopEntity()) {
						continue;
					}
					violations.add(new AnnotationRuleViolation(getRuleId(), "unsatisfiable class: "+pp.render(c), (GeneAnnotation) null, ViolationType.Warning));
				}
				if (!violations.isEmpty()) {
					return violations;
				}
			}
			return Collections.emptySet();
		}
		finally {
			reasoner.dispose();
		}
	}

	
}
