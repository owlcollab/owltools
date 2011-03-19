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

public class GraphClosureWriter {

	protected PrintStream stream;
	
	

	public GraphClosureWriter(PrintStream stream) {
		super();
		this.stream = stream;
	}

	public GraphClosureWriter(String file) {
		super();
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(file);
			this.stream = new PrintStream(new BufferedOutputStream(fos));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void serializeClosure(OWLGraphWrapper g) throws IOException {
		//FileOutputStream fos = new FileOutputStream(filename);
		ObjectOutputStream out = new ObjectOutputStream(stream);
		for (OWLObject obj : g.getAllOWLObjects()) {
			g.getOutgoingEdgesClosure(obj);
		}
		out.writeObject(g.inferredEdgeBySource);
	}

	public void saveClosure(OWLGraphWrapper g) {
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
			//System.err.println(obj);
			for (OWLGraphEdge e : g.getOutgoingEdgesClosure(obj)) {
				//System.err.println("  E:"+e);
				print(obj);
				sep();
				//stream.print("[");
				int n = 0;
				for (OWLQuantifiedProperty qp : e.getQuantifiedPropertyList()) {
					if (n>0) {
						stream.print(", ");
					}
					if (qp.hasProperty()) {
						print(qp.getProperty());
						stream.print(" ");
					}
					stream.print(qp.getQuantifier());

					n++;
				}
				//stream.print("]");
				sep();
				if (!(e.getTarget() instanceof OWLNamedObject)) {
					//System.err.println("undefined behavior: "+e.getTarget());
					continue;
				}
				print(e.getTarget());
				nl();
			}
		}
		stream.close();
	}

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

	protected void sep() {
		stream.print("\t");
	}

	protected void nl() {
		stream.print("\n");
	}
}
