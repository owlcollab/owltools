package owltools.test;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import org.obolibrary.obo2owl.Obo2Owl;
import org.obolibrary.oboformat.model.Frame;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.model.Xref;
import org.obolibrary.oboformat.parser.OBOFormatParser;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

import junit.framework.TestCase;

public class LCSTestCARO extends TestCase {

	public static void testConvertXPs() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException {
		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper g =
			pw.parseToOWLGraph("file:test_resources/lcstest1.owl");
		OWLOntology ont = g.getOntology();
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
