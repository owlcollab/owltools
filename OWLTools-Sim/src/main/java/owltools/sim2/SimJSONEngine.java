package owltools.sim2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphWrapper;
import owltools.sim2.OwlSim.ScoreAttributeSetPair;
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
		Set<OWLClass> known = sos.getAllAttributeClasses();
		Set<OWLClass> filteredClasses = new HashSet<OWLClass>();
		
		Set<String> ids = new HashSet<String>();
		for (OWLClass objA : objAs) {
			if (!known.contains(objA)) {
				if (isIgnoreUnknownClasses)
					continue;
				throw new UnknownOWLClassException(objA);
			}
			filteredClasses.add(objA);
			ids.add(objA.getIRI().toString());
		}
		LOG.info("Finding matches for :"+filteredClasses);
		LOG.info("OwlSim = "+sos);
		List<ElementPairScores> matches = sos.findMatches(filteredClasses, targetIdSpace);
		
		// todo use a gson writer
		Gson gson = new Gson();
		List<Map> matchObjs = new ArrayList<Map>();
		for (ElementPairScores m : matches) {
			Map mObj = new HashMap();
			makeObjAndSet(mObj, "i", m.i);
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
			matchObjs.add(mObj);
		}
		Map payload = new HashMap();
		payload.put("query_IRIs", ids);
		payload.put("results", matchObjs);
		return gson.toJson(payload);
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

	protected Map<String,Object> makeObj(OWLObject obj) {
		Map<String,Object> m = new HashMap<String,Object>();
		m.put("id", g.getIdentifier(obj));
		m.put("label", g.getLabel(obj));
		return m;
	}
	
	protected void makeObjAndSet(Map<String,Object> subj, String key, OWLObject t) {
		Map<String,Object> tobj = makeObj(t);
		if (tobj.keySet().size() > 0)
			subj.put(key, tobj);
	}

}
