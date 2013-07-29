package owltools.reasoner;


import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.UnknownOWLOntologyException;
import org.semanticweb.owlapi.reasoner.AxiomNotInProfileException;
import org.semanticweb.owlapi.reasoner.BufferingMode;
import org.semanticweb.owlapi.reasoner.ClassExpressionNotInProfileException;
import org.semanticweb.owlapi.reasoner.FreshEntitiesException;
import org.semanticweb.owlapi.reasoner.FreshEntityPolicy;
import org.semanticweb.owlapi.reasoner.InconsistentOntologyException;
import org.semanticweb.owlapi.reasoner.IndividualNodeSetPolicy;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerConfiguration;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.ReasonerInterruptedException;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.semanticweb.owlapi.reasoner.TimeOutException;
import org.semanticweb.owlapi.reasoner.UnsupportedEntailmentTypeException;
import org.semanticweb.owlapi.reasoner.impl.OWLReasonerBase;
import org.semanticweb.owlapi.util.Version;

/**
 * Wraps a reasoner. Any queries involving class expressions are first materialized to classes.
 * 
 * Primarily for use with Elk (0.3)
 * 
 * @author cjm
 *
 */
public class LazyExpressionMaterializingReasoner extends OWLReasonerBase implements OWLReasoner {

	private static Logger LOG = Logger.getLogger(LazyExpressionMaterializingReasoner.class);
	private static final Version version = new Version(1, 0, 0, 0);

	private OWLReasoner wrappedReasoner;
	private OWLReasonerFactory wrappedReasonerFactory;

	private OWLDataFactory dataFactory;
	private OWLOntology ontology;
	OWLOntologyManager manager;
	private Map<OWLObjectProperty,OWLClass> pxMap;
	private Map<OWLClass,OWLObjectProperty> xpMap;
	private Map<OWLClass,OWLClassExpression> cxMap;


	protected LazyExpressionMaterializingReasoner(OWLOntology rootOntology,
			OWLReasonerConfiguration configuration, BufferingMode bufferingMode) {
		super(rootOntology, configuration, bufferingMode);
		try {
			this.ontology = rootOntology;
			manager = rootOntology.getOWLOntologyManager();
			dataFactory = manager.getOWLDataFactory();
		} catch (UnknownOWLOntologyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public LazyExpressionMaterializingReasoner(OWLOntology ont) {
		this(ont, new SimpleConfiguration(), BufferingMode.BUFFERING);
	}
	
	

	public OWLReasonerFactory getWrappedReasonerFactory() {
		return wrappedReasonerFactory;
	}

	public void setWrappedReasonerFactory(OWLReasonerFactory wrappedReasonerFactory) {
		this.wrappedReasonerFactory = wrappedReasonerFactory;
	}

	public OWLReasoner getWrappedReasoner() {
		if (wrappedReasoner == null) {
			if (wrappedReasonerFactory == null) {
				wrappedReasonerFactory = new ElkReasonerFactory();
			}
			wrappedReasoner = wrappedReasonerFactory.createReasoner(ontology, this.getReasonerConfiguration());
		}
		return wrappedReasoner;
	}



	public void setWrappedReasoner(OWLReasoner wrappedReasoner) {
		this.wrappedReasoner = wrappedReasoner;
	}
	
	private OWLClass materializeExpression(OWLClassExpression ce) {
		UUID uuid = UUID.randomUUID();
		OWLClass qc = dataFactory.getOWLClass(IRI.create("http://owltools.org/Q/"+uuid.toString()));
		manager.removeAxioms(ontology, 		ontology.getAxioms(qc));
		OWLEquivalentClassesAxiom ax = dataFactory.getOWLEquivalentClassesAxiom(ce, qc);
		manager.addAxiom(ontology, ax);
		LOG.info("Materialized: "+ax);
		if (wrappedReasoner != null)
			getWrappedReasoner().flush();
		return qc;
	}

	
	public NodeSet<OWLClass> getSubClasses(OWLClassExpression ce, boolean direct)
	throws ReasonerInterruptedException, TimeOutException,
	FreshEntitiesException, InconsistentOntologyException,
	ClassExpressionNotInProfileException {
		if (ce.isAnonymous()) {
			OWLClass c = materializeExpression(ce);
			return getSubClasses(c, direct);
		}
		return getWrappedReasoner().getSubClasses(ce, direct);
	}



	public NodeSet<OWLClass> getSuperClasses(OWLClassExpression ce,
			boolean direct) throws InconsistentOntologyException,
			ClassExpressionNotInProfileException, FreshEntitiesException,
			ReasonerInterruptedException, TimeOutException {

		if (ce.isAnonymous()) {
			OWLClass c = materializeExpression(ce);
			return getSuperClasses(c, direct);
		}
		return getWrappedReasoner().getSuperClasses(ce, direct);
	}
	
	public Node<OWLClass> getEquivalentClasses(OWLClassExpression ce)
	throws InconsistentOntologyException,
	ClassExpressionNotInProfileException, FreshEntitiesException,
	ReasonerInterruptedException, TimeOutException {
		if (ce.isAnonymous()) {
			OWLClass c = materializeExpression(ce);
			return getEquivalentClasses(c);
		}
		return getWrappedReasoner().getEquivalentClasses(ce);
	}

	public NodeSet<OWLClass> getDisjointClasses(OWLClassExpression ce)
	throws ReasonerInterruptedException, TimeOutException,
	FreshEntitiesException, InconsistentOntologyException {
		return getWrappedReasoner().getDisjointClasses(ce);
	}



	public String getReasonerName() {
		return "Expression Materializing Reasoner";
	}

	public Version getReasonerVersion() {
		return getWrappedReasoner().getReasonerVersion();
	}

	public BufferingMode getBufferingMode() {
		return getWrappedReasoner().getBufferingMode();
	}

	public void flush() {
		getWrappedReasoner().flush();
	}

	public List<OWLOntologyChange> getPendingChanges() {
		return getWrappedReasoner().getPendingChanges();
	}

	public Set<OWLAxiom> getPendingAxiomAdditions() {
		return getWrappedReasoner().getPendingAxiomAdditions();
	}

	public Set<OWLAxiom> getPendingAxiomRemovals() {
		return getWrappedReasoner().getPendingAxiomRemovals();
	}

	public OWLOntology getRootOntology() {
		return getWrappedReasoner().getRootOntology();
	}

	public void interrupt() {
		getWrappedReasoner().interrupt();
	}

	public void precomputeInferences(InferenceType... inferenceTypes)
	throws ReasonerInterruptedException, TimeOutException,
	InconsistentOntologyException {
		getWrappedReasoner().precomputeInferences(inferenceTypes);
	}

	public boolean isPrecomputed(InferenceType inferenceType) {
		return getWrappedReasoner().isPrecomputed(inferenceType);
	}

	public Set<InferenceType> getPrecomputableInferenceTypes() {
		return getWrappedReasoner().getPrecomputableInferenceTypes();
	}

	public boolean isConsistent() throws ReasonerInterruptedException,
	TimeOutException {
		return getWrappedReasoner().isConsistent();
	}

	public boolean isSatisfiable(OWLClassExpression classExpression)
	throws ReasonerInterruptedException, TimeOutException,
	ClassExpressionNotInProfileException, FreshEntitiesException,
	InconsistentOntologyException {
		return getWrappedReasoner().isSatisfiable(classExpression);
	}

	public Node<OWLClass> getUnsatisfiableClasses()
	throws ReasonerInterruptedException, TimeOutException,
	InconsistentOntologyException {
		return getWrappedReasoner().getUnsatisfiableClasses();
	}

	public boolean isEntailed(OWLAxiom axiom)
	throws ReasonerInterruptedException,
	UnsupportedEntailmentTypeException, TimeOutException,
	AxiomNotInProfileException, FreshEntitiesException,
	InconsistentOntologyException {
		return getWrappedReasoner().isEntailed(axiom);
	}

	public boolean isEntailed(Set<? extends OWLAxiom> axioms)
	throws ReasonerInterruptedException,
	UnsupportedEntailmentTypeException, TimeOutException,
	AxiomNotInProfileException, FreshEntitiesException,
	InconsistentOntologyException {
		return getWrappedReasoner().isEntailed(axioms);
	}

	public boolean isEntailmentCheckingSupported(AxiomType<?> axiomType) {
		return getWrappedReasoner().isEntailmentCheckingSupported(axiomType);
	}

	public Node<OWLClass> getTopClassNode() {
		return getWrappedReasoner().getTopClassNode();
	}

	public Node<OWLClass> getBottomClassNode() {
		return getWrappedReasoner().getBottomClassNode();
	}



	public Node<OWLObjectPropertyExpression> getTopObjectPropertyNode() {
		return getWrappedReasoner().getTopObjectPropertyNode();
	}

	public Node<OWLObjectPropertyExpression> getBottomObjectPropertyNode() {
		return getWrappedReasoner().getBottomObjectPropertyNode();
	}

	public NodeSet<OWLObjectPropertyExpression> getSubObjectProperties(
			OWLObjectPropertyExpression pe, boolean direct)
			throws InconsistentOntologyException, FreshEntitiesException,
			ReasonerInterruptedException, TimeOutException {
		return getWrappedReasoner().getSubObjectProperties(pe, direct);
	}

	public NodeSet<OWLObjectPropertyExpression> getSuperObjectProperties(
			OWLObjectPropertyExpression pe, boolean direct)
			throws InconsistentOntologyException, FreshEntitiesException,
			ReasonerInterruptedException, TimeOutException {
		return getWrappedReasoner().getSuperObjectProperties(pe, direct);
	}

	public Node<OWLObjectPropertyExpression> getEquivalentObjectProperties(
			OWLObjectPropertyExpression pe)
			throws InconsistentOntologyException, FreshEntitiesException,
			ReasonerInterruptedException, TimeOutException {
		return getWrappedReasoner().getEquivalentObjectProperties(pe);
	}

	public NodeSet<OWLObjectPropertyExpression> getDisjointObjectProperties(
			OWLObjectPropertyExpression pe)
			throws InconsistentOntologyException, FreshEntitiesException,
			ReasonerInterruptedException, TimeOutException {
		return getWrappedReasoner().getDisjointObjectProperties(pe);
	}

	public Node<OWLObjectPropertyExpression> getInverseObjectProperties(
			OWLObjectPropertyExpression pe)
			throws InconsistentOntologyException, FreshEntitiesException,
			ReasonerInterruptedException, TimeOutException {
		return getWrappedReasoner().getInverseObjectProperties(pe);
	}

	public NodeSet<OWLClass> getObjectPropertyDomains(
			OWLObjectPropertyExpression pe, boolean direct)
			throws InconsistentOntologyException, FreshEntitiesException,
			ReasonerInterruptedException, TimeOutException {
		return getWrappedReasoner().getObjectPropertyDomains(pe, direct);
	}

	public NodeSet<OWLClass> getObjectPropertyRanges(
			OWLObjectPropertyExpression pe, boolean direct)
			throws InconsistentOntologyException, FreshEntitiesException,
			ReasonerInterruptedException, TimeOutException {
		return getWrappedReasoner().getObjectPropertyRanges(pe, direct);
	}

	public Node<OWLDataProperty> getTopDataPropertyNode() {
		return getWrappedReasoner().getTopDataPropertyNode();
	}

	public Node<OWLDataProperty> getBottomDataPropertyNode() {
		return getWrappedReasoner().getBottomDataPropertyNode();
	}

	public NodeSet<OWLDataProperty> getSubDataProperties(OWLDataProperty pe,
			boolean direct) throws InconsistentOntologyException,
			FreshEntitiesException, ReasonerInterruptedException,
			TimeOutException {
		return getWrappedReasoner().getSubDataProperties(pe, direct);
	}

	public NodeSet<OWLDataProperty> getSuperDataProperties(OWLDataProperty pe,
			boolean direct) throws InconsistentOntologyException,
			FreshEntitiesException, ReasonerInterruptedException,
			TimeOutException {
		return getWrappedReasoner().getSuperDataProperties(pe, direct);
	}

	public Node<OWLDataProperty> getEquivalentDataProperties(OWLDataProperty pe)
	throws InconsistentOntologyException, FreshEntitiesException,
	ReasonerInterruptedException, TimeOutException {
		return getWrappedReasoner().getEquivalentDataProperties(pe);
	}

	public NodeSet<OWLDataProperty> getDisjointDataProperties(
			OWLDataPropertyExpression pe) throws InconsistentOntologyException,
			FreshEntitiesException, ReasonerInterruptedException,
			TimeOutException {
		return getWrappedReasoner().getDisjointDataProperties(pe);
	}

	public NodeSet<OWLClass> getDataPropertyDomains(OWLDataProperty pe,
			boolean direct) throws InconsistentOntologyException,
			FreshEntitiesException, ReasonerInterruptedException,
			TimeOutException {
		return getWrappedReasoner().getDataPropertyDomains(pe, direct);
	}

	public NodeSet<OWLClass> getTypes(OWLNamedIndividual ind, boolean direct)
	throws InconsistentOntologyException, FreshEntitiesException,
	ReasonerInterruptedException, TimeOutException {
		return getWrappedReasoner().getTypes(ind, direct);
	}

	public NodeSet<OWLNamedIndividual> getInstances(OWLClassExpression ce,
			boolean direct) throws InconsistentOntologyException,
			ClassExpressionNotInProfileException, FreshEntitiesException,
			ReasonerInterruptedException, TimeOutException {
		if (ce.isAnonymous()) {
			OWLClass c = materializeExpression(ce);
			return getInstances(c, direct);
		}
		return getWrappedReasoner().getInstances(ce, direct);
	}

	public NodeSet<OWLNamedIndividual> getObjectPropertyValues(
			OWLNamedIndividual ind, OWLObjectPropertyExpression pe)
			throws InconsistentOntologyException, FreshEntitiesException,
			ReasonerInterruptedException, TimeOutException {
		return getWrappedReasoner().getObjectPropertyValues(ind, pe);
	}

	public Set<OWLLiteral> getDataPropertyValues(OWLNamedIndividual ind,
			OWLDataProperty pe) throws InconsistentOntologyException,
			FreshEntitiesException, ReasonerInterruptedException,
			TimeOutException {
		return getWrappedReasoner().getDataPropertyValues(ind, pe);
	}

	public Node<OWLNamedIndividual> getSameIndividuals(OWLNamedIndividual ind)
	throws InconsistentOntologyException, FreshEntitiesException,
	ReasonerInterruptedException, TimeOutException {
		return getWrappedReasoner().getSameIndividuals(ind);
	}

	public NodeSet<OWLNamedIndividual> getDifferentIndividuals(
			OWLNamedIndividual ind) throws InconsistentOntologyException,
			FreshEntitiesException, ReasonerInterruptedException,
			TimeOutException {
		return getWrappedReasoner().getDifferentIndividuals(ind);
	}

	public long getTimeOut() {
		return getWrappedReasoner().getTimeOut();
	}

	public FreshEntityPolicy getFreshEntityPolicy() {
		return getWrappedReasoner().getFreshEntityPolicy();
	}

	public IndividualNodeSetPolicy getIndividualNodeSetPolicy() {
		return getWrappedReasoner().getIndividualNodeSetPolicy();
	}

	public void dispose() {
		getWrappedReasoner().dispose();
	}

	@Override
	protected void handleChanges(Set<OWLAxiom> addAxioms,
			Set<OWLAxiom> removeAxioms) {
		// TODO Auto-generated method stub

	}


}
