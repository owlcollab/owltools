package owltools.gfx;

import java.awt.*;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;

/**
 * hacked crudely from QuickGO
 *
 */
public class GraphStyle {
	
    public int labelFontSize=11;
    public String labelFontName="Arial";
    private volatile Font labelFont;
    
    public int infoFontSize=9;
    public String infoFontName="Arial";
    private volatile Font infoFont;
    
    public int errorFontSize=16;
    public String errorFontName="Arial";
    private volatile Font errorFont;
    
    public boolean termIds=false;
    public boolean fill=true;
    public int height=55;
    public int width=85;
    public boolean key=true;
    
    
    /**
     * Create default style.
     */
    public GraphStyle() {
    	this(Collections.<String, String>emptyMap());
    }
    
    /**
     * Create a custom style
     * 
     * TODO implement configuration options.
     * 
     * @param parameters
     */
    public GraphStyle(Map<String, String> parameters) {
    	/*
        fontName= StringUtils.nvl(parameters.get("font"),fontName);
        fill= StringUtils.parseBoolean(parameters.get("fill"),fill);
        termIds=StringUtils.parseBoolean(parameters.get("ids"),termIds);
        key=StringUtils.parseBoolean(parameters.get("key"),key);
        slimColours=StringUtils.parseBoolean(parameters.get("slim"),slimColours);
        fontSize=StringUtils.parseInt(parameters.get("font"),fontSize);
        width=StringUtils.parseInt(parameters.get("width"),width);
        height=StringUtils.parseInt(parameters.get("height"),height);
*/
        
    }

    Font getLabelFont() {
    	synchronized (this) {
    		if (labelFont==null) {
            	labelFont =new Font(labelFontName,Font.PLAIN,labelFontSize);
            }	
		}
        return labelFont;
    }
    
    Font getInfoFont() {
    	synchronized (this) {
    		if (infoFont==null) {
    			infoFont =new Font(infoFontName,Font.PLAIN,infoFontSize);
            }	
		}
        return infoFont;
    }
    
    Font getErrorFont() {
    	synchronized (this) {
    		if (errorFont==null) {
    			errorFont =new Font(errorFontName,Font.PLAIN,errorFontSize);
            }	
		}
        return errorFont;
    }

    @Deprecated
    public static void main(String[] args) {
        new GraphStyle(new HashMap<String, String>()).test();
    }

    @Deprecated
    private void test() {
        Graphics2D g2 = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).createGraphics();
        
        labelFontSize=12;
        g2.setFont(getLabelFont());
        FontMetrics fm=g2.getFontMetrics();
        AffineTransform tf = g2.getFontRenderContext().getTransform();
        System.out.println("Ascent: "+fm.getAscent());
        System.out.println("Descent: "+fm.getDescent());
        System.out.println("Height: "+fm.getHeight());
        System.out.println("XForm "+tf.getScaleX()+" "+tf.getScaleY());
        Rectangle2D r=fm.getStringBounds("Hello",g2);
        LineMetrics lm=labelFont.getLineMetrics("Hello",g2.getFontRenderContext());
        System.out.println(r.getMinX()+"-"+r.getMaxX()+" "+r.getMinY()+"-"+r.getMaxY());
        System.out.println(lm.getAscent()+" "+lm.getDescent());
    }
}
