package sim1;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphWrapper;
import owltools.sim.MaximumInformationContentSimilarity;
import owltools.sim.Similarity;

/**
 * Tests MaxIC using 
 * 
 * @author cjm
 *
 */
public class MaxInfContentOnCaroTest extends AbstractSimEngineTest {

	@Test
	public void testCAROPair() throws Exception{
		OWLGraphWrapper  wrapper = getOntologyWrapper("caro.owl");
		Similarity sa = 
			new MaximumInformationContentSimilarity();
		OWLObject a = wrapper.getOWLObjectByIdentifier("CARO:0000066"); // epithelium
		OWLObject b = wrapper.getOWLObjectByIdentifier("CARO:0000040"); // acellular anatomical structure
		run(wrapper,sa,a,b);
	}	
	
	@Test
	public void testAllPairs() throws Exception{
		OWLGraphWrapper  wrapper = getOntologyWrapper("caro.owl");
		Similarity sa = 
			new MaximumInformationContentSimilarity();
		for (OWLObject a : wrapper.getAllOWLObjects()) {
			if (!(a instanceof OWLClass))
				continue;
			for (OWLObject b : wrapper.getAllOWLObjects()) {
				if (!(b instanceof OWLClass))
					continue;
				run(wrapper,sa,a,b);
			}
		}
	}	
	
}
