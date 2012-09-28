package org.bbop.piccolo;

import org.bbop.util.EndpointShapeExtender;
import org.bbop.util.ShapeExtender;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.activities.PActivity;
import edu.umd.cs.piccolo.nodes.PPath;

public class DefaultNodeRestorer implements PNodeRestorer {

	// generated
	private static final long serialVersionUID = 8314305572917539407L;
	
	protected ShapeExtender shapeExtender = new EndpointShapeExtender();

	@Override
	public PActivity animateRestoreState(PNode fromState, PNode toState, long duration) {
		if (fromState == null || toState == null)
			return null;
		PCompoundActivity out = new PCompoundActivity();

		out.addActivity(new PositionScaleRotationActivity(fromState, toState.getXOffset(),
				toState.getYOffset(), toState.getScale(), toState.getRotation(),
				duration));
		out.addActivity(fromState.animateToTransparency(toState.getTransparency(),
				duration));
		if (fromState instanceof PPath) {
			out.addActivity(new MorphActivity((PPath) fromState, ((PPath) toState)
					.getPathReference(), shapeExtender, duration));
		}
		return out;
	}

	@Override
	public void cleanup() {
	}

	@Override
	public void init() {
	}

	@Override
	public void restoreState(PNode node, PNode clone) {
		node.setOffset(clone.getOffset());
		node.setRotation(clone.getRotation());
		node.setScale(clone.getScale());
		node.setTransparency(clone.getTransparency());
		if (node instanceof PPath) {
			((PPath) node).setPathTo(((PPath) clone).getPathReference());
		}
	}

	public ShapeExtender getShapeExtender() {
		return shapeExtender;
	}

	public void setShapeExtender(ShapeExtender shapeExtender) {
		this.shapeExtender = shapeExtender;
	}

}
