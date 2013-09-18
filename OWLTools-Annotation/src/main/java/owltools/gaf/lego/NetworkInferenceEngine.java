package owltools.gaf.lego;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLPropertyExpression;

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
 *  <li> {@link #addHasParts} - create partonomy P
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
	Set<String> processClassSet;
	/**
	 * M &sube; N x N, N = A &cup; P
	 */
	Partonomy partonomy;
	OWLGraphWrapper ogw;


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
		String activityClass;
		String gene;
		Double strength;
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
		T subject;
		U object;
		String type;
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
		Set<Activity> activitySet = new HashSet<Activity>();
		Set<Edge<Activity,Activity>> activityEdgeSet = new HashSet<Edge<Activity,Activity>>();
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
	}
	
	public class Partonomy {
		Set<Edge<String, String>> edgeSet = new HashSet<Edge<String, String>>();
		public void addEdge(String s, String o) {
			edgeSet.add(new Edge<String,String>(s, o, "part_of"));
		}
		
	}
	
	/**
	 * @param processCls
	 * @param seedGenes
	 */
	public void buildNetwork(String processCls, Set<String> seedGenes) {
		seedGraph(processCls, seedGenes);
		connectGraph();
	}


	/**
	 * Create basic activity set, one activity per gene is seed gene list
	 * @param processCls
	 * @param seedGenes
	 */
	public void seedGraph(String processCls, Set<String> seedGenes) {
		activityNetwork = new ActivityNetwork();
		for (String g : seedGenes) {
			Activity a = getMostLikelyActivityForGene(g, processCls);
			activityNetwork.add(a);
		}
	}
	
	public void addHasParts(String processCls) {
		partonomy = new Partonomy();
		OWLObject c = ogw.getOWLObjectByIdentifier(processCls);
		OWLPropertyExpression HP = ogw.getOWLObjectPropertyByIdentifier("BFO:0000051");
		//ps = new HashSet<OWLPropertyExpression>();
		Set<OWLObject> parts = ogw.getAncestors(c, Collections.singleton(HP));
		Set<OWLObject> partsRedundant = new HashSet<OWLObject>();
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

			if (this.activityClassSet.contains(pid)) {
				LOG.info("NULL ACTIVITY="+processCls + " h-p "+pid);
				Activity a = new Activity(pid, null);
				activityNetwork.add(a);
			}
			else if (this.processClassSet.contains(pid)) {
				boolean isIntermediate = false;
				// for now, only add "intermediates" - revise later?
				for (Activity a : activityNetwork.activitySet) {
					if (a.activityClass == null)
						continue;
					OWLObject ac = ogw.getOWLObjectByIdentifier(a.activityClass);
					if (ogw.getAncestors(ac).contains(part)) {
						isIntermediate = true;
						partonomy.addEdge( a.activityClass, pid);
					}
				}
				if (isIntermediate) {
					LOG.info("INTERMEDIATE PROCESS="+processCls + " h-p "+pid);
					partonomy.addEdge(pid, processCls);
				}
			}
		}
		
	}

	/**
	 * Add default edges based on PPI network
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


	public void calculateHasPartProbailityTable() {

	}

	public Set<String> getActivityTypes(String g) {
		HashSet<String> cset = new HashSet<String>(clsByGeneMap.get(g));
		cset.retainAll(activityClassSet);
		return cset;
	}


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

	public Set<String> getGenes(String cls) {
		if (!geneByInferredClsMap.containsKey(cls)) {
			LOG.info("Nothing known about "+cls);
			return new HashSet<String>();
		}
		return new HashSet<String>(geneByInferredClsMap.get(cls));
	}

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
			if (g.getNamespace(cls).equals("molecular_function")) {
				activityClassSet.add(c);
			}
			if (g.getNamespace(cls).equals("biological_process")) {
				processClassSet.add(c);
			}
			labelMap.put(c, g.getLabel(cls));
		}

	}

	private void addPPI(String a, String b) {
		if (!proteinInteractionMap.containsKey(a))
			proteinInteractionMap.put(a, new HashSet<String>());
		proteinInteractionMap.get(a).add(b);

	}

	public String getLabel(String id) {
		if (labelMap.containsKey(id))
			return this.labelMap.get(id);
		return id;
	}
}
