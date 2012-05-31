package owltools.graph.shunt;

import static junit.framework.Assert.*;
import org.junit.Test;

import owltools.OWLToolsTestBasics;

/**
 * Check the basic workings of the export shunt.
 */
public class OWLShuntTest extends OWLToolsTestBasics {

	/**
	 * 
	 * Just check the basic ensemble assembly, since it doesn't really _do_ anything functional.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testTrivialAdd() throws Exception {

		OWLShuntGraph g = new OWLShuntGraph();
		g.addNode(new OWLShuntNode("a", "A"));
		g.addNode(new OWLShuntNode("b", "B"));
		g.addEdge(new OWLShuntEdge("a", "b"));
		
		assertEquals("two nodes", 2, g.nodes.size());
		assertEquals("one edge", 1, g.edges.size());

		// Add another similar, but different (has rel), edge.
		g.addEdge(new OWLShuntEdge("a", "b", "e"));
		
		assertEquals("still two nodes", 2, g.nodes.size());
		assertEquals("two edges", 2, g.edges.size());

		// Add same node and edges, for hopefully no changes.
		g.addNode(new OWLShuntNode("b", "B"));
		g.addEdge(new OWLShuntEdge("a", "b", "e"));
		
		assertEquals("even still two nodes", 2, g.nodes.size());
		assertEquals("even still two edges", 2, g.edges.size());
	}
	
	/**
	 * 
	 * Check the basic internal check ops.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testTrivialHas() throws Exception {

		OWLShuntGraph g = new OWLShuntGraph();
		g.addNode(new OWLShuntNode("a", "A"));
		g.addNode(new OWLShuntNode("b", "B"));
		g.addNode(new OWLShuntNode("c", "C"));
		g.addEdge(new OWLShuntEdge("a", "b", "foo"));
		g.addEdge(new OWLShuntEdge("a", "b", "bar"));
		g.addEdge(new OWLShuntEdge("b", "c"));
		
		assertEquals("yes node b", true, g.hasNode(new OWLShuntNode("b")));
		assertEquals("no node d", false, g.hasNode(new OWLShuntNode("d")));
		
		assertEquals("yes edge: a b foo", true, g.hasEdge(new OWLShuntEdge("a", "b", "foo")));
		assertEquals("yes edge: b c", true, g.hasEdge(new OWLShuntEdge("b", "c")));
		assertEquals("no edge: b c foo", false, g.hasEdge(new OWLShuntEdge("b", "c", "foo")));
		assertEquals("no edge: a b", false, g.hasEdge(new OWLShuntEdge("a", "b")));
	}

}
