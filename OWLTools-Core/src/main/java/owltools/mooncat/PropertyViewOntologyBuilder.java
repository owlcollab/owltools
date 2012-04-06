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
 * This class will create a property view or *faceted* view over an ontology using a specified
 * property (relation).
 * 
 * One use for this is building Solr indexes - each property view ontology
 * would correspond to a distinct facet.
 * 
 * For example, a faceted view over samples classified using a cell type ontology might have 
 * individual classifications for sample-by-cell-lineage, sample-by-system, sample-by-disease
 *
 * <h2>Algorithm</h2>
 * 
 * A PropertyViewOntology O(P) is a materialized view over a Source Ontology O using an object property P, 
 * such that for every class C in O there is a class C(P) in O(P), together with an axiom
 * C(P) = P some C. In addition, O(P) imports O.
 * 
 * Each class C(P) in O(P) is optionally named automatically according to some template.
 * 
 * An Inferred PropertyViewOntology O(P)' is created by adding all direct inferred subclass axioms
 * between classes in O(P)', as well as inferred class assertion axioms.
 * 
 * Optionally, another import can be added to O(P) - this is where we would add an ontology
 * of elements to classified using classes in O(P). The elements may be individuals or classes.
 * We denote this as O(P,O^e) where O^e is the imported element ontology, and O(P,O^e)' is obtained
 * by finding all inferred direct subclass axioms between classes in O(P) or subclass/class assertion
 * axioms between entities i O^e and classes in O(P).
 * 
 * For example, if the O is an anatomy ontology (which includes partOf relations) including classes
 * {body, limb, forelimb, hand, ...} and axioms {forelimb SubClassOf limb, hand SubClassOf part_of finger},
 * then the O(partOf) will include only equivalence axioms, all of the form
 * {'forelimb part' = partOf some forelimb, hand part' = partOf some hand, etc}
 * (assuming we use a naming scheme where we suffix using " part").
 * 
 * After reasoning, the inferred property view ontology O(partOf)' will contain subclass axioms forming a
 * hierarchy like this:
 * 
 * <pre>
 *  body part
 *    limb part
 *      forelimb part
 *        hand part
 *          finger part
 *            phalanx part
 * </pre>
 * 
 * 
 * If we also have an additional element ontology G containing individuals {gene1, gene2, ...}
 * and class assertions {gene1 Type expressedIn some finger, ...}, where expressedIn is declared as
 * 
 *   expressedIn o partOf -> expressedIn
 *   
 * Then the combined subclass/class assertion hierarchy O(expressedIn,G)' will look like this: 
 * 
 * <pre>
 *  body gene
 *    limb gene
 *      forelimb gene
 *        hand gene
 *          finger gene
 *            gene1
 *            phalanx gene
 * </pre>
 * 
 * TODO - allow O(P)' to be exported as SKOS
 * 
 * @author cjm
 *
 */

public class PropertyViewOntologyBuilder {

	private Logger LOG = Logger.getLogger(PropertyViewOntologyBuilder.class);

	private OWLDataFactory owlDataFactory;
	private OWLOntologyManager owlOntologyManager;
	
	private OWLOntology sourceOntology;       // O
	private OWLOntology elementsOntology;     // O^E
	private OWLOntology assertedViewOntology; // O(P) or O(P,O^E)
	private OWLOntology inferredViewOntology; // O(P)' or O(P,O^E)'
	private OWLObjectProperty viewProperty;   // P
	
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
		this.elementsOntology = unitsOntology;
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
		this.elementsOntology = unitsOntology;
		this.viewProperty = viewProperty;
		init();
	}

	private void init() {
		viewLabelPrefix = getAnyLabel(viewProperty)+" a ";
		viewEntities = new HashSet<OWLEntity>();
	}



	/**
	 * Automatically generated Property View Ontology O(P)
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
	 * Generated after running {@link #buildInferredViewOntology(OWLReasoner)}
	 * 
	 * Note that O(P) and O(P)' can share the same object,
	 * i.e the assertedViewOntology is augmented to become the inferred view ontology
	 * 
	 * @return O(P)' or O(P,E)'
	 */
	public OWLOntology getInferredViewOntology() {
		return inferredViewOntology;
	}

	public void setInferredViewOntology(OWLOntology inferredViewOntology) {
		this.inferredViewOntology = inferredViewOntology;
	}

	/**
	 * @return The set of all entities in the view ontology O(P)
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
	 * As the we treat Thing as belonging to O, O(P) will contain "P some Thing", and this
	 * will be the root of O(P)
	 * 
	 * @return the root declared class of O(P)
	 */
	public OWLClass getViewRootClass() {
		return viewRootClass;
	}

	public String getViewLabelPrefix() {
		return viewLabelPrefix;
	}

	/**
	 * Set this to prefix all class labels in O(P)
	 * @param viewLabelPrefix
	 */
	public void setViewLabelPrefix(String viewLabelPrefix) {
		this.viewLabelPrefix = viewLabelPrefix;
	}

	public String getViewLabelSuffix() {
		return viewLabelSuffix;
	}

	/**
	 * Set this to suffix all class labels in O(P)
	 * @param viewLabelSuffix
	 */
	public void setViewLabelSuffix(String viewLabelSuffix) {
		this.viewLabelSuffix = viewLabelSuffix;
	}

	public void setViewLabelPrefixAndSuffix(String viewLabelPrefix, String viewLabelSuffix) {
		this.viewLabelPrefix = viewLabelPrefix;
		this.viewLabelSuffix = viewLabelSuffix;
	}

	/**
	 * As {@link #buildViewOntology(IRI, IRI)}, but both O(P) and O(P)' have automatically
	 * generated IRIs
	 * 
	 * @throws OWLOntologyCreationException
	 */
	public void buildViewOntology() throws OWLOntologyCreationException {
		buildViewOntology((new OWLOntologyID()).getOntologyIRI(), 
				(new OWLOntologyID()).getOntologyIRI());
	}

	/**
	 * Constructs a property view ontology O(P) or O(P,E) from source ontology O,
	 * such that every class C in O
	 * has a corresponding view class C' in O(P), such that C' EquivalentTo = P some C
	 * 
	 * O(P) imports both the O, and optionally the elements ontology E - in which case
	 * we call the ontology O(P,E).
	 * 
	 * As part of this procedre, an inferred property view ontology O(P)' or O(P,E)' is created
	 * 
	 * You must call buildInferredViewOntology yourself
	 * (because you need to set up the reasoner object yourself, and feed in O(P) as input)
	 * TODO - use reasonerfactory
	 * 
	 * E.g.
	 * <pre>
	 * 		pvob.buildViewOntology(IRI.create("http://x.org"), IRI.create("http://y.org"));
	 *		OWLOntology avo = pvob.getAssertedViewOntology();
	 *		OWLReasoner vr = reasonerFactory.createReasoner(avo);
	 *		pvob.buildInferredViewOntology(vr); 
	 * </pre>
	 * 
	 * @param avoIRI
	 * @param ivoIRI
	 * @throws OWLOntologyCreationException
	 */
	public void buildViewOntology(IRI avoIRI, IRI ivoIRI) throws OWLOntologyCreationException {
		Set<OWLOntology> imports = new HashSet<OWLOntology>();
		imports.add(sourceOntology);
		imports.add(elementsOntology);
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
		LOG.info("making view for "+elementsOntology);
		if (isClassifyIndividuals && elementsOntology.getIndividualsInSignature(false).size() > 0) {
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
