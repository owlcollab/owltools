package owltools.gfx.test;

import static junit.framework.Assert.*;

import java.io.IOException;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import owltools.gfx.OWLGraphLayoutRenderer;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;
import owltools.test.OWLToolsTestBasics;

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
		r.renderHTML();
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
