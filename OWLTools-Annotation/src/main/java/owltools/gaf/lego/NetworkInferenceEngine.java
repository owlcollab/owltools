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
import org.semanticweb.owlapi.model.OWLNamedObject;
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
import owltools.graph.OWLGraphEdge;
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
	Map<Object,String> labelMap; 
	Map<OWLClass,Set<String>> geneByInferredClsMap;
	HashMap<String, Set<OWLClass>> clsByGeneMap;
	Set<OWLClass> activityClassSet;
	public Set<OWLClass> processClassSet;
	/**
	 * M &sube; N x N, N = A &cup; P
	 */
	public Partonomy partonomy;
	Set<Process> processSet; // P : process *instances*
	OWLGraphWrapper ogw;
	OWLOntology exportOntology; // destination for OWL/lego. may be refactored into separate class
	public OWLOntologyManager owlOntologyManager;

	String contextId = ""; // TODO

	private IRI createIRI(String... toks) {
		StringBuffer id = new StringBuffer();
		for (String tok : toks) {
			//if (tok instanceof OWLObject)
			//	tok = ogw.getIdentifier(tok);
			if (tok.contains(":"))
				tok = tok.replaceAll(":", "_");
			id.append("-"+tok);
		}
		return IRI.create("http://x.org"+id);
	}

	// N = A &cup; P
	// note: each node wraps an OWLObject
	public abstract class InstanceNode {
		public OWLIndividual owlObject;
		public OWLClassExpression typeOf;
		public Set<OWLClass> locations = new HashSet<OWLClass>();
		public String label;
		public int numParents;
		public void setLocation(OWLClass c) {
			locations.add(c);
			OWLClassExpression x = getOWLDataFactory().getOWLObjectSomeValuesFrom(
					getObjectProperty("occurs_in"),
					c); // TODO <-- protein IRI should be here
			addAxiom(getOWLDataFactory().getOWLClassAssertionAxiom(
					x,
					owlObject));
		}
		private void addAxioms() {
		}
	}

	/**
	 * A &sube; G x T<sup>A</sup>
	 * </br>
	 * An instance of an activity that is enabled by some gene/product
	 */
	public class Activity extends InstanceNode {
		public final String gene;
		public Double strength;		

		public Activity(OWLClassExpression a, String g, String context) {
			typeOf = a;
			gene = g;
			String acid = "";
			if (a != null) {
				acid = ogw.getIdentifier(a);
				label = ogw.getLabelOrDisplayId(a) + " enabled by "+getLabel(g);
			}
			else {
				label = getLabel(g);
			}
			IRI iri = createIRI(context, acid, g);
			owlObject = getOWLDataFactory().getOWLNamedIndividual(iri);

		}
		private void addAxioms() {
			super.addAxioms();
			OWLClassExpression c = typeOf;
			if (c == null)
				c = ogw.getOWLClassByIdentifier("GO:0003674"); // TODO - use vocab
			addAxiom(getOWLDataFactory().getOWLClassAssertionAxiom(
					c, owlObject));
			OWLClass geneProductClass = null;
			if (gene != null) {
				geneProductClass = getOWLClass(gene);
			}
			else {
				//geneProductClass = getOWLClass("PR:00000001");
			}
			if (geneProductClass != null) {
				OWLClassExpression x = getOWLDataFactory().getOWLObjectSomeValuesFrom(
						getObjectProperty("enabled_by"),
						geneProductClass); // TODO <-- protein IRI should be here
				addAxiom(getOWLDataFactory().getOWLClassAssertionAxiom(
						x,
						owlObject));
				addOwlLabel(geneProductClass, getLabel(gene));
			}
			addOwlLabel(owlObject, label);
		}
	}

	public class Process extends InstanceNode {
		public Process(OWLClassExpression pc, String context) {
			typeOf = pc;
			String pid = pc.toString();
			if (pc instanceof OWLClass)
				pid = ogw.getIdentifier((OWLClass)pc);
			IRI iri = createIRI(context, pid); // TODO
			//LOG.info("Creating process: "+iri);
			owlObject = getOWLDataFactory().getOWLNamedIndividual(iri);
			label = ogw.getLabelOrDisplayId(pc);
		}
		private void addAxioms() {
			super.addAxioms();
			addAxiom(getOWLDataFactory().getOWLClassAssertionAxiom(
					typeOf,
					owlObject));
			addOwlLabel(owlObject, label);
		}

	}

	/**
	 * 
	 * </br>
	 * EdgeType can be null for unknown (i.e. those derived from
	 * PPIs)
	 * @param <T> 
	 *
	 */
	public class Edge<T extends InstanceNode,U extends InstanceNode> {
		public final T subject;
		public final U object;
		public final OWLObjectPropertyExpression type;
		public OWLObjectPropertyAssertionAxiom owlObject;

		/**
		 * @param subject
		 * @param object
		 * @param type
		 */
		public Edge(T subject, U object, OWLObjectPropertyExpression type) {
			super();
			this.subject = subject;
			this.object = object;
			this.type = type;
			// todo - add to owl ontology
		}
		public Edge(T subject, U object, String type) {
			super();
			this.subject = subject;
			this.object = object;
			this.type = getObjectProperty(type);
			// todo - add to owl ontology
		}
		private void addAxioms() {
			// note: type may change with time so we may want to defer...?
			owlObject = 
					getOWLDataFactory().getOWLObjectPropertyAssertionAxiom(
							type,
							subject.owlObject,
							object.owlObject);
			addAxiom(owlObject);

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
			a.addAxioms();
		}
		public void addEdge(Activity s, Activity o, OWLObjectPropertyExpression owlObjectPropertyExpression) {
			Edge e = new Edge(s, o, owlObjectPropertyExpression);
			activityEdgeSet.add(e);
			e.addAxioms();
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
		public Set<Activity> lookupByActivityType(OWLClassExpression t) {
			Set<Activity> activitySubset = new HashSet<Activity>();
			for (Activity a : activitySet) {
				if (a.typeOf == null)
					continue;
				if (a.typeOf.equals(t)) {
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
		public Set<Edge<InstanceNode, InstanceNode>> edgeSet = new HashSet<Edge<InstanceNode, InstanceNode>>();
		public void addEdge(InstanceNode s, InstanceNode o) {
			Edge e = new Edge<InstanceNode,InstanceNode>(s, o, "part_of");
			edgeSet.add(e); // TODO - use vocab
			e.addAxioms();
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
		seedGraph(processCls, seedGenes);
		createPartonomy(processCls);
		inferLocations();
		connectGraph();
	}
	public void buildNetwork(String processClsId, Set<String> seedGenes) throws OWLOntologyCreationException {
		OWLClass processCls = ogw.getOWLClassByIdentifier(processClsId);
		buildNetwork(processCls, seedGenes);
	}

	/**
	 * Create initial activation node set A for a process P and a set of seed genes
	 * 
	 * for all g &in; G<sup>seed</sup>, add a = <g,t> to A where f = argmax(p) { t :  t &in; T<sup>A</sup>, p=Prob( t | g) } 
	 * 
	 * @param processCls
	 * @param seedGenes
	 * @throws OWLOntologyCreationException 
	 */
	public void seedGraph(OWLClass processCls, Set<String> seedGenes) throws OWLOntologyCreationException {

		contextId = ogw.getIdentifier(processCls); // TODO

		IRI ontIRI = this.getIRI("TEMP:" + contextId);
		//LOG.info("ONT IRI = "+ontIRI);
		exportOntology = getOWLOntologyManager().createOntology(ontIRI);

		activityNetwork = new ActivityNetwork();
		for (String g : seedGenes) {
			Activity a = getMostLikelyActivityForGene(g, processCls);
			activityNetwork.add(a);
		}
	}
	public void seedGraph(String processClsId, Set<String> seedGenes) throws OWLOntologyCreationException {
		OWLClass processCls = ogw.getOWLClassByIdentifier(processClsId);
		seedGraph(processCls, seedGenes);
	}

	/**
	 * @see seedGraph(String p, Set seed)
	 * @param processCls
	 * @throws OWLOntologyCreationException 
	 */
	public void seedGraph(OWLClass processCls) throws OWLOntologyCreationException {
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
	 * TODO: MAPKK activity part_of activation of MAPK +reg MAPK activity
	 * 
	 * @param processCls
	 */
	public void createPartonomy(OWLClass processCls) {
		processSet = new HashSet<Process>();
		partonomy = new Partonomy();
		String contextId = ogw.getIdentifier(processCls); // TODO

		// ROOT
		Process rootProcess = new Process(processCls, contextId);
		rootProcess.addAxioms();

		// TOP-DOWN : find all necessary parts of an instance of this process
		OWLPropertyExpression HP = ogw.getOWLObjectPropertyByIdentifier("BFO:0000051");
		Set<OWLPropertyExpression> downSet = Collections.singleton(HP);

		//ps = new HashSet<OWLPropertyExpression>();
		// note that "ancestors" is potetially confusing here - ancestors of has_part yields necessary parts
		Set<OWLObject> partClasses = ogw.getAncestors(processCls, downSet, true);
		partClasses.add(processCls); // reflexive
		
		//LOG.info("NECESSARY PARTS: "+partClasses);
		//for (OWLObject c : partClasses) {
		//LOG.info("     NECPART="+ogw.getIdentifier(c)+ogw.getLabelOrDisplayId(c));
		//}
		//Set<Activity> activitiesWithoutParents = new HashSet<Activity>(activityNetwork.activitySet);
		
		/*
		Set<OWLObject> partClassesRedundant = new HashSet<OWLObject>();
		for (OWLObject part : partClasses) {
			// loose redundancy - superclasses only
			partClassesRedundant.addAll(ogw.getAncestors(part, new HashSet<OWLPropertyExpression>()));
		}
		// must have has_part in chain; TODO - more elegant way of doing this
		partClassesRedundant.addAll(ogw.getAncestors(processCls, Collections.EMPTY_SET));
		 */

		// BOTTOM-UP : define path used to find larger processes a smaller process/acitivity is part of (or regulates)
		HashSet<OWLPropertyExpression> upSet = new HashSet<OWLPropertyExpression>();
		OWLPropertyExpression PO = ogw.getOWLObjectPropertyByIdentifier("BFO:0000050");
		OWLPropertyExpression REGULATES = ogw.getOWLObjectPropertyByIdentifier("RO:0002211");
		OWLPropertyExpression NEGATIVELY_REGULATES = ogw.getOWLObjectPropertyByIdentifier("RO:0002212");
		OWLPropertyExpression POSITIVELY_REGULATES = ogw.getOWLObjectPropertyByIdentifier("RO:0002213");
		upSet.add(REGULATES);
		upSet.add(NEGATIVELY_REGULATES);
		upSet.add(POSITIVELY_REGULATES);
		upSet.add(PO);

		// the activity set is already seeded based on this process.
		// we find the most likely parent for each activity
		for (Activity a : activityNetwork.activitySet) {
			// TODO - handdle regulates
			Set<OWLObject> activityParentClasses = ogw.getAncestors(a.typeOf, upSet, true);
			Set<OWLClass> directParentClasses = makeClasses(activityParentClasses);
			directParentClasses.retainAll(partClasses);
			
			// also include process annotations that have a path to any part
			// ** this may be too liberal **
			for (OWLClass apc : this.getProcessTypes(a.gene)) {
				Set<OWLClass> pAncs = makeClasses(ogw.getAncestors(apc, upSet, true));
				//pAncs.retainAll(partClasses); <-- todo - only NR partClasses
				pAncs.retainAll(Collections.singleton(processCls));
				if (pAncs.size() > 0) {
					removeRedundant(pAncs, upSet);
					Process partProcess = new Process(apc, contextId); // TODO - reuse if exists
					partProcess.addAxioms();
					partonomy.addEdge(a, partProcess);
					for (OWLClass pa : pAncs) {
						Process pap = new Process(pa, contextId);
						pap.addAxioms();
						partonomy.addEdge(partProcess, pap);
						partonomy.addEdge(pap, rootProcess);
					}
				}
			}

			
			//LOG.info(" ALL INTERMEDIATES FOR "+a.activityClass + " ==> "+directParentClasses);
			//for (OWLClass dpc : directParentClasses) {
			//	LOG.info("     DPC="+ogw.getIdentifier(dpc)+ogw.getLabelOrDisplayId(dpc));
			//}
			removeRedundant(directParentClasses, upSet);
			//LOG.info(" NR INTERMEDIATES FOR "+a.activityClass + " ==> "+directParentClasses);
			if (directParentClasses.size() > 1) {
				//LOG.warn("TODO - find best parent");
			}
			if (directParentClasses.size() == 0) {
				//LOG.warn("No intermediate parent found for "+a.activityClass);
			}
			for (OWLClass partClass : directParentClasses) {
				Process partProcess = new Process(partClass, contextId); // TODO - reuse if exists
				partProcess.addAxioms();
				partonomy.addEdge(a, partProcess);
				partonomy.addEdge(partProcess, rootProcess);
				processSet.add(partProcess);
			}
			a.numParents = directParentClasses.size();
		}
		
		/*

		// TODO - for now we leave it as implicit that every member a of A is in P = a x p<sup>seed</sup>
		for (Activity a : activitiesWithoutParents) {
			if (a.activityClass != null)
				partonomy.addEdge(a, rootProcess);
		}
		 */

	}

	public void inferLocations() {
		for (Process p : processSet) {
			inferLocation(p);
		}
		for (Activity a : activityNetwork.activitySet) {
			inferLocation(a);
		}
	}

	// TODO - use reasoner
	private void inferLocation(InstanceNode n) {
		OWLPropertyExpression PO = ogw.getOWLObjectPropertyByIdentifier("BFO:0000050");
		OWLPropertyExpression OCCURS_IN = ogw.getOWLObjectPropertyByIdentifier("BFO:0000066");
		Set<OWLPropertyExpression> overProps = Collections.singleton(PO);
		// TODO - traverse instance too
		Set<OWLObject> ancs = ogw.getAncestorsReflexive(n.typeOf, overProps);
		//LOG.info("Ancs for "+n.owlObject+" == "+ancs.size());
		for (OWLObject anc : ancs) {
			for (OWLGraphEdge e : ogw.getPrimitiveOutgoingEdges(anc)) {
				if (e.getSingleQuantifiedProperty().getProperty() != null &&
						e.getSingleQuantifiedProperty().getProperty().equals(OCCURS_IN)) {
					if (e.getTarget() instanceof OWLClass) {
						LOG.info("Adding location "+n+" --> "+e.getTarget());
						n.setLocation((OWLClass)e.getTarget());		
					}
				}
			}
		}
	}

	private Set<OWLClass> makeClasses(Set<OWLObject> objs) {
		Set<OWLClass> s = new HashSet<OWLClass>();
		for (OWLObject obj : objs) {
			if (!(obj instanceof OWLClass)) {
				//LOG.warn("");
				continue;
			}
			s.add((OWLClass) obj);
		}
		return s;
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
			Set<Activity> aset = activityNetwork.lookupByGene(p1);
			if (aset.size() == 0)
				continue;
			for (String p2 : proteinInteractionMap.get(p1)) {
				Set<Activity> aset2 = activityNetwork.lookupByGene(p2);
				for (Activity a1 : aset) {
					for (Activity a2 : aset2) {
						activityNetwork.addEdge(a1, a2, getObjectProperty("directly_activates")); // TODO
					}
				}
			}
		}

		// Using ontology knowledge (e.g. connected_to relationships; has_input = has_output)

		for (Activity a : activityNetwork.activitySet) {
			//
		}

		// Annotation extension method
		// TODO: e.g. PomBase SPCC645.07      rgf1            GO:0032319-regulation of Rho GTPase activity    PMID:16324155   IGI     PomBase:SPAC1F7.04      P       RhoGEF for Rho1, Rgf1           protein taxon:4896      20100429  PomBase 
		// in: =GO:0051666 ! actin cortical patch localization
	}


	/**
	 * @param g
	 * @param processCls
	 * @return
	 */
	public Activity getMostLikelyActivityForGene(String g, OWLClass processCls) {
		Double bestPr = null;
		Activity bestActivity = new Activity(null, g, contextId); // todo
		for (OWLClass activityCls : getMostSpecificActivityTypes(g)) {
			//Double pr = this.calculateConditionalProbaility(processCls, activityCls);
			Double pr = this.calculateConditionalProbaility(activityCls, processCls);
			if (bestPr == null || pr >= bestPr) {
				Activity a = new Activity(activityCls, g, contextId);
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
	
	public Set<OWLClass> getProcessTypes(String g) {
		HashSet<OWLClass> cset = new HashSet<OWLClass>(clsByGeneMap.get(g));
		cset.retainAll(processClassSet);
		return cset;
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
	 * Gets all genes that enable a given activity type (i.e. inverred annotations to MF term)
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

	public void initialize(GafDocument gafdoc, OWLGraphWrapper g) {
		ogw = g;
		geneByInferredClsMap = new HashMap<OWLClass,Set<String>>();
		populationGeneSet = new HashSet<String>();
		clsByGeneMap = new HashMap<String,Set<OWLClass>>();
		labelMap = new HashMap<Object,String>();
		proteinInteractionMap = new HashMap<String,Set<String>>();
		// TODO - set context from GAF Doc

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

			for (OWLObject ancCls : g.getNamedAncestorsReflexive(cls)) {
				if (!(ancCls instanceof OWLClass)) {
					LOG.error(ancCls + " is ancestor of "+cls+" and not a class...?");
				}
				OWLClass anc = (OWLClass) ancCls;
				//LOG.info("   "+gene + " => "+c+" => "+anc + " // "+ancCls);
				if (!geneByInferredClsMap.containsKey(anc))
					geneByInferredClsMap.put(anc, new HashSet<String>());
				geneByInferredClsMap.get(anc).add(gene);				
			}
		}

		activityClassSet = new HashSet<OWLClass>();
		processClassSet = new HashSet<OWLClass>();
		for (OWLClass cls : g.getAllOWLClasses()) {
			String ns = g.getNamespace(cls);
			if (ns == null) ns = "";
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
	@Deprecated
	public OWLOntology translateNetworkToOWL() throws OWLOntologyCreationException {
		//if (exportOntology == null) {
		//IRI ontIRI = this.getIRI("TEMP:" + contextId);
		//LOG.info("ONT IRI = "+ontIRI);
		//exportOntology = getOWLOntologyManager().createOntology(ontIRI);
		//}
		// a = g x t &in; A &rarr; a &in; OWLNamedIndividual, a.iri = genIRI(g, t), a rdf:type t, a rdf:type (enabled_by some g)
		Map<Activity,String> activityToIdMap = new HashMap<Activity,String>();
		for (Activity a : activityNetwork.activitySet) {
			OWLClassExpression activityClass = a.typeOf;
			String gene = a.gene;
			if (activityClass == null)
				activityClass = ogw.getOWLClassByIdentifier("GO:0003674"); // TODO - use vocab
			if (gene == null)
				gene = "PR:00000001";
			//activityToIdMap.put(a, id);
			//addOwlInstanceRelationType(a.owlObject, "enabled_by", gene);
			//addOwlInstanceType(id, activityClass);
			String label = getLabel(a.owlObject) + " enabled by "+getLabel(gene);
			addOwlLabel(a.owlObject, label);
		}
		for (Edge<InstanceNode, InstanceNode> e : partonomy.edgeSet) {
			LOG.info("PTNMY="+e.subject + " --> "+e.object);
			// TODO - this is really contorted, all because we are overloading String in the partonomy
			/*
			Set<Activity> aset = activityNetwork.lookupByActivityType(e.subject);
			if (aset.size() > 0) {
				Activity a = aset.iterator().next();
				addOwlFact(activityToIdMap.get(a), e.type, e.object);
			}	
			else {
				addOwlFact(e.subject, e.type, e.object);
			}
			 */
			//addOwlLabel(e.subject, getLabel(e.subject));
			addOwlLabel(e.object.owlObject, getLabel(e.object)); // TODO <-- do this earlier
			//this.addOwlInstanceType(e.object, e.object); // PUNNING!!!
		}
		for (Edge<Activity, Activity> e : activityNetwork.activityEdgeSet) {

			addOwlFact(e.subject.owlObject,
					e.type,
					e.object.owlObject
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

	// TODO - use a vocabulary/enum
	private OWLObjectPropertyExpression getObjectProperty(String rel) {
		IRI iri;
		if (rel.equals("part_of"))
			rel = "BFO:0000050";
		if (rel.equals("occurs_in"))
			rel = "BFO:0000066";
		if (rel.contains(":")) {
			iri = getIRI(rel);
		}
		else {
			iri = getIRI("http://purl.obolibrary.org/obo/"+rel); // TODO
		}
		return getOWLDataFactory().getOWLObjectProperty(iri);
	}

	@Deprecated
	private void addOwlFact(OWLIndividual subj, OWLObjectPropertyExpression type, OWLIndividual obj) {
		//LOG.info("Adding " + subj + "   "+rel + " "+obj);
		addAxiom(getOWLDataFactory().getOWLObjectPropertyAssertionAxiom(type, subj, obj));
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
	}


	@Deprecated
	private void addOwlInstanceType(String i, String t) {
		//LOG.info("Adding " + i + " instance of  "+t);
		addAxiom(getOWLDataFactory().getOWLClassAssertionAxiom(
				getOWLClass(t),
				getIndividual(i)));
	}

	@Deprecated
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
