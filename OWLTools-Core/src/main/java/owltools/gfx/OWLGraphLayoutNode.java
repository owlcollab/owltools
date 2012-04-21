package owltools.gfx;

import uk.ac.ebi.interpro.graphdraw.*;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphWrapper;

public class OWLGraphLayoutNode implements Node, LayoutNode {
    public Font font;

    public String label;
    public String id;

    public int x;
    public int y;
    public int width;
    public int height;
    private int hmargin=1;
    public int topLine=0;

    int[] colours=new int[0];
    
    private final GraphStyle style;
	private final OWLObject owlObject;
   
    public OWLGraphLayoutNode(OWLGraphWrapper owlGraphWrapper, OWLObject owlObject) {
    	this(owlGraphWrapper, owlObject, new GraphStyle());
    }  

    public OWLGraphLayoutNode(OWLGraphWrapper owlGraphWrapper, OWLObject owlObject, GraphStyle style) {
        this.owlObject = owlObject;
		this.style = style == null ? new GraphStyle() : style;
    	if (owlObject == null) {
    		label = "??";
    		return;
    	}
    	String label;
    	if (owlObject instanceof OWLNamedObject)
    		label = owlGraphWrapper.getLabelOrDisplayId(owlObject);
    	else
    		label = owlObject.getClass().toString();
    	if (label == null)
    		label = "?";
    	label = label.replace('_', ' ');
    	
        this.label = label;
        this.id = owlGraphWrapper.getIdentifier(owlObject);
        
        height=this.style.height;
        width=this.style.width;
        font=this.style.getLabelFont();

    }
    
	/**
	 * @return the owlObject
	 */
	public OWLObject getOwlObject() {
		return owlObject;
	}

	@Override
	public int getWidth() {return width;}
	
    @Override
	public int getHeight() {return height;}
    
    @Override
	public void setLocation(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int left() {return x-width/2;}
    public int right() {return x+width/2;}
    public int top() {return y-height/2;}
    public int bottom() {return y+height/2;}

    public Color fill=Color.white;

    public Color line=Color.black;
    public Stroke border=new BasicStroke(1);
    public Color idColour=new Color(192,0,0);


    public void render(Graphics2D g2) {
        String text=label;

        g2.setFont(font);

        g2.setColor(fill);
        g2.fillRect(x-width/2, y-height/2, width, height);

        g2.setColor(line);
        g2.setStroke(border);
        g2.drawRect(x-width/2, y-height/2, width, height);
        for (int i = 0; i < colours.length; i++) {
            g2.setColor(new Color(colours[i]));
            g2.fillRect(x - width / 2+i*10+1, y + height / 2-1,10,2);
        }
        g2.setColor(line);

        FontMetrics fm = g2.getFontMetrics();

        reflow(text, fm, g2);

        int ypos=y-yheight/2+topLine;
        for (Line line : lines) {
            line.draw(g2, x,ypos);
            ypos+=line.height();
        }
    }


    public void renderID(Graphics2D g2) {
        g2.setColor(idColour);

        FontMetrics fm = g2.getFontMetrics();
        Rectangle2D r= fm.getStringBounds(id,g2);       
        g2.drawString(id,(float)(right()-r.getWidth()),(float)(top()-r.getMinY()));
    }

    static class Line {
        String text;
        Rectangle2D bounds;
        private Graphics2D g2;
        private FontMetrics fm;
        private Font f;


        public Line(Graphics2D g2, FontMetrics fm) {

            this.g2 = g2;
            this.f=fm.getFont();
            this.fm = fm;
        }

        boolean fit(String text,int from,int to,int width) {
            String t = text.substring(from,to);

            Rectangle2D r= fm.getStringBounds(t,g2);
            if (r.getWidth()>width) return false;

            this.text=t;
            this.bounds=r;

            return true;
        }

        public int length() {return text==null?0:text.length();}


        public void draw(Graphics2D g2,int x,int y) {
            //double ascent = -bounds.getMinY();
            double ascent=f.getSize2D();
            g2.drawString(text,(float)(x-bounds.getWidth()/2-bounds.getMinX()), (float) (y+ ascent));
        }

        public int height() {
            return f.getSize();//int) bounds.getHeight();
        }
    }

    List<Line> lines=new ArrayList<Line>();
    int yheight;

    private void reflow( String text, FontMetrics fm, Graphics2D g2) {
        int start=0;
        lines.clear();
        yheight=topLine;
        while (start<text.length()) {
            Line current=new Line(g2, fm);
            int end = start;
            while (end<=text.length() && current.fit(text,start,end,width-hmargin*2)) end=nextSpace(text,end+1);

            if (current.length()==0) {
                end=start;
                while (end<=text.length() && current.fit(text,start,end,width-hmargin*2)) end++;
            }

            if (current.length()==0) break;
            if (yheight+current.height()>=height) break;

            yheight+=current.height();
            lines.add(current);
            start+=current.length();
            while (start < text.length() && text.charAt(start) == ' ') start++;
        }
    }

    private int nextSpace(String text,int from) {
        if (from>text.length()) return from;
        int attempt=text.indexOf(" ",from);
        if (attempt==-1) attempt=text.length();
        return attempt;
    }



/*

    private boolean textFits(FontMetrics fm, String text, int start, int attempt, Graphics2D g2) {
        Rectangle2D r= fm.getStringBounds(text.substring(start,attempt),g2);
        if (r.getWidth()>width-hmargin*2) return true;
        end=attempt;
        bounds=r;
        return false;
    }
*/

    public class SVGRectangle {
        public int left=x-width/2;
        public int top=y-height/2;
        public int right=x+width/2;
        public int bottom=y+height/2;
    }

    public Object serialise() {return new SVGRectangle();}    
}
