package sim1;

import static org.junit.Assert.*;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;
import owltools.sim.SimEngine;

/**
 * tests getLeastCommonSubsumerSimpleClassExpression
 * 
 * @author cjm
 *
 */
public class LCSExpressionTest extends AbstractSimEngineTest {

	@Test
	public void testOrganismPair() throws Exception{
		OWLGraphWrapper  g =  getOntologyWrapper("lcstest3.owl");
		SimEngine se = new SimEngine(g);
		
		OWLObject a = g.getOWLObject("http://example.org#o1");
		OWLObject b = g.getOWLObject("http://example.org#o2");
		OWLPrettyPrinter pp = new OWLPrettyPrinter(g);
		
		OWLClassExpression lcs = se.getLeastCommonSubsumerSimpleClassExpression(a, b, true);	
		System.out.println("LCSx(o1,o2, true) = "+pp.render(lcs));
		
		lcs = se.getLeastCommonSubsumerSimpleClassExpression(a, b, false);		
		System.out.println("LCSx(o1,o2, false) = "+pp.render(lcs));
		
	}
	
	@Test
	public void testHippocampusLCS() throws Exception{
		OWLGraphWrapper  g =  getOntologyWrapper("lcstest3.owl");
		SimEngine se = new SimEngine(g);
		OWLPrettyPrinter pp = new OWLPrettyPrinter(g);
		
		OWLObject a = g.getOWLObject("http://example.org#axon_terminals_degenerated_in_ca2");
		OWLObject b = g.getOWLObject("http://example.org#axon_terminals_degenerated_in_ca3");
		
		System.out.println("Comparing = "+pp.render(a)+" -vs- "+pp.render(b));

		OWLClassExpression lcs = se.getLeastCommonSubsumerSimpleClassExpression(a, b, true);	
		System.out.println("LCSx(p1,p2, true) = "+pp.render(lcs));
		
		lcs = se.getLeastCommonSubsumerSimpleClassExpression(a, b, false);		
		System.out.println("LCSx(p1,p2, false) = "+pp.render(lcs));

	}	
	

	@Test
	public void testSimEq() throws Exception{
		OWLGraphWrapper  g =  getOntologyWrapper("lcstest3.owl");
		SimEngine se = new SimEngine(g);
		
		OWLObject a = g.getOWLObject("http://example.org#atrophied_hippocampus");
		OWLObject b = g.getOWLObject("http://example.org#atrophied_hippocampus");
		
		OWLClassExpression lcs = se.getLeastCommonSubsumerSimpleClassExpression(a, b);
		
		OWLPrettyPrinter pp = new OWLPrettyPrinter(g);
		System.out.println("Comparing = "+pp.render(a)+" -vs- "+pp.render(b));
		
		System.out.println("LCSx(p1,p2) = "+pp.render(lcs));
		assertEquals(lcs, a);
		//pp.print(lcs);
		//sa.print();
		
	}	


	
}
