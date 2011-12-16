package owltools.sim;

import java.util.HashMap;
import java.io.OutputStream;
import java.io.PrintStream;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.sim.SimEngine.SimilarityAlgorithmException;

/**
 * use MultiSimilarity with a subSim method of MaxIC instead
 * 
 * @author cjm
 *
 */
@Deprecated
public class AvgInformationContentLCSSimilarity extends Similarity {
	Map <OWLObject,Similarity> matchMap = new HashMap<OWLObject,Similarity>();
	Set <OWLObject> aset;
	Set <OWLObject> bset;

	@Override
	public void calculate(SimEngine simEngine, OWLObject a, OWLObject b) throws SimilarityAlgorithmException {
		OWLGraphWrapper graph = simEngine.getGraph();
		Map <OWLObject,Similarity> bestMap = new HashMap<OWLObject,Similarity>();
		aset = new HashSet<OWLObject>();
		bset = new HashSet<OWLObject>();

		for (OWLGraphEdge ea : graph.getOutgoingEdges(a)) {
			OWLObject pa = ea.getTarget();
			aset.add(pa);
			for (OWLGraphEdge eb : graph.getOutgoingEdges(b)) {
				OWLObject pb = eb.getTarget();
				bset.add(pb);
				//if (pa.compareTo(pb) < 0)
				//	continue;
				Set<OWLObject> lcss = simEngine.getLeastCommonSubsumers(pa, pb);
				Set<OWLObject> lcsxs = new HashSet<OWLObject>();
				for (OWLObject lcs : lcss) {
					OWLObject lcsx = simEngine.createUnionExpression(pa,pb,lcs);
					lcsxs.add(lcsx);
					System.out.println("  LCSX="+lcsx);
				}
				// todo - cache this for symmetric results
				MaximumInformationContentSimilarity mic = new MaximumInformationContentSimilarity();
				simEngine.calculateSimilarity(mic, pa, pb);
				mic.bestSubsumers = lcsxs; // treat all LCSs as best
				getBestMatch(bestMap, pa, mic);
				getBestMatch(bestMap, pb, mic);
			}
		}
		int n = 0;
		double sum = 0;
		for (OWLObject pa : aset) {
			sum += bestMap.get(pa).score;
			n++;
		}
		for (OWLObject pb : bset) {
			sum += bestMap.get(pb).score;
			n++;
		}
		setScore(sum/n);
		matchMap = bestMap;

	}

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



	public void print(PrintStream s) {
		s.println("Matches A");
		print(s, aset);
		s.println("Matches B");
		print(s, bset);
		s.println("Score: "+score);
	}

	private void print(PrintStream s, Set<OWLObject> oset) {
		for (OWLObject x : oset) {
			s.print(x);
			s.print(" ");
			Similarity sim = matchMap.get(x);
			s.print(sim.a+" -vs- "+sim.b+" "); // TODO -- one way
			s.println(sim.toString());
		}

	}

	@Override
	protected void translateResultsToOWLAxioms(String id,
			OWLNamedIndividual result, Set<OWLAxiom> axioms) {
		// TODO Auto-generated method stub
		
	}
}
