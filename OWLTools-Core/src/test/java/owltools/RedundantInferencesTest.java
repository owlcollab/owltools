package owltools;

import static org.junit.Assert.*;

import java.util.Map;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import owltools.RedundantInferences.RedundantAxiom;
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
		OWLReasonerFactory reasonerFactory = new ReasonerFactory();
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
		Map<OWLClass, Set<RedundantAxiom>> redundantAxiomMap = RedundantInferences.removeRedundantSubClassAxioms(graph.getSourceOntology(), reasoner);
		assertNotNull(redundantAxiomMap);
		assertEquals(1, redundantAxiomMap.size());
		
		OWLClass test1Sub = graph.getOWLClassByIdentifier("FOO:0004");
		OWLClass test1Super = graph.getOWLClassByIdentifier("FOO:0002");
		Set<RedundantAxiom> redundantAxioms = redundantAxiomMap.get(test1Sub);
		assertEquals(1, redundantAxioms.size());
		RedundantAxiom redundantAxiom = redundantAxioms.iterator().next();
		OWLSubClassOfAxiom owlAxiom = redundantAxiom.getAxiom();
		assertTrue(AxiomAnnotationTools.isMarkedAsInferredAxiom(owlAxiom));
		assertEquals(test1Super, owlAxiom.getSuperClass());
		Set<OWLClass> moreSpecific = redundantAxiom.getMoreSpecific();
		assertEquals(1, moreSpecific.size());
		OWLClass moreSpecificClass = moreSpecific.iterator().next();
		assertEquals(graph.getOWLClassByIdentifier("FOO:0003"), moreSpecificClass);


//		graph.getManager().saveOntology(graph.getSourceOntology(), System.err);
	}

}
