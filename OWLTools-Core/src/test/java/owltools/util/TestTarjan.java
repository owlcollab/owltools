package owltools.util;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;

/**
 * Tests for {@link Tarjan} algorithm implementations using
 * {@link AbstractTarjan} and {@link MappingTarjan}.
 */
public class TestTarjan {

	@Test
	public void testMappingBasedTarjan1() {
		Tarjan<IRI> t = new MappingTarjan<IRI>(false);

		Adjacency<IRI> graph = new TestIRIAdjacency();
		
		List<List<IRI>> scc = t.executeTarjan(graph);

		assertEquals(2, scc.size());
		assertEquals(1, scc.get(0).size());
		assertEquals(3, scc.get(1).size());
	}
	
	@Test
	public void testMappingBasedTarjan2() {
		Tarjan<IRI> t = new MappingTarjan<IRI>(true);

		Adjacency<IRI> graph = new TestIRIAdjacency();
		
		List<List<IRI>> scc = t.executeTarjan(graph);

		assertEquals(1, scc.size());
		assertEquals(3, scc.get(0).size());
	}

	static class TestIRIAdjacency implements Adjacency<IRI> {

		final IRI a = IRI.create("a");
		final IRI b = IRI.create("b");
		final IRI c = IRI.create("c");
		final IRI d = IRI.create("d");

		@Override
		public List<IRI> getAdjacent(IRI source) {
			if (b.equals(source)) {
				return Arrays.asList(a,d);
			}
			else if (c.equals(source)) {
				return Arrays.asList(b);
			}
			else if (d.equals(source)) {
				return Arrays.asList(c);
			}
			return Collections.emptyList();
		}

		@Override
		public Collection<IRI> getSources() {
			return Arrays.asList(d,c,a,b);
		}

	}
	
	@Test
	public void testNodeBasedTarjan() {
		NodeTarjan t = new NodeTarjan();

		Adjacency<Node> graph = new TestAdjacency();
		
		List<List<Node>> scc = t.executeTarjan(graph);

		assertEquals(2, scc.size());
		assertEquals(1, scc.get(0).size());
		assertEquals(3, scc.get(1).size());
	}

	static class NodeTarjan extends AbstractTarjan<Node> {
		
		public NodeTarjan() {
			super(false);
		}
		
		@Override
		protected void setIndex(Node n, int index) {
			n.setIndex(index);
		}
		
		@Override
		protected int getIndex(Node n) {
			return n.getIndex();
		}
		
		@Override
		protected void setLowlink(Node n, int lowlink) {
			n.setLowlink(lowlink);
		}
		
		@Override
		protected int getLowlink(Node n) {
			return n.getLowlink();
		}

		@Override
		protected boolean notEquals(Node n1, Node n2) {
			return n1 != n2;
		}
	}
	
	static class TestAdjacency implements Adjacency<Node> {

		final Node a = new Node(1);
		final Node b = new Node(2);
		final Node c = new Node(3);
		final Node d = new Node(4);

		TestAdjacency() {

		}

		@Override
		public List<Node> getAdjacent(Node source) {
			if (b.equals(source)) {
				return Arrays.asList(a,d);
			}
			else if (c.equals(source)) {
				return Arrays.asList(b);
			}
			else if (d.equals(source)) {
				return Arrays.asList(c);
			}
			return Collections.emptyList();
		}

		@Override
		public Collection<Node> getSources() {
			return Arrays.asList(d,c,a,b);
		}

	}
	
	static class Node {
		
		private final int name;
		private int lowlink = -1;
		private int index = -1;

		public Node(final int argName) {
			name = argName;
		}

		/**
		 * @return the lowlink
		 */
		public int getLowlink() {
			return lowlink;
		}

		/**
		 * @param lowlink the lowlink to set
		 */
		public void setLowlink(int lowlink) {
			this.lowlink = lowlink;
		}

		/**
		 * @return the index
		 */
		public int getIndex() {
			return index;
		}

		/**
		 * @param index the index to set
		 */
		public void setIndex(int index) {
			this.index = index;
		}

		/**
		 * @return the name
		 */
		public int getName() {
			return name;
		}

		// generated
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("Node [name=");
			builder.append(name);
			builder.append(", lowlink=");
			builder.append(lowlink);
			builder.append(", index=");
			builder.append(index);
			builder.append("]");
			return builder.toString();
		}
	}
}
