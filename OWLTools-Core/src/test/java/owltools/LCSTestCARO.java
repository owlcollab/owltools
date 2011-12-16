package owltools;

import static junit.framework.Assert.*;

import java.io.IOException;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class LCSTestCARO extends OWLToolsTestBasics {

	@Test
	public void testConvertXPs() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException {
		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper g =
			pw.parseToOWLGraph(getResourceIRIString("lcstest1.owl"));
		OWLOntology ont = g.getSourceOntology();
		OWLObject o2 = g.getOWLObject("http://example.org#o2");
		
		System.out.println("getting ancestors for: "+o2);
		for (OWLGraphEdge e : g.getOutgoingEdgesClosure(o2)) {
			System.out.println(e);
		}
		for (OWLClass c : ont.getClassesInSignature()) {
			System.out.println("getting individuals for "+c+" "+g.getLabel(c));
			for (OWLIndividual i : g.getInstancesFromClosure(c)) {
				System.out.println("  "+i);
			}
		}

		/*
		for (OWLClass c : ont.getClassesInSignature()) {
			System.out.println("c="+c+" "+g.getLabel(c));
			for (OWLGraphEdge e : g.getOutgoingEdgesClosure(c)) {
				System.out.println("CLOSURE: "+e);
			}
		}
		*/
		/*
		OWLObject s = g.getOWLObjectByIdentifier("CARO:0000014");
		OWLObject t = g.getOWLObjectByIdentifier("CARO:0000000");
		assertTrue(g.getEdgesBetween(s, t).size() > 0);
		*/
	}
	
}
