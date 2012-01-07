package owltools.mooncat;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

/*
 * a PropertyViewOntology (PVO) is a materialized view over a Source Ontology (SO) using an object property P, 
 * such that for every class C in SO there is a class C' in PVO, together with an axiom
 * C' = P some C
 * 
 * Each class C' in PVO is optionally named automatically according to some template.
 * 
 * An Inferred PVO (IPVO) is the materialized inferred subclass axioms from PVO plus the SO plus an ontology
 * containing zero or more entities that are to be classified using the PVO.
 * 
 * For example, if the SO is an anatomy ontology (which includes part_of relations) then the PVO
 * defined using the part_of object property, and we use a naming scheme "<X> part", the the PVO subsumption
 * hierarchy will look like this:
 * 
 *  body part
 *    limb part
 *      forelimb part
 *        hand part
 *          finger part
 *            phalanx part
 * 
 * Alternatively, if we define an object property [expressed_in <- expressed_in o part_of], we can use
 * this to build a IPVO that directly classifies genes. E.h. if g1 SubClassOf expressed_in some hand,
 * then g1 will be inferred to be a subclass of "limb gene", if "limb gene" is equivalent to
 * [expressed_in some limb]
 * 
 * One use for this is building Solr indexes - each IPVO would correspond to a distinct facet 
 * 
 * 
 * @author cjm
 *
 */

public class PropertyViewOntologyBuilder {

	private Logger LOG = Logger.getLogger(PropertyViewOntologyBuilder.class);

	private OWLDataFactory owlDataFactory;
	private OWLOntologyManager owlOntologyManager;
	private OWLOntology sourceOntology;
	private OWLOntology unitsOntology;
	private OWLOntology assertedViewOntology;
	private OWLOntology inferredViewOntology;
	private OWLObjectProperty viewProperty;
	private boolean isUseOriginalClassIRIs = false;
	private boolean isClassifyIndividuals = false;
	private String viewLabelPrefix="";
	private String viewLabelSuffix="";
	private Set<OWLEntity> viewEntities;
	private OWLClass viewRootClass;

	/**
	 * @param sourceOntology
	 * @param unitsOntology
	 * @param viewProperty
	 */
	public PropertyViewOntologyBuilder(OWLOntology sourceOntology,
			OWLOntology unitsOntology, OWLObjectProperty viewProperty) {
		super();
		this.owlOntologyManager = OWLManager.createOWLOntologyManager();
		this.owlDataFactory = owlOntologyManager.getOWLDataFactory();
		this.sourceOntology = sourceOntology;
		this.unitsOntology = unitsOntology;
		this.viewProperty = viewProperty;
		init();
	}

	/**
	 * @param owlDataFactory
	 * @param owlOntologyManager
	 * @param sourceOntology
	 * @param unitsOntology
	 * @param viewProperty
	 */
	public PropertyViewOntologyBuilder(OWLDataFactory owlDataFactory,
			OWLOntologyManager owlOntologyManager, OWLOntology sourceOntology,
			OWLOntology unitsOntology, OWLObjectProperty viewProperty) {
		super();
		this.owlDataFactory = owlDataFactory;
		this.owlOntologyManager = owlOntologyManager;
		this.sourceOntology = sourceOntology;
		this.unitsOntology = unitsOntology;
		this.viewProperty = viewProperty;
		init();
	}

	private void init() {
		viewLabelPrefix = getAnyLabel(viewProperty)+" a ";
		viewEntities = new HashSet<OWLEntity>();
	}



	/**
	 * Automatically generated Property View Ontology,
	 * containing axioms C' == P some C, for each C in source ontology
	 * 
	 * @return
	 */
	public OWLOntology getAssertedViewOntology() {
		return assertedViewOntology;
	}

	public void setAssertedViewOntology(OWLOntology assertedViewOntology) {
		this.assertedViewOntology = assertedViewOntology;
	}

	/**
	 * Generated after running {@link buildInferredViewOntology}.
	 * 
	 * Can be the same as assertedViewOntology - in which case both the assertions
	 * and the inferences go in the same ontology
	 * 
	 * @return
	 */
	public OWLOntology getInferredViewOntology() {
		return inferredViewOntology;
	}

	public void setInferredViewOntology(OWLOntology inferredViewOntology) {
		this.inferredViewOntology = inferredViewOntology;
	}

	/**
	 * The set of all entities in the view ontology
	 * 
	 * @return
	 */
	public Set<OWLEntity> getViewEntities() {
		return viewEntities;
	}

	public void setViewEntities(Set<OWLEntity> viewEntities) {
		this.viewEntities = viewEntities;
	}

	public void addViewEntities(Set<OWLEntity> newEntities) {
		viewEntities.addAll(newEntities);
	}

	/**
	 * typically [P some owl:Thing]
	 * @return
	 */
	public OWLClass getViewRootClass() {
		return viewRootClass;
	}

	public String getViewLabelPrefix() {
		return viewLabelPrefix;
	}

	public void setViewLabelPrefix(String viewLabelPrefix) {
		this.viewLabelPrefix = viewLabelPrefix;
	}

	public String getViewLabelSuffix() {
		return viewLabelSuffix;
	}

	public void setViewLabelSuffix(String viewLabelSuffix) {
		this.viewLabelSuffix = viewLabelSuffix;
	}

	public void setViewLabelPrefixAndSuffix(String viewLabelPrefix, String viewLabelSuffix) {
		this.viewLabelPrefix = viewLabelPrefix;
		this.viewLabelSuffix = viewLabelSuffix;
	}

	public void buildViewOntology() throws OWLOntologyCreationException {
		buildViewOntology((new OWLOntologyID()).getOntologyIRI(), 
				(new OWLOntologyID()).getOntologyIRI());
	}

	/**
	 * Constructs view ontology (PVO) from source ontology (SO), such that every class C in SO
	 * has a corresponding view class C', such that C' == P some C
	 * 
	 * The PVO imports both the SO, and the elements ontology
	 * 
	 * Also prepares the inferred view ontology (IPVO)
	 * 
	 * You must call buildInferredViewOntology yourself
	 * (because you need to set up the reasoner object yourself)
	 * 
	 * @param avoIRI
	 * @param ivoIRI
	 * @throws OWLOntologyCreationException
	 */
	public void buildViewOntology(IRI avoIRI, IRI ivoIRI) throws OWLOntologyCreationException {
		Set<OWLOntology> imports = new HashSet<OWLOntology>();
		imports.add(sourceOntology);
		imports.add(unitsOntology);
		// the AVO includes the source ontology and elemental ontology in its imports
		//  - when we reason, we need axioms from all
		assertedViewOntology = owlOntologyManager.createOntology(avoIRI, imports);
		if (avoIRI.equals(ivoIRI))
			inferredViewOntology = assertedViewOntology;
		else
			inferredViewOntology = owlOntologyManager.createOntology(ivoIRI);

		Set<OWLClass> sourceClasses = sourceOntology.getClassesInSignature();
		OWLClass thing = owlDataFactory.getOWLThing();
		sourceClasses.add(thing);
		for (OWLClass c : sourceClasses) {
			IRI vcIRI = makeViewClassIRI(c);
			OWLClass vc = owlDataFactory.getOWLClass(vcIRI);
			OWLObjectSomeValuesFrom vx = owlDataFactory.getOWLObjectSomeValuesFrom(viewProperty, c);
			OWLEquivalentClassesAxiom eca = 
				owlDataFactory.getOWLEquivalentClassesAxiom(vc, vx);
			owlOntologyManager.addAxiom(assertedViewOntology, eca);
			if (c.equals(thing)) {
				viewRootClass = vc;
			}
		}		
	}


	/**
	 * Once the PVO has been constructed, this uses a reasoner to classify it. The inferred
	 * direct superclasses of all view entities are added to the IPVO
	 * 
	 *  In addition to classifying PVO, the elements ontology (e,g genes) is also classified.
	 *  
	 * 
	 * @param reasoner
	 */
	public void buildInferredViewOntology(OWLReasoner reasoner) {

		// todo - make this configurable
		// for now assume we include all entities that satisfy query
		OWLObjectSomeValuesFrom qx = owlDataFactory.getOWLObjectSomeValuesFrom(viewProperty, owlDataFactory.getOWLThing());

		// set up the list of entities we will classify - this is all classes in the PVO, as
		// well as any entities in the element ontology that can be classified directly by a class in PVO.
		// the easiest way to build this list is simply to find everything that instantiates or is subsumed
		// by SomeValuesFrom(P, owl:Thing)
		viewEntities = new HashSet<OWLEntity>();
		Set<OWLClass> srcClasses = this.sourceOntology.getClassesInSignature(true);
		for (OWLClass elementEntity : reasoner.getSubClasses(getViewRootClass(), false).getFlattened()) {
			if (elementEntity.equals(owlDataFactory.getOWLNothing()))
				continue;
			if (srcClasses.contains(elementEntity))
				continue;
			viewEntities.add(elementEntity);
		}
		LOG.info("making view for "+unitsOntology);
		if (isClassifyIndividuals && unitsOntology.getIndividualsInSignature(false).size() > 0) {
			// only attempt to look for individuals if the element ontology contains them
			// (remember, ELK 0.2.0 fails with individuals) 
			for (OWLNamedIndividual elementEntity : reasoner.getInstances(getViewRootClass(), false).getFlattened()) {
				viewEntities.add(elementEntity);
			}
		}

		for (OWLEntity e : viewEntities) {
			if (e instanceof OWLClass) {
				OWLClass c = (OWLClass) e;
				//System.out.println("C="+c);

				Set<OWLClass> ecs = reasoner.getEquivalentClasses(c).getEntities();
				for (OWLClass ec : ecs) {
					if (ec.equals(c))
						continue;
					if (viewEntities.contains(ec)) {
						LOG.info(c+" == "+ec);
						// TODO - allow for merging of equivalent classes
						OWLEquivalentClassesAxiom eca = owlDataFactory.getOWLEquivalentClassesAxiom(c, ec);
						owlOntologyManager.addAxiom(inferredViewOntology, eca);
					}
				}

				Set<OWLClass> scs = reasoner.getSuperClasses(c, true).getFlattened();
				for (OWLClass sc : scs) {
					if (viewEntities.contains(sc)) {
						OWLSubClassOfAxiom sca = owlDataFactory.getOWLSubClassOfAxiom(c, sc);
						owlOntologyManager.addAxiom(inferredViewOntology, sca);
					}
				}
			}
			else if (e instanceof OWLNamedIndividual){
				OWLNamedIndividual ind = (OWLNamedIndividual) e;
				Set<OWLClass> scs = reasoner.getTypes(ind, true).getFlattened();
				for (OWLClass sc : scs) {
					if (viewEntities.contains(sc)) {
						OWLClassAssertionAxiom sca = owlDataFactory.getOWLClassAssertionAxiom(sc, ind);
						owlOntologyManager.addAxiom(inferredViewOntology, sca);
					}
				}
			}
			else {
				// throw?
			}
		}
	}

	private IRI makeViewClassIRI(OWLClass c) {
		IRI vcIRI = c.getIRI();
		if (!isUseOriginalClassIRIs) {
			vcIRI = IRI.create(vcIRI.toString() + "_view");
			String vLabel = getViewLabel(c);
			if (vLabel != null) {
				OWLAnnotationAssertionAxiom aaa = owlDataFactory.getOWLAnnotationAssertionAxiom(owlDataFactory.getRDFSLabel(), vcIRI, 
						owlDataFactory.getOWLLiteral(vLabel));
				// anything derived -- including labels -- goes in the derived ontology
				owlOntologyManager.addAxiom(inferredViewOntology, aaa);
			}
		}
		return vcIRI;
	}

	private String getAnyLabel(OWLEntity c) {
		String label = null;
		// todo - ontology import closure
		for (OWLAnnotation ann : c.getAnnotations(sourceOntology, owlDataFactory.getRDFSLabel())) {
			OWLAnnotationValue v = ann.getValue();
			if (v instanceof OWLLiteral) {
				label = ((OWLLiteral)v).getLiteral();
				break;
			}
		}
		return label;
	}

	private String getViewLabel(OWLEntity c) {
		String label = getAnyLabel(c);
		if (label != null) {
			return viewLabelPrefix + label + viewLabelSuffix;
		}
		return null;
	}



}
