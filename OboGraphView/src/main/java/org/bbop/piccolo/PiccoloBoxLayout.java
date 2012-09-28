package org.bbop.piccolo;

import java.util.Iterator;

import edu.umd.cs.piccolo.PNode;

public class PiccoloBoxLayout implements PiccoloLayoutManager {

	// generated
	private static final long serialVersionUID = 3808155897105771262L;

	public enum Orientation {
		HORZ,
		VERT,
//		LEFT_ALIGN,
//		RIGHT_ALIGN,
//		CENTER_ALIGN
	}

	protected Orientation orientation;
	protected double gap;

	public PiccoloBoxLayout(Orientation orientation) {
		setOrientation(orientation);
	}

	public void setOrientation(Orientation orientation) {
		this.orientation = orientation;
	}

	public void setGap(double gap) {
		this.gap = gap;
	}
	
	public static PNode createBox(Orientation orientation) {
		PLayoutNode node = new PLayoutNode();
		node.setLayoutManager(new PiccoloBoxLayout(orientation));
		return node;
	}

	@Override
	public void layoutChildren(PNode parent) {
		double currentSize = 0;
		double maxHeight = 0;
		double maxWidth = 0;
		Iterator<?> it = parent.getChildrenIterator();
		while (it.hasNext()) {
			PNode node = (PNode) it.next();
			maxWidth = Math.max(node.getFullBoundsReference().getWidth(),
					maxWidth);
			maxHeight = Math.max(node.getFullBoundsReference().getHeight(),
					maxHeight);
		}

		it = parent.getChildrenIterator();
		boolean first = true;
		while (it.hasNext()) {
			PNode child = (PNode) it.next();
			if (first) {
				first = false;
			} else {
				currentSize += gap;
			}
			if (orientation == Orientation.HORZ) {
				child.setOffset(currentSize, (maxHeight - child
						.getFullBoundsReference().getHeight()) / 2);
				currentSize += child.getFullBoundsReference().getWidth();
			} else {
				child.setOffset((maxWidth - child.getFullBoundsReference()
						.getWidth()) / 2, currentSize);
				currentSize += child.getFullBoundsReference().getHeight();
			}
		}
	}
}
