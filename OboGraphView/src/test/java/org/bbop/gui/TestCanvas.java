package org.bbop.gui;

import javax.swing.JFrame;

import org.bbop.obo.GraphViewCanvas;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class TestCanvas {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		// load ontology
		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper graph = pw.parseToOWLGraph("src/test/resources/simple.obo");
//		OWLGraphWrapper graph = pw.parseToOWLGraph("src/test/resources/caro.obo");
		OWLOntology ontology = graph.getSourceOntology();
		
		// create reasoner
		OWLReasoner reasoner = null;
//		try {
			ElkReasonerFactory factory = new ElkReasonerFactory();
			reasoner = factory.createReasoner(ontology);
			
			// create frame and exit behavior
			final GraphViewCanvas canvas = new GraphViewCanvas(graph, reasoner);
			
			JFrame frame = new JFrame();
			frame.setSize(800, 600);
			frame.add(canvas);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		
			// show
			frame.setVisible(true);
//		}
//		finally {
//			if (reasoner != null) {
//				reasoner.dispose();
//			}
//		}
	}

}
