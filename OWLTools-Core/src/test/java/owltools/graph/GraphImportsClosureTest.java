package owltools.graph;

import static org.junit.Assert.*;

import org.junit.Ignore;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;

public class GraphImportsClosureTest extends OWLToolsTestBasics {

	@Test
	@Ignore("Disabling test due to lack of resources to debug")
	public void testClosure() throws Exception {
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

	@Test
	public void testClosureIntra() throws Exception {
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

	@SuppressWarnings("deprecation")
	private OWLGraphWrapper getOntologyWrapper(boolean imp) throws OWLOntologyCreationException{
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology ontology = manager.loadOntologyFromOntologyDocument(getResource("caro_mireot_test.owl"));
		return new OWLGraphWrapper(ontology, imp);
	}
	
}
