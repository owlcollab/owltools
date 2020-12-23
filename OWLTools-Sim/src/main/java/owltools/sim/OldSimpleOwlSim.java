package owltools.sim;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.HypergeometricDistributionImpl;
import org.apache.log4j.Logger;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import owltools.io.OWLPrettyPrinter;
import owltools.mooncat.PropertyViewOntologyBuilder;
import owltools.sim2.preprocessor.SimPreProcessor;
import owltools.util.OwlHelper;

/**
 * <h2>Inputs</h2>
 * 
 * The input is an ontology O, which contains:
 * <p>
 * ABox: a collection of individuals to be compared, plus class assertions
 * TBox: actual ontology
 * <p>
 * Optionally: a list of "SEP" properties, plus a list of classification properties.
 * <p>
 * E.g.
 * 
 * Individual: organism1 Types: human, has_phenotype some HP_nnnnn
 * 
 * 
 * <h2>Procedure</h2>
 * 
 * First of all the ontology is processed, generating new grouping classes. This
 * is called the owlsim ontology.
 * <p>
 * After this has completed, standard semantc similarity measures are applied. 
 * <p>
 * These steps can be decoupled.
 * 
 * <h2>Creating owlsim ontology</h2>
 * 
 * <h2>STEP 1: "SEP" creation (optional)</h2>
 * 
 * A series of "SEP" properties are iterated through in order, for each class C in O,
 * a new class C' = P some C is added to O, provided this subsumes at least one other class in O.
 * Afterwards an axiom C SubClassOf P some C is added to O.
 * <p>
 * For example, if P is part_of, and O contains an anatomy ontology, then the resulting hierarchy
 * will be a snomed-like: hepatocyte SubClassOf cell, liver part etc.
 * 
 * <h2>STEP 2: classification view properties</h2>
 * 
 * A set of classification properties are iterated through. for each such property P
 * and each class C in O, a new class C' = P some C is added to O, provided it subsumes
 * at least one of the ABox individuals. 
 * <p>
 * Thus if E = has_phenotype, and C = lung, if there are no elements that instantiate
 * has_phenotype some lung. this is rejected.
 * 
 * <h3>STEP 3: generation of new LCSs</h3>
 * 
 * the set of all type assertions from the ABox are collected. for each pair C,D in this set,
 * the set-intersection of all reflexive subsumers of C and D is obtained, and redundant nodes removed.
 * If the resulting set-intersection has >1 element, an owl class intersection is generated, together
 * with an equivalence axiom to a new named class. This is inserted into O.
 * 
 *  
 * 
 * <h3>Semantic Similarity</h3>
 * 
 * 
 * @author cjm
 *
 */
public class OldSimpleOwlSim {

	private Logger LOG = Logger.getLogger(OldSimpleOwlSim.class);

	public OWLPrettyPrinter owlpp = null;
	Set<OWLObjectProperty> viewProperties;
	List<OWLObjectProperty> sourceViewProperties; // pre-processing is ordered
	Set<OWLObjectProperty> viewsCreated = new HashSet<OWLObjectProperty>();
	private OWLDataFactory owlDataFactory;
	private OWLOntologyManager owlOntologyManager;
	private OWLReasonerFactory reasonerFactory;
	private Set<OWLClass> ignoreSubClassesOf = null;
	private boolean includeOnlyViewClasses = true;
	private boolean isViewClassesCreated = false;
	private boolean isSourceViewClassesCreated = false;
	OWLReasoner reasoner;
	private Set<OWLClass> fixedAttributeClasses = null;
	final String OBO_PREFIX = "http://purl.obolibrary.org/obo/";
	int idNum = 1;
	private Integer corpusSize;

	private OWLOntology sourceOntology;       
	private OWLOntology associationOntology;       
	private SimPreProcessor simPreProcessor = null;
	
	Set<OWLClass> viewClasses;
	Map<OWLClassExpression,Set<Node<OWLClass>>> superclassMap = null;
	Map<OWLEntity,Set<OWLClass>> elementToAttributesMap;
	Map<OWLClass,Set<OWLEntity>> attributeToElementsMap;
	Map<OWLClassExpression, OWLClass> expressionToClassMap;
	Map<OWLClassExpressionPair, ScoreAttributePair> simCache;
	Map<OWLClassExpressionPair, OWLClass> lcsCache;
	Map<OWLClass, Integer> attributeElementCount = null;
	Map<OWLClassExpression,OWLClass> lcsExpressionToClass = new HashMap<OWLClassExpression,OWLClass>();



	public enum Stage {VIEW, LCS};
	public String baseFileName = null;

	public OldSimpleOwlSim(OWLOntology sourceOntology) {
		super();
		this.sourceOntology = sourceOntology;
		this.owlOntologyManager = sourceOntology.getOWLOntologyManager();
		this.owlDataFactory = owlOntologyManager.getOWLDataFactory();
		this.sourceOntology = sourceOntology;
		init();
	}

	private void init() {
		reasonerFactory = new ReasonerFactory();
		elementToAttributesMap = new HashMap<OWLEntity,Set<OWLClass>>();
		attributeToElementsMap = new HashMap<OWLClass,Set<OWLEntity>>();
		expressionToClassMap = new HashMap<OWLClassExpression, OWLClass>();
		viewClasses = new HashSet<OWLClass>();
		sourceViewProperties = new ArrayList<OWLObjectProperty>();
		simCache = new HashMap<OWLClassExpressionPair, ScoreAttributePair>();
		lcsCache = new HashMap<OWLClassExpressionPair, OWLClass>();
	}

	public class OWLClassExpressionPair {
		public OWLClassExpression c1;
		public OWLClassExpression c2;
		public OWLClassExpressionPair(OWLClassExpression c1,
				OWLClassExpression c2) {
			super();
			this.c1 = c1;
			this.c2 = c2;
		}
		public int hashCode() {
			int n1 = c1.hashCode();
			int n2 = c2.hashCode();

			return (n1 + n2) * n2 + n1;
		}
		public boolean equals(Object other) {
			if (other instanceof OWLClassExpressionPair) {
				OWLClassExpressionPair otherPair = (OWLClassExpressionPair) other;
				return this.c1 == otherPair.c1 && this.c2 == otherPair.c2;
			}

			return false;
		}

	}

	public class ScoreAttributePair {
		public Double score;
		public OWLClassExpression attributeClass;
		public ScoreAttributePair(Double score,
				OWLClassExpression attributeClass) {
			super();
			this.score = score;
			this.attributeClass = attributeClass;
		}
	}

	public class ScoreAttributesPair {
		public Double score;
		public Set<OWLClassExpression> attributeClassSet = new HashSet<OWLClassExpression>(); // all attributes with this score

		public ScoreAttributesPair(Double score,
				OWLClassExpression ac) {
			super();
			this.score = score;
			if (ac != null)
				attributeClassSet.add(ac);
		}
		public ScoreAttributesPair(Double score,
				Set<OWLClassExpression> acs) {
			super();
			this.score = score;
			this.attributeClassSet = acs;
		}	
		public ScoreAttributesPair(double score) {
			super();
			this.score = score;
		}
		public void addAttributeClass(OWLClassExpression ac) {
			if (attributeClassSet == null)
				attributeClassSet = new HashSet<OWLClassExpression>();
			this.attributeClassSet.add(ac);
		}
	}


	public OWLReasonerFactory getReasonerFactory() {
		return reasonerFactory;
	}

	public void setReasonerFactory(OWLReasonerFactory reasonerFactory) {
		this.reasonerFactory = reasonerFactory;
	}
	
	public SimPreProcessor getSimPreProcessor() {
		return simPreProcessor;
	}

	public void setSimPreProcessor(SimPreProcessor simPreProcessor) {
		this.simPreProcessor = simPreProcessor;
	}

	public Set<OWLObjectProperty> getViewProperties() {
		if (viewProperties == null)
			return getAllObjectProperties();
		else
			return viewProperties;
	}

	@Deprecated
	public void setViewProperties(Set<OWLObjectProperty> viewProperties) {
		this.viewProperties = viewProperties;
	}

	@Deprecated
	public void addViewProperty(OWLObjectProperty viewProperty) {
		if (viewProperties == null)
			viewProperties = new HashSet<OWLObjectProperty>();
		viewProperties.add(viewProperty);
	}

	@Deprecated
	public void addViewProperty(IRI iri) {
		addViewProperty(owlDataFactory.getOWLObjectProperty(iri));
	}

	@Deprecated
	public List<OWLObjectProperty> getSourceViewProperties() {
		return sourceViewProperties;
	}

	@Deprecated
	public void setSourceViewProperties(List<OWLObjectProperty> sourceViewProperties) {
		this.sourceViewProperties = sourceViewProperties;
	}

	@Deprecated
	public void addSourceViewProperty(OWLObjectProperty p) {
		if (sourceViewProperties == null)
			sourceViewProperties = new ArrayList<OWLObjectProperty>();
		sourceViewProperties.add(p);
	}

	public void addSourceViewProperty(IRI iri) {
		addSourceViewProperty(owlDataFactory.getOWLObjectProperty(iri));
	}



	public Set<OWLClass> getIgnoreSubClassesOf() {
		return ignoreSubClassesOf;
	}

	public void setIgnoreSubClassesOf(Set<OWLClass> ignoreSubClassesOf) {
		this.ignoreSubClassesOf = ignoreSubClassesOf;
	}

	public void addIgnoreSubClassesOf(OWLClass c) {
		if (ignoreSubClassesOf == null)
			ignoreSubClassesOf = new HashSet<OWLClass>();
		ignoreSubClassesOf.add(c);
	}
	public void addIgnoreSubClassesOf(IRI iri) {
		addIgnoreSubClassesOf(owlDataFactory.getOWLClass(iri));
	}

	public String getBaseFileName() {
		return baseFileName;
	}

	public void setBaseFileName(String baseFileName) {
		this.baseFileName = baseFileName;
	}

	private Set<OWLObjectProperty> getAllObjectProperties() {
		return sourceOntology.getObjectPropertiesInSignature();
	}
	
	/**
	 * NEW: 
	 * externalize preprocessing to separate class
	 * 
	 */
	public void preprocess() {
		this.simPreProcessor.setInputOntology(sourceOntology);
		this.simPreProcessor.setInputOntology(sourceOntology);
		this.simPreProcessor.setReasoner(getReasoner());
		this.simPreProcessor.preprocess();
	}

	/**
	 * generates view ontologies and pre-computes all LCSs.
	 * These are added to the source ontology
	 * 
	 * @throws OWLOntologyCreationException
	 * @throws OWLOntologyStorageException 
	 * @throws FileNotFoundException 
	 */
	
	@Deprecated
	public void generateGroupingClasses() throws OWLOntologyCreationException, FileNotFoundException, OWLOntologyStorageException {
		createElementAttributeMapFromOntology();
		removeUnreachableAxioms();

		generateSourcePropertyViews();
		generatePropertyViews();
		this.saveOntology(Stage.VIEW);

		makeAllByAllLowestCommonSubsumer();
		this.saveOntology(Stage.LCS);
		attributeElementCount = null;

		reason();
	}


	/**
	 * for each pi in P, generate an ontology O(P) using a PVOB, i.e. C' = P some C for all C in O.
	 * reasoner over that ontology, remove non-subsuming expressions, add all axioms
	 * to source ontology
	 * 
	 * @throws OWLOntologyCreationException
	 */
	@Deprecated
	public void generatePropertyViews() throws OWLOntologyCreationException {

		if (isViewClassesCreated)
			return;
		Set<OWLAxiom> allAxioms = new HashSet<OWLAxiom>();
		for (OWLObjectProperty viewProperty : getViewProperties()) {
			if (viewsCreated.contains(viewProperty)) {
				LOG.info("SKIP: already created view for "+viewProperty);
				continue;
			}
			viewsCreated.add(viewProperty);
			LOG.info("Building view for "+viewProperty);
			PropertyViewOntologyBuilder pvob = 
				new PropertyViewOntologyBuilder(sourceOntology,
						sourceOntology,
						viewProperty);

			pvob.setExcludedClasses(this.getVerbotenClasses());

			// the view must classify the elements (e.g. genes, diseases)
			pvob.setLeafClasses(this.getAllAttributeClasses());
			pvob.setClassifyIndividuals(true); // ELK3 now standard
			pvob.setFilterUnused(true);
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

			// cache view axioms - we add these at the end
			for (OWLEntity e : pvob.getViewEntities()) {
				//System.out.println(" E:"+e);
				allAxioms.add(owlDataFactory.getOWLDeclarationAxiom(e));
				allAxioms.addAll(pvob.getAssertedViewOntology().getAnnotationAssertionAxioms(e.getIRI()));
				//viewClasses.add((OWLClass)e);
			}
			LOG.info("VIEW_SIZE: "+pvob.getViewEntities().size()+" for "+viewProperty);
		}

		owlOntologyManager.addAxioms(sourceOntology, allAxioms);
		LOG.info("Added "+allAxioms.size()+" axioms");

		reason();
		isViewClassesCreated = true;
	}

	/**
	 * this initial step adds grouping classes to the source ontology - e.g.
	 * the "SE" part of SEP triples. For example "lung part", "lung derivative".
	 * 
	 * called in order, such that we can make "lung part derivative"
	 * 
	 * @throws OWLOntologyCreationException
	 */
	@Deprecated
	public void generateSourcePropertyViews() throws OWLOntologyCreationException {

		if (isSourceViewClassesCreated)
			return;
		for (OWLObjectProperty sourceViewProperty : getSourceViewProperties()) {

			if (viewsCreated.contains(sourceViewProperty)) {
				LOG.info("SKIP: already created view for "+sourceViewProperty);
				continue;
			}
			viewsCreated.add(sourceViewProperty);

			Set<OWLAxiom> allAxioms = new HashSet<OWLAxiom>();
			LOG.info("Building view for "+sourceViewProperty);
			PropertyViewOntologyBuilder pvob = 
				new PropertyViewOntologyBuilder(sourceOntology,
						sourceOntology,
						sourceViewProperty);
			pvob.setExcludedClasses(this.getVerbotenClasses());
			// the view must classify existing classes
			pvob.setLeafClasses(sourceOntology.getClassesInSignature(true));

			pvob.setCreateReflexiveClasses(true);
			pvob.setClassifyIndividuals(true);
			pvob.setFilterUnused(true);
			pvob.setAssumeOBOStyleIRIs(false);
			pvob.build(reasonerFactory);

			// bring ALL axioms in. Assume no filtering for now
			//for (OWLAxiom ax : pvob.getAssertedViewOntology().getAxioms()) {
			//	allAxioms.add(ax);
			//}

			// TODO: DRY
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
				System.out.println(" SVE:"+e);

				// reflexivity - do this AFTER filtering
				if (e instanceof OWLClass) {
					OWLClass c = (OWLClass)e;
					OWLClass src = pvob.getOriginalClassForViewClass(c);
					//OWLObjectSomeValuesFrom svf =
					//	owlDataFactory.getOWLObjectSomeValuesFrom(sourceViewProperty, c);
					OWLSubClassOfAxiom sca = owlDataFactory.getOWLSubClassOfAxiom(src, c);
					allAxioms.add(sca);
					LOG.info("REFLEXIVE: "+sca);
				}

				allAxioms.add(owlDataFactory.getOWLDeclarationAxiom(e));
				allAxioms.addAll(pvob.getAssertedViewOntology().getAnnotationAssertionAxioms(e.getIRI()));
				//viewClasses.add((OWLClass)e);
			}
			LOG.info("SOURCE+VIEW_SIZE: "+pvob.getViewEntities().size()+" for "+sourceViewProperty);


			// note we add these incrementally
			owlOntologyManager.addAxioms(sourceOntology, allAxioms);
			LOG.info("Added "+allAxioms.size()+" axioms");
		}

		reason();
		isSourceViewClassesCreated = true;
	}
	public void saveOntology(Stage stage) throws FileNotFoundException, OWLOntologyStorageException {
		if (this.baseFileName != null) {
			this.saveOntology(baseFileName +"-" + stage + ".owl");
		}
	}
	public void saveOntology(String fn) throws FileNotFoundException, OWLOntologyStorageException {
		FileOutputStream os = new FileOutputStream(new File(fn));
		OWLDocumentFormat owlFormat = new RDFXMLDocumentFormat();

		owlOntologyManager.saveOntology(sourceOntology, owlFormat, os);
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
		LOG.info("Preparing to reason.\t"+sourceOntology.getClassesInSignature().size()+
				"\t"+sourceOntology.getLogicalAxiomCount());
		superclassMap = null;
		reasoner = reasonerFactory.createReasoner(sourceOntology);
		LOG.info("Pre-reasoned...");		
	}

	/**
	 * If a node cannot be reached on the existential graph, then remove it.
	 * 
	 * Note that this could result in loss of inferences if there are axioms of
	 * the form
	 * 
	 * (a and part_of some b) SubClassOf c
	 * 
	 */
	public void removeUnreachableAxioms() {
		LOG.info("Removing unreachable axioms. Starting with: "+sourceOntology.getAxiomCount());

		Stack<OWLClass> stack = new Stack<OWLClass>();
		stack.addAll(getAllAttributeClasses());
		Set<OWLClass> visited = new HashSet<OWLClass>();
		visited.addAll(stack);

		while (!stack.isEmpty()) {
			OWLClass elt = stack.pop();
			Set<OWLClass> parents = getParents(elt);
			parents.removeAll(visited);
			stack.addAll(parents);
			visited.addAll(parents);
		}
		visited.removeAll(getVerbotenClasses());

		Set<OWLAxiom> rmAxioms = new HashSet<OWLAxiom>();
		for (OWLClass c : sourceOntology.getClassesInSignature()) {
			if (!visited.contains(c)) {
				LOG.info("removing axioms for EL-unreachable class: "+c);
				rmAxioms.addAll(sourceOntology.getAxioms(c, Imports.EXCLUDED));
				rmAxioms.add(owlDataFactory.getOWLDeclarationAxiom(c));
			}
		}

		// remove axioms of the form "SubClassOf organ_part".
		// however, keep equiv axioms that reference these
		for (OWLClass c : getVerbotenClasses()) {
			rmAxioms.addAll(sourceOntology.getSubClassAxiomsForSuperClass(c));
		}

		owlOntologyManager.removeAxioms(sourceOntology, rmAxioms);
		LOG.info("Removed "+rmAxioms.size()+" axioms. Remaining: "+sourceOntology.getAxiomCount());
	}
	private Set<OWLClass> getParents(OWLClass c) {
		Set<OWLClass> parents = new HashSet<OWLClass>();
		Set<OWLClassExpression> xparents = OwlHelper.getSuperClasses(c, sourceOntology);
		xparents.addAll(OwlHelper.getEquivalentClasses(c, sourceOntology));
		for (OWLClassExpression x : xparents) {
			parents.addAll(x.getClassesInSignature());
		}
		return parents;
	}

	Set<OWLClass> verbotenClasses = null; // classes that should not be used for grouping
	private Set<OWLClass> getVerbotenClasses() {
		if (verbotenClasses == null) {
			verbotenClasses = new HashSet<OWLClass>();
			for (OWLClass c : sourceOntology.getClassesInSignature(Imports.INCLUDED)) {
				// TODO - don't hardcode this!
				if (c.getIRI().toString().startsWith("http://purl.obolibrary.org/obo/FMA_")) {
					// eliminate FMA classes that have no uberon equiv
					if (OwlHelper.getEquivalentClasses(c, sourceOntology).isEmpty()) {
						LOG.info("removing FMA class: "+c);
						verbotenClasses.add(c);
						continue;
					}
				}
				Set<OWLAnnotationAssertionAxiom> aaas = 
					sourceOntology.getAnnotationAssertionAxioms(c.getIRI());
				for (OWLAnnotationAssertionAxiom aaa : aaas) {
					String ap = aaa.getProperty().getIRI().toString();
					OWLAnnotationValue v = aaa.getValue();
					if (v instanceof OWLLiteral) {
						OWLLiteral lv = (OWLLiteral)v;

					}
					if (v instanceof IRI) {
						IRI iv = (IRI)v;
						if (ap.endsWith("inSubset")) {
							if (iv.toString().endsWith("upper_level")) {
								LOG.info("removing upper level class: "+c);
								verbotenClasses.add(c);
							}
						}

					}
				}
			}
			Set<OWLClass> veqs = new HashSet<OWLClass>();
			for (OWLClass vc : verbotenClasses) {
				for (OWLClassExpression eqc : OwlHelper.getEquivalentClasses(vc, sourceOntology)) {
					if (eqc instanceof OWLClass) {
						LOG.info("removing equiv "+eqc+" "+vc);
						veqs.add((OWLClass) eqc);
					}
				}
			}
			verbotenClasses.addAll(veqs);
		}

		return verbotenClasses;
	}

	public void assertInferences(boolean isRemoveLogicalAxioms) {
		// TODO
	}

	// ------------------------
	// POST-ONTOLOGY PROCESSING
	// ------------------------


	public void makeAllByAllLowestCommonSubsumer() {
		LOG.info("all x all...");		

		Set<OWLClass> atts = getAllAttributeClasses();
		for (OWLClass a : atts) {
			LOG.info("  "+a+" vs ALL");		
			for (OWLClass b : atts) {
				// LCS operation is symmetric, only pre-compute one way
				if (a.compareTo(b) > 0) {
					OWLClass lcs = this.getLowestCommonSubsumerClass(a, b);
					//System.out.println("LCS( "+pp(a)+" , "+pp(b)+" ) = "+pp(lcs));
				}
			}
		}
		LOG.info("DONE all x all");		
		reason();
	}


	// ----------- ----------- ----------- -----------
	// SUBSUMERS AND LOWEST COMMON SUBSUMERS
	// ----------- ----------- ----------- -----------

	public Set<Node<OWLClass>> getNamedSubsumers(OWLClassExpression a) {
		return getReasoner().getSuperClasses(a, false).getNodes();
	}
	public Set<Node<OWLClass>> getNamedSubsumers(OWLNamedIndividual a) {
		Set<Node<OWLClass>> nodes = new HashSet<Node<OWLClass>>();
		// for now we do not use reasoners for the first step (Elk does not support ABoxes)
		for (OWLClass c: this.getAttributesForElement(a)) {
			nodes.addAll(getReasoner().getSuperClasses(c, false).getNodes());
		}
		return nodes;
	}

	public Set<Node<OWLClass>> getNamedReflexiveSubsumers(OWLClassExpression a) {
		if (superclassMap != null && superclassMap.containsKey(a)) {
			return new HashSet<Node<OWLClass>>(superclassMap.get(a));
		}
		Set<Node<OWLClass>> nodes =  getReasoner().getSuperClasses(a, false).getNodes();
		nodes.add(getReasoner().getEquivalentClasses(a));
		if (superclassMap == null) {
			superclassMap = new HashMap<OWLClassExpression,Set<Node<OWLClass>>>();
		}
		superclassMap.put(a, new HashSet<Node<OWLClass>>(nodes));
		return nodes;
	}

	public Set<Node<OWLClass>> getNamedCommonSubsumers(OWLClassExpression a, OWLClassExpression b) {
		Set<Node<OWLClass>> nodes = getNamedReflexiveSubsumers(a);
		nodes.retainAll(getNamedReflexiveSubsumers(b));
		return nodes;
	}
	public Set<Node<OWLClass>> getNamedCommonSubsumers(OWLNamedIndividual a, OWLNamedIndividual b) {
		Set<Node<OWLClass>> nodes = getNamedSubsumers(a);
		nodes.retainAll(getNamedSubsumers(b));
		return nodes;
	}

	// TODO - cache this
	public Set<Node<OWLClass>> getNamedLowestCommonSubsumers(OWLClassExpression a, OWLClassExpression b) {
		Set<Node<OWLClass>> nodes = getNamedCommonSubsumers(a, b);
		Set<Node<OWLClass>> rNodes = new HashSet<Node<OWLClass>>();
		for (Node<OWLClass> node : nodes) {
			rNodes.addAll(getReasoner().getSuperClasses(node.getRepresentativeElement(), false).getNodes());
		}
		nodes.removeAll(rNodes);
		return nodes;
	}

	/**
	 * gets the LCS. If multiple named LCSs exist in the ontology, then
	 * a class expression is generated from the filtered intersection
	 * 
	 * @param a
	 * @param b
	 * @return OWLClassExpression
	 */
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

		// returns the set of named LCSs that already exist
		Set<Node<OWLClass>> ccs = getNamedLowestCommonSubsumers(a,b);

		// single LCS - return this directly
		if (ccs.size() == 1) {
			return ccs.iterator().next().getRepresentativeElement();
		}

		// make a class expression from the filtered intersection of LCSs

		Set<OWLClass> ops = new HashSet<OWLClass>();
		Set<OWLClass> skips = new HashSet<OWLClass>();
		OWLClass best = null;
		double bestIC = 0.0;
		for (Node<OWLClass> n : ccs) {
			OWLClass c = n.getRepresentativeElement();
			// allows custom filtering; e.g. upper-level classes
			// note: should be removed at view stage			
			if (this.getVerbotenClasses().contains(c))
				continue;
			// TODO: custom filtering
			boolean skip = false;
			Double ic = getInformationContentForAttribute(c);
			if (ic == null) {
				// if the attributes classes have not been filtered, then null values
				// (ie classes with no instances) are possible
				continue;
			}
			if (ic > bestIC) {
				bestIC = ic;
				best = c;
			}
			if (ic < 2.5) { // TODO: configure
				//LOG.info("SKIPPING: "+c+" IC="+ic);
				continue;
			}
			if (this.getNamedReflexiveSubsumers(c).size() < 2) {
				LOG.info("SKIPPING: "+c+" no parents");
				// non-grouping attribute
				continue;
			}
			// don't make intersections of similar elements
			for (Node<OWLClass> n2 : ccs) {
				OWLClass c2 = n2.getRepresentativeElement();
				double ic2 = getInformationContentForAttribute(c2); 
				// prioritize the one with highest IC
				if (ic < ic2 || (ic==ic2 && c.compareTo(c2) > 0)) {
					// TODO: configure simj thresh
					if (this.getAttributeJaccardSimilarity(c, c2) > 0.75) {
						LOG.info("SKIPPING: "+c+" too similar to "+n2);
						skip = true;
					}
				}
			}
			if (skip)
				continue;
			// not filtered
			ops.add(c);
		}

		if (ops.size() == 1) {
			return ops.iterator().next();
		}
		if (ops.size() == 0) {
			// none pass: choose the best representative of the intersection
			return best;
		}
		return owlDataFactory.getOWLObjectIntersectionOf(ops);
	}

	/**
	 * generates a LCS expression and makes it a class if it is a class expression
	 * 
	 * @param a
	 * @param b
	 * @return named class representing LCS
	 */
	public OWLClass getLowestCommonSubsumerClass(OWLClassExpression a, OWLClassExpression b) {
		OWLClassExpressionPair pair = new OWLClassExpressionPair(a,b);
		if (lcsCache.containsKey(pair)) {
			return lcsCache.get(pair);
		}
		OWLClassExpression x = getLowestCommonSubsumer(a,b);
		OWLClass lcs;
		if (lcsExpressionToClass.containsKey(x)) {
			lcs = lcsExpressionToClass.get(x);
		}
		if (x instanceof OWLClass)
			lcs = (OWLClass)x;
		else if (x instanceof OWLObjectIntersectionOf)
			lcs = makeClass((OWLObjectIntersectionOf) x);
		else
			lcs = null;
		lcsCache.put(pair, lcs);
		return lcs;
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

	public float getElementJaccardSimilarity(OWLNamedIndividual a, OWLNamedIndividual b) {
		Set<Node<OWLClass>> ci = getNamedCommonSubsumers(a,b);
		Set<Node<OWLClass>> cu = getNamedSubsumers(a);
		cu.addAll(getNamedSubsumers(b));
		return ci.size() / (float)cu.size();
	}

	public ScoreAttributePair getLowestCommonSubsumerIC(OWLClassExpression a, OWLClassExpression b) {
		OWLClassExpressionPair pair = new OWLClassExpressionPair(a,b);
		if (simCache.containsKey(pair)) {
			return simCache.get(pair);
		}
		OWLClass lcs = getLowestCommonSubsumerClass(a, b);
		ScoreAttributePair sap = new ScoreAttributePair(getInformationContentForAttribute(lcs), lcs);
		simCache.put(pair, sap);
		return sap;
	}

	public ScoreAttributesPair getSimilarityMaxIC(OWLNamedIndividual i, OWLNamedIndividual j) {
		ScoreAttributesPair best = new ScoreAttributesPair(0.0);
		for (OWLClass a : this.getAttributesForElement(i)) {
			for (OWLClass b : this.getAttributesForElement(j)) {
				ScoreAttributePair sap = getLowestCommonSubsumerIC(a, b);
				if (Math.abs(sap.score - best.score) < 0.001) {
					best.addAttributeClass(sap.attributeClass);
				}
				if (sap.score > best.score) {
					best = new ScoreAttributesPair(sap.score, sap.attributeClass);
				}
			}
		}
		return best;
	}

	/**
	 * Pesquita et al
	 * @param i
	 * @param j
	 * @return pair
	 */
	public ScoreAttributesPair getSimilarityBestMatchAverageAsym(OWLNamedIndividual i, OWLNamedIndividual j) {

		List<ScoreAttributesPair> bestMatches = new ArrayList<ScoreAttributesPair>();
		Set<OWLClassExpression> atts = new HashSet<OWLClassExpression>();
		double total = 0.0;
		int n = 0;
		for (OWLClass t1 : this.getAttributesForElement(i)) {
			ScoreAttributesPair best = new ScoreAttributesPair(0.0);

			for (OWLClass t2 : this.getAttributesForElement(j)) {
				ScoreAttributePair sap = getLowestCommonSubsumerIC(t1, t2);
				if (Math.abs(sap.score - best.score) < 0.001) {
					best.addAttributeClass(sap.attributeClass);
				}
				if (sap.score > best.score) {
					best = new ScoreAttributesPair(sap.score, sap.attributeClass);
				}
			}
			atts.addAll(best.attributeClassSet);
			bestMatches.add(best);
			total += best.score;
			n++;
		}
		ScoreAttributesPair sap = new ScoreAttributesPair(total/n, atts);
		return sap;
	}

	public static class EnrichmentConfig {
		public Double pValueCorrectedCutoff;
		public Double attributeInformationContentCutoff;
	}
	public EnrichmentConfig enrichmentConfig;
	

	public EnrichmentConfig getEnrichmentConfig() {
		return enrichmentConfig;
	}

	public void setEnrichmentConfig(EnrichmentConfig enrichmentConfig) {
		this.enrichmentConfig = enrichmentConfig;
	}

	public class EnrichmentResult implements Comparable<EnrichmentResult> {
		public OWLClass enrichedClass;  // attribute being tested
		public OWLClass sampleSetClass; // e.g. gene set
		public Double pValue;
		public Double pValueCorrected;
		public EnrichmentResult(OWLClass sampleSetClass, OWLClass enrichedClass, double pValue,
				double pValueCorrected) {
			super();
			this.sampleSetClass = sampleSetClass;
			this.enrichedClass = enrichedClass;
			this.pValue = pValue;
			this.pValueCorrected = pValueCorrected;
		}

		@Override
		public int compareTo(EnrichmentResult result2) {
			return this.pValue.compareTo((result2).pValue);
		}

		public String toString() {
			return sampleSetClass + " " + enrichedClass+" "+pValue+" "+pValueCorrected;
		}

	}

	private void addEnrichmentResult(EnrichmentResult result,
			List<EnrichmentResult> results) {
		if (enrichmentConfig != null) {
			if (enrichmentConfig.pValueCorrectedCutoff != null && 
					result.pValueCorrected > enrichmentConfig.pValueCorrectedCutoff) {
				return;
			}
			if (enrichmentConfig.attributeInformationContentCutoff != null && 
					this.getInformationContentForAttribute(result.enrichedClass) < 
					enrichmentConfig.attributeInformationContentCutoff) {
				return;
			}
				
		}
		results.add(result);
	}
	
	
	/**
	 * @param populationClass
	 * @param pc1 - sample set class
	 * @param pc2 - enriched set class
	 * @return enrichment
	 * @throws MathException
	 */
	public List<EnrichmentResult> calculateAllByAllEnrichment(OWLClass populationClass,
			OWLClass pc1,
			OWLClass pc2) throws MathException {
		List<EnrichmentResult> results = new Vector<EnrichmentResult>();
		OWLClass nothing = this.owlDataFactory.getOWLNothing();
		for (OWLClass sampleSetClass : getReasoner().getSubClasses(pc1, false).getFlattened()) {
			if (sampleSetClass.equals(nothing))
				continue;
			LOG.info("sample set class:"+sampleSetClass);
			List<EnrichmentResult> resultsInner = new Vector<EnrichmentResult>();
			for (OWLClass enrichedClass : this.getReasoner().getSubClasses(pc2, false).getFlattened()) {
				if (enrichedClass.equals(nothing))
					continue;
				if (sampleSetClass.equals(enrichedClass) ||
						this.getNamedSubsumers(enrichedClass).contains(sampleSetClass) ||
						this.getNamedSubsumers(sampleSetClass).contains(enrichedClass)) {
					continue;
				}
				EnrichmentResult result = calculatePairwiseEnrichment(populationClass,
						sampleSetClass, enrichedClass);
				addEnrichmentResult(result, resultsInner);			
			}
			//LOG.info("sorting results:"+resultsInner.size());
			Collections.sort(resultsInner);
			//LOG.info("sorted results:"+resultsInner.size());
			results.addAll(resultsInner);
		}
		LOG.info("enrichment completed");
		//Collections.sort(results);
		return results;
	}

	

	public List<EnrichmentResult> calculateEnrichment(OWLClass populationClass,
			OWLClass sampleSetClass) throws MathException {
		List<EnrichmentResult> results = new Vector<EnrichmentResult>();
		for (OWLClass enrichedClass : this.getReasoner().getSubClasses(populationClass, false).getFlattened()) {
			LOG.info("Enrichment test for: "+enrichedClass+ " vs "+populationClass);
			results.add(calculatePairwiseEnrichment(populationClass,
					sampleSetClass, enrichedClass));					
		}
		Collections.sort(results);
		return results;
	}

	/**
	 * @param populationClass
	 * @param sampleSetClass
	 * @param enrichedClass
	 * @return enrichment
	 * @throws MathException
	 */
	public EnrichmentResult calculatePairwiseEnrichment(OWLClass populationClass,
			OWLClass sampleSetClass, OWLClass enrichedClass) throws MathException {
		HypergeometricDistributionImpl hg = 
			new HypergeometricDistributionImpl(
					getNumElementsForAttribute(populationClass),
					getNumElementsForAttribute(sampleSetClass),
					getNumElementsForAttribute(enrichedClass)
			);
		/*
		LOG.info("popsize="+getNumElementsForAttribute(populationClass));
		LOG.info("sampleSetSize="+getNumElementsForAttribute(sampleSetClass));
		LOG.info("enrichedClass="+getNumElementsForAttribute(enrichedClass));
		*/
		Set<OWLEntity> eiSet = getElementsForAttribute(sampleSetClass);
		eiSet.retainAll(this.getElementsForAttribute(enrichedClass));
		//LOG.info("both="+eiSet.size());
		double p = hg.cumulativeProbability(eiSet.size(), 
				Math.min(getNumElementsForAttribute(sampleSetClass),
						getNumElementsForAttribute(enrichedClass)));
		double pCorrected = p * getCorrectionFactor(populationClass);
		return new EnrichmentResult(sampleSetClass, enrichedClass, p, pCorrected);		
	}

	// hardcode bonferoni for now
	Integer correctionFactor = null; // todo - robust cacheing
	private int getCorrectionFactor(OWLClass populationClass) {
		if (correctionFactor == null) {
			int n = 0;
			for (OWLClass sc : this.getReasoner().getSubClasses(populationClass, false).getFlattened()) {
				LOG.info("testing count for "+sc);
				if (getNumElementsForAttribute(sc) > 1) {
					n++;
					LOG.info("  ++testing count for "+sc);
				}
			}

			correctionFactor = n;
		}
		return correctionFactor;
	}


	/**
	 * returns all attribute classes - i.e. the classes used to annotate the elements (genes, diseases, etc)
	 * being studied
	 * 
	 *  defaults to all classes in source ontology signature
	 * 
	 * @return set of classes
	 */
	public Set<OWLClass> getAllAttributeClasses() {
		if (fixedAttributeClasses == null)
			return sourceOntology.getClassesInSignature(Imports.INCLUDED);
		else
			return fixedAttributeClasses;
	}

	@Deprecated
	public void setAttributesFromOntology(OWLOntology o) {
		fixedAttributeClasses = o.getClassesInSignature();
	}

	/**
	 * assumes that the ontology contains both attributes (TBox) and elements + associations (ABox)
	 */
	public void createElementAttributeMapFromOntology() {
		Set<OWLClass> allTypes = new HashSet<OWLClass>();
		for (OWLNamedIndividual e : sourceOntology.getIndividualsInSignature(Imports.INCLUDED)) {
			
			// NEW: we assume that grouping classes have already been generated
			Set<OWLClass> types = getReasoner().getTypes(e, true).getFlattened();
			allTypes.addAll(addElement(e, types));
			
			/*
			// find all attributes for an individual. May be
			//   direct named parent classes;
			//   parent class expressions
			Set<OWLClassExpression> types = e.getTypes(sourceOntology);

			// role-bounded MSC for k=1. TODO  k>1
			Map<OWLObjectPropertyExpression, Set<OWLIndividual>> pvs = e.getObjectPropertyValues(sourceOntology);
			for (OWLObjectPropertyExpression pe : pvs.keySet()) {
				for (OWLIndividual j : pvs.get(pe)) {
					for (OWLClassExpression t : j.getTypes(sourceOntology)) {
						types.add(owlDataFactory.getOWLObjectSomeValuesFrom(pe, t));
					}
				}
			}

			allTypes.addAll(addElement(e, types));
			*/

		}
		// need to materialize as classes...
		LOG.info("Using "+allTypes.size()+" attribute classes");
		fixedAttributeClasses = allTypes;
	}

	// adds an element plus associated attributes
	private Set<OWLClass> addElement(OWLEntity e, Set<OWLClass> atts) {
		// TODO - fully fold TBox so that expressions of form (inh (part_of x))
		// generate a class "part_of x", to ensure that a SEP grouping class is created
		Set<OWLClass> attClasses = new HashSet<OWLClass>();
		for (OWLClass attClass : atts) {

			// filtering, e.g. Type :human. This is a somewhat unsatisfactory way to do this;
			// better to filter at the outset
			if (attClass instanceof OWLClass && ignoreSubClassesOf != null && ignoreSubClassesOf.size() > 0) {
				if (this.getReasoner().getSuperClasses(attClass, false).getFlattened().retainAll(ignoreSubClassesOf)) {
					continue;
				}
			}
			if (!this.attributeToElementsMap.containsKey(attClass))
				attributeToElementsMap.put(attClass, new HashSet<OWLEntity>());
			attributeToElementsMap.get(attClass).add(e);
			attClasses.add(attClass);
		}

		// note this only caches direct associations
		this.elementToAttributesMap.put(e, attClasses);
		return attClasses;
	}

	/*
	private Set<OWLClass> addElement(OWLEntity e, Set<OWLClassExpression> atts) {
		// TODO - fully fold TBox so that expressions of form (inh (part_of x))
		// generate a class "part_of x", to ensure that a SEP grouping class is created
		Set<OWLClass> attClasses = new HashSet<OWLClass>();
		for (OWLClassExpression x : atts) {

			// filtering, e.g. Type :human. This is a somewhat unsatisfactory way to do this;
			// better to filter at the outset
			if (x instanceof OWLClass && ignoreSubClassesOf != null && ignoreSubClassesOf.size() > 0) {
				if (this.getReasoner().getSuperClasses(x, false).getFlattened().retainAll(ignoreSubClassesOf)) {
					continue;
				}
			}
			OWLClass attClass = expressionToClass(x);
			if (!this.attributeToElementsMap.containsKey(attClass))
				attributeToElementsMap.put(attClass, new HashSet<OWLEntity>());
			attributeToElementsMap.get(attClass).add(e); // NOT YET USED
			attClasses.add(attClass);
		}

		// note this only caches direct associations
		this.elementToAttributesMap.put(e, attClasses);
		return attClasses;
	}
	*/


	private OWLClass expressionToClass(OWLClassExpression x) {
		if (x instanceof OWLClass)
			return (OWLClass)x;
		if (this.expressionToClassMap.containsKey(x))
			return this.expressionToClassMap.get(x);
		OWLClass c = owlDataFactory.getOWLClass(IRI.create("http://owlsim.org#"+idNum));
		idNum++;

		OWLEquivalentClassesAxiom eca =
			owlDataFactory.getOWLEquivalentClassesAxiom(c, x);
		owlOntologyManager.addAxiom(sourceOntology, eca);
		expressionToClassMap.put(x, c);

		// fully fold tbox (AND and SOME only)
		if (x instanceof OWLObjectIntersectionOf) {
			for (OWLClassExpression y : ((OWLObjectIntersectionOf)x).getOperands()) {
				expressionToClass(y);
			}
		}
		else if (x instanceof OWLObjectSomeValuesFrom) {
			expressionToClass(((OWLObjectSomeValuesFrom)x).getFiller());
		}
		return c;
	}

	public Set<OWLClass> getAttributesForElement(OWLEntity e) {
		return this.elementToAttributesMap.get(e);
	}

	public void precomputeAttributeElementCount() {
		if (attributeElementCount != null)
			return;
		attributeElementCount = new HashMap<OWLClass, Integer>();
		for (OWLEntity e : this.getAllElements()) {
			LOG.info("Adding 1 to all attributes of "+e);
			for (OWLClass dc : getAttributesForElement(e)) {
				for (Node<OWLClass> n : this.getNamedReflexiveSubsumers(dc)) {
					for (OWLClass c : n.getEntities()) {
						if (!attributeElementCount.containsKey(c))
							attributeElementCount.put(c, 1);
						else
							attributeElementCount.put(c, attributeElementCount.get(c)+1);
					}
				}

			}
		}
	}

	/**
	 * inferred
	 * @param c
	 * @return set of entities
	 */
	public Set<OWLEntity> getElementsForAttribute(OWLClass c) {
		Set<OWLClass> subclasses = getReasoner().getSubClasses(c, false).getFlattened();
		subclasses.add(c);
		Set<OWLEntity> elts = new HashSet<OWLEntity>();
		for (OWLClass sc : subclasses) {
			if (attributeToElementsMap.containsKey(sc)) {
				elts.addAll(attributeToElementsMap.get(sc));
			}
		}
		return elts;
	}

	/**
	 * |{e|e in a(c)}|
	 * 
	 * @param c
	 * @return count
	 */
	public int getNumElementsForAttribute(OWLClass c) {
		if (attributeElementCount == null)
			precomputeAttributeElementCount();
		if (attributeElementCount.containsKey(c))
			return attributeElementCount.get(c);
		LOG.info("Uncached count for: "+c);
		int num;
		try {
			num = getElementsForAttribute(c).size();
		}
		catch (Exception e) {
			LOG.error("cannot fetch elements for: "+c);
			LOG.error(e);
			num = this.getCorpusSize();
		}
		attributeElementCount.put(c, num);
		return num;
	}

	public Set<OWLEntity> getAllElements() {
		return elementToAttributesMap.keySet();
	}
	public int getCorpusSize() {
		if (corpusSize == null) {
			corpusSize = getAllElements().size();
		}
		return corpusSize;
	}
	public void setCorpusSize(int size) {
		corpusSize = size;
	}

	public Double getInformationContentForAttribute(OWLClass c) {
		int freq = getNumElementsForAttribute(c);
		Double ic = null;
		if (freq > 0) {
			ic = -Math.log(((double) (freq) / getCorpusSize())) / Math.log(2);
		}
		return ic;
	}

	@Deprecated
	public void getAllByAllLowestCommonSubsumer() {
		for (OWLClass a : getAllAttributeClasses()) {
			for (OWLClass b : getAllAttributeClasses()) {
				OWLClassExpression x = getLowestCommonSubsumer(a,b);
				System.out.println("LCS( "+a+" , "+b+" ) = "+x);
			}
		}
	}



	/**
	 * given a CE of the form A1 and A2 and ... An,
	 * get a class with an IRI formed by concatenating parts of the IRIs of Ai,
	 * and create a label assertion, where the label is formed by concatenating label(A1)....
	 * 
	 * note that the reasoner will need to be synchronized after new classes are made
	 * 
	 * @param x
	 * @return class
	 */
	public OWLClass makeClass(OWLObjectIntersectionOf x) {
		StringBuffer id = new StringBuffer();
		StringBuffer label = new StringBuffer();
		int n = 0;
		int nlabels = 0;

		// TODO - optimize following
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
		lcsExpressionToClass.put(x, c);
		LOG.info(" new LCS: "+c+" label: "+label.toString()+" == "+x);
		owlOntologyManager.addAxioms(sourceOntology, newAxioms);
		return c;
	}

	private String getLabel(OWLEntity e) {
		String label = null;		
		// todo - ontology import closure
		OWLAnnotationProperty property = owlDataFactory.getRDFSLabel();
		for (OWLAnnotation ann : OwlHelper.getAnnotations(e, property, sourceOntology)) {
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
