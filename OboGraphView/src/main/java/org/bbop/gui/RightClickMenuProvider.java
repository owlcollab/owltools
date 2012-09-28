package org.bbop.gui;

import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;

public interface RightClickMenuProvider {
	
	public void fillInMenu(MouseEvent e, JPopupMenu menu);
}
