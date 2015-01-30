package owltools.reasoner;


import java.util.HashSet;
import java.util.Set;

import org.geneontology.reasoner.OWLExtendedReasoner;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.AxiomNotInProfileException;
import org.semanticweb.owlapi.reasoner.BufferingMode;
import org.semanticweb.owlapi.reasoner.ClassExpressionNotInProfileException;
import org.semanticweb.owlapi.reasoner.FreshEntitiesException;
import org.semanticweb.owlapi.reasoner.InconsistentOntologyException;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasonerConfiguration;
import org.semanticweb.owlapi.reasoner.ReasonerInterruptedException;
import org.semanticweb.owlapi.reasoner.TimeOutException;
import org.semanticweb.owlapi.reasoner.UnsupportedEntailmentTypeException;
import org.semanticweb.owlapi.reasoner.impl.DefaultNodeSet;
import org.semanticweb.owlapi.reasoner.impl.OWLClassNode;
import org.semanticweb.owlapi.reasoner.impl.OWLClassNodeSet;
import org.semanticweb.owlapi.reasoner.impl.OWLNamedIndividualNodeSet;
import org.semanticweb.owlapi.reasoner.impl.OWLReasonerBase;
import org.semanticweb.owlapi.util.Version;

import owltools.graph.OWLGraphWrapper;

/**
 * 
 * incomplete.
 * 
 * the goal is to expose OWLGraphReasoner reachability queries via the reasoner interface.
 * 
 * however, OWLReasoners do not support returning class expressions for superclass queries,
 * so this provides an extra method
 * 
 * @author cjm
 *
 */
public class GraphReasoner extends OWLReasonerBase implements OWLExtendedReasoner {


	private static final Version version = new Version(1, 0, 0, 0);

	private OWLGraphWrapper gw;

	protected GraphReasoner(OWLOntology rootOntology,
			OWLReasonerConfiguration configuration, BufferingMode bufferingMode) {
		super(rootOntology, configuration, bufferingMode);
		gw = new OWLGraphWrapper(rootOntology);
	}

	public String getReasonerName() {
		return "Graph Reasoner";
	}

	public Version getReasonerVersion() {
		return version;
	}

	public void interrupt() {
		// TODO Auto-generated method stub

	}

	public void precomputeInferences(InferenceType... inferenceTypes)
	throws ReasonerInterruptedException, TimeOutException,
	InconsistentOntologyException {
		// TODO Auto-generated method stub

	}

	public boolean isPrecomputed(InferenceType inferenceType) {
		// TODO Auto-generated method stub
		return false;
	}

	public Set<InferenceType> getPrecomputableInferenceTypes() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isConsistent() throws ReasonerInterruptedException,
	TimeOutException {
		return true;
	}

	public boolean isSatisfiable(OWLClassExpression classExpression)
	throws ReasonerInterruptedException, TimeOutException,
	ClassExpressionNotInProfileException, FreshEntitiesException,
	InconsistentOntologyException {
		// TODO Auto-generated method stub
		return true;
	}

	public Node<OWLClass> getUnsatisfiableClasses()
	throws ReasonerInterruptedException, TimeOutException,
	InconsistentOntologyException {
		return OWLClassNode.getBottomNode();
	}

	public boolean isEntailed(OWLAxiom axiom)
	throws ReasonerInterruptedException,
	UnsupportedEntailmentTypeException, TimeOutException,
	AxiomNotInProfileException, FreshEntitiesException,
	InconsistentOntologyException {
		return getRootOntology().containsAxiomIgnoreAnnotations(axiom, true);
	}

	public boolean isEntailed(Set<? extends OWLAxiom> axioms)
	throws ReasonerInterruptedException,
	UnsupportedEntailmentTypeException, TimeOutException,
	AxiomNotInProfileException, FreshEntitiesException,
	InconsistentOntologyException {
		for (OWLAxiom ax : axioms) {
			if (!getRootOntology().containsAxiomIgnoreAnnotations(ax, true)) {
				return false;
			}
		}
		return true;
	}

	public boolean isEntailmentCheckingSupported(AxiomType<?> axiomType) {
		return false;
	}


	public NodeSet<OWLClass> getSubClasses(OWLClassExpression ce, boolean direct)
	throws ReasonerInterruptedException, TimeOutException,
	FreshEntitiesException, InconsistentOntologyException,
	ClassExpressionNotInProfileException {

		DefaultNodeSet<OWLClass> result = new OWLClassNodeSet();
		Set<OWLObject> subs = gw.queryDescendants(ce, false, true);
		for (OWLObject s : subs) {
			if (s instanceof OWLClassExpression) {
				if (s instanceof OWLClass) {
					result.addEntity((OWLClass) s);
				}
				else {

				}
			}
			else {

			}
		}
		return result;
	}

	/**
	 * note that this is not a standard reasoner method
	 * 
	 * @param ce
	 * @param direct
	 * @return all superclasses, where superclasses can include anon class expressions
	 * @throws InconsistentOntologyException
	 * @throws ClassExpressionNotInProfileException
	 * @throws FreshEntitiesException
	 * @throws ReasonerInterruptedException
	 * @throws TimeOutException
	 */
	public Set<OWLClassExpression> getSuperClassExpressions(OWLClassExpression ce,
			boolean direct) throws InconsistentOntologyException,
			ClassExpressionNotInProfileException, FreshEntitiesException,
			ReasonerInterruptedException, TimeOutException {

		Set<OWLClassExpression> result = new HashSet<OWLClassExpression>();
		Set<OWLObject> supers = gw.getSubsumersFromClosure(ce);
		for (OWLObject sup : supers) {
			if (sup instanceof OWLClassExpression) {
				result.add((OWLClassExpression) sup);
			}
			else {

			}
		}
		return result;
	}


	public NodeSet<OWLClass> getSuperClasses(OWLClassExpression ce,
			boolean direct) throws InconsistentOntologyException,
			ClassExpressionNotInProfileException, FreshEntitiesException,
			ReasonerInterruptedException, TimeOutException {

		DefaultNodeSet<OWLClass> result = new OWLClassNodeSet();
		Set<OWLObject> supers = gw.getSubsumersFromClosure(ce);
		for (OWLObject sup : supers) {
			if (sup instanceof OWLClassExpression) {
				if (sup instanceof OWLClass) {
					result.addEntity((OWLClass) sup);
				}
				else {

				}
			}
			else {

			}
		}
		return result;
	}

	public Node<OWLClass> getEquivalentClasses(OWLClassExpression ce)
	throws InconsistentOntologyException,
	ClassExpressionNotInProfileException, FreshEntitiesException,
	ReasonerInterruptedException, TimeOutException {
		// TODO Auto-generated method stub
		return null;
	}

	public NodeSet<OWLClass> getDisjointClasses(OWLClassExpression ce)
	throws ReasonerInterruptedException, TimeOutException,
	FreshEntitiesException, InconsistentOntologyException {
		return new OWLClassNodeSet();
	}

	public Node<OWLObjectPropertyExpression> getTopObjectPropertyNode() {
		// TODO Auto-generated method stub
		return null;
	}

	public Node<OWLObjectPropertyExpression> getBottomObjectPropertyNode() {
		// TODO Auto-generated method stub
		return null;
	}

	public NodeSet<OWLObjectPropertyExpression> getSubObjectProperties(
			OWLObjectPropertyExpression pe, boolean direct)
			throws InconsistentOntologyException, FreshEntitiesException,
			ReasonerInterruptedException, TimeOutException {
		// TODO Auto-generated method stub
		return null;
	}

	public NodeSet<OWLObjectPropertyExpression> getSuperObjectProperties(
			OWLObjectPropertyExpression pe, boolean direct)
			throws InconsistentOntologyException, FreshEntitiesException,
			ReasonerInterruptedException, TimeOutException {
		// TODO Auto-generated method stub
		return null;
	}

	public Node<OWLObjectPropertyExpression> getEquivalentObjectProperties(
			OWLObjectPropertyExpression pe)
			throws InconsistentOntologyException, FreshEntitiesException,
			ReasonerInterruptedException, TimeOutException {
		// TODO Auto-generated method stub
		return null;
	}

	public NodeSet<OWLObjectPropertyExpression> getDisjointObjectProperties(
			OWLObjectPropertyExpression pe)
			throws InconsistentOntologyException, FreshEntitiesException,
			ReasonerInterruptedException, TimeOutException {
		// TODO Auto-generated method stub
		return null;
	}

	public Node<OWLObjectPropertyExpression> getInverseObjectProperties(
			OWLObjectPropertyExpression pe)
			throws InconsistentOntologyException, FreshEntitiesException,
			ReasonerInterruptedException, TimeOutException {
		// TODO Auto-generated method stub
		return null;
	}

	public NodeSet<OWLClass> getObjectPropertyDomains(
			OWLObjectPropertyExpression pe, boolean direct)
			throws InconsistentOntologyException, FreshEntitiesException,
			ReasonerInterruptedException, TimeOutException {
		// TODO Auto-generated method stub
		return null;
	}

	public NodeSet<OWLClass> getObjectPropertyRanges(
			OWLObjectPropertyExpression pe, boolean direct)
			throws InconsistentOntologyException, FreshEntitiesException,
			ReasonerInterruptedException, TimeOutException {
		// TODO Auto-generated method stub
		return null;
	}

	public Node<OWLDataProperty> getTopDataPropertyNode() {
		// TODO Auto-generated method stub
		return null;
	}

	public Node<OWLDataProperty> getBottomDataPropertyNode() {
		// TODO Auto-generated method stub
		return null;
	}

	public NodeSet<OWLDataProperty> getSubDataProperties(OWLDataProperty pe,
			boolean direct) throws InconsistentOntologyException,
			FreshEntitiesException, ReasonerInterruptedException,
			TimeOutException {
		// TODO Auto-generated method stub
		return null;
	}

	public NodeSet<OWLDataProperty> getSuperDataProperties(OWLDataProperty pe,
			boolean direct) throws InconsistentOntologyException,
			FreshEntitiesException, ReasonerInterruptedException,
			TimeOutException {
		// TODO Auto-generated method stub
		return null;
	}

	public Node<OWLDataProperty> getEquivalentDataProperties(OWLDataProperty pe)
	throws InconsistentOntologyException, FreshEntitiesException,
	ReasonerInterruptedException, TimeOutException {
		// TODO Auto-generated method stub
		return null;
	}

	public NodeSet<OWLDataProperty> getDisjointDataProperties(
			OWLDataPropertyExpression pe) throws InconsistentOntologyException,
			FreshEntitiesException, ReasonerInterruptedException,
			TimeOutException {
		// TODO Auto-generated method stub
		return null;
	}

	public NodeSet<OWLClass> getDataPropertyDomains(OWLDataProperty pe,
			boolean direct) throws InconsistentOntologyException,
			FreshEntitiesException, ReasonerInterruptedException,
			TimeOutException {
		// TODO Auto-generated method stub
		return null;
	}

	public NodeSet<OWLClass> getTypes(OWLNamedIndividual ind, boolean direct)
	throws InconsistentOntologyException, FreshEntitiesException,
	ReasonerInterruptedException, TimeOutException {
		// TODO Auto-generated method stub
		return null;
	}

	public NodeSet<OWLNamedIndividual> getInstances(OWLClassExpression ce,
			boolean direct) throws InconsistentOntologyException,
			ClassExpressionNotInProfileException, FreshEntitiesException,
			ReasonerInterruptedException, TimeOutException {
		DefaultNodeSet<OWLNamedIndividual> result = new OWLNamedIndividualNodeSet();
		Set<OWLObject> subs = gw.queryDescendants(ce, true, true);
		for (OWLObject s : subs) {
			if (s instanceof OWLNamedIndividual) {
				result.addEntity((OWLNamedIndividual) s);
			}
			else {

			}
		}
		return result;
	}

	public NodeSet<OWLNamedIndividual> getObjectPropertyValues(
			OWLNamedIndividual ind, OWLObjectPropertyExpression pe)
			throws InconsistentOntologyException, FreshEntitiesException,
			ReasonerInterruptedException, TimeOutException {
		// TODO Auto-generated method stub
		return null;
	}

	public Set<OWLLiteral> getDataPropertyValues(OWLNamedIndividual ind,
			OWLDataProperty pe) throws InconsistentOntologyException,
			FreshEntitiesException, ReasonerInterruptedException,
			TimeOutException {
		// TODO Auto-generated method stub
		return null;
	}

	public Node<OWLNamedIndividual> getSameIndividuals(OWLNamedIndividual ind)
	throws InconsistentOntologyException, FreshEntitiesException,
	ReasonerInterruptedException, TimeOutException {
		// TODO Auto-generated method stub
		return null;
	}

	public NodeSet<OWLNamedIndividual> getDifferentIndividuals(
			OWLNamedIndividual ind) throws InconsistentOntologyException,
			FreshEntitiesException, ReasonerInterruptedException,
			TimeOutException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void handleChanges(Set<OWLAxiom> addAxioms,
			Set<OWLAxiom> removeAxioms) {
		// TODO Auto-generated method stub

	}

	public Node<OWLClass> getTopClassNode() {
		// TODO Auto-generated method stub
		return null;
	}

	public Node<OWLClass> getBottomClassNode() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<OWLClass> getSuperClassesOver(OWLClassExpression ce,
			OWLObjectProperty p, boolean direct)
			throws InconsistentOntologyException,
			ClassExpressionNotInProfileException, FreshEntitiesException,
			ReasonerInterruptedException, TimeOutException {
		// TODO Auto-generated method stub
		return null;
	}

}
