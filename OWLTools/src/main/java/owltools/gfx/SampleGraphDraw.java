package owltools.gfx;


import javax.imageio.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import uk.ac.ebi.interpro.graphdraw.*;

/**
 * Draw an example graph.
 * <p/>
 * After running see: <a href="../../../../../../hierarchicalGraph.html">results</a>.
 */
public class SampleGraphDraw {


    public static void main(String[] args) throws IOException {


        StandardGraph<RectangularNode, StrokeEdge<RectangularNode>> g = new StandardGraph<RectangularNode, StrokeEdge<RectangularNode>>();

        Stroke thinStroke = new BasicStroke(1);
        Stroke fatStroke = new BasicStroke(3);


        RectangularNode n1 = new RectangularNode(100, 30, "N1", "http://www.google.com/?q=N1", null, "Node1", Color.RED, Color.BLACK, thinStroke);
        RectangularNode n2 = new RectangularNode(100, 50, "N2", "http://www.google.com/?q=N2", null, "Node2", Color.CYAN, Color.BLACK, thinStroke);
        RectangularNode n3 = new RectangularNode(100, 30, "N3", "http://www.google.com/?q=N3", null, "Node3", Color.YELLOW, Color.BLACK, thinStroke);
        RectangularNode n4 = new RectangularNode(100, 30, "N4", "http://www.google.com/?q=N4", null, "Node4", Color.GREEN, Color.BLACK, thinStroke);

        g.nodes.add(n1);
        g.nodes.add(n2);
        g.nodes.add(n3);
        g.nodes.add(n4);

        Shape parent=StrokeEdge.standardArrow(10,8,0);
        Shape child=StrokeEdge.standardArrow(10,8,5);

        g.edges.add(new StrokeEdge<RectangularNode>(n1, n2, Color.RED, fatStroke,parent,child));
        g.edges.add(new StrokeEdge<RectangularNode>(n1, n3, Color.BLUE, fatStroke,parent,child));
        g.edges.add(new StrokeEdge<RectangularNode>(n1, n4, Color.GREEN, fatStroke,parent,child));
        g.edges.add(new StrokeEdge<RectangularNode>(n2, n4, Color.ORANGE, fatStroke,parent,child));

        PrintWriter pw = new PrintWriter(new FileWriter("hierarchicalGraph.html"));
        pw.println("<html><body>");

        for (HierarchicalLayout.Orientation orientation : HierarchicalLayout.Orientation.values()) {

            HierarchicalLayout<RectangularNode, StrokeEdge<RectangularNode>> layout =
                    new HierarchicalLayout<RectangularNode, StrokeEdge<RectangularNode>>(g, orientation);
            layout.betweenLevelExtraGap=20;
            layout.edgeLengthHeightRatio=1;
            layout.layout();

            final int width = layout.getWidth();
            final int height = layout.getHeight();
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

            final Graphics2D g2 = image.createGraphics();

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(Color.white);

            g2.fillRect(0, 0, width, height);

            g2.setColor(Color.black);

            for (StrokeEdge<RectangularNode> edge : g.edges) {
                edge.render(g2);
            }

            StringBuilder sb = new StringBuilder();

            for (RectangularNode node : g.nodes) {
                node.render(g2);
                sb.append(node.getImageMap());
            }

            ImageIO.write(image, "png", new FileOutputStream("hierarchicalGraph"+orientation+".png"));


            pw.println("<img src='hierarchicalGraph"+orientation+".png' usemap='#bob' /><map name='bob'>" + sb.toString() + "</map>");

        }
        pw.println("</body></html>");
        pw.close();
    }
}



