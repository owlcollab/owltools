package owltools;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLAxiom;

import owltools.InferenceBuilder.AxiomPair;
import owltools.graph.OWLGraphWrapper;

public class CheckPotentialRedundantRelationsTest extends OWLToolsTestBasics {

	@Test
	public void test1() throws Exception {
		OWLGraphWrapper graph = getGraph("potential_redundant_rel.obo");
		
		InferenceBuilder builder = new InferenceBuilder(graph);
		
		List<AxiomPair> axioms = builder.checkPotentialRedundantSubClassAxioms();
		
		assertEquals(1, axioms.size());
	}
	
	@Test
	public void test2() throws Exception {
		OWLGraphWrapper graph = getGraph("potential_redundant_rel.obo");
		
		InferenceBuilder builder = new InferenceBuilder(graph);
		
		Set<OWLAxiom> allAxioms = graph.getSourceOntology().getAxioms();
		List<AxiomPair> axioms = builder.checkPotentialRedundantSubClassAxioms(allAxioms);
		
		assertEquals(1, axioms.size());
	}
}
