package owltools.gaf.inference;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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

	@Override
	public Set<Prediction> predict(String bioentity) {
		Set<GeneAnnotation> anns = gafDocument.getGeneAnnotations(bioentity);
		Bioentity e = gafDocument.getBioentity(bioentity);
		return predictForBioEntity(e, anns);
	}

	@Override
	public Set<Prediction> getAllPredictions() {
		Map<Bioentity, Set<GeneAnnotation>> allAnnotations = new HashMap<Bioentity, Set<GeneAnnotation>>();
		
		for(GeneAnnotation annotation : getGafDocument().getGeneAnnotations()) {
			Bioentity e = annotation.getBioentityObject();
			Set<GeneAnnotation> anns = allAnnotations.get(e);
			if (anns == null) {
				anns = new HashSet<GeneAnnotation>();
				allAnnotations.put(e, anns);
			}
			anns.add(annotation);
		}
		
		Set<Prediction> pset = new HashSet<Prediction>();
		for (Bioentity e : allAnnotations.keySet()) {
			Collection<GeneAnnotation> anns = allAnnotations.get(e);
			pset.addAll(predictForBioEntity(e, anns));
		}
		return pset;
	}

	@Override
	public void dispose() {
		// do nothing
	}

	
}
