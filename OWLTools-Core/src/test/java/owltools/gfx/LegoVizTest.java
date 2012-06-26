package owltools.gfx;

import java.io.File;

import org.junit.Test;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class LegoVizTest extends OWLToolsTestBasics {

	@Test
	public void testRenderLego() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper g =
			pw.parseToOWLGraph(getResourceIRIString("NEDD4.owl"));
		OWLGraphLayoutRenderer r = new OWLGraphLayoutRenderer(g);
		r.addAllObjects();
		File folder = new File("out/"+getClass().getSimpleName());
		folder.mkdirs();
		r.renderHTML(folder);
	}
	
}
