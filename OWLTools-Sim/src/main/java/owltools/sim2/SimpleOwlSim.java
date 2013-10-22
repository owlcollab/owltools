package owltools.sim2;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.io.IOUtils;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.HypergeometricDistributionImpl;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.sim2.preprocessor.NullSimPreProcessor;
import owltools.sim2.preprocessor.SimPreProcessor;
import owltools.util.ClassExpressionPair;

/**
 * 
 * <h2>Semantic Similarity</h2>
 * 
 * <h3>Basic Concepts</h3>
 * 
 * This class allows the comparison of one or more entities (modeled as
 * OWLIndividuals) to another set, based on the attributes they possess (modeled
 * as OWLClasses).
 * 
 * Entities may be genes, genotypes, diseases, organisms, ...
 * 
 * Attributes may be functions, phenotypes, ...
 * 
 * Usage is generic beyond biology. The entities compared could be pizzas,
 * and the attrubutes could be toppings.
 * 
 * Similarity scores are generated for any pair of entities based on
 * shared attributes. An <i>ontology</i> is used so that we can compare based
 * on shared <i>inferred</i> attributes as well.
 * 
 * <h3>Pre-processing (optional)</h3>
 * 
 * Each individual is assumed to be an instance of the class used in the
 * comparison. This may not be the natural representation, so some
 * pre-processing may be required.
 * 
 * For example, for representing the relationships between gene individuals and
 * tissues, the natural representation may be
 * 
 * <pre>
 * Individual: g1
 *   Types: gene, expressed_in some hand
 * Individual: g2
 *   Types: gene, expressed_in some toe
 * </pre>
 * 
 * Here the only *named* class commonly instantiated between g1 and g2 is
 * 'gene'. OWLReasoners do not return anonymous class expressions.
 * 
 * The input ontology should be pre-processed to pre-generate named classes such
 * as
 * 
 * <pre>
 * "X gene" EquivalentTo expressed_in some X
 * </pre>
 * 
 * If we assume a property chain
 * 
 * <pre>
 * expressed_in o part_of -> expressed_in
 * </pre>
 * 
 * And a typical partonomy
 * 
 * Then g1 and g2 will both instantiate "expressed_in some autopod"
 * 
 * See the "--mpi" command in OWLTools for building these views
 * 
 * Note that for some applications, it may be best to model entities such as
 * genes and diseases as classes. However, to compare these effectively, the
 * representation should be translated to individuals.
 * 
 * <h3>Reasoning</h3>
 * 
 * Any reasoner can be plugged in but for most datasets we use Elk 0.4.x, which
 * can scale well.
 * 
 * A reasoner performs operations over an ontology consisting of OWLIndividuals and OWLClasses.
 * We typically use i, j for variable names for instances, and a, b, c, d as variable names
 * for classes.
 * 
 * The following operations are used:
 * 
 * <ul>
 * <li> <i>Type(i)</i> : {@link OWLReasoner#getTypes(OWLNamedIndividual, boolean)} - to find all of the attributes possessed
 * by an entity - may be direct or indirect. The inverse operation is <i>Inst(c)</i>
 * <li> <i>Sub(c)</i> :  {@link OWLReasoner#getSuperClasses(OWLClassExpression, boolean)} - to find all the ancestors (subsumers) of an attribute - direct and indirect.
 * The inverse operation is <i>SubBy(c)</i>
 * <li> <i>Eq(c)</i> : {@link OWLReasoner#getEquivalentClasses(OWLClassExpression)} - find all all equivalent attributes/
 * Note that OWLSim is cycle-safe - cycles over SubClassOf are permitted, these are the same as an equivalence axiom between the classes
 * </ul>
 * 
 * The latter two are combined to yield the <i>reflexive</i> common subsumers list - 
 * see {@link #getNamedReflexiveSubsumers(OWLClassExpression)}. We write this here as
 * RSub(c). i.e.
 * <pre>
 * RSub(c) = Sub(c) &cup; Eq(c)
 * </pre>
 * 
 * The use of an OWLReasoner could be replaced with a basic graph traversal algorithm,
 * but it's useful to use a reasoner here, as the ontology may not have been <i>pre-reasoned</i>
 * 	
 * 
 * <h3>Similarity metrics</h3>
 * 
 * See individual methods for details. Generally there are two kinds of
 * comparison
 * 
 * <ul>
 * <li>Between two <i>classes</i> (aka attributes, which are used to describe individuals)
 * <li>Between two <i>individuals</i> (aka entities, e.g. between two genes, two organisms, or a
 * disease vs genotype)
 * </ul>
 * 
 * The former can be used to calculate the latter.
 * 
 * <h4>Common Subsumers and Lowest Common Subsumers</h4>
 * 
 * class-class comparisons are based on <i>inferred common subsumers</i>.
 * <pre>
 * CS(c,d) = RSub(c) &cup; RSub(d)
 * </pre>
 * 
 * The <i>Lowest Common Subsumer</i> is the set of common subsumers that are not
 * redundant with other common subsumers:
 * 
 * <pre>
 * LCS(c,d) = { a : a &in; CS(c,d), not [ &E; a' : a' &in; CS(c,d), c' in Sub(c) ] }
 * </pre>
 * 
 * This is implemented by {@link #getLowestCommonSubsumerIC(OWLClassExpression, OWLClassExpression)}
 * 
 * <h4>Information Content</h4>
 * 
 * The <i>information content</i> (IC) of an class is a measure of how rare the class is
 * 
 *  <pre>
 *  IC(c) = -log<sub>2</sub>( Pr(c) )
 *  </pre>
 *  
 *  Where the probability Pr of a class c is the frequency of that class (number of
 *  instances that instantiate it) divided by the number of instances in the corpus:
 *  
 *  <pre>
 *  Pr(c) = | Inst(c) | / | Inst(RootClass) |
 *  </pre>
 * 
 *  Here the semantics of Inst(c) are provided by
 *   {@link OWLReasoner#getInstances(OWLClassExpression, boolean)}
 *  
 *  <h4>Similarity between classes</h4>
 *  
 *  One measure of similarity between two classes is the IC of the lowest common
 *  subsumer. See {@link #getLowestCommonSubsumerIC(OWLClassExpression, OWLClassExpression)}
 *  
 *  <h4>Similarity Measures between Entities (Individuals)</h4>
 *  
 *  There are a variety of methods for comparing two entities based on the
 *  attributes they share.
 *  
 *  Some methods take the two best-mtaching attributes, others compute some kind of
 *  sum of similarity over the ensemble of attributes.
 *  
 *  An example of a best-attribute method is the <i>maximum information content
 *  of the lowest common subsumers</i>. This is implemented by
 *  {@link #getSimilarityMaxIC(OWLNamedIndividual, OWLNamedIndividual)}. This
 *  is the same method as described in Lord et al 2003.
 *  
 *  Other methods
 *  <ul>
 *  <li> {@link #getElementJaccardSimilarity(OWLNamedIndividual, OWLNamedIndividual)}
 *  <li> {@link #getElementGraphInformationContentSimilarity(OWLNamedIndividual, OWLNamedIndividual)}
 *  <li> 
 *  </ul>
 * 
 * @author cjm
 * 
 */
public class SimpleOwlSim {

	private Logger LOG = Logger.getLogger(SimpleOwlSim.class);

	// Convenience handles for OWLAPI top-level objects
	private OWLDataFactory owlDataFactory;

	private OWLOntologyManager owlOntologyManager;

	private OWLOntology sourceOntology;

	// TODO - more fine grained control over axes of classification
	private Set<OWLClass> ignoreSubClassesOf = null;

	// the classes used to annotate the elements (genes, diseases, etc)
	private Set<OWLClass> cachedAttributeClasses = null;

	// number of individuals in domain
	private Integer corpusSize;

	public SimStats simStats;

	private SimPreProcessor simPreProcessor = null;

	// private OWLOntology resultsOntology = null;

	private Map<OWLClassExpression, Set<Node<OWLClass>>> superclassMap = null;

	private Map<OWLNamedIndividual, Set<OWLClass>> elementToAttributesMap;

	private Map<OWLNamedIndividual, Set<Node<OWLClass>>> elementToInferredAttributesMap;

	private Map<OWLClass, Set<OWLNamedIndividual>> attributeToElementsMap;

	private Map<ClassExpressionPair, ScoreAttributePair> lcsICcache;
	private boolean isLCSCacheFullyPopulated = false;
	private boolean isICCacheFullyPopulated = false;
	public boolean isNoLookupForLCSCache = false;

	// @Deprecated
	private Map<ClassExpressionPair, Set<Node<OWLClass>>> csCache;

	private Map<OWLClass, Double> icCache;

	Map<OWLClass, Integer> attributeElementCount = null;

	// private Map<OWLClassExpression,OWLClass> lcsExpressionToClass = new
	// HashMap<OWLClassExpression,OWLClass>();

	/**
	 * Base similarity metric - may be applied at individual or class level
	 */
	public enum Metric {
		JACCARD("Best Match Average using Jaccard scoring", false, true), OVERLAP(
				"", false, false), NORMALIZED_OVERLAP("", false, false), DICE("", true,
						false), IC_MCS("Best Match Average using Information Content", true,
								false), GIC("GraphInformationContent", true, false), MAXIC(
										"Maximum Information Content", true, false), SIMJ(
												"Similarity based on Jaccard score", false, true), LCSIC(
														"Least Common Subsumer Information Content Score", true, false);

		private final String description;
		private final Boolean isICmetric;

		private final Boolean isJmetric;

		Metric(String description, Boolean isICmetric, Boolean isJmetric) {
			this.description = description;
			this.isICmetric = isICmetric;
			this.isJmetric = isJmetric;
		}

		private String description() {
			return description;
		}

		private Boolean isICmetric() {
			return isICmetric;
		}

		private Boolean isJmetric() {
			return isJmetric;
		}

	};

	/**
	 * Asymmetric metrics can be applied in either of two directions, or both can
	 * be aggregated
	 */
	public enum Direction {
		A_TO_B("Asymmetric, matching all annotations on first element"), B_TO_A(
		"Asymmetric, matching all annotations on second element"), AVERAGE(
		"Symmetric - taking average of both directions");

		private final String description;

		Direction(String description) {
			this.description = description;
		}

		private String description() {
			return description;
		}

	};

	/**
	 * The OutputFormat enumerates the different styles of output for sim results,
	 * and maps 1:1 with different renderers. To be set as an optional
	 * configurable parameter.
	 */
	public enum OutputFormat {
		TXT, CSV, ROW, JSON;
	}

	private Properties simProperties;

	/**
	 * This class will hold some basic statistics about the most-recently invoked
	 * analysis run. Perhaps in the future it could be more elaborate. We may want
	 * a whole separate stats package. I can imagine wanting to know things like
	 * stdev.
	 * 
	 * @author Nicole Washington
	 */


	/**
	 * Set of tags that can be used in configuration
	 * 
	 */
	public enum SimConfigurationProperty {
		analysisRelation,
		/**
		 * The miminum MaxIC threshold for filtering similarity results. Default is
		 * 4.0.
		 */
		minimumMaxIC("4.0"),
		/**
		 * The miminum IC threshold for a LCS to count. Default is 2.0
		 * 4.0.
		 */
		minimumLCSIC("2.0"),
		/**
		 * The miminum simJ threshold for filtering similarity results. Default is
		 * 0.25.
		 */
		minimumSimJ("0.25"),
		/**
		 * comma-separated pair of ID prefixes (what precedes the colon in an ID) to
		 * compare.
		 */
		compare(""),
		/**
		 * a comma delimited list, and values can be drawn from the {@link Metric}.
		 * Default is SIMJ.
		 */
		scoringMetrics(Metric.SIMJ.toString()),
		/**
		 * The output format style. Only a single value is valid, and values can be
		 * drawn from the {@link OutputFormat}. Default is TXT.
		 */
		outputFormat("TXT");

		private final String defaultValue;

		SimConfigurationProperty() {
			this.defaultValue = null;
		}

		SimConfigurationProperty(String defaultValue) {
			this.defaultValue = defaultValue;
		}

		public String defaultValue() {
			return defaultValue;
		}

	}

	/**
	 * @param sourceOntology
	 */
	public SimpleOwlSim(OWLOntology sourceOntology) {
		super();
		this.sourceOntology = sourceOntology;
		this.owlOntologyManager = sourceOntology.getOWLOntologyManager();
		this.owlDataFactory = owlOntologyManager.getOWLDataFactory();
		this.sourceOntology = sourceOntology;
		init();
	}

	private void init() {
		// default pre-processor
		NullSimPreProcessor pproc = new NullSimPreProcessor();
		pproc.setInputOntology(this.sourceOntology);
		pproc.setOutputOntology(this.sourceOntology);
		this.setSimPreProcessor(pproc);

		elementToAttributesMap = null;
		elementToInferredAttributesMap = new HashMap<OWLNamedIndividual,Set<Node<OWLClass>>>();
		attributeToElementsMap = new HashMap<OWLClass,Set<OWLNamedIndividual>>();

		lcsICcache = new HashMap<ClassExpressionPair, ScoreAttributePair>();
		icCache = new HashMap<OWLClass, Double>();
		csCache = new HashMap<ClassExpressionPair, Set<Node<OWLClass>>>();
		simStats = new SimStats();
	}

	/**
	 * A pair consisting of an attribute class, and a score for that class
	 * 
	 * @author cjm
	 * 
	 */
	public class ScoreAttributePair implements Comparable<ScoreAttributePair> {
		public double score;

		public OWLClassExpression attributeClass;

		public ScoreAttributePair(double score, OWLClassExpression attributeClass) {
			super();
			this.score = score;
			this.attributeClass = attributeClass;
		}

		@Override
		public int compareTo(ScoreAttributePair p2) {
			// TODO Auto-generated method stub
			return 0 - Double.compare(score, p2.score);
		}
	}

	/**
	 * A pair consisting of a set of equal-scoring attributes, and a score
	 * 
	 * @author cjm
	 * 
	 */
	public class ScoreAttributesPair implements Comparable<ScoreAttributesPair> {
		public double score;

		public Set<OWLClassExpression> attributeClassSet = new HashSet<OWLClassExpression>(); // all
		// attributes
		// with
		// this
		// score

		public ScoreAttributesPair(double score, OWLClassExpression ac) {
			super();
			this.score = score;
			if (ac != null) attributeClassSet.add(ac);
		}

		public ScoreAttributesPair(double score, Set<OWLClassExpression> acs) {
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

		public void setAttributeClassSet(Set<OWLClass> acs) {
			attributeClassSet = new HashSet<OWLClassExpression>();
			for (OWLClass ac : acs)
				attributeClassSet.add(ac);
		}

		@Override
		public int compareTo(ScoreAttributesPair p2) {
			// TODO Auto-generated method stub
			return 0 - Double.compare(score, p2.score);
		}

	}

	public OWLOntology getSourceOntology() {
		return sourceOntology;
	}

	public void setSourceOntology(OWLOntology sourceOntology) {
		this.sourceOntology = sourceOntology;
	}

	public Properties getSimProperties() {
		return simProperties;
	}

	public void setSimProperties(Properties simProperties) {
		this.simProperties = simProperties;
	}

	public SimPreProcessor getSimPreProcessor() {
		return simPreProcessor;
	}

	public void setSimPreProcessor(SimPreProcessor simPreProcessor) {
		this.simPreProcessor = simPreProcessor;
	}

	public Set<OWLClass> getIgnoreSubClassesOf() {
		return ignoreSubClassesOf;
	}

	public void setIgnoreSubClassesOf(Set<OWLClass> ignoreSubClassesOf) {
		this.ignoreSubClassesOf = ignoreSubClassesOf;
	}

	/**
	 * e.g. 'human'
	 * 
	 * @param c
	 */
	public void addIgnoreSubClassesOf(OWLClass c) {
		if (ignoreSubClassesOf == null)
			ignoreSubClassesOf = new HashSet<OWLClass>();
		ignoreSubClassesOf.add(c);
	}

	public void addIgnoreSubClassesOf(IRI iri) {
		addIgnoreSubClassesOf(owlDataFactory.getOWLClass(iri));
	}

	private Set<OWLObjectProperty> getAllObjectProperties() {
		return sourceOntology.getObjectPropertiesInSignature();
	}

	public OWLReasoner getReasoner() {
		return simPreProcessor.getReasoner();
	}
	public void setReasoner(OWLReasoner r) {
		simPreProcessor.setReasoner(r);
	}

	private Set<OWLClass> getParents(OWLClass c) {
		Set<OWLClass> parents = new HashSet<OWLClass>();
		Set<OWLClassExpression> xparents = c.getSuperClasses(sourceOntology);
		xparents.addAll(c.getEquivalentClasses(sourceOntology));
		for (OWLClassExpression x : xparents) {
			parents.addAll(x.getClassesInSignature());
		}
		return parents;
	}

	// ----------- ----------- ----------- -----------
	// SUBSUMERS AND LOWEST COMMON SUBSUMERS
	// ----------- ----------- ----------- -----------

	// TODO - DRY - preprocessor
	public Set<Node<OWLClass>> getNamedSubsumers(OWLClassExpression a) {
		return getReasoner().getSuperClasses(a, false).getNodes();
	}

	/**
	 * CACHED
	 * 
	 * @param a
	 * @return nodes for all classes that a instantiates - direct and inferred
	 */
	public Set<Node<OWLClass>> getInferredAttributes(OWLNamedIndividual a) {
		// TODO - refactor this method - in previous versions of Elk it was
		// not possible to ask for the types an instance instantiates, now
		// with Elk 0.4 it is
		if (elementToInferredAttributesMap.containsKey(a))
			return new HashSet<Node<OWLClass>>(elementToInferredAttributesMap.get(a));
		Set<Node<OWLClass>> nodes = new HashSet<Node<OWLClass>>();
		for (OWLClass c : this.getAttributesForElement(a)) {
			// if nodes contains c, it also contains all subsumers of c
			if (nodes.contains(c)) continue;
			nodes.addAll(getNamedReflexiveSubsumers(c));
			// nodes.addAll(getReasoner().getSuperClasses(c, false).getNodes());
		}
		elementToInferredAttributesMap.put(a, nodes);
		return new HashSet<Node<OWLClass>>(nodes);
	}

	// TODO - DRY - preprocessor
	/**
	 * 
	 * @param a
	 * @return anc(a)
	 */
	// TODO - CACHE
	public Set<Node<OWLClass>> getNamedReflexiveSubsumers(OWLClassExpression a) {
		if (superclassMap != null && superclassMap.containsKey(a)) {
			return new HashSet<Node<OWLClass>>(superclassMap.get(a));
		}
		if (a.isAnonymous()) {
			LOG.error("finding superclasses of:" + a);
		}
		LOG.info("finding superclasses of:" + a); // TODO - tmp
		Set<Node<OWLClass>> nodes = new HashSet<Node<OWLClass>>(getReasoner()
				.getSuperClasses(a, false).getNodes());
		nodes.add(getReasoner().getEquivalentClasses(a));
		if (superclassMap == null) {
			superclassMap = new HashMap<OWLClassExpression, Set<Node<OWLClass>>>();
		}
		superclassMap.put(a, new HashSet<Node<OWLClass>>(nodes));
		LOG.info("# of superclasses of:" + a + " = " + nodes.size());
		return nodes;
	}

	/**
	 * 
	 * <pre>
	 *   CS(a,b) = { c : c &isin; RSub(a), c &isin; RSub(b) }
	 * </pre>
	 * 
	 * NOT CACHED.
	 * Always returns a copy of the CS list
	 * 
	 * @param a
	 * @param b
	 * @return nodes
	 */
	public Set<Node<OWLClass>> getNamedCommonSubsumers(OWLClassExpression a,
			OWLClassExpression b) {
		ClassExpressionPair pair = new ClassExpressionPair(a, b); // TODO - optimize
		// - assume named
		// classes
		//if (csCache.containsKey(pair))
		//	return new HashSet<Node<OWLClass>>(csCache.get(pair));
		Set<Node<OWLClass>> nodes = getNamedReflexiveSubsumers(a);
		nodes.retainAll(getNamedReflexiveSubsumers(b));
		//csCache.put(pair, nodes);
		return new HashSet<Node<OWLClass>>(nodes);
	}

	/**
	 * <pre>
	 *   CS(i,j) = { c : c &isin; Type(i), c &isin; Type(j) }
	 * </pre>
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public Set<Node<OWLClass>> getNamedCommonSubsumers(OWLNamedIndividual a,
			OWLNamedIndividual b) {
		// we don't cache this as we assume it will be called at most once
		Set<Node<OWLClass>> nodes = getInferredAttributes(a);
		nodes.retainAll(getInferredAttributes(b));
		return nodes;
	}

	/**
	 * <pre>
	 *   CS<sub>redundant</sub>(a,b) = { c : c &in; CS(a,b), &E; c' : c' &in; CS(a,b), c' &in Sub(c) }
	 *   LCS(a,b) = CS(a,b) - CS<SUB>redundant</SUB>
	 * </pre>
	 * 
	 * @param a
	 * @param b
	 * @return Lowest Common Subsumers of a and b
	 */
	public Set<Node<OWLClass>> getNamedLowestCommonSubsumers(
			OWLClassExpression a, OWLClassExpression b) {
		// currently no need to cache this, as only called from
		// getLowestCommonSubsumerIC, which does its own caching
		Set<Node<OWLClass>> commonSubsumerNodes = getNamedCommonSubsumers(a, b);
		Set<Node<OWLClass>> rNodes = new HashSet<Node<OWLClass>>();
		for (Node<OWLClass> node : commonSubsumerNodes) {
			rNodes.addAll(getReasoner().getSuperClasses(
					node.getRepresentativeElement(), false).getNodes());
		}
		commonSubsumerNodes.removeAll(rNodes);
		return commonSubsumerNodes;
	}

	/**
	 * This method provides a generic wrapper onto other attribute-based similarity
	 * methods
	 * 
	 * Compares two classes (attributes) according to the specified metric
	 * 
	 * @param a
	 * @param b
	 * @param metric
	 * @return
	 */
	public double getAttributeSimilarity(OWLClassExpression a, OWLClassExpression b, Metric metric) {
		Set<Node<OWLClass>> ci = getNamedCommonSubsumers(a,b);
		Set<Node<OWLClass>> cu = getNamedReflexiveSubsumers(a);
		cu.addAll(getNamedReflexiveSubsumers(b));
		// TODO - DRY
		if (metric.equals(Metric.JACCARD)) {
			return ci.size() / (float) cu.size();
		} else if (metric.equals(Metric.OVERLAP)) {
			return ci.size();
		} else if (metric.equals(Metric.NORMALIZED_OVERLAP)) {
			return ci.size()
			/ Math.min(getNamedReflexiveSubsumers(a).size(),
					getNamedReflexiveSubsumers(b).size());
		} else if (metric.equals(Metric.DICE)) {
			return 2
			* ci.size()
			/ ((getNamedReflexiveSubsumers(a).size() + getNamedReflexiveSubsumers(
					b).size()));
		} else if (metric.equals(Metric.JACCARD)) {
			return ci.size() / (float) cu.size();
		} else {
			LOG.error("No such metric: " + metric);
			return 0;
		}
	}

	/**
	 * <pre>
	 * SimJ(a,b) = | anc(a) &cap; anc(b) | / | anc(a) &cup; anc(b) |
	 * </pre>
	 * 
	 * @param a
	 * @param b
	 * @return SimJ of two attribute classes
	 */
	public double getAttributeJaccardSimilarity(OWLClassExpression a,
			OWLClassExpression b) {
		Set<Node<OWLClass>> ci = getNamedCommonSubsumers(a, b);
		Set<Node<OWLClass>> cu = getNamedReflexiveSubsumers(a);
		cu.addAll(getNamedReflexiveSubsumers(b));
		return ci.size() / (float) cu.size();
	}

	/**
	 * <pre>
	 * SimJ(i,j) = | Type(i) &cap; Type(j) | / | Type(i) &cup; Type(j) |
	 * </pre>
	 * 
	 * Here Type(i) is the set of all (direct and indirect) inferred types
	 * for an individual.
	 * 
	 * @param i
	 * @param j
	 * @return SimJ
	 */
	public float getElementJaccardSimilarity(OWLNamedIndividual i,
			OWLNamedIndividual j) {
		Set<Node<OWLClass>> ci = getNamedCommonSubsumers(i, j);
		Set<Node<OWLClass>> cu = getInferredAttributes(i);
		cu.addAll(getInferredAttributes(j));
		return ci.size() / (float) cu.size();
	}

	/**
	 * <pre>
	 * SimJ<sup>ASym</sup>(c,d) = | anc(c) &cap; anc(d) | / | anc(c)  |
	 * </pre>
	 * 
	 * If d is an ancestor of c then the score is 1. i.e.
	 * <pre>
	 * d &in; Sub(c) &rarr;  SimJ<sup>ASym</sup>(c,d) = 1
	 * <pre>
	 * 
	 * @param c
	 * @param d
	 * @return Asymmetric SimJ of two attribute classes
	 */
	public double getAsymmerticAttributeJaccardSimilarity(OWLClassExpression a,
			OWLClassExpression b) {
		Set<Node<OWLClass>> ci = getNamedCommonSubsumers(a, b);
		Set<Node<OWLClass>> d = getNamedReflexiveSubsumers(a);
		return ci.size() / (float) d.size();
	}

	/**
	 * sums of IC of the intersection attributes/ sum of IC of union attributes.
	 * <img src=
	 * "http://www.pubmedcentral.nih.gov/picrender.fcgi?artid=2238903&blobname=gkm806um8.jpg"
	 * alt="formula for simGIC"/>
	 * 
	 * @param i
	 * @param j
	 * @return similarity. 
	 */
	public double getElementGraphInformationContentSimilarity(
			OWLNamedIndividual i, OWLNamedIndividual j) {
		Set<Node<OWLClass>> ci = getNamedCommonSubsumers(i, j);
		Set<Node<OWLClass>> cu = getInferredAttributes(i);
		cu.addAll(getInferredAttributes(j));
		double sumICboth = 0;
		double sumICunion = 0;
		for (Node<OWLClass> c : ci) {
			sumICboth += getInformationContentForAttribute(c
					.getRepresentativeElement());
		}
		for (Node<OWLClass> c : cu) {
			sumICunion += getInformationContentForAttribute(c
					.getRepresentativeElement());
		}
		return sumICboth / sumICunion;
	}

	/**
	 * 
	 * <pre>
	 *    IC(c) = -log<SUB>2</SUB>(p(c))
	 *    p(c) = | Inst(c) | / | Inst(owl:Thing) |
	 * </pre>
	 * 
	 * CACHED
	 * 
	 * @param a
	 * @param b
	 * @return Lowest common Subsumer plus its Information Content
	 */
	public ScoreAttributePair getLowestCommonSubsumerIC(OWLClassExpression a,
			OWLClassExpression b) {
		return getLowestCommonSubsumerIC(a, b, null);
	}
	public ScoreAttributePair getLowestCommonSubsumerIC(OWLClassExpression a,
			OWLClassExpression b, Double minimumIC) {

		ClassExpressionPair pair = new ClassExpressionPair(a, b);
		
		// lookup cache to see if this has been calculated already
		if (!isNoLookupForLCSCache) {
			if (lcsICcache.containsKey(pair)) {
				return lcsICcache.get(pair); // don't make a copy, assume unmodified
			}
			ClassExpressionPair pairRev = new ClassExpressionPair(b, a);
			if (lcsICcache.containsKey(pairRev)) {
				return lcsICcache.get(pairRev); // don't make a copy, assume unmodified
			}

			if (this.isLCSCacheFullyPopulated) {
				// entry not found in cache and the cache is fully populated;
				// this means that the IC was below threshold
				return null;
			}
		}
		// TODO: test whether it is more efficient to get redundant common subsumers too,
		// then simply keep the ones with the highest.
		// removing redundant may be better as those deeper in the hierarchy may
		// have the same IC as a parent
		Set<Node<OWLClass>> lcsSet = getNamedLowestCommonSubsumers(a, b);

		ScoreAttributePair sap = null;
		if (lcsSet.size() == 1) {
			OWLClass lcs = lcsSet.iterator().next().getRepresentativeElement();
			sap = new ScoreAttributePair(getInformationContentForAttribute(lcs), lcs);
		} else if (lcsSet.size() > 1) {

			// take the best one; if tie, select arbitrary
			Double bestIC = null;
			OWLClass bestLCS = null;
			for (Node<OWLClass> node : lcsSet) {
				OWLClass lcs = node.getRepresentativeElement();
				Double ic = getInformationContentForAttribute(lcs);
				if (bestIC == null || ic > bestIC) {
					bestIC = ic;
					bestLCS = lcs;
				}

			}
			sap = new ScoreAttributePair(bestIC, bestLCS);
		} else {
			LOG.warn("LCS of " + a + " + " + b + " = {}");
			sap = new ScoreAttributePair(0.0, owlDataFactory.getOWLThing());
		}
		LOG.debug("LCS_IC\t" + a + "\t" + b + "\t" + sap.attributeClass + "\t"
				+ sap.score);
		if (minimumIC != null  && sap.score < minimumIC) {
			// do not cache
		}
		else {
			lcsICcache.put(pair, sap);
		}
		return sap;
	}

	/**
	 * Find the inferred attribute shared by both i and j that has highest IC. If
	 * there is a tie then the resulting structure will have multiple classes.
	 * 
	 * <pre>
	 * MaxIC(i,j) = max { IC(c) : c &in LCS(i,j) }
	 * </pre>
	 * 
	 * As a convenience, this method also returns the LCS class as well as the IC
	 * 
	 * This is the same metric used by Lord et al in <a href="http://bioinformatics.oxfordjournals.org/cgi/reprint/19/10/1275">
	 * Investigating semantic similarity measures  across the Gene Ontology: the relationship between sequence and annotation</a>.
	 * 
	 * @param i
	 * @param j
	 * @return ScoreAttributesPair
	 */
	public ScoreAttributesPair getSimilarityMaxIC(OWLNamedIndividual i,
			OWLNamedIndividual j) {
		Set<Node<OWLClass>> atts = getInferredAttributes(i);
		atts.retainAll(getInferredAttributes(j)); // intersection

		ScoreAttributesPair best = new ScoreAttributesPair(0.0);
		for (Node<OWLClass> n : atts) {
			OWLClass c = n.getRepresentativeElement();
			Double ic = this.getInformationContentForAttribute(c);
			if (Math.abs(ic - best.score) < 0.001) {
				// tie for best attribute
				best.addAttributeClass(c);
			}
			if (ic > best.score) {
				best = new ScoreAttributesPair(ic, c);
			}
		}
		return best;
	}

	public ScoreAttributesPair getSimilarityBestMatchAverageAsym(
			OWLNamedIndividual i, OWLNamedIndividual j) {
		return getSimilarityBestMatchAverage(i, j, Metric.IC_MCS, Direction.A_TO_B);
	}

	/**
	 * See: Pesquita et al
	 * 
	 * <pre>
	 * BMA<sup>M</sup>(i,j) = avg { s : c &in; Attr(i), max { s : d &in; Attr(j), s = M(c,d) } }
	 * </pre>
	 * 
	 * 
	 * @param i
	 * @param j
	 * @param metric
	 * @return pair
	 */
	public ScoreAttributesPair getSimilarityBestMatchAverageAsym(OWLNamedIndividual i, OWLNamedIndividual j, Metric metric) {
		return getSimilarityBestMatchAverage(i, j, metric, Direction.A_TO_B);
	}

	public ScoreAttributesPair getSimilarityBestMatchAverage(OWLNamedIndividual i, OWLNamedIndividual j, Metric metric, Direction dir) {

		if (dir.equals(Direction.B_TO_A)) {
			return getSimilarityBestMatchAverage(j, i, metric, Direction.A_TO_B);
		}
		if (dir.equals(Direction.AVERAGE)) {
			LOG.error("TODO - make this more efficient");
			ScoreAttributesPair bma1 = getSimilarityBestMatchAverage(i, j, metric,
					Direction.A_TO_B);
			ScoreAttributesPair bma2 = getSimilarityBestMatchAverage(j, i, metric,
					Direction.A_TO_B);
			Set<OWLClassExpression> atts = new HashSet<OWLClassExpression>(
					bma1.attributeClassSet);
			atts.addAll(bma2.attributeClassSet);
			ScoreAttributesPair bma = new ScoreAttributesPair(
					(bma1.score + bma2.score) / 2, bma1.attributeClassSet);
			return bma;
		}

		// no cache - assume only called once for each pair
		List<ScoreAttributesPair> bestMatches = new ArrayList<ScoreAttributesPair>();
		Set<OWLClassExpression> atts = new HashSet<OWLClassExpression>();
		double total = 0.0;
		int n = 0;
		for (OWLClass t1 : this.getAttributesForElement(i)) {
			ScoreAttributesPair best = new ScoreAttributesPair(0.0);

			for (OWLClass t2 : this.getAttributesForElement(j)) {
				ScoreAttributePair sap;
				if (metric.equals(Metric.IC_MCS)) {
					sap = getLowestCommonSubsumerIC(t1, t2);
					if (sap == null) {
						sap = new ScoreAttributePair(0.0, this.owlDataFactory.getOWLThing());
					}
				}
				else if (metric.equals(Metric.JACCARD)) {
					// note this may be partly inefficient as the common sumers of t1 and t2
					// may be re-calculated for the same values. Consider cacheing simj of AxA
					sap = new ScoreAttributePair(getAttributeJaccardSimilarity(t1, t2),
							null);
				} else {
					LOG.warn("NOT IMPLEMENTED: " + metric);
					sap = null;
				}
				if (Math.abs(sap.score - best.score) < 0.001) {
					// identical or near identical score
					best.addAttributeClass(sap.attributeClass);
				}
				if (sap.score > best.score) {
					best = new ScoreAttributesPair(sap.score, sap.attributeClass);
				}
			}
			atts.addAll(best.attributeClassSet);
			bestMatches.add(best); // TODO - do something with this
			total += best.score;
			n++;
		}
		ScoreAttributesPair sap = new ScoreAttributesPair(total / n, atts);
		return sap;
	}
	
	public void search(Set<OWLClass> atts, Metric metric) {
		Set<Node<OWLClass>> iatts = new HashSet<Node<OWLClass>>();
		for (OWLClass att : atts) {
			iatts.addAll(getNamedReflexiveSubsumers(att));
		}
		for (OWLNamedIndividual j : this.getAllElements()) {
			Set<Node<OWLClass>> jatts = this.getInferredAttributes(j);
			Set<Node<OWLClass>> attsInBoth = new HashSet<Node<OWLClass>>(iatts);
			iatts.retainAll(jatts);
			Set<Node<OWLClass>> attsInEither = new HashSet<Node<OWLClass>>(iatts);
			iatts.addAll(jatts);
			double simj = attsInBoth.size() / attsInEither.size();
			// TODO
		}
	}

	//
	// ENRICHMENT
	//

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
		public OWLClass enrichedClass; // attribute being tested

		public OWLClass sampleSetClass; // e.g. gene set

		public Double pValue;

		public Double pValueCorrected;

		public EnrichmentResult(OWLClass sampleSetClass, OWLClass enrichedClass,
				double pValue, double pValueCorrected) {
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
			return sampleSetClass + " " + enrichedClass + " " + pValue + " "
			+ pValueCorrected;
		}

	}

	private void addEnrichmentResult(EnrichmentResult result,
			List<EnrichmentResult> results) {
		if (result == null)
			return;
		if (enrichmentConfig != null) {
			if (enrichmentConfig.pValueCorrectedCutoff != null
					&& result.pValueCorrected > enrichmentConfig.pValueCorrectedCutoff) {
				return;
			}
			if (enrichmentConfig.attributeInformationContentCutoff != null
					&& this.getInformationContentForAttribute(result.enrichedClass) < enrichmentConfig.attributeInformationContentCutoff) {
				return;
			}

		}
		LOG.info(result);
		results.add(result);
	}

	/**
	 * @param populationClass
	 * @param pc1
	 *          - sample set root class
	 * @param pc2
	 *          - enriched set root class
	 * @return enrichment results
	 * @throws MathException
	 */
	public List<EnrichmentResult> calculateAllByAllEnrichment(
			OWLClass populationClass, OWLClass pc1, OWLClass pc2)
			throws MathException {
		List<EnrichmentResult> results = new Vector<EnrichmentResult>();
		OWLClass nothing = this.owlDataFactory.getOWLNothing();
		for (OWLClass sampleSetClass : getReasoner().getSubClasses(pc1, false)
				.getFlattened()) {
			if (sampleSetClass.equals(nothing)) continue;
			int sampleSetSize = getNumElementsForAttribute(sampleSetClass);
			LOG.info("sample set class:" + sampleSetClass + " size="+sampleSetSize);
			if (sampleSetSize < 2)
				continue;
			List<EnrichmentResult> resultsInner = new Vector<EnrichmentResult>();
			for (OWLClass enrichedClass : this.getReasoner()
					.getSubClasses(pc2, false).getFlattened()) {
				if (enrichedClass.equals(nothing)) continue;
				if (getNumElementsForAttribute(enrichedClass) < 2)
					continue;
				if (sampleSetClass.equals(enrichedClass)
						|| this.getNamedSubsumers(enrichedClass).contains(sampleSetClass)
						|| this.getNamedSubsumers(sampleSetClass).contains(enrichedClass)) {
					continue;
				}
				EnrichmentResult result = calculatePairwiseEnrichment(populationClass,
						sampleSetClass, enrichedClass);
				addEnrichmentResult(result, resultsInner);
			}
			// LOG.info("sorting results:"+resultsInner.size());
			Collections.sort(resultsInner);
			// LOG.info("sorted results:"+resultsInner.size());
			results.addAll(resultsInner);
		}
		LOG.info("enrichment completed");
		// Collections.sort(results);
		return results;
	}

	public List<EnrichmentResult> calculateEnrichment(OWLClass populationClass,
			OWLClass sampleSetClass) throws MathException {
		List<EnrichmentResult> results = new Vector<EnrichmentResult>();
		for (OWLClass enrichedClass : this.getReasoner()
				.getSubClasses(populationClass, false).getFlattened()) {
			LOG.info("Enrichment test for: " + enrichedClass + " vs "
					+ populationClass);
			results.add(calculatePairwiseEnrichment(populationClass, sampleSetClass,
					enrichedClass));
		}
		Collections.sort(results);
		return results;
	}

	/**
	 * @param populationClass
	 * @param sampleSetClass
	 * @param enrichedClass
	 * @return enrichment result
	 * @throws MathException
	 */
	public EnrichmentResult calculatePairwiseEnrichment(OWLClass populationClass,
			OWLClass sampleSetClass, OWLClass enrichedClass) throws MathException {

		// LOG.info("Hyper :"+populationClass
		// +" "+sampleSetClass+" "+enrichedClass);
		int populationClassSize = getNumElementsForAttribute(populationClass);
		int sampleSetClassSize = getNumElementsForAttribute(sampleSetClass);
		int enrichedClassSize = getNumElementsForAttribute(enrichedClass);
		// LOG.info("Hyper :"+populationClassSize
		// +" "+sampleSetClassSize+" "+enrichedClassSize);
		Set<OWLNamedIndividual> eiSet = getElementsForAttribute(sampleSetClass);
		eiSet.retainAll(this.getElementsForAttribute(enrichedClass));
		if (eiSet.size() == 0) {
			return null;
		}
		LOG.info(" shared elements: "+eiSet.size()+" for "+enrichedClass);
		HypergeometricDistributionImpl hg = new HypergeometricDistributionImpl(
				populationClassSize, sampleSetClassSize, enrichedClassSize);
		/*
		 * LOG.info("popsize="+getNumElementsForAttribute(populationClass));
		 * LOG.info("sampleSetSize="+getNumElementsForAttribute(sampleSetClass));
		 * LOG.info("enrichedClass="+getNumElementsForAttribute(enrichedClass));
		 */
		// LOG.info("both="+eiSet.size());
		double p = hg.cumulativeProbability(eiSet.size(),
				Math.min(sampleSetClassSize, enrichedClassSize));
		double pCorrected = p * getCorrectionFactor(populationClass);
		return new EnrichmentResult(sampleSetClass, enrichedClass, p, pCorrected);
	}

	// hardcode bonferoni for now
	Integer correctionFactor = null; // todo - robust cacheing

	private int getCorrectionFactor(OWLClass populationClass) {
		if (correctionFactor == null) {
			int n = 0;
			for (OWLClass sc : this.getReasoner()
					.getSubClasses(populationClass, false).getFlattened()) {
				LOG.info("testing count for " + sc);
				if (getNumElementsForAttribute(sc) > 1) {
					n++;
					LOG.info("  ++testing count for " + sc);
				}
			}

			correctionFactor = n;
		}
		return correctionFactor;
	}

	/**
	 * returns all attribute classes - i.e. the classes used to annotate the
	 * elements (genes, diseases, etc) being studied
	 * 
	 * defaults to all classes in source ontology signature
	 * 
	 * @return set of classes
	 */
	public Set<OWLClass> getAllAttributeClasses() {
		if (cachedAttributeClasses == null)
			return sourceOntology.getClassesInSignature(true);
		else
			return new HashSet<OWLClass>(cachedAttributeClasses);
	}

	/**
	 * assumes that the ontology contains both attributes (TBox) and elements +
	 * associations (ABox)
	 */
	// TODO - make this private & call automatically
	public void createElementAttributeMapFromOntology() {
		elementToAttributesMap = new HashMap<OWLNamedIndividual,Set<OWLClass>>();
		Set<OWLClass> allTypes = new HashSet<OWLClass>();
		for (OWLNamedIndividual e : sourceOntology.getIndividualsInSignature(true)) {

			// The attribute classes for an individual are the direct inferred
			// named types. We assume that grouping classes have already been
			// generated.
			// if they have not then the types may be as general as {Thing}
			Set<OWLClass> types = getReasoner().getTypes(e, true).getFlattened();
			allTypes.addAll(addElement(e, types));
		}
		// need to materialize as classes...
		LOG.info("Using " + allTypes.size()
				+ " attribute classes, based on individuals: "
				+ sourceOntology.getIndividualsInSignature(true).size());
		cachedAttributeClasses = allTypes;
	}

	// adds an element plus associated attributes
	private Set<OWLClass> addElement(OWLNamedIndividual e, Set<OWLClass> atts) {
		// TODO - fully fold TBox so that expressions of form (inh (part_of x))
		// generate a class "part_of x", to ensure that a SEP grouping class is
		// created
		Set<OWLClass> attClasses = new HashSet<OWLClass>();
		for (OWLClass attClass : atts) {

			// filtering, e.g. Type :human. This is a somewhat unsatisfactory way to
			// do this;
			// better to filter at the outset - TODO
			if (attClass instanceof OWLClass && ignoreSubClassesOf != null && ignoreSubClassesOf.size() > 0) {
				if (getReasoner().getSuperClasses(attClass, false).getFlattened().retainAll(ignoreSubClassesOf)) {
					continue;
				}
			}
			if (!this.attributeToElementsMap.containsKey(attClass))
				attributeToElementsMap.put(attClass, new HashSet<OWLNamedIndividual>());
			attributeToElementsMap.get(attClass).add(e);
			attClasses.add(attClass);
		}

		// note this only caches direct associations
		// TODO - cache indirect here
		this.elementToAttributesMap.put(e, attClasses);
		return attClasses;
	}

	/**
	 * Gets all attribute classes used to describe individual element e.
	 * 
	 * Includes inferred classes
	 * @param e
	 * @return
	 */
	public Set<OWLClass> getAttributesForElement(OWLNamedIndividual e) {
		if (elementToAttributesMap == null)
			createElementAttributeMapFromOntology();
		return new HashSet<OWLClass>(elementToAttributesMap.get(e));
	}

	/**
	 * Given an individual element, return all direct attribute classes that the individual instantiates,
	 * combined with IC.
	 * 
	 * Results are ordered with highest IC first
	 * 
	 * @param e
	 * @return
	 */
	public List<ScoreAttributesPair> getScoredAttributesForElement(OWLNamedIndividual e) {
		List<ScoreAttributesPair> saps = new ArrayList<ScoreAttributesPair>();
		for (Node<OWLClass> n : getReasoner().getTypes(e, true)) {
			double score = getInformationContentForAttribute(n.getRepresentativeElement());
			ScoreAttributesPair sap = new ScoreAttributesPair(score);
			sap.setAttributeClassSet(n.getEntities());
			saps.add(sap);
		}
		Collections.sort(saps);
		return saps;
	}


	/**
	 * Mapping between an attribute (e.g. phenotype class) and the number of
	 * instances it classifies
	 */
	protected void precomputeAttributeElementCount() {
		if (attributeElementCount != null) return;
		attributeElementCount = new HashMap<OWLClass, Integer>();
		// some high level attributes will classify all or most of the ABox;
		// this way may be faster...
		for (OWLNamedIndividual e : this.getAllElements()) {
			LOG.info("Incrementing count all attributes of " + e);
			LOG.info(" DIRECT ATTS: " + getAttributesForElement(e).size());
			for (Node<OWLClass> n : this.getInferredAttributes(e)) {
				for (OWLClass c : n.getEntities()) {
					if (!attributeElementCount.containsKey(c))
						attributeElementCount.put(c, 1);
					else
						attributeElementCount.put(c, attributeElementCount.get(c) + 1);
				}
			}
		}
		LOG.info("Finished precomputing attribute element count");
	}

	/**
	 * inferred
	 * 
	 * @param c
	 * @return set of entities
	 */
	public Set<OWLNamedIndividual> getElementsForAttribute(OWLClass c) {
		Set<OWLClass> subclasses = getReasoner().getSubClasses(c, false)
		.getFlattened();
		subclasses.add(c);
		Set<OWLNamedIndividual> elts = new HashSet<OWLNamedIndividual>();
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
		if (attributeElementCount == null) precomputeAttributeElementCount();
		if (attributeElementCount.containsKey(c))
			return attributeElementCount.get(c);
		// DEPRECATED:
		LOG.info("Uncached count for: " + c);
		int num;
		try {
			num = getElementsForAttribute(c).size();
		} catch (Exception e) {
			LOG.error("cannot fetch elements for: " + c);
			LOG.error(e);
			num = this.getCorpusSize();
		}
		attributeElementCount.put(c, num);
		return num;
	}

	public Set<OWLNamedIndividual> getAllElements() {
		if (elementToAttributesMap == null)
			createElementAttributeMapFromOntology();
		return elementToAttributesMap.keySet();
	}

	/**
	 * @return the number of entities in the entire set being analyzed
	 */
	public int getCorpusSize() {
		if (corpusSize == null) {
			corpusSize = getAllElements().size();
			LOG.info("corpusSize = " + corpusSize);
		}
		return corpusSize;
	}

	public void setCorpusSize(int size) {
		corpusSize = size;
	}

	// IC = 0.0 : 100% (1/1)
	// IC = 1.0 : 50% (1/2)
	// IC = 2.0 : 25% (1/4)
	// IC = 3.0 : 12.5% (1/8)
	/**
	 * IC = -(log {@link #getNumElementsForAttribute(c)} / corpus size) / log(2)
	 * @param c
	 * @return Information Content value for a given class
	 */
	public Double getInformationContentForAttribute(OWLClass c) {
		if (icCache.containsKey(c)) return icCache.get(c);
		if (this.isICCacheFullyPopulated) {
			return null;
		}
		int freq = getNumElementsForAttribute(c);
		Double ic = null;
		if (freq > 0) {
			ic = -Math.log(((double) (freq) / getCorpusSize())) / Math.log(2);
		}
		icCache.put(c, ic);
		return ic;
	}

	/**
	 * @param m - metric name
	 * @return boolean the given metric m is a measurement that uses IC values
	 */
	public Boolean isICmetric(String m) {
		for (Metric metric : Metric.values()) {
			if (m.equals(metric.toString())) {
				return metric.isICmetric();
			}
		}
		return false;
	}

	/**
	 * @param m - metric name
	 * @return boolean of the given metric m is a measurement that uses J values
	 */
	public Boolean isJmetric(String m) {
		for (Metric metric : Metric.values()) {
			if (m.equals(metric.toString())) {
				return metric.isJmetric();
			}
		}
		return false;
	}

	// ---------------
	// CACHE I/O
	// ---------------

	final String prefix = "http://purl.obolibrary.org/obo/";
	private String getShortId(OWLClassExpression c) {
		IRI x = ((OWLClass) c).getIRI();
		return x.toString().replace(prefix, ""); // todo - do not hardcode
	}
	private OWLClass getOWLClassFromShortId(String id) {
		return owlDataFactory.getOWLClass(IRI.create(prefix + id));
	}

	/**
	 * Saves the contents of the LCS-IC cache
	 * 
	 * Assumes that this has already been filled by comparing all classes by all classes.
	 * 
	 * @param fileName
	 * @throws IOException
	 */
	public void saveLCSCache(String fileName) throws IOException {
		saveLCSCache(fileName, null);
	}
	public void saveLCSCache(String fileName, Double thresholdIC) throws IOException {
		FileOutputStream fos = new FileOutputStream(fileName);
		for (ClassExpressionPair p : lcsICcache.keySet()) {
			ScoreAttributePair sap = lcsICcache.get(p);
			if (thresholdIC != null && sap.score < thresholdIC) {
				continue;
			}
			IOUtils.write(getShortId(p.c1) +"\t" + getShortId(p.c2) + "\t" + sap.score + "\t" + 
					getShortId(sap.attributeClass) + "\n", fos);
		}
		fos.close();
	}

	/**
	 * @param fileName
	 * @throws IOException
	 */
	public void loadLCSCache(String fileName) throws IOException {
		lcsICcache = new HashMap<ClassExpressionPair, ScoreAttributePair>();
		FileInputStream s = new FileInputStream(fileName);
		List<String> lines = IOUtils.readLines(s);
		for (String line : lines) {
			String[] vals = line.split("\t");
			OWLClass c1 = getOWLClassFromShortId(vals[0]);
			OWLClass c2 = getOWLClassFromShortId(vals[1]);
			OWLClass a = getOWLClassFromShortId(vals[3]);
			lcsICcache.put(new ClassExpressionPair(c1,c2),
					new ScoreAttributePair(Double.valueOf(vals[2]), a));
		}
		isLCSCacheFullyPopulated = true;
	}

	public final String icIRIString = "http://owlsim.org/ontology/ic"; // TODO

	public OWLOntology cacheInformationContentInOntology() throws OWLOntologyCreationException {
		OWLOntology o = this.owlOntologyManager.createOntology();
		OWLAnnotationProperty p = owlDataFactory.getOWLAnnotationProperty(IRI.create(icIRIString));
		for (OWLClass c : sourceOntology.getClassesInSignature()) {
			Double ic = this.getInformationContentForAttribute(c);
			if (ic != null) {
				owlOntologyManager.addAxiom(o,
						this.owlDataFactory.getOWLAnnotationAssertionAxiom(p, 
								c.getIRI(), 
								owlDataFactory.getOWLLiteral(ic)));
			}

		}
		return o;
	}

	public void setInformationContentFromOntology(OWLOntology o) {
		icCache = new HashMap<OWLClass, Double>();
		for (OWLAnnotationAssertionAxiom ax : o.getAxioms(AxiomType.ANNOTATION_ASSERTION)) {
			if (ax.getProperty().getIRI().toString().equals(icIRIString)) {
				OWLLiteral lit = (OWLLiteral) ax.getValue();
				icCache.put(owlDataFactory.getOWLClass((IRI) ax.getSubject()), 
						lit.parseDouble());
			}
		}
	}

	public Double getEntropy() {
		return getEntropy(getAllAttributeClasses());
	}

	public Double getEntropy(Set<OWLClass> cset) {
		double e = 0.0;
		for (OWLClass c : cset) {
			int freq = getNumElementsForAttribute(c);
			if (freq == 0)
				continue;
			double p = ((double) freq) / getCorpusSize();
			e += p * Math.log(p) ;
		}
		return -e / Math.log(2);
	}


}
