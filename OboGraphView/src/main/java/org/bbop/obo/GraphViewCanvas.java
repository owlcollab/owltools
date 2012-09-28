package org.bbop.obo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.bbop.graph.HierarchicalGraphLayout;
import org.bbop.gui.GraphCanvas;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.graph.OWLGraphWrapper;

public class GraphViewCanvas extends JPanel {

	// generated
	private static final long serialVersionUID = -219490018608769710L;
	
	private final JPanel dagPanel = new JPanel();
	private final JLabel topLabel = new JLabel();
	
	private GraphCanvas canvas = null;

	public GraphViewCanvas(OWLGraphWrapper graph, OWLReasoner reasoner) {
		super();
		setLayout(new BorderLayout());
		setBackground(Color.white);
		dagPanel.setOpaque(false);
		add(dagPanel, "Center");
		add(topLabel, "North");
		updatePanels(graph, reasoner);
	}

	private GraphCanvas createCanvas(OWLGraphWrapper graph, OWLReasoner reasoner) {
		GraphCanvas canvas = new GraphCanvas(new HierarchicalGraphLayout(graph), graph, reasoner);
		canvas.relayout();
		return canvas;
	}

	public void updatePanels(OWLGraphWrapper graph, OWLReasoner reasoner) {
		dagPanel.removeAll();
		canvas = createCanvas(graph, reasoner);
		dagPanel.setLayout(new GridLayout(1, 1));
		dagPanel.add(canvas);
		dagPanel.validate();
		validate();
		repaint();
		canvas.zoomToObjects();
	}

	public void redraw() {
		if (canvas != null) {
			canvas.redraw();
		}
	}

}
