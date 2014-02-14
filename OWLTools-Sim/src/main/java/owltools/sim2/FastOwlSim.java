package owltools.sim2;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.log4j.Logger;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.impl.OWLClassNode;

import owltools.sim2.SimpleOwlSim.Direction;
import owltools.sim2.SimpleOwlSim.Metric;
import owltools.sim2.SimpleOwlSim.SimConfigurationProperty;
import owltools.sim2.io.SimResultRenderer.AttributesSimScores;
import owltools.sim2.scores.AttributePairScores;
import owltools.sim2.scores.ElementPairScores;
import owltools.sim2.scores.ScoreMatrix;

import com.googlecode.javaewah.EWAHCompressedBitmap;
import com.googlecode.javaewah.IntIterator;

/**
 * Faster implementation of OwlSim
 * 
 * Makes use of integers to index classes, and bitmaps to represent class sets.
 * 
 * @author cjm
 *
 */
public class FastOwlSim extends AbstractOwlSim implements OwlSim {

	private Logger LOG = Logger.getLogger(FastOwlSim.class);

	public OWLClass debugClass;
	int debugClassMessages = 0;
	int debugClassMessages2 = 0;
	
	private Map<OWLNamedIndividual, Set<OWLClass>> elementToDirectAttributesMap;
	
	// -----------
	// CACHES
	// -----------
	
	// all direct and inferred classes for al individual
	// todo: resolve redundancy with inferredTypesMap
	private Map<OWLNamedIndividual, Set<Node<OWLClass>>> elementToInferredAttributesMap;

	 // caches RSub(c) (reflexive subsumers of a class) - NodeSet
	private Map<OWLClass,Set<Node<OWLClass>>> superclassMap;
	
	// cache of Type(i)->Cs
	private Map<OWLNamedIndividual,Set<Node<OWLClass>>> inferredTypesMap; 

	// cache of RSub(c)->Ints
	private Map<OWLClass,Set<Integer>> superclassIntMap; 
	private Map<OWLNamedIndividual,Set<Integer>> inferredTypesIntMap; // cache of RSub(c)->Ints

	// -------------
	// BITMAP CACHES
	// -------------
	// for efficiency, we store sets of classes as bitmaps.
	// each class is assigned an integer value, resulting
	// it bitmaps of with |c| bits
	
	// given a class index, return superclasses, as a bitmap
	// e.g. if class C has index 7, then superclassBitmapIndex[7] returns a bitmap,
	// in which every "1" value is the index of a superclass of C
	private EWAHCompressedBitmap[] superclassBitmapIndex;
	
	// given a class object, return superclasses, as a bitmap. Fast cache for RSub(c)
	private Map<OWLClass,EWAHCompressedBitmap> superclassBitmapMap; 
	
	// given an individual, return superclasses as a bit map
	private Map<OWLNamedIndividual, EWAHCompressedBitmap> inferredTypesBitmapMap; // cache of Type(i)->BM

	// given a class object, return proper superclasses, as a bitmap. Fast cache for Sub(c)
	Map<OWLClass,EWAHCompressedBitmap> properSuperclassBitmapMap; // cache of Sub(c)->BM

	// -------------
	// CLASS INDICES
	// -------------
	// Each class is assigned a numeric index.
	// We can collapse sets of equivalent classes into a node, which
	// has an arbitrarily assigned representative element.
	//
	// note that the following are for *all* classes in the ontology.
	// E.g. may include anatomy classes in a phenotype analysis.
	// to limit memory usage, first filter ontology to classes with members
	// (being sure to retain inferred axioms).
	// in practice this may not be necessary, as 2D arrays are only used
	// for classes with members.
	
	// maps a set of equivalent classes to a representative
	Map<Node<OWLClass>, OWLClass> representativeClassMap;
	
	// maps a class to a representative for that class (typically itself)
	Map<OWLClass, OWLClass> classTorepresentativeClassMap;

	// maps a class to a unique integer
	Map<OWLClass,Integer> classIndex;
	
	// maps a class index to a class
	OWLClass[] classArray;

	// all types used directly.
	// same as elementToDirectAttributesMap.values()
	private Set<OWLClass> allTypesDirect = null; 

	// all Types used in Type(e) for all e in E.
	// note this excludes classes with no (inferred) members
	// same as elementToInferredAttributesMap.values()
	private Set<OWLClass> allTypesInferred = null; 

	// cache of information content, by class
	private Map<OWLClass, Double> icCache = new HashMap<OWLClass,Double>();
	
	// cache of information content, by class index
	Double[] icClassArray = null;

	//	private Map<ClassIntPair, Set<Integer>> classPairLCSMap;
	//	private Map<ClassIntPair, ScoreAttributeSetPair> classPairICLCSMap;

	// used for storing IC values as integers
	final int scaleFactor = 1000;
	//short[][] ciPairScaledScore;
	ScoreAttributeSetPair[][] testCache = null;
	boolean[][] ciPairIsCached = null;
	int[][] ciPairLCS = null;


	@Override
	public void dispose() {
		showTimings();
	}




	// represents a pair of classes using their indices
	// NOTE; replaced by arrays
	@Deprecated
	private class ClassIntPair {
		int c;
		int d;
		public ClassIntPair(int c, int d) {
			super();
			this.c = c;
			this.d = d;
		}

		@Override
		public int hashCode() {
			final int prime = 991;
			int result = 1;
			result = prime * result + c;
			result = prime * result + d;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ClassIntPair other = (ClassIntPair) obj;
			return c == other.c && d == other.d;
		}
	}



	/**
	 * @param sourceOntology
	 */
	public FastOwlSim(OWLOntology sourceOntology) {
		reasoner = new ElkReasonerFactory().createReasoner(sourceOntology);
	}

	/**
	 * @param reasoner
	 */
	public FastOwlSim(OWLReasoner reasoner) {
		this.reasoner = reasoner;
	}

	@Override
	public Set<OWLClass> getAllAttributeClasses() {
		return allTypesInferred; // note - only attributes used directly or indirectly
	}

	private int getClassIndex(OWLClass c) throws UnknownOWLClassException {
		Integer ix = classIndex.get(c);
		if (ix == null) {
			throw new UnknownOWLClassException(c);
		}
		return ix;
	}
	private int getClassIndex(Node<OWLClass> n) throws UnknownOWLClassException {
		OWLClass c = n.getRepresentativeElement();
		return getClassIndex(c);
	}

	// not yet implemented: guaranteed to yield and indexed class
	private OWLClass getIndexedClass(Node<OWLClass> n) throws UnknownOWLClassException {
		if (representativeClassMap == null)
			representativeClassMap = new HashMap<Node<OWLClass>, OWLClass>();
		else if (representativeClassMap.containsKey(n))
			return representativeClassMap.get(n);
		for (OWLClass c : n.getEntities()) {
			if (classIndex.containsKey(c)) {
				representativeClassMap.put(n,c);
				return c;				
			}
		}
		throw new UnknownOWLClassException(n.getRepresentativeElement());
	}

	/* (non-Javadoc)
	 * @see owltools.sim2.OwlSim#createElementAttributeMapFromOntology()
	 */
	@Override
	public void createElementAttributeMapFromOntology() throws UnknownOWLClassException {
		getReasoner().flush();
		
		// cache E -> Type(E)
		elementToDirectAttributesMap = new HashMap<OWLNamedIndividual,Set<OWLClass>>();
		elementToInferredAttributesMap = new HashMap<OWLNamedIndividual,Set<Node<OWLClass>>>();
		allTypesDirect = new HashSet<OWLClass>();
		allTypesInferred = new HashSet<OWLClass>();
		Set<OWLNamedIndividual> inds = getSourceOntology().getIndividualsInSignature(true);
		for (OWLNamedIndividual e : inds) {

			// The attribute classes for an individual are the direct inferred
			// named types. We assume that grouping classes have already been
			// generated.
			NodeSet<OWLClass> nodesetDirect = getReasoner().getTypes(e, true);
			NodeSet<OWLClass> nodesetInferred = getReasoner().getTypes(e, false);
			allTypesDirect.addAll(nodesetDirect.getFlattened());
			allTypesInferred.addAll(nodesetInferred.getFlattened());
			elementToDirectAttributesMap.put(e, nodesetDirect.getFlattened());
			elementToInferredAttributesMap.put(e, nodesetInferred.getNodes());

			// TODO - remove deprecated classes
			elementToDirectAttributesMap.get(e).remove(owlThing());
			elementToInferredAttributesMap.get(e).remove(owlThing());
		}
		
		LOG.info("|TypesUsedDirectly|="+allTypesDirect.size());
		LOG.info("|TypesUsedInferred|="+allTypesInferred.size());

		Set<OWLClass> cset;
		//cset = getSourceOntology().getClassesInSignature(true);
		cset = allTypesInferred;
		LOG.info("|C|="+cset.size());
		LOG.info("|I|="+inds.size());

		Set<Node<OWLClass>> nodes = new HashSet<Node<OWLClass>>();
		for (OWLClass c : cset) {
			nodes.add(getReasoner().getEquivalentClasses(c));
		}
		LOG.info("|N|="+nodes.size()); // TODO - use thisg

		// classes are collapsed into nodes. Create a map from node to
		// class, and a map of every class
		// to the representative element from its node
		representativeClassMap = new HashMap<Node<OWLClass>, OWLClass>();
		classTorepresentativeClassMap = new HashMap<OWLClass, OWLClass>();
		for (Node<OWLClass> n : nodes) {
			OWLClass c = n.getRepresentativeElement();
			representativeClassMap.put(n, c);
			for (OWLClass c2 : n.getEntities()) {
				classTorepresentativeClassMap.put(c2, c);
			}
		}

		// Create a bidirectional index, class by number
		int n=0;
		classArray = (OWLClass[]) Array.newInstance(OWLClass.class, cset.size()+1);
		classIndex = new HashMap<OWLClass,Integer>();

		// 0th element is always owlThing (root)
		classArray[n] = owlThing();
		classIndex.put(owlThing(), n);
		n++;

		// TODO - investigate if ordering elements makes a difference;
		// e.g. if more frequent classes recieve lower bit indices this
		// may speed certain BitMap operations?
		for (OWLClass c : cset) {
			if (c.equals(owlThing()))
				continue;
			classArray[n] = c;
			classIndex.put(c, n);
			n++;
		}

		// ensure cached
		cset.add(owlThing());
		for (OWLClass c : cset) {
			ancsCachedModifiable(c);
			ancsIntsCachedModifiable(c);
			ancsBitmapCachedModifiable(c);
		}
		for (OWLNamedIndividual e : inds) {
			// force cacheing
			ancsBitmapCachedModifiable(e);
		}



		// cache - this is for ALL classes in signature
		for (OWLClass c : cset) {
			getInformationContentForAttribute(c);
			getInformationContentForAttribute(classIndex.get(c));
		}
		
		this.computeSystemStats();

	}

	// TODO - change set to be (ordered) List, to avoid sorting each time
	private EWAHCompressedBitmap convertIntsToBitmap(Set<Integer> bits) {
		EWAHCompressedBitmap bm = new EWAHCompressedBitmap();
		ArrayList<Integer> bitlist = new ArrayList<Integer>(bits);
		// necessary for EWAH API, otherwise silently fails
		Collections.sort(bitlist);
		for (Integer i : bitlist) {
			bm.set(i.intValue());
		}
		return bm;
	}

	// cached proper superclasses (i.e. excludes equivalent classes) as BitMap
	private EWAHCompressedBitmap ancsProperBitmapCachedModifiable(OWLClass c) {
		if (properSuperclassBitmapMap != null && properSuperclassBitmapMap.containsKey(c)) {
			return properSuperclassBitmapMap.get(c);
		}

		Set<Integer> ancsInts = new HashSet<Integer>();
		for (Node<OWLClass> anc : reasoner.getSuperClasses(c, false)) {
			// TODO - verify robust for non-Rep elements
			OWLClass ac = anc.getRepresentativeElement();
			if (ac.equals(thing))
				continue;
			ancsInts.add(classIndex.get(ac));
		}

		EWAHCompressedBitmap bm = convertIntsToBitmap(ancsInts);
		if (properSuperclassBitmapMap == null)
			properSuperclassBitmapMap = new HashMap<OWLClass,EWAHCompressedBitmap>();
		properSuperclassBitmapMap.put(c, bm);
		return bm;		
	}


	private EWAHCompressedBitmap ancsBitmapCachedModifiable(OWLClass c) throws UnknownOWLClassException {
		if (superclassBitmapMap != null && superclassBitmapMap.containsKey(c)) {
			return superclassBitmapMap.get(c);
		}
		Set<Integer> caints = ancsIntsCachedModifiable(c);
		EWAHCompressedBitmap bm = convertIntsToBitmap(caints);
		if (superclassBitmapMap == null)
			superclassBitmapMap = new HashMap<OWLClass,EWAHCompressedBitmap>();
		superclassBitmapMap.put(c, bm);
		return bm;		
	}

	private EWAHCompressedBitmap ancsBitmapCachedModifiable(int cix) throws UnknownOWLClassException {
		if (superclassBitmapIndex != null && superclassBitmapIndex[cix] != null) {
			return superclassBitmapIndex[cix];
		}
		Set<Integer> caints = ancsIntsCachedModifiable(classArray[cix]);
		EWAHCompressedBitmap bm = convertIntsToBitmap(caints);
		if (superclassBitmapIndex == null)
			superclassBitmapIndex = new EWAHCompressedBitmap[classArray.length];
		superclassBitmapIndex[cix] = bm;
		return bm;		
	}



	private EWAHCompressedBitmap ancsBitmapCachedModifiable(OWLNamedIndividual i) throws UnknownOWLClassException {
		if (inferredTypesBitmapMap != null && inferredTypesBitmapMap.containsKey(i)) {
			return inferredTypesBitmapMap.get(i);
		}
		Set<Integer> caints = ancsIntsCachedModifiable(i);
		EWAHCompressedBitmap bm = convertIntsToBitmap(caints);
		if (inferredTypesBitmapMap == null)
			inferredTypesBitmapMap = new HashMap<OWLNamedIndividual,EWAHCompressedBitmap>();
		inferredTypesBitmapMap.put(i, bm);
		return bm;		
	}

	private EWAHCompressedBitmap ancsBitmapCached(Set<OWLClass> cset) throws UnknownOWLClassException {
		Set<Integer> csetInts = new HashSet<Integer>();

		for (OWLClass c : cset) {
			csetInts.add(classIndex.get(c));
		}
		return convertIntsToBitmap(csetInts);
	}


	private Set<Integer> ancsIntsCachedModifiable(OWLClass c) throws UnknownOWLClassException {
		if (superclassIntMap != null && superclassIntMap.containsKey(c)) {
			return superclassIntMap.get(c);
		}
		Set<Integer> a = ancsInts(c);
		if (superclassIntMap == null)
			superclassIntMap = new HashMap<OWLClass,Set<Integer>>();
		superclassIntMap.put(c, a);
		return a;
	}	

	// TODO - make this an ordered list, for faster bitmaps
	private Set<Integer> ancsIntsCachedModifiable(OWLNamedIndividual i) throws UnknownOWLClassException {
		if (inferredTypesIntMap != null && inferredTypesIntMap.containsKey(i)) {
			return inferredTypesIntMap.get(i);
		}
		Set<Integer> a = ancsInts(i);
		if (inferredTypesIntMap == null)
			inferredTypesIntMap = new HashMap<OWLNamedIndividual,Set<Integer>>();
		inferredTypesIntMap.put(i, a);
		return a;
	}	

	// all ancestors as IntSet
	// note that for equivalence sets, the representative element is returned
	private Set<Integer> ancsInts(OWLClass c) throws UnknownOWLClassException {
		Set<Node<OWLClass>> ancs = ancsCachedModifiable(c);
		Set<Integer> ancsInts = new HashSet<Integer>();
		OWLClass thing = owlThing();
		for (Node<OWLClass> anc : ancs) {
			// TODO - verify robust for non-Rep elements
			OWLClass ac = anc.getRepresentativeElement();
			if (ac.equals(thing))
				continue;
			Integer ix = classIndex.get(ac);
			if (ix == null) {
				throw new UnknownOWLClassException(ac);
			}
			ancsInts.add(ix.intValue());
		}
		return ancsInts;
	}

	private Set<Integer> ancsInts(OWLNamedIndividual i) throws UnknownOWLClassException {
		Set<Node<OWLClass>> ancs = ancsCachedModifiable(i);
		Set<Integer> ancsInts = new HashSet<Integer>();
		OWLClass thing = owlThing();
		for (Node<OWLClass> anc : ancs) {
			// TODO - verify robust for non-Rep elements
			OWLClass ac = anc.getRepresentativeElement();
			if (ac.equals(thing))
				continue;
			Integer ix = classIndex.get(ac);
			if (ix == null) {
				throw new UnknownOWLClassException(ac);
			}
			ancsInts.add(ix.intValue());
		}
		return ancsInts;
	}

	private Set<Node<OWLClass>> ancsCachedModifiable(OWLClass c) {
		if (superclassMap != null && superclassMap.containsKey(c)) {
			return superclassMap.get(c);
		}
		Set<Node<OWLClass>> a = ancs(c);
		if (superclassMap == null)
			superclassMap = new HashMap<OWLClass,Set<Node<OWLClass>>>();
		superclassMap.put(c, a);
		return a;
	}	

	private Set<Node<OWLClass>> ancsCachedModifiable(OWLNamedIndividual i) {
		if (inferredTypesMap != null && inferredTypesMap.containsKey(i)) {
			return inferredTypesMap.get(i);
		}
		Set<Node<OWLClass>> a = ancs(i);
		if (inferredTypesMap == null)
			inferredTypesMap = new HashMap<OWLNamedIndividual,Set<Node<OWLClass>>>();
		inferredTypesMap.put(i, a);
		return a;
	}	

	private Set<Node<OWLClass>> ancs(OWLClass c) {
		NodeSet<OWLClass> ancs = getReasoner().getSuperClasses(c, false);
		Set<Node<OWLClass>> nodes = new HashSet<Node<OWLClass>>(ancs.getNodes());
		nodes.add(getReasoner().getEquivalentClasses(c));
		nodes.remove(owlThingNode());
		return nodes;
	}
	private Set<Node<OWLClass>> ancs(OWLNamedIndividual i) {		
		Set<Node<OWLClass>> nodes = getReasoner().getTypes(i, false).getNodes();
		nodes.remove(owlThingNode());
		return nodes;
	}


	@Override
	public Set<OWLClass> getAttributesForElement(OWLNamedIndividual e) throws UnknownOWLClassException {
		if (elementToDirectAttributesMap == null)
			createElementAttributeMapFromOntology();
		return new HashSet<OWLClass>(elementToDirectAttributesMap.get(e));
	}

	@Override
	public Set<OWLNamedIndividual> getElementsForAttribute(OWLClass c) throws UnknownOWLClassException {
		if (!this.getAllAttributeClasses().contains(c)) {
			throw new UnknownOWLClassException(c);
		}
		return getReasoner().getInstances(c, false).getFlattened();
	}

	@Override
	public int getNumElementsForAttribute(OWLClass c) throws UnknownOWLClassException {
		return this.getElementsForAttribute(c).size();
	}

	@Override
	public Set<OWLNamedIndividual> getAllElements() {
		// Note: will only return elements that have >=1 attributes
		return elementToDirectAttributesMap.keySet();
	}


	@Override
	public Double getInformationContentForAttribute(OWLClass c) throws UnknownOWLClassException {
		if (icCache.containsKey(c)) return icCache.get(c);
		int freq = getNumElementsForAttribute(c);
		Double ic = null ;
		if (freq > 0) {
			ic = -Math.log(((double) (freq) / getCorpusSize())) / Math.log(2);
			// experimental: use depth in graph as tie-breaker.
			// the amount added is |ancs(c)| / SF,
			// where SF is large enough to make overall increase negligible
			int numAncs = ancsBitmapCachedModifiable(c).cardinality();
			double bump = numAncs / (double) scaleFactor;
			if (bump > 0.2) {
				LOG.warn("Bump = "+bump+" for "+c);
			}
			ic += bump;
		}
		icCache.put(c, ic);
		return ic;
	}

	// gets IC by class index, cacheing if required
	Double getInformationContentForAttribute(int cix) throws UnknownOWLClassException {
		// check if present in cache; if so, use cached value
		if (icClassArray != null && icClassArray[cix] != null) {
			return icClassArray[cix];
		}

		// not cached - retrieve IC using the class
		OWLClass c = classArray[cix];
		Double ic = getInformationContentForAttribute(c);
		if (debugClass != null && c.equals(debugClass)) {
			LOG.info("DEBUG "+c+" IX:"+cix+" IC= "+ic);
		}
		// place results in cache, creating a new cache if none exists
		if (icClassArray == null) {
			icClassArray = new Double[classArray.length];
		}
		icClassArray[cix] = ic;
		return ic;
	}


	@Override
	public Set<Node<OWLClass>> getInferredAttributes(OWLNamedIndividual a) {
		return new HashSet<Node<OWLClass>>(elementToInferredAttributesMap.get(a));
	}

	@Override
	public Set<Node<OWLClass>> getNamedReflexiveSubsumers(OWLClass a) {
		return ancs(a);
	}

	@Override
	public Set<Node<OWLClass>> getNamedCommonSubsumers(OWLClass c, OWLClass d) throws UnknownOWLClassException {
		EWAHCompressedBitmap bmc = ancsBitmapCachedModifiable(c);
		EWAHCompressedBitmap bmd = ancsBitmapCachedModifiable(d);
		EWAHCompressedBitmap cad = bmc.and(bmd);
		Set<Node<OWLClass>> nodes = new HashSet<Node<OWLClass>>();
		for (int ix : cad.toArray()) {
			OWLClassNode node = new OWLClassNode(classArray[ix]);
			nodes.add(node);
		}
		return nodes;
	}

	private Set<Node<OWLClass>> getNamedCommonSubsumers(int cix, int dix) throws UnknownOWLClassException {
		EWAHCompressedBitmap bmc = ancsBitmapCachedModifiable(cix);
		EWAHCompressedBitmap bmd = ancsBitmapCachedModifiable(dix);
		EWAHCompressedBitmap cad = bmc.and(bmd);
		Set<Node<OWLClass>> nodes = new HashSet<Node<OWLClass>>();
		for (int ix : cad.toArray()) {
			OWLClassNode node = new OWLClassNode(classArray[ix]);
			nodes.add(node);
		}
		return nodes;
	}

	private EWAHCompressedBitmap getNamedCommonSubsumersAsBitmap(int cix, int dix) throws UnknownOWLClassException {
		EWAHCompressedBitmap bmc = ancsBitmapCachedModifiable(cix);
		EWAHCompressedBitmap bmd = ancsBitmapCachedModifiable(dix);
		EWAHCompressedBitmap cad = bmc.and(bmd);
		return cad;
	}


	//@Override
	public Set<Node<OWLClass>> getNamedUnionSubsumers(OWLClass c, OWLClass d) throws UnknownOWLClassException {
		EWAHCompressedBitmap bmc = ancsBitmapCachedModifiable(c);
		EWAHCompressedBitmap bmd = ancsBitmapCachedModifiable(d);
		EWAHCompressedBitmap cud = bmc.or(bmd);
		Set<Node<OWLClass>> nodes = new HashSet<Node<OWLClass>>();
		for (int ix : cud.toArray()) {
			OWLClassNode node = new OWLClassNode(classArray[ix]);
			nodes.add(node);
		}
		return nodes;
	}

	@Override
	public int getNamedCommonSubsumersCount(OWLClass c, OWLClass d) throws UnknownOWLClassException {
		EWAHCompressedBitmap bmc = ancsBitmapCachedModifiable(c);
		EWAHCompressedBitmap bmd = ancsBitmapCachedModifiable(d);
		return bmc.andCardinality(bmd);
	}

	@Override
	public Set<Node<OWLClass>> getNamedCommonSubsumers(OWLNamedIndividual i,
			OWLNamedIndividual j) throws UnknownOWLClassException {
		EWAHCompressedBitmap bmc = ancsBitmapCachedModifiable(i);
		EWAHCompressedBitmap bmd = ancsBitmapCachedModifiable(j);
		EWAHCompressedBitmap cad = bmc.and(bmd);
		Set<Node<OWLClass>> nodes = new HashSet<Node<OWLClass>>();
		for (int ix : cad.toArray()) {
			OWLClassNode node = new OWLClassNode(classArray[ix]);
			nodes.add(node);
		}
		return nodes;
	}

	private Set<Node<OWLClass>> getNamedUnionSubsumers(OWLNamedIndividual i,
			OWLNamedIndividual j) throws UnknownOWLClassException {
		EWAHCompressedBitmap bmc = ancsBitmapCachedModifiable(i);
		EWAHCompressedBitmap bmd = ancsBitmapCachedModifiable(j);
		EWAHCompressedBitmap cad = bmc.or(bmd);
		Set<Node<OWLClass>> nodes = new HashSet<Node<OWLClass>>();
		for (int ix : cad.toArray()) {
			OWLClassNode node = new OWLClassNode(classArray[ix]);
			nodes.add(node);
		}
		return nodes;
	}

	@Override
	public Set<Node<OWLClass>> getNamedLowestCommonSubsumers(OWLClass c,
			OWLClass d) throws UnknownOWLClassException {
		EWAHCompressedBitmap cad = getNamedLowestCommonSubsumersAsBitmap(c, d);
		Set<Node<OWLClass>> nodes = new HashSet<Node<OWLClass>>();
		// TODO - optimize this & ensure all elements of an equivalence set are included
		for (int ix : cad.toArray()) {
			OWLClassNode node = new OWLClassNode(classArray[ix]);
			nodes.add(node);
		}
		return nodes;

	}

	private Set<Node<OWLClass>> getNamedLowestCommonSubsumers(int cix, int dix) throws UnknownOWLClassException {
		EWAHCompressedBitmap cad = getNamedLowestCommonSubsumersAsBitmap(cix, dix);
		Set<Node<OWLClass>> nodes = new HashSet<Node<OWLClass>>();
		// TODO - optimize this & ensure all elements of an equivalence set are included
		for (int ix : cad.toArray()) {
			OWLClassNode node = new OWLClassNode(classArray[ix]);
			nodes.add(node);
		}
		return nodes;

	}


	// fast bitmap implementation of LCS
	private EWAHCompressedBitmap getNamedLowestCommonSubsumersAsBitmap(OWLClass c,
			OWLClass d) throws UnknownOWLClassException  {
		EWAHCompressedBitmap bmc = ancsBitmapCachedModifiable(c);
		EWAHCompressedBitmap bmd = ancsBitmapCachedModifiable(d);
		EWAHCompressedBitmap cad = bmc.and(bmd);	
		int[] csInts = cad.toArray();
		for (int ix : csInts) {
			cad = cad.andNot(ancsProperBitmapCachedModifiable(classArray[ix]));
		}
		return cad;
	}

	private EWAHCompressedBitmap getNamedLowestCommonSubsumersAsBitmap(int cix, int dix)
			throws UnknownOWLClassException  {
		EWAHCompressedBitmap bmc = ancsBitmapCachedModifiable(cix);
		EWAHCompressedBitmap bmd = ancsBitmapCachedModifiable(dix);
		EWAHCompressedBitmap cad = bmc.and(bmd);	
		int[] csInts = cad.toArray();
		for (int ix : csInts) {
			cad = cad.andNot(ancsProperBitmapCachedModifiable(classArray[ix]));
		}
		return cad;
	}

	@Deprecated
	private Set<Node<OWLClass>> getNamedLowestCommonSubsumersNaive(OWLClass a,
			OWLClass b) throws UnknownOWLClassException {
		// currently no need to cache this, as only called from
		// getLowestCommonSubsumerIC, which does its own caching
		Set<Node<OWLClass>> commonSubsumerNodes = getNamedCommonSubsumers(a, b);
		Set<Node<OWLClass>> rNodes = new HashSet<Node<OWLClass>>();

		// remove redundant
		for (Node<OWLClass> node : commonSubsumerNodes) {
			rNodes.addAll(getReasoner().getSuperClasses(
					node.getRepresentativeElement(), false).getNodes());
		}
		commonSubsumerNodes.removeAll(rNodes);
		return commonSubsumerNodes;
	}


	@Override
	public double getAttributeSimilarity(OWLClass c, OWLClass d, Metric metric) throws UnknownOWLClassException {
		if (metric.equals(Metric.JACCARD)) {
			return getAttributeJaccardSimilarity(c, d);
		} else if (metric.equals(Metric.OVERLAP)) {
			return getNamedCommonSubsumers(c, d).size();
		} else if (metric.equals(Metric.NORMALIZED_OVERLAP)) {
			return getNamedCommonSubsumers(c, d).size()
					/ Math.min(getNamedReflexiveSubsumers(c).size(),
							getNamedReflexiveSubsumers(d).size());
		} else if (metric.equals(Metric.DICE)) {
			// TODO
			return -1;
		} else {
			return 0;
		}
	}

	@Override
	public AttributePairScores getPairwiseSimilarity(OWLClass c, OWLClass d)
			throws UnknownOWLClassException {
		AttributePairScores s = new AttributePairScores(c,d);
		EWAHCompressedBitmap bmc = ancsBitmapCachedModifiable(c);
		EWAHCompressedBitmap bmd = ancsBitmapCachedModifiable(d);

		s.simjScore =  bmc.andCardinality(bmd) / (double) bmc.orCardinality(bmd);
		s.asymmetricSimjScore =  bmc.andCardinality(bmd) / (double) bmd.cardinality();
		s.inverseAsymmetricSimjScore =  bmc.andCardinality(bmd) / (double) bmc.cardinality();

		ScoreAttributeSetPair sap = getLowestCommonSubsumerWithIC(c, d);
		s.lcsIC = sap.score;
		s.lcsSet = sap.attributeClassSet;
		return s;
	}


	@Override
	public double getAttributeJaccardSimilarity(OWLClass c, OWLClass d) throws UnknownOWLClassException {
		EWAHCompressedBitmap bmc = ancsBitmapCachedModifiable(c);
		EWAHCompressedBitmap bmd = ancsBitmapCachedModifiable(d);

		return bmc.andCardinality(bmd) / (double) bmc.orCardinality(bmd);
	}

	@Override
	public int getAttributeJaccardSimilarityAsPercent(OWLClass c,
			OWLClass d) throws UnknownOWLClassException {
		EWAHCompressedBitmap bmc = ancsBitmapCachedModifiable(c);
		EWAHCompressedBitmap bmd = ancsBitmapCachedModifiable(d);

		return (bmc.andCardinality(bmd) * 100) / bmc.orCardinality(bmd);
	}


	@Override
	public double getElementJaccardSimilarity(OWLNamedIndividual i,
			OWLNamedIndividual j) throws UnknownOWLClassException {
		EWAHCompressedBitmap bmc = ancsBitmapCachedModifiable(i);
		EWAHCompressedBitmap bmd = ancsBitmapCachedModifiable(j);

		return bmc.andCardinality(bmd) / (double) bmc.orCardinality(bmd);
	}

	@Override
	public int getElementJaccardSimilarityAsPercent(OWLNamedIndividual i,
			OWLNamedIndividual j) throws UnknownOWLClassException {
		EWAHCompressedBitmap bmc = ancsBitmapCachedModifiable(i);
		EWAHCompressedBitmap bmd = ancsBitmapCachedModifiable(j);

		return (bmc.andCardinality(bmd) * 100) / bmc.orCardinality(bmd);
	}



	@Override
	public double getAsymmetricAttributeJaccardSimilarity(OWLClass c, OWLClass d) throws UnknownOWLClassException {
		EWAHCompressedBitmap bmc = ancsBitmapCachedModifiable(c);
		EWAHCompressedBitmap bmd = ancsBitmapCachedModifiable(d);

		return bmc.andCardinality(bmd) / (double) bmd.cardinality();
	}

	//@Override
	public double getAsymmetricElementJaccardSimilarity(OWLNamedIndividual i,
			OWLNamedIndividual j) throws UnknownOWLClassException {
		EWAHCompressedBitmap bmc = ancsBitmapCachedModifiable(i);
		EWAHCompressedBitmap bmd = ancsBitmapCachedModifiable(j);

		return bmc.andCardinality(bmd) / (double) bmd.cardinality();
	}

	//@Override
	public int getAsymmetricElementJaccardSimilarityAsPercent(OWLNamedIndividual i,
			OWLNamedIndividual j) throws UnknownOWLClassException {
		EWAHCompressedBitmap bmc = ancsBitmapCachedModifiable(i);
		EWAHCompressedBitmap bmd = ancsBitmapCachedModifiable(j);

		return (bmc.andCardinality(bmd) * 100) / bmd.cardinality();
	}


	// SimGIC
	// TODO - optimize
	@Override
	public double getElementGraphInformationContentSimilarity(
			OWLNamedIndividual i, OWLNamedIndividual j) throws UnknownOWLClassException {
		// TODO - optimize
		long t = System.currentTimeMillis();
		EWAHCompressedBitmap bmc = ancsBitmapCachedModifiable(i);
		EWAHCompressedBitmap bmd = ancsBitmapCachedModifiable(j);
		EWAHCompressedBitmap cad = bmc.and(bmd);
		EWAHCompressedBitmap cud = bmc.or(bmd);


		//Set<Node<OWLClass>> ci = getNamedCommonSubsumers(i, j);
		//Set<Node<OWLClass>> cu = getNamedUnionSubsumers(i, j);
		double sumICboth = 0;
		double sumICunion = 0;

		// faster than translating to integer list
		IntIterator it = cud.intIterator();
		while (it.hasNext()) {
			int x = it.next();
			double ic = getInformationContentForAttribute(x);
			// TODO - we can avoid doing this twice by using xor in the bitmap
			sumICunion += ic;

			if (cad.get(x)) {
				sumICboth += ic;
			}
		}

		totalTimeGIC += tdelta(t);
		this.totalCallsGIC++;

		return sumICboth / sumICunion;
	}

	// TODO - optimize
	@Override
	public double getAttributeGraphInformationContentSimilarity(
			OWLClass c, OWLClass d) throws UnknownOWLClassException {
		return getAttributeGraphInformationContentSimilarity(classIndex.get(c), 
				classIndex.get(d));
	}


	private double getAttributeGraphInformationContentSimilarity(
			int cix, int dix) throws UnknownOWLClassException {
		long t = System.currentTimeMillis();
		EWAHCompressedBitmap bmc = ancsBitmapCachedModifiable(cix);
		EWAHCompressedBitmap bmd = ancsBitmapCachedModifiable(dix);
		EWAHCompressedBitmap cad = bmc.and(bmd);
		EWAHCompressedBitmap cud = bmc.or(bmd);

		double sumICboth = 0;
		double sumICunion = 0;
		// faster than translating to integer list
		IntIterator it = cud.intIterator();
		while (it.hasNext()) {
			int x = it.next();
			double ic = getInformationContentForAttribute(x);
			// TODO - we can avoid doing this twice by using xor in the bitmap
			sumICunion += ic;

			if (cad.get(x)) {
				sumICboth += ic;
			}
		}
		totalTimeGIC += tdelta(t);
		this.totalCallsGIC++;
		return sumICboth / sumICunion;
	}

	//@Override
	public double getAsymmetricElementGraphInformationContentSimilarity(
			OWLNamedIndividual i, OWLNamedIndividual j) throws UnknownOWLClassException {
		// TODO - optimize
		Set<Node<OWLClass>> ci = getNamedCommonSubsumers(i, j);
		Set<Node<OWLClass>> cu = this.getInferredAttributes(j);
		double sumICboth = 0;
		double sumICunion = 0;
		for (Node<OWLClass> c : cu) {
			// TODO - we can avoid doing this twice by using xor in the bitmap
			sumICunion += getInformationContentForAttribute(c
					.getRepresentativeElement());
			if (ci.contains(c)) {
				sumICboth += getInformationContentForAttribute(c
						.getRepresentativeElement());
			}

		}
		return sumICboth / sumICunion;
	}

	// NOTE: current implementation will also return redundant classes if
	// they rank the same - use groupwise if true maxIC required
	@Override
	public ScoreAttributeSetPair getSimilarityMaxIC(OWLNamedIndividual i,
			OWLNamedIndividual j) throws UnknownOWLClassException {

		Set<Node<OWLClass>> atts = getNamedCommonSubsumers(i,j);

		ScoreAttributeSetPair best = new ScoreAttributeSetPair(0.0);
		for (Node<OWLClass> n : atts) {
			OWLClass c = n.getRepresentativeElement();
			Double ic = getInformationContentForAttribute(c);
			if (Math.abs(ic - best.score) < 0.000001) {
				// tie for best attribute
				best.addAttributeClass(c);
			}
			if (ic > best.score) {
				best = new ScoreAttributeSetPair(ic, c);
			}
		}
		return best;
	}

	@Override
	public ScoreAttributeSetPair getSimilarityBestMatchAverageAsym(
			OWLNamedIndividual i, OWLNamedIndividual j) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ScoreAttributeSetPair getSimilarityBestMatchAverageAsym(
			OWLNamedIndividual i, OWLNamedIndividual j, Metric metric) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ScoreAttributeSetPair getSimilarityBestMatchAverage(
			OWLNamedIndividual i, OWLNamedIndividual j, Metric metric,
			Direction dir) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public ElementPairScores getGroupwiseSimilarity(OWLNamedIndividual i, OWLNamedIndividual j) throws UnknownOWLClassException {
		ElementPairScores s = new ElementPairScores(i,j);
		populateSimilarityMatrix(i, j, s);
		s.simGIC = getElementGraphInformationContentSimilarity(i, j);
		s.combinedScore = (int) (s.simGIC * 100); // default - TODO - combined
		return s;
	}

	protected void populateSimilarityMatrix(
			OWLNamedIndividual i, OWLNamedIndividual j,
			ElementPairScores ijscores) throws UnknownOWLClassException {

		/*
		EWAHCompressedBitmap bmc = ancsBitmapCachedModifiable(i);
		EWAHCompressedBitmap bmd = ancsBitmapCachedModifiable(j);
		EWAHCompressedBitmap cad = bmc.and(bmd);
		EWAHCompressedBitmap cud = bmc.or(bmd);
		Set<Node<OWLClass>> nodes = new HashSet<Node<OWLClass>>();
		for (int ix : cad.toArray()) {
			OWLClassNode node = new OWLClassNode(classArray[ix]);
			nodes.add(node);
		}
		 */

		/*
		ijscores.simGIC = getElementGraphInformationContentSimilarity(i, j);
		ijscores.asymmetricSimGIC = getAsymmetricElementGraphInformationContentSimilarity(i, j);
		ijscores.inverseAsymmetricSimGIC = getAsymmetricElementGraphInformationContentSimilarity(j, i);
		 */

		ijscores.simjScore = getElementJaccardSimilarity(i, j);
		ijscores.asymmetricSimjScore = 
				getAsymmetricElementJaccardSimilarity(i, j);
		ijscores.inverseAsymmetricSimjScore =
				getAsymmetricElementJaccardSimilarity(j, i);

		Vector<OWLClass> cs = new Vector<OWLClass>(getAttributesForElement(i));
		Vector<OWLClass> ds = new Vector<OWLClass>(getAttributesForElement(j));
		populateSimilarityMatrix(cs, ds, ijscores);
	}

	private void populateSimilarityMatrix(Vector<OWLClass> cs,
			Vector<OWLClass> ds, ElementPairScores ijscores)  throws UnknownOWLClassException {

		ScoreAttributeSetPair bestsap = null;
		int csize = cs.size();
		int dsize = ds.size();
		ijscores.cs = cs;
		ijscores.ds = ds;
		double total = 0.0;
		double[][] scoreMatrix = new double[csize][dsize];
		ScoreAttributeSetPair[][] sapMatrix = new ScoreAttributeSetPair[csize][dsize];
		ScoreAttributeSetPair[] bestSapForC = new ScoreAttributeSetPair[csize];
		ScoreAttributeSetPair[] bestSapForD = new ScoreAttributeSetPair[dsize];
		double bestMatchCTotal = 0;
		double bestMatchDTotal = 0;

		// populate matrix
		for (int cx=0; cx<csize; cx++) {
			OWLClass c = cs.elementAt(cx);
			int cix = classIndex.get(c);
			ScoreAttributeSetPair bestcsap = null;
			for (int dx=0; dx<dsize; dx++) {
				OWLClass d = ds.elementAt(dx);
				int dix = classIndex.get(d);
				ScoreAttributeSetPair sap = getLowestCommonSubsumerWithIC(cix,dix);
				if (sap == null)
					continue;
				sapMatrix[cx][dx] = sap;
				double score = sap.score;
				total += score;
				if (bestsap == null || score >= bestsap.score) {
					bestsap = sap;
				}
				if (bestcsap == null || score >= bestcsap.score) {
					bestcsap = sap;
				}
			}
			bestSapForC[cx] = bestcsap;
			if (bestcsap != null)
				bestMatchCTotal += bestcsap.score;
		}

		// retrieve best values for each D
		for (int dx=0; dx<dsize; dx++) {
			ScoreAttributeSetPair bestdsap = null;
			for (int cx=0; cx<csize; cx++) {
				ScoreAttributeSetPair sap = sapMatrix[cx][dx];
				if (sap != null && (bestdsap == null || sap.score >= bestdsap.score)) {
					bestdsap = sap;
				}
			}
			if (bestdsap != null) {
				bestSapForD[dx] = bestdsap;
				bestMatchDTotal += bestdsap.score;
			}
		}

		// TODO - use these
		ijscores.avgIC = total / (csize * dsize);
		ijscores.bmaAsymIC = bestMatchCTotal / (double)csize;
		ijscores.bmaInverseAsymIC = bestMatchDTotal / (double)dsize;
		ijscores.bmaSymIC = (bestMatchCTotal + bestMatchDTotal) / (double)(csize+dsize);

		ijscores.iclcsMatrix = new ScoreMatrix<ScoreAttributeSetPair>();
		ijscores.iclcsMatrix.matrix = sapMatrix;
		ijscores.iclcsMatrix.bestForC = bestSapForC;
		ijscores.iclcsMatrix.bestForD = bestSapForD;

		if (bestsap != null) {
			ijscores.maxIC = bestsap.score;
			ijscores.maxICwitness = bestsap.attributeClassSet;
		}
		else {
			//LOG.warn("No best S.A.P. for "+ijscores);
			ijscores.maxIC = 0.0;
			ijscores.maxICwitness = null;			
		}

	}

	// uses integer 2D array cache
	private ScoreAttributeSetPair getLowestCommonSubsumerWithIC(int cix, int dix)
			throws UnknownOWLClassException {
		return getLowestCommonSubsumerWithIC(cix, dix, null);
	}
	private ScoreAttributeSetPair getLowestCommonSubsumerWithIC(int cix, int dix, Double thresh)
			throws UnknownOWLClassException {
		
		// if cache is disabled altogether, head straight for the implementation
		if (isDisableLCSCache) {
			return getLowestCommonSubsumerWithICNoCache(cix, dix);
		}

		// if the cache does not exist, initialize it
		if (ciPairIsCached == null) {
			// Estimates: 350mb for MP
			// 5.4Gb for 30k classes
			//int size = this.getAllAttributeClasses().size();
			int size = classArray.length;
			LOG.info("Creating 2D cache of "+size+" * "+size);
			ciPairIsCached = new boolean[size][size];
			ciPairLCS = new int[size][size];
			LOG.info("Created LCS cache"+size+" * "+size);
			//ciPairScaledScore = new short[size][size];
			//LOG.info("Created score cache cache"+size+" * "+size);
		}
		
		// if either (1) an entry exists in the cache or (2) we are
		// building the cache from fresh, then do a lookup
		if (!isNoLookupForLCSCache && ciPairIsCached[cix][dix]) {
			// TODO null vs 0
			int lcsix = ciPairLCS[cix][dix];

			return new ScoreAttributeSetPair(icClassArray[lcsix],
					classArray[lcsix]);
		}
		if (!isNoLookupForLCSCache && isLCSCacheFullyPopulated) {
			// true if a pre-generated cache has been loaded and there is no entry for this pair;
			// a cache excludes certain pairs if they are below threshold (see below)
			return null;
		}

		// use base method and cache results.
		// if score is below threshold, then nothing is returned or cached.
		// This reduces the size of the cache.
		ScoreAttributeSetPair sap = 
				getLowestCommonSubsumerWithICNoCache(cix, dix);
		if (debugClass != null && sap.attributeClassSet != null &&
				debugClassMessages < 100 &&
				sap.attributeClassSet.contains(debugClass)) {
			LOG.info("DEBUG1 "+debugClass+" ix="+ classIndex.get(debugClass)+" "+sap.score);
			debugClassMessages++;
		}
		if (thresh != null && sap.score < thresh) {			
			return null;
		}

		ciPairIsCached[cix][dix] = true;
		OWLClass lcsCls = null;
		if (sap.attributeClassSet != null && !sap.attributeClassSet.isEmpty()) {
			// we take an arbitrary member
			lcsCls = sap.attributeClassSet.iterator().next();
			int lcsix = classIndex.get(lcsCls);
			ciPairLCS[cix][dix] = lcsix;
			if (debugClass != null && lcsCls.equals(debugClass) && debugClassMessages2 < 100) {
				LOG.info("DEBUG2 "+lcsix+" " +sap.attributeClassSet+" sap.score="+sap.score);
				debugClassMessages2++;
			}
			//icClassArray[lcsix] = sap.score;
		}
		else {
			//TODO - remove obsoletes
			LOG.warn("uh oh"+classArray[cix] + " "+
					classArray[dix]+" "+sap.attributeClassSet);
		}

		return sap;
	}

	@Override
	public ScoreAttributeSetPair getLowestCommonSubsumerWithIC(OWLClass c,
			OWLClass d) throws UnknownOWLClassException {
		return getLowestCommonSubsumerWithIC(classIndex.get(c), classIndex.get(d));
	}

	@Override
	public ScoreAttributeSetPair getLowestCommonSubsumerWithIC(OWLClass c,
			OWLClass d, Double thresh) throws UnknownOWLClassException {
		return getLowestCommonSubsumerWithIC(classIndex.get(c), classIndex.get(d), thresh);
	}

	private ScoreAttributeSetPair getLowestCommonSubsumerWithICNoCache(int cix, int dix)
			throws UnknownOWLClassException {
		long t = System.currentTimeMillis();
		EWAHCompressedBitmap cad = getNamedLowestCommonSubsumersAsBitmap(cix, dix);

		Set<OWLClass> lcsClasses = new HashSet<OWLClass>();
		double maxScore = 0.0;
		for (int ix : cad.toArray()) {
			double score = 
					getInformationContentForAttribute(ix);
			double sdiff = score - maxScore;
			if (sdiff >= 0) {
				if (sdiff > 0.01) {
					lcsClasses= new HashSet<OWLClass>(Collections.singleton(classArray[ix]));
					maxScore = score;					
				}
				else {
					lcsClasses.add(classArray[ix]);
					maxScore = score;					
				}
			}
//			if (score == maxScore) {
//				lcsClasses.add(classArray[ix]);
//				maxScore = score;
//			}
//			else if (score >= maxScore) {
//				lcsClasses= new HashSet<OWLClass>(Collections.singleton(classArray[ix]));
//				maxScore = score;
//			}
		}
		if (lcsClasses.size() == 0) {
			// TODO - remove obsoletes
			//LOG.warn("Hmmmm "+c+" "+d+" "+lcs);
		}
		totalTimeLCSIC += tdelta(t);
		this.totalCallsLCSIC++;

		return new ScoreAttributeSetPair(maxScore, lcsClasses);
	}

	public List<ElementPairScores> findMatches(Set<OWLClass> atts, String targetIdSpace) throws UnknownOWLClassException {
		Set<OWLClass> csetFilteredDirect = new HashSet<OWLClass>(); // direct
		Set<OWLClass> cset = new HashSet<OWLClass>(); // closure
		Set<OWLClass> redundant = new HashSet<OWLClass>(); // closure
		boolean isIgnoreUnknownClasses = false;
		List<ElementPairScores> scoreSets = 
				new ArrayList<ElementPairScores>();
		int minSimJPct = 
				(int) (getPropertyAsDouble(SimConfigurationProperty.minimumSimJ, 0.05) * 100);
		double minMaxIC = getPropertyAsDouble(SimConfigurationProperty.minimumMaxIC, 2.5);

		// FIND CLOSURE
		for (OWLClass c : atts) {
			if (!this.getAllAttributeClasses().contains(c)) {
				if (isIgnoreUnknownClasses)
					continue;
				throw new UnknownOWLClassException(c);
			}
			csetFilteredDirect.add(c);
			for (Node<OWLClass> n : getNamedReflexiveSubsumers(c)) {
				cset.add(n.getRepresentativeElement());
			}
			for (Node<OWLClass> n :getNamedSubsumers(c)) {
				redundant.addAll(n.getEntities());
			}
		}

		csetFilteredDirect.removeAll(redundant);
		Vector csetV = new Vector<OWLClass>(atts.size());
		for (OWLClass c : csetFilteredDirect) {
			csetV.add(c);
		}

		// benchmarking
		long tSimJ = 0;
		int nSimJ = 0;
		long tMaxIC = 0;
		int nMaxIC = 0;
		long tSimGIC = 0;
		int nSimGIC = 0;
		long tBMA = 0;
		int nBMA = 0;
		long startTime = System.currentTimeMillis();
		
		// for calculation of phenodigm score
		double maxMaxIC = 0.0;
		double maxBMA = 0.0;
		
		EWAHCompressedBitmap searchProfileBM = ancsBitmapCached(cset);
		for (OWLNamedIndividual j : getAllElements()) {
			if (targetIdSpace != null && !j.getIRI().toString().contains("/"+targetIdSpace+"_")) {
				continue;
			}
			long t = System.currentTimeMillis();
			//LOG.info(" Comparing with:"+j);
			// SIMJ
			EWAHCompressedBitmap jAttsBM = ancsBitmapCachedModifiable(j);
			int cadSize = searchProfileBM.andCardinality(jAttsBM);
			int cudSize = searchProfileBM.orCardinality(jAttsBM);
			int simJPct = (cadSize * 100) / cudSize;
			nSimJ++;
			tSimJ += tdelta(t);
			
			if (nSimJ % 100 == 0) {
				LOG.info("tSimJ = "+tSimJ +" / "+nSimJ);
				LOG.info("tMaxIC = "+tMaxIC +" / "+nMaxIC);
				LOG.info("tSimGIC = "+tSimGIC +" / "+nSimGIC);
				LOG.info("tBMA = "+tBMA +" / "+nBMA);
			}
			
			if (simJPct < minSimJPct) {
				//LOG.info("simJ pct too low : "+simJPct+" = "+cadSize+" / "+cudSize);
				continue;
			}
			ElementPairScores s = new ElementPairScores(null, j);
			s.simjScore = simJPct / (double) 100;
			EWAHCompressedBitmap cad = searchProfileBM.and(jAttsBM);

			// COMMON SUBSUMERS (ALL)
			Set<OWLClass> csSet = new HashSet<OWLClass>();
			for (int ix : cad.toArray()) {
				csSet.add(classArray[ix]);
			}

			// MAXIC
			// TODO - evaluate if this is optimal;
			// MaxIC falls out of BMA calculation, but it may be useful
			// to calculate here to test if more expensive AxA is required
			t = System.currentTimeMillis();
			ScoreAttributeSetPair best = new ScoreAttributeSetPair(0.0);
			double icBest = 0;
			double icSumCAD = 0;
			for (int ix : cad.toArray()) {
				Double ic = getInformationContentForAttribute(ix);
				//OWLClass c = n.getRepresentativeElement();
				//Double ic = getInformationContentForAttribute(c);
				if (ic > icBest) {
					icBest = ic;
				}
				icSumCAD += ic;
			}
			tMaxIC += tdelta(t);
			nMaxIC++;
			if (icBest > maxMaxIC) {
				maxMaxIC = icBest;
			}
			if (icBest < minMaxIC) {
				//LOG.info("maxIC too low : "+icBest);
				continue;
			}
			s.maxIC = icBest;
			//LOG.info("computing simGIC");

			// SIMGIC
			t = System.currentTimeMillis();
			EWAHCompressedBitmap cud = searchProfileBM.or(jAttsBM);
			double icSumCUD = 0;
			for (int ix : cud.toArray()) {
				Double ic = getInformationContentForAttribute(ix);
				icSumCUD += ic;
			}
			s.simGIC = icSumCAD / icSumCUD;
			tSimGIC += tdelta(t);
			nSimGIC++;

			// BEST MATCHES
			t = System.currentTimeMillis();
			Vector dsetV = new Vector<OWLClass>(atts.size());
			for (OWLClass d : this.getAttributesForElement(j)) {
				dsetV.add(d);
			}
			populateSimilarityMatrix(csetV, dsetV, s);
			if (s.bmaSymIC > maxBMA) {
				maxBMA = s.bmaAsymIC;
			}
			tBMA += tdelta(t);
			nBMA++;
			
			scoreSets.add(s);
		}
		// calculate combined/phenodigm score
		// TODO - 
		calculateCombinedScores(scoreSets, maxMaxIC, maxBMA);
		LOG.info("tSimJ = "+tSimJ +" / "+nSimJ);
		LOG.info("tSearch = "+tdelta(startTime) +" / "+nSimJ);
		LOG.info("Sorting "+scoreSets.size()+" matches");
		Collections.sort(scoreSets);
		return scoreSets;
	}
	
	public void calculateCombinedScores(List<ElementPairScores> scoreSets,
			double maxMaxIC, double maxBMA) {
		int maxMaxIC100 = (int)(maxMaxIC * 100);
		int maxBMA100 = (int)(maxBMA * 100);
		LOG.info("Calculating combinedScores - upper bounds = "+maxMaxIC100+ " " + maxBMA100);
		// TODO - optimize this by using % scores as inputs
		for (ElementPairScores s : scoreSets) {
			int pctMaxScore = ((int) (s.maxIC * 10000)) / maxMaxIC100;
			int pctAvgScore = ((int) (s.bmaSymIC * 10000)) / maxMaxIC100;
			s.combinedScore = (pctMaxScore + pctAvgScore)/2;
		}
		
	}


	/**
	 * 
	 * @param c
	 * @param ds
	 * @return scores
	 * @throws UnknownOWLClassException 
	 */
	// TODO - rewrite
	@Override
	public List<AttributesSimScores> compareAllAttributes(OWLClass c, Set<OWLClass> ds) throws UnknownOWLClassException {
		List<AttributesSimScores> scoresets = new ArrayList<AttributesSimScores>();

		EWAHCompressedBitmap bmc = this.ancsBitmapCachedModifiable(c);
		int cSize = bmc.cardinality();

		Set<AttributesSimScores> best = new HashSet<AttributesSimScores>();
		Double bestScore = null;
		for (OWLClass d : ds) {
			EWAHCompressedBitmap bmd = this.ancsBitmapCachedModifiable(d);
			int dSize = bmd.cardinality();
			int cadSize = bmc.andCardinality(bmd);
			int cudSize = bmc.orCardinality(bmd);

			AttributesSimScores s = new AttributesSimScores(c,d);
			s.simJScore = cadSize / (double)cudSize;
			s.AsymSimJScore = cadSize / (double) dSize;
			//ClassExpressionPair pair = new ClassExpressionPair(c, d);
			//ScoreAttributePair lcs = getLowestCommonSubsumerIC(pair, cad, null);
			//s.lcsScore = lcs;
			scoresets.add(s);

			if (bestScore == null) {
				best.add(s);
				bestScore = s.simJScore;
			}
			else if (bestScore == s.simJScore) {
				best.add(s);
			}
			else if (s.simJScore > bestScore) {
				bestScore = s.simJScore;
				best = new HashSet<AttributesSimScores>(Collections.singleton(s));
			}
		}
		for (AttributesSimScores s : best) {
			s.isBestMatch = true;
		}

		return scoresets;
	}

	// ----
	// UTIL
	// ----

	OWLClass thing = null;
	Node<OWLClass> thingNode = null;

	/**
	 * Convenience method. Warning: method name may change
	 * @return owl:Thing (root class)
	 */
	public OWLClass owlThing() {
		if (thing == null)
			thing = getSourceOntology().getOWLOntologyManager().getOWLDataFactory().getOWLThing();
		return thing;
	}

	/**
	 * Convenience method. Warning: method name may change
	 * @return root class (owl:Thing and anything equivalent)
	 */
	public Node<OWLClass> owlThingNode() {
		if (thingNode == null)
			thingNode = getReasoner().getTopClassNode();
		return thingNode;
	}


	// I/O
	
	/* (non-Javadoc)
	 * @see owltools.sim2.AbstractOwlSim#saveState(java.lang.String)
	 */
	public void saveState(String fileName) throws IOException {
		FileOutputStream fos = new FileOutputStream(fileName);
		IOUtils.write("classArray:"+classArray.length+"\n", fos);
		for (int i=0; i<classArray.length; i++) {
			IOUtils.write("classArray "+i+" "+classArray[i]+"\n", fos);			
		}
		IOUtils.write("classIndex:"+classIndex.keySet().size()+"\n", fos);
		for (OWLClass c : classIndex.keySet()) {
			IOUtils.write("classIndex "+c+" "+classIndex.get(c)+"\n", fos);			
		}
		IOUtils.write("attributeClass-index: "+getAllAttributeClasses().size()+"\n", fos);
		for (OWLClass c : this.getAllAttributeClasses()) {
			IOUtils.write("attributeClass-index "+c+" "+classIndex.get(c)+"\n", fos);			
		}
		IOUtils.write("icClassArray:\n", fos);
		for (int i=0; i<icClassArray.length; i++) {
			IOUtils.write("icClassArray "+i+" "+icClassArray[i]+"\n", fos);			
		}
		IOUtils.write("icCache:\n", fos);
		for (OWLClass c : icCache.keySet()) {
			IOUtils.write("icCache "+c+" "+icCache.get(c)+"\n", fos);			
		}
		IOUtils.write("classToRepresentativeClassMap:\n", fos);
		for (OWLClass c : classTorepresentativeClassMap.keySet()) {
			IOUtils.write("classTorepresentativeClassMap "+c+" "+classTorepresentativeClassMap.get(c)+"\n", fos);			
		}
		IOUtils.write("representativeClassMap:\n", fos);
		for (Node<OWLClass> n : representativeClassMap.keySet()) {
			IOUtils.write("representativeClassMap "+n+" "+representativeClassMap.get(n)+"\n", fos);			
		}
		fos.close();
	}
	

	@Override
	public void saveLCSCache(String fileName, Double thresholdIC) throws IOException {
		FileOutputStream fos = new FileOutputStream(fileName);
		
		// iterate through all classes fetching their ICs, using the class index.
		// this has the side effect of ensuring that icClassArray is populated.
		int n=0;
		for (OWLClass c : this.getAllAttributeClasses()) {
			try {
				int cix = classIndex.get(c);
				Double ic = getInformationContentForAttribute(cix);
				Double icCheck = icClassArray[cix]; // for debugging
				Double icBase = getInformationContentForAttribute(c);
				LOG.info("Class "+c+" has ix="+cix+" IC="+ic+
						" IC(check)="+icCheck+" IC(base)="+icBase+
						" C(check)="+getShortId(classArray[cix]));
				n++;
			} catch (UnknownOWLClassException e) {
				LOG.error("cannot find IC values for class "+c, e);
				throw new IOException("unknown: "+c);
			}
		}
		
		// We assume ciPairIsCached is populated
		for ( int cix = 0; cix< ciPairIsCached.length; cix++) {
			boolean[] arr = ciPairIsCached[cix];
			OWLClass c = classArray[cix];
			for ( int dix = 0; dix< arr.length; dix++) {
				if (arr[dix]) {
					//double s = ciPairScaledScore[cix][dix] / (double) scaleFactor;
					int lcsix = ciPairLCS[cix][dix];
					Double s = icClassArray[lcsix];
					if (s == null || s.isNaN() || s.isInfinite()) {
						throw new IOException("No IC for "+classArray[lcsix]);
					}
					if (thresholdIC == null || s.doubleValue() >= thresholdIC) {
						OWLClass d = classArray[dix];
						OWLClass lcs = classArray[lcsix];
						IOUtils.write(getShortId((OWLClass) c) +"\t" + getShortId((OWLClass) d) + "\t" + s + "\t" + 
								getShortId(lcs) + "\n", fos);

					}
				}
			}


		}

		fos.close();
	}


	/**
	 * @param fileName
	 * @throws IOException
	 */
	@Override
	public void loadLCSCache(String fileName) throws IOException {
		try {
			clearLCSCache();
		} catch (UnknownOWLClassException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new IOException("Cannot clear cache");
		}
		LOG.info("Loading LCS cache from "+fileName);

		FileInputStream s = new FileInputStream(fileName);
		//List<String> lines = IOUtils.readLines(s);
		LineIterator itr = IOUtils.lineIterator(s, null);
		while (itr.hasNext()) {
			//for (String line : lines) {
			String line = itr.nextLine();
			String[] vals = line.split("\t");
			OWLClass c1 = getOWLClassFromShortId(vals[0]);
			OWLClass c2 = getOWLClassFromShortId(vals[1]);
			OWLClass a = getOWLClassFromShortId(vals[3]);
			Integer cix = classIndex.get(c1);
			Integer dix = classIndex.get(c2);
			Integer aix = classIndex.get(a);
			if (cix == null) {
				LOG.error("Unknown class C: "+c1);
			}
			if (dix == null) {
				LOG.error("Unknown class D: "+c2);
			}
			if (aix == null) {
				LOG.error("Unknown ancestor class: "+a);
			}

			ciPairIsCached[cix][dix] = true;
			//ciPairScaledScore[cix][dix] = (short)(Double.valueOf(vals[2]) * scaleFactor);
			// TODO - set all IC caches
			ciPairLCS[cix][dix] = aix;
		}
		s.close();
		LOG.info("Finished loading LCS cache from "+fileName);
		isLCSCacheFullyPopulated = true;
	}

	@Override
	protected void setInformtionContectForAttribute(OWLClass c, Double v) {
		icCache.put(c, v);
		if (icClassArray == null)
			icClassArray = new Double[classArray.length];
		if (!classIndex.containsKey(c)) {
			LOG.warn("Non-indexed class: "+c);
		}
		icClassArray[classIndex.get(c)] = v;
	}

	@Override
	protected void clearInformationContentCache() {
		LOG.info("Clearing IC cache");
		testCache = null;
		icCache = new HashMap<OWLClass,Double>();
		icClassArray = null;
	}

	protected void clearLCSCache() throws UnknownOWLClassException  {
		LOG.info("Clearing LCS cache");
		if (classArray == null) {
			createElementAttributeMapFromOntology();
		}
		ciPairLCS = new int[classArray.length][classArray.length];
		//ciPairScaledScore = new short[classArray.length][classArray.length];
		ciPairIsCached = new boolean[classArray.length][classArray.length];		
	}

	@Override
	public Map<OWLNamedIndividual, Set<OWLClass>> getElementToAttributesMap() {
		if (elementToDirectAttributesMap == null) try {
			createElementAttributeMapFromOntology();
		} catch (UnknownOWLClassException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return elementToDirectAttributesMap;
	}

	@Override
	public SummaryStatistics getSimStatistics(String stat) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void calculateMetricStats(Set<OWLNamedIndividual> iset, Set<OWLNamedIndividual> jset) throws UnknownOWLClassException {
		LOG.info("Calculating all-by-all summary statistics for all metrics");
		
		for (String m : metrics) {
			metricStatMeans.put(m, new SummaryStatistics());
			metricStatMins.put(m, new SummaryStatistics());
			metricStatMaxes.put(m, new SummaryStatistics());
		}

		for (OWLNamedIndividual i : iset) {
			HashMap<String,SummaryStatistics>  metricStatIndividual = new HashMap<String,SummaryStatistics>();
			for (String m : metrics) {
				metricStatIndividual.put(m, new SummaryStatistics());
			}
			for (OWLNamedIndividual j : jset) {
				ElementPairScores gwsim = this.getGroupwiseSimilarity(i, j);
				metricStatIndividual.get("bmaAsymIC").addValue(gwsim.bmaAsymIC);
				metricStatIndividual.get("bmaSymIC").addValue(gwsim.bmaSymIC);
				metricStatIndividual.get("bmaInverseAsymIC").addValue(gwsim.bmaInverseAsymIC);
				metricStatIndividual.get("combinedScore").addValue(gwsim.combinedScore);
				metricStatIndividual.get("simJ").addValue(gwsim.simjScore);
				metricStatIndividual.get("simGIC").addValue(gwsim.simGIC);
				metricStatIndividual.get("maxIC").addValue(gwsim.maxIC);
			}
			for (String m : metrics) {
				metricStatMins.get(m).addValue(metricStatIndividual.get(m).getMin());
				metricStatMeans.get(m).addValue(metricStatIndividual.get(m).getMean());
				metricStatMaxes.get(m).addValue(metricStatIndividual.get(m).getMax());
			}
		}

	}
}
