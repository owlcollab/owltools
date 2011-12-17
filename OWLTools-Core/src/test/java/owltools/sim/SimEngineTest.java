package owltools.sim;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphWrapper;
import owltools.sim.MaximumInformationContentSimilarity;
import owltools.sim.Similarity;

public class SimEngineTest extends AbstractSimEngineTest {

	@Test
	public void testCARO() throws Exception{
		OWLGraphWrapper  wrapper = getOntologyWrapper("caro.owl");
		Similarity sa = 
			new MaximumInformationContentSimilarity();
		OWLObject a = wrapper.getOWLObjectByIdentifier("CARO:0000066");
		OWLObject b = wrapper.getOWLObjectByIdentifier("CARO:0000040");
		run(wrapper,sa,a,b);
	}	
	
}
