package org.bbop.piccolo;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.activities.PActivity;

public interface Morpher {
	public PActivity morph(PNode before, PNode after, long duration);
}
