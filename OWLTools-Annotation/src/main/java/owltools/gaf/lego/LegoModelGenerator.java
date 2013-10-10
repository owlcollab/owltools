package owltools.gaf.lego;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.HypergeometricDistributionImpl;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLPropertyExpression;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import owltools.gaf.ExtensionExpression;
import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.graph.OWLGraphWrapper;
import owltools.util.MinimalModelGenerator;
import owltools.vocab.OBOUpperVocabulary;

/**
 * Generates a Ontological Functional Network (aka LEGO graph) for a process given an ontology and a set of annotations
 * 
 * The input is a GO BP class and a set of genes (e.g. all genes involved in that BP), construct the most likely
 * set of gene + activity-type (i.e. MF) pairs that are likely to be executed during this process.
 * Also infer causal relationships between gene-activity pairs based on PPI networks, annotation extensions.
 * 
 *  Finally, break the process into chunks, e.g. using has_part links.
 *  
 *  <h2>Definitions</h2>
 *  
 *  <ul>
 *  <li> G : set of genes/products (for a whole genome/organism)
 *  <li> T : set of all ontology classes and object properties
 *  <li> T<sup>A</sup> : all ontology classes in MF (activity) ontology
 *  <li> T<sup>P</sup> : all ontology classes in BP ontology
 *  <li> A : set of all activity instances in a functional network. Each activity is a gene / activity-class pair. ie A &sube; G x T<sup>A</sup>
 *  <li> E : Optionally typed connections between activity instances. ie E &sube; A x A x T<sup>Rel</sup>
 *  <li> P : Set of all process instances. P &sube; T<sup>P</sup>
 *  <li> M : Merelogy (partonomy), from activity instances to process instances, and between process instances.  ie M &sube; A &cup; P x P
 *  </ul>
 *  
 *  <h2>Algorithm</h2>
 *  
 *  <ul>
 *  <li> {@link #seedGraph} - seed initial A
 *  <li> {@link #createPartonomy} - create partonomy P
 *  <li> {@link #connectGraph} - created activity network E
 *  </ul>
 *  
 *  <h2>Mapping to OWL</h2>
 *	 <ul>
 *  <li> a = g x t &in; A &rarr; a &in; OWLNamedIndividual, a.iri = genIRI(g, t), a rdf:type t, a rdf:type (enabled_by some g)
 *  <li> g &in; G &rarr; g &in; OWLClass, g SubClassOf Protein
 *  <li> t &in; T &rarr; t &in; OWLClass 
 *  <li> e = a1 x a2 x t &in; E &rarr; e &in; OWLObjectPropertyAssertion, e.subject = a1, e.object = a2, e.property = t
 *  <li> p &in; P &rarr; p &in; OWLNamedIndividual
 *  <li> m = p1 x p2 & &in; M &rarr; m &in; OWLObjectPropertyAssertion, m.subject = p1, m.object = p2, m.property = part_of
 * <li>
 * </ul>
 * Notes: we treat all members of G as proteins, but these may be other kinds of gene product. Note also the source ID may be a gene ID.
 * In this case we can substitute "enabled_by some g" with "enabled_by some (product_of some g)"
 * 
 * In some cases the edge type is not known - here we can use a generic owlTopProperty - or we can assume an activates relation, and leave the user to prune/modify

 *  <h2>TODO</h2>
 *  
 *  <ul>
 *  <li>Convert to LEGO
 *  <li>Rules for inferring occurs_in (requires extending A to be A &sube; G x T<sup>A</sup> x T<sup>C</sup>)
 *  </ul>
 *  
 *
 */
public class LegoModelGenerator extends MinimalModelGenerator {


	private static Logger LOG = Logger.getLogger(LegoModelGenerator.class);

	/**
	 * E<sup>A</sup> &sube; A x A x T<sup>A</sup>
	 */
	//public ActivityNetwork activityNetwork;
	Map<String,Set<String>> proteinInteractionMap;
	Set<String> populationGeneSet;
	Map<Object,String> labelMap; 
	Map<OWLClass,Set<String>> geneByInferredClsMap;
	HashMap<String, Set<OWLClass>> clsByGeneMap;
	Set<OWLClass> activityClassSet;
	public Set<OWLClass> processClassSet;
	private Map<String,Set<OWLNamedIndividual>> activityByGene;

	/**
	 * M &sube; N x N, N = A &cup; P
	 */
	//public Partonomy partonomy;
	//Set<Process> processSet; // P : process *instances*
	OWLGraphWrapper ogw;

	String contextId = ""; // TODO

	// TODO - replaceme
	private IRI createIRI(Object... objs) {
		IRI iri;
		StringBuffer sb = new StringBuffer();
		for (Object obj : objs) {
			if (obj instanceof OWLObject) {
				obj = getFragmentID((OWLObject)obj);
			}
			sb.append("/"+obj.toString().replace(":", "_"));
		}
		iri = IRI.create("http://x.org"+sb.toString());
		return iri;
	}





	public LegoModelGenerator(OWLOntology tbox) throws OWLOntologyCreationException {
		super(tbox);
	}


	public LegoModelGenerator(OWLOntology tbox, OWLOntology abox,
			OWLReasoner reasoner) throws OWLOntologyCreationException {
		super(tbox, abox, reasoner);
		// TODO Auto-generated constructor stub
	}


	public LegoModelGenerator(OWLOntology tbox, OWLOntology abox, OWLReasonerFactory rf)
			throws OWLOntologyCreationException {
		super(tbox, abox, rf);
		// TODO Auto-generated constructor stub
	}


	public LegoModelGenerator(OWLOntology tbox, OWLOntology abox)
			throws OWLOntologyCreationException {
		super(tbox, abox);
		// TODO Auto-generated constructor stub
	}


	public LegoModelGenerator(OWLOntology tbox, OWLReasonerFactory reasonerFactory)
			throws OWLOntologyCreationException {
		super(tbox, reasonerFactory);
		// TODO Auto-generated constructor stub
	}

	public void initialize(GafDocument gafdoc, OWLGraphWrapper g) {
		ogw = g;
		geneByInferredClsMap = new HashMap<OWLClass,Set<String>>();
		populationGeneSet = new HashSet<String>();
		clsByGeneMap = new HashMap<String,Set<OWLClass>>();
		labelMap = new HashMap<Object,String>();
		proteinInteractionMap = new HashMap<String,Set<String>>();
		activityByGene = new HashMap<String,Set<OWLNamedIndividual>>(); 
		// TODO - set context from GAF Doc

		this.setPrecomputePropertyClassCombinations(true);
		Set<OWLPropertyExpression> rels = getInvolvedInRelations();

		for (GeneAnnotation ann : gafdoc.getGeneAnnotations()) {
			String c = ann.getCls();
			OWLClass cls = ogw.getOWLClassByIdentifier(c);
			String gene = ann.getBioentity();

			// special case : protein binding
			if (c.equals("GO:0005515")) {
				for (String b : ann.getWithExpression().split("\\|")) {
					addPPI(gene, b);
				}
				continue;
			}

			for (List<ExtensionExpression> eel : ann.getExtensionExpressions()) {
				for (ExtensionExpression ee : eel) {
					// temporary measure - treat all ext expressions as PPIs
					addPPI(gene, ee.getCls());
				}
			}

			populationGeneSet.add(gene);
			String sym = ann.getBioentityObject().getSymbol();
			if (sym != null && !sym.equals(""))
				labelMap.put(gene, sym);


			if (!clsByGeneMap.containsKey(gene))
				clsByGeneMap.put(gene, new HashSet<OWLClass>());
			clsByGeneMap.get(gene).add(cls);

			//LOG.info("Finding ancestors of "+cls+" over "+rels);
			for (OWLObject ancCls : g.getAncestorsReflexive(cls, rels)) { // TODO-rel
				if (ancCls instanceof OWLClass) {
					OWLClass anc = (OWLClass) ancCls;
					//LOG.info("   "+gene + " => "+c+" => "+anc + " // "+ancCls);
					if (!geneByInferredClsMap.containsKey(anc))
						geneByInferredClsMap.put(anc, new HashSet<String>());
					geneByInferredClsMap.get(anc).add(gene);	
				}
			}
		}

		activityClassSet = new HashSet<OWLClass>();
		processClassSet = new HashSet<OWLClass>();
		for (OWLClass cls : g.getAllOWLClasses()) {
			String ns = g.getNamespace(cls);
			if (ns == null) ns = "";

			// TODO - use reasoner
			if (ns.equals("molecular_function")) {
				activityClassSet.add(cls);
			}
			else if (ns.equals("biological_process")) {
				processClassSet.add(cls);
			}
			else if (!ns.equals("cellular_component")) {
				LOG.info("Adding "+cls+" to process subset - I assume anything not a CC or MF is a process");
				// todo - make configurable. The default assumption is that phenotypes etc are treated as pathological process
				processClassSet.add(cls);
			}
			String label = g.getLabel(cls);
			if (label != "" && label != null)
				labelMap.put(cls, label);
		}

	}


	/**
	 * Performs all steps to build activation network
	 * 
	 * @param processCls
	 * @param seedGenes
	 * @throws OWLOntologyCreationException 
	 */
	public void buildNetwork(OWLClass processCls, Set<String> seedGenes) throws OWLOntologyCreationException {
		generateNecessaryIndividuals(processCls, true);
		addGenes(processCls, seedGenes);
		//inferLocations();
		connectGraph();
		normalizeDirections();
	}
	public void buildNetwork(String processClsId, Set<String> seedGenes) throws OWLOntologyCreationException {
		OWLClass processCls = ogw.getOWLClassByIdentifier(processClsId);
		buildNetwork(processCls, seedGenes);
	}

	public void addGenes(String processClsId, Set<String> seedGenes) throws OWLOntologyCreationException {
		OWLClass processCls = ogw.getOWLClassByIdentifier(processClsId);
		addGenes(processCls, seedGenes);
	}


	/**
	 * @see seedGraph(String p, Set seed)
	 * @param processCls
	 * @throws OWLOntologyCreationException 
	 */
	public void addGenes(OWLClass processCls) throws OWLOntologyCreationException {
		addGenes(processCls, getGenes(processCls));
	}


	/**
	 * Create initial activation node set A for a process P and a set of seed genes
	 * 
	 * for all g &in; G<sup>seed</sup>, add a = <g,t> to A where f = argmax(p) { t :  t &in; T<sup>A</sup>, p=Prob( t | g) } 
	 * 
	 * @param processCls
	 * @param seedGenes
	 * @throws OWLOntologyCreationException 
	 * @throws MathException 
	 */
	public void addGenes(OWLClass processCls, Set<String> seedGenes) throws OWLOntologyCreationException {

		LOG.info("Adding genes...");
		Collection<OWLNamedIndividual> beforeSet = getGeneratedIndividuals();
		//contextId = ogw.getIdentifier(processCls); // TODO
		Collection<OWLNamedIndividual> generatedIndividuals = getGeneratedIndividuals();
		generatedIndividuals = beforeSet; // better to use instance closure?
		//		generatedIndividuals = new HashSet<OWLNamedIndividual>();
		//		// note: here ancestors may be sub-parts
		//		for (OWLObject obj : ogw.getAncestorsReflexive(prototypeIndividualMap.get(processCls))) {
		//			LOG.info(" ANCI="+getIdLabelPair(obj));
		//			generatedIndividuals.add((OWLNamedIndividual) obj);
		//		}

		//activityNetwork = new ActivityNetwork();
		for (String g : seedGenes) {
			LOG.info("  Seed gene="+getIdLabelPair(g));

			// each gene, find it's most likely function and most direct process class.
			// each gene can have multiple functions.
			// in addition, a gene may be annotated to different parts of processCls
			//

			Set<OWLClass> geneActivityTypes = getMostSpecificActivityTypes(g);

			Set<OWLClass> geneProcessTypes = getMostSpecificProcessTypes(g);
			Set<OWLClass> geneInferredTypes = getInferredTypes(g);

			LOG.info(" num inferred types = "+geneInferredTypes.size());

			Double best = null;
			OWLClass bestActivityClass = null;
			OWLClass bestParentClass = null;
			OWLNamedIndividual bestParent = null;
			for (OWLNamedIndividual gi : generatedIndividuals) {
				LOG.info(" genIndivid="+getIdLabelPair(gi));
				for (OWLClass generatedCls : getReasoner().getTypes(gi, true).getFlattened()) {
					LOG.info("  GenCls="+getIdLabelPair(generatedCls));
					// a gene must have been annotated to some descendant of generatedCls to be considered.
					if (geneInferredTypes.contains(generatedCls)) {
						for (OWLClass activityCls : geneActivityTypes ) {
							// pr( G+F | P)
							// note that generatedCls may be a MF, and may be a subclass,
							// which case this would be 1.0
							// todo - replace by hypergeometric test
							Double pval;
							try {
								pval = calculatePairwiseEnrichment(activityCls, generatedCls);
								LOG.info("enrichment of "+getIdLabelPair(activityCls)+" IN: "+getIdLabelPair(generatedCls)+
										" = "+pval);
								// temp hack - e.g. frp1 ferric-chelate reductase in iron assimilation by reduction and transport
								if (activityCls.equals(generatedCls)) {
									pval = 0.0;
								}

								if (best == null || pval < best) {
									// 
									// TODO - pval == best
									best = pval;
									bestActivityClass = activityCls;
									bestParentClass = generatedCls;
									bestParent = gi;
								}
							} catch (MathException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				}
				LOG.info(" DONE genIndivid="+getIdLabelPair(gi));
			}

			OWLNamedIndividual ai = addActivity(bestActivityClass, g);
			addPartOf(ai, bestParent);
		}
	}



	private void addPartOf(OWLNamedIndividual p, OWLNamedIndividual w) {
		OWLObjectPropertyExpression rel = this.getObjectProperty(OBOUpperVocabulary.BFO_part_of);
		addEdge(p, rel, w);
	}

	private void addEdge(OWLNamedIndividual p, OWLObjectPropertyExpression rel, OWLNamedIndividual w) {
		if (ogw.getAncestorsReflexive(p).contains(w)) {
			LOG.info("ALREADY CONNECTED TO "+w);
			return;
		}
		if (w != null) {

			OWLAxiom owlObject = 
					getOWLDataFactory().getOWLObjectPropertyAssertionAxiom(
							rel,
							p,
							w);
			addAxiom(owlObject);
		}
		else {
			LOG.warn("Parent of "+p+" is null");
		}
	}

	private OWLNamedIndividual addActivity(OWLClass bestActivityClass, String gene) {
		if (bestActivityClass == null) {
			bestActivityClass =  getOWLDataFactory().getOWLClass(OBOUpperVocabulary.GO_molecular_function.getIRI());
		}
		OWLNamedIndividual ai = this.generateNecessaryIndividuals(bestActivityClass);

		String label = getLabel(bestActivityClass);
		OWLClass geneProductClass = null;
		if (gene != null) {
			geneProductClass = getOWLClass(gene);
		}
		else {
			//geneProductClass = getOWLClass("PR:00000001");
		}
		if (geneProductClass != null) {
			OWLClassExpression x = getOWLDataFactory().getOWLObjectSomeValuesFrom(
					getObjectProperty(OBOUpperVocabulary.GOREL_enabled_by),
					geneProductClass); // TODO <-- protein IRI should be here
			addAxiom(getOWLDataFactory().getOWLClassAssertionAxiom(
					x,
					ai));
			String geneLabel = getLabel(gene);
			addOwlLabel(geneProductClass, geneLabel);
			label = label + " enabled by " + geneLabel;
			if (!activityByGene.containsKey(gene)) {
				activityByGene.put(gene, new HashSet<OWLNamedIndividual>());
			}
			activityByGene.get(gene).add(ai);
		}
		addOwlLabel(ai, label);

		return ai;
	}






	/**
	 * Add default edges based on PPI network
	 *  
	 * add ( a<sub>1</sub> , a<sub>2</sub> ) to E
	 * where ( g<sub>1</sub> , g<sub>2</sub> ) is in PPI, and
	 * a = (g, _) is in A
	 * 
	 */
	public void connectGraph() {

		// PPI Method
		for (String p1 : proteinInteractionMap.keySet()) {
			Set<OWLNamedIndividual> aset = lookupActivityByGene(p1);
			if (aset == null || aset.size() == 0)
				continue;
			LOG.info("P1="+getIdLabelPair(p1));
			for (String p2 : proteinInteractionMap.get(p1)) {
				LOG.info(" P2="+getIdLabelPair(p2));
				Set<OWLNamedIndividual> aset2 = lookupActivityByGene(p2);
				if (aset2 != null) {
					for (OWLNamedIndividual a1 : aset) {
						for (OWLNamedIndividual a2 : aset2) {
							addEdge(a1, getObjectProperty(OBOUpperVocabulary.GOREL_provides_input_for), a2); // TODO
						}
					}
				}
			}
		}

		// Using ontology knowledge (e.g. connected_to relationships; has_input = has_output)

		//for (OWLNamedIndividual a : activityNetwork.activitySet) {
		//
		//}

		// Annotation extension method
		// TODO: e.g. PomBase SPCC645.07      rgf1            GO:0032319-regulation of Rho GTPase activity    PMID:16324155   IGI     PomBase:SPAC1F7.04      P       RhoGEF for Rho1, Rgf1           protein taxon:4896      20100429  PomBase 
		// in: =GO:0051666 ! actin cortical patch localization
	}

	private Set<OWLNamedIndividual> lookupActivityByGene(String g) {
		if (activityByGene.containsKey(g))
			return activityByGene.get(g);
		else
			return null;

	}





	public void normalizeDirections() {
		normalizeDirections(getObjectProperty(OBOUpperVocabulary.BFO_part_of));
	}

	private void normalizeDirections(OWLObjectPropertyExpression p) {
		LOG.info("Normalizing: "+p);
		Set<OWLObjectPropertyExpression> invProps =
				p.getInverses(getAboxOntology().getImportsClosure());
		LOG.info("Inverse props: "+invProps);
		Set<OWLAxiom> rmAxioms = new HashSet<OWLAxiom>();
		Set<OWLAxiom> newAxioms = new HashSet<OWLAxiom>();
		for (OWLObjectPropertyAssertionAxiom opa : 
			getAboxOntology().getAxioms(AxiomType.OBJECT_PROPERTY_ASSERTION)) {
			if (invProps.contains(opa.getProperty())) {
				LOG.info("  FLIPPING:"+opa);
				rmAxioms.add(opa);
				newAxioms.add(getOWLDataFactory().getOWLObjectPropertyAssertionAxiom(p, 
						opa.getObject(), opa.getSubject()));

			}
		}
		getAboxOntology().getOWLOntologyManager().addAxioms(getAboxOntology(), newAxioms);
		getAboxOntology().getOWLOntologyManager().removeAxioms(getAboxOntology(), rmAxioms);
	}

	///////////////////
	//
	// Calculation of gene-class relationships and probabilities

	public Double calculatePairwiseEnrichment(OWLClass sampleSetClass, OWLClass enrichedClass) throws MathException {
		return calculatePairwiseEnrichment(sampleSetClass, enrichedClass, populationGeneSet.size());
	}

	public Double calculatePairwiseEnrichment(
			OWLClass sampleSetClass, OWLClass enrichedClass, int populationClassSize) throws MathException {

		// LOG.info("Hyper :"+populationClass
		// +" "+sampleSetClass+" "+enrichedClass);
		int sampleSetClassSize = getGenes(sampleSetClass).size();
		int enrichedClassSize = getGenes(enrichedClass).size();
		// LOG.info("Hyper :"+populationClassSize
		// +" "+sampleSetClassSize+" "+enrichedClassSize);
		HypergeometricDistributionImpl hg = new HypergeometricDistributionImpl(
				populationClassSize, sampleSetClassSize, enrichedClassSize);
		/*
		 * LOG.info("popsize="+getNumElementsForAttribute(populationClass));
		 * LOG.info("sampleSetSize="+getNumElementsForAttribute(sampleSetClass));
		 * LOG.info("enrichedClass="+getNumElementsForAttribute(enrichedClass));
		 */
		Set<String> eiSet = getGenes(sampleSetClass);
		eiSet.retainAll(getGenes(enrichedClass));
		// LOG.info("both="+eiSet.size());
		double p = hg.cumulativeProbability(eiSet.size(),
				Math.min(sampleSetClassSize, enrichedClassSize));
		//double pCorrected = p * getCorrectionFactor(populationClass);
		return p;
	}

	/**
	 * 
	 * Pr( F | P ) = Pr(F,P) / Pr(P)
	 * 
	 */

	public Double calculateConditionalProbaility(OWLClass wholeCls, OWLClass partCls) {
		Set<String> wgs = getGenes(wholeCls);
		Set<String> cgs = getGenes(partCls);
		cgs.retainAll(wgs);
		double n = getNumberOfGenes();
		Double pr = (cgs.size() / n) / ( wgs.size() / n);		
		return pr;
	}


	/**
	 * Get all activity types a gene enables (i.e. direct MF annotations)
	 * @param g
	 * @return { t : t &in; T<sup>A</sup>, g x t &in; Enables }
	 */
	public Set<OWLClass> getActivityTypes(String g) {
		HashSet<OWLClass> cset = new HashSet<OWLClass>(clsByGeneMap.get(g));
		cset.retainAll(activityClassSet);
		return cset;
	}


	/**
	 * @param g
	 * @return { t : t &in; getActivityTypes(g), &not; &Exists; t' : t' &in; getActivityTypes(g), t' ProperInferredßSubClassOf t }
	 */
	public Set<OWLClass>  getMostSpecificActivityTypes(String g) {
		Set<OWLClass> cset = getActivityTypes(g);
		removeRedundant(cset, null);
		return cset;
	}

	public Set<OWLClass>  getMostSpecificProcessTypes(String g) {
		Set<OWLClass> cset = getProcessTypes(g);
		removeRedundant(cset, null);
		return cset;
	}

	public Set<OWLClass> getProcessTypes(String g) {
		HashSet<OWLClass> cset = new HashSet<OWLClass>(clsByGeneMap.get(g));
		cset.retainAll(processClassSet);
		return cset;
	}

	public Set<OWLClass> getInferredTypes(String g) {
		HashSet<OWLClass> cset = new HashSet<OWLClass>();
		Set<OWLPropertyExpression> rels = this.getInvolvedInRelations();
		for (OWLClass c : clsByGeneMap.get(g)) {
			for (OWLObject a : ogw.getAncestorsReflexive(c, rels)) { // TODO-rel
				if (a instanceof OWLClass) {
					cset.add((OWLClass) a);
				}
			}
		}
		return cset;
	}

	public Set getInferredRelationshipsForGene(String g) {
		Set<OWLObjectSomeValuesFrom> results = new HashSet<OWLObjectSomeValuesFrom>();
		HashSet<OWLClass> cset = new HashSet<OWLClass>(clsByGeneMap.get(g));
		for (OWLClass c : cset) {
			results.addAll(getExistentialRelationships(c));
		}
		return results;
	}




	private void removeRedundant(Set<OWLClass> cset, Set<OWLPropertyExpression> props) {
		Set<OWLClass> allAncs = new HashSet<OWLClass>();
		for (OWLClass c : cset) {
			Set<OWLObject> ancClsSet = ogw.getAncestors(c, props);
			for (OWLObject obj : ancClsSet) {
				if (obj instanceof OWLClass) {
					// named ancestors only
					allAncs.add((OWLClass) obj);
				}

			}
		}
		cset.removeAll(allAncs);
	}

	/**
	 * Gets all genes annotated to cls or descendant
	 * @param t
	 * @return { g : g x t &in; InferredInvolvedIn }
	 */
	public Set<String> getGenes(OWLClass wholeCls) {
		if (!geneByInferredClsMap.containsKey(wholeCls)) {
			LOG.info("Nothing known about "+wholeCls);
			return new HashSet<String>();
		}
		return new HashSet<String>(geneByInferredClsMap.get(wholeCls));
	}

	/**
	 * @return |G|
	 */
	public int getNumberOfGenes() {
		return populationGeneSet.size();
	}


	// adds an (external) protein-protein interaction
	private void addPPI(String a, String b) {
		if (!proteinInteractionMap.containsKey(a))
			proteinInteractionMap.put(a, new HashSet<String>());
		proteinInteractionMap.get(a).add(b);

	}

	/**
	 * @param id
	 * @return label for any class or entity in the graph
	 */
	public String getLabel(Object k) {
		if (k == null)
			return "Null";
		if (labelMap.containsKey(k))
			return this.labelMap.get(k);
		return k.toString();
	}

	public String getIdLabelPair(Object k) {
		if (k == null)
			return "Null";
		if (labelMap.containsKey(k))
			return k+" '"+this.labelMap.get(k)+"'";
		return k.toString();
	}


	public Map<String,Object> getGraphStatistics() {
		Map<String,Object> sm = new HashMap<String,Object>();
		//sm.put("activity_node_count", activityNetwork.activitySet.size());
		//sm.put("activity_edge_count", activityNetwork.activityEdgeSet.size());
		//sm.put("process_count", processSet.size());
		return sm;
	}



	private IRI getIRI(String id) {
		return ogw.getIRIByIdentifier(id);
	}

	private OWLNamedIndividual getIndividual(String id) {
		return getOWLDataFactory().getOWLNamedIndividual(getIRI(id));
	}
	private OWLClass getOWLClass(String id) {
		return getOWLDataFactory().getOWLClass(getIRI(id));
	}

	
	private Set<OWLPropertyExpression> getInvolvedInRelations() {
		Set<OWLPropertyExpression> rels = new HashSet<OWLPropertyExpression>();
		rels.add(getObjectProperty(OBOUpperVocabulary.BFO_part_of));
		rels.add(getObjectProperty(OBOUpperVocabulary.RO_regulates));
		rels.add(getObjectProperty(OBOUpperVocabulary.RO_negatively_regulates));
		rels.add(getObjectProperty(OBOUpperVocabulary.RO_positively_regulates));
		//rels.add(getObjectProperty(OBORelationsVocabulary.BFO_occurs_in));
		return rels;
	}


	private OWLObjectPropertyExpression getObjectProperty(
			OBOUpperVocabulary vocab) {
		// TODO Auto-generated method stub
		return getObjectProperty(vocab.getIRI());
	}
	private OWLObjectPropertyExpression getObjectProperty(
			IRI iri) {
		// TODO Auto-generated method stub
		return getOWLDataFactory().getOWLObjectProperty(iri);
	}





	private void addOwlData(OWLObject subj, OWLAnnotationProperty p, String val) {
		OWLLiteral lit = getOWLDataFactory().getOWLLiteral(val);
		addAxiom(getOWLDataFactory().getOWLAnnotationAssertionAxiom(
				p,
				((OWLNamedObject) subj).getIRI(), 
				lit));
	}

	private void addOwlLabel(OWLObject owlObject, String val) {
		addOwlData(owlObject, 
				getOWLDataFactory().getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI()),
				val);
		labelMap.put(owlObject, val);
	}




}
