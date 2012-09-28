package org.bbop.piccolo;

import java.awt.geom.AffineTransform;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.activities.PTransformActivity;
import edu.umd.cs.piccolo.util.PAffineTransform;
import edu.umd.cs.piccolo.util.PUtil;

import org.apache.log4j.*;

public class PositionScaleRotationActivity extends PTransformActivity {

	//initialize logger
	protected final static Logger logger = Logger.getLogger(PositionScaleRotationActivity.class);

	protected double xoffset;
	protected double yoffset;
	protected double scale;
	protected double rotation;
	protected PNode node;

	public PositionScaleRotationActivity(PNode node, double xoffset,
			double yoffset, double scale, double rotation, long duration) {
		super(duration, PUtil.DEFAULT_ACTIVITY_STEP_RATE, getTarget(node));
		this.node = node;
		this.xoffset = xoffset;
		this.yoffset = yoffset;
		this.scale = scale;
		this.rotation = rotation;
	}

	protected static Target getTarget(final PNode node) {
		return new PTransformActivity.Target() {
			@Override
			public void setTransform(AffineTransform aTransform) {
				node.setTransform(aTransform);
			}

			@Override
			public void getSourceMatrix(double[] aSource) {
				node.getTransformReference(true).getMatrix(aSource);
			}
		};
	}

	@Override
	protected void activityStarted() {
		double [] matrix = new double[6];
		PAffineTransform t = node.getTransform();
		t.setOffset(xoffset, yoffset);
		t.setScale(scale);
		t.setRotation(rotation);
		t.getMatrix(matrix);
		setDestinationTransform(matrix);
		logger.info("animating "+node+" from ("+node.getXOffset()+","+node.getYOffset()+","+node.getScale()+","+node.getRotation()+") to ("+xoffset+","+yoffset+","+scale+","+rotation+")");
		super.activityStarted();
	}
}
