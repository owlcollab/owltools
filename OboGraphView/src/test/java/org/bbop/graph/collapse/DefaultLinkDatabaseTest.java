package org.bbop.graph.collapse;

import static org.junit.Assert.*;

import java.util.Collection;

import org.bbop.graph.LinkDatabase.Link;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class DefaultLinkDatabaseTest {
	
	static OWLGraphWrapper graph;
	static OWLReasoner reasoner;
	static DefaultLinkDatabase db;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		graph = pw.parseToOWLGraph("src/test/resources/simple.obo");
		OWLOntology ontology = graph.getSourceOntology();
		ElkReasonerFactory factory = new ElkReasonerFactory();
		reasoner = factory.createReasoner(ontology);
		db = new DefaultLinkDatabase(graph, reasoner);
		
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		if (reasoner != null) {
			reasoner.dispose();
		}
	}

	@Test
	public void testGetChildren() {
		testChildren("CARO:0000", "CARO:0001", "CARO:0002");
	}
	
	private void testChildren(String id, String...childrenIds) {
		OWLObject o = graph.getOWLObjectByIdentifier(id);
		assertNotNull("Could not find OWLObject for id: "+id, o);
		
		Collection<Link> children = db.getChildren(o);
		assertEquals(childrenIds.length, children.size());
	}

	@Test
	public void testGetParents() {
		testParents("CARO:0001", "CARO:0000");
		testParents("CARO:0002", "CARO:0000");
	}
	
	private void testParents(String id, String...parentIds) {
		OWLObject o = graph.getOWLObjectByIdentifier(id);
		assertNotNull("Could not find OWLObject for id: "+id, o);
		
		Collection<Link> parents = db.getParents(o);
		assertEquals(parentIds.length, parents.size());
	}

	@Test
	public void testGetRoots() {
		Collection<OWLObject> roots = db.getRoots();
		assertEquals(1, roots.size());
		OWLObject r = roots.iterator().next();
		assertEquals("caro 0", graph.getLabel(r));
	}

}
