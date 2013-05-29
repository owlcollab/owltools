package owltools.gaf.inference;

import java.util.Set;

/**
 * 
 * given a bioentity, predict a set of annotations
 * 
 * @author cjm
 *
 */
public interface AnnotationPredictor {
	
	public Set<Prediction> predict(String bioentity);

	public Set<Prediction> getAllPredictions();

}
