package org.bbop.graph;

import edu.umd.cs.piccolo.PCanvas;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.event.PPanEventHandler;

import org.apache.log4j.*;

public class SingleCameraPanHandler extends PPanEventHandler {

	//initialize logger
	protected final static Logger logger = Logger.getLogger(SingleCameraPanHandler.class);

	@Override
	protected void pan(PInputEvent e) {
		if (isDefaultCamera(e)) {
			super.pan(e);
		}
	}

	protected boolean isDefaultCamera(PInputEvent e) {
		return e.getComponent() instanceof PCanvas
				&& ((PCanvas) e.getComponent()).getCamera().equals(
						e.getCamera());
	}

	@Override
	protected void dragActivityStep(PInputEvent e) {
		if (isDefaultCamera(e)) {
			super.dragActivityStep(e);
		}
	}
}
