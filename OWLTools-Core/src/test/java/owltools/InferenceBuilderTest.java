package owltools;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

/**
 * Tests for {@link InferenceBuilder}.
 */
public class InferenceBuilderTest extends OWLToolsTestBasics {

	/**
	 * Test that the {@link InferenceBuilder} does not report the trivial fact
	 * that OWL:Nothing is a subclass of of every class.
	 * 
	 * @throws Exception 
	 */
	@Test
	public void testIgnoreIgnoreOwlNothingInNewInferences() throws Exception {
		// create a test ontology which declares owl:Nothing and one term
		
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		
		OWLOntology ontology = manager.createOntology();
		OWLDataFactory factory = manager.getOWLDataFactory();
		
		// declare class
		
		OWLClass owlEntity = factory.getOWLClass(IRI.create("http://foo.bar/1"));
		manager.addAxiom(ontology, factory.getOWLDeclarationAxiom(owlEntity));
		
		// declare owl:Nothing
		manager.addAxiom(ontology, factory.getOWLDeclarationAxiom(factory.getOWLNothing()));
		
		OWLGraphWrapper graph = new OWLGraphWrapper(ontology);
		
		// check that the inference builder does not report a new inferred axiom
		InferenceBuilder builder = new InferenceBuilder(graph);
		List<OWLAxiom> inferences = builder.buildInferences();
		if (!inferences.isEmpty()) {
			fail("Do not expect any new inferences, but was: "+inferences);
		}
	}
	
	@Test
	public void testNewAndRedundant() throws Exception {
		
		ParserWrapper pw = new ParserWrapper();
		OWLOntology ontology = pw.parseOWL(getResourceIRI("inference_builder_test.omn"));
		
		OWLGraphWrapper graph  = new OWLGraphWrapper(ontology);
		InferenceBuilder builder = new InferenceBuilder(graph, InferenceBuilder.REASONER_ELK);
		
		List<OWLAxiom> newInferences = builder.buildInferences();
		assertEquals(2, newInferences.size());

		Collection<OWLAxiom> redundantAxioms = builder.getRedundantAxioms();
		assertEquals(3, redundantAxioms.size());
	}

}
