package org.bbop.piccolo;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import edu.umd.cs.piccolo.activities.PActivity;
import edu.umd.cs.piccolo.activities.PActivityScheduler;
import edu.umd.cs.piccolo.activities.PInterpolatingActivity;

import org.apache.log4j.*;

public class PCompoundActivity extends PInterpolatingActivity {

	//initialize logger
	protected final static Logger logger = Logger.getLogger(PCompoundActivity.class);

	protected List<PActivity> activityList = new LinkedList<PActivity>();

	protected List<Runnable> startupList = new LinkedList<Runnable>();

	protected List<Runnable> finishList = new LinkedList<Runnable>();
	
	protected List<Runnable> immediateList = new LinkedList<Runnable>();

	protected boolean filibuster = false;
	
	public Exception startupException = new Exception();

	public PCompoundActivity() {
		super(0, 0);
	}
	
	@Override
	public void setSlowInSlowOut(boolean isSlowInSlowOut) {
		for(PActivity activity : activityList) {
			if (activity instanceof PInterpolatingActivity)
			((PInterpolatingActivity) activity).setSlowInSlowOut(isSlowInSlowOut);
		}
		super.setSlowInSlowOut(isSlowInSlowOut);
	}
	
	@Override
	public void setActivityScheduler(PActivityScheduler aScheduler) {
		super.setActivityScheduler(aScheduler);
		for(Runnable r: immediateList)
			r.run();
	}
	
	public void addImmediateAction(Runnable r) {
		immediateList.add(r);
	}

	public void removeImmediateAction(Runnable r) {
		immediateList.remove(r);
	}

	public void addStartAction(Runnable r) {
		startupList.add(r);
	}

	public void removeStartAction(Runnable r) {
		startupList.remove(r);
	}

	public void addFinishAction(Runnable r) {
		finishList.add(r);
	}

	public void removeFinishAction(Runnable r) {
		finishList.remove(r);
	}

	public void addActivity(PActivity activity) {
		activityList.add(activity);
	}

	public void removeActivity(PActivity activity) {
		activityList.add(activity);
	}

	@Override
	protected void activityStarted() {
		for (Runnable r : startupList)
			r.run();
		Iterator<PActivity> it = activityList.iterator();
		while (it.hasNext()) {
			PActivity activity = it.next();
			getActivityScheduler().addActivity(activity);
		}
		super.activityStarted();
	}

	@Override
	protected void activityFinished() {
		super.activityFinished();
		for (Runnable r : finishList)
			r.run();
	}

	@Override
	public long getStopTime() {
		if (filibuster)
			return Long.MAX_VALUE;
		else
			return super.getStopTime();
	}

	@Override
	public long processStep(long currentTime) {
		if (isStepping()) {
			filibuster = false;
			boolean childIsActive = false;
			for (PActivity activity : activityList) {
				if (getActivityScheduler().getActivitiesReference().contains(
						activity)) {
					childIsActive = true;
					break;
				}
			}
			if (childIsActive)
				filibuster = true;
		} else
			filibuster = true;
		return super.processStep(currentTime);
	}

	@Override
	public long getDuration() {
		long out = 0;
		Iterator<PActivity> it = activityList.iterator();
		while (it.hasNext()) {
			PActivity activity = it.next();
			out = Math.max(out, activity.getDuration());
		}
		return out;
	}

	@Override
	public void terminate(int terminationBehavior) {
		Iterator<PActivity> it = activityList.iterator();
		while (it.hasNext()) {
			PActivity activity = it.next();
			activity.terminate(terminationBehavior);
		}
		super.terminate(terminationBehavior);
	}

}
