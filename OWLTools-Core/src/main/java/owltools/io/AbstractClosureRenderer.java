package owltools.io;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;

/**
 * Renders the graph closure - i.e. the inferred paths emanating from all named entities
 * 
 * @author cjm
 *
 */
public abstract class AbstractClosureRenderer extends AbstractRenderer implements GraphRenderer {

	public AbstractClosureRenderer(PrintStream stream) {
		super(stream);
	}

	public AbstractClosureRenderer(String file) {
		super(file);
	}
	

	public void render(OWLGraphWrapper g) {
		graph = g;
		
		Set<OWLObject> objs = new HashSet<OWLObject>(g.getSourceOntology().getClassesInSignature(false));
		objs.addAll(g.getSourceOntology().getIndividualsInSignature(false));

		for (OWLObject obj : objs) {
			for (OWLGraphEdge e : g.getOutgoingEdgesClosure(obj)) {
				render(e);
			}
		}
		stream.close();
	}
	

	public abstract void render(OWLGraphEdge e);
	

}

