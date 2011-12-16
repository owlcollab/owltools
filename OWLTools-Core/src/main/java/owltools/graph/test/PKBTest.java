package owltools.graph.test;

import static junit.framework.Assert.*;

import java.io.IOException;
import java.util.Set;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;
import owltools.test.OWLToolsTestBasics;

public class PKBTest extends OWLToolsTestBasics {

	@Test
	public void testAnc() throws Exception {
		OWLGraphWrapper  g =  getOntologyWrapper();
		OWLObject obj = g.getOWLObject("http://ccdb.ucsd.edu/PKB/1.0/PKB.owl#PATO_0001555_251");
		Set<OWLObject> ancs = g.getAncestorsReflexive(obj);
		for (OWLObject a : ancs) {
			System.out.println(a);
			for (OWLGraphEdge e : g.getEdgesBetween(obj, a)) {
				System.out.println("  EL"+e);
			}
		}
		assertTrue(true);
	}

	private OWLGraphWrapper getOntologyWrapper() throws OWLOntologyCreationException, IOException{
		ParserWrapper pw = new ParserWrapper();
		OWLOntology ont = pw.parse(getResourceIRIString("PKB_all.owl"));
		return new OWLGraphWrapper(ont);
	}
	
}
