package owltools.sim;

import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphWrapper;

public interface SimilarityCalculator {
	
	public OWLGraphWrapper getGraph() ;
	public void setGraph(OWLGraphWrapper graph);


	
	
	/**
	 * given two objects, calculate their similarity, returning a number between zero (no similarity)
	 * and 1 (equivalence)
	 * 
	 * @param a
	 * @param b
	 * @return similarity
	 */
	public Double calculateSimilarity(OWLObject a, OWLObject b);

}
