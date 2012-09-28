package org.bbop.graph;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.bbop.gui.GraphCanvas;
import org.bbop.gui.ViewBehavior;

import edu.umd.cs.piccolo.event.PBasicInputEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;

public class RightClickMenuBehavior implements ViewBehavior {

	protected GraphCanvas canvas;
	protected List<RightClickMenuFactory> menuFactories = new ArrayList<RightClickMenuFactory>();

	protected PBasicInputEventHandler handler = new PBasicInputEventHandler() {
		@Override
		public void mouseClicked(PInputEvent event) {
			if (event.isRightMouseButton() && event.getClickCount() == 1) {
				doClick(event);
			}
		}
	};

	@Override
	public void install(GraphCanvas canvas) {
		this.canvas = canvas;
		canvas.addInputEventListener(handler);
	}

	@Override
	public void uninstall(GraphCanvas canvas) {
		canvas.removeInputEventListener(handler);
		this.canvas = canvas;
	}

	protected void doClick(PInputEvent event) {
		JPopupMenu menu = new JPopupMenu();
		for (RightClickMenuFactory factory : menuFactories) {
			Collection<JMenuItem> factories = factory.getMenuItems(canvas,
					event);
			if (factories == null)
				continue;
			for (JMenuItem item : factories) {
				if (item == null)
					continue;
				if (item == RightClickMenuFactory.SEPARATOR_ITEM)
					menu.addSeparator();
				else
					menu.add(item);
			}
		}
		MouseEvent e = (MouseEvent) event.getSourceSwingEvent();
		if (menu.getComponentCount() > 0)
			menu.show(canvas, e.getX(), e.getY());
	}
}
