package org.bbop.piccolo;

import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PCanvas;
import edu.umd.cs.piccolo.PLayer;
import edu.umd.cs.piccolo.PRoot;

public class ExtensibleCanvas extends PCanvas {

	// generated
	private static final long serialVersionUID = -9134134837054668274L;

	@Override
	protected PCamera createDefaultCamera() {
		PRoot r = createRoot();
		PLayer l = createLayer();
		PCamera c = createCamera();
		
		r.addChild(c); 
		r.addChild(l); 
		c.addLayer(l);
		
		return c;	
	}
	
	protected PRoot createRoot() {
		return new ExtensibleRoot();
	}
	
	protected PLayer createLayer() {
		return new PLayer();
	}
	
	protected PCamera createCamera() {
		return new PCamera();
	}
}
