package owltools.mooncat;

import static org.junit.Assert.*;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObject;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class RemoveAxiomsAboutTest extends OWLToolsTestBasics {
	

	@Test
	public void testRemove() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		
		// this test ontology has a class defined using a caro class, and imports caro_local
		OWLGraphWrapper g =
			pw.parseToOWLGraph(getResourceIRIString("mooncat/multiont.obo"));
		Mooncat m = new Mooncat(g);
		
		m.removeAxiomsAboutIdSpace("Z", true);
		
		int nErr = 0;
		for (OWLAxiom a : g.getSourceOntology().getAxioms()) {
		    //System.out.println("STAGE1: "+a);
		    if (a.toString().contains("/Z_")) {
		        nErr++;
		    }
		}
		assertEquals(0, nErr);

	      m.removeAxiomsAboutIdSpace("Y", false);

        for (OWLAxiom a : g.getSourceOntology().getAxioms()) {
            //System.out.println("STAGE2: "+a);
            if (a.toString().contains("/Z_")) {
                nErr++;
            }
        }
        assertEquals(0, nErr);
		
	}
	
}
