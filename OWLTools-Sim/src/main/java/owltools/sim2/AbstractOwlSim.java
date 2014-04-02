package owltools.sim2;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
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
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.AggregateSummaryStatistics;
import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.StatisticalSummaryValues;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.log4j.Logger;
import org.semanticweb.elk.reasoner.saturation.conclusions.ForwardLink.ThisBackwardLinkRule;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.graph.OWLGraphWrapper;
import owltools.mooncat.ontologymetadata.OntologySetMetadata;
import owltools.sim2.OwlSim.ScoreAttributeSetPair;
import owltools.sim2.SimpleOwlSim.Direction;
import owltools.sim2.SimpleOwlSim.Metric;
import owltools.sim2.SimpleOwlSim.ScoreAttributePair;
import owltools.sim2.SimpleOwlSim.SimConfigurationProperty;
import owltools.sim2.scores.ElementPairScores;
import owltools.util.ClassExpressionPair;


public abstract class AbstractOwlSim implements OwlSim {

	private Logger LOG = Logger.getLogger(AbstractOwlSim.class);

	long totalTimeSimJ = 0;
	long totalCallsSimJ = 0;
	long totalTimeLCSIC = 0;
	long totalCallsLCSIC = 0;
	long totalTimeGIC = 0;
	long totalCallsGIC = 0;
	public SimStats simStats = new SimStats(); 
	protected boolean isDisableLCSCache = false;


	protected OWLReasoner reasoner;
	protected Integer corpusSize; // number of individuals in domain

	protected boolean isLCSCacheFullyPopulated = false;
	protected boolean isNoLookupForLCSCache = false;
	private Properties simProperties;

	public SummaryStatistics overallStats;

	public StatsPerIndividual overallSummaryStatsPerIndividual = new StatsPerIndividual();
	public HashMap<OWLClass,StatsPerIndividual> subgraphSummaryStatsPerIndividual = new HashMap<OWLClass,StatsPerIndividual>();

	public SummaryStatistics simStatsPerIndividual = new SummaryStatistics();

	public HashMap<String,SummaryStatistics> metricStatMeans = new HashMap<String,SummaryStatistics>(); 
	public HashMap<String,SummaryStatistics> metricStatMaxes = new HashMap<String,SummaryStatistics>(); 
	public HashMap<String,SummaryStatistics> metricStatMins = new HashMap<String,SummaryStatistics>(); 






	@Override
	public OWLOntology getSourceOntology() {
		return getReasoner().getRootOntology();
	}

	@Override
	public OWLReasoner getReasoner() {
		return reasoner;
	}


	public boolean isNoLookupForLCSCache() {
		return isNoLookupForLCSCache;
	}

	public void setNoLookupForLCSCache(boolean isNoLookupForLCSCache) {
		this.isNoLookupForLCSCache = isNoLookupForLCSCache;
	}



	public boolean isDisableLCSCache() {
		return isDisableLCSCache;
	}

	public void setDisableLCSCache(boolean isDisableLCSCache) {
		this.isDisableLCSCache = isDisableLCSCache;
	}

	@Override
	public Properties getSimProperties() {
		return simProperties;
	}

	@Override
	public void setSimProperties(Properties simProperties) {
		this.simProperties = simProperties;
	}


	public SimStats getSimStats() {
		return simStats;
	}

	public void setSimStats(SimStats simStats) {
		this.simStats = simStats;
	}

	protected long tdelta(long prev) {
		return System.currentTimeMillis() - prev;
	}

	/**
	 * 
	 */
	public void showTimings() {
		LOG.info("Timings:");
		if (totalCallsSimJ > 0) {
			LOG.info("t(SimJ) ms = "+totalTimeSimJ + " / "+totalCallsSimJ + " = " + totalTimeSimJ / (double) totalCallsSimJ);
		}
		if (totalCallsLCSIC > 0) {
			LOG.info("t(LCS) ms = "+totalTimeLCSIC + " / "+totalCallsLCSIC + " = " + totalTimeLCSIC / (double) totalCallsLCSIC);
		}
		if (totalCallsGIC > 0) {
			LOG.info("t(GIC) ms = "+totalTimeGIC + " / "+totalCallsGIC + " = " + totalTimeGIC / (double) totalCallsGIC);
		}
	}

	public void showTimingsAndReset() {
		showTimings();
		totalTimeSimJ = 0;
		totalCallsSimJ = 0;
		totalTimeLCSIC = 0;
		totalCallsLCSIC = 0;
		totalTimeGIC = 0;
		totalCallsGIC = 0;

	}

	@Override
	public void precomputeAttributeAllByAll()  throws UnknownOWLClassException {
		LOG.info("precomputing attribute all x all");
		long t = System.currentTimeMillis();
		Set<OWLClass> cset = this.getAllAttributeClasses();
		int n=0;
		for (OWLClass c : cset ) {
			n++;
			if (n % 100 == 0) {
				LOG.info("Cached LCS for "+n+" / "+cset.size()+" attributes");
			}
			for (OWLClass d : cset ) {
				getLowestCommonSubsumerWithIC(c, d);
			}			
		}
		LOG.info("Time precomputing attribute all x all = "+tdelta(t));
		showTimingsAndReset();
	}

	@Override
	public Set<Node<OWLClass>> getNamedSubsumers(OWLClass a) {
		return getReasoner().getSuperClasses(a, false).getNodes();
	}


	/* (non-Javadoc)
	 * @see owltools.sim2.OwlSim#getCorpusSize()
	 */	
	public int getCorpusSize() {
		if (corpusSize == null) {
			corpusSize = getAllElements().size();
		}
		return corpusSize;
	}

	/* (non-Javadoc)
	 * @see owltools.sim2.OwlSim#setCorpusSize(int)
	 */
	public void setCorpusSize(int size) {
		corpusSize = size;
	}

	// Note: inefficient for graphs with multiple parentage, due to
	// use of unmemoized recursion
	protected Set<List<OWLClass>> getPaths(OWLClass c, OWLClass d) {
		Set<List<OWLClass>> paths = new HashSet<List<OWLClass>>();
		for (Node<OWLClass> node : getReasoner().getSuperClasses(c, true)) {
			if (node.contains(d)) {
				ArrayList<OWLClass> path = new ArrayList<OWLClass>();
				path.add(d);
				path.add(c);
				paths.add(path);
			}
			else {
				OWLClass nc = node.getRepresentativeElement();
				if (getReasoner().getSuperClasses(nc, false).getFlattened().contains(d)) {
					Set<List<OWLClass>> ppaths = getPaths(nc, d);
					for (List<OWLClass> ppath : ppaths) {
						ArrayList<OWLClass> path = new ArrayList<OWLClass>(ppath);
						path.add(c);
						paths.add(path);
					}
				}
				else {
					// veered off path
				}
			}
		}
		return paths;
	}

	protected int getMinimumDistanceToAncestor(OWLClass c, OWLClass d) {
		int minDist = 0;
		for (List<OWLClass> path : getPaths(c,d)) {
			int dist = path.size()-1;
			if (dist<minDist)
				minDist = dist;
		}
		return minDist;
	}

	protected int getMinimumDistanceViaMICA(OWLClass c, OWLClass d) throws UnknownOWLClassException {
		int minDist = 0;
		for (OWLClass a : getLowestCommonSubsumerWithIC(c, d).attributeClassSet) {
			int dist = getMinimumDistanceToAncestor(c,a) + getMinimumDistanceToAncestor(d,a);
			if (dist<minDist)
				minDist = dist;
		}
		return minDist;
	}

	public void getICSimDisj(OWLClass c, OWLClass d) {

	}

	/* (non-Javadoc)
	 * @see owltools.sim2.OwlSim#getLowestCommonSubsumerWithLinScore(org.semanticweb.owlapi.model.OWLClass, org.semanticweb.owlapi.model.OWLClass)
	 * 
	 * Note this is uncached - subclasses may wish to impement caching
	 */
	public ScoreAttributeSetPair getLowestCommonSubsumerWithLinScore(OWLClass c, OWLClass d)
			throws UnknownOWLClassException {
		ScoreAttributeSetPair sap = this.getLowestCommonSubsumerWithIC(c, d);
		sap.score = (sap.score * 2) / 
				(getInformationContentForAttribute(c) +
						getInformationContentForAttribute(d));
		return sap;
	}


	// may be less performant than direct computation
	public int getAttributeJaccardSimilarityAsPercent(OWLClass a,
			OWLClass b) throws UnknownOWLClassException {
		return (int) (getAttributeJaccardSimilarity(a, b) * 100);
	}
	// may be less performant than direct computation
	public int getElementJaccardSimilarityAsPercent(OWLNamedIndividual i,
			OWLNamedIndividual j) throws UnknownOWLClassException {
		return (int) (getElementJaccardSimilarity(i, j) * 100);
	}

	// may be less performant than direct computation
	public int getAsymmetricAttributeJaccardSimilarityAsPercent(OWLClass a,
			OWLClass b) throws UnknownOWLClassException {
		return (int) (getAsymmetricAttributeJaccardSimilarity(a, b) * 100);
	}

	// may be less performant than direct computation
	public int getAsymmetricElementJaccardSimilarityAsPercent(OWLNamedIndividual i,
			OWLNamedIndividual j) throws UnknownOWLClassException {
		return (int) (getAsymmetricElementJaccardSimilarity(i, j) * 100);
	}


	@Override
	public ElementPairScores getGroupwiseSimilarity(OWLNamedIndividual i, OWLNamedIndividual j) throws Exception {
		ElementPairScores ijscores = new ElementPairScores(i,j);
		//populateSimilarityMatrix(i, j, s);
		ijscores.simGIC = getElementGraphInformationContentSimilarity(i, j);
		ijscores.simjScore = getElementJaccardSimilarity(i, j);
		ijscores.asymmetricSimjScore = 
				getAsymmetricElementJaccardSimilarity(i, j);
		ijscores.inverseAsymmetricSimjScore =
				getAsymmetricElementJaccardSimilarity(j, i);

		// WAS this deprecated function:
		// ScoreAttributeSetPair bma = this.getSimilarityBestMatchAverage(i, j, Metric.IC_MCS, Direction.A_TO_B);
		ScoreAttributeSetPair bmaI = this.getSimilarityBestMatchAverageAsym(i, j, Metric.IC_MCS);
		ijscores.bmaAsymIC = bmaI.score;
		if (i!=j) {
			//we were skipping the inverse calculation before -- intentional?
			ScoreAttributeSetPair bmaJ = this.getSimilarityBestMatchAverageAsym(j, i, Metric.IC_MCS);
			ijscores.bmaInverseAsymIC = bmaJ.score;
			ijscores.bmaSymIC = (bmaI.score + bmaJ.score) / 2;
		} else {
			ijscores.bmaInverseAsymIC = bmaI.score;
			ijscores.bmaSymIC = bmaI.score;
		} 
		return ijscores;
	}

	public List<ElementPairScores> findMatches(OWLNamedIndividual i, String targetIdSpace)
			throws Exception {
		Set<OWLClass> atts = getAttributesForElement(i);
		List<ElementPairScores> matches = findMatches(atts, targetIdSpace);
		for (ElementPairScores m : matches) {
			m.i = i;
		}
		return matches;
	}

	public List<ElementPairScores> findMatches(OWLNamedIndividual i, String targetIdSpace, double minSimJPct, double minMaxIC)
			throws Exception {
		Set<OWLClass> atts = getAttributesForElement(i);
		List<ElementPairScores> matches = findMatches(atts, targetIdSpace, minSimJPct, minMaxIC);
		for (ElementPairScores m : matches) {
			m.i = i;
		}
		return matches;
	}


	/* (non-Javadoc)
	 * @see owltools.sim2.OwlSim#getEntropy()
	 */
	@Override
	public Double getEntropy() throws UnknownOWLClassException {
		return getEntropy(getAllAttributeClasses());
	}

	/* (non-Javadoc)
	 * @see owltools.sim2.OwlSim#getEntropy(java.util.Set)
	 */
	@Override
	public Double getEntropy(Set<OWLClass> cset) throws UnknownOWLClassException {
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

	@Override
	public void dispose() {

	}

	// CACHES
	public final String icIRIString = "http://owlsim.org/ontology/ic"; // TODO

	@Override
	public OWLOntology cacheInformationContentInOntology() throws OWLOntologyCreationException, UnknownOWLClassException {
		OWLOntologyManager mgr = getSourceOntology().getOWLOntologyManager();
		OWLDataFactory df = mgr.getOWLDataFactory();
		OWLOntology o = mgr.createOntology();
		OWLAnnotationProperty p = df.getOWLAnnotationProperty(IRI.create(icIRIString));
		for (OWLClass c : getSourceOntology().getClassesInSignature()) {
			Double ic = getInformationContentForAttribute(c);
			if (ic != null) {
				mgr.addAxiom(o,
						df.getOWLAnnotationAssertionAxiom(p, 
								c.getIRI(), 
								df.getOWLLiteral(ic)));
			}

		}
		return o;
	}

	protected abstract void setInformtionContectForAttribute(OWLClass c, Double v);
	protected abstract void clearInformationContentCache();

	@Override
	public void setInformationContentFromOntology(OWLOntology o) {
		OWLOntologyManager mgr = getSourceOntology().getOWLOntologyManager();
		OWLDataFactory df = mgr.getOWLDataFactory();
		clearInformationContentCache();
		//icCache = new HashMap<OWLClass, Double>();
		for (OWLAnnotationAssertionAxiom ax : o.getAxioms(AxiomType.ANNOTATION_ASSERTION)) {
			if (ax.getProperty().getIRI().toString().equals(icIRIString)) {
				OWLLiteral lit = (OWLLiteral) ax.getValue();
				OWLClass c = df.getOWLClass((IRI) ax.getSubject());
				Double v = lit.parseDouble();
				setInformtionContectForAttribute(c, v);
			}
		}
	}

	public void saveState(String fileName) throws IOException {
		LOG.warn("not implemented");
	}

	public void saveLCSCache(String fileName) throws IOException {
		saveLCSCache(fileName, null);
	}

	protected final String prefix = "http://purl.obolibrary.org/obo/";
	protected String getShortId(OWLClass c) {
		IRI x = ((OWLClass) c).getIRI();
		return x.toString().replace(prefix, ""); // todo - do not hardcode
	}
	protected OWLClass getOWLClassFromShortId(String id) {
		// todo - standardize this
		if (id.equals("http://www.w3.org/2002/07/owl#Thing") ||
				id.equals("Thing") ||
				id.equals("owl:Thing")) {
			return getSourceOntology().getOWLOntologyManager().getOWLDataFactory().getOWLThing();
		}
		return getSourceOntology().getOWLOntologyManager().getOWLDataFactory().getOWLClass(IRI.create(prefix + id));
	}


	//
	// ENRICHMENT
	//

	public EnrichmentConfig enrichmentConfig;

	public EnrichmentConfig getEnrichmentConfig() {
		return enrichmentConfig;
	}

	public void setEnrichmentConfig(EnrichmentConfig enrichmentConfig) {
		this.enrichmentConfig = enrichmentConfig;
	}

	private void addEnrichmentResult(EnrichmentResult result,
			List<EnrichmentResult> results) throws UnknownOWLClassException {
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

	public List<EnrichmentResult> calculateAllByAllEnrichment() throws MathException, UnknownOWLClassException {
		OWLClass thing = this.getSourceOntology().getOWLOntologyManager().getOWLDataFactory().getOWLThing();
		return calculateAllByAllEnrichment(thing, thing, thing);
	}


	/**
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
					throws MathException, UnknownOWLClassException {
		List<EnrichmentResult> results = new Vector<EnrichmentResult>();
		OWLClass nothing = getSourceOntology().getOWLOntologyManager().getOWLDataFactory().getOWLNothing();
		for (OWLClass sampleSetClass : getReasoner().getSubClasses(pc1, false)
				.getFlattened()) {
			if (sampleSetClass.equals(nothing)) {
				continue;
			}
			int sampleSetSize = getNumElementsForAttribute(sampleSetClass);
			LOG.info("sample set class:" + sampleSetClass + " size="+sampleSetSize);
			if (sampleSetSize < 2)
				continue;
			List<EnrichmentResult> resultsInner = new Vector<EnrichmentResult>();
			for (OWLClass enrichedClass : this.getReasoner()
					.getSubClasses(pc2, false).getFlattened()) {
				if (enrichedClass.equals(nothing)) continue;
				//LOG.info(" population class:" + enrichedClass + " size="+getNumElementsForAttribute(enrichedClass));
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
		results = filterEnrichmentResults(results);
		return results;
	}

	public List<EnrichmentResult> filterEnrichmentResults(List<EnrichmentResult> resultsIn) {
		// assume sorted by p-value
		LOG.info("Sorting: "+resultsIn.size()+" results");
		List<EnrichmentResult> resultsOut = new ArrayList<EnrichmentResult>();

		// map from all sample set classes to all better enriched classes
		Map<OWLClass,Set<OWLClass>> betters = new HashMap<OWLClass,Set<OWLClass>>();
		for (int i=0; i<resultsIn.size(); i++) {
			EnrichmentResult r = resultsIn.get(i);
			OWLClass sc = r.sampleSetClass;
			OWLClass ec = r.enrichedClass;
			//LOG.info(" R: "+r);
			if (!betters.containsKey(sc)) {
				betters.put(sc, new HashSet<OWLClass>());
			}
			boolean isRedundant = false;

			// everything that came before will have a higher score;
			// for the given sample class, find the enriched classes
			// that are better; if any of these  is more specific than the
			// current ec under consideration, skip it
			for (OWLClass bc : betters.get(sc)) {
				for (Node<OWLClass> bca : getNamedSubsumers(bc)) {
					//LOG.info("T: "+bca+" of "+bc+" SC="+sc);
					if (bca.contains(ec)) {
						isRedundant = true;
						LOG.info("  Redundant: "+sc+" "+ec+" with:"+bc);
						break;
					}
				}
				if (isRedundant)
					break;
			}
			if (!isRedundant) {
				resultsOut.add(r);
			}
			betters.get(sc).add(ec);
		}
		return resultsOut;

	}

	/**
	 * @param populationClass
	 * @param sampleSetClass
	 * @return results
	 * @throws MathException
	 * @throws UnknownOWLClassException 
	 */
	public List<EnrichmentResult> calculateEnrichment(OWLClass populationClass,
			OWLClass sampleSetClass) throws MathException, UnknownOWLClassException {
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
	 * @throws UnknownOWLClassException 
	 */
	public EnrichmentResult calculatePairwiseEnrichment(OWLClass populationClass,
			OWLClass sampleSetClass, OWLClass enrichedClass) throws MathException, UnknownOWLClassException {

		// LOG.info("Hyper :"+populationClass
		// +" "+sampleSetClass+" "+enrichedClass);
		int populationClassSize;
		if (populationClass != null) {
			populationClassSize = getNumElementsForAttribute(populationClass);
		}
		else {
			populationClassSize = getCorpusSize();
			populationClass =  
					getSourceOntology().getOWLOntologyManager().getOWLDataFactory().getOWLThing();
		}
		int sampleSetClassSize = getNumElementsForAttribute(sampleSetClass);
		int enrichedClassSize = getNumElementsForAttribute(enrichedClass);
		// LOG.info("Hyper :"+populationClassSize
		// +" "+sampleSetClassSize+" "+enrichedClassSize);
		Set<OWLNamedIndividual> eiSet = getElementsForAttribute(sampleSetClass);
		eiSet.retainAll(this.getElementsForAttribute(enrichedClass));
		int eiSetSize = eiSet.size();
		if (eiSetSize == 0) {
			return null;
		}
		//LOG.info(" shared elements: "+eiSet.size()+" for "+enrichedClass);
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
		return new EnrichmentResult(sampleSetClass, enrichedClass, p, pCorrected, 
				populationClassSize, sampleSetClassSize, enrichedClassSize, eiSetSize);
	}

	// hardcode bonferoni for now
	Integer correctionFactor = null; // todo - robust cacheing

	private int getCorrectionFactor(OWLClass populationClass) throws UnknownOWLClassException {
		if (correctionFactor == null) {
			int n = 0;
			for (OWLClass sc : this.getReasoner()
					.getSubClasses(populationClass, false).getFlattened()) {
				//LOG.info("testing count for " + sc);
				if (getNumElementsForAttribute(sc) > 1) {
					n++;
					//LOG.info("  ++testing count for " + sc);
				}
			}

			correctionFactor = n;
		}
		return correctionFactor;
	}

	/**
	 * @param c
	 * @param d
	 * @return P(c|d) = P(c^d|d)
	 * @throws UnknownOWLClassException 
	 */
	@Override
	public double getConditionalProbability(OWLClass c, OWLClass d) throws UnknownOWLClassException {
		Set<OWLNamedIndividual> cis = this.getElementsForAttribute(c);
		Set<OWLNamedIndividual> dis = this.getElementsForAttribute(d);
		cis.retainAll(dis);
		return cis.size() / (double) dis.size();
	}

	// PROPS

	protected String getProperty(SimConfigurationProperty p) {
		if (simProperties == null) {
			return null;
		}
		return simProperties.getProperty(p.toString());
	}

	protected Double getPropertyAsDouble(SimConfigurationProperty p) {
		String v = getProperty(p);
		if (v == null)
			return null;
		return Double.valueOf(v);
	}

	protected Double getPropertyAsDouble(SimConfigurationProperty p, 
			Double dv) {
		Double v = getPropertyAsDouble(p);
		if (v==null)
			return dv;
		return v;
	}

	protected Boolean getPropertyAsBoolean(SimConfigurationProperty p) {
		String v = getProperty(p);
		if (v == null) {
			return false;
		}
		return Boolean.valueOf(v);
	}

	public void computeSystemStats() throws UnknownOWLClassException {
		Set<OWLNamedIndividual> insts = this.getAllElements();
		LOG.info("Computing system stats for " + insts.size() + " individuals");
		LOG.info("Creating singular stat scores for all IDspaces");

		Collection<SummaryStatistics> aggregate = new ArrayList<SummaryStatistics>();

		this.overallStats = new SummaryStatistics();

		int counter = 0;
		for (OWLNamedIndividual i : insts) {			
			counter++;
			SummaryStatistics statsPerIndividual = computeIndividualStats(i);			
			//put this individual into the aggregate
			if (statsPerIndividual.getN() == 0) {
				LOG.error("No annotations found for Individual "+i.toStringID());
			} else {
				aggregate.add(statsPerIndividual);
			}
			//TODO: put this individual into an idSpace aggregate
			//			String idSpace = i.getIRI().getNamespace();
			this.overallStats.addValue(statsPerIndividual.getMean());
			if (counter % 1000 == 0) {
				LOG.info("Finished "+counter+" individuals");
			}
		}
		//		this.aggregateStatsPerIndividual = AggregateSummaryStatistics.aggregate(aggregate);	
		StatsPerIndividual myStats = new StatsPerIndividual();

		myStats.mean = getSummaryStatisticsForCollection(aggregate,Stat.MEAN);
		myStats.sum  = getSummaryStatisticsForCollection(aggregate,Stat.SUM);
		myStats.min  = getSummaryStatisticsForCollection(aggregate,Stat.MIN);
		myStats.max  = getSummaryStatisticsForCollection(aggregate,Stat.MAX);
		myStats.n  = getSummaryStatisticsForCollection(aggregate,Stat.N);		
		myStats.aggregate = AggregateSummaryStatistics.aggregate(aggregate);
		this.overallSummaryStatsPerIndividual = myStats;
		LOG.info("Finished computing overall statsPerIndividual:\n"+this.getSummaryStatistics().toString());
	}

	public void computeSystemStatsForSubgraph(OWLClass c) throws UnknownOWLClassException {
		Set<OWLNamedIndividual> insts = this.getAllElements();
		LOG.info("Computing system stats for subgraph rooted at" + c.toString() +" with "+ insts.size() + " individuals");
		//		LOG.info("Creating singular stat scores for all IDspaces");

		Collection<SummaryStatistics> aggregate = new ArrayList<SummaryStatistics>();

		SummaryStatistics subgraphStats = new SummaryStatistics();

		for (OWLNamedIndividual i : insts) {			
			SummaryStatistics statsPerIndividual = computeIndividualStatsForSubgraph(i,c);			
			//put this individual into the aggregate
			if (statsPerIndividual.getN() == 0) {
				//LOG.info("No annotations found in this subgraph for Individual "+i.toStringID());
			} else {
				//LOG.info(statsPerIndividual.getN()+" Annotations found in this subgraph for Individual "+i.toStringID());
				aggregate.add(statsPerIndividual);
			}
			//TODO: put this individual into an idSpace aggregate
			//String idSpace = i.getIRI().getNamespace();
			subgraphStats.addValue(statsPerIndividual.getMean());
		}		
		StatsPerIndividual myStats = new StatsPerIndividual();
		myStats.mean = getSummaryStatisticsForCollection(aggregate,Stat.MEAN);
		myStats.sum  = getSummaryStatisticsForCollection(aggregate,Stat.SUM);
		myStats.min  = getSummaryStatisticsForCollection(aggregate,Stat.MIN);
		myStats.max  = getSummaryStatisticsForCollection(aggregate,Stat.MAX);
		myStats.n  = getSummaryStatisticsForCollection(aggregate,Stat.N);		
		this.subgraphSummaryStatsPerIndividual.put(c, myStats);
	}


	/**
	 * This function will take an aggregated collection of Summary Statistics
	 * and will generate a derived {@link SummaryStatistic} based on a flag for the  
	 * desired summation.  This is particularly helpful for finding out the
	 * means of the individual statistics of the collection.
	 * For example, if you wanted to find out the mean of means of the collection
	 * you would call this function like <p>
	 * getSummaryStatisticsForCollection(aggregate,1).getMean(); <p>
	 * Or if you wanted to determine the max number of annotations per
	 * individual, you could call: <p>
	 * getSummaryStatisticsForCollection(aggregate,5).getMax(); <p>
	 * The stat flag should be set to the particular individual statistic that should
	 * be summarized over.
	 *
	 * @param aggregate The aggregated collection of summary statistics
	 * @param stat  Integer flag for the statistic (1:mean ; 2:sum; 3:min; 4:max; 5:N)
	 * @return {@link SummaryStatistics} of the selected statistic
	 */
	public SummaryStatistics getSummaryStatisticsForCollection(Collection<SummaryStatistics> aggregate, Stat stat) {
		//LOG.info("Computing stats over collection of "+aggregate.size()+" elements ("+stat+"):");
		//TODO: turn stat into enum
		int x = 0;
		//To save memory, I am using SummaryStatistics, which does not store the values,
		//but this could be changed to DescriptiveStatistics to see values
		//as well as other statistical functions like distributions
		SummaryStatistics stats = new SummaryStatistics();
		Double v = 0.0;
		ArrayList<String> vals = new ArrayList();
		for (SummaryStatistics s : aggregate) {
			switch (stat) {
			case MEAN : v= s.getMean(); stats.addValue(s.getMean()); break;
			case SUM : v=s.getSum(); stats.addValue(s.getSum()); break;
			case MIN : v=s.getMin(); stats.addValue(s.getMin()); break;
			case MAX : v=s.getMax(); stats.addValue(s.getMax()); break;
			case N : v= ((int)s.getN())*1.0; stats.addValue(s.getN()); break;
			};
			//vals.add(v.toString());
		};
		//LOG.info("vals: "+vals.toString());
		return stats;
	}


	public SummaryStatistics computeIndividualStats(OWLNamedIndividual i) throws UnknownOWLClassException {
		//LOG.info("Computing individual stats for "+i.toString());
		return this.computeAttributeSetSimilarityStats(this.getAttributesForElement(i));
	}

	public SummaryStatistics computeIndividualStatsForSubgraph(OWLNamedIndividual i,OWLClass c) throws UnknownOWLClassException {
		return this.computeAttributeSetSimilarityStatsForSubgraph(this.getAttributesForElement(i),c);
	}

	public SummaryStatistics computeAttributeSetSimilarityStats(Set<OWLClass> atts)  {

		SummaryStatistics statsPerAttSet = new SummaryStatistics();
		//		Set<OWLClass> allClasses = getSourceOntology().getClassesInSignature(true);
		OWLDataFactory g = getSourceOntology().getOWLOntologyManager().getOWLDataFactory();

		for (OWLClass c : atts) {
			Double ic;
			try {
				ic = this.getInformationContentForAttribute(c);
				if (ic == null) { 
					//If a class hasn't been annotated in the loaded corpus, we will
					//assume that it is very rare, and assign MaxIC
					if (g.getOWLClass(c.getIRI()) != null) {
						ic = this.getSummaryStatistics().max.getMax();
					} else {
						throw new UnknownOWLClassException(c); }
				}
				if (ic.isInfinite() || ic.isNaN()) {
					//If a class hasn't been annotated in the loaded corpus, we will
					//assume that it is very rare, and assign MaxIC
					//a different option would be to skip adding this value, 
					//but i'm not sure that's wise
					ic = this.getSummaryStatistics().max.getMax();
				}
				//LOG.info("IC for "+c.toString()+"is: "+ic);
				statsPerAttSet.addValue(ic);	

			} catch (UnknownOWLClassException e) {
				//This is an extra catch here, but really it should be caught upstream.
				LOG.info("Unknown class "+c.toStringID()+" submitted for summary stats. Removed from calculation.");
				continue;
			}
		}
		return statsPerAttSet;
	}	

	public SummaryStatistics computeAttributeSetSimilarityStatsForSubgraph(Set<OWLClass> atts, OWLClass sub)  {
		SummaryStatistics statsPerAttSet = new SummaryStatistics();
		//		Set<OWLClass> allClasses = getSourceOntology().getClassesInSignature(true);
		OWLDataFactory g = getSourceOntology().getOWLOntologyManager().getOWLDataFactory();
		OWLGraphWrapper gwrap = new OWLGraphWrapper(getSourceOntology());

		for (OWLClass c : atts) {
			Double ic;
			try {
				//check if sub is an inferred superclass of the current annotated class
				//TODO check if i need all of these; this might be expensive and unnecessary
				if (gwrap.getAncestorsReflexive(c).contains(sub) || 
						getReasoner().getSuperClasses(c, false).containsEntity(sub) ||
						getReasoner().getEquivalentClasses(c).contains(sub))  { 
					ic = this.getInformationContentForAttribute(c);

					if (ic == null) { 
						//If a class hasn't been annotated in the loaded corpus, we will
						//assume that it is very rare, and assign MaxIC
						if (g.getOWLClass(c.getIRI()) != null) {
							ic = this.getSummaryStatistics().max.getMax();
						} else {
							throw new UnknownOWLClassException(c); }
					}

					if (ic.isInfinite() || ic.isNaN()) {
						//If a class hasn't been annotated in the loaded corpus, we will
						//assume that it is very rare, and assign MaxIC
						ic = this.getSummaryStatistics().max.getMax();
					}
					//LOG.info("IC for "+c.toString()+"is: "+ic);
					statsPerAttSet.addValue(ic);	
				} else {
					//LOG.info("tossing "+c.toString()+"; not a subclass of "+sub.toString());
				}
			} catch (UnknownOWLClassException e) {
				//This is an extra catch here, but really it should be caught upstream.
				LOG.info("Unknown class "+c.toStringID()+" submitted for summary stats. Removed from calculation.");
				continue;
			}
		}
		return statsPerAttSet;
	}	


	public StatisticalSummaryValues getSystemStats() {
		//		return this.aggregateStatsPerIndividual;
		return this.overallSummaryStatsPerIndividual.aggregate;
	}

	public StatsPerIndividual getSummaryStatistics() {
		return this.overallSummaryStatsPerIndividual;
	}

	public StatsPerIndividual getSummaryStatistics(OWLClass c) {
		return this.subgraphSummaryStatsPerIndividual.get(c);
	}

	public SummaryStatistics getSimStatistics() {
		SummaryStatistics s = new SummaryStatistics();
		//for each metric, we need the min/max/average

		return s;
	}

	public SummaryStatistics computeIndividualSimilarityStats(OWLNamedIndividual i) throws UnknownOWLClassException {
		SummaryStatistics s = new SummaryStatistics();

		return s;
	}

	public HashMap<String,SummaryStatistics> getMetricStats(Stat stat) {
		HashMap<String,SummaryStatistics> s = new HashMap<String,SummaryStatistics>();
		switch(stat) {
		case MIN : s = this.metricStatMins; break;
		case MAX : s = this.metricStatMaxes; break;
		case MEAN : s = this.metricStatMeans; break;
		}
		return s;
	}

	public double calculateOverallAnnotationSufficiencyForIndividual(OWLNamedIndividual i) throws UnknownOWLClassException {
		return calculateOverallAnnotationSufficiencyForAttributeSet(this.getAttributesForElement(i));
	}	

	public double calculateSubgraphAnnotationSufficiencyForIndividual(OWLNamedIndividual i, OWLClass c) throws UnknownOWLClassException {
		return calculateSubgraphAnnotationSufficiencyForAttributeSet(this.getAttributesForElement(i), c);
	}	

	public double calculateOverallAnnotationSufficiencyForAttributeSet(Set<OWLClass> atts) throws UnknownOWLClassException {
		SummaryStatistics stats = computeAttributeSetSimilarityStats(atts);
		if ((this.getSummaryStatistics() == null) || Double.isNaN(this.getSummaryStatistics().mean.getMean())) {
			LOG.info("Stats have not been computed yet - doing this now");
			this.computeSystemStats();
		}
		// score = mean(atts)/mean(overall) + max(atts)/max(overall) + sum(atts)/mean(sum(overall))
		double overall_score = 0.0;
		Double mean_score = stats.getMean();
		Double max_score = stats.getMax();
		Double sum_score = stats.getSum();
		if (!(mean_score.isNaN() || max_score.isNaN() || sum_score.isNaN())) {
			mean_score = StatUtils.min(new double[]{(mean_score / this.overallSummaryStatsPerIndividual.mean.getMean()),1.0});
			max_score = StatUtils.min(new double[]{(max_score / this.overallSummaryStatsPerIndividual.max.getMax()),1.0});
			sum_score = StatUtils.min(new double[]{(sum_score / this.overallSummaryStatsPerIndividual.sum.getMean()),1.0});
			overall_score = (mean_score + max_score + sum_score) / 3;		
		}
		LOG.info("Overall mean: "+mean_score + " max: "+max_score + " sum:"+sum_score + " combined:"+overall_score);
		return overall_score;
	}


	public double calculateSubgraphAnnotationSufficiencyForAttributeSet(Set<OWLClass> atts, OWLClass c) throws UnknownOWLClassException {
		SummaryStatistics stats = computeAttributeSetSimilarityStatsForSubgraph(atts,c);
		//TODO: compute statsPerIndividual for this subgraph
		if ((this.overallSummaryStatsPerIndividual == null ) || (Double.isNaN(this.overallSummaryStatsPerIndividual.max.getMean()))) {
			LOG.info("Stats have not been computed yet - doing this now");
			this.computeSystemStats();
		}

		if (!(this.subgraphSummaryStatsPerIndividual.containsKey(c))) {
			//only do this once for the whole system, per class requested
			this.computeSystemStatsForSubgraph(c);
		}
		// score = mean(atts)/mean(overall) + max(atts)/max(overall) + sum(atts)/mean(sum(overall))
		//TODO: need to normalize this based on the whole corpus
		double score = 0.0;
		Double mean_score = stats.getMean();
		Double max_score = stats.getMax();
		Double sum_score = stats.getSum();

		if (!(mean_score.isNaN() || max_score.isNaN() || sum_score.isNaN())) {
			mean_score = StatUtils.min(new double[]{(mean_score / this.subgraphSummaryStatsPerIndividual.get(c).mean.getMean()),1.0});
			max_score = StatUtils.min(new double[]{(max_score / this.subgraphSummaryStatsPerIndividual.get(c).max.getMax()),1.0});
			sum_score = StatUtils.min(new double[]{(sum_score / this.subgraphSummaryStatsPerIndividual.get(c).sum.getMean()),1.0});
			score = (mean_score + max_score + sum_score) / 3;		
		}
		LOG.info(getShortId(c)+" n: "+stats.getN()+" mean: "+mean_score + " max: "+max_score + " sum:"+sum_score + " combined:"+score);
		return score;
	}

	public void calculateCombinedScore(ElementPairScores s, double maxMaxIC, double maxBMA) {
		int maxMaxIC100 = (int)(maxMaxIC * 100);
		int maxBMA100 = (int)(maxBMA * 100);
		if (maxMaxIC == 0 || maxBMA == 0) {
			return;
		}
		int pctMaxScore = ((int) (s.maxIC * 10000)) / maxMaxIC100;
		//TODO should this be using maxBMA100?
		int pctAvgScore = ((int) (s.bmaSymIC * 10000)) / maxBMA100;

		s.combinedScore = (pctMaxScore + pctAvgScore)/2;
	}


	public OwlSimMetadata getMetadata() {
		OwlSimMetadata md = new OwlSimMetadata();
		md.ontologySet = new OntologySetMetadata(this.getSourceOntology());
		md.individualCount = getAllElements().size();

		return md;
	}
}
