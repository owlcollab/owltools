package owltools.io;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
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
public class TableRenderer extends AbstractRenderer implements GraphRenderer {

	public TableRenderer(PrintStream stream) {
		super(stream);
	}

	public TableRenderer(String file) {
		super(file);
	}
	

	public void render(OWLGraphWrapper g) {
		graph = g;
		
		Set<OWLObject> objs = new HashSet<OWLObject>(g.getSourceOntology().getClassesInSignature(false));
		objs.addAll(g.getSourceOntology().getIndividualsInSignature(false));

		for (OWLObject obj : objs) {
			if (obj.equals(g.getDataFactory().getOWLNothing()))
				continue;
			if (obj.equals(g.getDataFactory().getOWLThing()))
				continue;
			if (obj instanceof OWLNamedObject)
				render((OWLNamedObject)obj);
		}
		stream.close();
	}
	

	// TODO - make this configurable
	private void render(OWLNamedObject obj) {
		print(obj.getIRI().toString());
		sep();
		print(graph.getLabel(obj));
		sep();
		print(graph.getDef(obj));
		nl();
	}

	

}

