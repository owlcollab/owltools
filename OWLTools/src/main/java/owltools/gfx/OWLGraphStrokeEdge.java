package owltools.gfx;


import uk.ac.ebi.interpro.graphdraw.*;

import java.awt.*;
import java.awt.geom.*;

import org.semanticweb.owlapi.model.OWLObjectProperty;

import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLQuantifiedProperty;

/**
 * Represents an edge in a layour graph
 * 
 * Adapted from QuickGO
 * 
 * @author cjm
 *
 */
public class OWLGraphStrokeEdge extends StrokeEdge<OWLGraphLayoutNode>  {

    private static final Stroke relationStroke = new BasicStroke(2f);
//    private static final Shape parentArrow = null; //StrokeEdge.standardArrow(8,6,2);
//    private static final Shape childArrow = null;
	private static final Shape arrow = StrokeEdge.standardArrow(8,6,2);

    OWLGraphEdge owlGraphEdge;
    RelationType relType;

    public OWLGraphStrokeEdge(OWLGraphLayoutNode parent, OWLGraphLayoutNode child, OWLGraphEdge oge) {
        //super(parent, child, Color.black, relationStroke, (rtype.polarity == OWLGraphEdge.Polarity.POSITIVE || rtype.polarity == OWLGraphEdge.Polarity.BIPOLAR) ? arrow : null, (rtype.polarity == OWLGraphEdge.Polarity.NEGATIVE || rtype.polarity == OWLGraphEdge.Polarity.BIPOLAR) ? arrow : null);
    	super(parent, child, Color.black, relationStroke,
    			arrow,null);
    	this.owlGraphEdge = oge;
    	setRelationType();
    	System.out.println("RT="+relType);
    	if (relType != null)
    		this.colour = relType.color;
    	
    	//Shape childArrow;
		//this.setChildArrow(childArrow);
    	// TODO - use dashed lines or make configurable
    	// need to store link to GraphicsConfig object
    	
       	if (oge.getFinalQuantifiedProperty().isInferred()) {
    		this.colour = new Color(255, 0 ,0);
    	}
    }
    
    /**
     * Sets the value of relType based on OWLGraphEdge properties
     * 
     * TODO : this is too hacky. Make this a soft configuration, e.g. 
     * an ontology with color properties
     * 
     */
    public void setRelationType() {
     	OWLQuantifiedProperty qp = owlGraphEdge.getSingleQuantifiedProperty();
     	if (qp.isSubClassOf()) {
     		relType = RelationType.ISA;
     		return;
     	}
     	String pid = qp.getPropertyId();
     	//System.out.println("PID="+pid+" // "+qp);
     	// multiple layers of indirection - first we map the relation obo ID to
     	// the hardcoded list of relation types in QuickGO. 
       	if (pid.contains("part_of")) {
    		relType = RelationType.PARTOF;
    	}
     	else if (pid.contains("has_part")) {
    		relType = RelationType.HASPART;
    	}
     	else if (pid.contains("develops_from")) {
    		relType = RelationType.DEVELOPSFROM;
    	}
    }

    public class SVGEdge {
        public String svgPath;
        public String colour;

        SVGEdge() {

            PathIterator pi = route.getPathIterator(null);
            double[] locations = new double[6];
            final StringBuilder svgPathSB = new StringBuilder();
            while (!pi.isDone()) {
                int type = pi.currentSegment(locations);
                pi.next();
                switch (type) {
                    case PathIterator.SEG_MOVETO:
                        svgPathSB.append(" M ").append(nf(locations[0])).append(" ").append(nf(locations[1]));
                        break;
                    case PathIterator.SEG_LINETO:
                        svgPathSB.append(" L ").append(nf(locations[0])).append(" ").append(nf(locations[1]));
                        break;
                    case PathIterator.SEG_CUBICTO:
                        svgPathSB.append(" C ").append(nf(locations[0])).append(" ").append(nf(locations[1]))
                                .append(" ").append(nf(locations[2])).append(" ").append(nf(locations[3]))
                                .append(" ").append(nf(locations[4])).append(" ").append(nf(locations[5]));
                        break;
                }

            }

            svgPath = svgPathSB.toString();
            colour = getColourCode(OWLGraphStrokeEdge.this.colour);
        }

        private String nf(double location) {
            return String.valueOf(Math.round(location));
        }
    }

    public Object serialise() {
	    return new SVGEdge();
    }

    private String getColourCode(Color color) {
        //return StringUtils.encodeHex((byte)color.getRed(), (byte)color.getGreen(), (byte)color.getBlue());
    	return "aaaaaa";
    }

  }
