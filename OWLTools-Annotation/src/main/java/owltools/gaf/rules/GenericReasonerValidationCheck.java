package owltools.gaf.rules;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.elk.owlapi.ElkReasoner;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.rules.AnnotationRuleViolation.ViolationType;
import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;

import com.clarkparsia.owlapi.explanation.DefaultExplanationGenerator;
import com.clarkparsia.owlapi.explanation.ExplanationGenerator;

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
	
	public static boolean CREATE_EXPLANATIONS = false;
	
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
	public Set<AnnotationRuleViolation> getRuleViolations(GafDocument gafDoc, OWLGraphWrapper graph) {
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
				StringBuilder msgBuilder = new StringBuilder();
				msgBuilder.append("Logic inconsistency in combined annotations and ontology detected.");
				
				/*
				
				// try to find an explanation
				ExplanationGenerator explanationGen = new DefaultExplanationGenerator(graph.getManager(), factory, graph.getSourceOntology(), null);
				// inconsistent === OWLThing is unsatisfiable
				final OWLClass owlThing = graph.getDataFactory().getOWLThing();
				Set<OWLAxiom> explanation = explanationGen.getExplanation(owlThing);
				OWLPrettyPrinter pp = new OWLPrettyPrinter(graph);
				appendExplanation(explanation, msgBuilder, pp);
				
				*/
				return Collections.singleton(new AnnotationRuleViolation(getRuleId(), msgBuilder.toString()));
			}

			if (logger.isDebugEnabled()) {
				logger.debug("Start - Check for unsatisfiable classes");
			}
			Node<OWLClass> unsatisfiableClasses = reasoner.getUnsatisfiableClasses();
			if (logger.isDebugEnabled()) {
				logger.debug("Finished - Check for unsatisfiable classes");
			}
			if (unsatisfiableClasses != null) {
				ExplanationGenerator explanationGen = null;
				if (CREATE_EXPLANATIONS) {
					explanationGen = new DefaultExplanationGenerator(graph.getManager(), factory, graph.getSourceOntology(), reasoner, null);
				}
				OWLPrettyPrinter pp = new OWLPrettyPrinter(graph);
				Set<OWLClass> entities = unsatisfiableClasses.getEntitiesMinusBottom();
				logger.info("Found unsatisfiable classes, count: "+entities.size()+" list: "+entities);
				Set<AnnotationRuleViolation> violations = new HashSet<AnnotationRuleViolation>();
				for (OWLClass c : entities) {
					if (c.isBuiltIn()) {
						continue;
					}
					StringBuilder msgBuilder = new StringBuilder();
					msgBuilder.append("unsatisfiable class: ").append(pp.render(c));
					if (CREATE_EXPLANATIONS) {
						logger.info("Finding explanations for unsatisfiable class: "+pp.render(c));
						Set<OWLAxiom> explanation = explanationGen.getExplanation(c);
						appendExplanation(explanation, msgBuilder, pp);
					}
					violations.add(new AnnotationRuleViolation(getRuleId(), msgBuilder.toString(), (GeneAnnotation) null, ViolationType.Warning));
					logger.info("Finished finding explanation");
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

	/**
	 * @param explanation
	 * @param msgBuilder
	 * @param pp
	 */
	private void appendExplanation(Set<OWLAxiom> explanation, StringBuilder msgBuilder, OWLPrettyPrinter pp) {
		if (explanation.isEmpty() == false) {
			msgBuilder.append(" Explanation: [");
			for (Iterator<OWLAxiom> it = explanation.iterator(); it.hasNext();) {
				OWLAxiom axiom = it.next();
				msgBuilder.append(pp.render(axiom));
				if (it.hasNext()) {
					msgBuilder.append("; ");
				}
			}
			msgBuilder.append("]");
		}
	}

	
}
