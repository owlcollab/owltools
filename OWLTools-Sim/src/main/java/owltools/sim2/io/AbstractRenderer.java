package owltools.sim2.io;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;

public abstract class AbstractRenderer implements SimResultRenderer {
	

	private static NumberFormat doubleRenderer = new DecimalFormat("#.##########");


	protected PrintStream resultOutStream;
	public OWLGraphWrapper graph;
	protected OWLPrettyPrinter owlpp;

	public OWLGraphWrapper getGraph() {
		return graph;
	}
	public void setGraph(OWLGraphWrapper graph) {
		this.graph = graph;
		owlpp = new OWLPrettyPrinter(graph);
	}
	
	@Override
	public void dispose() {
		resultOutStream.flush();		
		resultOutStream.close();
	}
	public PrintStream getResultOutStream() {
		return resultOutStream;
	}
	public void setResultOutStream(PrintStream resultOutStream) {
		this.resultOutStream = resultOutStream;
	}
	
	


}

