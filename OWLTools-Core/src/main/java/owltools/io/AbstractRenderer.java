package owltools.io;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphWrapper;

/**
 * Renders the graph closure - i.e. the inferred paths emanating from all named entities
 * 
 * @author cjm
 *
 */
public abstract class AbstractRenderer implements GraphRenderer {

	protected OWLGraphWrapper graph;
	protected OWLPrettyPrinter prettyPrinter;
	protected PrintStream stream;

	public AbstractRenderer(PrintStream stream) {
		super();
		this.stream = stream;
	}

	public AbstractRenderer(String file) {
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

