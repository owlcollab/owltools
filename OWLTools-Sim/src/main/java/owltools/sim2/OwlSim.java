package owltools.sim2;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.sim.io.SimResultRenderer.AttributesSimScores;
import owltools.sim2.OwlSim.ScoreAttributeSetPair;
import owltools.sim2.SimpleOwlSim.Direction;
import owltools.sim2.SimpleOwlSim.Metric;

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
public interface OwlSim {

	public OWLOntology getSourceOntology();

	public OWLReasoner getReasoner();

	public Set<Node<OWLClass>> getNamedSubsumers(OWLClass a);

	/**
	 * 
	 * @param a
	 * @return nodes for all classes that a instantiates - direct and inferred
	 */
	public Set<Node<OWLClass>> getInferredAttributes(
			OWLNamedIndividual a);

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

	public int getNamedCommonSubsumersCount(OWLClass a,
			OWLClass b) throws UnknownOWLClassException;

	/**
	 * <pre>
	 *   CS(i,j) = { c : c &isin; Type(i), c &isin; Type(j) }
	 * </pre>
	 * 
	 * @param a
	 * @param b
	 * @return
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
	 * @return Lowest Common Subsumers of a and b
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
	 * @return
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
	public double getAsymmerticAttributeJaccardSimilarity(
			OWLClass c, OWLClass d) throws UnknownOWLClassException;

	/**
	 * sums of IC of the intersection attributes/ sum of IC of union attributes.
	 * <img src=
	 * "http://www.pubmedcentral.nih.gov/picrender.fcgi?artid=2238903&blobname=gkm806um8.jpg"
	 * alt="formula for simGIC"/>
	 * 
	 * @param i
	 * @param j
	 * @return similarity. 
	 * @throws UnknownOWLClassException 
	 */
	public double getElementGraphInformationContentSimilarity(
			OWLNamedIndividual i, OWLNamedIndividual j) throws UnknownOWLClassException;

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
	 * @throws UnknownOWLClassException 
	 */
	public ScoreAttributeSetPair getSimilarityMaxIC(
			OWLNamedIndividual i, OWLNamedIndividual j) throws UnknownOWLClassException;

	public ScoreAttributeSetPair getSimilarityBestMatchAverageAsym(
			OWLNamedIndividual i, OWLNamedIndividual j);

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
	 * @return
	 */
	public ScoreAttributeSetPair getSimilarityBestMatchAverage(
			OWLNamedIndividual i, OWLNamedIndividual j, Metric metric,
			Direction dir);

	/**
	 * returns all attribute classes - i.e. the classes used to annotate the
	 * elements (genes, diseases, etc) being studied
	 * 
	 * defaults to all classes in source ontology signature
	 * 
	 * @return set of classes
	 */
	public Set<OWLClass> getAllAttributeClasses();

	/**
	 * assumes that the ontology contains both attributes (TBox) and elements +
	 * associations (ABox)
	 * @throws UnknownOWLClassException 
	 */
	// TODO - make this private & call automatically
	public void createElementAttributeMapFromOntology() throws UnknownOWLClassException;

	/**
	 * Gets all attribute classes used to describe individual element e.
	 * 
	 * Includes inferred classes
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
	 */
	public Set<OWLNamedIndividual> getElementsForAttribute(OWLClass c);

	/**
	 * |{e : e &isin; Inst(c)}|
	 * 
	 * @param c
	 * @return count
	 */
	public int getNumElementsForAttribute(OWLClass c);

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
	 * IC = -(log {@link #getNumElementsForAttribute(c)} / corpus size) / log(2)
	 * @param c
	 * @return Information Content value for a given class
	 */
	public Double getInformationContentForAttribute(OWLClass c);

	/**
	 * @return -Σ<sub>i</sub>[ (P(x<sub>i</sub>)log<sub>2</sub>P(x<sub>i</sub>)]
	 */
	public Double getEntropy();

	/**
	 * @param cset
	 * @return -Σ<sub>i</sub>[ (P(x<sub>i</sub>)log<sub>2</sub>P(x<sub>i</sub>)] for x<sub>i</sub> &isin; cset
	 */
	public Double getEntropy(Set<OWLClass> cset);

	public List<AttributesSimScores> compareAllAttributes(OWLClass c, Set<OWLClass> ds) throws UnknownOWLClassException;

	public ScoreAttributeSetPair getLowestCommonSubsumerWithIC(OWLClass c, OWLClass d)
			throws UnknownOWLClassException;

	/**
	 * A pair consisting of a set of equal-scoring attributes, and a score
	 * 
	 * @author cjm
	 * 
	 */
	public class ScoreAttributeSetPair implements Comparable<ScoreAttributeSetPair> {
		public double score;

		public Set<OWLClass> attributeClassSet = new HashSet<OWLClass>(); // all
		// attributes
		// with
		// this
		// score

		public ScoreAttributeSetPair(double score, OWLClass ac) {
			super();
			this.score = score;
			if (ac != null) attributeClassSet.add(ac);
		}

		public ScoreAttributeSetPair(double score, Set<OWLClass> acs) {
			super();
			this.score = score;
			this.attributeClassSet = acs;
		}

		public ScoreAttributeSetPair(double score) {
			super();
			this.score = score;
		}

		public void addAttributeClass(OWLClass ac) {
			if (attributeClassSet == null)
				attributeClassSet = new HashSet<OWLClass>();
			this.attributeClassSet.add(ac);
		}

		public void setAttributeClassSet(Set<OWLClass> acs) {
			attributeClassSet = new HashSet<OWLClass>();
			for (OWLClass ac : acs)
				attributeClassSet.add(ac);
		}

		@Override
		public int compareTo(ScoreAttributeSetPair p2) {
			// TODO Auto-generated method stub
			return 0 - Double.compare(score, p2.score);
		}

	}
	
	public class AttributePairScores {
		OWLClass c;
		OWLClass d;
		
		public Double lcIC = null;
		public Set<OWLClass> lcsSet = null;
		
		public Double simjScore = null;
		public Double asymmetricSimjScore = null;
		public Double inverseAsymmetricSimjScore = null;
		
		public Double simGIC = null;		
	}
	
	public class ElementPairScores {
		OWLNamedIndividual i;
		OWLNamedIndividual j;
		
		public int numberOfAttributesI;
		public int numberOfAttributesJ;
		
		public Double maxIC = null;
		public Set<OWLClass> maxICwitness = null;
		
		public Double simjScore = null;
		public Double asymmetricSimjScore = null;
		public Double inverseAsymmetricSimjScore = null;
		
		
		public ScoreAttributeSetPair bmaSymIC = null;
		public ScoreAttributeSetPair bmaAsymIC = null;
		public ScoreAttributeSetPair bmaInverseAsymIC = null;
		
		//public ScoreAttributeSetPair bmaAsymJ = null;
		
		//public ScoreAttributeSetPair bmaSymJ = null;
		
		public Double simGIC = null;

		
	}


}