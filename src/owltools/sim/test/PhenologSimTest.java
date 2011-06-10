package owltools.sim.test;

import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphWrapper;
import owltools.sim.DescriptionTreeSimilarity;
import owltools.sim.MaximumInformationContentSimilarity;
import owltools.sim.SimEngine;
import owltools.sim.Similarity;

public class PhenologSimTest extends AsbtractSimEngineTest {

	// TODO - takes a long time, add to separate test suite
	public static void testSim() throws Exception{
		OWLGraphWrapper  wrapper = 
			getOntologyWrapper("http://obo.svn.sourceforge.net/viewvc/obo/phenotype-commons/ontology/mp-hp-ext-merged-uberon.owl?revision=3905");
		DescriptionTreeSimilarity sa = 
			new DescriptionTreeSimilarity();
		OWLObject a = wrapper.getOWLObject("http://purl.obolibrary.org/obo/MP_0005391");
		OWLObject b = wrapper.getOWLObject("http://purl.obolibrary.org/obo/HP_0000478");
		SimEngine se = new SimEngine(wrapper);
		//sa.forceReflexivePropertyCreation = true;
		sa.calculate(se, b, a);
		sa.print();
		System.out.println(sa.getScore());
		
	}	

	
}
