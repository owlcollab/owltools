package owltools.gfx;


import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import uk.ac.ebi.interpro.graphdraw.HierarchicalLayout;
import uk.ac.ebi.interpro.graphdraw.StandardGraph;

/**
 * Render a portion of an ontology using the QuickGO graphics code
 * 
 * This should be capable of rendering relationships between any OWLObjects - as well as
 * class hierarchies, also object property hierarchies.
 * 
 * In contrast to some rendering engines, it shows all relationships, not just subclass.
 * See {@link owltools.graph} for a description of how an OWLOntology is viewed as an OWLGraph
 * 
 * Adapted from QuickGO (David Binns and Tony Sawford, EBI)
 * @author cjm
 *
 */
public class OWLGraphLayoutRenderer {

	protected OWLGraphWrapper owlGraphWrapper;
	public GraphicsConfig graphicsConfig = new GraphicsConfig();

	protected StandardGraph<OWLGraphLayoutNode, OWLGraphStrokeEdge> g = 
		new StandardGraph<OWLGraphLayoutNode, OWLGraphStrokeEdge>();
	
	final GraphStyle style;
	HierarchicalLayout.Orientation orientation = HierarchicalLayout.Orientation.TOP;
	Stroke thinStroke = new BasicStroke(1);
	Stroke fatStroke = new BasicStroke(3);
	Map<OWLObject, OWLGraphLayoutNode> nodemap = new HashMap<OWLObject,OWLGraphLayoutNode>();
	Set<RelationType> relationTypes = new HashSet<RelationType>();
	
	Shape parent=OWLGraphStrokeEdge.standardArrow(10,8,0);
	Shape child=OWLGraphStrokeEdge.standardArrow(10,8,5);


	
	/**
	 * Create a new renderer with default graph style.
	 * 
	 * @param owlGraphWrapper
	 */
	public OWLGraphLayoutRenderer(OWLGraphWrapper owlGraphWrapper) {
		this(owlGraphWrapper, new GraphStyle());
	}
	
	/**
	 * @param owlGraphWrapper
	 * @param style
	 */
	public OWLGraphLayoutRenderer(OWLGraphWrapper owlGraphWrapper, GraphStyle style) {
		super();
		this.owlGraphWrapper = owlGraphWrapper;
		this.style = style;
	}

	/**
	 * fetch a layout node via the cognate OWLObject
	 * 
	 * make private?
	 * @param ob
	 * @return {@link OWLGraphLayoutNode}
	 */
	public OWLGraphLayoutNode getNode(OWLObject ob) {
		OWLGraphLayoutNode node = nodemap.get(ob);
		if (node == null) {
			node = new OWLGraphLayoutNode(owlGraphWrapper, ob, style);
			nodemap.put(ob, node);
			g.nodes.add(node);
		}
		return node;
	}

	/**
	 * Make a layout edge from a OWLGraphEdge
	 * 
	 * make private?
	 * @param e
	 * @return OWLGraphStrokeEdge
	 */
	public OWLGraphStrokeEdge makeEdge(OWLGraphEdge e) {
		OWLGraphLayoutNode sn = getNode(e.getSource());
		OWLGraphLayoutNode tn = getNode(e.getTarget());

		OWLGraphStrokeEdge ge = new OWLGraphStrokeEdge(tn, sn, e, owlGraphWrapper);	
		//		new OWLGraphStrokeEdge(n1, n2, color, fatStroke,parent,child);	
		relationTypes.add(ge.relType);
		return ge;
	}

	/**
	 * Adds an edge to the graph. Typically this does not need called explicitly.
	 * 
	 * @param e
	 */
	public void addEdge(OWLGraphEdge e) {
		g.edges.add(makeEdge(e));
	}

	/**
	 * Adds all objects from the OGW into the rendered graph. 
	 * This includes also all object properties (i.e. relation types).
	 * If this is not intended use {@link #addAllClasses()} instead.
	 * 
	 * 
	 * CAUTION: do this only for small ontologies
	 */
	public void addAllObjects() {
		addObjectsInternal(owlGraphWrapper.getAllOWLObjects());
	}
	
	/**
	 * Adds all classes from the OGW into the rendered graph
	 * 
	 * CAUTION: do this only for small ontologies
	 */
	public void addAllClasses() {
		Set<OWLObject> allOWLObjects = owlGraphWrapper.getAllOWLObjects();
		Set<OWLObject> allOWLClasses = new HashSet<OWLObject>();
		for (OWLObject owlObject : allOWLObjects) {
			if (owlObject instanceof OWLClass) {
				allOWLClasses.add(owlObject);
			}
		}
		addObjectsInternal(allOWLClasses);
	}

	/**
	 * Adds an object to the graph to be rendered
	 * 
	 * This will also add all ancestors of the focusObj, and all direct edges
	 * from each ancestor
	 * 
	 * @param focusObj
	 */
	public void addObject(OWLObject focusObj) {
		Set<OWLObject> ancObjs = owlGraphWrapper.getNamedAncestors(focusObj);
		ancObjs.add(focusObj);
		addObjectsInternal(ancObjs);
	}

	public void addObjects(Set<OWLObject> objs) {
		Set<OWLObject> allObjects = new HashSet<OWLObject>();
		for (OWLObject owlObject : objs) {
			Set<OWLObject> ancestors = owlGraphWrapper.getNamedAncestors(owlObject);
			if (ancestors != null) {
				allObjects.addAll(ancestors);
			}
		}
		addObjectsInternal(allObjects);
	}
	
	private void addObjectsInternal(Collection<OWLObject> objs) {
		for (OWLObject ob : objs) {
			getNode(ob); // creates the nodes
		}
		for (OWLObject ob : objs) {
			for (OWLGraphEdge e : owlGraphWrapper.getOutgoingEdges(ob)) {
				OWLGraphStrokeEdge edge = makeEdge(e);
				g.edges.add(edge);
			}
		}
	}


	/**
	 * generates both HTML and a PNG
	 * 
	 * @param folder targetFolder for the output files 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void renderHTML(File folder) throws FileNotFoundException, IOException {
		PrintWriter pw = new PrintWriter(new FileWriter(new File(folder, "hierarchicalGraph.html")));
		FileOutputStream fos = new FileOutputStream(new File(folder, "hierarchicalGraph"+orientation+".png"));
		String imageMapString = renderImage("png", fos);
		pw.println("<html><body>");
		pw.println("<img src='hierarchicalGraph"+orientation+".png' usemap='#bob' /><map name='bob'>" + imageMapString + "</map>");

		pw.println("</body></html>");
		pw.close();

	}

	/**
	 * renders the graph in the specified format
	 * 
	 * @param fmt - see ImageIO
	 * @param fos
	 * @return imageMap - TODO currently an empty string, no map is provided
	 * @throws IOException
	 */
	public String renderImage(String fmt, OutputStream fos) throws IOException {


		HierarchicalLayout<OWLGraphLayoutNode, OWLGraphStrokeEdge> layout =
			new HierarchicalLayout<OWLGraphLayoutNode, OWLGraphStrokeEdge>(g, orientation);
		layout.horizontalMargin=2;
		layout.verticalMargin=5;
		layout.edgeLengthHeightRatio=5;
		/*
		layout.betweenLevelExtraGap=20;
		layout.edgeLengthHeightRatio=1;
		 */
		layout.layout();

		final int width = layout.getWidth();
		final int height = layout.getHeight();
		ImageRender hierarchyImage = new HierarchyImage(width, height, g.nodes, g.edges, style, relationTypes);
		RenderedImage image = hierarchyImage.render();
		ImageIO.write(image, fmt, fos);
		return "";

	}


}



