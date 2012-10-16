package org.geneontology.lego.dot;

import java.io.IOException;
import java.util.Collection;

import org.geneontology.lego.model.LegoTools.UnExpectedStructureException;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

public interface LegoRenderer {

	/**
	 * Render the given individuals (aka set of annotations).
	 * 
	 * @param individuals
	 * @param name name of the graph (optional)
	 * @param renderKey
	 * @throws IOException
	 * @throws UnExpectedStructureException thrown, if there are unexpected axioms.
	 */
	public void render(Collection<OWLNamedIndividual> individuals, String name,
			boolean renderKey) throws IOException, UnExpectedStructureException;

}