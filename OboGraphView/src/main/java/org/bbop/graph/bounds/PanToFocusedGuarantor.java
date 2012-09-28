package org.bbop.graph.bounds;

import java.awt.geom.Rectangle2D;

import org.apache.log4j.Logger;
import org.bbop.graph.OELink;
import org.bbop.graph.OENode;
import org.bbop.gui.GraphCanvas;

import edu.umd.cs.piccolo.PNode;

public class PanToFocusedGuarantor extends BoundsGuarantorCycleState {

	//initialize logger
	protected final static Logger logger = Logger.getLogger(PanToFocusedGuarantor.class);

	public PanToFocusedGuarantor(GraphCanvas canvas) {
		setDesc("Pan to focused node");
		setCanvas(canvas);
	}

	@Override
	public boolean getZoom() {
		return false;
	}
	
	public PNode getFocusedNode() {
		PNode node = canvas.getFocusedNode();
		if (node == null)
			return null;
		if (canvas.isLayingOut()) {
			
			PNode layoutNode = null;
			if (node instanceof OENode) {
				layoutNode = canvas.getFinalLayoutVersion(((OENode) node).getObject());
			} else if (node instanceof OELink)
				layoutNode = canvas.getFinalLayoutVersion(((OELink) node).getObject());
			if (layoutNode != null)
				node = layoutNode;
		}
		return node;
	}
	
	@Override
	public Rectangle2D getNewBounds() {
		if (getFocusedNode() == null)
			return null;
		return getFocusedNode().getFullBounds();
	}

}
