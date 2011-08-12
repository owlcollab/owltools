package owltools.ontologyrelease.test;

import java.io.File;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;

import junit.framework.TestCase;

public class ReleaseInferenceTest extends TestCase {

	public static void testDescendants() throws Exception {
		OWLGraphWrapper  g =  getOntologyWrapper();
		OWLClass c = g.getOWLClass("http://purl.obolibrary.org/obo/NCBITaxon_10090");
		OWLObject i = g.getOWLObject("http://purl.obolibrary.org/obo/MGI_101761");
		System.out.println("Descendants of "+c);
		System.out.println("Expecting "+i);
		boolean ok = false;
		for (OWLGraphEdge e : g.getIncomingEdgesClosure(c)) {
			System.out.println("i:"+e);
			if (e.getSource().equals(i))
				ok = true;
		}

		assertTrue(ok);
	}
	public static void testDescendantsQuery() throws Exception {
		OWLGraphWrapper  g =  getOntologyWrapper();
		OWLClass c = g.getOWLClass("http://purl.obolibrary.org/obo/NCBITaxon_10090");
		OWLObject i = g.getOWLObject("http://purl.obolibrary.org/obo/MGI_101761");
		System.out.println("Expecting "+i);
		boolean ok = false;
		for (OWLObject e : g.queryDescendants(c)) {
			System.out.println("ORG:"+e);
			if (e.equals(i))
				ok = true;
		}
		assertTrue(ok);
	}
	public static void testAncestorsQuery() throws Exception {
		OWLGraphWrapper  g =  getOntologyWrapper();
		OWLClass c = g.getOWLClass("http://purl.obolibrary.org/obo/NCBITaxon_10090");
		OWLObject i = g.getOWLObject("http://purl.obolibrary.org/obo/MGI_101761");
		boolean ok = false;
		for (OWLGraphEdge e : g.getOutgoingEdgesClosureReflexive(i)) {
			System.out.println("i:"+e+" "+e.getTarget().getClass());
			if (e.getTarget().equals(c))
				ok = true;
		}
		assertTrue(ok);
	}



	private static OWLGraphWrapper getOntologyWrapper() throws OWLOntologyCreationException{
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		return new OWLGraphWrapper( 
				manager.loadOntologyFromOntologyDocument(
						new File("test_resources/test_phenotype.owl")));
	}
		
	
	
}
