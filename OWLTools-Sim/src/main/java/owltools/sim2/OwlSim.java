package owltools.sim2;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.math.MathException;
import org.apache.commons.math3.stat.descriptive.StatisticalSummaryValues;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.sim2.SimpleOwlSim.Direction;
import owltools.sim2.SimpleOwlSim.Metric;
import owltools.sim2.io.SimResultRenderer.AttributesSimScores;
import owltools.sim2.scores.AttributePairScores;
import owltools.sim2.scores.ElementPairScores;

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
 * <li> <i>Type(i)</i> : {@link OWLReasoner#getTypes(OWLNamedIndividual, boolean)}
 *  - to find all of the attributes possessed
 * by an entity - may be direct or indirect. The inverse operation is <i>Inst(c)</i>
 * <li> <i>Sub(c)</i> :  {@link OWLReasoner#getSuperClasses(OWLClassExpression, boolean)} 
 *  - to find all the ancestors (subsumers) of an attribute - direct and indirect.
 * The inverse operation is <i>SubBy(c)</i>
 * <li> <i>Eq(c)</i> : {@link OWLReasoner#getEquivalentClasses(OWLClassExpression)} - find all all equivalent attributes/
 * Note that OWLSim is cycle-safe - cycles over SubClassOf are permitted, these are the same as an equivalence axiom between the classes
 * </ul>
 * 
 * The latter two are combined to yield the <i>reflexive</i> common subsumers list - 
 * see {@link #getNamedReflexiveSubsumers(OWLClass)}. We write this here as
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
 * This is implemented by {@link #getLowestCommonSubsumerWithIC(OWLClass, OWLClass)}
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
 *  subsumer. See {@link #getLowestCommonSubsumerWithIC(OWLClass, OWLClass)}
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
public interface OwlSim {
	
	//TODO: replace with enum
	public String[] metrics = {"bmaAsymIC","bmaSymIC","bmaInverseAsymIC", "combinedScore", "simJ", "simGIC","maxIC"};


	public enum Stat {
		MEAN,MIN,MAX,N,SUM
	}


	/**
	 * @return ontology used to store both classes and individuals
	 */
	public OWLOntology getSourceOntology();

	/**
	 * @return reasoner initialized with source ontology
	 */
	public OWLReasoner getReasoner();

	/**
	 * @param a
	 * @return Sub(c)
	 */
	public Set<Node<OWLClass>> getNamedSubsumers(OWLClass a);

	/**
	 * 
	 * @param i
	 * @return Type(i) - nodes for all classes that a instantiates - direct and inferred
	 */
	public Set<Node<OWLClass>> getInferredAttributes(
			OWLNamedIndividual i);

	/**
	 * 
	 * @param a
	 * @return anc(a)
	 */
	public Set<Node<OWLClass>> getNamedReflexiveSubsumers(
			OWLClass a);

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
	 * @throws UnknownOWLClassException 
	 */
	public Set<Node<OWLClass>> getNamedCommonSubsumers(
			OWLClass a, OWLClass b) throws UnknownOWLClassException;

	/**
	 * <pre>
	 *  | CS(i,j) | = | { c : c &isin; Type(i), c &isin; Type(j) } |
	 * </pre>
	 * @param a
	 * @param b
	 * @return | CS(a,b) | 
	 * @throws UnknownOWLClassException
	 */
	public int getNamedCommonSubsumersCount(OWLClass a,
			OWLClass b) throws UnknownOWLClassException;

	/**
	 * <pre>
	 *   CS(i,j) = { c : c &isin; Type(i), c &isin; Type(j) }
	 * </pre>
	 * 
	 * @param a
	 * @param b
	 * @return CS(i,j)
	 * @throws UnknownOWLClassException 
	 */
	public Set<Node<OWLClass>> getNamedCommonSubsumers(
			OWLNamedIndividual a, OWLNamedIndividual b) throws UnknownOWLClassException;

	/**
	 * <pre>
	 *   CS<sub>redundant</sub>(a,b) = { c : c &in; CS(a,b), &E; c' : c' &in; CS(a,b), c' &in Sub(c) }
	 *   LCS(a,b) = CS(a,b) - CS<SUB>redundant</SUB>
	 * </pre>
	 * 
	 * @param a
	 * @param b
	 * @return LCS(a,b)
	 * @throws UnknownOWLClassException 
	 */
	public Set<Node<OWLClass>> getNamedLowestCommonSubsumers(
			OWLClass a, OWLClass b) throws UnknownOWLClassException;

	/**
	 * This method provides a generic wrapper onto other attribute-based similarity
	 * methods
	 * 
	 * Compares two classes (attributes) according to the specified metric
	 * 
	 * @param a
	 * @param b
	 * @param metric
	 * @return Sim<sub>M</sub>(a,b)
	 * @throws UnknownOWLClassException 
	 */
	public double getAttributeSimilarity(OWLClass a,
			OWLClass b, Metric metric) throws UnknownOWLClassException;

	/**
	 * <pre>
	 * SimJ(a,b) = | anc(a) &cap; anc(b) | / | anc(a) &cup; anc(b) |
	 * </pre>
	 * 
	 * @param a
	 * @param b
	 * @return SimJ of two attribute classes
	 * @throws UnknownOWLClassException 
	 */
	public double getAttributeJaccardSimilarity(OWLClass a,
			OWLClass b) throws UnknownOWLClassException;



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
	 * @throws UnknownOWLClassException 
	 */
	public double getElementJaccardSimilarity(OWLNamedIndividual i,
			OWLNamedIndividual j) throws UnknownOWLClassException;

	/**
	 * <pre>
	 * SimJ(i,j) = | Type(i) &cap; Type(j) | / | Type(j) |
	 * </pre>
	 * 
	 * 
	 * @param i
	 * @param j
	 * @return SimJ(i,j)
	 * @throws UnknownOWLClassException
	 */
	public double getAsymmetricElementJaccardSimilarity(OWLNamedIndividual i,
			OWLNamedIndividual j) throws UnknownOWLClassException;

	/**
	 * <pre>
	 * SimJ<sup>ASym</sup>(c,d) = | anc(c) &cap; anc(d) | / | anc(d)  |
	 * </pre>
	 * 
	 * If c is subsumed by d then the score is 1. i.e.
	 * <pre>
	 * c &in; Sub(d) &rarr;  SimJ<sup>ASym</sup>(c,d) = 1
	 * <pre>
	 * 
	 * @param c
	 * @param d
	 * @return Asymmetric SimJ of two attribute classes
	 * @throws UnknownOWLClassException 
	 */
	public double getAsymmetricAttributeJaccardSimilarity(
			OWLClass c, OWLClass d) throws UnknownOWLClassException;

	/**
	 * sums of IC of the intersection attributes/ sum of IC of union attributes.
	 * <img src=
	 * "http://www.pubmedcentral.nih.gov/picrender.fcgi?artid=2238903&blobname=gkm806um8.jpg"
	 * alt="formula for simGIC"/>
	 * 
	 * @param i
	 * @param j
	 * @return Σ<sub>t &in; Type(i) &cap; Type(j)</sub> IC(t) / Σ<sub>t &in; Type(i) &cup; Type(j)</sub> IC(t)
	 * @throws UnknownOWLClassException 
	 */
	public double getElementGraphInformationContentSimilarity(
			OWLNamedIndividual i, OWLNamedIndividual j) throws UnknownOWLClassException;

	/**
	 * sums of IC of the intersection attributes/ sum of IC of union attributes.
	 * <img src=
	 * "http://www.pubmedcentral.nih.gov/picrender.fcgi?artid=2238903&blobname=gkm806um8.jpg"
	 * alt="formula for simGIC"/>
	 * 
	 * 
	 * @param c
	 * @param d
	 * @return Σ<sub>t &in; RSub(c) &cap; RSub(d)</sub> IC(t) / Σ<sub>t &in; RSub(c) &cup; RSub(d)</sub> IC(t)
	 * @throws UnknownOWLClassException
	 */
	public double getAttributeGraphInformationContentSimilarity(
			OWLClass c, OWLClass d) throws UnknownOWLClassException;

	/**
	 * Find the inferred attribute shared by both i and j that has highest IC. If
	 * there is a tie then the resulting structure will have multiple classes.
	 * 
	 * <pre>
	 * MaxIC(i,j) = max { IC(c) : c &in; LCS(i,j) }
	 * C<sub>MaxIC(i,j)</sub> =  { c : c &in; LCS(i,j), IC(c) = MaxIC(i,j) }
	 * </pre>
	 * 
	 * As a convenience, this method also returns the LCS class as well as the IC
	 * 
	 * This is the same metric used by Lord et al in <a href="http://bioinformatics.oxfordjournals.org/cgi/reprint/19/10/1275">
	 * Investigating semantic similarity measures  across the Gene Ontology: the relationship between sequence and annotation</a>.
	 * 
	 * @param i
	 * @param j
	 * @return <MaxIC(i,j) C<sub>MaxIC(i,j)</sub> 
	 * @throws UnknownOWLClassException 
	 */
	public ScoreAttributeSetPair getSimilarityMaxIC(
			OWLNamedIndividual i, OWLNamedIndividual j) throws UnknownOWLClassException;

	/**
	 * @param i
	 * @param j
	 * @return BMA<sub>Asym</sub>(i,j)
	 */
	@Deprecated
	public ScoreAttributeSetPair getSimilarityBestMatchAverageAsym(
			OWLNamedIndividual i, OWLNamedIndividual j);

	/**
	 * @param c
	 * @param d
	 * @return scores
	 * @throws UnknownOWLClassException
	 */
	@Deprecated
	public AttributePairScores getPairwiseSimilarity(OWLClass c, OWLClass d) throws UnknownOWLClassException;

	/**
	 * Perform multiple groupwise similarity measures on (i,j) 
	 * 
	 * @param i
	 * @param j
	 * @return scores
	 * @throws UnknownOWLClassException
	 */
	public ElementPairScores getGroupwiseSimilarity(OWLNamedIndividual i,
			OWLNamedIndividual j) throws UnknownOWLClassException;


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
	public ScoreAttributeSetPair getSimilarityBestMatchAverageAsym(
			OWLNamedIndividual i, OWLNamedIndividual j, Metric metric);

	/**
	 * @param i
	 * @param j
	 * @param metric
	 * @param dir
	 * @return scores
	 */
	@Deprecated
	public ScoreAttributeSetPair getSimilarityBestMatchAverage(
			OWLNamedIndividual i, OWLNamedIndividual j, Metric metric,
			Direction dir);

	// Possible optimizations:

	/**
	 * Equivalent to {@link #getAttributeJaccardSimilarity(OWLClass, OWLClass)} * 100
	 * @param a
	 * @param b
	 * @return simj*100
	 * @throws UnknownOWLClassException
	 */
	public int getAttributeJaccardSimilarityAsPercent(OWLClass a,
			OWLClass b) throws UnknownOWLClassException;

	/**
	 * Equivalent to {@link #getElementJaccardSimilarity(OWLNamedIndividual, OWLNamedIndividual)} * 100
	 * @param i
	 * @param j
	 * @return simj*100
	 * @throws UnknownOWLClassException
	 */
	public int getElementJaccardSimilarityAsPercent(OWLNamedIndividual i,
			OWLNamedIndividual j) throws UnknownOWLClassException;

	/**
	 * Equivalent to {@link #getAsymmetricAttributeJaccardSimilarity(OWLClass, OWLClass)} * 100
	 * @param a
	 * @param b
	 * @return asimj*100
	 * @throws UnknownOWLClassException
	 */
	public int getAsymmetricAttributeJaccardSimilarityAsPercent(OWLClass a,
			OWLClass b) throws UnknownOWLClassException;

	/**
	 * Equivalent to {@link #getAsymmetricElementJaccardSimilarity(OWLNamedIndividual, OWLNamedIndividual)} * 100
	 * @param i
	 * @param j
	 * @return asimj*100
	 * @throws UnknownOWLClassException
	 */
	public int getAsymmetricElementJaccardSimilarityAsPercent(OWLNamedIndividual i,
			OWLNamedIndividual j) throws UnknownOWLClassException;

	/**
	 * returns all attribute classes - i.e. the classes used to annotate the
	 * elements (genes, diseases, etc) being studied, either directly or indirectly
	 * 
	 * defaults to all classes in source ontology signature
	 * 
	 * @return set of classes
	 */
	public Set<OWLClass> getAllAttributeClasses();

	/**
	 * This must be called prior to calculation of any similarity metrics.
	 * 
	 * It creates a set of attribute classes (the subset of classes that
	 * have inferred or direct individuals) and the set of elements (individuals),
	 * together with any internal indexing
	 * 
	 * assumes that the ontology contains both attributes (TBox) and elements +
	 * associations (ABox)
	 * 
	 * @throws UnknownOWLClassException 
	 */
	public void createElementAttributeMapFromOntology() throws UnknownOWLClassException;

	/**
	 * For implementations that use a cache, this performs an all by all comparison
	 * of all attributes and caches the results.
	 * 
	 * @throws UnknownOWLClassException
	 */
	public void precomputeAttributeAllByAll()  throws UnknownOWLClassException;

	/**
	 * Gets all attribute classes used to describe individual element e.
	 * 
	 * Does *not* include inferred classes
	 * @param e
	 * @return Type(e,indirect)
	 * @throws UnknownOWLClassException 
	 */
	public Set<OWLClass> getAttributesForElement(OWLNamedIndividual e) throws UnknownOWLClassException;

	/**
	 * inferred
	 * 
	 * @param c
	 * @return set of entities
	 * @throws UnknownOWLClassException 
	 */
	public Set<OWLNamedIndividual> getElementsForAttribute(OWLClass c) throws UnknownOWLClassException;

	/**
	 * |{e : e &isin; Inst(c)}|
	 * 
	 * @param c
	 * @return count
	 * @throws UnknownOWLClassException 
	 */
	public int getNumElementsForAttribute(OWLClass c) throws UnknownOWLClassException;

	/**
	 * @return E = {e : e &isin; Inst(Τ)}
	 */
	public Set<OWLNamedIndividual> getAllElements();

	/**
	 * The number of entities in the entire set being analyzed.
	 * 
	 * Typically this is
	 * |{e : e &isin; Inst(Τ)}|
	 * But this can be overridden (for the scenario where the ABox is a subset
	 * of the known universe)
	 * 
	 * @return corpus size 
	 */
	public int getCorpusSize();

	/**
	 * Sets the number of entities in domain. Can be used to override
	 * the default amount, which is the number of elements
	 * 
	 * @param size
	 */
	public void setCorpusSize(int size);

	// IC = 0.0 : 100% (1/1)
	// IC = 1.0 : 50% (1/2)
	// IC = 2.0 : 25% (1/4)
	// IC = 3.0 : 12.5% (1/8)
	/**
	 * IC = -(log {@link #getNumElementsForAttribute(OWLClass)} / corpus size) / log(2)
	 * @param c
	 * @return Information Content value for a given class
	 * @throws UnknownOWLClassException 
	 */
	public Double getInformationContentForAttribute(OWLClass c) throws UnknownOWLClassException;

	/**
	 * @return -Σ<sub>i</sub>[ (P(x<sub>i</sub>)log<sub>2</sub>P(x<sub>i</sub>)]
	 * @throws UnknownOWLClassException 
	 */
	public Double getEntropy() throws UnknownOWLClassException;

	/**
	 * @param cset
	 * @return -Σ<sub>i</sub>[ (P(x<sub>i</sub>)log<sub>2</sub>P(x<sub>i</sub>)] for x<sub>i</sub> &isin; cset
	 * @throws UnknownOWLClassException 
	 */
	public Double getEntropy(Set<OWLClass> cset) throws UnknownOWLClassException;

	/**
	 * @param c - query class
	 * @param ds - target class set
	 * @return list of scores
	 * @throws UnknownOWLClassException
	 */
	public List<AttributesSimScores> compareAllAttributes(OWLClass c, Set<OWLClass> ds) throws UnknownOWLClassException;

	/**
	 * @param atts
	 * @param targetIdSpace
	 * @return sorted scores
	 * @throws UnknownOWLClassException
	 */
	public List<ElementPairScores> findMatches(Set<OWLClass> atts, String targetIdSpace) throws UnknownOWLClassException;

	/**
	 * @param i
	 * @param targetIdSpace
	 * @return sorted scores
	 * @throws UnknownOWLClassException
	 */
	public List<ElementPairScores> findMatches(OWLNamedIndividual i, String targetIdSpace) throws UnknownOWLClassException;

	/**
	 * Resnick similarity measure, together with all LCSs (MICAs) that
	 * have this as IC
	 * 
	 * @param c
	 * @param d
	 * @return LCS together with IC(LCS)
	 * @throws UnknownOWLClassException
	 */
	public ScoreAttributeSetPair getLowestCommonSubsumerWithIC(OWLClass c, OWLClass d)
			throws UnknownOWLClassException;

	public ScoreAttributeSetPair getLowestCommonSubsumerWithLinScore(OWLClass c, OWLClass d)
			throws UnknownOWLClassException;

	
	/**
	 * @param i
	 * @param j
	 * @param thresh
	 * @return LCS together with IC(LCS)
	 * @throws UnknownOWLClassException
	 */
	public ScoreAttributeSetPair getLowestCommonSubsumerWithIC(OWLClass i, OWLClass j, Double thresh)
			throws UnknownOWLClassException;

	/**
	 * Experimental: save internal state of object
	 * @param fileName
	 * @throws IOException
	 */
	public void saveState(String fileName) throws IOException;
	
	/**
	 * Saves the contents of the LCS-IC cache
	 * 
	 * Assumes that this has already been filled by comparing all classes by all classes.
	 * 
	 * 
	 * @param fileName
	 * @throws IOException
	 */
	public void saveLCSCache(String fileName) throws IOException;

	/**
	 * As {@link #saveLCSCache(String)}, but do not write a line for any LCS
	 * whose IC falls below the threshold.
	 * 
	 * Use this if you expect the size of the cache to be large. If not set,
	 * the number of lines will be |A| x |A|
	 * 
	 * @param fileName
	 * @param thresholdIC
	 * @throws IOException
	 */
	public void saveLCSCache(String fileName, Double thresholdIC) throws IOException;

	/**
	 * Loads a pregenerated IC cache.
	 * 
	 * May or may not set IC values of LCS classes - implementation dependent
	 * 
	 * @param fileName
	 * @throws IOException
	 */
	public void loadLCSCache(String fileName) throws IOException;

	/**
	 * Generates an ontology containing annotation assertion axioms connecting
	 * a class to an IC value.
	 * 
	 * Can be used to persist class to IC mappings
	 * 
	 * @return ontology containing annotation assertions
	 * @throws OWLOntologyCreationException
	 * @throws UnknownOWLClassException
	 */
	public OWLOntology cacheInformationContentInOntology() throws OWLOntologyCreationException, UnknownOWLClassException;


	/**
	 * Populate the IC cache from an ontology of class-IC mappings.
	 * 
	 * Can be used to restore an IC cache from a previously persisted state.
	 * 
	 * 
	 * @param o
	 */
	public void setInformationContentFromOntology(OWLOntology o);

	/**
	 *  Ff set, the cache is written to but not read from.
	 *  
	 *  Set this is you are building a cached to be persisted.
	 * 
	 * @param b
	 */
	public void setNoLookupForLCSCache(boolean b);

	/**
	 * A pair consisting of a set of equal-scoring attributes, and a score
	 * 
	 * @author cjm
	 * 
	 */
	public class ScoreAttributeSetPair implements Comparable<ScoreAttributeSetPair> {
		/**
		 * Score shared by all attributes in attribute set
		 */
		public double score;

		/**
		 * Set of attributes that have the score
		 */
		public Set<OWLClass> attributeClassSet = new HashSet<OWLClass>(); // all
		// attributes
		// with
		// this
		// score

		/**
		 * @param score
		 * @param ac
		 */
		public ScoreAttributeSetPair(double score, OWLClass ac) {
			super();
			this.score = score;
			if (ac != null) attributeClassSet.add(ac);
		}

		/**
		 * @param score
		 * @param acs
		 */
		public ScoreAttributeSetPair(double score, Set<OWLClass> acs) {
			super();
			this.score = score;
			this.attributeClassSet = acs;
		}

		/**
		 * Combined/Summary score
		 * 
		 * May vary depending on the method used
		 * 
		 * @param score
		 */
		public ScoreAttributeSetPair(double score) {
			super();
			this.score = score;
		}

		/**
		 * Adds am attribute to set (first creating empty set if not already present)
		 * @param ac
		 */
		public void addAttributeClass(OWLClass ac) {
			if (attributeClassSet == null)
				attributeClassSet = new HashSet<OWLClass>();
			this.attributeClassSet.add(ac);
		}

		/**
		 * Setter
		 * @param acs
		 */
		public void setAttributeClassSet(Set<OWLClass> acs) {
			attributeClassSet = new HashSet<OWLClass>();
			for (OWLClass ac : acs)
				attributeClassSet.add(ac);
		}



		@Override
		public int compareTo(ScoreAttributeSetPair p2) {
			return 0 - Double.compare(score, p2.score);
		}

		/**
		 * @return an arbitrary member of the attribute class set
		 */
		@Deprecated
		public OWLClass getArbitraryAttributeClass() {
			if (attributeClassSet == null)
				return null;
			return this.attributeClassSet.iterator().next();
		}

	}

	/**
	 * 
	 * if set, the cache is neither read nor written to.
	 * 
	 * Set this if you expect to do each att comparison once, 
	 * and you do not wish to persist the cache when done.
	 * Contrast with {@link OwlSim#setNoLookupForLCSCache(boolean)}
	 * @param isDisableLCSCache
	 */
	public void setDisableLCSCache(boolean isDisableLCSCache);


	/**
	 * call when owlsim object is no longer required
	 */
	public void dispose();

	/**
	 * @param simProperties
	 */
	public void setSimProperties(Properties simProperties);
	/**
	 * @return properties
	 */
	public Properties getSimProperties();

	/**
	 * @return stats
	 */
	SimStats getSimStats();

	/**
	 * Writes timing information using log4j. For benchmarking
	 */
	public void showTimings();

	
	/**
	 * @return elementToAttributesMap
	 */
	public Map<OWLNamedIndividual, Set<OWLClass>> getElementToAttributesMap();
	// enrichment

	/**
	 * @return current configuration
	 */
	public EnrichmentConfig getEnrichmentConfig();

	/**
	 * @param enrichmentConfig
	 */
	public void setEnrichmentConfig(EnrichmentConfig enrichmentConfig);

	/**
	 * For every c &in; sample set, test c against all classes &in; enriched class set.
	 * 
	 * Uses {@link #calculatePairwiseEnrichment(OWLClass, OWLClass, OWLClass)}}
	 * 
	 * @param populationClass
	 * @param pc1
	 *          - sample set root class
	 * @param pc2
	 *          - enriched set root class
	 * @return enrichment results
	 * @throws MathException
	 * @throws UnknownOWLClassException 
	 */
	public List<EnrichmentResult> calculateAllByAllEnrichment(
			OWLClass populationClass, OWLClass pc1, OWLClass pc2)
					throws MathException, UnknownOWLClassException;


	/**
	 * 
	 * Uses {@link #calculatePairwiseEnrichment(OWLClass, OWLClass, OWLClass)}}
	 *
	 * @param populationClass
	 * @param sampleSetClass
	 * @return enrichment results
	 * @throws MathException
	 * @throws UnknownOWLClassException 
	 */
	public List<EnrichmentResult> calculateEnrichment(OWLClass populationClass,
			OWLClass sampleSetClass) throws MathException, UnknownOWLClassException;

	/**
	 * Pairwise enrichment test
	 * 
	 * @param populationClass
	 * @param sampleSetClass
	 * @param enrichedClass
	 * @return enrichment result
	 * @throws MathException
	 * @throws UnknownOWLClassException 
	 */
	public EnrichmentResult calculatePairwiseEnrichment(OWLClass populationClass,
			OWLClass sampleSetClass, OWLClass enrichedClass) throws MathException, UnknownOWLClassException;

	/**
	 * @param c
	 * @param d
	 * @return P(c|d) = P(c &cap; d)|p(d)
	 * @throws UnknownOWLClassException 
	 */
	public abstract double getConditionalProbability(OWLClass c, OWLClass d) throws UnknownOWLClassException;

	/**
	 * This will compute the individual summary statistics over the 
	 * set of anntoated classes to an individual i.
	 * @param i
	 * @return
	 * @throws UnknownOWLClassException
	 */
	public SummaryStatistics computeIndividualStats(OWLNamedIndividual i)
			throws UnknownOWLClassException;

	/**
	 * This function will compute the IC-based summary statistics over the set of 
	 * individuals for direct annotations.  It will generate a statistical 
	 * summary over each individual, as well as over the whole set.  
	 * @throws UnknownOWLClassException 
	 */
	public void computeSystemStats() throws UnknownOWLClassException;


	public StatisticalSummaryValues getSystemStats();

	/**
	 * Fetch the pre-computed summary values
	 * @return {@link SummaryStatistics} 
	 */
	public StatsPerIndividual getSummaryStatistics();
	
	public SummaryStatistics getSimStatistics(String stat);
	
	void calculateMetricStats(Set<OWLNamedIndividual> iset,
			Set<OWLNamedIndividual> jset) throws UnknownOWLClassException;
	
	public HashMap<String,SummaryStatistics> getMetricStats(Stat stat);

	public double calculateOverallAnnotationSufficiencyForIndividual(OWLNamedIndividual i) throws UnknownOWLClassException;

	public double calculateOverallAnnotationSufficiencyForAttributeSet(Set<OWLClass> atts) throws UnknownOWLClassException;

	public void computeSystemStatsForSubgraph(OWLClass c) throws UnknownOWLClassException;

	
	public class StatsPerIndividual {
		//TODO: make top-level class
		//TODO: getters and setters
		public SummaryStatistics mean;
		public SummaryStatistics min;
		public SummaryStatistics max;
		public SummaryStatistics n;
		public SummaryStatistics sum;
		public StatisticalSummaryValues aggregate;
		
		public StatsPerIndividual() {
			this.mean = new SummaryStatistics();
			this.min = new SummaryStatistics();
			this.max = new SummaryStatistics();
			this.n = new SummaryStatistics();
			this.sum = new SummaryStatistics();
		}

		public String toString() {
			String s = "";
			s+="individuals: "+n.getN()+"\n";
			s+="mean(n/indiv): "+String.format("%1$.5f", n.getMean())+"\n";
			s+="mean(meanIC): "+String.format("%1$.5f", mean.getMean())+"\n";
			s+="mean(maxIC): "+String.format("%1$.5f", max.getMean())+"\n";
			s+="max(maxIC): "+String.format("%1$.5f", max.getMax())+"\n";
			s+="mean(sumIC): "+String.format("%1$.5f", sum.getMean())+"\n";
			return s;
		}
	}

	/**
	 * {@link StatsPerIndividual} summary statistic calculations using {@link OWLClass} c as the root node
	 * @param c {@link OWLClass}
	 * @return {@link StatsPerIndividual} 
	 */
	public StatsPerIndividual getSummaryStatistics(OWLClass c);

	public double calculateSubgraphAnnotationSufficiencyForAttributeSet(
			Set<OWLClass> atts, OWLClass c) throws UnknownOWLClassException;

	public SummaryStatistics computeAttributeSetSimilarityStatsForSubgraph(
			Set<OWLClass> atts, OWLClass n);

	public SummaryStatistics computeAttributeSetSimilarityStats(Set<OWLClass> goodAtts);

}


