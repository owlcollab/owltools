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
 * 
 * 
 * @author cjm
 *
 */
public abstract class AbstractClosureRenderer implements GraphRenderer {

	protected OWLGraphWrapper graph;
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
		if (obj instanceof OWLNamedObject) {
			OWLNamedObject nobj = (OWLNamedObject)obj;
			if (nobj.getIRI().toString() == null || nobj.getIRI().toString().equals("")) {
				System.err.println("uh oh:"+nobj);
			}
			stream.print(nobj.getIRI().toString());
		}
		else if (obj instanceof OWLClassExpression) {
			stream.print(obj.toString());
		}
		else {

		}
	}



	protected void print(OWLNamedObject obj) {
		// TODO: prefixes
		if (obj.getIRI().toString() == null || obj.getIRI().toString().equals("")) {
			System.err.println("uh oh:"+obj);
		}
		stream.print(obj.getIRI().toString());
	}
	

	protected void print(OWLClassExpression obj) {
		stream.print(obj.toString());
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

