package owltools.graph.test;

import java.io.File;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;

import junit.framework.TestCase;

public class GraphImportsClosureTest extends TestCase {

	public static void testClosure() throws Exception {
		OWLGraphWrapper  g =  getOntologyWrapper(true);
		OWLObject obj = g.getOWLObjectByIdentifier("X:1");
		OWLObject root = g.getOWLObjectByIdentifier("CARO:0000000");
		boolean ok = false;
		for (OWLGraphEdge e : g.getOutgoingEdgesClosureReflexive(obj)) {
			System.out.println("with imports:"+e);
			if (e.getTarget().equals(root))
				ok = true;
		}
		assertTrue(ok);
	}


	public static void testClosureIntra() throws Exception {
		OWLGraphWrapper  g =  getOntologyWrapper(false);
		OWLObject obj = g.getOWLObjectByIdentifier("X:1");
		OWLObject root = g.getOWLObjectByIdentifier("CARO:0000000");
		boolean ok = true;
		for (OWLGraphEdge e : g.getOutgoingEdgesClosureReflexive(obj)) {
			System.out.println("no imports:"+e);
			if (e.getTarget().equals(root))
				ok = false;
		}
		assertTrue(ok);
	}

	private static OWLGraphWrapper getOntologyWrapper(boolean imp) throws OWLOntologyCreationException{
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		return new OWLGraphWrapper( 
				manager.loadOntologyFromOntologyDocument(
						new File("test_resources/caro_mireot_test.owl")),
						imp);
	}
		
	
	
}
