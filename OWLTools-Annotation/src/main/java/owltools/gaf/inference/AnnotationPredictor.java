package owltools.gaf.inference;

import java.util.Collection;
import java.util.Set;

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
	
	public Set<Prediction> predict(String bioentity);

	public Set<Prediction> predictForBioEntity(Bioentity e, Collection<GeneAnnotation> annotations);
	
	public Set<Prediction> getAllPredictions();
	
	public void dispose();

}
