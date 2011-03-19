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
 * 
 * Adapted from QuickGO (David Binns and Tony Sawford, EBI)
 * @author cjm
 *
 */
public class OWLGraphLayoutRenderer {

	protected OWLGraphWrapper owlGraphWrapper;

	protected StandardGraph<OWLGraphLayoutNode, OWLGraphStrokeEdge> g = 
		new StandardGraph<OWLGraphLayoutNode, OWLGraphStrokeEdge>();
	GraphStyle style;
	HierarchicalLayout.Orientation orientation = HierarchicalLayout.Orientation.TOP;
	Stroke thinStroke = new BasicStroke(1);
	Stroke fatStroke = new BasicStroke(3);
	HashMap<OWLObject, OWLGraphLayoutNode> nodemap = new HashMap<OWLObject,OWLGraphLayoutNode>();

	Shape parent=OWLGraphStrokeEdge.standardArrow(10,8,0);
	Shape child=OWLGraphStrokeEdge.standardArrow(10,8,5);


	public OWLGraphLayoutRenderer(OWLGraphWrapper owlGraphWrapper) {
		super();
		this.owlGraphWrapper = owlGraphWrapper;
	}
	
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
	
	public OWLGraphStrokeEdge makeEdge(OWLGraphEdge e) {
		OWLGraphLayoutNode sn = getNode(e.getSource());
		OWLGraphLayoutNode tn = getNode(e.getTarget());
		//System.out.println("adding:"+e+" "+n1+"//"+n2);
		
		OWLQuantifiedProperty qr = e.getSingleQuantifiedProperty();
		Color color = Color.RED;
		if (qr.isSubClassOf()) {
			color = Color.GREEN;
		}
		OWLGraphStrokeEdge ge = 
			new OWLGraphStrokeEdge(tn, sn, e);	
//		new OWLGraphStrokeEdge(n1, n2, color, fatStroke,parent,child);	
		return ge;
	}

        public void addEdge(OWLGraphEdge e) {
            g.edges.add(makeEdge(e));
        }

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
	
	public void addObject(OWLObject focusObj) {
		Set<OWLObject> ancObjs = owlGraphWrapper.getAncestors(focusObj);
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


	/**
	 * generates both html and a png
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
			System.out.println("EDGE:"+edge);
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



