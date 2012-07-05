package owltools.sim;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import owltools.io.OWLPrettyPrinter;
import owltools.mooncat.PropertyViewOntologyBuilder;

public class SimpleOwlSim {
	
	private Logger LOG = Logger.getLogger(SimpleOwlSim.class);
	
	public OWLPrettyPrinter owlpp = null;
	Set<OWLObjectProperty> viewProperties;
	private OWLDataFactory owlDataFactory;
	private OWLOntologyManager owlOntologyManager;
	private OWLReasonerFactory reasonerFactory;
	OWLReasoner reasoner;
	private Set<OWLClass> fixedAttributeClasses = null;
	final String OBO_PREFIX = "http://purl.obolibrary.org/obo/";

	private OWLOntology sourceOntology;       
	private OWLOntology associationOntology;       
	
	Map<OWLEntity,Set<OWLClassExpression>> elementToAttributesMap = new HashMap<OWLEntity,Set<OWLClassExpression>>();
	Map<OWLClassExpression,Set<OWLEntity>> attributeToElementsMap = new HashMap<OWLClassExpression,Set<OWLEntity>>();

	public SimpleOwlSim(OWLOntology sourceOntology) {
		super();
		this.sourceOntology = sourceOntology;
		this.owlOntologyManager = sourceOntology.getOWLOntologyManager();
		this.owlDataFactory = owlOntologyManager.getOWLDataFactory();
		this.sourceOntology = sourceOntology;
		init();
	}

	private void init() {
		reasonerFactory = new ElkReasonerFactory();
	}



	public Set<OWLObjectProperty> getViewProperties() {
		if (viewProperties == null)
			return getAllObjectProperties();
		else
			return viewProperties;
	}

	public void setViewProperties(Set<OWLObjectProperty> viewProperties) {
		this.viewProperties = viewProperties;
	}
	
	public void addViewProperty(OWLObjectProperty viewProperty) {
		if (viewProperties == null)
			viewProperties = new HashSet<OWLObjectProperty>();
		viewProperties.add(viewProperty);
	}
	
	public void addViewProperty(IRI iri) {
		addViewProperty(owlDataFactory.getOWLObjectProperty(iri));
	}


	private Set<OWLObjectProperty> getAllObjectProperties() {
		return sourceOntology.getObjectPropertiesInSignature();
	}

	/**
	 * for each pi in P, generate an ontology O(P) using a PVOB, i.e. C' = P some C for all C in O.
	 * reasoner over that ontology, remove non-subsuming expressions, add all axioms
	 * to source ontology
	 * 
	 * @throws OWLOntologyCreationException
	 */
	public void prepareOntology() throws OWLOntologyCreationException {

		Set<OWLAxiom> allAxioms = new HashSet<OWLAxiom>();
		for (OWLObjectProperty viewProperty : getViewProperties()) {
			LOG.info("Building view for "+viewProperty);
			PropertyViewOntologyBuilder pvob = 
				new PropertyViewOntologyBuilder(sourceOntology,
						sourceOntology,
						viewProperty);

			pvob.setClassifyIndividuals(false);
			pvob.setFilterUnused(true);
			//pvob.setViewLabelPrefix("involves ");
			pvob.setAssumeOBOStyleIRIs(false);
			pvob.build(reasonerFactory);

			// include axioms in AVO for view subset
			// (i.e. filter out useless classes that do not classify anything)
			for (OWLAxiom ax : pvob.getAssertedViewOntology().getAxioms()) {
				for (OWLClass c : ax.getClassesInSignature()) {
					if (pvob.getViewEntities().contains(c)) {
						allAxioms.add(ax);
						//System.out.println("ADD:"+ax);
						break;
					}
				}
			}
			for (OWLEntity e : pvob.getViewEntities()) {
				//System.out.println(" E:"+e);
				allAxioms.add(owlDataFactory.getOWLDeclarationAxiom(e));
				allAxioms.addAll(e.getAnnotationAssertionAxioms(pvob.getAssertedViewOntology()));
			}
			LOG.info("VIEW_SIZE: "+pvob.getViewEntities().size()+" for "+viewProperty);
		}
		
		owlOntologyManager.addAxioms(sourceOntology, allAxioms);
		LOG.info("Added "+allAxioms.size()+" axioms");
		
		reason();
	}
	

	public OWLReasoner getReasoner() {
		if (reasoner == null) {
			reason();
		}
		return reasoner;
	}

	public void setReasoner(OWLReasoner reasoner) {
		this.reasoner = reasoner;
	}

	public void reason() {
		LOG.info("Preparing to reason...");
		reasoner = reasonerFactory.createReasoner(sourceOntology);
		reasoner.precomputeInferences(InferenceType.values()); // Elk only?
		LOG.info("Pre-reasoned...");		
	}
	
	public void createSimOnt() throws OWLOntologyCreationException {
		this.prepareOntology();
		this.makeAllByAllLowestCommonSubsumer();
		reason();
	}

	// ----------- ----------- ----------- -----------
	// SUBSUMERS AND LOWEST COMMON SUBSUMERS
	// ----------- ----------- ----------- -----------
	
	public Set<Node<OWLClass>> getNamedSubsumers(OWLClassExpression a) {
		return getReasoner().getSuperClasses(a, false).getNodes();
	}

	public Set<Node<OWLClass>> getNamedReflexiveSubsumers(OWLClassExpression a) {
		// consider cacheing
		Set<Node<OWLClass>> nodes =  getReasoner().getSuperClasses(a, false).getNodes();
		nodes.add(getReasoner().getEquivalentClasses(a));
		return nodes;
	}

	public Set<Node<OWLClass>> getNamedCommonSubsumers(OWLClassExpression a, OWLClassExpression b) {
		Set<Node<OWLClass>> nodes = getNamedReflexiveSubsumers(a);
		nodes.retainAll(getNamedReflexiveSubsumers(b));
		return nodes;
	}
	
	public Set<Node<OWLClass>>  getNamedLowestCommonSubsumers(OWLClassExpression a, OWLClassExpression b) {
		Set<Node<OWLClass>> nodes = getNamedCommonSubsumers(a, b);
		Set<Node<OWLClass>> rNodes = new HashSet<Node<OWLClass>>();
		for (Node<OWLClass> node : nodes) {
			rNodes.addAll(getReasoner().getSuperClasses(node.getRepresentativeElement(), false).getNodes());
		}
		nodes.removeAll(rNodes);
		return nodes;
	}

	public OWLClassExpression getLowestCommonSubsumer(OWLClassExpression a, OWLClassExpression b) {
		if (a.equals(b)) {
			return a;
		}
		if (a instanceof OWLClass && b instanceof OWLClass) {
			if (getReasoner().getSuperClasses(a, false).getFlattened().contains(b)) {
				return b;
			}
			if (getReasoner().getSuperClasses(b, false).getFlattened().contains(a)) {
				return a;
			}
		}
		Set<Node<OWLClass>> ccs = getNamedLowestCommonSubsumers(a,b);
		if (ccs.size() == 1) {
			return ccs.iterator().next().getRepresentativeElement();
		}
		
		Set<OWLClass> ops = new HashSet<OWLClass>();
		for (Node<OWLClass> n : ccs) {
			// TODO: custom filtering
			ops.add(n.getRepresentativeElement());
		}
		
		// RESTORE CODE AFTER IMPLEMENTING FILTERING:
		//if (ops.size() == 1) {
		//	return ops.iterator().next();
		//}
		return owlDataFactory.getOWLObjectIntersectionOf(ops);
	}
	
	/**
	 * generates a LCS expression and makes it a class if it is a class expression
	 * 
	 * @param a
	 * @param b
	 * @return named class representing LCS
	 */
	public OWLClass makeLowestCommonSubsumerClass(OWLClassExpression a, OWLClassExpression b) {
		OWLClassExpression x = getLowestCommonSubsumer(a,b);
		if (x instanceof OWLClass)
			return (OWLClass)x;
		else if (x instanceof OWLObjectIntersectionOf)
			return makeClass((OWLObjectIntersectionOf) x);
		else
			return null;
	}
	
	/**
	 * @param a
	 * @param b
	 * @return SimJ of two attribute classes
	 */
	public float getAttributeJaccardSimilarity(OWLClassExpression a, OWLClassExpression b) {
		Set<Node<OWLClass>> ci = getNamedCommonSubsumers(a,b);
		Set<Node<OWLClass>> cu = getNamedReflexiveSubsumers(a);
		cu.addAll(getNamedReflexiveSubsumers(b));
		return ci.size() / (float)cu.size();
	}
	


	/**
	 * returns all attribute classes - i.e. the classes used to annotate the elements (genes, diseases, etc)
	 * being studied
	 * 
	 *  defaults to all classes in source ontology signature
	 * 
	 * @return
	 */
	public Set<OWLClass> getAllAttributeClasses() {
		if (fixedAttributeClasses == null)
			return sourceOntology.getClassesInSignature(true);
		else
			return fixedAttributeClasses;
	}
	
	public void setAttributesFromOntology(OWLOntology o) {
		fixedAttributeClasses = o.getClassesInSignature();
	}
	
	public void createElementAttributeMapFromOntology() {
		for (OWLNamedIndividual e : sourceOntology.getIndividualsInSignature(true)) {
			
		}
	}
	
	public Set<OWLClass> getAttributesForElement(OWLEntity e) {
		return null; // TODO
	}
	
	public Set<OWLEntity> getElementsForAttribute(OWLClass c) {
		return null; // TODO
	}
	
	public int getNumElementsForAttribute(OWLClass c) {
		return getElementsForAttribute(c).size();
	}
	
	public Set<OWLEntity> getAllElements() {
		return null; // TODO
	}
	public int getCorpusSize() {
		return getAllElements().size();
	}

	public Double getInformationContentForAttribute(OWLClass c) {
		int freq = getNumElementsForAttribute(c);
		Double ic = null;
		if (freq > 0) {
			ic = -Math.log(((double) (freq) / getCorpusSize())) / Math.log(2);
		}
		return ic;
	}

	public void getAllByAllLowestCommonSubsumer() {
		for (OWLClass a : getAllAttributeClasses()) {
			for (OWLClass b : getAllAttributeClasses()) {
				OWLClassExpression x = getLowestCommonSubsumer(a,b);
				System.out.println("LCS( "+a+" , "+b+" ) = "+x);
			}
		}
	}
	
	public void makeAllByAllLowestCommonSubsumer() {
		LOG.info("all x all...");		

		Set<OWLClass> atts = getAllAttributeClasses();
		for (OWLClass a : atts) {
			LOG.info("  "+a+" vs ALL");		
			for (OWLClass b : atts) {
				// LCS operation is symmetric, only pre-compute one way
				if (a.compareTo(b) > 0) {
					OWLClass lcs = this.makeLowestCommonSubsumerClass(a, b);
					//System.out.println("LCS( "+pp(a)+" , "+pp(b)+" ) = "+pp(lcs));
				}
			}
		}
		LOG.info("DONE all x all");		
		reason();
	}

	
	/**
	 * given a CE of the form A1 and A2 and ... An,
	 * get a class with an IRI formed by concatenating parts of the IRIs of Ai,
	 * and create a label assertion, where the label is formed by concatenating label(A1)....
	 * 
	 * note that the reasoner will need to be synchronized after new classes are made
	 * 
	 * @param x
	 * @return
	 */
	public OWLClass makeClass(OWLObjectIntersectionOf x) {
		StringBuffer id = new StringBuffer();
		StringBuffer label = new StringBuffer();
		int n = 0;
		int nlabels = 0;
		for (OWLClassExpression op : x.getOperands()) {
			n++;
			// throw exception if ops are not named
			OWLClass opc = (OWLClass)op;
			String opid = opc.getIRI().toString();
			String oplabel = getLabel(opc);
			
			
			if (n == 1) {
				// make base
				String prefix = opid.toString();
				prefix = prefix.replaceAll("#.*","#");
				if (prefix.startsWith(OBO_PREFIX))
					id.append(OBO_PREFIX);
				else 
					id.append(prefix);				
			}

			if (n > 1) {
				label.append(" and ");
			}
			if (oplabel != null) {
				nlabels++;
				label.append(oplabel);
			}
			else {
				label.append("?"+opid);
			}


			opid = opid.replaceAll(".*#", "");
			opid = opid.replaceAll(".*\\/", "");
			if (n > 1) {
				id.append("-and-");
			}

			id.append(opid);
		}
		OWLClass c = owlDataFactory.getOWLClass(IRI.create(id.toString()));
		Set<OWLAxiom> newAxioms = new HashSet<OWLAxiom>();
		newAxioms.add( owlDataFactory.getOWLEquivalentClassesAxiom(c, x) );
		newAxioms.add( owlDataFactory.getOWLDeclarationAxiom(c));
		//LOG.info(" Generated label("+c+")="+label.toString());
		if (nlabels > 0) {
			newAxioms.add( owlDataFactory.getOWLAnnotationAssertionAxiom(owlDataFactory.getRDFSLabel(), c.getIRI(), 
					owlDataFactory.getOWLLiteral(label.toString())));
		}
		owlOntologyManager.addAxioms(sourceOntology, newAxioms);
		return c;
	}
	
	private String getLabel(OWLEntity e) {
		String label = null;		
		// todo - ontology import closure
		for (OWLAnnotation ann : e.getAnnotations(sourceOntology, owlDataFactory.getRDFSLabel())) {
			OWLAnnotationValue v = ann.getValue();
			if (v instanceof OWLLiteral) {
				label = ((OWLLiteral)v).getLiteral();
				break;
			}
		}
		return label;
	}






	private String pp(OWLClass a) {
		if (owlpp == null)
			return a.toString();
		else
			return owlpp.render(a);
	}

	
}
