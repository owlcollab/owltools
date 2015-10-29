package owltools.flex;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.obolibrary.obo2owl.Obo2Owl;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.parser.OBOFormatParser;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

/**
 * Tests FlexLoader using an ontology module extracted from Regulation of Ribosome Biogenesis
 * 
 * @author cjm
 *
 */
public class RegulationFlexTest {

	private static Logger LOG = Logger.getLogger(OWLGraphWrapper.class);
  
		
		
	/*
	 * Testing getting relation-flexible closures.
	 */
	@Test
	public void testFlexClosures() throws Exception{

		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper g = pw.parseToOWLGraph(getResourceString("reg-of-ribosome-biogenesis.owl"));
		
		// Get objects and a FlexCollection to play with.
		String regulatesTermId = "GO:0090069";
		String regulatedTermId = "GO:0042254";
		String directSuperClassId = "GO:0044087";
		String posRegulatesTermId = "GO:0090070";
	
		// Get objects and a FlexCollection to play with.
		OWLObject regulatesTerm = g.getOWLObjectByIdentifier(regulatesTermId);
		OWLObject regulatedTerm = g.getOWLObjectByIdentifier(regulatedTermId);
		OWLObject directSuperClass = g.getOWLObjectByIdentifier(directSuperClassId);
		OWLObject posRegulatesTerm = g.getOWLObjectByIdentifier(posRegulatesTermId);

		// Define isa.
		List<String> isa = new ArrayList<String>();
		isa.add("getRelationIDClosure");
		// Define isa_partof.
		List<String> isa_partof = new ArrayList<String>();
		isa_partof.add("getRelationIDClosure");
		isa_partof.add("BFO:0000050");
		// Define regulates.
		List<String> reg = new ArrayList<String>();
		reg.add("getRelationIDClosure");
		reg.add("BFO:0000050");
		reg.add("BFO:0000066");
		reg.add("RO:0002211");
		reg.add("RO:0002212");
		reg.add("RO:0002213");

		FlexCollection fc = new FlexCollection(g);
		
		// Isa Closure
		List<String> ans_t1 = fc.getExtStringList(regulatesTerm, isa, null);
		assertTrue("In is_a closure: super", ans_t1.contains(directSuperClassId));
		assertTrue("In is_a closure: SELF", ans_t1.contains(regulatesTermId));
		assertFalse("/Not/ in is_a closure: REGULATED", ans_t1.contains(regulatedTermId));

		// Regulates Closure
		List<String> ans_t2 = fc.getExtStringList(regulatesTerm,reg, null);
		LOG.info("R:"+ans_t2);
		assertTrue("In R closure: REGULATED", ans_t2.contains(regulatedTermId));
		assertTrue("In R closure: SELF", ans_t2.contains(regulatesTermId));
		assertFalse("/Not/ in is_a/part_of closure: SUPER", ans_t2.contains(posRegulatesTermId));

		// Regulates Closure, from pos
		List<String> ans_t3 = fc.getExtStringList(posRegulatesTerm,reg, null);
		LOG.info("R:"+ans_t2);
		assertTrue("In R closure: REGULATED", ans_t3.contains(regulatedTermId));
		assertTrue("In R closure: SUPER", ans_t3.contains(regulatesTermId));
		assertTrue("In R closure: SUPER, transitive", ans_t3.contains(directSuperClassId));
		assertTrue("In R closure: SELF", ans_t3.contains(posRegulatesTermId));


	}
	
	// A little helper from Chris stolen from somewhere else...
	protected static String getResourceString(String name) {
		assertNotNull(name);
		assertFalse(name.length() == 0);
		// TODO: Replace this with a mechanism not relying on the relative path.
		return "src/test/resources/" + name;
	}
}
