package owltools.graph;

import static junit.framework.Assert.*;

import java.util.Set;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.OWLToolsTestBasics;
import owltools.io.ParserWrapper;

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

	private OWLGraphWrapper getOntologyWrapper() throws Exception{
		ParserWrapper pw = new ParserWrapper();
		OWLOntology ont = pw.parse(getResourceIRIString("PKB_all.owl"));
		return new OWLGraphWrapper(ont);
	}
	
}
