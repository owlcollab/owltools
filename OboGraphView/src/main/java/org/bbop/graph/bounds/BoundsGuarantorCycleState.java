package org.bbop.graph.bounds;

import java.awt.geom.Rectangle2D;

import org.apache.log4j.Logger;
import org.bbop.piccolo.BoundsUtil;

import edu.umd.cs.piccolo.activities.PActivity;

public abstract class BoundsGuarantorCycleState extends ActivityCycleState {

	protected final static Logger logger = Logger.getLogger(BoundsGuarantorCycleState.class);
	
	protected BoundsGuarantor boundsGuarantor;

	public void setBoundsGuarantor(BoundsGuarantor boundsGuarantor) {
		this.boundsGuarantor = boundsGuarantor;
	}

	public boolean getZoom() {
		return true;
	}

	@Override
	public boolean alwaysActivate() {
		return true;
	}

	@Override
	public synchronized void apply() {
		super.apply();
		boundsGuarantor.scheduleBoundsChange(activity);
	}

	@Override
	public PActivity getActivity() {
		Rectangle2D bounds = getNewBounds();
		if (bounds != null)
			if (getZoom())
				return canvas.getCamera().animateViewToCenterBounds(bounds,
						getZoom(), getBoundsDuration());
			else
				return canvas.getCamera().animateViewToPanToBounds(bounds,
						getBoundsDuration());
		return null;
	}

	@Override
	public boolean isActive() {
		Rectangle2D bounds = getNewBounds();
		if (bounds == null) {
			logger.info("Null bounds!");
			return true;
		}
		Rectangle2D camBounds = canvas.getCamera().getViewBounds();
		logger.info("isActive:  CAM BOUNDS = " + camBounds);
		logger.info("isActive: NODE BOUNDS = " + bounds);
		if (getZoom())
			return BoundsUtil
					.snugFit(bounds, camBounds, BoundsUtil.DEFAULT_ERR);
		else
			return camBounds.contains(bounds);
	}

	public abstract Rectangle2D getNewBounds();

	public long getBoundsDuration() {
		return canvas.getLayoutDuration();
	}
}
