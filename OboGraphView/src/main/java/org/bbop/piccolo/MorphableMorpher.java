package org.bbop.piccolo;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.activities.PActivity;

import org.apache.log4j.*;

public class MorphableMorpher extends DefaultMorpher {

	//initialize logger
	protected final static Logger logger = Logger.getLogger(MorphableMorpher.class);
	@Override
	public PCompoundActivity morph(PNode oldNode, PNode newNode, long duration) {

		if (oldNode instanceof Morphable) {
			PCompoundActivity out;
			if (((Morphable) oldNode).doDefaultMorph())
				out = super.morph(oldNode, newNode, duration);
			else
				out = new PCompoundActivity();
			PActivity activity = ((Morphable) oldNode).morphTo(newNode,
					duration);
			out.addActivity(activity);
			return out;
		} else
			return super.morph(oldNode, newNode, duration);
	}
}
