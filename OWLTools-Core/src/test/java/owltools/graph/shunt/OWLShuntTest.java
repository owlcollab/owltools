package owltools.graph.shunt;

import static org.junit.Assert.*;

import java.util.Set;

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

	/**
	 * 
	 * Make sure the basic graph structure hangs together.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGraphStructure() throws Exception {

		//      A
		//    /   \
		//   B     C
		//  / \   / \
		// D   E F   G
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
		
		Set<String> r = g.getRoots();
		assertEquals("root: 1", 1, r.size());
		assertEquals("root: it's a", "a", r.iterator().next());

		Set<String> l = g.getLeaves();
		assertEquals("leaves: 4", 4, l.size());
		assertEquals("leaves: d", true, l.contains("d"));
		assertEquals("leaves: e", true, l.contains("e"));
		assertEquals("leaves: f", true, l.contains("f"));
		assertEquals("leaves: g", true, l.contains("g"));

		// Examine "a"
		Set<String> pa = g.getParents("a");
		assertEquals("a: no parents", 0, pa.size());
		Set<String> ca = g.getChildren("a");
		assertEquals("a: 2 kids", 2, ca.size());
		assertEquals("a: kid b", true, ca.contains("b"));
		assertEquals("a: kid c", true, ca.contains("c"));

		// Examine "b"
		Set<String> pb = g.getParents("b");
		assertEquals("b: 1 parent", 1, pb.size());
		assertEquals("b: parent a", "a", pb.iterator().next());
		Set<String> cb = g.getChildren("b");
		assertEquals("b: 2 kids", 2, cb.size());
		assertEquals("b: kid d", true, cb.contains("d"));
		assertEquals("b: kid e", true, cb.contains("e"));

		// Examine "d"
		Set<String> pd = g.getParents("d");
		assertEquals("d: 1 parent", 1, pd.size());
		assertEquals("d: parent b", "b", pd.iterator().next());
		Set<String> cd = g.getChildren("d");
		assertEquals("d: 0 kids", 0, cd.size());		
	}

}
