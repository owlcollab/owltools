package owltools.gaf.inference;

import java.util.Collection;
import java.util.List;

import owltools.gaf.Bioentity;
import owltools.gaf.GeneAnnotation;

/**
 * 
 * given a bioentity, predict a set of annotations
 * 
 * @author cjm
 *
 */
public interface AnnotationPredictor {
	
	public List<Prediction> predict(String bioentity);

	public List<Prediction> predictForBioEntity(Bioentity e, Collection<GeneAnnotation> annotations);
	
	public List<Prediction> getAllPredictions();
	
	public void dispose();

}
