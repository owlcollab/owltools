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
public class GraphClosureRenderer extends AbstractClosureRenderer implements GraphRenderer {

	public GraphClosureRenderer(PrintStream stream) {
		super(stream);
	}

	public GraphClosureRenderer(String file) {
		super(file);
	}

	public void render(OWLGraphEdge e) {
		if (!(e.getTarget() instanceof OWLNamedObject)) {
			return;
		}
		print(e.getSource());
		sep();
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
		sep();
		stream.print(e.getDistance());
		sep();
		print(e.getTarget());
		nl();

	}


}
