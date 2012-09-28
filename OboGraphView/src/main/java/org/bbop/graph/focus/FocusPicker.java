package org.bbop.graph.focus;

import java.awt.Color;
import java.awt.Paint;

import org.apache.log4j.Logger;
import org.bbop.graph.NodeDecorator;
import org.bbop.graph.OENode;
import org.bbop.graph.PCNode;
import org.bbop.graph.RelayoutListener;
import org.bbop.gui.GraphCanvas;
import org.bbop.gui.ViewBehavior;
import org.bbop.piccolo.PiccoloUtil;
import org.bbop.util.ColorUtil;
import org.semanticweb.owlapi.model.OWLObject;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.activities.PActivity;
import edu.umd.cs.piccolo.event.PBasicInputEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;

public class FocusPicker implements ViewBehavior, NodeDecorator {

	//initialize logger
	protected final static Logger logger = Logger.getLogger(FocusPicker.class);

	protected static final Object REAL_PAINT = new Object();

	protected GraphCanvas canvas;

	protected PBasicInputEventHandler handler = new PBasicInputEventHandler() {
		@Override
		public void mousePressed(PInputEvent event) {
			if (event.getClickCount() == 1) {
				PCNode<?> node = PiccoloUtil.getNodeOfClass(event.getPath(), PCNode.class);
				if (node != null) {
					if (node instanceof OENode) {
						OENode focused = (OENode) node;
						OENode old = canvas.getFocusedNode();
						if (old == null || focused.equals(old) == false) {
							canvas.setFocusedObject(focused.getObject());
						}
					}
					return;
				}
			}
		}
	};

	protected FocusedNodeListener focusListener = new FocusedNodeListener() {
		@Override
		public void focusedChanged(OWLObject oldFocus, OWLObject newFocus) {
			decorate(oldFocus, true);
			decorate(newFocus, true);
		}
	};

	protected RelayoutListener layoutListener = new RelayoutListener() {

		@Override
		public void relayoutComplete() {
			for (OENode node : canvas.getVisibleNodes()) {
				node.addAttribute(REAL_PAINT, null);
			}
		}

		@Override
		public void relayoutStarting() {
		}

	};

	public FocusPicker() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void install(GraphCanvas canvas) {
		canvas.addInputEventListener(handler);
		canvas.addDecorator(this);
		canvas.addFocusedNodeListener(focusListener);
		canvas.addRelayoutListener(layoutListener);
		this.canvas = canvas;
	}

	@Override
	public void uninstall(GraphCanvas canvas) {
		canvas.removeInputEventListener(handler);
		canvas.removeDecorator(this);
		canvas.removeFocusedNodeListener(focusListener);
		canvas.removeRelayoutListener(layoutListener);
		this.canvas = null;
	}

	public PActivity decorate(OWLObject pc, boolean noAnimation) {
		PCNode<?> node = canvas.getNode(pc);
		if (node == null)
			return null;
		else
			return decorate(node, noAnimation);
	}

	@Override
	public PActivity decorate(PNode node, boolean noAnimation) {
		if (!(node instanceof OENode))
			return null;
		Paint p = node.getPaint();
		p = (Paint) node.getAttribute(REAL_PAINT);
		if (p == null) {
			p = node.getPaint();
			node.addAttribute(REAL_PAINT, p);
		}
		Color c = null;
		if (p instanceof Color)
			c = (Color) p;
		if (canvas.getFocusedNode() != null
				&& canvas.getFocusedNode().equals(node)) {
			if (c == null) {
				node.setPaint(Color.white);
			} else {
				Color newColor = ColorUtil.mergeColors(c, Color.white); 
				node.setPaint(newColor);
			}

		} else
			node.setPaint(p);

		return null;
	}

	@Override
	public boolean onlyDecorateAfterLayout() {
		return false;
	}

}
