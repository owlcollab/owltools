package org.bbop.obo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.bbop.graph.HierarchicalGraphLayout;
import org.bbop.gui.GraphCanvas;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.graph.OWLGraphWrapper;

public class GraphViewCanvas extends JPanel {

	// generated
	private static final long serialVersionUID = -219490018608769710L;
	
	private final JPanel dagPanel = new JPanel();
	private final JLabel topLabel = new JLabel();
	
	private GraphCanvas canvas = null;

	public GraphViewCanvas(OWLGraphWrapper graph, OWLReasoner reasoner, Set<OWLObject> selection) {
		super();
		setLayout(new BorderLayout());
		setBackground(Color.white);
		dagPanel.setOpaque(false);
		add(dagPanel, "Center");
		add(topLabel, "North");
		updatePanels(graph, reasoner, selection);
	}

	private GraphCanvas createCanvas(OWLGraphWrapper graph, OWLReasoner reasoner, Set<OWLObject> selection) {
		GraphCanvas canvas = new GraphCanvas(new HierarchicalGraphLayout(graph), graph, reasoner, selection);
		canvas.relayout();
		return canvas;
	}

	public void updatePanels(OWLGraphWrapper graph, OWLReasoner reasoner, Set<OWLObject> selection) {
		dagPanel.removeAll();
		canvas = createCanvas(graph, reasoner, selection);
		dagPanel.setLayout(new GridLayout(1, 1));
		dagPanel.add(canvas);
		dagPanel.validate();
		validate();
		repaint();
	}

	public void redraw() {
		if (canvas != null) {
			canvas.redraw();
		}
	}

	public void reset() {
		if (canvas != null) {
			canvas.reset();
			canvas.panToObjects();
		}
	}

	public void setSelected(Set<OWLObject> objects) {
		if (canvas != null) {
			canvas.setSelected(objects);
			canvas.panToObjects();
		}
	}
	
	public void panToObjects() {
		if (canvas != null) {
			canvas.panToObjects();
		}
	}
}
