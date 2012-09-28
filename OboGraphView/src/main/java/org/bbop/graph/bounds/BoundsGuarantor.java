package org.bbop.graph.bounds;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.geom.Line2D;
import java.util.Iterator;

import org.bbop.graph.KeyedBehavior;
import org.bbop.graph.RelayoutListener;
import org.bbop.graph.focus.FocusedNodeListener;
import org.bbop.gui.GraphCanvas;
import org.bbop.piccolo.PLayoutNode;
import org.bbop.piccolo.PiccoloBoxLayout;
import org.bbop.piccolo.StatusMessageDisplayer;
import org.bbop.util.CycleState;
import org.bbop.util.StateCycler;
import org.semanticweb.owlapi.model.OWLObject;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.activities.PActivity;
import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.nodes.PText;

public class BoundsGuarantor extends KeyedBehavior {

	private StateCycler boundsGuarantor = new StateCycler();
	private StatusMessageDisplayer messageDisplayer;
	private PActivity boundsChangeActivity;

	public BoundsGuarantor() {
		setKeyCode(KeyEvent.VK_V);

	}

	public synchronized void scheduleBoundsChange(PActivity boundsChangeActivity) {
		PActivity oldBoundsActivity = this.boundsChangeActivity; 
		if (oldBoundsActivity != null
				&& oldBoundsActivity.isStepping()) {
			oldBoundsActivity.terminate();
		}
		this.boundsChangeActivity = boundsChangeActivity;
	}

	public void guaranteeViewBounds() {
		boundsGuarantor.enforceCurrentState();
	}

	public void updateViewBounds() {
		boundsGuarantor.cycleStates();
		messageDisplayer.showStatusMessage(getCyclerMessage(boundsGuarantor), 3000);
	}

	protected PNode getCyclerMessage(StateCycler cycler) {
		PLayoutNode out = new PLayoutNode();
		out.setLayoutManager(new PiccoloBoxLayout(PiccoloBoxLayout.Orientation.VERT));
		if (cycler.getDesc() != null) {
			Font headerFont = new Font("Arial", Font.ITALIC, 18);
			PText header = new PText(cycler.getDesc());
			header.setFont(headerFont);
			header.setTextPaint(Color.white);
			out.addChild(header);
			PPath line = new PPath(
					new Line2D.Double(0, 0, header.getWidth(), 0));
			line.setStrokePaint(Color.white);
			line.setStroke(new BasicStroke(1));
			out.addChild(line);
			PNode spacer = new PNode();
			spacer.setBounds(0, 0, 12, 12);
			out.addChild(spacer);
		}
		Font font = new Font("Arial", Font.PLAIN, 12);
		Iterator<CycleState> it = cycler.getStates().iterator();
		while (it.hasNext()) {
			CycleState state = it.next();
			PText text = new PText(state.getDesc());
			text.setFont(font);
			text.setTextPaint(Color.white);
			if (state.equals(cycler.getCurrentState()))
				text.setFont(new Font("Arial", Font.BOLD, 18));
			out.addChild(text);
		}
		return out;
	}

	@Override
	public void install(final GraphCanvas canvas) {
		super.install(canvas);
		canvas.addRelayoutListener(new RelayoutListener() {

			@Override
			public void relayoutComplete() {
				guaranteeViewBounds();
			}

			@Override
			public void relayoutStarting() {
				guaranteeViewBounds();
			}

		});
		canvas.addFocusedNodeListener(new FocusedNodeListener() {
			@Override
			public void focusedChanged(OWLObject oldFocus, OWLObject newFocus) {
				guaranteeViewBounds();
			}
		});
		installDefaultCyclers();
		messageDisplayer = new StatusMessageDisplayer(canvas.getCamera());
	}

	protected void installDefaultCyclers() {
		addBoundsGuarantor(new PanToFocusedGuarantor(canvas));
		addBoundsGuarantor(new ZoomToFocusedGuarantor(canvas));
		addBoundsGuarantor(new ZoomToAllGuarantor(canvas));

	}

	public void addBoundsGuarantor(BoundsGuarantorCycleState g) {
		boundsGuarantor.addState(g);
		g.setBoundsGuarantor(this);
	}

	public void removeBoundsGuarantor(BoundsGuarantorCycleState g) {
		boundsGuarantor.addState(g);
		g.setBoundsGuarantor(null);
	}

	@Override
	public void setKeyCode(int keyCode) {
		super.setKeyCode(keyCode);

		boundsGuarantor.setDesc("Changed view mode ("
				+ KeyEvent.getKeyText(keyCode) + " key)");
	}

	@Override
	protected void action() {
		updateViewBounds();
	}
}
