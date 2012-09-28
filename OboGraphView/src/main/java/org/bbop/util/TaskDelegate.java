package org.bbop.util;

public interface TaskDelegate<T> {

	public void cancel();
	public boolean isCancelled();
	public boolean isRunning();
	public void run();
	public boolean isFailed();
	public T getResults();
	public Throwable getException();
}
