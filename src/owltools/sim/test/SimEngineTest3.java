package owltools.sim.test;

import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphWrapper;
import owltools.sim.DescriptionTreeSimilarity;
import owltools.sim.MaximumInformationContentSimilarity;
import owltools.sim.SimEngine;
import owltools.sim.Similarity;

public class SimEngineTest3 extends AsbtractSimEngineTest {

	public static void testSim() throws Exception{
		OWLGraphWrapper  wrapper =  getOntologyWrapper("file:test_resources/lcstest2.owl");
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
