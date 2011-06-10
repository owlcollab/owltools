package owltools.sim.test;

import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphWrapper;
import owltools.sim.MaximumInformationContentSimilarity;
import owltools.sim.Similarity;

public class SimEngineTest2 extends AsbtractSimEngineTest {

	public static void testCARO() throws Exception{
		OWLGraphWrapper  wrapper =  getOntologyWrapper("file:test_resources/lcstest1.owl");
		Similarity sa = 
			new MaximumInformationContentSimilarity();
		OWLObject a = wrapper.getOWLObjectByIdentifier("http://example.org#o1");
		OWLObject b = wrapper.getOWLObjectByIdentifier("http://example.org#o2");
		run(wrapper,sa,a,b);
		
	}	

	
}
