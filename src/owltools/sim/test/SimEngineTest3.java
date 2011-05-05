package owltools.sim.test;

import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;
import owltools.sim.DescriptionTreeSimilarity;
import owltools.sim.MaximumInformationContentSimilarity;
import owltools.sim.SimEngine;
import owltools.sim.Similarity;

public class SimEngineTest3 extends AsbtractSimEngineTest {

	public static void testSim() throws Exception{
		OWLGraphWrapper  g =  getOntologyWrapper("file:test_resources/lcstest3.owl");
		SimEngine se = new SimEngine(g);
		
		OWLObject a = g.getOWLObject("http://example.org#o1");
		OWLObject b = g.getOWLObject("http://example.org#o2");
		
		OWLClassExpression lcs = se.getLeastCommonSubsumerSimpleClassExpression(a, b);
		
		OWLPrettyPrinter pp = new OWLPrettyPrinter(g);
		System.out.println(lcs);
		//pp.print(lcs);
		//sa.print();
		
	}	
	
	public static void testSimEq() throws Exception{
		OWLGraphWrapper  g =  getOntologyWrapper("file:test_resources/lcstest3.owl");
		SimEngine se = new SimEngine(g);
		
		OWLObject a = g.getOWLObject("http://example.org#atrophied_hippocampus");
		OWLObject b = g.getOWLObject("http://example.org#atrophied_hippocampus");
		
		OWLClassExpression lcs = se.getLeastCommonSubsumerSimpleClassExpression(a, b);
		
		OWLPrettyPrinter pp = new OWLPrettyPrinter(g);
		System.out.println(lcs);
		//pp.print(lcs);
		//sa.print();
		
	}	


	
}
