package sim1;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphWrapper;
import owltools.sim.CombinedJaccardConjunctiveSetSimilarity;
import owltools.sim.ConjunctiveSetSimilarity;
import owltools.sim.SimEngine;

public class CombinedJaccardConjunctiveSetSimilarityTest extends AbstractSimEngineTest {

	@Test
	public void testSim() throws Exception{
		OWLGraphWrapper  wrapper =  getOntologyWrapper("lcstest3.owl");
		CombinedJaccardConjunctiveSetSimilarity sa = new CombinedJaccardConjunctiveSetSimilarity();
		OWLObject a = wrapper.getOWLObject("http://example.org#axon_terminals_degenerated_in_ca2");
		OWLObject b = wrapper.getOWLObject("http://example.org#axon_terminals_degenerated_in_ca3");
		SimEngine se = new SimEngine(wrapper);
		sa.calculate(se, a, b);
		sa.print();
	}	
	
}
