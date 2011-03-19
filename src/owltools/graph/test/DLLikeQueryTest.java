package owltools.graph.test;

import java.io.File;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLRestriction;

import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLGraphWrapper.Config;

import junit.framework.TestCase;

public class DLLikeQueryTest extends TestCase {

	public static void testQuery() throws Exception {
		OWLGraphWrapper g = getOntologyWrapper();
		OWLClass obj = g.getOWLClass("http://example.org#probe_3");
		boolean ok = false;
		for (OWLObject e : g.queryDescendants(obj)) {
			System.out.println(e);
				ok = true;
		}
		assertTrue(g.queryDescendants(obj).size() == 3);
	}
	
	public static void testIntersectionsReturnedInClosure() throws Exception {
		OWLGraphWrapper g = getOntologyWrapper();
		OWLClass obj = g.getOWLClass("http://example.org#probe_4");
		boolean ok = false;
		for (OWLObject e : g.queryDescendants(obj)) {
			System.out.println(e);
				ok = true;
		}
		assertTrue(g.queryDescendants(obj).size() == 3);
	}
	
	public static void testRestrictionQuery() throws Exception {
		OWLGraphWrapper g = getOntologyWrapper();
		OWLClass c = g.getOWLClass("http://example.org#degenerated");
		OWLObjectProperty p = g.getOWLObjectProperty("http://example.org#has_quality");
		boolean ok = false;
		OWLObjectSomeValuesFrom obj = g.getDataFactory().getOWLObjectSomeValuesFrom(p, c);
		for (OWLObject x : g.queryDescendants(obj)) {
			System.out.println("R:"+x);
			ok = true;
		}
		assertTrue(g.queryDescendants(obj).size() == 5);
		assertTrue(ok);
	}

	public static void testRestrictionQuery2() throws Exception {
		OWLGraphWrapper g = getOntologyWrapper();
		OWLClass c = g.getOWLClass("http://example.org#axon_terminals_degenerated");
		OWLObjectProperty p = g.getOWLObjectProperty("http://example.org#has_part");
		boolean ok = false;
		OWLObjectSomeValuesFrom obj = g.getDataFactory().getOWLObjectSomeValuesFrom(p, c);
		
		for (OWLObject x : g.queryDescendants(obj)) {
			System.out.println("R2:"+x);
			ok = true;
		}
		assertTrue(ok);
		assertTrue(g.queryDescendants(obj).size() == 10);
	}




	private static OWLGraphWrapper getOntologyWrapper() throws OWLOntologyCreationException{
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		return new OWLGraphWrapper( 
				manager.loadOntologyFromOntologyDocument(
						new File("test_resources/lcstest2.owl")));
	}
		
	
	
}
