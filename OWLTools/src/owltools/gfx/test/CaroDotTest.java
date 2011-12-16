package owltools.gfx.test;

import java.io.IOException;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import owltools.gfx.OWLGraphvizRenderer;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;
import owltools.test.OWLToolsTestBasics;

public class CaroDotTest extends OWLToolsTestBasics {

	@Test
	public void testRenderCARO() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException {
		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper g =
			pw.parseToOWLGraph(getResourceIRIString("caro.obo"));
		OWLGraphvizRenderer r = new OWLGraphvizRenderer(g);
		r.addAllObjects();
		System.out.println(r.renderDot());
	}
	
}
