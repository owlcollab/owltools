package owltools.ontologyrelease;

import static org.junit.Assert.*;

import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;

public class AncestorsTest extends OWLToolsTestBasics {

	@Test
	public void testDescendants() throws Exception {
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
	
	@Test
	public void testDescendantsQuery() throws Exception {
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
	
	@Test
	public void testAncestorsQuery() throws Exception {
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



	private OWLGraphWrapper getOntologyWrapper() throws OWLOntologyCreationException{
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology ontology = manager.loadOntologyFromOntologyDocument(getResource("test_phenotype.owl"));
		return new OWLGraphWrapper(ontology);
	}
		
	
	
}
