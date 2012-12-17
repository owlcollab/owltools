package owltools;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class ChainRegulationTest extends OWLToolsTestBasics {

	@Test
	public void testConvertXPs() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper g =
			pw.parseToOWLGraph(getResourceIRIString("positive_regulation_of_anti_apoptosis.obo"));
		OWLOntology ont = g.getSourceOntology();
		for (OWLClass c : ont.getClassesInSignature()) {
			System.out.println("c="+c+" "+g.getLabel(c));
			for (OWLGraphEdge e : g.getOutgoingEdges(c)) {
				System.out.println(e);
			}
		}

		for (OWLClass c : ont.getClassesInSignature()) {
			System.out.println("c="+c+" "+g.getLabel(c));
			for (OWLGraphEdge e : g.getOutgoingEdgesClosure(c)) {
				System.out.println("CLOSURE: "+e);
			}
		}
	}
	
}
