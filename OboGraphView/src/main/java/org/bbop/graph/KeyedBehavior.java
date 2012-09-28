package org.bbop.graph;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import org.bbop.gui.GraphCanvas;
import org.bbop.gui.ViewBehavior;

public abstract class KeyedBehavior implements ViewBehavior {

	protected int keyCode = KeyEvent.VK_UNDEFINED;

	protected int modifiers = 0;
	
	protected GraphCanvas canvas;

	protected KeyListener keyListener = new KeyAdapter() {
		@Override
		public void keyPressed(KeyEvent e) {
			if ((modifiers == 0 || ((e.getModifiers() & modifiers) != 0))
					&& keyCode == e.getKeyCode())
				action();
		}
	};

	@Override
	public void install(GraphCanvas canvas) {
		this.canvas = canvas;
		canvas.addKeyListener(keyListener);
	}

	@Override
	public void uninstall(GraphCanvas canvas) {
		canvas.removeKeyListener(keyListener);
		this.canvas = null;
	}

	protected abstract void action();

	public int getKeyCode() {
		return keyCode;
	}

	public void setKeyCode(int keyCode) {
		this.keyCode = keyCode;
	}

	public int getModifiers() {
		return modifiers;
	}

	public void setModifiers(int modifiers) {
		this.modifiers = modifiers;
	}

}
