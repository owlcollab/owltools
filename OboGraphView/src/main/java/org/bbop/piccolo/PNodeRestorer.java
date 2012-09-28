package org.bbop.piccolo;

import java.io.Serializable;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.activities.PActivity;

public interface PNodeRestorer extends Serializable {
	public void init();
	public void cleanup();
	public void restoreState(PNode node, PNode clone);
	public PActivity animateRestoreState(PNode node, PNode clone, long duration);
}
