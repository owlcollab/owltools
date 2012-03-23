package owltools.gfx;


import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;


import uk.ac.ebi.interpro.graphdraw.Node;
import uk.ac.ebi.interpro.graphdraw.StrokeEdge;

/**
 * Adapted from QuickGO
 */
public class HierarchyImage extends ImageRender {

	public Collection<OWLGraphLayoutNode> terms = new ArrayList<OWLGraphLayoutNode>();
    public Collection<OWLGraphStrokeEdge> relations = new ArrayList<OWLGraphStrokeEdge>();
	public Collection<KeyNode> legend = new ArrayList<KeyNode>();

    private GraphStyle style;

    public String selected;
    public final String errorMessage;

    public String id() {
        return String.valueOf(System.identityHashCode(this));
    }

    public static final int keyMargin = 50;
    public static final int rightMargin = 10;
    public static final int bottomMargin = 16;
    public static final int minWidth = 250;

    public HierarchyImage(String errorMessage) {
        super(500, 100);
        this.errorMessage = errorMessage;
    }

    public HierarchyImage(int width, int height, Collection<OWLGraphLayoutNode> terms, Collection<OWLGraphStrokeEdge> relations, GraphStyle style, Set<RelationType> types) {
        super(Math.max(minWidth, width + (style.key ? keyMargin + (style.width * 2) + rightMargin : 0)), height + bottomMargin);

        this.errorMessage = null;

        this.terms = terms;
        this.relations = relations;
        this.style = style;

        if (style.key) {
			int knHeight = style.height / 2;
			int knY = knHeight;

	        int yMax = knY;
	        for (RelationType rt : types) {
	            KeyNode kn = new KeyNode(style, super.width - style.width - rightMargin, knY, style.width * 2, knHeight, rt);
				legend.add(kn);
		        knY += knHeight;
		        yMax = kn.bottom();
	        }

			int bottom = yMax + bottomMargin;
			if (this.height < bottom) {
				this.height = bottom;
			}
        }
    }

    @Override
	protected void render(Graphics2D g2) {
        if (errorMessage != null) {
            g2.setFont(style.getErrorFont());
            g2.setColor(Color.BLACK);
            g2.drawString(errorMessage, 5, 50);
        }
		else {
	        for (OWLGraphStrokeEdge relation : relations) {
	            relation.render(g2);
	        }
	        for (OWLGraphLayoutNode term : terms) {
	            term.render(g2);
	        }
		    for (KeyNode ke : legend) {
			    ke.render(g2);
		    }

	        g2.setFont(style.getInfoFont());
	        g2.setColor(Color.BLACK);
        }
    }

	static class KeyNode implements Node {
		private final int xCentre;
		private final int yCentre;
		private final int width;
		private final int height;
		
		private final RelationType relType;
		private final GraphStyle style;
	
		public KeyNode(GraphStyle style, int xCentre, int yCentre, int width, int height, RelationType relType) {
			this.xCentre = xCentre;
			this.yCentre = yCentre;
			this.width = width;
			this.height = height;
			this.relType = relType;
			this.style = style;
		}
	
		Stroke border = new BasicStroke(1);
	
		public void render(Graphics2D g2) {
			int margin = height / 10;
			int boxSide = height - (2 * margin);
			int offsetY = boxSide / 4;
			new RelationStroke(xCentre + (width / 2) - boxSide - (2 * margin), yCentre + offsetY, xCentre - (width / 2) + boxSide + (2 * margin), yCentre + offsetY, relType).render(g2);
	
			int left = xCentre - (width / 2) + margin;
			int top = yCentre - (height / 2) + margin;
			drawBox(g2, left, top, boxSide, boxSide, "A");
			left +=  (width - margin - boxSide);
			drawBox(g2, left, top, boxSide, boxSide, "B");
	
			g2.setFont(style.getLabelFont());
			Rectangle2D r = g2.getFontMetrics().getStringBounds(relType.description, g2);
			g2.drawString(relType.description, (float)(xCentre - (r.getWidth() / 2)), (float)(yCentre + offsetY - (r.getHeight() / 2)));
		}
	
		void drawBox(Graphics2D g2, int left, int top, int width, int height, String label) {
			g2.setColor(Color.black);
			g2.setStroke(border);
			g2.drawRect(left, top, width, height);
	
			g2.setFont(style.getLabelFont());
			Rectangle2D r = g2.getFontMetrics().getStringBounds(label, g2);
			g2.drawString(label, (float)(left + (width / 2) - (r.getWidth() / 2)), (float)(top + (height / 2) + (r.getHeight() / 2)));
		}
	
		public int left() {
			return xCentre - width / 2;
		}
		public int right() {
			return xCentre + width / 2;
		}
		public int top() {
			return yCentre - height / 2;
		}
		public int bottom() {
			return yCentre + height / 2;
		}
	
		public String topic() {
			return relType.formalCode;
		}

		static class RelationStroke extends StrokeEdge<Node> {
			private static final Stroke relationStroke = new BasicStroke(2f);
			private static final Shape arrow = StrokeEdge.standardArrow(8,6,2);
		
			RelationType type;
		
			public RelationStroke(int xFrom, int yFrom, int xTo, int yTo, RelationType rtype) {
			    super(null, null, Color.black, relationStroke, (rtype.polarity == RelationType.Polarity.POSITIVE || rtype.polarity == RelationType.Polarity.BIPOLAR) ? arrow : null, (rtype.polarity == RelationType.Polarity.NEGATIVE || rtype.polarity == RelationType.Polarity.BIPOLAR) ? arrow : null);
			    this.type = rtype;
			    this.colour = rtype.color;
		
				GeneralPath shape = new GeneralPath();
				shape.moveTo(xFrom, yFrom);
				shape.lineTo(xTo, yTo);
				this.setRoute(shape);
			}
		}
	}
}
