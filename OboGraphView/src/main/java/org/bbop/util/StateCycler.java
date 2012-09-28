package org.bbop.util;

import java.util.ArrayList;
import java.util.List;

public class StateCycler {

	protected final List<CycleState> states = new ArrayList<CycleState>();

	protected int currentIndex = 0;

	protected boolean tryEveryState = false;

	protected String desc;

	public StateCycler() {
	}

	public StateCycler(String desc) {
		setDesc(desc);
	}

	public void addState(CycleState state) {
		states.add(state);
	}

	public void addState(CycleState state, int index) {
		states.add(index, state);
	}

	public void removeState(CycleState state) {
		states.remove(state);
	}

	public CycleState getCurrentState() {
		if (states.size() == 0)
			return null;
		return (CycleState) states.get(currentIndex);
	}

	public void enforceCurrentState() {
		CycleState currentState = getCurrentState();
		if (currentState != null)
			currentState.apply();
	}
	
	public void setCurrentState(CycleState state) {
		int index = states.indexOf(state);
		if (index >= 0) {
			currentIndex = index;
			enforceCurrentState();
		}
	}

	/**
	 * First, check the current state. If it isn't active, activate it. If it is
	 * active, start checking the other states in the list, looping around to
	 * the beginning if the end of the list is reached. If a usable state is
	 * found, that state is applied and becomes the current state. A state is
	 * usable if either: 1) it is not currently active 2) its alwaysActivate()
	 * method returns true 3) the tryEveryState flag has been set for this state
	 * cycler
	 * 
	 */
	public void cycleStates() {
		int updates = 0;
		CycleState currentState = getCurrentState();
		if (!currentState.isActive()) {
			enforceCurrentState();
			return;
		}
		do {
			currentIndex = (currentIndex + 1) % states.size();
			updates++;
			currentState = getCurrentState();
			if (tryEveryState || currentState.alwaysActivate()
					|| !currentState.isActive()) {
				enforceCurrentState();
				return;
			}
		} while (updates < states.size());
	}

	public boolean getTryEveryState() {
		return tryEveryState;
	}

	public void setTryEveryState(boolean tryEveryState) {
		this.tryEveryState = tryEveryState;
	}

	public List<CycleState> getStates() {
		return states;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}
}
