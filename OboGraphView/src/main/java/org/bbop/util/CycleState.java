package org.bbop.util;

public interface CycleState {

	public void apply();
	public void halt();
	public boolean isActive();
	public boolean alwaysActivate();
	public String getDesc();

}
