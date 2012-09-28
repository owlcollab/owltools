package org.bbop.piccolo;

import java.awt.Graphics2D;

import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.util.PPaintContext;

public class FullPaintCamera extends PCamera {

	// generated
	private static final long serialVersionUID = 5405445331211404711L;

	public void paintUnclipped(Graphics2D g) {
		PPaintContext paintContext = new PPaintContext(g);
		paintCameraView(paintContext);
	}
}
