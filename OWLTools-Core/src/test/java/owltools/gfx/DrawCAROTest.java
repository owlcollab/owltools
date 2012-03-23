package owltools.gfx;

import java.io.File;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLObject;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class DrawCAROTest extends OWLToolsTestBasics {

	@Test
	public void testRenderCARO() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper g =
			pw.parseToOWLGraph(getResourceIRIString("caro.obo"));
		OWLGraphLayoutRenderer r = new OWLGraphLayoutRenderer(g);
		OWLObject ob = g.getOWLObjectByIdentifier("CARO:0000070");
		System.out.println("drawing: "+ob);
		r.addObject(ob);
		File folder = new File("out/"+getClass().getSimpleName());
		folder.mkdirs();
		r.renderHTML(folder);
	}
	
}
