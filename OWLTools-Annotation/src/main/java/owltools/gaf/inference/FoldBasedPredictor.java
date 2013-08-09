package owltools.gaf.inference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import owltools.gaf.Bioentity;
import owltools.gaf.ExtensionExpression;
import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.graph.OWLGraphWrapper;

/**
 * Use a reasoner to find more specific named classes for annotations with extension expressions.
 * 
 * see http://code.google.com/p/owltools/wiki/AnnotationExtensionFolding
 * @author cjm
 */
public class FoldBasedPredictor extends AbstractAnnotationPredictor implements AnnotationPredictor {

	private static final Logger LOG = Logger.getLogger(FoldBasedPredictor.class);
	
	private OWLReasoner reasoner = null;
	private Set<OWLClass> relevantClasses;
	private Map<OWLClassExpression, Set<OWLClass>> reasonerCache = new HashMap<OWLClassExpression, Set<OWLClass>>();

	public FoldBasedPredictor(GafDocument gafDocument, OWLGraphWrapper graph) {
		super(gafDocument, graph);
		init();
		Logger.getLogger("org.semanticweb.elk").setLevel(Level.ERROR);
	}

	public void init() {
		OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();
		reasoner = reasonerFactory.createReasoner(getGraph().getSourceOntology());
		boolean consistent = reasoner.isConsistent();
		if (!consistent) {
			throw new RuntimeException("The ontology is not consistent. Impossible to make proper predictions.");
		}
		relevantClasses = new HashSet<OWLClass>();
		// add GO
		// biological process
		OWLClass bp = getGraph().getOWLClassByIdentifier("GO:0008150");
		if (bp != null) {
			relevantClasses.addAll(reasoner.getSubClasses(bp, false).getFlattened());
		}
		
		// molecular function
		OWLClass mf = getGraph().getOWLClassByIdentifier("GO:0003674");
		if (mf != null) {
			relevantClasses.addAll(reasoner.getSubClasses(mf, false).getFlattened());
		}
		// cellular component
		OWLClass cc = getGraph().getOWLClassByIdentifier("GO:0005575");
		if (cc != null) {
			relevantClasses.addAll(reasoner.getSubClasses(cc, false).getFlattened());
		}
		
		if (relevantClasses.isEmpty()) {
			throw new RuntimeException("No valid classes found for fold based prediction folding.");
		}
	}

	@Override
	public List<Prediction> predictForBioEntity(Bioentity e, Collection<GeneAnnotation> anns) {
		List<Prediction> predictions = new ArrayList<Prediction>();
		final OWLGraphWrapper g = getGraph();
		final OWLDataFactory f = g.getDataFactory();
		
		for (GeneAnnotation ann : anns) {
			String evidenceString = ann.getEvidenceCls();
			if ("ND".equals(evidenceString)) {
				continue;
			}
			String annotatedToClassString = ann.getCls();
			OWLClass annotatedToClass = g.getOWLClassByIdentifier(annotatedToClassString);
			if (annotatedToClass == null) {
				LOG.warn("Skipping annotation for prediction. Could not find cls for id: "+annotatedToClassString);
				continue;
			}
			Set<OWLClass> annotatedToSuperClasses = reasoner.getSuperClasses(annotatedToClass, false).getFlattened();
			Collection<ExtensionExpression> exts = ann.getExtensionExpressions();
			if (exts != null && !exts.isEmpty()) {
				Set<OWLClass> used = new HashSet<OWLClass>();
				for (ExtensionExpression ext : exts) {
					String extClsString = ext.getCls();
					String extRelString = ext.getRelation();
					
					OWLClass extCls = g.getOWLClassByIdentifier(extClsString);
					if (extCls == null) {
						continue;
					}
					OWLObjectProperty extRel = g.getOWLObjectPropertyByIdentifier(extRelString);
					if (extRel == null) {
						continue;
					}
					
					Set<OWLClass> cached = handleInferences(annotatedToClass, extRel, extCls, f);
					for (OWLClass c : cached) {
						if (c.isBuiltIn()) {
							continue;
						}
						if (relevantClasses.contains(c) == false) {
							continue;
						}
						if (c.equals(annotatedToClass) || annotatedToSuperClasses.contains(c)) {
							continue;
						}
						boolean added = used.add(c);
						if (added) {
							Prediction prediction = getPrediction(ann, c, e.getId(), ann.getCls());
							prediction.setReason(generateReason(c, annotatedToClass, extRelString, extCls, evidenceString, g));
							predictions.add(prediction);
						}
					}
				}
			}
		}
		return predictions;
	}

	private String generateReason(OWLClass foldedClass, OWLClass annotatedToClass, String extRelString, OWLClass extCls, String evidence, OWLGraphWrapper g) {
		StringBuilder sb = new StringBuilder();
		sb.append(g.getIdentifier(foldedClass));
		sb.append('\t');
		String foldedClassLabel = g.getLabel(foldedClass);
		if (foldedClassLabel != null) {
			sb.append(foldedClassLabel);
		}
		sb.append('\t');
		sb.append(FoldBasedPredictor.class.getSimpleName());
		sb.append('\t');
		sb.append(g.getIdentifier(annotatedToClass));
		sb.append('\t');
		String annotatedToClassLabel = g.getLabel(annotatedToClass);
		if (annotatedToClassLabel != null) {
			sb.append(annotatedToClassLabel);
		}
		sb.append('\t');
		sb.append(extRelString);
		sb.append('\t');
		sb.append(g.getIdentifier(extCls));
		sb.append('\t');
		String extClsLabel = g.getLabel(extCls);
		if (extClsLabel != null) {
			sb.append(extClsLabel);
		}
		sb.append('\t');
		sb.append(evidence);
		return sb.toString();
	}
	
	private Set<OWLClass> handleInferences(OWLClass annotatedToClass, OWLObjectProperty extRel, OWLClass extCls, OWLDataFactory f) {
		final OWLObjectSomeValuesFrom someValuesFrom = f.getOWLObjectSomeValuesFrom(extRel, extCls);
		final OWLObjectIntersectionOf intersectionOf = f.getOWLObjectIntersectionOf(annotatedToClass, someValuesFrom);
		
		Set<OWLClass> cached = reasonerCache.get(intersectionOf);
		if (cached == null) {
			// first check that the ce is satisfiable,
			if (reasoner.isSatisfiable(intersectionOf)) {
				// work round for elk bug, make a call to the reasoner to flush the stale ce info
				reasoner.getSuperClasses(annotatedToClass, true);
				
				// check for equivalent named classes
				cached = reasoner.getEquivalentClasses(intersectionOf).getEntitiesMinusBottom();
				if(cached.isEmpty()) {
					// work round for elk bug, make a call to the reasoner to flush the stale ce info
					reasoner.getSuperClasses(extCls, true);
					
					// if no equivalent named classes exist check for super classes
					cached = reasoner.getSuperClasses(intersectionOf, true).getFlattened();
				}
			}
			else {
				cached = Collections.emptySet();
			}
			reasonerCache.put(intersectionOf, cached);
		}
		return cached;
	}
	
	protected Prediction getPrediction(GeneAnnotation ann, OWLClass c, String bioentity, String with) {
		GeneAnnotation annP = new GeneAnnotation(ann);
		annP.setBioentity(bioentity);
		annP.setCls(getGraph().getIdentifier(c));
		annP.setEvidenceCls("IC");
		annP.setWithExpression(with);
		Prediction prediction = new Prediction(annP);
		return prediction;
	}

	@Override
	public void dispose() {
		if (reasoner != null) {
			reasoner.dispose();
		}
		if (reasonerCache != null) {
			reasonerCache.clear();
			reasonerCache = null;
		}
		
	}

}
