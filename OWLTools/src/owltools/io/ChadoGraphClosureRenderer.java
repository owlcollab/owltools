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

	public boolean isChain = false;

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
		if (e.getQuantifiedPropertyList().size() != 1 && !isChain) {
			return;
		}
		StringBuffer rel = new StringBuffer("");
		int n = 0;
		for (OWLQuantifiedProperty qp : e.getQuantifiedPropertyList()) {
			//OWLQuantifiedProperty qp = e.getQuantifiedPropertyList().get(0);
			if (n > 0) {
				rel.append(",");
			}
			if (qp.isSubClassOf()) {
				rel.append("OBO_REL:is_a");
			}
			else if (qp.isSomeValuesFrom()) {
				rel.append(graph.getIdentifier(qp.getProperty()));
			}
			else {
				return;
			}
			n++;
		}
		stream.print(graph.getIdentifier(e.getSource()));
		sep();

		stream.print(rel);
		sep();
		stream.print(e.getDistance());
		sep();
		stream.print(graph.getIdentifier(e.getTarget()));
		nl();

	}






}
