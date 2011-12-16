package owltools.io;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLProperty;

import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLQuantifiedProperty;

/**
 * Renders the graph closure - i.e. the inferred paths emanating from all named entities
 * 
 * @author cjm
 *
 */
public abstract class AbstractClosureRenderer implements GraphRenderer {

	protected OWLGraphWrapper graph;
	protected OWLPrettyPrinter prettyPrinter;
	protected PrintStream stream;

	public AbstractClosureRenderer(PrintStream stream) {
		super();
		this.stream = stream;
	}

	public AbstractClosureRenderer(String file) {
		super();
		setStream(file);
	}
	
	
	
	public PrintStream getStream() {
		return stream;
	}

	public void setStream(PrintStream stream) {
		this.stream = stream;
	}
	
	public void setStream(String file) {
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(file);
			this.stream = new PrintStream(new BufferedOutputStream(fos));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	public void render(OWLGraphWrapper g) {
		graph = g;
		prettyPrinter = new OWLPrettyPrinter(g);
		g.getConfig().isCacheClosure = false;
		int i = 0;
		for (OWLObject obj : g.getAllOWLObjects()) {
			if (i % 100 == 0) {
				if (g.getConfig().isMonitorMemory) {
					System.gc();
					System.gc();
					System.gc();
					long tm = Runtime.getRuntime().totalMemory();
					long fm = Runtime.getRuntime().freeMemory();
					long mem = tm-fm;
					System.err.println(i+" Memory total:"+tm+" free:"+fm+" diff:"+mem+" (bytes) diff:"+(mem/1000000)+" (mb)");
				}
			}
			i++;
			for (OWLGraphEdge e : g.getOutgoingEdgesClosure(obj)) {
				render(e);
			}
		}
		stream.close();
	}
	

	public abstract void render(OWLGraphEdge e);
	
	protected void print(OWLObject obj) {
		stream.print(prettyPrinter.render(obj));
	}
	
	protected void print(String s) {
		stream.print(s);
	}


	protected void sep() {
		stream.print("\t");
	}

	protected void nl() {
		stream.print("\n");
	}

}

