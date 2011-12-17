package owltools.gfx;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class DrawAllCAROTest extends OWLToolsTestBasics {

	@Test
	public void testRenderCARO() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException {
		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper g =
			pw.parseToOWLGraph(getResourceIRIString("caro.obo"));
		OWLGraphLayoutRenderer r = new OWLGraphLayoutRenderer(g);
		r.addAllObjects();
		File folder = new File("out/"+getClass().getSimpleName());
		folder.mkdirs();
		r.renderHTML(folder);
	}
	
}
