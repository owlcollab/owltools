package owltools.test;

import static junit.framework.Assert.*;

import java.io.IOException;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class ClosureTestCARO extends OWLToolsTestBasics {

	@Test
	public void testConvertXPs() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException {
		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper g = pw.parseToOWLGraph(getResourceIRIString("caro.obo"));
		OWLOntology ont = g.getSourceOntology();
		for (OWLClass c : ont.getClassesInSignature()) {
			System.out.println("c="+c+" "+g.getLabel(c));
			for (OWLGraphEdge e : g.getOutgoingEdges(c)) {
				System.out.println(e);
			}
		}

		OWLObject s = g.getOWLObjectByIdentifier("CARO:0000014");
		OWLObject t = g.getOWLObjectByIdentifier("CARO:0000000");

		boolean ok1 = false;
		for (OWLClass c : ont.getClassesInSignature()) {
			System.out.println("getting ancestors for: "+c+" "+g.getLabel(c));
			for (OWLGraphEdge e : g.getOutgoingEdgesClosure(c)) {
				System.out.println("CLOSURE: "+e);
				if (c.equals(s) && e.getTarget().equals(t))
					ok1 = true;
						
			}
		}
		assertTrue(ok1);
		assertTrue(g.getEdgesBetween(s, t).size() > 0);
	}
	
}
