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

	public StatisticalSummaryValues aggregateStatsPerIndividual;
	public SummaryStatistics overallStats;
	public SummaryStatistics meanStatsPerIndividual = new SummaryStatistics();
	public SummaryStatistics minStatsPerIndividual = new SummaryStatistics();
	public SummaryStatistics maxStatsPerIndividual = new SummaryStatistics();
	public SummaryStatistics nStatsPerIndividual = new SummaryStatistics();
	public SummaryStatistics sumStatsPerIndividual = new SummaryStatistics();
	
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
	public ElementPairScores getGroupwiseSimilarity(OWLNamedIndividual i, OWLNamedIndividual j) throws UnknownOWLClassException {
		ElementPairScores ijscores = new ElementPairScores(i,j);
		//populateSimilarityMatrix(i, j, s);
		ijscores.simGIC = getElementGraphInformationContentSimilarity(i, j);
		ijscores.simjScore = getElementJaccardSimilarity(i, j);
		ijscores.asymmetricSimjScore = 
				getAsymmetricElementJaccardSimilarity(i, j);
		ijscores.inverseAsymmetricSimjScore =
				getAsymmetricElementJaccardSimilarity(j, i);
		
		ScoreAttributeSetPair bma = this.getSimilarityBestMatchAverage(i, j, Metric.IC_MCS, Direction.A_TO_B);
		ijscores.bmaSymIC = bma.score;
		return ijscores;
	}

	public List<ElementPairScores> findMatches(OWLNamedIndividual i, String targetIdSpace)
			throws UnknownOWLClassException {
		Set<OWLClass> atts = getAttributesForElement(i);
		return findMatches(atts, targetIdSpace);
	}


	/* (non-Javadoc)
	 * @see owltools.sim2.OwlSim#getEntropy()
	 */


	@Override
	public Double getEntropy() {
		return getEntropy(getAllAttributeClasses());
	}

	/* (non-Javadoc)
	 * @see owltools.sim2.OwlSim#getEntropy(java.util.Set)
	 */

	@Override
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
		return results;
	}
	
	
	/**
	 * @param populationClass
	 * @param sampleSetClass
	 * @return results
	 * @throws MathException
	 */
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

	private int getCorrectionFactor(OWLClass populationClass) {
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
	 */
	@Override
	public double getConditionalProbability(OWLClass c, OWLClass d) {
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
		
		for (OWLNamedIndividual i : insts) {			
			SummaryStatistics statsPerIndividual = computeIndividualStats(i);			
			//put this individual into the aggregate
			if (statsPerIndividual.getN() == 0) {
				LOG.error("No annotations found for Individual "+i.toStringID());
			} else {
				aggregate.add(statsPerIndividual);
			}
			//TODO: put this individual into an idSpace aggregate
			String idSpace = i.getIRI().getNamespace();
			this.overallStats.addValue(statsPerIndividual.getMean());
		}		
		this.aggregateStatsPerIndividual = AggregateSummaryStatistics.aggregate(aggregate);	
		this.meanStatsPerIndividual = getSummaryStatisticsForCollection(aggregate,Stat.MEAN);
		this.sumStatsPerIndividual  = getSummaryStatisticsForCollection(aggregate,Stat.SUM);
		this.minStatsPerIndividual  = getSummaryStatisticsForCollection(aggregate,Stat.MIN);
		this.maxStatsPerIndividual  = getSummaryStatisticsForCollection(aggregate,Stat.MAX);
		this.nStatsPerIndividual  = getSummaryStatisticsForCollection(aggregate,Stat.N);

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
		return this.computeAttributeSetSimilarityStats(this.getAttributesForElement(i));
	}
	
	public SummaryStatistics computeAttributeSetSimilarityStats(Set<OWLClass> atts) throws UnknownOWLClassException {
		SummaryStatistics statsPerAttSet = new SummaryStatistics();
		for (OWLClass c : atts) {
			statsPerAttSet.addValue(this.getInformationContentForAttribute(c));	
		}
		return statsPerAttSet;
	}	
	
	public StatisticalSummaryValues getSystemStats() {
		return this.aggregateStatsPerIndividual;
	}
	
	public SummaryStatistics getSummaryStatistics(Stat stat) {
		SummaryStatistics s = null;
		switch (stat) {
			case MEAN : s = this.meanStatsPerIndividual;  break;
			case SUM : s = this.sumStatsPerIndividual; break;
			case MIN : s = this.minStatsPerIndividual; break;
			case MAX : s = this.maxStatsPerIndividual; break;
			case N : s = this.nStatsPerIndividual; break;
		};
		return s;
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
	
	public double calculateOverallAnnotationSufficiencyForAttributeSet(Set<OWLClass> atts) throws UnknownOWLClassException {
		SummaryStatistics stats = computeAttributeSetSimilarityStats(atts);
		// score = mean(atts)/mean(overall) + max(atts)/max(overall) + sum(atts)/mean(sum(overall))
		double mean_score = StatUtils.min(new double[]{(stats.getMean() / this.meanStatsPerIndividual.getMean()),1.0});
		double max_score = StatUtils.min(new double[]{(stats.getMax() / this.maxStatsPerIndividual.getMax()),1.0});
		double sum_score = StatUtils.min(new double[]{(stats.getSum() / this.sumStatsPerIndividual.getMean()),1.0});
		double overall_score = (mean_score + max_score + sum_score) / 3;		
//		LOG.info("mean: "+mean_score + " max: "+max_score + " sum:"+sum_score + " combined:"+overall_score);
		return overall_score;
	}
}
