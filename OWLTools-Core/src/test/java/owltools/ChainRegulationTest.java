package owltools;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class ChainRegulationTest extends OWLToolsTestBasics {

	private static Logger LOG = Logger.getLogger(ChainRegulationTest.class);

	@Test
	public void testConvertXPs() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper g =
			pw.parseToOWLGraph(getResourceIRIString("positive_regulation_of_anti_apoptosis.obo"));
		OWLOntology ont = g.getSourceOntology();
		for (OWLClass c : ont.getClassesInSignature()) {
			LOG.debug("c="+c+" "+g.getLabel(c));
			for (OWLGraphEdge e : g.getOutgoingEdges(c)) {
				LOG.debug(e);
			}
		}

		for (OWLClass c : ont.getClassesInSignature()) {
			LOG.debug("c="+c+" "+g.getLabel(c));
			for (OWLGraphEdge e : g.getOutgoingEdgesClosure(c)) {
				LOG.debug("CLOSURE: "+e);
			}
		}
	}
	
}
