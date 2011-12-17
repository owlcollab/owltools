package owltools.sim;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;
import owltools.sim.DescriptionTreeSimilarity;
import owltools.sim.MaximumInformationContentSimilarity;
import owltools.sim.SimEngine;
import owltools.sim.Similarity;

public class SimEngine3Test extends AbstractSimEngineTest {

	@Test
	public void testSim() throws Exception{
		OWLGraphWrapper  g =  getOntologyWrapper("lcstest3.owl");
		SimEngine se = new SimEngine(g);
		
		OWLObject a = g.getOWLObject("http://example.org#o1");
		OWLObject b = g.getOWLObject("http://example.org#o2");
		
		OWLClassExpression lcs = se.getLeastCommonSubsumerSimpleClassExpression(a, b);
		
		OWLPrettyPrinter pp = new OWLPrettyPrinter(g);
		System.out.println(lcs);
		//pp.print(lcs);
		//sa.print();
		
	}
	
	@Test
	public void testHippocampusLCS() throws Exception{
		OWLGraphWrapper  g =  getOntologyWrapper("lcstest3.owl");
		SimEngine se = new SimEngine(g);
		
		OWLObject a = g.getOWLObject("http://example.org#axon_terminals_degenerated_in_ca2");
		OWLObject b = g.getOWLObject("http://example.org#axon_terminals_degenerated_in_ca3");
		
		OWLClassExpression lcs = se.getLeastCommonSubsumerSimpleClassExpression(a, b);
		
		OWLPrettyPrinter pp = new OWLPrettyPrinter(g);
		System.out.println("LCS:"+lcs);
		//pp.print(lcs);
		//sa.print();
		
	}	

	@Test
	public void testSimEq() throws Exception{
		OWLGraphWrapper  g =  getOntologyWrapper("lcstest3.owl");
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
