package owltools.sim2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphWrapper;
import owltools.sim2.OwlSim.ScoreAttributeSetPair;
import owltools.sim2.SimpleOwlSim.ScoreAttributePair;

import com.google.gson.Gson;

public class SimJSONEngine {

	private Logger LOG = Logger.getLogger(SimJSONEngine.class);

	OwlSim sos;
	OWLGraphWrapper g;

	public SimJSONEngine(OWLGraphWrapper g, OwlSim sos2) {
		this.sos = sos2;
		this.g = g;
	}

	public String compareAttributeSetPair(Set<OWLClass> objAs, Set<OWLClass> objBs) throws UnknownOWLClassException {
		Gson gson = new Gson();

		List<Map> pairs = new ArrayList<Map>();
		for (OWLClass objA : objAs) {
			for (OWLClass objB : objBs) {
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

	public Map<String,Object> makeObj(OWLObject obj) {
		Map<String,Object> m = new HashMap<String,Object>();
		m.put("id", g.getIdentifier(obj));
		m.put("label", g.getLabel(obj));
		return m;
	}

}
