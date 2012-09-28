package org.bbop.piccolo;

import java.io.Serializable;

import edu.umd.cs.piccolo.PNode;

public interface PiccoloLayoutManager extends Serializable {
	public void layoutChildren(PNode node);
}
