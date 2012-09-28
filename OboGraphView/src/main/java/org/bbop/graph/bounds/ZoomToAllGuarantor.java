package org.bbop.graph.bounds;

import java.awt.geom.Rectangle2D;

import org.bbop.gui.GraphCanvas;

public class ZoomToAllGuarantor extends BoundsGuarantorCycleState {

	public ZoomToAllGuarantor(GraphCanvas canvas) {
		setDesc("Zoom to all");
		setCanvas(canvas);
	}
	
	@Override
	public Rectangle2D getNewBounds() {
		if (canvas.isLayingOut()) {
			return canvas.getNewLayer().getFullBoundsReference();
		}
		return canvas.getLayer().getFullBoundsReference();
	}
}
