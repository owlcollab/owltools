package org.bbop.graph.bounds;

import java.awt.geom.Rectangle2D;

import org.bbop.gui.GraphCanvas;

public class ZoomToFocusedGuarantor extends BoundsGuarantorCycleState {

	public ZoomToFocusedGuarantor(GraphCanvas canvas) {
		setDesc("Zoom to focused node");
		setCanvas(canvas);
	}
	
	@Override
	public Rectangle2D getNewBounds() {
		if (canvas.getFocusedNode() == null)
			return null;
		return canvas.getFocusedNode().getGlobalFullBounds();
	}


}
