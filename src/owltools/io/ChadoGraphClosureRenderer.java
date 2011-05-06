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
 * implements a graph closure writer suitable for imports into Chado; in particular:
 * 
 * (i) all identifiers are translated to OBO IDs
 * (ii) relationship chains of length > 1 are ignored
 * (iii) SUBCLASS is written as is_a, and only existential restrictions are emitted
 * 
 * @author cjm
 *
 */
public class ChadoGraphClosureRenderer extends AbstractClosureRenderer implements GraphRenderer {

	
	public ChadoGraphClosureRenderer(PrintStream stream) {
		super(stream);
	}
	
	public ChadoGraphClosureRenderer(String file) {
		super(file);
	}

	public void render(OWLGraphEdge e) {
		if (!(e.getTarget() instanceof OWLNamedObject)) {
			return;
		}
		if (e.getQuantifiedPropertyList().size() != 1) {
			// TODO - add option to allow automatic creation of chains?
			return;
		}
		OWLQuantifiedProperty qp = e.getQuantifiedPropertyList().get(0);
		String rel = null;
		if (qp.isSubClassOf()) {
			rel = "OBO_REL:is_a";
		}
		else if (qp.isSomeValuesFrom()) {
			rel = graph.getIdentifier(qp.getProperty());
		}
		else {
			return;
		}
		print(e.getSource());
		sep();
		int n = 0;
		stream.print(rel);
		sep();
		stream.print(e.getDistance());
		sep();
		print(e.getTarget());
		nl();

	}

	@Override
	protected void print(OWLNamedObject obj) {
		stream.print(graph.getIdentifier(obj));
	}



}
