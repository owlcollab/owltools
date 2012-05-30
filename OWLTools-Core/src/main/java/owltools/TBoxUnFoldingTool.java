package owltools;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLClassExpressionVisitorEx;
import org.semanticweb.owlapi.model.OWLDataAllValuesFrom;
import org.semanticweb.owlapi.model.OWLDataExactCardinality;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataHasValue;
import org.semanticweb.owlapi.model.OWLDataMaxCardinality;
import org.semanticweb.owlapi.model.OWLDataMinCardinality;
import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectExactCardinality;
import org.semanticweb.owlapi.model.OWLObjectHasSelf;
import org.semanticweb.owlapi.model.OWLObjectHasValue;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectMaxCardinality;
import org.semanticweb.owlapi.model.OWLObjectMinCardinality;
import org.semanticweb.owlapi.model.OWLObjectOneOf;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;

/**
 * Provide TBox unfolding methods. This can be considered a non-standard
 * reasoner task.
 */
public class TBoxUnFoldingTool {
	
	private static final Logger LOG = Logger.getLogger(TBoxUnFoldingTool.class);
	
	private final OWLGraphWrapper graph;
	private final OWLOntology ontology;
	private final UnfoldingVisitor visitor;
	
	/**
	 * Create a new instance for the given ontology graph. Use the given set of parent
	 * ids to retrieve the classes which are to be unfolded.
	 * 
	 * @param graph ontology
	 * @param parents set of OBO-style ids
	 * @param reasonerName type of reasoner to be used for inferring the relevant sub classes of the parent set.
	 * @throws NonDeterministicUnfoldException
	 */
	public TBoxUnFoldingTool(OWLGraphWrapper graph, Set<String> parents, String reasonerName) throws NonDeterministicUnfoldException {
		this.graph = graph;
		this.ontology = graph.getSourceOntology();
		
		InferenceBuilder inferenceBuilder = new InferenceBuilder(graph, reasonerName);
		OWLReasoner reasoner = inferenceBuilder.getReasoner(ontology);
		
		Set<OWLClass> unfoldClasses = new HashSet<OWLClass>();
		
		for(String parent : parents) {
			OWLClass parentClass = graph.getOWLClassByIdentifier(parent);
			NodeSet<OWLClass> nodeSet = reasoner.getSubClasses(parentClass , false);
			if (nodeSet != null && !nodeSet.isEmpty() && !nodeSet.isBottomSingleton()) {
				unfoldClasses.addAll(nodeSet.getFlattened());
			}
		}
		inferenceBuilder.dispose();
		
		if (unfoldClasses.isEmpty()) {
			throw new RuntimeException("No classes found for given parents.");
		}
		
		visitor = new UnfoldingVisitor(unfoldClasses, ontology);
	}

	/**
	 * Unfold the equivalence axiom of the {@link OWLClass} for the given id.
	 * 
	 * @param id OBO-style id
	 * @return unfolded equivalence axiom or null
	 * @throws NonDeterministicUnfoldException
	 */
	public OWLEquivalentClassesAxiom unfold(String id) throws NonDeterministicUnfoldException {
		OWLClass owlClass = graph.getOWLClassByIdentifier(id);
		if (owlClass == null) {
			return null;
		}
		return unfold(owlClass);
	}
	
	/**
	 * Unfold the equivalence axiom of the {@link OWLClass}
	 * 
	 * @param owlClass
	 * @return unfolded equivalence axiom or null
	 * @throws NonDeterministicUnfoldException
	 */
	public OWLEquivalentClassesAxiom unfold(OWLClass owlClass) throws NonDeterministicUnfoldException {
		Set<OWLEquivalentClassesAxiom> axioms = ontology.getEquivalentClassesAxioms(owlClass);
		if (axioms == null || axioms.isEmpty()) {
			return null;
		}
		if (axioms.size() > 1) {
			throw new NonDeterministicUnfoldException("Non deterministic unfold for class: "+owlClass.getIRI());
		}
		final OWLEquivalentClassesAxiom axiom = axioms.iterator().next();
		OWLEquivalentClassesAxiom unfolded = visitor.unfoldAxiom(axiom, owlClass);
		return unfolded;
	}
	
	/**
	 * Unfold the {@link OWLEquivalentClassesAxiom} of the {@link OWLClass}.
	 * Return either the updated axiom or the original axiom
	 * 
	 * @param ax
	 * @param owlClass
	 * @return axiom (never null)
	 */
	public OWLEquivalentClassesAxiom unfold(OWLEquivalentClassesAxiom ax, OWLClass owlClass) {
		OWLEquivalentClassesAxiom unfolded = visitor.unfoldAxiom(ax, owlClass);
		if (unfolded != null) {
			return unfolded;
		}
		return ax;
	}
	
	/**
	 * Exception indication, that the unfold operation is not deterministic.
	 * This is usually the case for ontologies, with an expansion term with
	 * multiple {@link OWLEquivalentClassesAxiom}s.
	 */
	public static class NonDeterministicUnfoldException extends Exception {

		// generated
		private static final long serialVersionUID = 337910909767722849L;

		/**
		 * @param message
		 */
		protected NonDeterministicUnfoldException(String message) {
			super(message);
		}
		
	}
	
	/**
	 * Recursive unfold of {@link OWLAxiom} and {@link OWLClassExpression}. 
	 * Unfold only for a given set of classes.
	 * 
	 * First, the constructor checks that the unfolding is deterministic 
	 * (i.e. there is at most one equivalence axiom per class). 
	 */
	static class UnfoldingVisitor implements OWLClassExpressionVisitorEx<OWLClassExpression> {
		
		private final Map<OWLClass, OWLClassExpression> unfoldClasses;
		private final OWLDataFactory factory;

		UnfoldingVisitor(Set<OWLClass> unfoldClasses, OWLOntology ontology) throws NonDeterministicUnfoldException {
			this.unfoldClasses = new HashMap<OWLClass, OWLClassExpression>();
			factory = ontology.getOWLOntologyManager().getOWLDataFactory();
			
			for(OWLClass owlClass : unfoldClasses) {
				Set<OWLEquivalentClassesAxiom> eqAxioms = ontology.getEquivalentClassesAxioms(owlClass);
				if (eqAxioms != null && !eqAxioms.isEmpty()) {
					if(eqAxioms.size() > 1) {
						throw new NonDeterministicUnfoldException("Non deterministic unfold for class: "+owlClass.getIRI());
					}
					OWLEquivalentClassesAxiom eqAxiom = eqAxioms.iterator().next();
					Set<OWLClassExpression> expressions = eqAxiom.getClassExpressionsMinus(owlClass);
					if (expressions.size() == 1) {
						this.unfoldClasses.put(owlClass, expressions.iterator().next());
					}
					else if (expressions.size() > 1) {
						OWLClassExpression ce = factory.getOWLObjectIntersectionOf(expressions);
						this.unfoldClasses.put(owlClass, ce);
					}
				}
			}
			
			// TODO check that there are no cycles in the unfold expressions, otherwise this unfold will not terminate!
		}
		
		OWLEquivalentClassesAxiom unfoldAxiom(OWLEquivalentClassesAxiom ax, OWLClass owlClass) {
			Set<OWLClassExpression> existing = ax.getClassExpressionsMinus(owlClass);
			OWLClassExpression ce;
			if (existing == null || existing.isEmpty()) {
				return null;
			}
			else if (existing.size() == 1) {
				ce = existing.iterator().next();
			}
			else {
				ce = factory.getOWLObjectIntersectionOf(existing);
			}
			if(LOG.isDebugEnabled()) {
				LOG.debug("Unfolding axiom: "+ax);
			}
			OWLClassExpression unfolded = ce.accept(this);
			
			if (unfolded != null) {
				return factory.getOWLEquivalentClassesAxiom(owlClass, unfolded);
			}
			return null;
		}

		Set<OWLClassExpression> unfoldExpressions(Collection<OWLClassExpression> expressions) {
			Set<OWLClassExpression> unfolded = new HashSet<OWLClassExpression>();
			boolean changed = false;
			
			for (OWLClassExpression expression : expressions) {
				OWLClassExpression unfoldedExpression = expression.accept(this);
				if (unfoldedExpression != null) {
					changed = true;
					unfolded.add(unfoldedExpression);
				}
				else {
					unfolded.add(expression);
				}
			}
			
			if (changed) {
				return unfolded;
			}
			return null;
		}
		
		@Override
		public OWLClassExpression visit(OWLClass owlClass) {
			OWLClassExpression ce = unfoldClasses.get(owlClass);
			if (ce != null) {
				
				if (LOG.isDebugEnabled()) {
					LOG.debug("Unfolding class: "+owlClass.getIRI());
				}
				
				// recursive unfold
				OWLClassExpression unfold = ce.accept(this);
				if (unfold != null) {
					return unfold;
				}
			}
			return ce;
		}

		@Override
		public OWLObjectIntersectionOf visit(OWLObjectIntersectionOf ce) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Unfolding intersection_of: "+ce);
			}
			
			Set<OWLClassExpression> operands = ce.getOperands();
			if (operands != null && !operands.isEmpty()) {
				Set<OWLClassExpression> unfolded = unfoldExpressions(operands);
				if (unfolded != null) {
					return factory.getOWLObjectIntersectionOf(unfolded);
				}
			}
			return null;
		}

		@Override
		public OWLObjectUnionOf visit(OWLObjectUnionOf ce) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Unfolding union_of: "+ce);
			}
			
			Set<OWLClassExpression> operands = ce.getOperands();
			if (operands != null && !operands.isEmpty()) {
				Set<OWLClassExpression> unfolded = unfoldExpressions(operands);
				if (unfolded != null) {
					return factory.getOWLObjectUnionOf(unfolded);
				}
			}
			return null;
		}

		@Override
		public OWLObjectComplementOf visit(OWLObjectComplementOf ce) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Unfolding complement_of: "+ce);
			}
			
			OWLClassExpression operand = ce.getOperand();
			if (operand != null) {
				OWLClassExpression unfold = operand.accept(this);
				if (unfold != null) {
					return factory.getOWLObjectComplementOf(unfold);
				}
			}
			return null;
		}

		@Override
		public OWLObjectSomeValuesFrom visit(OWLObjectSomeValuesFrom ce) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Unfolding some_values_from: "+ce);
			}
			
			OWLClassExpression filler = ce.getFiller();
			if (filler != null) {
				OWLClassExpression unfold = filler.accept(this);
				if (unfold != null) {
					return factory.getOWLObjectSomeValuesFrom(ce.getProperty(), unfold);
				}
			}
			return null;
		}

		@Override
		public OWLObjectAllValuesFrom visit(OWLObjectAllValuesFrom ce) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Unfolding all_values_from: "+ce);
			}
			
			OWLClassExpression filler = ce.getFiller();
			if (filler != null) {
				OWLClassExpression unfold = filler.accept(this);
				if (unfold != null) {
					return factory.getOWLObjectAllValuesFrom(ce.getProperty(), unfold);
				}
			}
			return null;
		}

		@Override
		public OWLObjectHasValue visit(OWLObjectHasValue ce) {
			return null;
		}

		@Override
		public OWLObjectMinCardinality visit(OWLObjectMinCardinality ce) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Unfolding min_cardinality: "+ce);
			}
			
			OWLClassExpression filler = ce.getFiller();
			if (filler != null) {
				OWLClassExpression unfold = filler.accept(this);
				if (unfold != null) {
					return factory.getOWLObjectMinCardinality(ce.getCardinality(), ce.getProperty(), unfold);
				}
			}
			return null;
		}

		@Override
		public OWLObjectExactCardinality visit(OWLObjectExactCardinality ce) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Unfolding exact_cardinality: "+ce);
			}
			
			OWLClassExpression filler = ce.getFiller();
			if (filler != null) {
				OWLClassExpression unfold = filler.accept(this);
				if (unfold != null) {
					return factory.getOWLObjectExactCardinality(ce.getCardinality(), ce.getProperty(), unfold);
				}
			}
			return null;
		}

		@Override
		public OWLObjectMaxCardinality visit(OWLObjectMaxCardinality ce) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Unfolding max_cardinality: "+ce);
			}
			OWLClassExpression filler = ce.getFiller();
			if (filler != null) {
				OWLClassExpression unfold = filler.accept(this);
				if (unfold != null) {
					return factory.getOWLObjectMaxCardinality(ce.getCardinality(), ce.getProperty(), unfold);
				}
			}
			return null;
		}

		@Override
		public OWLObjectHasSelf visit(OWLObjectHasSelf ce) {
			return null;
		}

		@Override
		public OWLObjectOneOf visit(OWLObjectOneOf ce) {
			return null;
		}

		@Override
		public OWLDataSomeValuesFrom visit(OWLDataSomeValuesFrom ce) {
			return null;
		}

		@Override
		public OWLDataAllValuesFrom visit(OWLDataAllValuesFrom ce) {
			return null;
		}

		@Override
		public OWLDataHasValue visit(OWLDataHasValue ce) {
			return null;
		}

		@Override
		public OWLDataMinCardinality visit(OWLDataMinCardinality ce) {
			return null;
		}

		@Override
		public OWLDataExactCardinality visit(OWLDataExactCardinality ce) {
			return null;
		}

		@Override
		public OWLDataMaxCardinality visit(OWLDataMaxCardinality ce) {
			return null;
		}
		
	}

	/**
	 * Unfold the equivalence axiom of the {@link OWLClass} for the given id.
	 * 
	 * @param id OBO-style id
	 * @return string representation for the unfolded class equivalence axiom or null
	 * @throws NonDeterministicUnfoldException
	 */
	public String unfoldToString(String id) throws NonDeterministicUnfoldException {
		OWLClass owlClass = graph.getOWLClassByIdentifier(id);
		if (owlClass == null) {
			return null;
		}
		return unfoldToString(owlClass);
	}

	/**
	 * Unfold the equivalence axiom of the {@link OWLClass}
	 * 
	 * @param owlClass
	 * @return string representation for the unfolded class equivalence axiom or null
	 * @throws NonDeterministicUnfoldException
	 */
	public String unfoldToString(OWLClass owlClass) throws NonDeterministicUnfoldException {
		OWLEquivalentClassesAxiom unfolded = unfold(owlClass);
		if (unfolded != null) {
			OWLPrettyPrinter pp = new OWLPrettyPrinter(graph);
			return pp.render(unfolded);
		}
		return null;
	}

}
