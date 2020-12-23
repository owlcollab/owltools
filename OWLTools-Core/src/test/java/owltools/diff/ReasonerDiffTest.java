package owltools.diff;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLAxiom;

import owltools.InferenceBuilder;
import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;
import owltools.io.ParserWrapper;

/**
 * Tests for {@link ReasonerDiff}.
 */
public class ReasonerDiffTest extends OWLToolsTestBasics {

	@Test
	public void testDiff() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper baseLine = pw.parseToOWLGraph(getResourceIRIString("regulation_of_anti_apoptosis_xp-baseline.obo"));
		
		OWLGraphWrapper change = pw.parseToOWLGraph(getResourceIRIString("regulation_of_anti_apoptosis_xp_addon.obo"));
		ReasonerDiff diff = ReasonerDiff.createReasonerDiff(baseLine, change, InferenceBuilder.REASONER_HERMIT);
		
		List<OWLAxiom> newAxioms = diff.getNewAxioms();
		List<OWLAxiom> rmAxioms = diff.getRemovedInferredAxioms();
		
		OWLPrettyPrinter pp = new OWLPrettyPrinter(baseLine);
		
		for (OWLAxiom owlAxiom : newAxioms) {
			System.out.println(pp.render(owlAxiom));
		}
		for (OWLAxiom owlAxiom : rmAxioms) {
			System.out.println("RM: "+pp.render(owlAxiom));
		}
		assertEquals(1, newAxioms.size());
		assertEquals(2, rmAxioms.size());
	}

}
