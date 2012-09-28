package org.bbop.piccolo;

import java.awt.Color;
import java.awt.geom.Point2D;

import org.apache.log4j.Logger;
import org.bbop.util.EndpointShapeExtender;
import org.bbop.util.ShapeExtender;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.activities.PInterpolatingActivity;
import edu.umd.cs.piccolo.nodes.PPath;

public class DefaultMorpher implements Morpher {

	//initialize logger
	protected final static Logger logger = Logger.getLogger(DefaultMorpher.class);

	protected PNode newNodeOriginNode = null;

	protected PNode deadNodeDestNode = null;

	protected PNode deadNodeOriginNode = null;

	protected PNode newNodeDestNode = null;

	protected ShapeExtender extender = new EndpointShapeExtender();

	protected Point2D scratchPoint = new Point2D.Double();

	@Override
	public PCompoundActivity morph(final PNode oldNode, final PNode newNode,
			long duration) {
		PCompoundActivity relayoutActivity = new PCompoundActivity();
		if (newNode == null) {
			if (deadNodeDestNode != null) {
				scratchPoint.setLocation(deadNodeDestNode.getX()
						- deadNodeDestNode.getFullBoundsReference().getWidth()
						/ 2, deadNodeDestNode.getY()
						- deadNodeDestNode.getFullBoundsReference().getHeight()
						/ 2);
				PInterpolatingActivity effect = oldNode
						.animateToPositionScaleRotation(scratchPoint.getX(),
								scratchPoint.getY(), .001, 0, duration);
				relayoutActivity.addActivity(effect);
			} else {
				PInterpolatingActivity effect = oldNode.animateToTransparency(
						0, duration);
				relayoutActivity.addActivity(effect);
			}
		} else if (oldNode == null) {
			if (newNodeOriginNode != null) {

				scratchPoint.setLocation(newNodeOriginNode.getXOffset()
						- newNodeOriginNode.getFullBoundsReference().getWidth()
						/ 2, newNodeOriginNode.getYOffset()
						- newNodeOriginNode.getFullBoundsReference()
								.getHeight() / 2);
				PInterpolatingActivity effect = newNode
						.animateToPositionScaleRotation(newNode.getXOffset(),
								newNode.getYOffset(), 1, 0, duration);
				newNode.setScale(.01);
				newNode.setOffset(scratchPoint);
				newNode.moveToBack();
				relayoutActivity.addActivity(effect);
			} else {
				newNode.setTransparency(0);
				PInterpolatingActivity effect = newNode.animateToTransparency(
						1, duration);
				relayoutActivity.addActivity(effect);
			}
		} else {
			if (newNode instanceof PPath && oldNode instanceof PPath) {
				relayoutActivity.addActivity(new MorphActivity((PPath) oldNode,
						((PPath) newNode).getPathReference(), extender,
						duration));
			} else {
//				logger.info("don't known how to morph "+newNode+" to "+oldNode);
			}
			relayoutActivity.addActivity(oldNode
					.animateToPositionScaleRotation(newNode.getXOffset(),
							newNode.getYOffset(), newNode.getScale(), newNode
									.getRotation(), duration));
			if (newNode.getPaint() != null && newNode.getPaint() instanceof Color
					&& oldNode.getPaint() != null && oldNode.getPaint() instanceof Color) {
				relayoutActivity.addActivity(oldNode.animateToColor(
						(Color) newNode.getPaint(), duration));
			} else if (newNode.getPaint() != null) {
				oldNode.setPaint(newNode.getPaint());
			} else if (oldNode.getPaint() != null) {
				oldNode.setPaint(null);
			}
			relayoutActivity.addActivity(new PCompoundActivity() {
				@Override
				protected void activityStarted() {
					super.activityStarted();
					if (oldNode instanceof PPath && newNode instanceof PPath) {
						((PPath) oldNode).setStroke(((PPath) newNode)
								.getStroke());
						((PPath) oldNode).setStrokePaint(((PPath) newNode)
								.getStrokePaint());
					}
				}
			});
		}
		return relayoutActivity;
	}

	public ShapeExtender getExtender() {
		return extender;
	}

	public void setExtender(ShapeExtender extender) {
		this.extender = extender;
	}

	public PNode getDeadNodeDestNode() {
		return deadNodeDestNode;
	}

	public void setDeadNodeDestNode(PNode deadNodeDestNode) {
		this.deadNodeDestNode = deadNodeDestNode;
	}

	public PNode getNewNodeOriginNode() {
		return newNodeOriginNode;
	}

	public void setNewNodeOriginNode(PNode newNodeOriginNode) {
		this.newNodeOriginNode = newNodeOriginNode;
	}

	public PNode getDeadNodeOriginNode() {
		return deadNodeOriginNode;
	}

	public void setDeadNodeOriginNode(PNode deadNodeOriginNode) {
		this.deadNodeOriginNode = deadNodeOriginNode;
	}

	public PNode getNewNodeDestNode() {
		return newNodeDestNode;
	}

	public void setNewNodeDestNode(PNode newNodeDestNode) {
		this.newNodeDestNode = newNodeDestNode;
	}

}
