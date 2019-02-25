package owltools;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class CAROLCSTest extends OWLToolsTestBasics {

	private static Logger LOG = Logger.getLogger(CAROLCSTest.class);

	@Test
	public void testConvertXPs() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper g =
			pw.parseToOWLGraph(getResourceIRIString("lcstest1.owl"));
		OWLOntology ont = g.getSourceOntology();
		OWLObject o2 = g.getOWLObject("http://example.org#o2");

		LOG.debug("getting ancestors for: "+o2);
		for (OWLGraphEdge e : g.getOutgoingEdgesClosure(o2)) {
			LOG.debug(e);
		}
		for (OWLClass c : ont.getClassesInSignature()) {
			LOG.debug("getting individuals for "+c+" "+g.getLabel(c));
			for (OWLIndividual i : g.getInstancesFromClosure(c)) {
				LOG.debug("  "+i);
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
