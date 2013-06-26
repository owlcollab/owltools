package owltools.gaf.inference;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLPropertyExpression;

import owltools.gaf.Bioentity;
import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
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
	 * side-effects: removes redundant predictions over all relationships
	 * 
	 * @param predictions
	 * @param aClasses
	 */
	protected void setAndFilterRedundantPredictions(Set<Prediction> predictions, Set<OWLClass> aClasses) {
		setAndFilterRedundantPredictions(predictions, aClasses, null);

	}
	
	/**
	 * side-effects: removes redundant predictions over a set of relationships.
	 * If overProps set is empty, only the subClassOf hierarchy is used, if it's
	 * null all relationships are used.
	 * 
	 * @param predictions
	 * @param aClasses
	 * @param overProps
	 */
	protected void setAndFilterRedundantPredictions(Set<Prediction> predictions, Set<OWLClass> aClasses, Set<OWLPropertyExpression> overProps) {
		Set<Prediction> newPredictions = new HashSet<Prediction>();

		for (Prediction p : predictions) {
			boolean isRedundant = false;
			GeneAnnotation a = p.getGeneAnnotation();
			OWLClass cls = (OWLClass) graph.getOWLObjectByIdentifier(a.getCls());
			for (OWLClass aClass : aClasses) {
				if (graph.getAncestorsReflexive(aClass, overProps).contains(cls)) {
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
