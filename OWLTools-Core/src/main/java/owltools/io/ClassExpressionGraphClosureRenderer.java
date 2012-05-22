package owltools.io;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLProperty;

import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLQuantifiedProperty;

/**
 * Writes graph closure as class expressions
 * 
 * @author cjm
 *
 */
public class ClassExpressionGraphClosureRenderer extends AbstractClosureRenderer implements GraphRenderer {

	public ClassExpressionGraphClosureRenderer(PrintStream stream) {
		super(stream);
	}

	public ClassExpressionGraphClosureRenderer(String file) {
		super(file);
	}

	public void render(OWLGraphEdge e) {
		OWLObject s = e.getSource();
		OWLClassExpression x = (OWLClassExpression) graph.edgeToTargetExpression(e);
		OWLAxiom ax;
		if (s instanceof OWLClassExpression) {
			ax = graph.getDataFactory().getOWLSubClassOfAxiom((OWLClassExpression)s, x);
		}
		else if (s instanceof OWLIndividual) {
			ax = graph.getDataFactory().getOWLClassAssertionAxiom(x, (OWLIndividual) s);		
		}
		else {
			ax = null;
		}
		if (ax != null) {
			stream.print(ax.toString());
		}
	}

}
