package owltools;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLAxiom;

import owltools.InferenceBuilder.PotentialRedundant;
import owltools.graph.OWLGraphWrapper;

public class CheckPotentialRedundantRelationsTest extends OWLToolsTestBasics {

	@Test
	public void test1() throws Exception {
		OWLGraphWrapper graph = getGraph("potential_redundant_rel.obo");
		
		InferenceBuilder builder = new InferenceBuilder(graph);
		
		List<PotentialRedundant> redundants = builder.checkPotentialRedundantSubClassAxioms();
		
		assertEquals(1, redundants.size());
		PotentialRedundant potentialRedundant = redundants.get(0);
		assertNotNull(potentialRedundant.getClassA());
		assertEquals("TEST:0004", graph.getIdentifier(potentialRedundant.getClassA()));
		assertNotNull(potentialRedundant.getProperty());
		assertEquals("BFO:0000050", graph.getIdentifier(potentialRedundant.getProperty()));
		assertNotNull(potentialRedundant.getClassB());
		assertEquals("TEST:0002", graph.getIdentifier(potentialRedundant.getClassB()));
	}
	
	@Test
	public void test2() throws Exception {
		OWLGraphWrapper graph = getGraph("potential_redundant_rel.obo");
		
		InferenceBuilder builder = new InferenceBuilder(graph);
		
		Set<OWLAxiom> allAxioms = graph.getSourceOntology().getAxioms();
		List<PotentialRedundant> redundants = builder.checkPotentialRedundantSubClassAxioms(allAxioms);
		
		assertEquals(1, redundants.size());
		PotentialRedundant potentialRedundant = redundants.get(0);
		assertNotNull(potentialRedundant.getClassA());
		assertEquals("TEST:0004", graph.getIdentifier(potentialRedundant.getClassA()));
		assertNotNull(potentialRedundant.getProperty());
		assertEquals("BFO:0000050", graph.getIdentifier(potentialRedundant.getProperty()));
		assertNotNull(potentialRedundant.getClassB());
		assertEquals("TEST:0002", graph.getIdentifier(potentialRedundant.getClassB()));
	}
}
