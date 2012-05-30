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
	 * Just check the basic ensemble, since it doesn't really _do_ anything functional.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testTrivial() throws Exception {

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
}
