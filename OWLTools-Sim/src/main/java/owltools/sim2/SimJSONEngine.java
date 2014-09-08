package owltools.sim2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.reasoner.impl.OWLClassNode;

import owltools.graph.OWLGraphWrapper;
import owltools.sim2.FastOwlSim.ClassCount;
import owltools.sim2.OwlSim.ScoreAttributeSetPair;
import owltools.sim2.OwlSim.Stat;
import owltools.sim2.OwlSim.StatsPerIndividual;
import owltools.sim2.scores.ElementPairScores;

import com.google.gson.Gson;

/**
 * @author cjm
 *
 */
public class SimJSONEngine {

	private Logger LOG = Logger.getLogger(SimJSONEngine.class);

	OwlSim sos;
	OWLGraphWrapper g;

	/**
	 * @param g
	 * @param sos2
	 */
	public SimJSONEngine(OWLGraphWrapper g, OwlSim sos2) {
		this.sos = sos2;
		this.g = g;
	}

	public String search(Set<OWLClass> objAs,
			String targetIdSpace,
			boolean isIgnoreUnknownClasses) throws Exception {
		return search(objAs, targetIdSpace, isIgnoreUnknownClasses, 3000, false);
	}
	
	public String search(Set<OWLClass> objAs,
			String targetIdSpace,
			boolean isIgnoreUnknownClasses, boolean includeFullMatchingTriples) throws Exception {
		return search(objAs, targetIdSpace, isIgnoreUnknownClasses, 3000, includeFullMatchingTriples);
	}

	/**
	 * @param objAs
	 * @param targetIdSpace
	 * @param isIgnoreUnknownClasses
	 * @param limit
	 * @return json string
	 * @throws Exception 
	 */
	public String search(Set<OWLClass> objAs,
			String targetIdSpace,
			boolean isIgnoreUnknownClasses,
			Integer limit) throws Exception {
		return search(objAs, targetIdSpace, isIgnoreUnknownClasses, limit, false);
	}

	
	public String search(Set<OWLClass> objAs,
			String targetIdSpace,
			boolean isIgnoreUnknownClasses,
			Integer limit, boolean includeFullMatchingTriples) throws Exception {
		Set<OWLClass> known = sos.getAllAttributeClasses();
		Set<OWLClass> filteredClasses = new HashSet<OWLClass>();

		Set<String> ids = new HashSet<String>();
		Set<String> idsUnresolved = new HashSet<String>();

		//clean up the query id list
		for (OWLClass objA : objAs) {
			String objAId = objA.getIRI().toString();
			if (!known.contains(objA)) {
				LOG.info("unknown attribute:"+objA);
				idsUnresolved.add(objAId);
				if (isIgnoreUnknownClasses)
					continue;
				throw new UnknownOWLClassException(objA);
			}
			filteredClasses.add(objA);
			ids.add(objAId);
		}
		LOG.info("Unresoved classes: "+idsUnresolved.toString());
		LOG.info("Finding matches for :"+filteredClasses);
		LOG.info("OwlSim = "+sos);
		List<ElementPairScores> matches = sos.findMatches(filteredClasses, targetIdSpace);

		Gson gson = new Gson();
		List<Map> matchObjs = new ArrayList<Map>();
		int n=0;
		for (ElementPairScores m : matches) {
			n++;
			if (limit != null && n > limit)
				break;
			matchObjs.add(makeComparisonResult(m,includeFullMatchingTriples));
		}
		
		Map payload = new HashMap();
		payload.put("query_IRIs", ids);
		payload.put("unresolved", idsUnresolved);
		payload.put("results", matchObjs);
		return gson.toJson(payload);
	}

	private Map makeComparisonResult(ElementPairScores m, Boolean includeFullMatchingTriples) throws UnknownOWLClassException {
		Map mObj = new HashMap();
		mObj.put("i", "_query");
		makeObjAndSet(mObj, "j", m.j);
		mObj.put("combinedScore", m.combinedScore);
		mObj.put("maxIC", m.maxIC);
		mObj.put("bmaSymIC", m.bmaSymIC);
		mObj.put("bmaAsymIC", m.bmaAsymIC);
		mObj.put("bmaInverseAsymIC", m.bmaInverseAsymIC);
		if (m.maxICwitness != null) {
			mObj.put("maxIC_class",makeObj(m.maxICwitness.iterator().next()));
		}
		mObj.put("simJ", m.simjScore);
		mObj.put("simGIC", m.simGIC);
		
		//use a hashmap so that we eliminate duplicate values
		//that come from C and D having identical terms
		HashMap<String,Map> matchingAtts = new HashMap<String,Map>();
		
		for (int ci = 0; ci < m.cs.size(); ci++) {
			for (int di = 0; di < m.ds.size(); di++) {
				ScoreAttributeSetPair cv = m.iclcsMatrix.bestForC[ci];
				ScoreAttributeSetPair dv = m.iclcsMatrix.bestForD[di];
				if ((cv != null) && (dv != null)) {
					OWLClass c = ((FastOwlSim)sos).classTorepresentativeClassMap.get(m.cs.get(ci));
					if (cv.equals(dv)) {
						Map<String,Object> o;
						String objID;
						if (includeFullMatchingTriples) {
							OWLClass d = ((FastOwlSim)sos).classTorepresentativeClassMap.get(m.ds.get(di));
							OWLClass lcs = ((FastOwlSim)sos).classTorepresentativeClassMap.get(cv.getArbitraryAttributeClass());
							objID = g.getIdentifier(c);
							objID.concat(g.getIdentifier(d));
							objID.concat(g.getIdentifier(lcs));								
							o = makeLCSTriple(m.cs.get(ci),m.ds.get(di),cv.getArbitraryAttributeClass());
						} else {
							objID = g.getIdentifier(c);
							o = makeObj(cv.getArbitraryAttributeClass());
						}
						matchingAtts.put(objID,o);
					}
				}
			}
		}
		mObj.put("matches", matchingAtts.values());
		mObj.put("system_stats", makeSummaryStatistics());
		
		return mObj;
	}
	
	public List<Map> makeUniqAttSet(List<Map> list) {

		return list;
	}
	
	/**
	 * @param objAs
	 * @param objBs
	 * @return json
	 * @throws Exception 
	 */
	public String compareAttributeSetPair(Set<OWLClass> objAs, Set<OWLClass> objBs) throws Exception {
		Gson gson = new Gson();
		return compareAttributeSetPair(objAs, objBs, false);
	}

	/**
	 * @param objAs
	 * @param objBs
	 * @param isIgnoreUnknownClasses
	 * @return json
	 * @throws UnknownOWLClassException
	 */
	public String compareAttributeSetPair(Set<OWLClass> objAs, Set<OWLClass> objBs, 
			boolean isIgnoreUnknownClasses) throws Exception {
		Gson gson = new Gson();

		ElementPairScores s = null;
		Map payload = new HashMap();

		//go ahead and check class lists - make sure these are robust before continuing
		Set<OWLClass> known = sos.getAllAttributeClasses();
		Set<OWLClass> okAs = new HashSet<OWLClass>(objAs);
		Set<OWLClass> okBs = new HashSet<OWLClass>(objBs);
		
		Set<String> idsA = new HashSet<String>();
		Set<String> idsB = new HashSet<String>();
		Set<String> idsUnresolved = new HashSet<String>();

		//clean up the query id list
		for (OWLClass objA : objAs) {
			String objId = objA.getIRI().toString();
			idsA.add(objId);
			if (!known.contains(objA)) {
				LOG.info("Skipping Unknown class: "+objId);
				idsUnresolved.add(objId);
				if (isIgnoreUnknownClasses) {
					okAs.remove(objA);
					continue;
				}
				throw new UnknownOWLClassException(objA);
			}
		}
		//clean up the target id list
		for (OWLClass objB : objBs) {
			String objId = objB.getIRI().toString();
			idsB.add(objId);
			if (!known.contains(objB)) {
				LOG.info("Skipping Unknown class: "+objId);
				idsUnresolved.add(objId);
				if (isIgnoreUnknownClasses) {
					okBs.remove(objB);
					continue;
				}
				throw new UnknownOWLClassException(objB);
			}
		}
		
		//generate payload of comparison results, including sim scores
		//to keep things consistent with search, make it a list of one
		List<Map> matchObjs = new ArrayList<Map>();

		try {
				s = sos.getGroupwiseSimilarity(okAs,okBs,0.01,0.01);
				matchObjs.add(makeComparisonResult(s,true));
		} catch (UnknownOWLClassException e) {
			//we should never get here, as we clean up the lists before trying
			if (isIgnoreUnknownClasses) {
				LOG.info("Skipping unknown classes: "+e);
			} else {
				payload.put("UnknownOWLClassException",e);
			}
		} catch (CutoffException e) {
			//the minimal maxIC is smaller than the cutoff - we really don't care
			LOG.info(e);
		}

		payload.put("results",matchObjs);
		payload.put("query_IRIs", idsA);
		payload.put("target_IRIs", idsB);
		payload.put("unresolved", idsUnresolved);

		return gson.toJson(payload);		
	}

	protected Map<String,Object> makeLCSTriple(OWLObject a, OWLObject b, OWLObject lcs) throws UnknownOWLClassException {
		Map<String,Object> triple = new HashMap<String,Object>();
		triple.put("a", makeObj(a));
		triple.put("b", makeObj(b));
		triple.put("lcs", makeObj(lcs));
		return triple;
	}
	
	protected Map<String,Object> makeObj(OWLObject obj) throws UnknownOWLClassException {
		Map<String,Object> m = new HashMap<String,Object>();
		m.put("id", g.getIdentifier(obj));
		m.put("label", g.getLabel(obj));
		if (obj != null && obj.getClass() == OWLClass.class) {
			Double ic = sos.getInformationContentForAttribute(g.getOWLClass(obj));
			if (ic == null) {
				//LOG.info("Using max(maxIC) for "+g.getIdentifier(obj));
				ic = sos.getSummaryStatistics().max.getMax();
			}
			m.put("IC",ic);
		}
		return m;
	}

	protected void makeObjAndSet(Map<String,Object> subj, String key, OWLObject t) throws UnknownOWLClassException {
		Map<String,Object> tobj = makeObj(t);
		if (tobj.keySet().size() > 0)
			subj.put(key, tobj);
	}

	public HashMap<String,String> makeIndividualSummaryStatistics(SummaryStatistics s) {
		HashMap<String,String> stats = new HashMap<String,String>();
		stats.put("n", String.format("%d", s.getN()));		
		stats.put("meanIC", String.format("%1$.5f", s.getMean()));		
		stats.put("maxIC", String.format("%1$.5f", s.getMax()));		
		stats.put("sumIC", String.format("%1$.5f", s.getSum()));		
		stats.put("stdevIC", String.format("%1$.5f", s.getStandardDeviation()));		
		return stats;
	}
	
	public HashMap<String,String> makeSummaryStatistics(StatsPerIndividual s) {
		HashMap<String,String> stats = new HashMap<String,String>();
		stats.put("individuals", String.format("%d", s.max.getN()));		
		stats.put("meanMeanIC", String.format("%1$.5f", s.mean.getMean()));		
		stats.put("meanMaxIC", String.format("%1$.5f", s.max.getMean()));		
		stats.put("maxMaxIC", String.format("%1$.5f", s.max.getMax()));		
		stats.put("maxSumIC", String.format("%1$.5f", s.sum.getMax()));		
		stats.put("meanSumIC", String.format("%1$.5f", s.sum.getMean()));		
		stats.put("meanN", String.format("%1$.5f", s.n.getMean()));		

		return stats;
	}
	
	public HashMap<String,String> makeSummaryStatistics(OWLClass c) {
		StatsPerIndividual s = sos.getSummaryStatistics(c);
		HashMap<String,String> stats = new HashMap<String,String>();
		stats = makeSummaryStatistics(s);	
		return stats;
	}
	
	public HashMap<String,String> makeSummaryStatistics() {
		StatsPerIndividual s = sos.getSummaryStatistics();
		HashMap<String,String> stats = new HashMap<String,String>();
		stats = makeSummaryStatistics(s);	
		return stats;
	}
	

	public String getAnnotationSufficiencyScore(OWLNamedIndividual i) throws UnknownOWLClassException {
		Gson gson = new Gson();
		return gson.toJson(this.makeAnnotationSufficiencyScore(sos.getAttributesForElement(i)));
	}


	public String getAnnotationSufficiencyScore(Set<OWLClass> atts) throws UnknownOWLClassException {
		Gson gson = new Gson();
		return gson.toJson(this.makeAnnotationSufficiencyScore(atts));
	}
	
	public String getAnnotationSufficiencyScore(Set<OWLClass> atts, OWLClass c) throws UnknownOWLClassException {
		Gson gson = new Gson();
		return gson.toJson(this.makeAnnotationSufficiencySubgraphScore(atts,c));
	}

	private Map<String,String> makeAnnotationSufficiencyScore(Set<OWLClass> atts) throws UnknownOWLClassException {
		HashMap<String,String> s = new HashMap<String,String>();
		double score = sos.calculateOverallAnnotationSufficiencyForAttributeSet(atts);
		s.put("score",String.format("%1$.5f",score));
		return s;
	}
	
	private Map<String,String> makeAnnotationSufficiencySubgraphScore(Set<OWLClass> atts, OWLClass c) throws UnknownOWLClassException {
		HashMap<String,String> s = new HashMap<String,String>();
		double score = sos.calculateSubgraphAnnotationSufficiencyForAttributeSet(atts,c);
		s.put("score",String.format("%1$.5f",score));
		//Map<String,Object> annotation_sufficiency = new HashMap<String,Object>();
		//annotation_sufficiency.put("annotation_sufficiency",s); 
		return s;
	}

	private Map<String,Object> makeAttributeInformationProfile(Set<OWLClass> atts) throws UnknownOWLClassException {
		List<Map> alist = new ArrayList<Map>();
		List<Map> badAttList = new ArrayList<Map>();
		Set<OWLClass> goodAtts = new HashSet<OWLClass>();
		//filter to include only resolvable classes
		for (OWLClass a : atts) {
			if (g.getOWLClass(a.getIRI()) == null) {
				badAttList.add(makeObj(a));
				LOG.info("Class "+a.toString()+" does not exist.  Tossing from profile calculation.");
			} else {
				alist.add(makeObj(a));
				goodAtts.add(a);
			}		
		}
		
		Map<String,Object> results = new HashMap<String,Object>();
		results.put("input",alist);
		results.put("score", makeAnnotationSufficiencyScore(goodAtts).get("score"));
		results.put("stats", this.makeIndividualSummaryStatistics(sos.computeAttributeSetSimilarityStats(goodAtts)));
		results.put("system_stats",this.makeSummaryStatistics());
		results.put("unresolvable_classes",badAttList);
		return results;
	}	
	
	/**
	 * Given a set of query classes that together make an annotation profile, 
	 * and a set of (optional) classes to root a subgraph calculation,
	 * this will deliver an overall annotation sufficiency score, along with 
	 * metadata about the classes used as input, and the system statistics as a whole.
	 * 
	 * @param atts classes in annotation profile
	 * @param nodes classes to root a categorical score calculation
	 * @return JSON object with the following format: <br />
	 *   input : [ list of resolvable classes, together with labels and IC scores], <br />
	 *   system_stats : min/max/sum/avg from {@link getSystemStats}, <br />
	 *   annotation_sufficiency : {@link makeAnnotationSufficiencyScore} <br />
	 *   unresolvable_classes : [ list of unresolvable classes ], <br />
	 *   categorical_scores : [ { id, label, score, n }... ], <br />
	 *   unresolvable_categories : [ list of unresolvable categories ] <br />
	 *   
	 * @throws UnknownOWLClassException
	 * @see makeAttributeInformationProfile
	 */
	private Map<String,Object> makeAttributeInformationProfileWithSubgraphScores(Set<OWLClass> atts, Set<OWLClass> nodes) throws UnknownOWLClassException {
		//TODO: add unmatched classes
		List<Map> badNodeList = new ArrayList<Map>();
		Set<OWLClass> goodNodes = new HashSet<OWLClass>();
		LOG.info("Attempting to compute subgraph scores with "+nodes.size()+" classes");
		nodes.remove(null);
		for (OWLClass n : nodes) {
			if (!(sos.getSourceOntology().getClassesInSignature().contains(n))) {
				badNodeList.add(makeObj(n));
				LOG.info("Class "+n.toString()+" does not exist.  Tossing from subgraph calculation.");
			} else {
				goodNodes.add(n);
			}
		}
		LOG.info("Now computing subgraph scores with "+goodNodes.size()+" available classes");

		List<Map> subgraphScores = new ArrayList<Map>();

		Map<String,Object> results = makeAttributeInformationProfile(atts);

		for (OWLClass n : goodNodes) {
			Map<String,Object> score = new HashMap<String,Object>();
			score.put("id", g.getIdentifier(n, true));
			score.put("label", g.getLabel(n));
			score.put("score", String.format("%1$.5f",sos.calculateSubgraphAnnotationSufficiencyForAttributeSet(atts,n)));
			SummaryStatistics stats = sos.computeAttributeSetSimilarityStatsForSubgraph(atts,n);
			//number of annotations in the query per category
			score.put("stats", this.makeIndividualSummaryStatistics(stats));
			score.put("system_stats",this.makeSummaryStatistics(n));
			subgraphScores.add(score);
		}
		results.put("categorical_scores", subgraphScores);
		results.put("unresolvable_categories", badNodeList);

		return results;
	}	
		
		

	public String getAttributeInformationProfile(Set<OWLClass> atts) throws UnknownOWLClassException {
		Gson gson = new Gson();
		return gson.toJson(this.makeAttributeInformationProfile(atts));
	}
	
	
	public String getAttributeInformationProfile(Set<OWLClass> atts, Set<OWLClass> nodes) throws UnknownOWLClassException {
		Gson gson = new Gson();
		if (nodes == null) {
			return gson.toJson(this.makeAttributeInformationProfile(atts));
		} else {
		return gson.toJson(this.makeAttributeInformationProfileWithSubgraphScores(atts, nodes));
		}
	}
	
	private ArrayList<HashMap<String,String>> makeCoAnnotationList(List<ClassCount> classCounts, int limit) {
		ArrayList<HashMap<String,String>> l = new ArrayList<HashMap<String,String>>();
		if (classCounts.size() < limit) {
			limit = classCounts.size();
		}
		for (int i=0; i<limit; i++) {
			HashMap<String,String> s = new HashMap<String,String>();
			s.put("id", g.getIdentifier(classCounts.get(i).c, true));
			s.put("label", g.getLabel(classCounts.get(i).c));
			s.put("score",String.format("%1$.5f",classCounts.get(i).score));
			l.add(s);
		}
		return l;
	}
	
	public String getCoAnnotationListForAttribute(OWLClass c, int limit) throws Exception {
		Gson gson = new Gson();
		List<ClassCount> classCounts = sos.getCoannotatedClassesForAttribute(c);
		return gson.toJson(makeCoAnnotationList(classCounts, limit));
	}



	//These are not ready for prime-time yet
	/*
	public String getCoAnnotationListForListAndAtts(List<ElementPairScores> matches, Set<OWLClass> atts) throws Exception {
		Gson gson = new Gson();
		return gson.toJson(sos.getCoAnnotatedClassesForMatches(matches, atts));
	}
	
	public String getCoAnnotationListForIndividual(OWLNamedIndividual i, int matchCutoff) throws Exception {
		Gson gson = new Gson();
		List<ClassCount> classCounts = sos.getCoAnnotatedClassesForIndividual(i, matchCutoff);		
		return gson.toJson(makeCoAnnotationList(classCounts));
	}
	
	public String getCoAnnotationListForAttributes(Set<OWLClass> atts, int matchCutoff) throws Exception {
		Gson gson = new Gson();
		List<ClassCount> classCounts = sos.getCoAnnotatedClassesForAttributes(atts, matchCutoff);
		return gson.toJson(makeCoAnnotationList(classCounts));
	} */
	
	
}
