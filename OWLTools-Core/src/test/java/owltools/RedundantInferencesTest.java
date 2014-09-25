package owltools;

import static org.junit.Assert.*;

import java.util.Map;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import owltools.graph.AxiomAnnotationTools;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class RedundantInferencesTest extends OWLToolsTestBasics {
	
	private static OWLGraphWrapper graph = null;
	private static OWLReasoner reasoner = null;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		graph = pw.parseToOWLGraph(getResourceIRIString("remove-redundant-inferences.obo"));
		OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();
		reasoner = reasonerFactory.createReasoner(graph.getSourceOntology());
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		if (reasoner != null) {
			reasoner.dispose();
		}
	}

	@Test
	public void test() throws Exception {
		Map<OWLClass, Set<OWLSubClassOfAxiom>> redundantAxiomMap = RedundantInferences.removeRedundantSubClassAxioms(graph.getSourceOntology(), reasoner);
		assertNotNull(redundantAxiomMap);
		assertEquals(1, redundantAxiomMap.size());
		
		OWLClass test1Sub = graph.getOWLClassByIdentifier("FOO:0004");
		OWLClass test1Super = graph.getOWLClassByIdentifier("FOO:0002");
		Set<OWLSubClassOfAxiom> redundantAxioms = redundantAxiomMap.get(test1Sub);
		assertEquals(1, redundantAxioms.size());
		for (OWLSubClassOfAxiom owlAxiom : redundantAxioms) {
			assertTrue(AxiomAnnotationTools.isMarkedAsInferredAxiom(owlAxiom));
			assertEquals(test1Super, owlAxiom.getSuperClass());
		}
		
		graph.getManager().saveOntology(graph.getSourceOntology(), System.err);
	}

}
