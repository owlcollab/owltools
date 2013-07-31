package owltools.gaf.rules;

import java.text.NumberFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.HermiT.Configuration;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.ReasonerProgressMonitor;

import owltools.gaf.GeneAnnotation;
import owltools.gaf.owl.GAFOWLBridge;
import owltools.gaf.rules.AnnotationRuleViolation.ViolationType;
import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;
import owltools.reasoner.PrecomputingMoreReasonerFactory;

/**
 * Checks if an annotation is valid according to taxon constraints.
 * 
 */
public class AnnotationTaxonRule extends AbstractAnnotationRule {

	private static final Logger logger = Logger.getLogger(AnnotationTaxonRule.class);
	
	private final OWLGraphWrapper graph;
	private final Map<String, OWLObject> allOWLObjectsByAltId;
	
	/**
	 * @param graph
	 */
	public AnnotationTaxonRule(OWLGraphWrapper graph) {
		allOWLObjectsByAltId = graph.getAllOWLObjectsByAltId();
		this.graph = graph;
	}

	@Override
	public Set<AnnotationRuleViolation> getRuleViolations(GeneAnnotation a) {
		// check that all identifiers, required for a taxon check, can be resolved.
		
		String annotationCls = a.getCls();
		String taxonCls = a.getBioentityObject().getNcbiTaxonId();
		
		if (taxonCls == null) {
			AnnotationRuleViolation v = new AnnotationRuleViolation(getRuleId(), "Taxon id is null", a);
			return Collections.singleton(v);
		}
		
		OWLObject cls = graph.getOWLObjectByIdentifier(annotationCls);
		OWLObject tax = graph.getOWLObjectByIdentifier(taxonCls);
		
		if (cls == null) {
			AnnotationRuleViolation v = new AnnotationRuleViolation(getRuleId(), "Could not retrieve a class for annotationCls: "+annotationCls, a, ViolationType.Warning);
			return Collections.singleton(v);
		}
		if (tax == null) {
			tax = allOWLObjectsByAltId.get(taxonCls);
			
			if (tax == null) {
				AnnotationRuleViolation v = new AnnotationRuleViolation(getRuleId(), "Could not retrieve a class for taxonCls: "+taxonCls, a, ViolationType.Warning);
				return Collections.singleton(v);
			}
			else {
				String mainTaxId = graph.getIdentifier(tax);
				AnnotationRuleViolation v = new AnnotationRuleViolation(getRuleId(), "Use of out-dated taxon identifier: "+taxonCls+" is replaced by "+mainTaxId, a, ViolationType.Warning);
				return Collections.singleton(v);
			}
		}
		
		
		return Collections.emptySet();
	}
	
	@Override
	public boolean isOwlDocumentLevel() {
		return true;
	}

	@Override
	public Set<AnnotationRuleViolation> getRuleViolations(OWLGraphWrapper graph) {
		final OWLOntology ontology = graph.getSourceOntology();
		
//		OWLReasoner reasoner = createHermit(ontology);
//		OWLReasoner reasoner = createMore(ontology);
		OWLReasoner reasoner = createElk(ontology);
		try {
			boolean consistent = reasoner.isConsistent();
			if (!consistent) {
				return Collections.singleton(new AnnotationRuleViolation(getRuleId(), "Logic inconsistency in combined annotations and ontology detected."));
			}

			Node<OWLClass> unsatisfiableClasses = reasoner.getUnsatisfiableClasses();
			if (unsatisfiableClasses != null) {
				Set<AnnotationRuleViolation> result = new HashSet<AnnotationRuleViolation>();
				OWLDataFactory fact = ontology.getOWLOntologyManager().getOWLDataFactory();
				final OWLAnnotationProperty lineProperty = fact.getOWLAnnotationProperty(GAFOWLBridge.GAF_LINE_NUMBER_ANNOTATION_PROPERTY_IRI);
				OWLPrettyPrinter pp = new OWLPrettyPrinter(graph);
				Set<OWLClass> entities = unsatisfiableClasses.getEntities();
				Set<OWLClass> unsatisfiable = new HashSet<OWLClass>();
				for (OWLClass c : entities) {
					if (c.isBottomEntity() || c.isTopEntity()) {
						continue;
					}
					unsatisfiable.add(c);
					Set<OWLAnnotationAssertionAxiom> axioms = ontology.getAnnotationAssertionAxioms(c.getIRI());
					Set<Integer> lineNumbers = new HashSet<Integer>();
					for (OWLAnnotationAssertionAxiom axiom : axioms) {
						if (lineProperty.equals(axiom.getProperty())) {
							OWLAnnotationValue value = axiom.getValue();
							if (value instanceof OWLLiteral) {
								OWLLiteral literal = (OWLLiteral) value;
								String stringValue = literal.getLiteral();
								try {
									Integer lineNumber = new Integer(stringValue);
									lineNumbers.add(lineNumber);
								} catch (NumberFormatException e) {
									// ignore error
								}
							}
						}
					}
					if (lineNumbers.isEmpty() == false) {
						for (Integer lineNumber : lineNumbers) {
							AnnotationRuleViolation violation = new AnnotationRuleViolation(getRuleId(), "unsatisfiable class: "+pp.render(c), (GeneAnnotation) null, ViolationType.Error);
							violation.setLineNumber(lineNumber.intValue());
							result.add(violation);
						}
						
					}
					
				}
				handleUnsatisfiable(unsatisfiable, ontology);
				return result;
			}
			return Collections.emptySet();
		}
		finally {
			reasoner.dispose();
		}
		
	}
	
	protected void handleUnsatisfiable(Set<OWLClass> unsatisfiable, OWLOntology ontology) {
		// do nothing
	}

	private OWLReasoner createElk(OWLOntology ontology) {
		ElkReasonerFactory factory = new ElkReasonerFactory();
		return factory.createReasoner(ontology);
	}
	
	private OWLReasoner createMore(OWLOntology ontology) {
		PrecomputingMoreReasonerFactory factory = PrecomputingMoreReasonerFactory.getMoreHermitFactory();
		return factory.createReasoner(ontology);
	}
	
	private OWLReasoner createHermit(final OWLOntology ontology) {
		// use Hermit, as GO has inverse_of relations between part_of and has_part
		OWLReasonerFactory factory = new org.semanticweb.HermiT.Reasoner.ReasonerFactory();
		
		final Configuration configuration = new Configuration();
		configuration.reasonerProgressMonitor = new ReasonerProgressMonitor() {
			
			@Override
			public void reasonerTaskStopped() {
				logger.info("HermiT reasoning task - Finished.");
				
			}
			
			@Override
			public void reasonerTaskStarted(String taskName) {
				logger.info("HermiT reasoning task - Start: "+taskName);
				
			}
			
			double lastProgress = 0.0d;
			
			@Override
			public void reasonerTaskProgressChanged(int value, int max) {
				double progress = value / (double) max;
				if (Math.abs(progress - lastProgress) > 0.05d) {
					NumberFormat percentFormat = NumberFormat.getPercentInstance();
					percentFormat.setMaximumFractionDigits(1);
					logger.info("HermiT reasoning task - Progress: "+percentFormat.format(progress));
					lastProgress = progress;
				}
			}
			
			@Override
			public void reasonerTaskBusy() {
				// do nothing
			}
		};
		OWLReasoner reasoner = factory.createReasoner(ontology, configuration);
		return reasoner;
	}

}

