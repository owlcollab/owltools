package owltools.gfx;

import java.io.IOException;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import owltools.OWLToolsTestBasics;
import owltools.gfx.OWLGraphLayoutRenderer;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class MPVizTest extends OWLToolsTestBasics {

	@Test
	public void testRenderCARO() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException {
		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper g =
			pw.parseToOWLGraph("http://purl.org/obo/obo/MP.obo");
		OWLGraphLayoutRenderer r = new OWLGraphLayoutRenderer(g);
		OWLObject ob = g.getOWLObjectByIdentifier("MP:0001293");
		System.out.println("drawing: "+ob);
		r.addObject(ob);
		r.renderHTML();
	}
	
}
