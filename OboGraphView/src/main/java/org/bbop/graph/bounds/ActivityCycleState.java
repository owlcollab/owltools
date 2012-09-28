package org.bbop.graph.bounds;

import org.bbop.gui.GraphCanvas;
import org.bbop.util.CycleState;

import edu.umd.cs.piccolo.activities.PActivity;

public abstract class ActivityCycleState implements CycleState {

	protected GraphCanvas canvas;
	protected String desc;
	protected PActivity activity;

	@Override
	public boolean alwaysActivate() {
		return false;
	}

	public int getTerminationMode() {
		return PActivity.TERMINATE_WITHOUT_FINISHING;
	}

	@Override
	public synchronized void apply() {
		if (activity != null) {
			activity.terminate(getTerminationMode());
		}
		activity = getActivity();
		if (activity != null) {
			canvas.getRoot().addActivity(activity);
		}
	}

	public abstract PActivity getActivity();

	@Override
	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	@Override
	public void halt() {
		if (activity != null) {
			activity.terminate(getTerminationMode());
		}
	}

	public GraphCanvas getCanvas() {
		return canvas;
	}

	public void setCanvas(GraphCanvas canvas) {
		this.canvas = canvas;
	}
}
