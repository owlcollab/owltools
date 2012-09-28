package org.bbop.graph.tooltip;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import edu.umd.cs.piccolo.PNode;

public abstract class AbstractTooltipFactory implements TooltipFactory {

	protected Collection<TooltipChangeListener> listeners = new ArrayList<TooltipChangeListener>();

	protected void fireTooltipChanged() {
		Iterator<TooltipChangeListener> it = listeners.iterator();
		while(it.hasNext()) {
			TooltipChangeListener listener = it.next();
			listener.tooltipChanged();
		}
	}
	
	@Override
	public void addTooltipChangeListener(TooltipChangeListener listener) {
		listeners.add(listener);
	}

	@Override
	public void destroyTooltip(PNode tooltip) {
	}

	@Override
	public long getDelay() {
		return 2000;
	}

	@Override
	public void removeTooltipChangeListener(TooltipChangeListener listener) {
		listeners.remove(listener);
	}

}
