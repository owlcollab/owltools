package owltools.graph.shunt;

import static org.junit.Assert.*;

import org.junit.Test;

import owltools.OWLToolsTestBasics;

/**
 * Check the basic workings of the export shunt iterator.
 */
public class OWLShuntDFIteratorTest extends OWLToolsTestBasics {

	/**
	 * 
	 * Check the DFS iterator.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGraphStructure() throws Exception {

		// First assemble graph:
		//
		//      A
		//    /   \
		//   B     C
		//  / \   / \
		// D   E F   G
		//
		OWLShuntGraph g = new OWLShuntGraph();
		g.addNode(new OWLShuntNode("a", "A"));
		g.addNode(new OWLShuntNode("b", "B"));
		g.addNode(new OWLShuntNode("c", "C"));
		g.addNode(new OWLShuntNode("d", "D"));
		g.addNode(new OWLShuntNode("e", "E"));
		g.addNode(new OWLShuntNode("f", "F"));
		g.addNode(new OWLShuntNode("g", "G"));
		g.addEdge(new OWLShuntEdge("a", "b"));
		g.addEdge(new OWLShuntEdge("a", "c"));
		g.addEdge(new OWLShuntEdge("b", "d"));
		g.addEdge(new OWLShuntEdge("b", "e"));
		g.addEdge(new OWLShuntEdge("c", "f"));
		g.addEdge(new OWLShuntEdge("c", "g"));

		// DFS string collection.
		OWLShuntGraphDFIterator gi = new OWLShuntGraphDFIterator(g);
		String test = "";
		while( gi.hasNext() ){
			test = test + gi.next();
		}
		
		//System.err.println("i root: " + test);
		
		// Test relative string positions.
		assertTrue("item: a is first", test.indexOf("a") == 0);
		assertTrue("item: b after a", test.indexOf("b") > test.indexOf("a"));
		assertTrue("item: c after a", test.indexOf("c") > test.indexOf("a"));
		assertTrue("item: d after b", test.indexOf("d") > test.indexOf("b"));
		assertTrue("item: e after b", test.indexOf("e") > test.indexOf("b"));
		assertTrue("item: f after c", test.indexOf("f") > test.indexOf("c"));
		assertTrue("item: g after c", test.indexOf("g") > test.indexOf("c"));
	}

}
