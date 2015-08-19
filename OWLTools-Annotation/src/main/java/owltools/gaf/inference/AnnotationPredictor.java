package owltools.gaf.inference;

import java.util.Collection;
import java.util.List;
import java.util.Map;

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
	
	public boolean isInitialized();
	
	public List<Prediction> predict(String bioentity);

	public List<Prediction> predictForBioEntities(Map<Bioentity, ? extends Collection<GeneAnnotation>> annotations);
	
	public List<Prediction> getAllPredictions();
	
	public void dispose();

}
