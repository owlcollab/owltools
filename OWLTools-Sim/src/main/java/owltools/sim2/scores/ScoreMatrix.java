package owltools.sim2.scores;

/**
 * A matrix of scores or scored entities for two groups of attributes
 * 
 * @author cjm
 *
 * @param <T>
 */
public class ScoreMatrix<T> {
	public T[][] matrix;
	public T[] bestForC;
	public T[] bestForD;
}