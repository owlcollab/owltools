package org.bbop.piccolo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PInputManager;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.PRoot;
import edu.umd.cs.piccolo.activities.PActivityScheduler;
import edu.umd.cs.piccolo.util.PDebug;
import edu.umd.cs.piccolo.util.PNodeFilter;

public class ExtensibleRoot extends PRoot {

	// generated
	private static final long serialVersionUID = -8769239779941873240L;
	
	private PInputManager defaultInputManager;
	private transient List<PRoot.InputSource> inputSources;
	private transient long globalTime;
	private PActivityScheduler activityScheduler;
	protected boolean noAnimations = false;
	protected boolean disableUpdates = false;

	public void setNoAnimations(boolean noAnimations) {
		this.noAnimations = noAnimations;
	}

	/**
	 * This interfaces is for advanced use only. If you want to implement a
	 * different kind of input framework then Piccolo provides you can hook it
	 * in here.
	 */
	public static interface InputSource {
		public void processInput();
	}

	/**
	 * Construct a new PRoot(). Note the PCanvas already creates a basic scene
	 * graph for you so often you will not need to construct your own roots.
	 */
	public ExtensibleRoot() {
		super();
		inputSources = new ArrayList<PRoot.InputSource>();
		globalTime = System.currentTimeMillis();
		activityScheduler = createActivityScheduler();
		defaultInputManager = getDefaultInputManager();
	}

	protected PActivityScheduler createActivityScheduler() {
		return new PActivityScheduler(this);
	}

	public void setActivityScheduler(PActivityScheduler scheduler) {
		this.activityScheduler = scheduler;
	}

	@Override
	public void addInputSource(PRoot.InputSource inputSource) {
		inputSources.add(inputSource);
		firePropertyChange(PROPERTY_CODE_INPUT_SOURCES, PROPERTY_INPUT_SOURCES,
				null, inputSources);
	}

	@Override
	public PInputManager getDefaultInputManager() {
		if (defaultInputManager == null) {
			defaultInputManager = new PInputManager();
			ExtensibleRoot.this.addInputSource(defaultInputManager);
		}
		return defaultInputManager;
	}

	protected PInputManager createInputManager() {
		return new PInputManager();
	}

	/**
	 * Get the activity scheduler associated with this root.
	 */
	@Override
	public PActivityScheduler getActivityScheduler() {
		return activityScheduler;
	}

	/**
	 * Wait for all scheduled activities to finish before returning from this
	 * method. This will freeze out user input, and so it is generally
	 * recommended that you use PActivities.setTriggerTime() to offset
	 * activities instead of using this method.
	 */
	@Override
	public void waitForActivities() {
		PNodeFilter cameraWithCanvas = new PNodeFilter() {
			@Override
			public boolean accept(PNode aNode) {
				return (aNode instanceof PCamera)
						&& (((PCamera) aNode).getComponent() != null);
			}

			@Override
			public boolean acceptChildrenOf(PNode aNode) {
				return true;
			}
		};

		while (getActivityScheduler().getActivitiesReference().size() > 0) {
			processInputs();
			Iterator<?> i = getAllNodes(cameraWithCanvas, null).iterator();
			while (i.hasNext()) {
				PCamera each = (PCamera) i.next();
				each.getComponent().paintImmediately();
			}
		}
	}

	/**
	 * Advanced. If you want to remove the default input source from the roots
	 * UI process you can do that here. You will seldom do this unless you are
	 * making additions to the piccolo framework.
	 */
	@Override
	public void removeInputSource(PRoot.InputSource inputSource) {
		inputSources.remove(inputSource);
		firePropertyChange(PROPERTY_CODE_INPUT_SOURCES, PROPERTY_INPUT_SOURCES,
				null, inputSources);
	}

	// ****************************************************************
	// UI Loop - Methods for running the main UI loop of Piccolo.
	// ****************************************************************

	/**
	 * Get the global Piccolo time. This is set to System.currentTimeMillis() at
	 * the beginning of the roots <code>processInputs</code> method.
	 * Activities should usually use this global time instead of System.
	 * currentTimeMillis() so that multiple activities will be synchronized.
	 */
	@Override
	public long getGlobalTime() {
		return globalTime;
	}

	public void setDisableUpdates(boolean disableUpdates) {
		this.disableUpdates = disableUpdates;
		if (disableUpdates == false)
			processInputs();
	}

	/**
	 * This is the heartbeat of the Piccolo framework. Pending input events are
	 * processed. Activities are given a chance to run, and the bounds caches
	 * and any paint damage is validated.
	 */
	@Override
	public void processInputs() {
		if (disableUpdates)
			return;
		PDebug.startProcessingInput();
		processingInputs = true;

		globalTime = System.currentTimeMillis();
		List<PRoot.InputSource> l = inputSources;
		int count = l == null ? 0 : l.size();
		for (int i = 0; i < count; i++) {
			PRoot.InputSource each = l.get(i);
			each.processInput();
		}

		long time;
		if (noAnimations)
			time = Long.MAX_VALUE;
		else
			time = globalTime;
		try {
		activityScheduler.processActivities(time);
		} catch (Throwable t) {
			t.printStackTrace();
		}
		validateFullBounds();
		validateFullPaint();

		processingInputs = false;
		PDebug.endProcessingInput();
	}

}
