package org.bbop.piccolo;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Dimension2D;
import java.awt.image.BufferedImage;

import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PDimension;
import edu.umd.cs.piccolo.util.PPaintContext;
import edu.umd.cs.piccolo.util.PPickPath;

public class PZNodeCache extends PNode {

	// generated
	private static final long serialVersionUID = -5084916084218596598L;
	
	protected PCamera camera;
	protected transient Image imageCache;
	protected boolean validatingCache;

	/**
	 * Override this method to customize the image cache creation process. For
	 * example if you want to create a shadow effect you would do that here.
	 * Fill in the cacheOffsetRef if needed to make your image cache line up
	 * with the nodes children.
	 */
	public Image createImageCache(Dimension2D cacheOffsetRef) {
		try {
			return toImage();
		} catch (Exception ex) {
			return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		}
	}
	
	public Image getImageCache() {
		if (imageCache == null) {
			PDimension cacheOffsetRef = new PDimension();
			validatingCache = true;
			resetBounds();
			imageCache = createImageCache(cacheOffsetRef);
			PBounds b = getFullBoundsReference();
			setBounds(b.getX() + cacheOffsetRef.getWidth(), b.getY()
					+ cacheOffsetRef.getHeight(), imageCache.getWidth(null),
					imageCache.getHeight(null));
			validatingCache = false;
		}
		return imageCache;
	}

	public void setCamera(PCamera camera) {
		this.camera = camera;
	}

	public void invalidateCache() {
		imageCache = null;
	}

	@Override
	public void invalidatePaint() {
		if (!validatingCache) {
			super.invalidatePaint();
		}
	}

	@Override
	public void repaintFrom(PBounds localBounds, PNode childOrThis) {
		if (!validatingCache) {
			super.repaintFrom(localBounds, childOrThis);
			invalidateCache();
		}
	}

	@Override
	public void fullPaint(PPaintContext paintContext) {
		if (validatingCache || (camera != null && camera.getViewScale() > 1)) {
			super.fullPaint(paintContext);
		} else {
			Graphics2D g2 = paintContext.getGraphics();
			g2.drawImage(getImageCache(), (int) getX(), (int) getY(), null);
		}
	}

	@Override
	protected boolean pickAfterChildren(PPickPath pickPath) {
		return false;
	}
}
