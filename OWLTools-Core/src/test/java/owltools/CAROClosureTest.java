package owltools;

import static org.junit.Assert.*;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class CAROClosureTest extends OWLToolsTestBasics {

	private static Logger LOG = Logger.getLogger(CAROClosureTest.class);

	@Test
	public void testConvertXPs() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper g = pw.parseToOWLGraph(getResourceIRIString("caro.obo"));
		OWLOntology ont = g.getSourceOntology();
		for (OWLClass c : ont.getClassesInSignature()) {
			LOG.debug("c="+c+" "+g.getLabel(c));
			for (OWLGraphEdge e : g.getOutgoingEdges(c)) {
				LOG.debug(e);
			}
		}

		OWLObject s = g.getOWLObjectByIdentifier("CARO:0000014");
		OWLObject t = g.getOWLObjectByIdentifier("CARO:0000000");

		boolean ok1 = false;
		for (OWLClass c : ont.getClassesInSignature()) {
			LOG.debug("getting ancestors for: "+c+" "+g.getLabel(c));
			for (OWLGraphEdge e : g.getOutgoingEdgesClosure(c)) {
				LOG.debug("CLOSURE: "+e);
				if (c.equals(s) && e.getTarget().equals(t))
					ok1 = true;
						
			}
		}
		assertTrue(ok1);
		assertTrue(g.getEdgesBetween(s, t).size() > 0);
	}
	
}
