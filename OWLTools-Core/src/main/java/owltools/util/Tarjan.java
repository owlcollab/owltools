package owltools.util;

import java.util.List;

/**
 * Interface for the Tarjan's strongly connected components algorithm.
 *
 * @param <NODE>
 */
public interface Tarjan<NODE> {

	/**
	 * Create the strongly connected components for the given graph.
	 * 
	 * @param graph
	 * @return list of strongly connected components
	 */
	public List<List<NODE>> executeTarjan(Adjacency<NODE> graph);

}