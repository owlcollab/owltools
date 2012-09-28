package org.bbop.graph.tooltip;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;

import javax.swing.Timer;

import org.apache.log4j.Logger;
import org.bbop.graph.RelayoutListener;
import org.bbop.gui.GraphCanvas;
import org.bbop.gui.ViewBehavior;
import org.bbop.piccolo.WordBubbleNode;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.activities.PActivity;
import edu.umd.cs.piccolo.activities.PActivity.PActivityDelegate;
import edu.umd.cs.piccolo.event.PBasicInputEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.util.PPickPath;

public class TooltipBehavior implements ViewBehavior {

	//initialize logger
	protected final static Logger logger = Logger.getLogger(TooltipBehavior.class);

	public static long DEFAULT_TOOLTIP_VISIBILITY_DELAY = 1000;

	protected long tooltipVisibleDelay = DEFAULT_TOOLTIP_VISIBILITY_DELAY;
	boolean tooltipFlareVisible;
	boolean tooltipBubbleVisible;

	protected class TooltipEventHandler extends PBasicInputEventHandler {
		protected PNode lastEntered;

		protected TooltipFactory lastTooltipFactory;

		protected PNode currentTooltip;

		protected WordBubbleNode tooltipHolder;

		protected boolean tooltipFlareVisible;

		public boolean isTooltipFlareVisible() {
			return tooltipFlareVisible;
		}

		public void setTooltipFlareVisible(boolean tooltipFlareVisible) {
			this.tooltipFlareVisible = tooltipFlareVisible;
		}

		protected Point2D lastMousePos;

		protected ActionListener timerListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showTooltip();
			}
		};

		@Override
		public void mouseMoved(PInputEvent event) {
			lastMousePos = event.getPosition();
		}

		public void hideTooltip() {
			hideTooltip(currentTooltip, tooltipHolder);
		}

		public void showTooltip() {
			currentTooltip = lastTooltipFactory.getTooltip(lastEntered);
			if (currentTooltip != null && lastMousePos != null) {
				Point2D pointsAt = canvas.getCamera().viewToLocal(lastMousePos);
				tooltipHolder = new WordBubbleNode();
				//logger.debug("TooltipBehavior: showToolTip(): tooltipFlareVisible = " + tooltipFlareVisible);
				tooltipHolder.setContents(currentTooltip);
				tooltipHolder.setPointsAt(pointsAt);
				tooltipHolder.setBubbleOffset(new Point2D.Double(0, canvas
						.getCamera().getHeight()
						- tooltipHolder.getBubbleBounds().getHeight()));
				tooltipHolder.setTooltipFlareVisible(tooltipFlareVisible);
				canvas.getCamera().addChild(tooltipHolder);
				final PActivity a = tooltipHolder
				.animateScaleInFromPoint(getTooltipFadeInTime());
				canvas.getCamera().addActivity(a);

			}
		}

		public void hideTooltip(final PNode destroyedTooltip,
				final PNode destroyedHolder) {
			if (destroyedHolder != null) {
				PActivity activity = destroyedHolder.animateToTransparency(0, getTooltipFadeOutTime());
				activity.setDelegate(new PActivityDelegate() {
					@Override
					public void activityFinished(PActivity arg0) {
						if (canvas.getCamera().getChildrenReference().contains(destroyedHolder))
							canvas.getCamera().removeChild(destroyedHolder);
					}

					@Override
					public void activityStarted(PActivity activity) {
					}

					@Override
					public void activityStepped(PActivity activity) {
					}
				});
			}
		}

		Timer popupTimer = new Timer(Integer.MAX_VALUE, timerListener);
		{
			popupTimer.setRepeats(false);
		}

		@Override
		public void mouseEntered(PInputEvent event) {
			popupTimer.stop();
			hideTooltip(currentTooltip, tooltipHolder);
			PPickPath path = event.getPath();
			lastEntered = null;
			lastTooltipFactory = null;
			for (int i = path.getNodeStackReference().size() - 1; i >= 0; i--) {
				PNode node = (PNode) path.getNodeStackReference().get(i);
				TooltipFactory tf = (TooltipFactory) node
				.getAttribute(TooltipFactory.KEY);
				if (tf != null) {
					lastEntered = node;
					lastTooltipFactory = tf;
					long delay = tf.getDelay();
					popupTimer.setInitialDelay((int) Math.min(delay,
							getTooltipVisibleDelay()));

					popupTimer.start();
					return;
				}
			}
			// PNode node = event.getPickedNode();
		}

		@Override
		public void mouseExited(PInputEvent event) {
			popupTimer.stop();
			hideTooltip(currentTooltip, tooltipHolder);
		}


	}

	protected TooltipEventHandler tooltipHandler = new TooltipEventHandler();

	protected RelayoutListener relayoutListener = new RelayoutListener() {
		@Override
		public void relayoutComplete() {
		}

		@Override
		public void relayoutStarting() {
			tooltipHandler.hideTooltip();
		}
	};

	protected GraphCanvas canvas;

	public boolean isTooltipFlareVisible() {

		return tooltipFlareVisible;
	}

	public void setTooltipFlareVisible(boolean tooltipFlareVisible) {
		tooltipHandler.setTooltipFlareVisible(tooltipFlareVisible);
		this.tooltipFlareVisible = tooltipFlareVisible;
	}

	@Override
	public void install(GraphCanvas canvas) {
		this.canvas = canvas;
		canvas.addRelayoutListener(relayoutListener);
		canvas.addInputEventListener(tooltipHandler);
	}

	@Override
	public void uninstall(GraphCanvas canvas) {
		if (this.canvas != null) {
			this.canvas.removeInputEventListener(tooltipHandler);
			this.canvas.removeRelayoutListener(relayoutListener);
		}
		this.canvas = null;
	}

	public long getTooltipFadeInTime() {
		return canvas.getLayoutDuration();
	}

	public long getTooltipFadeOutTime() {
		long time = canvas.getLayoutDuration() / 10;
		return Math.max(time, 1);
	}

	public long getTooltipVisibleDelay() {
		return tooltipVisibleDelay;
	}

	public void setTooltipVisibleDelay(long tooltipVisibleDelay) {
		this.tooltipVisibleDelay = tooltipVisibleDelay;
	}





}