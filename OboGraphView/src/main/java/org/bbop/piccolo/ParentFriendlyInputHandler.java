package org.bbop.piccolo;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PBasicInputEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.util.PBounds;

/**
 * Version of PBasicInputEventHandler that provides mouseEnteredFullBounds and
 * mouseExitedFullBounds methods. These methods are only called if the mouse
 * enters or exits the full bounds of the object, which tends to work better for
 * components that have pickable nested elements.
 * 
 * @author jrichter
 * 
 */
import org.apache.log4j.*;

public class ParentFriendlyInputHandler extends PBasicInputEventHandler {

	//initialize logger
	protected final static Logger logger = Logger.getLogger(ParentFriendlyInputHandler.class);

	public PNode attachedNode;
	protected boolean isInside = false;

	public ParentFriendlyInputHandler(PNode attachedNode) {
		this.attachedNode = attachedNode;
	}

	@Override
	public void mouseEntered(PInputEvent event) {
		if (!isInside && event.getPath().acceptsNode(attachedNode)) {
			isInside = true;
			mouseEnteredFullBounds(event);
		}
	}

	public void mouseEnteredFullBounds(PInputEvent event) {
	}

	@Override
	public void mouseExited(PInputEvent event) {
		if (!getFullBounds(attachedNode).contains(event.getPosition())) {
			isInside = false;
			mouseExitedFullBounds(event);
		}
	}
	
	public void mouseExitedFullBounds(PInputEvent event) {		
	}

	public PBounds getFullBounds(PNode node) {
		return node.getFullBoundsReference();
	}
}
