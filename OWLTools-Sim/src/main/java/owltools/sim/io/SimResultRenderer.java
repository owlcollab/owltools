package owltools.sim.io;

import org.semanticweb.owlapi.model.OWLClass;

import owltools.graph.OWLGraphWrapper;
import owltools.sim.io.DelimitedLineRenderer.SimScores;

public interface SimResultRenderer {

	public void printComment(CharSequence comment);

	public void printAttributeSim(SimScores simScores, OWLClass a, OWLClass b,
			OWLGraphWrapper graph);

}