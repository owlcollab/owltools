package owltools.gfx;


import uk.ac.ebi.interpro.graphdraw.*;

import java.awt.*;
import java.awt.geom.*;

import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLQuantifiedProperty;

/**
 * Represents an edge in a layout graph
 * 
 * Adapted from QuickGO
 * 
 * @author cjm
 *
 */
public final class OWLGraphStrokeEdge extends StrokeEdge<OWLGraphLayoutNode>  {
// this class is final due to a direct != check on the class in the equals method. 
	
    private static final Stroke relationStroke = new BasicStroke(2f);
	private static final Shape arrow = StrokeEdge.standardArrow(8,6,2);

    OWLGraphEdge owlGraphEdge;
    RelationType relType;

    public OWLGraphStrokeEdge(OWLGraphLayoutNode parent, OWLGraphLayoutNode child, OWLGraphEdge oge, OWLGraphWrapper graph) {
        //super(parent, child, Color.black, relationStroke, (rtype.polarity == OWLGraphEdge.Polarity.POSITIVE || rtype.polarity == OWLGraphEdge.Polarity.BIPOLAR) ? arrow : null, (rtype.polarity == OWLGraphEdge.Polarity.NEGATIVE || rtype.polarity == OWLGraphEdge.Polarity.BIPOLAR) ? arrow : null);
    	super(parent, child, Color.black, relationStroke,
    			arrow,null);
    	this.owlGraphEdge = oge;
    	relType = getRelationType(oge, graph);
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
     * @param owlGraphEdge 
     * @param graph 
     * @return relationtype or null
     */
    public static RelationType getRelationType(OWLGraphEdge owlGraphEdge, OWLGraphWrapper graph) {
     	OWLQuantifiedProperty qp = owlGraphEdge.getSingleQuantifiedProperty();
     	if (qp.isSubClassOf()) {
     		return RelationType.ISA;
     	}
     	
     	String id = graph.getIdentifier(qp.getProperty());
     	if ("BFO:0000050".equals(id) || "part_of".equals(id)) {
			return RelationType.PARTOF;
		}
     	else if ("RO:0002212".equals(id) || "negatively_regulates".equals(id)) {
     		return RelationType.NEGATIVEREGULATES;
     	}
     	else if ("RO:0002213".equals(id) || "positively_regulates".equals(id)) {
     		return RelationType.POSITIVEREGULATES;
     	}
     	else if ("RO:0002211".equals(id) || "regulates".equals(id)) {
     		return RelationType.REGULATES;
     	}
     	else if ("BFO:0000051".equals(id) || "has_part".equals(id)) {
     		return RelationType.HASPART;
     	}
     	
     	String s = graph.getLabelOrDisplayId(qp.getProperty());
     	
     	// multiple layers of indirection - first we map the relation obo ID to
     	// the hardcoded list of relation types in QuickGO. 
       	if (s.contains("part_of")) {
       		return RelationType.PARTOF;
    	}
     	else if (s.contains("has_part")) {
     		return RelationType.HASPART;
    	}
     	else if (s.contains("develops_from")) {
     		return RelationType.DEVELOPSFROM;
    	}
     	return null;
    }

    public static class SVGEdge {
        public final String svgPath;
        public final String colour;

        SVGEdge(Shape route, String color) {

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
            this.colour = color;
        }

        private String nf(double location) {
            return String.valueOf(Math.round(location));
        }
    }

    public Object serialise() {
	    return new SVGEdge(route, getColourCode(colour));
    }

    private String getColourCode(Color color) {
        //return StringUtils.encodeHex((byte)color.getRed(), (byte)color.getGreen(), (byte)color.getBlue());
    	return "aaaaaa";
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((owlGraphEdge == null) ? 0 : owlGraphEdge.hashCode());
		result = prime * result + ((relType == null) ? 0 : relType.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		OWLGraphStrokeEdge other = (OWLGraphStrokeEdge) obj;
		if (owlGraphEdge == null) {
			if (other.owlGraphEdge != null) {
				return false;
			}
		}
		else if (!owlGraphEdge.equals(other.owlGraphEdge)) {
			return false;
		}
		if (relType != other.relType) {
			return false;
		}
		return true;
	}
}
