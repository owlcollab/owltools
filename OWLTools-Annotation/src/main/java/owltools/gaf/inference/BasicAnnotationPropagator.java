package owltools.gaf.inference;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLPropertyExpression;

import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;

/**
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
			OWLClass c = getGraph().getOWLClassByIdentifier(cid);
			String subOnt = getSubOntology(c);

			final Set<OWLGraphEdge> outgoingEdgesClosure = getGraph().getOutgoingEdgesClosure(c);
			for (OWLGraphEdge e : outgoingEdgesClosure) {
				final OWLObject target = e.getTarget();
				if (target instanceof OWLClass) {
					final OWLClass targetClass = (OWLClass)target;
					String ancSubOnt = getSubOntology(targetClass);
					// TODO make allowed pairs configurable
					boolean sameSubOntology = StringUtils.equals(subOnt, ancSubOnt);
					if (sameSubOntology == false) {
						if (e.getQuantifiedPropertyList().size() == 1) {
							OWLObjectProperty prop = e.getSingleQuantifiedProperty().getProperty();
							String pl = getGraph().getLabel(prop);
							if (pl.equals("part_of") || pl.equals("occurs_in")) {
								// NEW INFERENCES
								aClasses.add(c);
								predictions.add(getPrediction(targetClass, bioentity, ann.getWithExpression(), ann));
							}
						}
					}
				}
			}
		}

		// filter redundant annotations, use only the subClassOf hierarchy
		Set<OWLPropertyExpression> set = Collections.emptySet(); // only subClassOf
		setAndFilterRedundantPredictions(predictions, aClasses, set);
		return predictions;
	}

	protected String getSubOntology(OWLClass c) {
		final String namespace = getGraph().getNamespace(c);
		return namespace;
	}

	// TODO - move up?
	protected Prediction getPrediction(OWLClass c, String bioentity, String with, GeneAnnotation source) {
		
		GeneAnnotation annP = new GeneAnnotation();
		annP.setBioentity(bioentity);
		annP.setCls(getGraph().getIdentifier(c));
		annP.setEvidenceCls("IC");
		annP.setWithExpression(with);
		// TODO - evidence
		
		Prediction prediction = new Prediction(annP);
		return prediction;
	}



}
