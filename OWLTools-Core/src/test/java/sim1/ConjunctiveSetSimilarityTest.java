package sim1;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphWrapper;
import owltools.sim.ConjunctiveSetSimilarity;
import owltools.sim.SimEngine;

/**
 * tests ConjunctiveSetSimilarity
 * 
 * @author cjm
 *
 */
public class ConjunctiveSetSimilarityTest extends AbstractSimEngineTest {

	@Test
	public void testSim() throws Exception{
		OWLGraphWrapper  wrapper =  getOntologyWrapper("lcstest3.owl");
		ConjunctiveSetSimilarity sa = new ConjunctiveSetSimilarity();
		OWLObject a = wrapper.getOWLObject("http://example.org#axon_terminals_degenerated_in_ca2");
		OWLObject b = wrapper.getOWLObject("http://example.org#axon_terminals_degenerated_in_ca3");
		SimEngine se = new SimEngine(wrapper);
		sa.calculate(se, b, a);
		sa.print();
		
	}	

	
}
