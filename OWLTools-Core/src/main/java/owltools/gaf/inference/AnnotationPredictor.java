package owltools.gaf.inference;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;

import owltools.gaf.*;
import owltools.graph.OWLGraphWrapper;

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
