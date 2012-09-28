package org.bbop.piccolo;

import edu.umd.cs.piccolo.PNode;

public class PLayoutNode extends PNode {

	// generated
	private static final long serialVersionUID = -3219117365039827453L;

	protected PiccoloLayoutManager layoutManager;

	@Override
	protected void layoutChildren() {
		if (layoutManager != null) {
			layoutManager.layoutChildren(this);
		}
	}

	public PiccoloLayoutManager getLayoutManager() {
		return layoutManager;
	}

	public void setLayoutManager(PiccoloLayoutManager layoutManager) {
		this.layoutManager = layoutManager;
	}
}
