package owltools.graph.test;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

import junit.framework.TestCase;

public class PKBTest extends TestCase {

	public static void testAnc() throws Exception {
		OWLGraphWrapper  g =  getOntologyWrapper(true);
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



	private static OWLGraphWrapper getOntologyWrapper(boolean imp) throws OWLOntologyCreationException, IOException{
		ParserWrapper pw = new ParserWrapper();
		OWLOntology ont = pw.parse("file:test_resources/PKB_all.owl");
		return new OWLGraphWrapper(ont);
	}
		
	
	
}
