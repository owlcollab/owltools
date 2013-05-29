package owltools.gaf.inference;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;

/**
 * TODO: this class has not yet been tested!
 * 
 * This performs basic annotation inferences involving propagation
 * between the 3 sub-ontologies in GO
 * 
 * <ul>
 *  <li> MF -> MP over part_of
 *  <li> BP -> CC over occurs_in
 * </ul>
 * 
 * TODO: reimplement using OWL semantics and reasoning
 * 
 * @author cjm
 *
 */
public class BasicAnnotationPropagator extends AbstractAnnotationPredictor implements AnnotationPredictor {

	protected static Logger LOG = Logger.getLogger(BasicAnnotationPropagator.class);

	public BasicAnnotationPropagator(GafDocument gafDocument,
			OWLGraphWrapper graph) {
		super(gafDocument, graph);

	}

	public Set<Prediction> predict(String bioentity) {
		Set<Prediction> predictions = new HashSet<Prediction>();
		Set<GeneAnnotation> anns = getGafDocument().getGeneAnnotations(bioentity);
		Set<OWLClass> aClasses = new HashSet<OWLClass>();
		LOG.info("collecting from:"+bioentity);
		for (GeneAnnotation ann : anns) {
			if (ann.getEvidenceCls().equals("ND")) {
				continue;
			}

			String cid = ann.getCls();
			OWLClass c = getGraph().getOWLClass(cid);
			String subOnt = getSubOntology(c);

			for (OWLGraphEdge e : getGraph().getOutgoingEdgesClosure(c)) {
				String ancSubOnt = getSubOntology((OWLClass)e.getTarget());
				if (!subOnt.equals(ancSubOnt)) {
					if (e.getQuantifiedPropertyList().size() == 1) {
						OWLObjectProperty prop = e.getSingleQuantifiedProperty().getProperty();
						String pl = getGraph().getLabel(prop);
						if (pl.equals("part_of") || pl.equals("occurs_in")) {
							// NEW INFERENCES
							aClasses.add(c);
							predictions.add(getPrediction(c, bioentity, ann.getWithExpression()));
						}
					}
				}
			}
		}

		this.setAndFilterRedundantPredictions(predictions, aClasses);
		return predictions;
	}

	private String getSubOntology(OWLClass c) {
		// TODO Auto-generated method stub
		return null;
	}

	// TODO - move up?
	protected Prediction getPrediction(OWLClass c, String bioentity, String with) {
		Prediction prediction = new Prediction();
		GeneAnnotation annP = new GeneAnnotation();
		annP.setBioentity(bioentity);
		annP.setCls(getGraph().getIdentifier(c));
		annP.setEvidenceCls("IC");
		annP.setWithExpression(with);
		// TODO - evidence
		prediction.setGeneAnnotation(annP);
		LOG.info("prediction="+prediction);
		return prediction;
	}



}
