package org.bbop.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.bbop.obo.GraphViewCanvas;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class TestCanvas {

	public static void main(String[] args) throws Exception {

		loadAndShow("src/test/resources/simple.obo", "CARO:0007", "CARO:0003");
//		loadAndShow("src/test/resources/go/gene_ontology_write.obo", "GO:0007406");
//		loadAndShow("src/test/resources/caro.obo");
		
	}
	
	private static void loadAndShow(String resource, String...selected) throws Exception {
		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper graph = pw.parseToOWLGraph(resource);

		OWLOntology ontology = graph.getSourceOntology();

		// create reasoner
		OWLReasoner reasoner = null;
		ElkReasonerFactory factory = new ElkReasonerFactory();
		reasoner = factory.createReasoner(ontology);

		Set<OWLObject> selectedObjects = null;
		if (selected != null && selected.length > 0) {
			selectedObjects = new HashSet<OWLObject>();
			for(String s : selected) {
				OWLObject owlObject = graph.getOWLObjectByIdentifier(s);
				if (owlObject != null) {
					selectedObjects.add(owlObject);
				}
			}
		}
		// create frame and exit behavior
		final GraphViewCanvas canvas = new GraphViewCanvas(graph, reasoner, selectedObjects);

		JFrame frame = new JFrame();
		frame.setSize(1000, 800);
		
		// add a reset button
		JPanel panel = new JPanel(new BorderLayout());
		JButton reset = new JButton("Reset");
		reset.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				canvas.reset();

			}
		});
		panel.add(canvas, BorderLayout.CENTER);
		panel.add(reset, BorderLayout.SOUTH);
		frame.add(panel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


		frame.addComponentListener(new ComponentListener() {

			@Override
			public void componentShown(ComponentEvent e) {
				canvas.panToObjects();
			}

			@Override
			public void componentResized(ComponentEvent e) {
				// empty
			}

			@Override
			public void componentMoved(ComponentEvent e) {
				// empty
			}

			@Override
			public void componentHidden(ComponentEvent e) {
				// empty
			}
		});

		// show
		frame.setVisible(true);

	}

}
