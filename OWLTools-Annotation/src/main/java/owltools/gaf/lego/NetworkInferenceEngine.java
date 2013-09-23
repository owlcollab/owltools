package owltools.gaf.lego;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLPropertyExpression;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import owltools.gaf.ExtensionExpression;
import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.graph.OWLGraphWrapper;

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
 *  <h2>TODO</h2>
 *  
 *  <ul>
 *  <li>Convert to LEGO
 *  <li>Rules for inferring occurs_in (requires extending A to be A &sube; G x T<sup>A</sup> x T<sup>C</sup>)
 *  </ul>
 *  
 *
 */
public class NetworkInferenceEngine {

	private static Logger LOG = Logger.getLogger(NetworkInferenceEngine.class);

	/**
	 * E<sup>A</sup> &sube; A x A x T<sup>A</sup>
	 */
	public ActivityNetwork activityNetwork;
	Map<String,Set<String>> proteinInteractionMap;
	Set<String> populationGeneSet;
	Map<String,String> labelMap;
	Map<String,Set<String>> geneByInferredClsMap;
	HashMap<String, Set<String>> clsByGeneMap;
	Set<String> activityClassSet;
	public Set<String> processClassSet;
	/**
	 * M &sube; N x N, N = A &cup; P
	 */
	public Partonomy partonomy;
	Set<String> processSet; // P : process *instances*
	OWLGraphWrapper ogw;
	OWLOntology exportOntology; // destination for OWL/lego. may be refactored into separate class
	public OWLOntologyManager owlOntologyManager;

	String contextId = ""; // TODO

	/**
	 * A &sube; G x T<sup>A</sup>
	 * </br>
	 * An instance of an activity that is enabled by some gene/product
	 */
	public class Activity {
		public Activity(String a, String g) {
			activityClass = a;
			gene = g;
		}
		public String activityClass;
		public String gene;
		public Double strength;
	}
	
	/**
	 * 
	 * </br>
	 * EdgeType can be null for unknown (i.e. those derived from
	 * PPIs)
	 * @param <T> 
	 *
	 */
	public class Edge<T,U> {
		public T subject;
		public U object;
		public String type;
		/**
		 * @param subject
		 * @param object
		 * @param type
		 */
		public Edge(T subject, U object, String type) {
			super();
			this.subject = subject;
			this.object = object;
			this.type = type;
		}
	}
	/*
	public class ActivityEdge extends Edge<Activity,Activity> {
		public ActivityEdge(Activity subject, Activity object, String type) {
			super(subject, object, type);
		}
	}
	*/

	/**
	 * N<sup>A</sup> = (A, E<sup>A</sup>)
	 * </br>
	 * A network/graph of activity nodes
	 */
	public class ActivityNetwork {
		public Set<Activity> activitySet = new HashSet<Activity>();
		public Set<Edge<Activity,Activity>> activityEdgeSet = new HashSet<Edge<Activity,Activity>>();
		public void add(Activity a) {
			activitySet.add(a);
		}
		public void addEdge(Activity s, Activity o, String type) {
			activityEdgeSet.add(new Edge(s, o, type));
		}
		public Set<Activity> lookupByGene(String g) {
			Set<Activity> activitySubset = new HashSet<Activity>();
			for (Activity a : activitySet) {
				if (a.gene == null)
					continue;
				if (a.gene.equals(g)) {
					activitySubset.add(a);
				}
			}
			return activitySubset;
		}
		public Set<Activity> lookupByActivityType(String t) {
			Set<Activity> activitySubset = new HashSet<Activity>();
			for (Activity a : activitySet) {
				if (a.activityClass == null)
					continue;
				if (a.activityClass.equals(t)) {
					activitySubset.add(a);
				}
			}
			return activitySubset;
		}
	}
	
	/**
	 * M : Merelogy (partonomy), from activity instances to process instances, and between process instances.  ie M &sube; A &cup; P x P
	 */
	public class Partonomy {
		public Set<Edge<String, String>> edgeSet = new HashSet<Edge<String, String>>();
		public void addEdge(String s, String o) {
			edgeSet.add(new Edge<String,String>(s, o, "part_of"));
		}
		
	}
	
	/**
	 * Performs all steps to build activation network
	 * 
	 * @param processCls
	 * @param seedGenes
	 */
	public void buildNetwork(String processCls, Set<String> seedGenes) {
		seedGraph(processCls, seedGenes);
		createPartonomy(processCls);
		connectGraph();
	}


	/**
	 * Create initial activation node set A for a process P and a set of seed genes
	 * 
	 * for all g &in; G<sup>seed</sup>, add a = <g,t> to A where f = argmax(p) { t :  t &in; T<sup>A</sup>, p=Prob( t | g) } 
	 * 
	 * @param processCls
	 * @param seedGenes
	 */
	public void seedGraph(String processCls, Set<String> seedGenes) {
		contextId = processCls; // TODO
		activityNetwork = new ActivityNetwork();
		for (String g : seedGenes) {
			Activity a = getMostLikelyActivityForGene(g, processCls);
			activityNetwork.add(a);
		}
	}
	
	/**
	 * @see seedGraph(String p, Set seed)
	 * @param processCls
	 */
	public void seedGraph(String processCls) {
		seedGraph(processCls, getGenes(processCls));
	}
	
	/**
	 * Generate M = N x N where N &in; P or N &in; A
	 * 
	 * Basic idea: we want to create a partonomy that breaks down a large process into smaller chunks and ultimately partonomic leaves - activities.
	 * This partonomy may not be identical to the GO partonomy - each node is an instance in the context of the larger process.
	 * 
	 * As a starting point we have a set of leaves - candidate activations we suspect to be involved somehow in the larger process.
	 * We also have knowledge in the ontology - both top-down (e.g. W has_part some P) and bottom-up (e.g. P part_of some W). We want to
	 * connect the leaves to the roots through intermediates. 
	 * 
	 * 
	 * @param processCls
	 */
	public void createPartonomy(String processCls) {
		processSet = new HashSet<String>();
		partonomy = new Partonomy();
		OWLObject c = ogw.getOWLObjectByIdentifier(processCls);
		OWLPropertyExpression HP = ogw.getOWLObjectPropertyByIdentifier("BFO:0000051");
		//ps = new HashSet<OWLPropertyExpression>();
		Set<OWLObject> parts = ogw.getAncestors(c, Collections.singleton(HP));
		Set<OWLObject> partsRedundant = new HashSet<OWLObject>();
		Set<Activity> activitiesWithoutParents = new HashSet<Activity>(activityNetwork.activitySet);
		for (OWLObject part : parts) {
			// loose redundancy - superclasses only
			partsRedundant.addAll(ogw.getAncestors(part, new HashSet<OWLPropertyExpression>()));
		}
		// must have has_part in chain; TODO - more elegant way of doing this
		partsRedundant.addAll(ogw.getAncestors(c,Collections.EMPTY_SET));
		for (OWLObject part : parts) {
			if (partsRedundant.contains(part))
				continue;
			String pid = ogw.getIdentifier(part);

			// The part is either an Activity (i.e. partonomy leaf node) or a Process instance
			
			if (this.activityClassSet.contains(pid)) {
				// the part is an MF class - make a new Activity
				// TODO - check - reuse existing if present?
				LOG.info("NULL ACTIVITY="+processCls + " h-p "+pid);
				Activity a = new Activity(pid, null);
				activityNetwork.add(a);
			}
			else if (this.processClassSet.contains(pid)) {
				boolean isIntermediate = false;
				// for now, only add "intermediates" - revise later? post-prune?
				// todo - intermediates within process part of partonomy
				for (Activity a : activityNetwork.activitySet) {
					if (a.activityClass == null)
						continue;
					OWLObject ac = ogw.getOWLObjectByIdentifier(a.activityClass);
					if (ogw.getAncestors(ac).contains(part)) {
						isIntermediate = true;
						partonomy.addEdge( a.activityClass, pid);
						activitiesWithoutParents.remove(a);
						processSet.add(pid);
					}
				}
				if (isIntermediate) {
					LOG.info("INTERMEDIATE PROCESS="+processCls + " h-p "+pid);
					partonomy.addEdge(pid, processCls);
				}
			}
		}
		
		// TODO - for now we leave it as implicit that every member a of A is in P = a x p<sup>seed</sup>
		for (Activity a : activitiesWithoutParents) {
			if (a.activityClass != null)
				partonomy.addEdge(a.activityClass, processCls);
		}
		
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
		for (String p1 : proteinInteractionMap.keySet()) {
			Set<Activity> aset = activityNetwork.lookupByGene(p1);
			if (aset.size() == 0)
				continue;
			for (String p2 : proteinInteractionMap.get(p1)) {
				Set<Activity> aset2 = activityNetwork.lookupByGene(p2);
				for (Activity a1 : aset) {
					for (Activity a2 : aset2) {
						activityNetwork.addEdge(a1, a2, null);
					}
				}
			}
		}
		
		// TODO: e.g. PomBase SPCC645.07      rgf1            GO:0032319-regulation of Rho GTPase activity    PMID:16324155   IGI     PomBase:SPAC1F7.04      P       RhoGEF for Rho1, Rgf1           protein taxon:4896      20100429  PomBase 
		// in: =GO:0051666 ! actin cortical patch localization
	}


	/**
	 * @param g
	 * @param processCls
	 * @return
	 */
	public Activity getMostLikelyActivityForGene(String g, String processCls) {
		Double bestPr = null;
		Activity bestActivity = new Activity(null, g);
		for (String activityCls : getMostSpecificActivityTypes(g)) {
			//Double pr = this.calculateConditionalProbaility(processCls, activityCls);
			Double pr = this.calculateConditionalProbaility(activityCls, processCls);
			if (bestPr == null || pr >= bestPr) {
				Activity a = new Activity(activityCls, g);
				a.strength = pr;
				bestActivity = a;
				bestPr = pr;
			}
		}
		return bestActivity;
	}



	/**
	 * 
	 * Pr( F | P ) = Pr(F,P) / Pr(P)
	 * 
	 */

	public Double calculateConditionalProbaility(String wholeCls, String partCls) {
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
	public Set<String> getActivityTypes(String g) {
		HashSet<String> cset = new HashSet<String>(clsByGeneMap.get(g));
		cset.retainAll(activityClassSet);
		return cset;
	}


	/**
	 * @param g
	 * @return { t : t &in; getActivityTypes(g), &not; &Exists; t' : t' &in; getActivityTypes(g), t' ProperInferredßSubClassOf t }
	 */
	public Set<String>  getMostSpecificActivityTypes(String g) {
		Set<String> cset = getActivityTypes(g);
		removeRedundant(cset);
		return cset;
	}

	private void removeRedundant(Set<String> cset) {
		Set<String> allAncs = new HashSet<String>();
		for (String c : cset) {
			Set<OWLObject> ancClsSet = ogw.getAncestors(ogw.getOWLObjectByIdentifier(c));
			for (OWLObject obj : ancClsSet) {
				allAncs.add(ogw.getIdentifier(obj));
			}
		}
		cset.removeAll(allAncs);
	}

	/**
	 * Gets all genes that enable a given activity type (i.e. inverred annotations to MF term)
	 * @param t
	 * @return { g : g x t &in; InferredInvolvedIn }
	 */
	public Set<String> getGenes(String cls) {
		if (!geneByInferredClsMap.containsKey(cls)) {
			LOG.info("Nothing known about "+cls);
			return new HashSet<String>();
		}
		return new HashSet<String>(geneByInferredClsMap.get(cls));
	}

	/**
	 * @return |G|
	 */
	public int getNumberOfGenes() {
		return populationGeneSet.size();
	}

	public void initialize(GafDocument gafdoc, OWLGraphWrapper g) {
		ogw = g;
		geneByInferredClsMap = new HashMap<String,Set<String>>();
		populationGeneSet = new HashSet<String>();
		clsByGeneMap = new HashMap<String,Set<String>>();
		labelMap = new HashMap<String,String>();
		proteinInteractionMap = new HashMap<String,Set<String>>();
		for (GeneAnnotation ann : gafdoc.getGeneAnnotations()) {
			String c = ann.getCls();
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
				clsByGeneMap.put(gene, new HashSet<String>());
			clsByGeneMap.get(gene).add(c);

			OWLObject cObj = g.getOWLObjectByIdentifier(c);
			for (OWLObject ancCls : g.getAncestorsReflexive(cObj)) {
				String anc = g.getIdentifier(ancCls);
				//LOG.info("   "+gene + " => "+c+" => "+anc + " // "+ancCls);
				if (!geneByInferredClsMap.containsKey(anc))
					geneByInferredClsMap.put(anc, new HashSet<String>());
				geneByInferredClsMap.get(anc).add(gene);				
			}
		}

		activityClassSet = new HashSet<String>();
		processClassSet = new HashSet<String>();
		for (OWLClass cls : g.getAllOWLClasses()) {
			String c = g.getIdentifier(cls);
			String ns = g.getNamespace(cls);
			if (ns == null) ns = "";
			if (ns.equals("molecular_function")) {
				activityClassSet.add(c);
			}
			else if (ns.equals("biological_process")) {
				processClassSet.add(c);
			}
			else if (!ns.equals("cellular_component")) {
				LOG.info("Adding "+c+" to process subset - I assume anything not a CC or MF is a process");
				// todo - make configurable. The default assumption is that phenotypes etc are treated as pathological process
				processClassSet.add(c);
			}
			String label = g.getLabel(cls);
			if (label != "" && label != null)
				labelMap.put(c, label);
		}

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
	public String getLabel(String id) {
		if (labelMap.containsKey(id))
			return this.labelMap.get(id);
		return id;
	}
	
	public Map<String,Object> getGraphStatistics() {
		Map<String,Object> sm = new HashMap<String,Object>();
		sm.put("activity_node_count", activityNetwork.activitySet.size());
		sm.put("activity_edge_count", activityNetwork.activityEdgeSet.size());
		sm.put("process_count", processSet.size());
		return sm;
	}
	
	/**
	 * Translates ontological activation network into OWL (aka lego model)
	 * <ul>
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
	 * 
	 * Warning: may possibly be refactored into a separate writer class.
	 * 
	 * @return
	 * @throws OWLOntologyCreationException 
	 */
	public OWLOntology translateNetworkToOWL() throws OWLOntologyCreationException {
		//if (exportOntology == null) {
			IRI ontIRI = this.getIRI("TEMP:" + contextId);
			LOG.info("ONT IRI = "+ontIRI);
			exportOntology = getOWLOntologyManager().createOntology(ontIRI);
		//}
		// a = g x t &in; A &rarr; a &in; OWLNamedIndividual, a.iri = genIRI(g, t), a rdf:type t, a rdf:type (enabled_by some g)
		Map<Activity,String> activityToIdMap = new HashMap<Activity,String>();
		for (Activity a : activityNetwork.activitySet) {
			String id = null; // TODO - skolemize?
			String activityClass = a.activityClass;
			String gene = a.gene;
			if (activityClass == null)
				activityClass = "GO:0003674";
			if (gene == null)
				gene = "PR:00000001";
			id = "TEMP:" + contextId + gene + activityClass;
			activityToIdMap.put(a, id);
			addOwlInstanceRelationType(id, "enabled_by", gene);
			addOwlInstanceType(id, activityClass);
			String label = getLabel(activityClass) + " enabled by "+getLabel(gene);
			addOwlLabel(id, label);
		}
		for (Edge<String, String> e : partonomy.edgeSet) {
			LOG.info("PTNMY="+e.subject + " --> "+e.object);
			// TODO - this is really contorted, all because we are overloading String in the partonomy
			Set<Activity> aset = activityNetwork.lookupByActivityType(e.subject);
			if (aset.size() > 0) {
				Activity a = aset.iterator().next();
				addOwlFact(activityToIdMap.get(a), e.type, e.object);
			}	
			else {
				addOwlFact(e.subject, e.type, e.object);
			}
			//addOwlLabel(e.subject, getLabel(e.subject));
			addOwlLabel(e.object, getLabel(e.object));
			this.addOwlInstanceType(e.object, e.object); // PUNNING!!!
		}
		for (Edge<Activity, Activity> e : activityNetwork.activityEdgeSet) {
			String type = e.type;
			if (type == null)
				type = "directly_activates";
			addOwlFact(activityToIdMap.get(e.subject),
					type,
					activityToIdMap.get(e.object)
					);
		}
		
		 return exportOntology;
	}
	
	public OWLOntology translateNetworkToOWL(OWLOntology ont) throws OWLOntologyCreationException {
		exportOntology = ont;
		return translateNetworkToOWL();
	}

	
	private OWLOntologyManager getOWLOntologyManager() {
		if (owlOntologyManager == null)
			owlOntologyManager = OWLManager.createOWLOntologyManager();
		return owlOntologyManager;
	}


	private OWLDataFactory getOWLDataFactory() {
		return exportOntology.getOWLOntologyManager().getOWLDataFactory();
	}
	
	private void addAxiom(OWLAxiom ax) {
		exportOntology.getOWLOntologyManager().addAxiom(exportOntology, ax);
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

	private OWLObjectPropertyExpression getObjectProperty(String rel) {
		IRI iri;
		if (rel.equals("part_of"))
			rel = "BFO:0000050";
		if (rel.contains(":")) {
			iri = getIRI(rel);
		}
		else {
			iri = getIRI("http://purl.obolibrary.org/obo/"+rel); // TODO
		}
		return getOWLDataFactory().getOWLObjectProperty(iri);
	}

	private void addOwlFact(String subj, String rel, String obj) {
		//LOG.info("Adding " + subj + "   "+rel + " "+obj);
		addAxiom(getOWLDataFactory().getOWLObjectPropertyAssertionAxiom(
				getObjectProperty(rel),
				getIndividual(subj), 
				getIndividual(obj)));
	}
	
	private void addOwlData(String subj, OWLAnnotationProperty p, String val) {
		OWLLiteral lit = getOWLDataFactory().getOWLLiteral(val);
		addAxiom(getOWLDataFactory().getOWLAnnotationAssertionAxiom(
				p,
				getIndividual(subj).getIRI(), 
				lit));
	}
	
	private void addOwlLabel(String subj, String val) {
		addOwlData(subj, 
				getOWLDataFactory().getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI()),
				val);
	}



	private void addOwlInstanceType(String i, String t) {
		//LOG.info("Adding " + i + " instance of  "+t);
		addAxiom(getOWLDataFactory().getOWLClassAssertionAxiom(
				getOWLClass(t),
				getIndividual(i)));
	}

	private void addOwlInstanceRelationType(String i, String r, String t) {
		//LOG.info("Adding " + i + " instance of "+r+" some "+t);
		OWLClassExpression x = getOWLDataFactory().getOWLObjectSomeValuesFrom(
				getObjectProperty(r),
				getOWLClass(t));
		addAxiom(getOWLDataFactory().getOWLClassAssertionAxiom(
				x,
				getIndividual(i)));
	}
	


}
