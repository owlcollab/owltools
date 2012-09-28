package org.bbop.util;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.bbop.util.AbstractTaskDelegate;
import org.bbop.util.TaskDelegate;

import org.apache.log4j.*;

public class BackgroundEventQueue {

	//initialize logger
	protected final static Logger logger = Logger.getLogger(BackgroundEventQueue.class);

	private static int idgen = 0;
	private int id = idgen++;

	protected static class TaskGroup extends AbstractTaskDelegate<Void> {

		protected TaskDelegate<?>[] tasks;

		public TaskGroup(TaskDelegate<?>[] tasks) {
			this.tasks = tasks;
		}

		public TaskDelegate<?>[] getTasks() {
			return tasks;
		}

		@Override
		public void execute() {
		}

		@Override
		public void cancel() {
			super.cancel();
			for (TaskDelegate<?> t : getTasks()) {
				t.cancel();
			}
		}

	}

	protected static class BackgroundEventThread extends Thread {

		protected LinkedList<TaskDelegate<?>> l = new LinkedList<TaskDelegate<?>>();
		protected TaskDelegate<?> currentTask;
		protected List<Runnable> startupNotifiers = new LinkedList<Runnable>();
		protected List<Runnable> sleepNotifiers = new LinkedList<Runnable>();

		protected boolean kill = false;
		protected StackTraceElement[] trace;

		public BackgroundEventThread() {
			setDaemon(true);
			trace = (new Exception()).getStackTrace();
		}

		public void kill() {
			kill = true;
			interrupt();
		}

		@Override
		public void run() {
			while (!kill) {
				try {
					join();
				} catch (InterruptedException e) {
				}
				TaskDelegate<?> t;

				for (Runnable r : startupNotifiers) {
					r.run();
				}
				while ((t = l.poll()) != null) {
					executeTask(t);
				}
				currentTask = null;
				for (Runnable r : new ArrayList<Runnable>(sleepNotifiers)) {
					r.run();
				}
			}
		}

		public void cancelAll() {
			List<TaskDelegate<?>> cancelUs = new LinkedList<TaskDelegate<?>>(l);
			if (currentTask != null)
				cancelUs.add(0, currentTask);
			try{
			for (TaskDelegate<?> t : cancelUs) {
					t.cancel();		
				}
			} catch(Exception e){
//				logger.debug("Exception while cancelling talk: " + e.getStackTrace());
			}

		}

		protected void addSleepNotifier(Runnable r) {
			sleepNotifiers.add(r);
		}

		public void removeSleepNotifier(Runnable r) {
			sleepNotifiers.remove(r);
		}

		protected void addStartupNotifier(Runnable r) {
			startupNotifiers.add(r);
		}

		protected void removeStartupNotifier(Runnable r) {
			startupNotifiers.remove(r);
		}

		protected boolean executeTask(TaskGroup g) {
			for (TaskDelegate<?> t : g.getTasks()) {
				boolean cancelled = executeTask(t);
				if (cancelled) {
					g.cancel();
					return true;
				}
			}
			return false;
		}

		protected boolean executeTask(TaskDelegate<?> t) {
			if (t instanceof TaskGroup)
				return executeTask((TaskGroup) t);
			else {
				try {
					if (t.isCancelled())
						return true;
					currentTask = t;
					currentTask.run();
					return currentTask.isCancelled();
				} catch (Throwable error) {
					return true;
				}
			}
		}

		protected void scheduleTask(TaskDelegate<?> t) {
			l.add(t);
			interrupt();
		}

		protected TaskDelegate<?> getCurrentTask() {
			return currentTask;
		}

		protected LinkedList<TaskDelegate<?>> getTaskQueue() {
			return l;
		}
	}

	protected BackgroundEventThread thread;

	protected static BackgroundEventQueue queue;

	public BackgroundEventQueue() {
	}

	public void cancelAll() {
		getBackgroundEventThread().cancelAll();
	}

	public void addStartupNotifier(Runnable r) {
		getBackgroundEventThread().addStartupNotifier(r);
	}

	public void removeStartupNotifier(Runnable r) {
		getBackgroundEventThread().removeStartupNotifier(r);
	}

	public static BackgroundEventQueue getGlobalQueue() {
		if (queue == null)
			queue = new BackgroundEventQueue();
		return queue;
	}

	public void scheduleTask(TaskDelegate<?> t) {
		BackgroundEventThread thread = getBackgroundEventThread();
		thread.scheduleTask(t);
	}

	public void scheduleTasks(TaskDelegate<?>... tasks) {
		BackgroundEventThread thread = getBackgroundEventThread();
		for (TaskDelegate<?> task : tasks) {
			thread.scheduleTask(task);
		}
	}

	public void scheduleDependentTasks(TaskDelegate<?>... tasks) {
		BackgroundEventThread thread = getBackgroundEventThread();
		thread.scheduleTask(new TaskGroup(tasks));
	}

	protected BackgroundEventThread getBackgroundEventThread() {
		if (thread == null) {
			thread = new BackgroundEventThread();
			thread.start();
		}
		return thread;
	}

	public TaskDelegate<?> getCurrentTask() {
		return thread.getCurrentTask();
	}

	public int getPendingTaskCount() {
		return thread.getTaskQueue().size();
	}

	@Override
	protected void finalize() throws Throwable {
		die();
		super.finalize();
	}

	public void die() {
		if (thread != null)
			thread.kill();
	}

	@Override
	public String toString() {
		return "Background Event Queue " + id;
	}

	public <T> T runTaskNow(TaskDelegate<T> task) {
		BackgroundEventThread thread = getBackgroundEventThread();
		final Thread currentThread = Thread.currentThread();
		Runnable r = new Runnable() {
			
			@Override
			public void run() {
				currentThread.interrupt();
			}
		};
		thread.addSleepNotifier(r);
		thread.scheduleTask(task);
		try {
			thread.join();
		} catch (InterruptedException e) {
		}
		thread.removeSleepNotifier(r);
		return task.getResults();
	}

}
