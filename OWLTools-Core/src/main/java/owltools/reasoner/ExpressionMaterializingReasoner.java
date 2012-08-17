package owltools.reasoner;


import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
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
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
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
import org.semanticweb.owlapi.reasoner.ReasonerInterruptedException;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.semanticweb.owlapi.reasoner.TimeOutException;
import org.semanticweb.owlapi.reasoner.UnsupportedEntailmentTypeException;
import org.semanticweb.owlapi.reasoner.impl.OWLReasonerBase;
import org.semanticweb.owlapi.util.Version;

/**
 * This wraps an existing reasoner to implement OWLExtendedReasoner.
 * 
 * It works by materializing expressions of the form "R some Y" as
 * equivalence axioms prior to reasoning.
 * 
 * After reasoning, it can retrieve these anonymous superclasses.
 * 
 * Currently limited to a single level of nesting - in principle it could be extended
 * to expressions of depth k
 * 
 * @author cjm
 *
 */
public class ExpressionMaterializingReasoner extends OWLReasonerBase implements OWLExtendedReasoner {

	private static Logger LOG = Logger.getLogger(ExpressionMaterializingReasoner.class);
	private static final Version version = new Version(1, 0, 0, 0);

	private OWLReasoner wrappedReasoner;

	private OWLDataFactory dataFactory;
	private OWLOntology ontology;
	OWLOntologyManager manager;
	private Map<OWLObjectProperty,OWLClass> pxMap;
	private Map<OWLClass,OWLObjectProperty> xpMap;
	private Map<OWLClass,OWLClassExpression> cxMap;


	protected ExpressionMaterializingReasoner(OWLOntology rootOntology,
			OWLReasonerConfiguration configuration, BufferingMode bufferingMode) {
		super(rootOntology, configuration, bufferingMode);
		try {
			this.ontology = rootOntology;
			manager = OWLManager.createOWLOntologyManager();
			dataFactory = manager.getOWLDataFactory();
		} catch (UnknownOWLOntologyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public ExpressionMaterializingReasoner(OWLOntology ont) {
		this(ont, new SimpleConfiguration(), BufferingMode.BUFFERING);
	}

	public OWLReasoner getWrappedReasoner() {
		return wrappedReasoner;
	}



	public void setWrappedReasoner(OWLReasoner wrappedReasoner) {
		this.wrappedReasoner = wrappedReasoner;
	}


	public void materializeExpressions(OWLOntology ontology) {
		this.ontology = ontology;
		materializeExpressions();
	}

	public void materializeExpressions() {
		pxMap = new HashMap<OWLObjectProperty,OWLClass>();
		cxMap = new HashMap<OWLClass, OWLClassExpression>();
		for (OWLClass baseClass : ontology.getClassesInSignature()) {
			for (OWLObjectProperty p : ontology.getObjectPropertiesInSignature()) {
				OWLObjectSomeValuesFrom x = dataFactory.getOWLObjectSomeValuesFrom(p, baseClass);
				IRI xciri = IRI.create(baseClass.getIRI().toString()+"--"+p.getIRI().toString());
				OWLClass xc = dataFactory.getOWLClass(xciri);
				OWLEquivalentClassesAxiom eca = dataFactory.getOWLEquivalentClassesAxiom(xc, x);
				pxMap.put(p, xc);
				cxMap.put(xc, x);
				//LOG.info("Materializing: "+eca);
				manager.addAxiom(ontology, eca);
				manager.addAxiom(ontology, dataFactory.getOWLDeclarationAxiom(xc));
			}
		}
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

		Set<OWLClassExpression> ces = new HashSet<OWLClassExpression>();
		for (OWLClass c : wrappedReasoner.getSuperClasses(ce, direct).getFlattened()) {
			LOG.info("SC:"+c);
			if (cxMap.containsKey(c)) {
				ces.add(cxMap.get(c));
			}
			else {
				ces.add(c);
			}
		}
		return ces;
	}

	public String getReasonerName() {
		return "Expression Materializing Reasoner";
	}

	public Version getReasonerVersion() {
		return wrappedReasoner.getReasonerVersion();
	}

	public BufferingMode getBufferingMode() {
		return wrappedReasoner.getBufferingMode();
	}

	public void flush() {
		wrappedReasoner.flush();
	}

	public List<OWLOntologyChange> getPendingChanges() {
		return wrappedReasoner.getPendingChanges();
	}

	public Set<OWLAxiom> getPendingAxiomAdditions() {
		return wrappedReasoner.getPendingAxiomAdditions();
	}

	public Set<OWLAxiom> getPendingAxiomRemovals() {
		return wrappedReasoner.getPendingAxiomRemovals();
	}

	public OWLOntology getRootOntology() {
		return wrappedReasoner.getRootOntology();
	}

	public void interrupt() {
		wrappedReasoner.interrupt();
	}

	public void precomputeInferences(InferenceType... inferenceTypes)
	throws ReasonerInterruptedException, TimeOutException,
	InconsistentOntologyException {
		wrappedReasoner.precomputeInferences(inferenceTypes);
	}

	public boolean isPrecomputed(InferenceType inferenceType) {
		return wrappedReasoner.isPrecomputed(inferenceType);
	}

	public Set<InferenceType> getPrecomputableInferenceTypes() {
		return wrappedReasoner.getPrecomputableInferenceTypes();
	}

	public boolean isConsistent() throws ReasonerInterruptedException,
	TimeOutException {
		return wrappedReasoner.isConsistent();
	}

	public boolean isSatisfiable(OWLClassExpression classExpression)
	throws ReasonerInterruptedException, TimeOutException,
	ClassExpressionNotInProfileException, FreshEntitiesException,
	InconsistentOntologyException {
		return wrappedReasoner.isSatisfiable(classExpression);
	}

	public Node<OWLClass> getUnsatisfiableClasses()
	throws ReasonerInterruptedException, TimeOutException,
	InconsistentOntologyException {
		return wrappedReasoner.getUnsatisfiableClasses();
	}

	public boolean isEntailed(OWLAxiom axiom)
	throws ReasonerInterruptedException,
	UnsupportedEntailmentTypeException, TimeOutException,
	AxiomNotInProfileException, FreshEntitiesException,
	InconsistentOntologyException {
		return wrappedReasoner.isEntailed(axiom);
	}

	public boolean isEntailed(Set<? extends OWLAxiom> axioms)
	throws ReasonerInterruptedException,
	UnsupportedEntailmentTypeException, TimeOutException,
	AxiomNotInProfileException, FreshEntitiesException,
	InconsistentOntologyException {
		return wrappedReasoner.isEntailed(axioms);
	}

	public boolean isEntailmentCheckingSupported(AxiomType<?> axiomType) {
		return wrappedReasoner.isEntailmentCheckingSupported(axiomType);
	}

	public Node<OWLClass> getTopClassNode() {
		return wrappedReasoner.getTopClassNode();
	}

	public Node<OWLClass> getBottomClassNode() {
		return wrappedReasoner.getBottomClassNode();
	}

	public NodeSet<OWLClass> getSubClasses(OWLClassExpression ce, boolean direct)
	throws ReasonerInterruptedException, TimeOutException,
	FreshEntitiesException, InconsistentOntologyException,
	ClassExpressionNotInProfileException {
		return wrappedReasoner.getSubClasses(ce, direct);
	}

	public NodeSet<OWLClass> getSuperClasses(OWLClassExpression ce,
			boolean direct) throws InconsistentOntologyException,
			ClassExpressionNotInProfileException, FreshEntitiesException,
			ReasonerInterruptedException, TimeOutException {
		return wrappedReasoner.getSuperClasses(ce, direct);
	}

	public Node<OWLClass> getEquivalentClasses(OWLClassExpression ce)
	throws InconsistentOntologyException,
	ClassExpressionNotInProfileException, FreshEntitiesException,
	ReasonerInterruptedException, TimeOutException {
		return wrappedReasoner.getEquivalentClasses(ce);
	}

	public NodeSet<OWLClass> getDisjointClasses(OWLClassExpression ce)
	throws ReasonerInterruptedException, TimeOutException,
	FreshEntitiesException, InconsistentOntologyException {
		return wrappedReasoner.getDisjointClasses(ce);
	}

	public Node<OWLObjectPropertyExpression> getTopObjectPropertyNode() {
		return wrappedReasoner.getTopObjectPropertyNode();
	}

	public Node<OWLObjectPropertyExpression> getBottomObjectPropertyNode() {
		return wrappedReasoner.getBottomObjectPropertyNode();
	}

	public NodeSet<OWLObjectPropertyExpression> getSubObjectProperties(
			OWLObjectPropertyExpression pe, boolean direct)
			throws InconsistentOntologyException, FreshEntitiesException,
			ReasonerInterruptedException, TimeOutException {
		return wrappedReasoner.getSubObjectProperties(pe, direct);
	}

	public NodeSet<OWLObjectPropertyExpression> getSuperObjectProperties(
			OWLObjectPropertyExpression pe, boolean direct)
			throws InconsistentOntologyException, FreshEntitiesException,
			ReasonerInterruptedException, TimeOutException {
		return wrappedReasoner.getSuperObjectProperties(pe, direct);
	}

	public Node<OWLObjectPropertyExpression> getEquivalentObjectProperties(
			OWLObjectPropertyExpression pe)
			throws InconsistentOntologyException, FreshEntitiesException,
			ReasonerInterruptedException, TimeOutException {
		return wrappedReasoner.getEquivalentObjectProperties(pe);
	}

	public NodeSet<OWLObjectPropertyExpression> getDisjointObjectProperties(
			OWLObjectPropertyExpression pe)
			throws InconsistentOntologyException, FreshEntitiesException,
			ReasonerInterruptedException, TimeOutException {
		return wrappedReasoner.getDisjointObjectProperties(pe);
	}

	public Node<OWLObjectPropertyExpression> getInverseObjectProperties(
			OWLObjectPropertyExpression pe)
			throws InconsistentOntologyException, FreshEntitiesException,
			ReasonerInterruptedException, TimeOutException {
		return wrappedReasoner.getInverseObjectProperties(pe);
	}

	public NodeSet<OWLClass> getObjectPropertyDomains(
			OWLObjectPropertyExpression pe, boolean direct)
			throws InconsistentOntologyException, FreshEntitiesException,
			ReasonerInterruptedException, TimeOutException {
		return wrappedReasoner.getObjectPropertyDomains(pe, direct);
	}

	public NodeSet<OWLClass> getObjectPropertyRanges(
			OWLObjectPropertyExpression pe, boolean direct)
			throws InconsistentOntologyException, FreshEntitiesException,
			ReasonerInterruptedException, TimeOutException {
		return wrappedReasoner.getObjectPropertyRanges(pe, direct);
	}

	public Node<OWLDataProperty> getTopDataPropertyNode() {
		return wrappedReasoner.getTopDataPropertyNode();
	}

	public Node<OWLDataProperty> getBottomDataPropertyNode() {
		return wrappedReasoner.getBottomDataPropertyNode();
	}

	public NodeSet<OWLDataProperty> getSubDataProperties(OWLDataProperty pe,
			boolean direct) throws InconsistentOntologyException,
			FreshEntitiesException, ReasonerInterruptedException,
			TimeOutException {
		return wrappedReasoner.getSubDataProperties(pe, direct);
	}

	public NodeSet<OWLDataProperty> getSuperDataProperties(OWLDataProperty pe,
			boolean direct) throws InconsistentOntologyException,
			FreshEntitiesException, ReasonerInterruptedException,
			TimeOutException {
		return wrappedReasoner.getSuperDataProperties(pe, direct);
	}

	public Node<OWLDataProperty> getEquivalentDataProperties(OWLDataProperty pe)
	throws InconsistentOntologyException, FreshEntitiesException,
	ReasonerInterruptedException, TimeOutException {
		return wrappedReasoner.getEquivalentDataProperties(pe);
	}

	public NodeSet<OWLDataProperty> getDisjointDataProperties(
			OWLDataPropertyExpression pe) throws InconsistentOntologyException,
			FreshEntitiesException, ReasonerInterruptedException,
			TimeOutException {
		return wrappedReasoner.getDisjointDataProperties(pe);
	}

	public NodeSet<OWLClass> getDataPropertyDomains(OWLDataProperty pe,
			boolean direct) throws InconsistentOntologyException,
			FreshEntitiesException, ReasonerInterruptedException,
			TimeOutException {
		return wrappedReasoner.getDataPropertyDomains(pe, direct);
	}

	public NodeSet<OWLClass> getTypes(OWLNamedIndividual ind, boolean direct)
	throws InconsistentOntologyException, FreshEntitiesException,
	ReasonerInterruptedException, TimeOutException {
		return wrappedReasoner.getTypes(ind, direct);
	}

	public NodeSet<OWLNamedIndividual> getInstances(OWLClassExpression ce,
			boolean direct) throws InconsistentOntologyException,
			ClassExpressionNotInProfileException, FreshEntitiesException,
			ReasonerInterruptedException, TimeOutException {
		return wrappedReasoner.getInstances(ce, direct);
	}

	public NodeSet<OWLNamedIndividual> getObjectPropertyValues(
			OWLNamedIndividual ind, OWLObjectPropertyExpression pe)
			throws InconsistentOntologyException, FreshEntitiesException,
			ReasonerInterruptedException, TimeOutException {
		return wrappedReasoner.getObjectPropertyValues(ind, pe);
	}

	public Set<OWLLiteral> getDataPropertyValues(OWLNamedIndividual ind,
			OWLDataProperty pe) throws InconsistentOntologyException,
			FreshEntitiesException, ReasonerInterruptedException,
			TimeOutException {
		return wrappedReasoner.getDataPropertyValues(ind, pe);
	}

	public Node<OWLNamedIndividual> getSameIndividuals(OWLNamedIndividual ind)
	throws InconsistentOntologyException, FreshEntitiesException,
	ReasonerInterruptedException, TimeOutException {
		return wrappedReasoner.getSameIndividuals(ind);
	}

	public NodeSet<OWLNamedIndividual> getDifferentIndividuals(
			OWLNamedIndividual ind) throws InconsistentOntologyException,
			FreshEntitiesException, ReasonerInterruptedException,
			TimeOutException {
		return wrappedReasoner.getDifferentIndividuals(ind);
	}

	public long getTimeOut() {
		return wrappedReasoner.getTimeOut();
	}

	public FreshEntityPolicy getFreshEntityPolicy() {
		return wrappedReasoner.getFreshEntityPolicy();
	}

	public IndividualNodeSetPolicy getIndividualNodeSetPolicy() {
		return wrappedReasoner.getIndividualNodeSetPolicy();
	}

	public void dispose() {
		wrappedReasoner.dispose();
	}

	@Override
	protected void handleChanges(Set<OWLAxiom> addAxioms,
			Set<OWLAxiom> removeAxioms) {
		// TODO Auto-generated method stub

	}


}
