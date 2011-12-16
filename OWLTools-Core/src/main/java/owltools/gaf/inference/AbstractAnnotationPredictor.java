package owltools.gaf.inference;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLClass;

import owltools.gaf.*;
import owltools.graph.OWLGraphWrapper;

/**
 * 
 * 
 * @author cjm
 *
 */
public abstract class AbstractAnnotationPredictor implements AnnotationPredictor {

	protected static Logger LOG = Logger.getLogger(CompositionalClassPredictor.class);

	private GafDocument gafDocument;
	private OWLGraphWrapper graph;
	private boolean removeAllRedundant = true;


	public AbstractAnnotationPredictor(GafDocument gafDocument,
			OWLGraphWrapper graph) {
		super();
		this.gafDocument = gafDocument;
		this.graph = graph;
	}

	public GafDocument getGafDocument() {
		return gafDocument;
	}
	public void setGafDocument(GafDocument gafDocument) {
		this.gafDocument = gafDocument;
	}
	public OWLGraphWrapper getGraph() {
		return graph;
	}
	public void setGraph(OWLGraphWrapper graph) {
		this.graph = graph;
	}

	/**
	 * side-effects: removes redundant predictions
	 * 
	 * @param predictions
	 * @param aClasses
	 */
	protected void setAndFilterRedundantPredictions(Set<Prediction> predictions, Set<OWLClass> aClasses) {
		Set<OWLClass> classes = new HashSet<OWLClass>();
		Set<Prediction> newPredictions = new HashSet<Prediction>();

		for (Prediction p : predictions) {
			boolean isRedundant = false;
			GeneAnnotation a = p.getGeneAnnotation();
			OWLClass cls = (OWLClass) graph.getOWLObjectByIdentifier(a.getCls());
			for (OWLClass aClass : aClasses) {
				if (graph.getAncestorsReflexive(aClass).contains(cls)) {
					isRedundant = true;
					break;
				}
			}
			if (isRedundant && this.removeAllRedundant) {
				continue;
			}
			p.setRedundantWithExistingAnnotations(isRedundant);
			
			isRedundant = false;
			for (Prediction p2 : predictions) {
				GeneAnnotation a2 = p2.getGeneAnnotation();
				OWLClass cls2 = (OWLClass) graph.getOWLObjectByIdentifier(a2.getCls());
				if (graph.getAncestors(cls2).contains(cls)) {
					isRedundant = true;
					break;
				}
			}
			if (isRedundant && this.removeAllRedundant) {
				continue;
			}
			p.setRedundantWithOtherPredictions(isRedundant);
			newPredictions.add(p);
		}
		predictions.clear();
		predictions.addAll(newPredictions);
		


	}

	public Set<Prediction> getAllPredictions() {
		Set<Prediction> pset = new HashSet<Prediction>();
		for (Bioentity e : gafDocument.getBioentities()) {
			pset.addAll(this.predict(e.getId()));
		}
		return pset;
	}

}
