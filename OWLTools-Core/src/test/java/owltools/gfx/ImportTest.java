package owltools.gfx;

import static junit.framework.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import owltools.OWLToolsTestBasics;
import owltools.gfx.OWLGraphLayoutRenderer;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class ImportTest extends OWLToolsTestBasics {

	@Test
	public void testRenderCARO() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException {
		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper g =
			pw.parseToOWLGraph(getResourceIRIString("import_caro.owl"));
		OWLObject ob = g.getOWLObjectByIdentifier("CARO:0000000");
		assertTrue(ob != null);
		OWLGraphLayoutRenderer r = new OWLGraphLayoutRenderer(g);
		r.addAllObjects();
		File folder = new File("out/"+getClass().getSimpleName());
		folder.mkdirs();
		r.renderHTML(folder);
		for (OWLAxiom ax : g.getSourceOntology().getAxioms()) {
			System.out.println("AX:"+ax);
		}
		boolean ok = false;
		for (OWLObject x : g.getAllOWLObjects()) {
			System.out.println("x="+x);
			if ("foo".equals(g.getLabel(x))) {
				ok = true;
			}
		}
		assertTrue(ok);
	}
	
}
