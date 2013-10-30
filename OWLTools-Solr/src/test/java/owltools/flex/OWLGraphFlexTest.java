package owltools.flex;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

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

public class OWLGraphFlexTest {

	private static Logger LOG = Logger.getLogger(OWLGraphWrapper.class);
  
//	/*
//	 * Testing a bit of the internal code of #getRelationClosureMap via mimicry.
//	 */
//	@Test
//	public void testGettingRelations() throws Exception{
//
//		// Setup wrapper environment.
//		String fstr = getResourceString("trivial.obo");
//		OBOFormatParser p = new OBOFormatParser();
//		OBODoc obodoc = p.parse(fstr);
//		Obo2Owl bridge = new Obo2Owl();
//		OWLOntology ont = bridge.convert(obodoc);
//		OWLGraphWrapper g = new OWLGraphWrapper(ont);
//
//		//ArrayList<String> relation_ids = new ArrayList<String>();
//		//relation_ids.add("BFO:0000050");
//
////		// Simulation of our relation collection.
//		HashSet<OWLObjectProperty> props = new HashSet<OWLObjectProperty>();
////		if( 1 == 2 ){
////			OWLObjectProperty foo = g.getOWLObjectPropertyByIdentifier("BFO:0000050");
////			LOG.info("IRI: " + foo.getIRI().toString());
////			assertEquals("yes, part_of", foo.getIRI().toString(), "http://purl.obolibrary.org/obo/BFO_0000050");
////			props.add(foo);
////			assertEquals("Just one thing in there", props.size(), 1);
////		}
//		
//		//if (qp.isSubClassOf() || props.contains(qp.getProperty())) {
//		OWLObject a4 = g.getOWLObjectByIdentifier("A:0000004");
//		Set<OWLGraphEdge> edges = g.getOutgoingEdgesClosureReflexive(a4);
//		//assertEquals("two edges", edges.size(), 2);
//		LOG.info("enum edges : " + edges.size());
//		for (OWLGraphEdge owlGraphEdge : edges) {
//			assertNotNull("extant edge", owlGraphEdge);
//			OWLQuantifiedProperty qp = owlGraphEdge.getSingleQuantifiedProperty();
//			assertNotNull("extant prop", qp);
//
//			//LOG.info("edge prop : " + qp.getProperty().getIRI().toString());
//			if(qp.isIdentity()){
//				LOG.info("found identity");
//			}else if (qp.isSubClassOf()) {
//
//				OWLObject target = owlGraphEdge.getTarget();
//				String id = g.getIdentifier(target);
//				LOG.info("found subclass: " + id);
//				
//			}else if( props.contains(qp.getProperty()) ){
//
//				OWLObject target = owlGraphEdge.getTarget();
//				String id = g.getIdentifier(target);
//				LOG.info("found part_of: " + id);
//
//			}else{
//				LOG.info("found other");				
//			}
//		}
//		
//	}		
		
	/*
	 * Testing getting relation-flexible closures.
	 */
	@Test
	public void testFlexClosures() throws Exception{

		// Setup wrapper environment.
		String fstr = getResourceString("trivial.obo");
		OBOFormatParser p = new OBOFormatParser();
		OBODoc obodoc = p.parse(fstr);
		Obo2Owl bridge = new Obo2Owl();
		OWLOntology ont = bridge.convert(obodoc);
		OWLGraphWrapper g = new OWLGraphWrapper(ont);
		
		// Get objects and a FlexCollection to play with.
		OWLObject a3 = g.getOWLObjectByIdentifier("A:0000003");
		OWLObject a4 = g.getOWLObjectByIdentifier("A:0000004");
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
		
		// Small closure on a3, over just is_a.
		List<String> ans_t1 = fc.getExtStringList(a3, isa);
		assertTrue("In is_a closure: A:0000001", ans_t1.contains("A:0000001"));
		assertFalse("/Not/ in is_a closure: A:0000002", ans_t1.contains("A:0000002"));
		assertTrue("In is_a closure: A:0000003", ans_t1.contains("A:0000003"));

		// Small closure on a3, over is_a/part_of.
		List<String> ans_t2 = fc.getExtStringList(a3, isa_partof);
		assertTrue("In is_a/part_of closure: A:0000001", ans_t2.contains("A:0000001"));
		assertFalse("/Not/ in is_a/part_of closure: A:0000002", ans_t2.contains("A:0000002"));
		assertTrue("In is_a/part_of closure: A:0000003", ans_t2.contains("A:0000003"));

		// Small closure on a4, over is_a.
		List<String> ans_t3 = fc.getExtStringList(a4, isa);
		assertTrue("In is_a closure: A:0000001", ans_t3.contains("A:0000001"));
		assertFalse("/Not/ in is_a closure: A:0000002", ans_t3.contains("A:0000002"));
		assertTrue("In is_a closure: A:0000003", ans_t3.contains("A:0000003"));
		assertTrue("In is_a closure: A:0000004", ans_t3.contains("A:0000004"));
		
		// Small closure on a4, over is_a/part_of.
		List<String> ans_t4 = fc.getExtStringList(a4, isa_partof);
		assertTrue("In is_a/part_of closure: A:0000001", ans_t4.contains("A:0000001"));
		assertTrue("In is_a/part_of closure: A:0000002", ans_t4.contains("A:0000002"));
		assertTrue("In is_a/part_of closure: A:0000003", ans_t4.contains("A:0000003"));
		assertTrue("In is_a/part_of closure: A:0000004", ans_t4.contains("A:0000004"));
		
	}
	
	// A little helper from Chris stolen from somewhere else...
	protected static String getResourceString(String name) {
		assertNotNull(name);
		assertFalse(name.length() == 0);
		// TODO: Replace this with a mechanism not relying on the relative path.
		return "src/test/resources/" + name;
	}
}
