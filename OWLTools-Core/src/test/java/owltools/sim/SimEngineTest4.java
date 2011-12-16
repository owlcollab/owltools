package owltools.sim;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphWrapper;
import owltools.sim.DescriptionTreeSimilarity;
import owltools.sim.SimEngine;

public class SimEngineTest4 extends AsbtractSimEngineTest {

	@Test
	public void testSim() throws Exception{
		OWLGraphWrapper  wrapper =  getOntologyWrapper("lcstest2.owl");
		DescriptionTreeSimilarity sa = 
			new DescriptionTreeSimilarity();
		OWLObject a = wrapper.getOWLObject("http://example.org#o1");
		OWLObject b = wrapper.getOWLObject("http://example.org#o2");
		SimEngine se = new SimEngine(wrapper);
		sa.forceReflexivePropertyCreation = true;
		sa.calculate(se, b, a);
		sa.print();
		
	}	
	
}
