package owltools.sim;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphWrapper;
import owltools.sim.MaximumInformationContentSimilarity;
import owltools.sim.Similarity;

public class SimEngineTest2 extends AsbtractSimEngineTest {

	@Test
	public void testCARO() throws Exception{
		OWLGraphWrapper  wrapper =  getOntologyWrapper("lcstest1.owl");
		Similarity sa = 
			new MaximumInformationContentSimilarity();
		OWLObject a = wrapper.getOWLObjectByIdentifier("http://example.org#o1");
		OWLObject b = wrapper.getOWLObjectByIdentifier("http://example.org#o2");
		run(wrapper,sa,a,b);
	}	
	
}
