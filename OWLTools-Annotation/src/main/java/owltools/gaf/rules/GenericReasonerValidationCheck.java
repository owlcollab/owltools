package owltools.gaf.rules;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.owl.GAFOWLBridge;
import owltools.gaf.rules.AnnotationRuleViolation.ViolationType;
import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;

public class GenericReasonerValidationCheck extends AbstractAnnotationRule {
	
	/**
	 * The string to identify this class in the annotation_qc.xml and related factories.
	 * This is not supposed to be changed. 
	 */
	public static final String PERMANENT_JAVA_ID = "org.geneontology.gold.rules.GenericReasonerValidationCheck";
	
	private static final Logger logger = Logger.getLogger(GenericReasonerValidationCheck.class);

	private final OWLGraphWrapper graph;
	private final OWLReasonerFactory factory = new ElkReasonerFactory();
	private final OWLPrettyPrinter pp;

	public GenericReasonerValidationCheck(OWLGraphWrapper graph) {
		super();
		this.graph = graph;
		pp = new OWLPrettyPrinter(graph);
	}

	@Override
	public Set<AnnotationRuleViolation> getRuleViolations(GeneAnnotation a) {
		// Do nothing silently ignore this call.
		return Collections.emptySet();
	}

	@Override
	public boolean isDocumentLevel() {
		return true;
	}

	@Override
	public Set<AnnotationRuleViolation> getRuleViolations(GafDocument gafDoc) {
		logger.info("Check generic logic violations for gaf");
		if (logger.isDebugEnabled()) {
			logger.debug("Converting gafDoc to owl");
		}
		GAFOWLBridge bridge = new GAFOWLBridge(graph);
		bridge.setGenerateIndividuals(false);
		OWLOntology translated = bridge.translate(gafDoc);
		
		if (logger.isDebugEnabled()) {
			logger.debug("Create reasoner");
		}
		OWLReasoner reasoner = factory.createReasoner(translated);
		
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
			Set<OWLClass> entities = unsatisfiableClasses.getEntities();
			Set<AnnotationRuleViolation> violations = new HashSet<AnnotationRuleViolation>();
			for (OWLClass c : entities) {
				if (c.isBottomEntity() || c.isTopEntity()) {
					continue;
				}
				violations.add(new AnnotationRuleViolation(getRuleId(), "unsatifiable class: "+pp.render(c), (GeneAnnotation) null, ViolationType.Warning));
			}
			if (!violations.isEmpty()) {
				return violations;
			}
		}
		
			
		return Collections.emptySet();
	}

	
}
