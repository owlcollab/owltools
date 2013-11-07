package owltools.sim;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAnonymousClassExpression;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLQuantifiedObjectRestriction;
import org.semanticweb.owlapi.model.OWLQuantifiedRestriction;

import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLQuantifiedProperty;
import owltools.sim.Similarity;

/**
 * SimEngine provides access to multiple similarity calculation
 * procedures (see {@link Similarity}).
 * 
 * It wraps an OWLGraphWrapper object, and also provides
 * convenience methods for calculating the LCA
 * 
 * @author cjm
 *
 */
public class SimEngine {

	private static Logger LOG = Logger.getLogger(SimEngine.class);


	// -------------------------------------
	// Class Attributes
	// -------------------------------------

	protected OWLGraphWrapper graph;
	public boolean excludeClassesInComparison = false;
	public OWLObject comparisonSuperclass = null;
	public Double minimumIC = null;
	private Map<OWLObject,Double> cacheObjectIC = new HashMap<OWLObject,Double>();
	private Map<OWLObjectPair,Set<OWLObject>> cacheLCS = new HashMap<OWLObjectPair,Set<OWLObject>> ();
	private Set<String> excludedLabels;

	public OWLClass comparisonClass = null;
	public OWLObjectProperty comparisonProperty = null;


	// -------------------------------------
	// Constructions
	// -------------------------------------

	public SimEngine(OWLGraphWrapper wrapper) {
		setGraph(wrapper);
	}

	// -------------------------------------
	// getters/setters
	// -------------------------------------

	public OWLGraphWrapper getGraph() {
		return graph;
	}

	public void setGraph(OWLGraphWrapper graph) {
		this.graph = graph;
	}

	/**
	 * any class whose label matches any of the strings returned
	 * here will be excluded from any analysis.
	 *
	 * the set of excluded labels is controlled by loading an
	 * ontology with an entity PhenoSim_0000001 where all
	 * the literals associated with this are excluded.
	 * (note that this method may change in future)
	 * 
	 * @return excluded labels 
	 */
	public Set<String> getExcludedLabels() {
		if (excludedLabels != null)
			return excludedLabels;
		excludedLabels = new HashSet<String>();
		for (OWLAnnotationAssertionAxiom aa :
			getGraph().getSourceOntology().getAnnotationAssertionAxioms(IRI.create("http://purl.obolibrary.org/obo/PhenoSim_0000001"))) {
			OWLAnnotationValue v = aa.getValue();
			if (v instanceof OWLLiteral) {
				excludedLabels.add(((OWLLiteral)v).getLiteral());
			}
		}
		return excludedLabels;
	}

	/**
	 * A class is excluded from the analysis if:
	 * 
	 * it is a named entity, and the label for that entity matches the exclude labels list
	 * OR: it is a class expression, and the signature contains an excluded class
	 * @param att
	 * @return boolean
	 */
	public boolean isExcludedFromAnalysis(OWLObject att) {
		if (att instanceof OWLAnonymousClassExpression) {
			for (OWLClass cls : ((OWLAnonymousClassExpression)att).getClassesInSignature()) {
				if (isExcludedFromAnalysis(cls))
					return true;
			}
			/*
			for (OWLGraphEdge e : graph.getOutgoingEdges(att)) {
				if (!(e.getTarget() instanceof OWLAnonymousClassExpression)) {
					if (isExcludedFromAnalysis(e.getTarget()))
						return true;
				}
			}
			 */
			return false;
		}
		String label = getGraph().getLabelOrDisplayId(att);
		if (label != null && getExcludedLabels().contains(label)) {
			return true;
		}
		return false;
	}



	// -------------------------------------
	// Utils
	// -------------------------------------

	/**
	 * factory class for generating a Similarity instance based on its name
	 * 
	 * @param name 
	 * @return {@link Similarity} 
	 * @throws SimilarityAlgorithmException 
	 */
	public Similarity getSimilarityAlgorithm(String name) throws SimilarityAlgorithmException {
		Class c = null;
		try {
			c = Class.forName(name);
		} catch (ClassNotFoundException e) {
			if (name.contains(".")) {
				e.printStackTrace();
				throw new SimilarityAlgorithmException(name);
			}
			//return getSimilarityAlgorithm("owltools.sim.SimEngine$"+name);
			return getSimilarityAlgorithm("owltools.sim."+name);
		}
		return getSimilarityAlgorithm(c);
	}

	public Similarity getSimilarityAlgorithm(Class c) throws SimilarityAlgorithmException {
		try {
			// http://stackoverflow.com/questions/728842/instantiating-an-inner-class
			//Constructor<Similarity> ctor = c.getConstructor(SimEngine.class);
			//return (Similarity) ctor.newInstance(this);
			return (Similarity) c.newInstance();
		} catch (Exception e) {
			e.printStackTrace();
			throw new SimilarityAlgorithmException("cannot make new instance of "+c);
		}
	}

	public Class[] getAllSimilarityAlgorithmClasses() {
		Class[] ms = {
				JaccardSimilarity.class,
				OverlapSimilarity.class,
				DescriptionTreeSimilarity.class,
				MaximumInformationContentSimilarity.class,
				AvgInformationContentBestMatchesSimilarity.class
		};
		return ms;
	}


	// todo - place this in a separate class
	public Set<OWLObject> getAttributeClosureFor(OWLObject x) {
		OWLGraphWrapper g = getGraph();
		Set<OWLObject> ancs = new HashSet<OWLObject>();
		for (OWLObject a : getAttributesFor(x)) {
			ancs.addAll(g.getAncestorsReflexive(a));
		}
		return ancs;
	}

	// todo - place this in a separate class
	public Set<OWLObject> getAttributesFor(OWLObject x) {
		Set<OWLObject> ancs = getAttributesForWithRedundant(x);
		makeNonRedundant(ancs);
		return ancs;
	}

	// does not filter redundant
	private Set<OWLObject> getAttributesForWithRedundant(OWLObject x) {
		OWLGraphWrapper g = getGraph();
		Set<OWLObject> ancs = new HashSet<OWLObject>();
		if (comparisonClass != null) {
			for (OWLGraphEdge e : g.getOutgoingEdgesClosure(x)) {
				OWLObject t = e.getTarget();
				if (g.getSubsumersFromClosure(t).contains(comparisonClass)) {
					ancs.add(t);
				}
			}
		}
		if (comparisonProperty != null) {

			// may either be directly linked, or may be an instance of something directly linked
			for (OWLGraphEdge e : g.getPrimitiveOutgoingEdges(x)) {
				OWLObject t = e.getTarget();
				OWLQuantifiedProperty qp = e.getSingleQuantifiedProperty();
				if (comparisonProperty.equals(qp.getProperty())) {
					ancs.add(t);
				}
				else if (qp.isInstanceOf() || qp.isSubClassOf()) {
					for (OWLGraphEdge e2 : g.getPrimitiveOutgoingEdges(t)) {
						OWLObject t2 = e2.getTarget();
						OWLQuantifiedProperty qp2 = e2.getSingleQuantifiedProperty();
						if (comparisonProperty.equals(qp2.getProperty())) {
							ancs.add(t2);
						}
					}

				}
			}


		}
		if (comparisonProperty == null && comparisonClass == null) {
			ancs.addAll(g.getSubsumersFromClosure(x));
			// this one probably better:
			/*

			for (OWLGraphEdge e : g.getPrimitiveOutgoingEdges(x)) {
				OWLObject t = e.getTarget();
				ancs.add(t);
			}
			 */
		}
		return ancs;
	}


	// -------------------------------------
	// Statistics
	// -------------------------------------

	private Integer corpusSize = null;
	public int getCorpusSize() {
		if (corpusSize != null)
			return corpusSize;
		// TODO - option for individuals; for now this is hardcoded
		int n = 0;
		LOG.info("calculating corpus size:");
		for (OWLObject x : graph.getAllOWLObjects()) {
			if (x instanceof OWLIndividual) {
				// exclude individuals that have no attributes from corpus.
				// note: comparisonProperty should be set
				int numAtts = getAttributesForWithRedundant(x).size();
				if (numAtts > 0) {
					//LOG.info("  num atts["+x+"] = "+numAtts);
					n++;
				}
			}
		}
		LOG.info("corpus size:"+n);
		corpusSize = n;
		return n;
	}

	public int getFrequency(OWLObject obj) {
		// TODO - option for individuals; for now this is hardcoded
		int n = 0;
		for (OWLObject x : graph.getDescendants(obj)) {
			if (x instanceof OWLIndividual) {
				n++;
			}
		}
		LOG.info("descendants of "+obj+" #= "+n);

		return n;
		//return graph.getDescendants(obj).size();	
	}

	/**
	 * as getFrequency(obj), treats objs as a conjunction
	 * @param objs
	 * @return frequency
	 */
	public int getFrequency(Set<OWLObject> objs) {
		Set<OWLObject> results = null;
		for (OWLObject q : objs) {
			if (results == null)
				results = graph.getIndividualDescendants(q);
			else {
				results.retainAll(graph.getIndividualDescendants(q));
			}
			LOG.debug("Q:"+q+" SIZE:"+results.size());
		}
		if (results == null)
			return 0;
		return results.size();
	}


	/**
	 * The IC of an OWLObject is
	 * 
	 * freq(Obj)/corpusSize
	 * 
	 * here the frequency of an object is the number of individuals with a graph path up to the object,
	 * and the corpus size is the number of individuals in the graph
	 * 
	 * @param obj
	 * @return IC
	 */
	public Double getInformationContent(OWLObject obj) {
		// caching is always on by default
		if (cacheObjectIC.containsKey(obj)) {
			return cacheObjectIC.get(obj);
		}
		Double ic = null;
		int freq = getFrequency(obj);
		if (freq > 0) {
			LOG.info("freq of "+obj+" is: "+freq);
			ic = -Math.log(((double) (freq) / getCorpusSize())) / Math.log(2);
		}
		cacheObjectIC.put(obj, ic);
		return ic;
	}
	public Double getInformationContent(Set<OWLObject> objs) {
		Double ic = null;
		int freq = getFrequency(objs);
		if (freq > 0) {
			ic = -Math.log(((double) freq / getCorpusSize())) / Math.log(2);
		}
		return ic;
	}

	public boolean hasInformationContent(OWLObject obj) {
		return getInformationContent(obj) != null;
	}

	Set<OWLObject> nonSignificantObjectSet = null;
	public Set<OWLObject> nonSignificantObjects() {
		if (minimumIC == null) {
			return new HashSet<OWLObject>();
		}
		if (nonSignificantObjectSet != null ) {
			return nonSignificantObjectSet;
		}
		nonSignificantObjectSet = new HashSet<OWLObject>();
		for (OWLObject obj : graph.getAllOWLObjects()) {
			if (this.hasInformationContent(obj) &&
					this.getInformationContent(obj) < minimumIC) {
				nonSignificantObjectSet.add(obj);
			}
		}
		return nonSignificantObjectSet;
	}

	public void filterObjects(Set<OWLObject> objs) {
		objs.removeAll(nonSignificantObjects());
		Set<OWLObject> rmSet = new HashSet<OWLObject>();
		for (OWLObject obj : objs) {
			if (this.isExcludedFromAnalysis(obj)) {
				rmSet.add(obj);

			}
		}
		objs.removeAll(rmSet);
	}

	public Set<OWLObject> getUnionSubsumers(OWLObject a, OWLObject b) {
		Set<OWLObject> s1 = getGraph().getAncestorsReflexive(a);
		s1.addAll(getGraph().getAncestorsReflexive(b));
		filterObjects(s1);
		return s1;
	}
	public int getUnionSubsumersSize(OWLObject a, OWLObject b) {
		return getUnionSubsumers(a,b).size();		
	}

	/**
	 * todo rename - actually gets common named ancestors
	 * @param a
	 * @param b
	 * @return set of ancestors
	 */
	public Set<OWLObject> getCommonSubsumers(OWLObject a, OWLObject b) {
		Set<OWLObject> s1 = getGraph().getAncestorsReflexive(a);
		s1.retainAll(getGraph().getAncestorsReflexive(b));
		filterObjects(s1);
		return s1;
	}
	public int getCommonSubsumersSize(OWLObject a, OWLObject b) {
		return getCommonSubsumers(a,b).size();		
	}

	/**
	 * note that this performs caching by default
	 * @param a
	 * @param b
	 * @return LCA of a and b
	 */
	public Set<OWLObject> getLeastCommonSubsumers(OWLObject a, OWLObject b) {
		OWLObjectPair pair = new OWLObjectPair(a,b,true);
		if (!cacheLCS.containsKey(pair)) {
			Set<OWLObject> objs = getCommonSubsumers(a,b);	
			cacheLCS.put(pair, makeNonRedundant(objs));
		}
		return cacheLCS.get(pair);
	}
	public int getLeastCommonSubsumersSize(OWLObject a, OWLObject b) {
		return getLeastCommonSubsumers(a,b).size();		
	}

	/**
	 * An object x is redundant with a set S if:
	 * 
	 * there exists an element y of S, such that
	 * y<x
	 * 
	 * @param objs
	 * @return non-redundant set of objects
	 */
	public Set<OWLObject> makeNonRedundant(Set<OWLObject> objs) {
		return makeNonRedundant(objs, true);
	}

	public Set<OWLObject> makeNonRedundant(Set<OWLObject> objs, boolean prioritizeNamedClasses) {
		// redundant set
		Set<OWLObject> rs = new HashSet<OWLObject>();

		// check each object to see if it's redundant
		for (OWLObject obj : objs) {
			Set<OWLObject> ancs = getAncestors(obj);
			ancs.retainAll(objs);
			ancs.remove(obj);

			for (OWLObject anc : ancs) {
				// we know that obj<anc,
				// anc is therefore redundant
				// (if it appears in the original set)

				// the exception is for cycles
				if (getAncestors(anc).contains(obj)) {
					// we have a cycle: anc subsumed by obj via the directly preceding test;
					//                  obj subsumed by anc via the getAncestors call

					// for cycles, keep both unless we want to prioritize named classes
					if (prioritizeNamedClasses) {
						if (anc instanceof OWLAnonymousClassExpression && obj instanceof OWLNamedObject) {
							rs.add(anc);
						}
					}
				}
				else {
					// not a cycle - schedule redundant class for removal
					rs.add(anc);
				}
			}
		}
		objs.removeAll(rs);
		return objs;
	}

	/*
	public Set<OWLObject> makeNonRedundantOrEquivalent(Set<OWLObject> objs) {
		// redundant set
		Set<OWLObject> rs = new HashSet<OWLObject>();
		Set<OWLObject> unions = new HashSet<OWLObject>();
		Map<OWLObject,Set<OWLObject>> amap = new HashMap<OWLObject,Set<OWLObject>>();
		for (OWLObject obj : objs) {
			amap.put(obj, getAncestors(obj));
			amap.get(obj).retainAll(objs);
			amap.get(obj).remove(obj);
		}
		for (OWLObject x : objs) {
			for (OWLObject y : objs) {
				if (x.equals(y))
					continue;
				if (amap.get(y).contains(x)) {
					rs.add(x);
					if (amap.get(x).contains(y)) {
						// reciprocal == equiv
						rs.add(y);

					}
					break;
				}
			}
		}

		objs.removeAll(rs);
		return objs;
	}
	 */

	private Set<OWLObject> getAncestors(OWLObject obj) {
		// TODO configurable
		return getGraph().getAncestorsReflexive(obj);
	}

	/**
	 * @param a
	 * @param b
	 * @return ObjectIntersectionOf(a b)
	 */
	public OWLClassExpression makeIntersectionPair(OWLClassExpression a, OWLClassExpression b) {
		OWLClassExpression ce =
			graph.getDataFactory().getOWLObjectIntersectionOf(a,b);
		return ce;		
	}

	/**
	 * @param a
	 * @param b
	 * @return ObjectUnionOf(a b)
	 */
	public OWLClassExpression makeUnionPair(OWLClassExpression a, OWLClassExpression b) {
		OWLClassExpression ce =
			graph.getDataFactory().getOWLObjectUnionOf(a, b);
		return ce;		
	}

	// ----------------------------------------
	// ANONYMOUS LCS
	// ----------------------------------------

	/**
	 * @param a
	 * @param b
	 * @return LCS expression
	 */
	public OWLClassExpression getLeastCommonSubsumerSimpleClassExpression(OWLObject a, OWLObject b) {
		return getLeastCommonSubsumerSimpleClassExpression(a,b,true);
	}

	/**
	 * @param a
	 * @param b
	 * @param isIgnoreEdgeLabels
	 * @return expression
	 */
	public OWLClassExpression getLeastCommonSubsumerSimpleClassExpression(OWLObject a, OWLObject b, boolean isIgnoreEdgeLabels) {

		if (a.equals(b)) {
			LOG.info("a equals b");
			if (a instanceof OWLClassExpression)
				return (OWLClassExpression) a;
			return null;
		}

		Set<OWLObject> ancsA;
		Set<OWLObject> ancsB;

		// first calculate LCAs
		ancsA = graph.getAncestorsReflexive(a);
		ancsB = graph.getAncestorsReflexive(b);

		Set<OWLObject> csl = new HashSet<OWLObject>(ancsA);
		csl.retainAll(ancsB);
		LOG.info("All CAs"+csl);
		filterObjects(csl); // user-filtering
		//makeNonRedundant(csl);
		//LOG.info("NR LCAs"+csl);


		Set<OWLGraphEdge> commonEdges = new HashSet<OWLGraphEdge>();
		for (OWLObject x : csl) {
			LOG.info(" CS:"+x);
			// expand edges
			Set<OWLGraphEdge> edgesA = graph.getCompleteEdgesBetween(a, x);
			Set<OWLGraphEdge> edgesB = graph.getCompleteEdgesBetween(b, x);

			for (OWLGraphEdge ea : edgesA) {
				if (!(ea.getTarget() instanceof OWLClass)) {
					break;
				}
				for (OWLGraphEdge eb : edgesB) {
					if (!(eb.getTarget() instanceof OWLClass)) {
						break;
					}
					LOG.info("  EDGEPAIR:"+ea+" vs "+eb);
					if (ea.getQuantifiedPropertyList().equals(eb.getQuantifiedPropertyList())) {
						commonEdges.add(ea);
					}
					else {
						if ((ea.getSingleQuantifiedProperty().isIdentity() &&
								eb.getSingleQuantifiedProperty().isSubClassOf()) ||
								(eb.getSingleQuantifiedProperty().isIdentity() &&
										ea.getSingleQuantifiedProperty().isSubClassOf())) {
							commonEdges.add(ea);
						}
					}
				}
			}
		}
		LOG.info("Common"+commonEdges);

		/*
		 * Also check for
		 * [R X] and [R.S Y]
		 * where X<S some Y
		 * 
		 * 
		 */

		// TODO: optimization
		Set<OWLClassExpression> ces = new HashSet<OWLClassExpression>();
		for (OWLGraphEdge ce : commonEdges) {
			// Candidate LCS
			OWLClassExpression cex = (OWLClassExpression) graph.edgeToTargetExpression(ce);

			// get LCS from CS:
			// check if there is a different candidate CE that
			//  * is subsumed by cex (i.e. more specific)
			//  * is not a subsumer of cex (i.e. equivalent)
			// the candidate will be in the set of common edges
			boolean isSubsumed = false;
			for (OWLGraphEdge e2 : commonEdges) {
				if (e2.equals(ce))
					continue;

				for (OWLGraphEdge se : graph.getOWLGraphEdgeSubsumers(e2)) {
					OWLObject e2t = e2.getTarget();
					if (ce.getQuantifiedPropertyList().equals(se.getQuantifiedPropertyList())) {

						Set<OWLObject> subsumers = graph.getSubsumersFromClosure(e2t);
						subsumers.add(e2t);
						if (subsumers.contains(ce.getTarget())) {
							isSubsumed = true;
							LOG.debug("isSubsumed: "+ce.getTarget());
							// check for reciprocal/equivalence (only need direct edges)
							for (OWLGraphEdge re: graph.getOutgoingEdges(e2t)) {
								if (re.getTarget().equals(e2t)) {
									LOG.debug("reciprocal: "+ce.getTarget());
									isSubsumed = false;
								}	
							}

							if (isSubsumed) {
								break;								
							}
						}
					}

					for (OWLGraphEdge xe : graph.getCompleteOutgoingEdgesClosure(se.getTarget())) {
						OWLGraphEdge ne = graph.combineEdgePair(null, se, xe, 1);
						if (ne == null)
							continue;
						if (graph.edgeToTargetExpression(ne).equals(cex)) {
							// the candidate expression includes a superfluous chain at the end
							LOG.debug("isSubsumed2: "+cex);
							isSubsumed = true;
							break;
						}
					}

				}
			}

			if (!isSubsumed) {
				ces.add(cex);
			}
		}
		if (ces.size() == 0) {
			return null; // TODO - owl:Thing
		}
		else if (ces.size() == 1) {
			return ces.iterator().next();
		}
		else {
			//LOG.info("Making ce: "+ces);
			OWLClassExpression lcs =
				graph.getDataFactory().getOWLObjectIntersectionOf(ces);

			return lcs;
		}
	}

	public Set<OWLGraphEdge> extendEdge(OWLGraphEdge e) {
		Set<OWLGraphEdge> edges = new HashSet<OWLGraphEdge>();
		return edges;
	}

	/*
	public OWLClassExpression old___getLeastCommonSubsumerSimpleClassExpression(OWLObject a, OWLObject b, boolean isIgnoreEdgeLabels) {

		if (a.equals(b)) {
			LOG.info("a equals b");
			if (a instanceof OWLClassExpression)
				return (OWLClassExpression) a;
			return null;
		}

		Set<OWLObject> ancsA;
		Set<OWLObject> ancsB;

		if (isIgnoreEdgeLabels) {
			// note that we calculate the edge list later
			ancsA = graph.getAncestorsReflexive(a);
			ancsB = graph.getAncestorsReflexive(b);
		}
		else {
			ancsA = graph.getSubsumersFromClosure(a);
			ancsB = graph.getSubsumersFromClosure(b);
		}

		boolean isASubsumesB = ancsB.contains(a);		
		boolean isBSubsumesA = ancsA.contains(b);

		if (isASubsumesB && isBSubsumesA) {
			LOG.info("A and B mutually subsume");
			return makeIntersectionPair((OWLClassExpression)a,(OWLClassExpression)b);
		}

		// TODO: for these two cases make a union class
		if (isASubsumesB) {
			LOG.info("A subsumes B");
			return (OWLClassExpression) a;
		}
		if (isBSubsumesA) {
			LOG.info("B subsumes A");
			return (OWLClassExpression) b;
		}

		Set<OWLObject> csl = new HashSet<OWLObject>(ancsA);
		csl.retainAll(ancsB);
		LOG.info("All LCAs"+csl);
		filterObjects(csl);
		makeNonRedundant(csl);
		LOG.info("NR LCAs"+csl);


		Set<OWLClassExpression> ces = new HashSet<OWLClassExpression>();
		for (OWLObject x : csl) {
			Set<OWLGraphEdge> edgesA = graph.getCompleteEdgesBetween(a, x);
			Set<OWLGraphEdge> edgesB = graph.getCompleteEdgesBetween(b, x);
			OWLGraphEdge commonEdge = null;
			for (OWLGraphEdge ea : edgesA) {
				for (OWLGraphEdge eb : edgesB) {
					if (ea.getQuantifiedPropertyList().equals(eb.getQuantifiedPropertyList())) {
						if (commonEdge == null) {
							commonEdge = ea;
						}
						else {
							boolean isSubsumed = false;
							for (OWLGraphEdge ec : graph.getOWLGraphEdgeSubsumers(commonEdge)) {
								if (ec.getQuantifiedPropertyList().equals(ea.getQuantifiedPropertyList())) {
									isSubsumed = true;
									break;
								}
							}
							if (!isSubsumed)
								commonEdge = ea;
						}
					}
				}
			}
			if (commonEdge != null) {
				OWLClassExpression ce = (OWLClassExpression) graph.edgeToTargetExpression(commonEdge);
				ces.add(ce);
			}
			// TODO - use property hierarchy here;
			// e.g. if path1 = inheres_in_part_of and path2 = inheres_in, use the more general one
		}
		if (ces.size() == 0) {
			return null; // TODO - owl:Thing
		}
		else if (ces.size() == 1) {
			return ces.iterator().next();
		}
		else {
			OWLClassExpression lcs =
				graph.getDataFactory().getOWLObjectIntersectionOf(ces);

			return lcs;
		}
	}
	 */

	/**
	 * makes a reduced union expression.
	 * 
	 * Uses the following two reduction rules:
	 * 
	 * (r1 some X) U (r2 some Y) ==> lcs(r1,r2) some MakeUnionOf(X,Y)
	 * (r1 some X) U X ==> reflexive-version-of-r1 some X
	 * 
	 * TODO: test for (r some r some X) u (r some X) cases. needs to be done over final expression.
	 *  
	 * if a reduced form cannot be made, returns null
	 * 
	 * @param xa
	 * @param xb
	 * @return
	 */
	// TODO - DRY with DTS
	/*
	private OWLClassExpression makeUnionUsingReflexiveProperty(OWLClassExpression xa, OWLClassExpression xb, boolean forceReflexivePropertyCreation) {
		LOG.info("testing if there is a more compact union expression for "+xa+" ++ "+xb);
		OWLDataFactory df = graph.getDataFactory();
		if (xa instanceof OWLQuantifiedRestriction) {
			// TODO - check before casting
			OWLObjectProperty prop = (OWLObjectProperty) ((OWLQuantifiedRestriction) xa).getProperty();
			OWLClassExpression xaRest = (OWLClassExpression) ((OWLQuantifiedRestriction)xa).getFiller();
			if (xb instanceof OWLQuantifiedRestriction) {
				OWLObjectPropertyExpression p2 =
					propertySubsumer(prop, 
						((OWLQuantifiedObjectRestriction) xb).getProperty());

				if (p2 != null) {
					OWLClassExpression xbRest = (OWLClassExpression) ((OWLQuantifiedRestriction)xb).getFiller();
					OWLClassExpression x = makeUnionWithReduction(xaRest,xbRest);
					// todo - mixing some and all
					if (xa instanceof OWLObjectSomeValuesFrom)
						return df.getOWLObjectSomeValuesFrom(p2,x);
					else if (xa instanceof OWLObjectAllValuesFrom)
						return df.getOWLObjectAllValuesFrom(p2, x);
				}
			}
			LOG.info("  test: "+xaRest+" == "+xb);


			if (xaRest.equals(xb)) {
				LOG.info("     TRUE: "+xaRest+" == "+xb);

				OWLObjectProperty rprop = null;
				if (graph.getIsReflexive(prop)) {
					rprop = prop;
				}
				if (forceReflexivePropertyCreation) {
					OWLOntologyManager manager = graph.getManager();
					OWLOntology ont = graph.getSourceOntology();
					rprop = 
						df.getOWLObjectProperty(IRI.create(prop.getIRI().toString()+"_reflexive"));
					manager.applyChange(new AddAxiom(ont, df.getOWLSubObjectPropertyOfAxiom(prop, rprop)));
					manager.applyChange(new AddAxiom(ont, df.getOWLTransitiveObjectPropertyAxiom(rprop)));
					LOG.info("  reflexive prop:"+rprop);

				}
				if (rprop != null) {
					if (xa instanceof OWLObjectSomeValuesFrom)
						return df.getOWLObjectSomeValuesFrom(rprop,xb);
					else if (xa instanceof OWLObjectAllValuesFrom)
						return df.getOWLObjectAllValuesFrom(rprop, xb);
				}

			}
		}
		return null;
	}
	 */

	/**
	 * @param m
	 * @param a
	 * @param b
	 * @return score
	 * @throws SimilarityAlgorithmException
	 */
	public Double calculateSimilarityScore(Similarity m, OWLObject a, OWLObject b) throws SimilarityAlgorithmException {
		Similarity r = calculateSimilarity(m,a,b);
		return r.score;
	}

	/**
	 * Calculates the similarity between two OWLObjects
	 * 
	 * this is the core method of the SimEngine
	 * 
	 * @param m
	 * @param a
	 * @param b
	 * @return similarity
	 * @throws SimilarityAlgorithmException
	 */
	public Similarity calculateSimilarity(Similarity m, OWLObject a, OWLObject b) throws SimilarityAlgorithmException {
		// TODO - consider making these immutable and forcing a constructor
		m.a = a;
		m.b = b;
		m.calculate(this,a,b);
		return m;
	}

	// consider moving to class that uses it..
	void getBestMatch(Map <OWLObject,Similarity> bestMap, OWLObject x, Similarity r) {
		if (bestMap.containsKey(x)) {
			Similarity prev = bestMap.get(x);
			if (r.score > prev.score ) {
				bestMap.put(x, r);
			}
		}
		else {
			bestMap.put(x, r);
		}

	}

	/**
	 * iterates through ALL similarity metrics and calculates
	 * @param a
	 * @param b
	 * @return set of all similarities
	 * @throws SimilarityAlgorithmException
	 */
	public Set<Similarity> calculateAllSimilarity(OWLObject a, OWLObject b) throws SimilarityAlgorithmException  {
		Set<Similarity> mm = 
			new HashSet<Similarity>();
		for (Class mc : getAllSimilarityAlgorithmClasses()) {
			Similarity m = null;
			try {
				m = getSimilarityAlgorithm(mc);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (m != null) {
				calculateSimilarity(m,a,b);
				m.simEngine = this;
				mm.add(m);
			}
		}
		return mm;
	}


	// TODO
	public void calculateSimilarityAllByAll(String similarityAlgorithmName, Double minScore) throws SimilarityAlgorithmException {
		Set<OWLObject> objs = graph.getAllOWLObjects();
		if (comparisonSuperclass != null) {
			System.out.println("finding descendants of :"+comparisonSuperclass);
			objs = graph.getDescendants(comparisonSuperclass);
			System.out.println("  descendants  :"+objs.size());
		}
		for (OWLObject a : objs) {
			if (excludeObjectFromComparison(a))
				continue;
			for (OWLObject b : graph.getAllOWLObjects()) {
				if (a.equals(b))
					continue;
				if (excludeObjectFromComparison(b))
					continue;
				System.out.println("COMPARE:"+label(a)+" -vs- "+label(b));
				Similarity s = this.getSimilarityAlgorithm(similarityAlgorithmName);
				calculateSimilarity(s,a,b);
				Double sc = s.score;
				if (minScore == null || sc > minScore) {
					System.out.println(a+" "+b+" = "+sc);
					s.print();
				}
			}
		}
	}

	public String label(OWLObject x) {
		String label = graph.getLabel(x);
		if (label == null)
			return x.toString();
		return x.toString()+" \""+label+"\"";
	}

	private boolean excludeObjectFromComparison(OWLObject a) {
		System.out.println("TESTING:"+a);
		//boolean exclude = false;
		if (excludeClassesInComparison && !(a instanceof OWLNamedIndividual)) {
			return true;
		}
		if (this.comparisonSuperclass != null) {
			if (!graph.getSubsumersFromClosure(a).contains(comparisonSuperclass)) {
				return true;
			}
		}
		return false;
	}

	public OWLObject createUnionExpression(OWLObject a, OWLObject b, OWLObject c) {
		Set<OWLGraphEdge> edgesA = graph.getEdgesBetween(a, c);
		Set<OWLGraphEdge> edgesB = graph.getEdgesBetween(b, c);
		if (edgesA.equals(edgesB)) {
			return edgeSetToExpression(edgesA);
		}
		else {
			OWLClassExpression xa = edgeSetToExpression(edgesA);
			OWLClassExpression xb = edgeSetToExpression(edgesA);
			HashSet<OWLClassExpression> xl = new HashSet<OWLClassExpression>();
			xl.add(xa);
			xl.add(xb);
			if (xl.size() == 1)
				return xl.iterator().next();
			OWLObjectUnionOf xu = graph.getDataFactory().getOWLObjectUnionOf(xl);
			return xu;
		}
	}

	public OWLClassExpression edgeSetToExpression(Set<OWLGraphEdge> edges) {
		Set<OWLClassExpression> xs = new HashSet<OWLClassExpression>();
		for (OWLGraphEdge e : edges) {
			OWLObject x = graph.edgeToTargetExpression(e);
			xs.add((OWLClassExpression)x);
		}
		if (xs.size() == 1)
			return xs.iterator().next();
		OWLObjectIntersectionOf ix = graph.getDataFactory().getOWLObjectIntersectionOf(xs);
		return ix;
	}

	// -------------------------------------
	// Exception Classes
	// -------------------------------------

	public class SimilarityAlgorithmException extends Exception {

		public SimilarityAlgorithmException(String m) {
			super(m);
		}

	}

}
