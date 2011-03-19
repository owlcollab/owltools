package owltools.gfx;

import java.awt.*;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.HashMap;

/**
 * hacked crudely from QuickGO
 *
 */
public class GraphStyle {
    public int fontSize=11;
    public boolean termIds=false;
    public boolean slimColours=true;
    public boolean fill=true;
    public int height=55;
    public int width=85;
    public boolean key=true;
    private Font nameFont;
    public String fontName="Arial";
    public GraphStyle() {
    
    }
    	 
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

    Font getFont() {
        if (nameFont==null) nameFont =new Font(fontName,Font.PLAIN,fontSize);
        System.out.println("font="+nameFont);
        return nameFont;
    }

    public static void main(String[] args) {
        new GraphStyle(new HashMap<String, String>()).test();
    }

    private void test() {
        Graphics2D g2 = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).createGraphics();
        
        fontSize=12;
        g2.setFont(getFont());
        FontMetrics fm=g2.getFontMetrics();
        AffineTransform tf = g2.getFontRenderContext().getTransform();
        System.out.println("Ascent: "+fm.getAscent());
        System.out.println("Descent: "+fm.getDescent());
        System.out.println("Height: "+fm.getHeight());
        System.out.println("XForm "+tf.getScaleX()+" "+tf.getScaleY());
        Rectangle2D r=fm.getStringBounds("Hello",g2);
        LineMetrics lm=nameFont.getLineMetrics("Hello",g2.getFontRenderContext());
        System.out.println(r.getMinX()+"-"+r.getMaxX()+" "+r.getMinY()+"-"+r.getMaxY());
        System.out.println(lm.getAscent()+" "+lm.getDescent());
    }
}
