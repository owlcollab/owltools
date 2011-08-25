package owltools.gfx.test;

import java.io.IOException;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import owltools.gfx.OWLGraphLayoutRenderer;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;
import owltools.test.OWLToolsTestBasics;

public class DrawCAROTest extends OWLToolsTestBasics {

	@Test
	public void testRenderCARO() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException {
		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper g =
			pw.parseToOWLGraph(getResourceIRIString("caro.obo"));
		OWLGraphLayoutRenderer r = new OWLGraphLayoutRenderer(g);
		OWLObject ob = g.getOWLObjectByIdentifier("CARO:0000070");
		System.out.println("drawing: "+ob);
		r.addObject(ob);
		r.renderHTML();
	}
	
}
