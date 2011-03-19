package owltools.sim.test;

import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphWrapper;
import owltools.sim.ConjunctiveSetSimilarity;
import owltools.sim.DescriptionTreeSimilarity;
import owltools.sim.MaximumInformationContentSimilarity;
import owltools.sim.SimEngine;
import owltools.sim.Similarity;

public class CombinedJaccardConjunctiveSetSimilarityTest extends AsbtractSimEngineTest {

	public static void testSim() throws Exception{
		OWLGraphWrapper  wrapper =  getOntologyWrapper("file:test_resources/lcstest3.owl");
		ConjunctiveSetSimilarity sa = 
			new ConjunctiveSetSimilarity();
		OWLObject a = wrapper.getOWLObject("http://example.org#axon_terminals_degenerated_in_ca2");
		OWLObject b = wrapper.getOWLObject("http://example.org#axon_terminals_degenerated_in_ca3");
		SimEngine se = new SimEngine(wrapper);
		sa.calculate(se, b, a);
		sa.print();
		
	}	

	
}
