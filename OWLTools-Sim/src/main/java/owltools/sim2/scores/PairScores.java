package owltools.sim2.scores;

/**
 * A set of scores for any pair of objects
 * 
 * @author cjm
 *
 * @param <T> - either OWLClass (attributes) or OWLNamedIndividual (elements)
 */
public interface PairScores<T> {
	public T getA();
	public T getB();
}