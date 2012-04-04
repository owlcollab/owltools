package owltools.sim;

import static org.junit.Assert.*;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;
import owltools.sim.DescriptionTreeSimilarity;
import owltools.sim.MaximumInformationContentSimilarity;
import owltools.sim.SimEngine;
import owltools.sim.Similarity;

/**
 * tests getLeastCommonSubsumerSimpleClassExpression
 * 
 * @author cjm
 *
 */
public class PhenoSimTest extends AbstractSimEngineTest {

	OWLGraphWrapper g;
	SimEngine se;
	OWLPrettyPrinter pp;

	@Test
	public void testOrganismPair() throws Exception{
		g =  getOntologyWrapper("q-in-e.omn");
		se = new SimEngine(g);

		OWLObject hypoFL = g.getOWLObject("http://x.org#hypoplastic_forelimb");
		OWLObject hypoHL = g.getOWLObject("http://x.org#hypoplastic_hindlimb");

		showLCS(hypoFL, hypoHL, "ObjectIntersectionOf(hypoplastic ObjectSomeValuesFrom(inheres_in limb))");

		OWLObject hyperHand = g.getOWLObject("http://x.org#hyperplastic_hand");
		
		showLCS(hypoFL, hyperHand, 
				"ObjectIntersectionOf(abnormal_morphology ObjectSomeValuesFrom(inheres_in_part_of forelimb))");
		showLCS(hypoHL, hyperHand,
				"ObjectIntersectionOf(abnormal_morphology ObjectSomeValuesFrom(inheres_in_part_of limb))");

		OWLObject abHLB = g.getOWLObject("http://x.org#abnormal_hindlimb_bud_morphology");
		
		showLCS(hypoHL, abHLB, "ObjectIntersectionOf(abnormal_morphology ObjectSomeValuesFrom(related_to hindlimb_bud))");
		showLCS(hyperHand, abHLB, "ObjectIntersectionOf(abnormal_morphology ObjectSomeValuesFrom(related_to limb_bud))");

		OWLObject h1 = g.getOWLObject("http://x.org#h1");
		OWLObject m1 = g.getOWLObject("http://x.org#m1");

		showLCS(h1, m1);
		
		Similarity sim = 
			new MultiSimilarity();

		run(g, sim, h1, m1);

	}

	protected void showLCS(OWLObject a, OWLObject b) {
		showLCS(a,b,null);
	}
	
	protected void showLCS(OWLObject a, OWLObject b, String check) {
		pp = new OWLPrettyPrinter(g);
		//showClosure("a", a);
		//showClosure("b", b);
		OWLClassExpression lcs = se.getLeastCommonSubsumerSimpleClassExpression(a, b);	
		System.out.println("LCSx("+pp.render(a)+" -vs- "+pp.render(b)+") = "+pp.render(lcs));
		if (check != null) {
			//TODO
			assertTrue(pp.render(lcs).equals(check));
		}
		boolean okA = false;
		boolean okB = false;
		for (OWLObject d : g.queryDescendants(lcs)) {
			//System.out.println("   D:"+pp.render(d));
			if (d.equals(a)) 
				okA = true;
			if (d.equals(b)) 
				okB = true;
		}
		assertTrue(okA);
		assertTrue(okB);
	}

	private void showClosure(String label, OWLObject b) {
		for (OWLObject e : g.getSubsumersFromClosure(b)) {
			System.out.println("  "+label+": "+pp.render(e));
		}
		
	}





}
