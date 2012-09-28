package org.bbop.piccolo;

import java.awt.Shape;
import java.awt.geom.GeneralPath;

import org.apache.log4j.Logger;
import org.bbop.util.ShapeExtender;
import org.bbop.util.ShapeMorpher;

import edu.umd.cs.piccolo.activities.PInterpolatingActivity;
import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.util.PUtil;

public class MorphActivity extends PInterpolatingActivity {

	//initialize logger
	protected final static Logger logger = Logger.getLogger(MorphActivity.class);

	
	
	protected Shape morphTo;
	protected ShapeExtender shapeExtender;

	public MorphActivity(PPath node, Shape morphTo, ShapeExtender shapeExtender, long duration) {
		super(duration, PUtil.DEFAULT_ACTIVITY_STEP_RATE);
		this.node = node;
		this.morphTo = morphTo;
		this.shapeExtender = shapeExtender;
	}
	
	protected ShapeMorpher morpher;
	protected PPath node;
	protected GeneralPath scratch = new GeneralPath();
	
	@Override
	protected void activityStarted() {
		morpher = new ShapeMorpher(node.getPathReference(), morphTo, 5, 2, shapeExtender);
		super.activityStarted();
	}

	@Override
	public void setRelativeTargetValue(float zeroToOne) {				
		Shape shape = morpher.getShapeAtTime(zeroToOne, scratch);
		node.getPathReference().reset();
		node.getPathReference().append(shape, false);
		node.updateBoundsFromPath();
	}

	@Override
	public boolean isAnimation() {
		return true;
	}
}
