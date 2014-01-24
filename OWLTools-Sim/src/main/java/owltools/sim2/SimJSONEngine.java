package owltools.sim2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphWrapper;
import owltools.sim2.OwlSim.ScoreAttributeSetPair;
import owltools.sim2.OwlSim.Stat;
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
			boolean isIgnoreUnknownClasses) throws UnknownOWLClassException {
		return search(objAs, targetIdSpace, isIgnoreUnknownClasses, 3000, false);
	}
	
	public String search(Set<OWLClass> objAs,
			String targetIdSpace,
			boolean isIgnoreUnknownClasses, boolean includeFullMatchingTriples) throws UnknownOWLClassException {
		return search(objAs, targetIdSpace, isIgnoreUnknownClasses, 3000, includeFullMatchingTriples);
	}

	/**
	 * @param objAs
	 * @param targetIdSpace
	 * @param isIgnoreUnknownClasses
	 * @param limit
	 * @return json string
	 * @throws UnknownOWLClassException
	 */
	public String search(Set<OWLClass> objAs,
			String targetIdSpace,
			boolean isIgnoreUnknownClasses,
			Integer limit) throws UnknownOWLClassException {
		return search(objAs, targetIdSpace, isIgnoreUnknownClasses, limit, false);
	}

	
	public String search(Set<OWLClass> objAs,
			String targetIdSpace,
			boolean isIgnoreUnknownClasses,
			Integer limit, boolean includeFullMatchingTriples) throws UnknownOWLClassException {
		Set<OWLClass> known = sos.getAllAttributeClasses();
		Set<OWLClass> filteredClasses = new HashSet<OWLClass>();

		Set<String> ids = new HashSet<String>();
		Set<String> idsUnresolved = new HashSet<String>();
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

		// todo use a gson writer
		Gson gson = new Gson();
		List<Map> matchObjs = new ArrayList<Map>();
		int n=0;
		for (ElementPairScores m : matches) {
			n++;
			if (limit != null && n > limit)
				break;
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
					if (cv != null) {
						if (cv.equals(dv)) {
							Map<String,Object> o;
							String objID;
							if (includeFullMatchingTriples) {
								objID = m.cs.get(ci).getIRI().getFragment();
								objID.concat(m.ds.get(di).getIRI().getFragment());
								objID.concat(cv.getArbitraryAttributeClass().getIRI().getFragment());
								o = makeLCSTriple(m.cs.get(ci),m.ds.get(di),cv.getArbitraryAttributeClass());
								//LOG.info("added triple: "+gson.toJson(o));
							} else {
								objID = cv.getArbitraryAttributeClass().getIRI().getFragment();
								o = makeObj(cv.getArbitraryAttributeClass());
							}
							matchingAtts.put(objID,o);
						}
					}
				}
			}
//			mObj.put("matches", matchingAtts);
			mObj.put("matches", matchingAtts.values());
			mObj.put("system_stats", makeSummaryStatistics());

			matchObjs.add(mObj);
		}
		Map payload = new HashMap();
		payload.put("query_IRIs", ids);
		payload.put("unresolved", idsUnresolved);
		payload.put("results", matchObjs);
		return gson.toJson(payload);
	}

	public List<Map> makeUniqAttSet(List<Map> list) {

		return list;
	}
	
	/**
	 * @param objAs
	 * @param objBs
	 * @return json
	 * @throws UnknownOWLClassException
	 */
	public String compareAttributeSetPair(Set<OWLClass> objAs, Set<OWLClass> objBs) throws UnknownOWLClassException {
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
			boolean isIgnoreUnknownClasses) throws UnknownOWLClassException {
		Gson gson = new Gson();

		Set<OWLClass> known = sos.getAllAttributeClasses();
		List<Map> pairs = new ArrayList<Map>();
		for (OWLClass objA : objAs) {
			if (!known.contains(objA)) {
				LOG.info("Unknown class: "+objA);
				if (isIgnoreUnknownClasses)
					continue;
				throw new UnknownOWLClassException(objA);
			}
			for (OWLClass objB : objBs) {
				if (!known.contains(objB)) {
					LOG.info("Unknown class: "+objB);
					if (isIgnoreUnknownClasses)
						continue;
					throw new UnknownOWLClassException(objB);
				}
				Map<String,Object> attPairMap = new HashMap<String,Object>();
				attPairMap.put("A", makeObj(objA));
				attPairMap.put("B", makeObj(objB));

				ScoreAttributeSetPair sap = sos.getLowestCommonSubsumerWithIC(objA, objB);
				if (sap == null) {
					//alternatively, could put the pair there, and add a note that
					//says it's below the threshold.. that would require a new pair
					//or return null for class/score
					LOG.info("PAIR NOT ADDED:"+attPairMap);
				} else {
					attPairMap.put("LCS", makeObj(sap.getArbitraryAttributeClass()));
					attPairMap.put("LCS_Score", sap.score);

					pairs.add(attPairMap);
					//LOG.info("ADDED PAIR:"+attPairMap);
				}

			}
		}

		return gson.toJson(pairs);
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
		m.put("IC",sos.getInformationContentForAttribute(g.getOWLClass(obj)));
		return m;
	}

	protected void makeObjAndSet(Map<String,Object> subj, String key, OWLObject t) throws UnknownOWLClassException {
		Map<String,Object> tobj = makeObj(t);
		if (tobj.keySet().size() > 0)
			subj.put(key, tobj);
	}

	public HashMap<String,String> makeSummaryStatistics() {
		//		Gson gson = new Gson();
		HashMap<String,String> stats = new HashMap<String,String>();
		stats.put("individuals", String.format("%d",sos.getSummaryStatistics(Stat.MEAN).getN()));		
		stats.put("mean(meanIC)", String.format("%1$.5f",sos.getSummaryStatistics(Stat.MEAN).getMean()));		
		stats.put("mean(maxIC)", String.format("%1$.5f",sos.getSummaryStatistics(Stat.MAX).getMean()));		
		stats.put("max(maxIC)", String.format("%1$.5f",sos.getSummaryStatistics(Stat.MAX).getMax()));		
		stats.put("max(sumIC)", String.format("%1$.5f",sos.getSummaryStatistics(Stat.SUM).getMax()));		
		stats.put("mean(sumIC)", String.format("%1$.5f",sos.getSummaryStatistics(Stat.SUM).getMean()));		
		stats.put("mean(n)", String.format("%1$.5f",sos.getSummaryStatistics(Stat.N).getMean()));		

		//		return gson.toJson(stats);
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

	private Map<String,Object> makeAnnotationSufficiencyScore(Set<OWLClass> atts) throws UnknownOWLClassException {
		HashMap<String,String> s = new HashMap<String,String>();
		double score = sos.calculateOverallAnnotationSufficiencyForAttributeSet(atts);
		s.put("score",String.format("%1$.5f",score));
		Map<String,Object> annotation_sufficiency = new HashMap<String,Object>();
		annotation_sufficiency.put("annotation_sufficiency",s);
		return annotation_sufficiency;
	}

	private Map<String,Object> makeAttributeInformationProfile(Set<OWLClass> atts) throws UnknownOWLClassException {
		List<Map> alist = new ArrayList<Map>();
		for (OWLClass a : atts) {
			alist.add(makeObj(a));
		}
		//TODO: add unmatched classes
		Map<String,Object> results = new HashMap<String,Object>();
		results.put("input",alist);
		results.put("system_stats",this.makeSummaryStatistics());
		return results;
	}	

	public String getAttributeInformationProfile(Set<OWLClass> atts) throws UnknownOWLClassException {
		Gson gson = new Gson();
		return gson.toJson(this.makeAttributeInformationProfile(atts));
	}
}
