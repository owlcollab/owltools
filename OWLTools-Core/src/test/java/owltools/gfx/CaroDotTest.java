package owltools.gfx;

import org.junit.Test;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class CaroDotTest extends OWLToolsTestBasics {

	@Test
	public void testRenderCARO() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper g =
			pw.parseToOWLGraph(getResourceIRIString("caro.obo"));
		OWLGraphvizRenderer r = new OWLGraphvizRenderer(g);
		r.addAllObjects();
		System.out.println(r.renderDot());
	}
	
}
