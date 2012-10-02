package owltools;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class ThreadedInferenceBuilderTest extends OWLToolsTestBasics {

	@Test
	public void testBuildInferences() throws Exception {
		
		ParserWrapper pw = new ParserWrapper();
		OWLOntology ontology = pw.parseOWL(getResourceIRI("inference_builder_test.omn"));
		
		OWLGraphWrapper graph  = new OWLGraphWrapper(ontology);
		InferenceBuilder builder = new ThreadedInferenceBuilder(graph, InferenceBuilder.REASONER_ELK, 2);
		
		List<OWLAxiom> newInferences = builder.buildInferences();
		assertEquals(2, newInferences.size());

		Collection<OWLAxiom> redundantAxioms = builder.getRedundantAxioms();
		assertEquals(3, redundantAxioms.size());
	}

}
