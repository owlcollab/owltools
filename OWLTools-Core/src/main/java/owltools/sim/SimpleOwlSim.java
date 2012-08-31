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
import java.util.Vector;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.HypergeometricDistributionImpl;
import org.apache.log4j.Logger;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import owltools.io.OWLPrettyPrinter;
import owltools.sim.preprocessor.SimPreProcessor;

/**
 * 
 * <h3>Semantic Similarity</h3>
 * 
 * 
 * @author cjm
 *
 */
public class SimpleOwlSim {

	private Logger LOG = Logger.getLogger(SimpleOwlSim.class);

	Set<OWLObjectProperty> viewProperties;
	List<OWLObjectProperty> sourceViewProperties; // pre-processing is ordered
	Set<OWLObjectProperty> viewsCreated = new HashSet<OWLObjectProperty>();
	private OWLDataFactory owlDataFactory;
	private OWLOntologyManager owlOntologyManager;
	//private OWLReasonerFactory reasonerFactory;
	private Set<OWLClass> ignoreSubClassesOf = null;
	//OWLReasoner reasoner;
	private Set<OWLClass> fixedAttributeClasses = null;
	final String OBO_PREFIX = "http://purl.obolibrary.org/obo/";
	private Integer corpusSize;

	private OWLOntology sourceOntology;       
	private SimPreProcessor simPreProcessor = null;
	private OWLOntology resultsOntology = null;
	
	Set<OWLClass> viewClasses;
	Map<OWLClassExpression,Set<Node<OWLClass>>> superclassMap = null;
	Map<OWLEntity,Set<OWLClass>> elementToAttributesMap;
	Map<OWLClass,Set<OWLEntity>> attributeToElementsMap;
	Map<OWLClassExpression, OWLClass> expressionToClassMap;
	Map<OWLClassExpressionPair, ScoreAttributePair> simCache;
	//Map<OWLClassExpressionPair, OWLClass> lcsCache;
	Map<OWLClass, Integer> attributeElementCount = null;
	Map<OWLClassExpression,OWLClass> lcsExpressionToClass = new HashMap<OWLClassExpression,OWLClass>();



	public enum Stage {VIEW, LCS};
	public String baseFileName = null;

	public SimpleOwlSim(OWLOntology sourceOntology) {
		super();
		this.sourceOntology = sourceOntology;
		this.owlOntologyManager = sourceOntology.getOWLOntologyManager();
		this.owlDataFactory = owlOntologyManager.getOWLDataFactory();
		this.sourceOntology = sourceOntology;
		init();
	}

	private void init() {
		//reasonerFactory = new ElkReasonerFactory();
		elementToAttributesMap = new HashMap<OWLEntity,Set<OWLClass>>();
		attributeToElementsMap = new HashMap<OWLClass,Set<OWLEntity>>();
		expressionToClassMap = new HashMap<OWLClassExpression, OWLClass>();
		viewClasses = new HashSet<OWLClass>();
		sourceViewProperties = new ArrayList<OWLObjectProperty>();
		simCache = new HashMap<OWLClassExpressionPair, ScoreAttributePair>();
		//lcsCache = new HashMap<OWLClassExpressionPair, OWLClass>();
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
		//this.simPreProcessor.setReasoner(getReasoner());
		this.simPreProcessor.preprocess();
	}

	


	public void saveOntology(Stage stage) throws FileNotFoundException, OWLOntologyStorageException {
		if (this.baseFileName != null) {
			this.saveOntology(baseFileName +"-" + stage + ".owl");
		}
	}
	public void saveOntology(String fn) throws FileNotFoundException, OWLOntologyStorageException {
		FileOutputStream os = new FileOutputStream(new File(fn));
		OWLOntologyFormat owlFormat = new RDFXMLOntologyFormat();

		owlOntologyManager.saveOntology(sourceOntology, owlFormat, os);
	}

	public OWLReasoner getReasoner() {
		return simPreProcessor.getReasoner();
		/*
		if (reasoner == null) {
			reason();
		}
		return reasoner;
		*/
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


	public void assertInferences(boolean isRemoveLogicalAxioms) {
		// TODO
	}
	


	// ----------- ----------- ----------- -----------
	// SUBSUMERS AND LOWEST COMMON SUBSUMERS
	// ----------- ----------- ----------- -----------

	// TODO - DRY - preprocessor	
	public Set<Node<OWLClass>> getNamedSubsumers(OWLClassExpression a) {
		return getReasoner().getSuperClasses(a, false).getNodes();
	}

	// TODO - DRY - preprocessor
	public Set<Node<OWLClass>> getNamedSubsumers(OWLNamedIndividual a) {
		Set<Node<OWLClass>> nodes = new HashSet<Node<OWLClass>>();
		// for now we do not use reasoners for the first step (Elk does not support ABoxes)
		for (OWLClass c: this.getAttributesForElement(a)) {
			nodes.addAll(getReasoner().getSuperClasses(c, false).getNodes());
		}
		return nodes;
	}

	// TODO - DRY - preprocessor
	public Set<Node<OWLClass>> getNamedReflexiveSubsumers(OWLClassExpression a) {
		if (superclassMap != null && superclassMap.containsKey(a)) {
			return new HashSet<Node<OWLClass>>(superclassMap.get(a));
		}
		Set<Node<OWLClass>> nodes =  new HashSet<Node<OWLClass>>(getReasoner().getSuperClasses(a, false).getNodes());
		nodes.add(getReasoner().getEquivalentClasses(a));
		if (superclassMap == null) {
			superclassMap = new HashMap<OWLClassExpression,Set<Node<OWLClass>>>();
		}
		superclassMap.put(a, new HashSet<Node<OWLClass>>(nodes));
		return nodes;
	}

	// TODO - DRY - preprocessor
	public Set<Node<OWLClass>> getNamedCommonSubsumers(OWLClassExpression a, OWLClassExpression b) {
		Set<Node<OWLClass>> nodes = getNamedReflexiveSubsumers(a);
		nodes.retainAll(getNamedReflexiveSubsumers(b));
		return nodes;
	}
	
	// TODO - DRY - preprocessor
	public Set<Node<OWLClass>> getNamedCommonSubsumers(OWLNamedIndividual a, OWLNamedIndividual b) {
		Set<Node<OWLClass>> nodes = getNamedSubsumers(a);
		nodes.retainAll(getNamedSubsumers(b));
		return nodes;
	}

	// TODO - DRY - preprocessor
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
		// TODO - the preprocessor implementation may not be optimized for similarity calculation
		return this.simPreProcessor.getLowestCommonSubsumer(a, b);
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
		// dangerous cast: we assume the ontology has been fully pre-processed.
		// however, if the ontology was pre-processed in advance additional filtering
		// may have happened, not captured in NullPreProcessor...
		OWLClassExpression lcsx = getLowestCommonSubsumer(a, b);
		if (lcsx instanceof OWLClass) {
			OWLClass lcs = (OWLClass) lcsx;
			ScoreAttributePair sap = new ScoreAttributePair(getInformationContentForAttribute(lcs), lcs);
			simCache.put(pair, sap);
			return sap;			
		}
		else if (lcsx instanceof OWLObjectIntersectionOf) {
			Double bestIC = null;
			OWLClass bestLCS = null;
			for (OWLClassExpression op : ((OWLObjectIntersectionOf)lcsx).getOperands()) {
				if (op instanceof OWLClass) {
					OWLClass lcs = (OWLClass) op;
					Double ic = getInformationContentForAttribute(lcs);
					if (bestIC == null || ic > bestIC) {
						bestIC = ic;
						bestLCS = lcs;
					}
				}
			}
			ScoreAttributePair sap = new ScoreAttributePair(bestIC, bestLCS);
			simCache.put(pair, sap);	
			return sap;
		}
		else {
			LOG.warn("LCS of "+a+" + "+b+" = "+lcsx);
			ScoreAttributePair sap = new ScoreAttributePair(0.0, owlDataFactory.getOWLThing());
			simCache.put(pair, sap);	
			return sap;
		}
		/*
		if (!(lcsx instanceof OWLClass))
			LOG.warn("LCS of "+a+" + "+b+" = "+lcsx);
		OWLClass lcs = (OWLClass) lcsx;
		ScoreAttributePair sap = new ScoreAttributePair(getInformationContentForAttribute(lcs), lcs);
		simCache.put(pair, sap);
		return sap;
		*/
	}

	public ScoreAttributesPair getSimilarityMaxIC(OWLNamedIndividual i, OWLNamedIndividual j) {
		ScoreAttributesPair best = new ScoreAttributesPair(0.0);
		for (OWLClass a : this.getAttributesForElement(i)) {
			for (OWLClass b : this.getAttributesForElement(j)) {
				ScoreAttributePair sap = getLowestCommonSubsumerIC(a, b);
				if (Math.abs(sap.score - best.score) < 0.001) {
					// tie for best attribute
					best.addAttributeClass(sap.attributeClass);
				}
				if (sap.score > best.score) {
					best = new ScoreAttributesPair(sap.score, sap.attributeClass);
				}
			}
		}
		if (resultsOntology != null) {
			//OWLNamedIndividual pair = generateSimPair(i, j, "MaxIC");
			
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
			bestMatches.add(best); // TODO - do something with this
			total += best.score;
			n++;
		}
		ScoreAttributesPair sap = new ScoreAttributesPair(total/n, atts);
		return sap;
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
	 * @return
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
	 * @return
	 * @throws MathException
	 */
	public EnrichmentResult calculatePairwiseEnrichment(OWLClass populationClass,
			OWLClass sampleSetClass, OWLClass enrichedClass) throws MathException {
		
		//LOG.info("Hyper :"+populationClass +" "+sampleSetClass+" "+enrichedClass);
		int populationClassSize = getNumElementsForAttribute(populationClass);
		int sampleSetClassSize = getNumElementsForAttribute(sampleSetClass);
		int enrichedClassSize = getNumElementsForAttribute(enrichedClass);
		//LOG.info("Hyper :"+populationClassSize +" "+sampleSetClassSize+" "+enrichedClassSize);
		HypergeometricDistributionImpl hg = 
			new HypergeometricDistributionImpl(
					populationClassSize,
					sampleSetClassSize,
					enrichedClassSize
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
				Math.min(sampleSetClassSize,
						enrichedClassSize));
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
			return sourceOntology.getClassesInSignature(true);
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
		for (OWLNamedIndividual e : sourceOntology.getIndividualsInSignature(true)) {
			
			// The attribute classes for an individual are the direct inferred
			// named types. We assume that grouping classes have already been generated.
			// if they have not then the types may be as general as {Thing}
			Set<OWLClass> types = getReasoner().getTypes(e, true).getFlattened();
			allTypes.addAll(addElement(e, types));
		}
		// need to materialize as classes...
		LOG.info("Using "+allTypes.size()+" attribute classes, based on individuals: "+sourceOntology.getIndividualsInSignature(true).size());
		fixedAttributeClasses = allTypes;
	}

	// adds an element plus associated attributes
	private Set<OWLClass> addElement(OWLEntity e, Set<OWLClass> atts) {
		// TODO - fully fold TBox so that expressions of form (inh (part_of x))
		// generate a class "part_of x", to ensure that a SEP grouping class is created
		Set<OWLClass> attClasses = new HashSet<OWLClass>();
		for (OWLClass attClass : atts) {

			// filtering, e.g. Type :human. This is a somewhat unsatisfactory way to do this;
			// better to filter at the outset - TODO
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


	public Set<OWLClass> getAttributesForElement(OWLEntity e) {
		return this.elementToAttributesMap.get(e);
	}

	/**
	 *  Mapping between an attribute (e.g. phenotype class) and the number
	 *  of instances it classifies
	 */
	protected void precomputeAttributeElementCount() {
		if (attributeElementCount != null)
			return;
		attributeElementCount = new HashMap<OWLClass, Integer>();
		// some high level attributes will classify all or most of the ABox;
		//  this way may be faster...
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
		LOG.info("Finished precomputing attribute element count");
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
			LOG.info("corpusSize = "+corpusSize);
		}
		return corpusSize;
	}
	public void setCorpusSize(int size) {
		corpusSize = size;
	}

	// IC = 0.0 : 100%   (1/1)
	// IC = 1.0 : 50%    (1/2)
	// IC = 2.0 : 25%    (1/4)
	// IC = 3.0 : 12.5%  (1/8)
	public Double getInformationContentForAttribute(OWLClass c) {
		int freq = getNumElementsForAttribute(c);
		Double ic = null;
		if (freq > 0) {
			ic = -Math.log(((double) (freq) / getCorpusSize())) / Math.log(2);
		}
		return ic;
	}

	

}
