package owltools.util;

import java.util.List;

/**
 * Representation of a graph (i.e. ontology in this context) using adjacency information.
 * 
 * @param <T> type parameter for the actual nodes in the graph
 */
public interface Adjacency<T> {

   public List<T> getAdjacent(T source);

   public Iterable<T> getSources();

}
