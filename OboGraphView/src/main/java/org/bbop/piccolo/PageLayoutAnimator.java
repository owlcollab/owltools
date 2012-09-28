package org.bbop.piccolo;

import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bbop.util.ShapeExtender;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.activities.PActivity;
import edu.umd.cs.piccolo.nodes.PPath;

public class PageLayoutAnimator extends PLayoutNode {

	// generated
	private static final long serialVersionUID = 1962672469529167501L;

	protected Map<PNode, PNode> objectToCloneMap = new HashMap<PNode, PNode>();
	protected Map<PNode, PNode> cloneToObjectMap = new HashMap<PNode, PNode>();
	
	@Override
	public void addChild(PNode node) {
		PNode clone = cloneAndFile(node);
		super.addChild(clone);
	}
	
	public PNode getSurrogate(PNode original) {
		return objectToCloneMap.get(original);
	}
	
	protected PNode cloneAndFile(PNode node) {
		List<?> childList = new LinkedList<Object>(node.getChildrenReference());
		PNode clone = (PNode) node.clone();
		clone.removeAllChildren();
		
		for (Object object : childList) {
			PNode child = (PNode) object;
			clone.addChild(cloneAndFile(child));
		}
		objectToCloneMap.put(node, clone);
		cloneToObjectMap.put(clone, node);
		return clone;
	}
	
	public PActivity animateToLayout(Collection<PNode> objects, ShapeExtender shapeExtender, long duration) {
		layoutChildren();
		Point2D myOffset = getOffset();
		setOffset(0,0);
		PCompoundActivity out = new PCompoundActivity();
		for (PNode node : objects) {
			PNode clone = objectToCloneMap.get(node);
			Point2D newCoords = globalToLocal(clone.getGlobalTranslation());
			double newScale = clone.getGlobalScale() / getGlobalScale();
			double newRotate = clone.getGlobalRotation() - getGlobalRotation();
			out.addActivity(node.animateToPositionScaleRotation(newCoords.getX()+myOffset.getX(), newCoords.getY()+myOffset.getY(), newScale, newRotate, duration));
			out.addActivity(node.animateToTransparency(clone.getTransparency(), duration));
			if (node instanceof PPath) {
				out.addActivity(new MorphActivity((PPath) node, ((PPath) clone).getPathReference(), shapeExtender, duration));
			}
		}
		setOffset(myOffset);
		return out;
	}
}
