package owltools.gfx;

import java.awt.image.*;
import java.awt.*;

/**
 * Adapted from QuickGO
 */
public abstract class ImageRender {

	protected int width;
	protected int height;

	protected ImageRender(int width, int height) {
	    this.width = width;
	    this.height = height;
	}

	public RenderedImage render() {

        BufferedImage image = prepare();
        final Graphics2D g2 = image.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(Color.white);

        g2.fillRect(0, 0, width, height);

        g2.setColor(Color.black);

        render(g2);

        return image;
    }

    protected abstract void render(Graphics2D g2);


    protected BufferedImage prepare() {
        return new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
    }
}
