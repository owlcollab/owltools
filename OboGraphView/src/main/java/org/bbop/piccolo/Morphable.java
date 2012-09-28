package org.bbop.piccolo;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.activities.PActivity;

public interface Morphable {

	public PActivity morphTo(PNode node, long duration);
	
	public boolean doDefaultMorph();
}
