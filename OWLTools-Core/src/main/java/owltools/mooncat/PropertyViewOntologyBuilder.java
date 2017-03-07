package owltools.mooncat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.obolibrary.obo2owl.Obo2OWLConstants;
import org.obolibrary.obo2owl.OWLAPIOwl2Obo;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubPropertyChainOfAxiom;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import owltools.util.OwlHelper;

/**
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
 * <h2>Notes</h2>
 * 
 * If you have some 'leaf' element to classify - e.g. genes - make sure you pass in an
 * elementsOntology E. Otherwise these will go into O(P) as "P some gene123", which will
 * not classify as anything.
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
	private boolean isClassifyIndividuals = true;
	private boolean isFilterUnused = false;
	private boolean isAssumeOBOStyleIRIs = true;
	private String viewLabelPrefix="";
	private String viewLabelSuffix="";
	private Set<OWLEntity> viewEntities; // E
	private Set<OWLClass> leafClasses;
	private OWLClass viewRootClass;
	private boolean isExpandPropertyChain = false; // TODO - to ensure subclass closure reflects existential tree
	private boolean isCreateUnionClasses = false; // TODO
	private boolean isCreateReflexiveClasses = false;
	private Map<OWLClass,OWLClass> classToViewClass = new HashMap<OWLClass,OWLClass>();
	private Map<OWLClass,OWLClass> viewClassToClass = new HashMap<OWLClass,OWLClass>();
	private Set<OWLClass> excludedClasses = new HashSet<OWLClass>();
	
	/**
	 * @param sourceOntology
	 * @param elementsOntology
	 * @param viewProperty
	 */
	public PropertyViewOntologyBuilder(OWLOntology sourceOntology,
			OWLOntology elementsOntology, OWLObjectProperty viewProperty) {
		super();
		this.owlOntologyManager = sourceOntology.getOWLOntologyManager();
		this.owlDataFactory = owlOntologyManager.getOWLDataFactory();
		this.sourceOntology = sourceOntology;
		this.elementsOntology = elementsOntology;
		this.viewProperty = viewProperty;
		init();
	}
	
	public PropertyViewOntologyBuilder(OWLOntology sourceOntology, OWLObjectProperty viewProperty) {
		this(sourceOntology, sourceOntology, viewProperty);
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
	 * @return ontology
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



	public OWLOntology getElementsOntology() {
		return elementsOntology;
	}

	public void setElementsOntology(OWLOntology elementsOntology) {
		this.elementsOntology = elementsOntology;
	}



	public Set<OWLClass> getLeafClasses() {
		return leafClasses;
	}

	public void setLeafClasses(Set<OWLClass> leafClasses) {
		this.leafClasses = leafClasses;
	}
	
	

	public Set<OWLClass> getExcludedClasses() {
		return excludedClasses;
	}

	public void setExcludedClasses(Set<OWLClass> excludedClasses) {
		this.excludedClasses = excludedClasses;
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

	public boolean isUseOriginalClassIRIs() {
		return isUseOriginalClassIRIs;
	}

	public void setUseOriginalClassIRIs(boolean isUseOriginalClassIRIs) {
		this.isUseOriginalClassIRIs = isUseOriginalClassIRIs;
	}
	
	
	public boolean isCreateReflexiveClasses() {
		return isCreateReflexiveClasses;
	}

	public void setCreateReflexiveClasses(boolean isCreateReflexiveClasses) {
		this.isCreateReflexiveClasses = isCreateReflexiveClasses;
	}

	public boolean isClassifyIndividuals() {
		return isClassifyIndividuals;
	}

	public void setClassifyIndividuals(boolean isClassifyIndividuals) {
		this.isClassifyIndividuals = isClassifyIndividuals;
	}

	public boolean isFilterUnused() {
		return isFilterUnused;
	}

	public void setFilterUnused(boolean isFilterUnused) {
		this.isFilterUnused = isFilterUnused;
	}

	public boolean isAssumeOBOStyleIRIs() {
		return isAssumeOBOStyleIRIs;
	}

	/**
	 * set to false if IRIs are not OBO purls.
	 * 
	 * if true, then IRIs in O(P) will be formed by concatenating
	 * C and P IDs
	 * 
	 * @param isAssumeOBOStyleIRIs - default is true
	 */
	public void setAssumeOBOStyleIRIs(boolean isAssumeOBOStyleIRIs) {
		this.isAssumeOBOStyleIRIs = isAssumeOBOStyleIRIs;
	}

	/**
	 * @param reasonerFactory
	 * @throws OWLOntologyCreationException 
	 */
	public void build(OWLReasonerFactory reasonerFactory) throws OWLOntologyCreationException {
		buildViewOntology();
		OWLReasoner reasoner = reasonerFactory.createReasoner(assertedViewOntology);
		try {
			buildInferredViewOntology(reasoner);
		}
		finally {
			reasoner.dispose();
		}
	}

	/**
	 * As {@link #buildViewOntology(IRI, IRI)}, but both O(P) and O(P)' have automatically
	 * generated IRIs
	 * 
	 * @throws OWLOntologyCreationException
	 */
	public void buildViewOntology() throws OWLOntologyCreationException {
		buildViewOntology(IRI.generateDocumentIRI(),  IRI.generateDocumentIRI());
	}

	/**
	 * as {@link #buildViewOntology(IRI,IRI)}, but sets the asserted and inferred view
	 * ontologies to be the same
	 * 
	 * @param voIRI
	 * @throws OWLOntologyCreationException
	 */
	public void buildViewOntology(IRI voIRI) throws OWLOntologyCreationException {
		buildViewOntology(voIRI, voIRI);
	}

	/**
	 * Constructs a property view ontology O(P) or O(P,E) from source ontology O,
	 * such that every class C in O
	 * has a corresponding view class C' in O(P), such that C' EquivalentTo = P some C
	 * 
	 * O(P) imports both the O, and optionally the elements ontology E - in which case
	 * we call the ontology O(P,E).
	 * 
	 * As part of this procedure, an inferred property view ontology O(P)' or O(P,E)' is created
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
	 *      vr.dispose();
	 * </pre>
	 * 
	 * @param avoIRI
	 * @param ivoIRI
	 * @throws OWLOntologyCreationException
	 */
	public void buildViewOntology(IRI avoIRI, IRI ivoIRI) throws OWLOntologyCreationException {
		if (avoIRI == null) {
			avoIRI = IRI.generateDocumentIRI();
		}
		if (ivoIRI == null) {
			ivoIRI = IRI.generateDocumentIRI();
		}
		Set<OWLOntology> imports = new HashSet<OWLOntology>();
		imports.add(sourceOntology);
		if (!elementsOntology.equals(sourceOntology))
			imports.add(elementsOntology);
		LOG.info("imports="+imports);
		LOG.info("AVO="+avoIRI);
		LOG.info("IVO="+ivoIRI);
		// the AVO includes the source ontology and elemental ontology in its imports
		//  - when we reason, we need axioms from all
		assertedViewOntology = owlOntologyManager.createOntology(avoIRI, imports);
		if ((avoIRI == null && ivoIRI == null) || avoIRI.equals(ivoIRI))
			inferredViewOntology = assertedViewOntology;
		else
			inferredViewOntology = owlOntologyManager.createOntology(ivoIRI);
		LOG.info("O= "+sourceOntology);
		LOG.info("O(P)= "+assertedViewOntology);
		LOG.info("O(P) direct imports "+assertedViewOntology.getDirectImports());
		LOG.info("O(P) imports "+assertedViewOntology.getImports());
		LOG.info("O(P)'= "+inferredViewOntology);
		LOG.info("E= "+elementsOntology);

		// TODO: add subclass axiom for expansion of property chain
		Set<List<OWLObjectPropertyExpression>> chains = getPropertyChains(viewProperty);


		Set<OWLClass> sourceClasses = new HashSet<OWLClass>(sourceOntology.getClassesInSignature());
		OWLClass thing = owlDataFactory.getOWLThing();
		sourceClasses.add(thing);
		for (OWLClass c : sourceClasses) {
			if (this.getExcludedClasses().contains(c))
				continue;
			IRI vcIRI = makeViewClassIRI(c);
			setViewClassLabel(c, vcIRI, assertedViewOntology);
			OWLClass vc = owlDataFactory.getOWLClass(vcIRI);
			classToViewClass.put(c, vc);
			viewClassToClass.put(vc, c);
			//LOG.info("C -> C' : "+c+" -> "+vc);
			OWLObjectSomeValuesFrom vx = owlDataFactory.getOWLObjectSomeValuesFrom(viewProperty, c);
			OWLEquivalentClassesAxiom eca = 
				owlDataFactory.getOWLEquivalentClassesAxiom(vc, vx);
			//LOG.info("Adding equiv axiom to "+assertedViewOntology);
			owlOntologyManager.addAxiom(assertedViewOntology, eca);

			for (List<OWLObjectPropertyExpression> chain : chains) {
				OWLClassExpression sc = this.expandPropertyChain(chain, c);
				owlOntologyManager.addAxiom(assertedViewOntology, owlDataFactory.getOWLSubClassOfAxiom(vc, sc));
			}

			// each view ontology has a root "P some Thing"
			if (c.equals(thing)) {
				viewRootClass = vc;
			}
			
			// for SEP-like structures
			if (isCreateUnionClasses) {
				IRI ucIRI = null;
				OWLClass uc = owlDataFactory.getOWLClass(vcIRI);
				LOG.info("UnionOf(C C') -> "+uc);
				//OWLSubClassOfAxiom
				// TODO
			}
			
			// e.g. if P = part_of and C = lung, this simulates the reflexivity
			// of part_of by adding an axiom "lung part_of some lung".
			// compare:
			//  WITHOUT         |  WITH
			//  * organ         | * organ part [V]
			//   * lung         |  * organ
			//  * organ part    |   * lung
			//   * lung part    |  * lung part [V]
			//    * lung lobe   |   * lung lobe
			//                  |   * lung
			// note that the "entire structure" (E) is always a subclass of / more specific
			// than the grouping structure (S). This is not a full SEP pattern as
			// we do not have the proper part (P).
			if (isCreateReflexiveClasses) {
				OWLObjectSomeValuesFrom svf =
					owlDataFactory.getOWLObjectSomeValuesFrom(viewProperty, c);
				OWLSubClassOfAxiom sca = owlDataFactory.getOWLSubClassOfAxiom(c, svf);
				owlOntologyManager.addAxiom(assertedViewOntology, sca);
				//LOG.info("REFLEXIVE: "+sca);
			}
		}		
	}

	public Set<List<OWLObjectPropertyExpression>> getPropertyChains(OWLObjectProperty p) {
		LOG.info("Getting chains for: "+p);
		Set<List<OWLObjectPropertyExpression>> chains = new HashSet<List<OWLObjectPropertyExpression>>();
		for (OWLSubPropertyChainOfAxiom spca : sourceOntology.getAxioms(AxiomType.SUB_PROPERTY_CHAIN_OF)) {
			if (spca.getSuperProperty().equals(p)) {
				List<OWLObjectPropertyExpression> chain = spca.getPropertyChain();
				chains.add(chain);

				// note: limited form of cycle checking
				if (!chain.contains(p)) {
					chains.addAll(expandPropertyChain(chain));
				}
			}
		}
		LOG.info(p+" ==> "+chains);
		return chains;
	}

	public Set<List<OWLObjectPropertyExpression>> expandPropertyChain(List<OWLObjectPropertyExpression> chain) {
		LOG.info(" ExpandingChain: "+chain);
		return expandPropertyChain(chain, chain.size()-1);
	}
	public Set<List<OWLObjectPropertyExpression>> expandPropertyChain(List<OWLObjectPropertyExpression> chain, int pos) {

		Set<List<OWLObjectPropertyExpression>> returnSet = new HashSet<List<OWLObjectPropertyExpression>>();
		if (pos < 0) {
			// base case
			returnSet.add(new ArrayList<OWLObjectPropertyExpression>());
			return returnSet;
		}

		// expansion of current property. Either to itself (chain of length 1), or to n new chains
		Set<List<OWLObjectPropertyExpression>> extChains = null;
		OWLObjectPropertyExpression pe = chain.get(pos);
		if (pe instanceof OWLObjectProperty) {
			extChains = getPropertyChains((OWLObjectProperty) pe);
		}

		if (extChains == null || extChains.size() == 0) {
			// no rewrite - 
			Set<List<OWLObjectPropertyExpression>> headChains = expandPropertyChain(chain, pos-1);
			for (List<OWLObjectPropertyExpression> headChain : headChains) {
				headChain.add(pe); // append headChain + [p] --> newChain
			}
			returnSet = headChains;
		}
		else {
			for (List<OWLObjectPropertyExpression> extChain : extChains) {
				// create a new returnset for every way to rewrite the property
				Set<List<OWLObjectPropertyExpression>> headChains = expandPropertyChain(chain, pos-1);
				for (List<OWLObjectPropertyExpression> headChain : headChains) {
					headChain.addAll(extChain); // append headChain + extChain --> newChain
				}
				returnSet.addAll(headChains);
			}
		}

		return returnSet;
	}

	public OWLClassExpression expandPropertyChain(List<OWLObjectPropertyExpression> chain, OWLClassExpression t) {
		OWLClassExpression x = t;
		for (int i = chain.size()-1; i>=0;	i--) {
			x = this.owlDataFactory.getOWLObjectSomeValuesFrom(chain.get(i),t);
			t = x;
		}
		return x;
	}
	
	public OWLClass getOriginalClassForViewClass(OWLClass vc) {
		return viewClassToClass.get(vc);
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
		//OWLObjectSomeValuesFrom restr = owlDataFactory.getOWLObjectSomeValuesFrom(viewProperty, owlDataFactory.getOWLThing());

		// set up the list of entities we will classify - this is all classes in O(P) or O(P,E)
		// (if there is an E, and it includes classes) that can be classified directly by a class in PVO.
		// the easiest way to build this list is simply to find everything that instantiates or is subsumed
		// by SomeValuesFrom(P, owl:Thing)
		viewEntities = new HashSet<OWLEntity>();
		Set<OWLClass> srcClasses = this.sourceOntology.getClassesInSignature(Imports.INCLUDED);
		for (OWLClass elementEntity : reasoner.getSubClasses(getViewRootClass(), false).getFlattened()) {
			if (elementEntity.equals(owlDataFactory.getOWLNothing()))
				continue;
			if (srcClasses.contains(elementEntity))
				continue;
			viewEntities.add(elementEntity);
		}
		LOG.info("making view for "+elementsOntology+" using viewEntities: "+viewEntities.size());
		NodeSet<OWLNamedIndividual> insts = null;

		if (isClassifyIndividuals && elementsOntology.getIndividualsInSignature(Imports.EXCLUDED).size() > 0) {
			// only attempt to look for individuals if the element ontology contains them
			// (remember, ELK 0.2.0 fails with individuals) 
			LOG.info("Getting individuals for type: "+getViewRootClass());
			insts = reasoner.getInstances(getViewRootClass(), false);
			if (insts == null) {
				LOG.warn("no individuals of type "+getViewRootClass());
				LOG.warn("Perhaps "+reasoner+" does not support classification of individuals?");
			}
			else {
				if (insts.getFlattened().size() == 0)
					LOG.warn("no individuals were classified!");
				LOG.info("Insts: "+insts+" FLATTENED="+insts.getFlattened());
				for (OWLNamedIndividual elementEntity : insts.getFlattened()) {
					viewEntities.add(elementEntity);

				}
			}
		}

		// remove any classes in the view O(P,E) that are not ancestors of elements in E.
		// this can be seen as a 'closed-world satisfiability' test
		if (isFilterUnused) {
			Set<OWLClass> usedClasses = new HashSet<OWLClass>();
			if (insts != null) {
				for (OWLNamedIndividual e : elementsOntology.getIndividualsInSignature()) {
					usedClasses.addAll(reasoner.getTypes(e, false).getFlattened());
				}
			}
			if (leafClasses != null) {
				for (OWLClass lc : leafClasses) {
					Set<OWLClass> subsumers = reasoner.getSuperClasses(lc, false).getFlattened();
					usedClasses.addAll(subsumers);
					usedClasses.addAll(reasoner.getEquivalentClasses(lc).getEntities());
				}
			}
			if (elementsOntology != sourceOntology) {
				for (OWLClass e : elementsOntology.getClassesInSignature()) {
					usedClasses.addAll(reasoner.getSuperClasses(e, false).getFlattened());
					usedClasses.add(e);
				}
			}

			LOG.info("Finding intersection of "+viewEntities.size()+" entites and used: "+usedClasses.size());
			viewEntities.retainAll(usedClasses);
			LOG.info("intersection has "+viewEntities.size());
			// add individuals back
			viewEntities.addAll(elementsOntology.getIndividualsInSignature());
			LOG.info("intersection has "+viewEntities.size()+" after adding individuals back");
		}


		for (OWLEntity e : viewEntities) {
			if (e instanceof OWLClass) {
				OWLClass c = (OWLClass) e;
				OWLDeclarationAxiom cDecl = owlDataFactory.getOWLDeclarationAxiom(c);
				owlOntologyManager.addAxiom(inferredViewOntology, cDecl);
				// copy across label
				String label = getLabel(c, assertedViewOntology);
				if (label != null) {
					OWLAnnotationAssertionAxiom aaa = owlDataFactory.getOWLAnnotationAssertionAxiom(owlDataFactory.getRDFSLabel(), c.getIRI(), 
							owlDataFactory.getOWLLiteral(label));
					// anything derived -- including labels -- goes in the derived ontology
					owlOntologyManager.addAxiom(inferredViewOntology, aaa);
				}
				

				// first add equivalence axioms
				// TODO - allow for merging of equivalent classes
				Set<OWLClass> ecs = reasoner.getEquivalentClasses(c).getEntities();
				for (OWLClass ec : ecs) {
					if (ec.equals(c))
						continue;
					if (viewEntities.contains(ec)) {
						LOG.info(c+" == "+ec);
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
				NodeSet<OWLClass> types = reasoner.getTypes(ind, true);
				if (types != null) {
					Set<OWLClass> scs = types.getFlattened();
					for (OWLClass sc : scs) {
						if (viewEntities.contains(sc)) {
							OWLClassAssertionAxiom caa = owlDataFactory.getOWLClassAssertionAxiom(sc, ind);
							owlOntologyManager.addAxiom(inferredViewOntology, caa);
						}
					}
				}
			}
			else {
				LOG.warn("Ignoring view entity "+e);
			}
		}
	}

	private IRI makeViewClassIRI(OWLClass c) {
		return makeViewClassIRI(c.getIRI(), viewProperty.getIRI(), "-");
	}

	public IRI makeViewClassIRI(IRI vcIRI, IRI vpIRI, String sep) {
		if (!isUseOriginalClassIRIs) {
			String vcIRIstr = vcIRI.toString();
			if (isAssumeOBOStyleIRIs) {
				String baseId = OWLAPIOwl2Obo.getIdentifier(vcIRI);
				String relId = OWLAPIOwl2Obo.getIdentifier(vpIRI);
				vcIRIstr = Obo2OWLConstants.DEFAULT_IRI_PREFIX
					+ baseId.replace(":", "_") + sep
					+ relId.replace("_", "-").replace(":", "-");
			}
			else {
				String frag = vpIRI.getFragment();
				if (frag == null || frag.equals("")) {
					frag = vpIRI.toString().replaceAll(".*\\/", "view");
				}
				vcIRIstr = vcIRIstr + sep + frag;
			}
			vcIRI = IRI.create(vcIRIstr);
		}
		return vcIRI;
	}

	private void setViewClassLabel(OWLClass c, IRI vcIRI, OWLOntology ont) {
		String vLabel = getViewLabel(c);
		if (vLabel != null) {
			OWLAnnotationAssertionAxiom aaa = owlDataFactory.getOWLAnnotationAssertionAxiom(owlDataFactory.getRDFSLabel(), vcIRI, 
					owlDataFactory.getOWLLiteral(vLabel));
			// anything derived -- including labels -- goes in the derived ontology
			owlOntologyManager.addAxiom(ont, aaa);
		}		
	}

	public String getLabel(OWLEntity c, OWLOntology ont) {
		String label = null;		
		// todo - ontology import closure
		for (OWLAnnotation ann : OwlHelper.getAnnotations(c, owlDataFactory.getRDFSLabel(), ont)) {
			OWLAnnotationValue v = ann.getValue();
			if (v instanceof OWLLiteral) {
				label = ((OWLLiteral)v).getLiteral();
				break;
			}
		}
		return label;
	}

	private String getAnyLabel(OWLEntity c) {
		String label = getLabel(c, sourceOntology);
		if (label == null) {
			// non-OBO-style ontology
			label = c.getIRI().getFragment();
			if (label == null) {
				label = c.getIRI().toString();
				label = label.replaceAll(".*/", "");
			}
		}
		return label;
	}

	private String getViewLabel(OWLClass c) {
		String label = getAnyLabel(c);
		if (label != null) {
			return viewLabelPrefix + label + viewLabelSuffix;
		}
		return null;
	}

	/**
	 * generates SubClassOf axioms from ClassAssertion axioms
	 * 
	 * Note that property assertions are currently ignored
	 * 
	 * @param srcOnt
	 * @throws OWLOntologyCreationException 
	 */
	@Deprecated
	public void translateABoxToTBox(OWLOntology srcOnt) throws OWLOntologyCreationException {
		Set<OWLAxiom> axs = new HashSet<OWLAxiom>();
		OWLOntology newElementsOntology = owlOntologyManager.createOntology();
		for (OWLNamedIndividual i : srcOnt.getIndividualsInSignature()) {
			OWLClass c = owlDataFactory.getOWLClass(i.getIRI()); // pun
			for (OWLClassExpression ce : OwlHelper.getTypes(i, srcOnt)) {
				axs.add(owlDataFactory.getOWLSubClassOfAxiom(c, ce));
			}
			//g.getDataFactory().getOWLDe
			for (OWLClassAssertionAxiom ax : srcOnt.getClassAssertionAxioms(i)) {
				owlOntologyManager.removeAxiom(srcOnt, ax);
			}
			for (OWLDeclarationAxiom ax : srcOnt.getDeclarationAxioms(i)) {
				owlOntologyManager.removeAxiom(srcOnt, ax);
			}
			axs.add(owlDataFactory.getOWLDeclarationAxiom(c));
			//g.getDataFactory().getOWLDeclarationAxiom(owlEntity)
		}

		for (OWLAxiom axiom : axs) {
			LOG.info("Tbox2Abox: "+axiom);
			owlOntologyManager.addAxiom(newElementsOntology, axiom);
		}
		// TODO - in future communicate leaf nodes a different way
		elementsOntology = newElementsOntology;
	}

	@Deprecated
	public void translateABoxToTBox() throws OWLOntologyCreationException {
		translateABoxToTBox(elementsOntology);		
	}


}
