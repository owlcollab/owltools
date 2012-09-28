package org.bbop.graph;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.activities.PActivity;

public interface NodeDecorator {
	public PActivity decorate(PNode node, boolean noAnimation);
	
	public boolean onlyDecorateAfterLayout();

}
