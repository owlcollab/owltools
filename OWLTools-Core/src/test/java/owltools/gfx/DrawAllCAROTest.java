package owltools.gfx;

import java.io.File;

import org.junit.Test;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class DrawAllCAROTest extends OWLToolsTestBasics {

	@Test
	public void testRenderCARO() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper g =
			pw.parseToOWLGraph(getResourceIRIString("caro.obo"));
		OWLGraphLayoutRenderer r = new OWLGraphLayoutRenderer(g);
		r.addAllClasses();
		File folder = new File("out/"+getClass().getSimpleName());
		folder.mkdirs();
		r.renderHTML(folder);
	}
	
}
