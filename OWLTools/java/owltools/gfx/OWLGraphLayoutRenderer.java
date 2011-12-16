package owltools.gfx;


import javax.imageio.*;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObject;

import owltools.gfx.GraphStyle;
import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLQuantifiedProperty;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import uk.ac.ebi.interpro.graphdraw.*;

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
	GraphStyle style;
	HierarchicalLayout.Orientation orientation = HierarchicalLayout.Orientation.TOP;
	Stroke thinStroke = new BasicStroke(1);
	Stroke fatStroke = new BasicStroke(3);
	HashMap<OWLObject, OWLGraphLayoutNode> nodemap = new HashMap<OWLObject,OWLGraphLayoutNode>();

	Shape parent=OWLGraphStrokeEdge.standardArrow(10,8,0);
	Shape child=OWLGraphStrokeEdge.standardArrow(10,8,5);


	/**
	 * @param owlGraphWrapper
	 */
	public OWLGraphLayoutRenderer(OWLGraphWrapper owlGraphWrapper) {
		super();
		this.owlGraphWrapper = owlGraphWrapper;
	}

	/**
	 * fetch a layout node via the cognate OWLObject
	 * 
	 * make private?
	 * @param ob
	 * @return
	 */
	public OWLGraphLayoutNode getNode(OWLObject ob) {
		if (nodemap.containsKey(ob))
			return nodemap.get(ob);
		//IRI iri = ((OWLNamedObject)ob).getIRI();
		OWLGraphLayoutNode node = 
			new OWLGraphLayoutNode(owlGraphWrapper, ob, style);

		/*
		RectangularNode node = 
			new RectangularNode(100, 30, label,
					iri.toString(), null, 
					label, Color.RED, Color.BLACK,
					thinStroke);
		 */
		nodemap.put(ob, node);
		g.nodes.add(node);
		return node;
	}

	/**
	 * Make a layout edge from a OWLGraphEdge
	 * 
	 * make private?
	 * @param e
	 * @return
	 */
	public OWLGraphStrokeEdge makeEdge(OWLGraphEdge e) {
		OWLGraphLayoutNode sn = getNode(e.getSource());
		OWLGraphLayoutNode tn = getNode(e.getTarget());
		//System.out.println("adding:"+e+" "+n1+"//"+n2);

		OWLQuantifiedProperty qr = e.getSingleQuantifiedProperty();

		/*
		// TODO : this isn't actually used yet...
		Color color = Color.RED;
		if (qr.isSubClassOf()) {
			color = Color.GREEN;
		}
		*/
		OWLGraphStrokeEdge ge = 
			new OWLGraphStrokeEdge(tn, sn, e);	
		//		new OWLGraphStrokeEdge(n1, n2, color, fatStroke,parent,child);	
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
	 * Adds all objects from the OGW into the rendered graph
	 * 
	 * CAUTION: do this only for small ontologies
	 * 
	 */
	public void addAllObjects() {
		for (OWLObject ob : owlGraphWrapper.getAllOWLObjects()) {
			OWLGraphLayoutNode node = getNode(ob);
		}
		for (OWLObject ob : owlGraphWrapper.getAllOWLObjects()) {
			for (OWLGraphEdge e : owlGraphWrapper.getOutgoingEdges(ob)) {
				g.edges.add(makeEdge(e));
			}
		}
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
		for (OWLObject ob : ancObjs) {
			OWLGraphLayoutNode node = getNode(ob);
		}
		for (OWLObject ob : ancObjs) {
			for (OWLGraphEdge e : owlGraphWrapper.getOutgoingEdges(ob)) {
				g.edges.add(makeEdge(e));
			}
		}
	}

	public void addObjects(Set<OWLObject> objs) {
		// TODO - make this more efficient
		for (OWLObject obj : objs) {
			addObject(obj);
		}
		
	}


	/**
	 * generates both HTML and a PNG
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void renderHTML() throws FileNotFoundException, IOException {
		PrintWriter pw = new PrintWriter(new FileWriter("hierarchicalGraph.html"));
		FileOutputStream fos = new FileOutputStream("hierarchicalGraph"+orientation+".png");
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
	 * @return imageMap - TODO
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public String renderImage(String fmt, OutputStream fos) throws FileNotFoundException, IOException {


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
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

		final Graphics2D g2 = image.createGraphics();

		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		g2.setColor(Color.white);

		g2.fillRect(0, 0, width, height);

		g2.setColor(Color.black);

		for (OWLGraphStrokeEdge edge : g.edges) {
			//System.out.println("EDGE:"+edge);
			edge.render(g2);
		}

		StringBuilder sb = new StringBuilder();

		for (OWLGraphLayoutNode node : g.nodes) {
			node.render(g2);
			//sb.append(node.getImageMap());
		}
		ImageIO.write(image, fmt, fos);
		return sb.toString();

	}


}



