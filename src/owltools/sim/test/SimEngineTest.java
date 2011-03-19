package owltools.sim.test;

import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphWrapper;
import owltools.sim.MaximumInformationContentSimilarity;
import owltools.sim.Similarity;

public class SimEngineTest extends AsbtractSimEngineTest {

	public static void testCARO() throws Exception{
		OWLGraphWrapper  wrapper =  getOntologyWrapper("file:test_resources/caro.owl");
		Similarity sa = 
			new MaximumInformationContentSimilarity();
		OWLObject a = wrapper.getOWLObjectByIdentifier("CARO:0000066");
		OWLObject b = wrapper.getOWLObjectByIdentifier("CARO:0000040");
		run(wrapper,sa,a,b);
		
	}	
	
	
}
