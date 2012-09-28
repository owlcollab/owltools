package org.bbop.graph;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.bbop.graph.LinkDatabase.Link;
import org.bbop.util.ShapeUtil;
import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphWrapper;

public class HierarchicalGraphLayout implements GraphLayout {

	//initialize logger
	protected final static Logger logger = Logger.getLogger(HierarchicalGraphLayout.class);

	private final OWLGraphWrapper graph;
	
	public HierarchicalGraphLayout(OWLGraphWrapper graph) {
		this.graph = graph;
	}
	
	//
	// Magic constants.
	// WARNING! Playing with these constants will mess with yo6ur head.
	//

	/** Ratio of maximum edge vertical distance to horizontal distance */
	public int edgeLengthHeightRatio = 20; // originally 6

	/**
	 * Number of passes up and down the levels to attempt to optimise node
	 * positions.
	 */
//	public final int reorderIterations = 10;
	// 6/2008: 1 seems to work almost as well as 10 (and of course it's much quicker).
	// Simple graphs come out almost exactly the same.  Huge hairy ones are huge and hairy
	// whether you do 1 iteration or 20.
	public final int reorderIterations = 1;

	/** Minimum gap between levels */
//	public final int minLevelGap = 20;
	public final int minLevelGap = 30; // originally 20

	/** Levels may be split if they have more than this number of nodes */
	public final int maxLevelSize = 100;

	/** Edges running though levels will be allocated this much horizontal space */
	public int insertedEdgeWidth = 20; // originally 20

	/** Horizontal gap between nodes */
	public int withinLevelGap = 20;

	// Public interface

	public enum Orientation {
		/** Parent nodes at top of graph layout */
		PARENT_TOP,

		/** Parent nodes at left of graph layout */
		PARENT_LEFT,

		/** Parent nodes at left of graph layout */
		PARENT_RIGHT,
	
		/** Parent nodes at left of graph layout */
		PARENT_BOTTOM
	}

	private Map<Object, Shape> posMap = new HashMap<Object, Shape>();

	private Map<DummyEdge, Shape> edgeShapes = new HashMap<DummyEdge, Shape>();

	private Map<Object, NodeObj> nodes = new LinkedHashMap<Object, NodeObj>();

	private Collection<DummyEdge> dummyEdges = new LinkedHashSet<DummyEdge>();

	private boolean drawEdgesToNodeCenters = false;

	private DummyEdge scratchEdge = new DummyEdge(new NodeObj("parent_str", -1),
			new NodeObj("parent_str", -2), true);

	// Internal implementation

	// fields

	private Orientation orientation = Orientation.PARENT_TOP;

	private ArrayList<Level> levels = new ArrayList<Level>();

	private Collection<DummyEdge> originalEdges;

	private class NodeObj {
		protected Object o;

		protected int width;

		protected int height;

		protected Level level;

		protected int location;

		protected boolean isDummy = false;

		protected int name;

		public NodeObj(Object o, int name) {
			this(o, name, false);
		}

		public NodeObj(Object o, int name, boolean isDummy) {
			this.o = o;
			this.name = name;
			this.isDummy = isDummy;
		}

		public boolean isDummy() {
			return isDummy;
		}

		public int getLocation() {
			return location;
		}

		public Object getNode() {
			return o;
		}

		public int getLayoutHeight() {
			if (orientation == Orientation.PARENT_TOP || orientation == Orientation.PARENT_BOTTOM)
				return height;
			else
				return width;
		}

		public int getHeight() {
			return height;
		}

		public void setHeight(int height) {
			this.height = height;
		}

		public Level getLevel() {
			return level;
		}

		public void setLevel(Level level) {
			this.level = level;
			if (level == null)
				(new Exception("null level specified for " + getNode()))
				.printStackTrace();
		}

		public int getWidth() {
			return width;
		}

		public int getLayoutWidth() {
			if (orientation == Orientation.PARENT_TOP || orientation == Orientation.PARENT_BOTTOM)
				return width;
			else
				return height;
		}

		public void setWidth(int width) {
			this.width = width;
		}

		public void setLocation(int location) {
			this.location = location;
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof NodeObj) {
				return ((NodeObj) o).getNode().equals(getNode());
			} else
				return false;
		}

		@Override
		public int hashCode() {
			return getNode().hashCode();
		}

		@Override
		public String toString() {
			return "node:" + getNode().toString();
		}
	}

	private class Level {

		int levelNumber;

		// int size;
		int location, depth;

		List<NodeObj> nodes = new ArrayList<NodeObj>();

		public Level(int levelNumber) {
			this.levelNumber = levelNumber;
		}

		public void addNode(NodeObj nodeObject) {
			nodes.add(nodeObject);
		}

		void reorder(Level above, Level below) {
			for (int j = 0; j < nodes.size(); j++) {

				NodeObj nj = nodes.get(j);

				double total = 0;
				int connected = 0;

				if (above != null)
					for (int i = 0; i < above.nodes.size(); i++) {
						NodeObj ni = above.nodes.get(i);

						if (isConnected(ni, nj)) {
							connected++;
							total += ni.getLocation();
						}
					}

				if (below != null)
					for (int i = 0; i < below.nodes.size(); i++) {
						NodeObj ni = below.nodes.get(i);

						if (isConnected(ni, nj)) {
							connected++;
							total += ni.getLocation();
						}
					}

				if (connected == 0) {
					continue;
					// throw new RuntimeException("No connected Nodes");
				} else {
					total /= connected;
				}

				nj.setLocation((int) total);
			}

			while (true) {
				Collections.sort(nodes, nodeLayoutComparator);

				boolean foundOverlap = false;
				for (int i = 1; i < nodes.size(); i++) {
					NodeObj a = nodes.get(i - 1);
					NodeObj b = nodes.get(i);

					int overlap = minLevelGap
					+ (a.getLocation() + a.getLayoutWidth() / 2)
					- (b.getLocation() - b.getLayoutWidth() / 2);
					if (overlap > 0) {
						foundOverlap = true;
						a.setLocation(a.getLocation() - overlap / 2 - 1);
						b.setLocation(b.getLocation() + overlap / 2 + 1);
					}
				}
				if (!foundOverlap)
					break;
			}
		}

		void calcInitialPositions() {

			int width = 0;
			for (int i = 0; i < nodes.size(); i++) {
				NodeObj n = nodes.get(i);

				n.setLocation(width + n.getLayoutWidth() / 2);
				width += n.getLayoutWidth() + withinLevelGap;
			}

		}

		void shiftLeft(int delta) {
			for (int i = 0; i < nodes.size(); i++) {
				NodeObj node = nodes.get(i);
				nodes.get(i).setLocation(node.getLocation() - delta);
			}
		}

		void setDepth(int depth) {
			this.depth = depth;

		}

		void setLocation(int location) {
			this.location = location;
		}

		int getWidth() {
			final NodeObj nd = nodes.get(nodes.size() - 1);
			return nd.getLocation() + nd.getLayoutWidth() / 2;
		}

		int getStart() {
			final NodeObj nd = nodes.get(0);
			return nd.getLocation() - nd.getLayoutWidth() / 2;
		}

	}

	/**
	 * After calling layout() call getWidth and getHeight <br/> All nodes will
	 * be in this bounding box. <br/>
	 * 0&lt;node.x+/-node.getWidth/2&lt;layout.getWidth <br/>
	 * 0&lt;node.y+/-node.getHeight/2&lt;layout.getHeight <br/> Noting that x
	 * and y are the centres of the nodes. All edge routes will also be in the
	 * bounding box.
	 * 
	 * @return width of layout
	 */

	public int getWidth() {
		int maxWidth = 0;

		for (Iterator<Level> i = levels.iterator(); i.hasNext();)
			maxWidth = Math.max(maxWidth, i.next().getWidth());

		return maxWidth;

	}

	/** See getWidth() */
	public int getHeight() {
		Level l = levels.get(levels.size() - 1);
		return l.location + l.depth / 2;
	}

	private Comparator<NodeObj> nodeLayoutComparator = new Comparator<NodeObj>() {
		
		@Override
		public int compare(NodeObj o1, NodeObj o2) {
			int val = o1.getLocation() - o2.getLocation();
			if (val == 0)
				return objectComparator.compare(o1, o2);
			else
				return val;
		}
	};

	public boolean isConnected(NodeObj a, NodeObj b) {
		scratchEdge.setChildParent(a, b);
		if (dummyEdges.contains(scratchEdge)) {
			return true;
		}
		scratchEdge.setChildParent(b, a);
		if (dummyEdges.contains(scratchEdge)) {
			return true;
		}
		return false;
	}

	public Collection<NodeObj> getParents(NodeObj child) {
		Collection<NodeObj> out = new LinkedList<NodeObj>();
		for (DummyEdge edge : dummyEdges) {
			if (edge.getLayoutChild()!= null && edge.getLayoutChild().equals(child)) {
				out.add(edge.getLayoutParent());
			}
		}
		return out;
	}

	/**
	 * Compute layout. This method finally calls setLocation on all the nodes
	 * and setRoute on all the edges.
	 */

	@Override
	public void reset() {
		nodes = new LinkedHashMap<Object, NodeObj>();
		dummyEdges.clear();
	}

	protected Comparator<NodeObj> objectComparator = new Comparator<NodeObj>() {
		@Override
		public int compare(NodeObj o1, NodeObj o2) {
			Object oa = o1.getNode();
			Object ob = o2.getNode();
			if (oa instanceof OWLObject
					&& ob instanceof OWLObject) {
				OWLObject a = (OWLObject) o1.getNode();
				OWLObject b = (OWLObject) o2.getNode();
				// Dangling objects have null names (Chris did it that way on purpose)
				String aName = graph.getLabel(a);
				if (aName == null)
					return -1;
				String bName = graph.getLabel(b);
				if (bName == null)
					return 1;
				return aName.compareToIgnoreCase(bName);
			} else if (ob instanceof OWLObject)
				return -1;
			else if (oa instanceof OWLObject)
				return 1;
			else
				return 0;
		}
	};


	protected int nodeCount = 0;

	@Override
	public void doLayout() {

		levels.clear();

		ArrayList<NodeObj> nodeList = new ArrayList<NodeObj>(nodes.values());
		boolean [] seenem = new boolean[nodes.size()];
		Collections.sort(nodeList, new Comparator<NodeObj>() {

			@Override
			public int compare(NodeObj o1, NodeObj o2) {
				return objectComparator.compare(o1, o2);
			}
		});

		for (NodeObj n : nodeList) {
			findLevel(maxLevelSize, n, seenem);
		}

		rationalise();

		for (Level l : levels) {
			l.calcInitialPositions();
		}

		orderNodesInLevels();

		calcLevelLocations();

		int minStart = Integer.MAX_VALUE;

		for (Level l : levels) {
			minStart = Math.min(minStart, l.getStart());
		}

		for (Level l : levels) {
			l.shiftLeft(minStart);
		}
		posMap = new HashMap<Object, Shape>();
		edgeShapes = new HashMap<DummyEdge, Shape>();

		int multiplier = 1;
		if (orientation == Orientation.PARENT_BOTTOM || orientation == Orientation.PARENT_RIGHT) {
			multiplier = -1;
		}

		for (NodeObj n : nodes.values()) {
			if (n.isDummy())
				continue;
			Point2D offset = new Point2D.Double();
			if (orientation == Orientation.PARENT_TOP || orientation == Orientation.PARENT_BOTTOM)
				offset.setLocation(n.getLocation(), multiplier
						* n.getLevel().location);
			else
				offset.setLocation(multiplier * n.getLevel().location, n
						.getLocation());

			offset.setLocation(offset.getX() - n.getWidth() / 2, 
//					offset.getY() - n.getHeight() / 2);
					// Put the icon box a bit lower on its link.
					// Problem:  what if subtracting 5 results in a number below 0??
					// I don't want to put a min or an if here because that could increase
					// the running time.
					offset.getY() - n.getHeight() / 2 - 5);
			Rectangle r = new Rectangle((int) offset.getX(), (int) offset
					.getY(), n.getWidth(), n.getHeight());

			posMap.put(n.getNode(), r);
		}
		for (DummyEdge n : dummyEdges) {
			if (n.isDummy())
				continue;

			GeneralPath shape = new GeneralPath();

			for (DummyEdge edge : n.getComponentEdges()) {

				NodeObj parent = edge.getLayoutParent();
				NodeObj child = edge.getLayoutChild();
				if (child == null)
					continue;

				int parentLocation = parent.getLocation();
				int childLocation = child.getLocation();

				int levelParent = parent.getLevel().location
				+ parent.getLevel().depth / 2;
				int levelChild = child.getLevel().location
				- child.getLevel().depth / 2;

				int levelCentre = (levelParent + levelChild) / 2;

				int nodeParent;
				int nodeChild;
				if (drawEdgesToNodeCenters) {
					nodeParent = parent.getLevel().location;
					nodeChild = child.getLevel().location;
				} else {
					nodeParent = parent.getLevel().location
					+ parent.getLayoutHeight() / 2;
					nodeChild = child.getLevel().location
					- child.getLayoutHeight() / 2;
				}
				if (orientation == Orientation.PARENT_TOP || orientation == Orientation.PARENT_BOTTOM) {
					if (shape.getCurrentPoint() == null)
						shape.moveTo(parentLocation, multiplier * nodeParent);
					else
						shape.lineTo(parentLocation, multiplier * nodeParent);
					shape.lineTo(parentLocation, multiplier * levelParent);
					shape.curveTo(parentLocation, multiplier * levelCentre,
							childLocation, multiplier * levelCentre,
							childLocation, multiplier * levelChild);
					shape.lineTo(childLocation, multiplier * nodeChild);

				} else {
					if (shape.getCurrentPoint() == null)
						shape.moveTo(multiplier * nodeParent, parentLocation);
					else
						shape.lineTo(multiplier * nodeParent, parentLocation);
					shape.lineTo(multiplier * levelParent, parentLocation);
					shape.curveTo(multiplier * levelCentre, parentLocation,
							multiplier * levelCentre, childLocation, multiplier
							* levelChild, childLocation);
					shape.lineTo(multiplier * nodeChild, childLocation);
				}
			}

			if (orientation == Orientation.PARENT_TOP || orientation == Orientation.PARENT_LEFT)
				shape = (GeneralPath) ShapeUtil.reverseShape(shape, null);

			edgeShapes.put(n, shape);
		}
	}

	// methods

	private int findLevel(int maxLevelSize, NodeObj node, boolean [] seenem) {

		if (node.getLevel() != null)
			return node.getLevel().levelNumber;

		if (seenem[node.name])
			return 0;

		seenem[node.name] = true;
		int maxParentLevel = -1;

		Collection<NodeObj> parents = getParents(node);

		for (NodeObj parent : parents) {
			if (parent == null)
				continue;
			int l = findLevel(maxLevelSize, parent, seenem);
			if (l > maxParentLevel)
				maxParentLevel = l;
		}

		int levelNumber = maxParentLevel + 1;

		while (true) {

			while (levelNumber >= levels.size())
				levels.add(new Level(levels.size()));

			if (levels.get(levelNumber).nodes.size() < maxLevelSize)
				break;

			levelNumber++;
		}

		node.setLevel(levels.get(levelNumber));

		node.getLevel().addNode(node);

		return levelNumber;
	}

	private void rationalise(DummyEdge e) {
		int parentLevel = e.getLayoutParent().getLevel().levelNumber;
		int childLevel = (e.getLayoutChild() == null) ? 0 : e.getLayoutChild().getLevel().levelNumber;

		if (parentLevel < childLevel - 1) {
			// logger.info("Rationalise "+parentLevel+" "+childLevel);
			NodeObj a = e.getLayoutParent();
			for (int i = parentLevel + 1; i <= childLevel; i++) {

				NodeObj b;
				if (i == childLevel) {
					b = e.getLayoutChild();
				} else {
					Object o = new Object();
					b = new NodeObj(o, -1, true);
					b.setHeight(-1);
					b.setWidth(insertedEdgeWidth);
					b.setLevel(levels.get(i));
					b.getLevel().addNode(b);
				}
				if (!nodes.containsKey(b.getNode()))
					nodes.put(b.getNode(), b);
				DummyEdge insertedEdge = new DummyEdge(b, a, true);
				dummyEdges.add(insertedEdge);
				e.getComponentEdges().add(insertedEdge);

				a = b;
			}

		} else {
			e.getComponentEdges().add(e);
		}
		dummyEdges.add(e);
	}

	private void rationalise() {
		originalEdges = new LinkedList<DummyEdge>(dummyEdges);

		dummyEdges.clear();

		for (DummyEdge e : originalEdges) {
			rationalise(e);
		}
	}

	private void orderNodesInLevels() {
		int s = levels.size();
		for (int j = 0; j < reorderIterations; j++) {

			for (int i = 0; i < s; i++) {
				Level p = (i == 0) ? null : levels.get(i - 1);
				Level l = levels.get(i);
				Level n = (i == s - 1) ? null : levels.get(i + 1);
				l.reorder(p, n);
			}

			for (int i = s - 1; i >= 0; i--) {
				Level p = (i == 0) ? null : levels.get(i - 1);
				Level l = levels.get(i);
				Level n = (i == s - 1) ? null : levels.get(i + 1);
				l.reorder(p, n);
			}

		}
	}

	private void calcLevelLocations() {
		int height = 0;

		Level p = null;

		for (Iterator<Level> i = levels.iterator(); i.hasNext();) {
			Level l = i.next();
			int maxLength = 0;

			// Calculate maximum edge length
			if (p != null) {
				for (Iterator<NodeObj> i2 = l.nodes.iterator(); i2.hasNext();) {
					NodeObj n1 = i2.next();
					for (Iterator<NodeObj> i3 = p.nodes.iterator(); i3.hasNext();) {
						NodeObj n2 = i3.next();
						if (isConnected(n1, n2)) {
							maxLength = Math.max(maxLength, Math.abs(n1
									.getLocation()
									- n2.getLocation()));
						}
					}
				}
				height += Math.max(minLevelGap, maxLength
						/ edgeLengthHeightRatio);
			}

			int maxHeight = 0;

			for (Iterator<NodeObj> i2 = l.nodes.iterator(); i2.hasNext();) {
				maxHeight = Math.max(maxHeight, i2.next().getLayoutHeight());
			}

			l.setDepth(maxHeight);

			height += l.depth / 2;

			l.setLocation(height);

			height += maxHeight;

			p = l;
		}
	}

	@Override
	public void addEdge(Link link) {
		NodeObj childNode = getNode(link.getSource());
		NodeObj parentNode = getNode(link.getTarget());
		if(childNode != null && parentNode != null){
			DummyEdge edge = new DummyEdge(childNode, parentNode);
			dummyEdges.add(edge);
		}
	}

	public NodeObj getNode(Object o) {
		return nodes.get(o);
	}

	@Override
	public void addNode(OWLObject node) {
		nodes.put(node, new NodeObj(node, nodes.size()));
	}

	@Override
	public Shape getEdgeShape(Link link) {
		scratchEdge.setChildParent(new NodeObj(link.getSource(), -1), new NodeObj(link.getTarget(), -1));
		return edgeShapes.get(scratchEdge);
	}

	@Override
	public Shape getNodeShape(OWLObject node) {
		return posMap.get(node);
	}

	@Override
	public void setNodeDimensions(OWLObject node, int width, int height) {
		NodeObj obj = nodes.get(node);
		obj.setWidth(width);
		obj.setHeight(height);
	}

	private class DummyEdge {
		private NodeObj layoutChild;

		private NodeObj layoutParent;

		private Collection<DummyEdge> componentEdges = new LinkedList<DummyEdge>();

		private boolean isDummy;

		public Collection<DummyEdge> getComponentEdges() {
			return componentEdges;
		}

		public DummyEdge(NodeObj child, NodeObj parent) {
			this(child, parent, false);
		}

		public DummyEdge(NodeObj child, NodeObj parent, boolean isDummy) {
			setChildParent(child, parent);
			this.isDummy = isDummy;
		}

		public boolean isDummy() {
			return isDummy;
		}

		public NodeObj getLayoutChild() {
			return layoutChild;
		}

		public NodeObj getLayoutParent() {
			return layoutParent;
		}

		public void setChildParent(NodeObj child, NodeObj parent) {
			this.layoutChild = child;
			this.layoutParent = parent;
		}

		@Override
		public String toString() {
			return layoutChild + "-" + layoutParent;
		}

		@Override
		public int hashCode() {
			return 31 * layoutChild.hashCode() + layoutParent.hashCode();
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof DummyEdge) {
				DummyEdge edge = (DummyEdge) o;
				return edge.getLayoutChild().equals(layoutChild)
				&& edge.getLayoutParent().equals(layoutParent);
			} else {
				return false;
			}
		}
	}
}
