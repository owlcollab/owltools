package org.bbop.graph;

import java.util.List;

import javax.swing.JMenuItem;

import org.bbop.gui.GraphCanvas;

import edu.umd.cs.piccolo.event.PInputEvent;

public interface RightClickMenuFactory {
	public static JMenuItem SEPARATOR_ITEM = new JMenuItem();
	
	public List<JMenuItem> getMenuItems(GraphCanvas canvas, PInputEvent e);
}
