package owltools.io;

import java.io.PrintStream;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphEdge;

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
