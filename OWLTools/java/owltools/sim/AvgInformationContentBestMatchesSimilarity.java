package owltools.sim;

import java.util.HashMap;
import java.io.OutputStream;
import java.io.PrintStream;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.sim.SimEngine.SimilarityAlgorithmException;

@Deprecated
public class AvgInformationContentBestMatchesSimilarity extends Similarity {
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
				//lcss = getLeastCommonSubsumers(m, pa, pb);
				// todo - cache this for symmetric results
				Similarity mic = new MaximumInformationContentSimilarity();
				simEngine.calculateSimilarity(mic, pa, pb);
				simEngine.getBestMatch(bestMap, pa, mic);
				simEngine.getBestMatch(bestMap, pb, mic);
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
			s.println(matchMap.get(x).toString());
		}
		
	}

	@Override
	protected void translateResultsToOWLAxioms(String id,
			OWLNamedIndividual result, Set<OWLAxiom> axioms) {
		// TODO Auto-generated method stub
		
	}
}
