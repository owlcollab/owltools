package owltools.gaf.inference;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import owltools.gaf.Bioentity;
import owltools.gaf.ExtensionExpression;
import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.owl.AnnotationExtensionFolder;
import owltools.graph.OWLGraphWrapper;

/**
 * 
 * see http://code.google.com/p/owltools/wiki/AnnotationExtensionFolding
 * @author cjm
 *
 */
public class FoldBasedPredictor extends AbstractAnnotationPredictor implements AnnotationPredictor {

	protected static Logger LOG = Logger.getLogger(FoldBasedPredictor.class);
	Map<OWLClass,Set<OWLClassExpression>> simpleDefMap = new HashMap<OWLClass,Set<OWLClassExpression>>();
	AnnotationExtensionFolder folder = null;
	private OWLReasoner reasoner = null;

	public FoldBasedPredictor(GafDocument gafDocument, OWLGraphWrapper graph) {
		super(gafDocument, graph);
		init();
	}

	public void init() {
		folder = new AnnotationExtensionFolder(this.getGraph());
		folder.fold(getGafDocument(), false); // do not replace
		//LOG.info("Candidate classes to deepen: "+folder.rewriteMap.size());
		OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();
		reasoner = reasonerFactory.createReasoner(getGraph().getSourceOntology());
	}

	@Override
	public Set<Prediction> predictForBioEntity(Bioentity e, Collection<GeneAnnotation> anns) {
		Set<Prediction> predictions = new HashSet<Prediction>();
		for (GeneAnnotation ann : anns) {
			if (ann.getEvidenceCls().equals("ND")) {
				continue;
			}
			OWLClass annotatedToClass = getGraph().getOWLClassByIdentifier(ann.getCls());
			Collection<ExtensionExpression> exts = ann.getExtensionExpressions();
			if (exts != null && exts.size() > 0) {
				for (ExtensionExpression ext : exts) {
					OWLClass rwCls = folder.mapExt(annotatedToClass, ext);
					if (rwCls != null) {
						Set<OWLClass> newClasses = getMSCs(annotatedToClass, rwCls);
						for (OWLClass c : newClasses) {
							predictions.add(getPrediction(ann, c, e.getId(), ann.getCls()));
						}
					}
				}
			}
		}


		return predictions;
	}

	private Set<OWLClass> getMSCs(OWLClass orig, OWLClass xpCls) {
		Set<OWLClass> origAncs = reasoner.getSuperClasses(orig, false).getFlattened();
		Set<OWLClass> ancs = reasoner.getSuperClasses(xpCls, false).getFlattened();
		ancs.addAll(reasoner.getEquivalentClasses(xpCls).getEntities());
		Set<OWLClass> mscs = new HashSet<OWLClass>();
		//LOG.info("anc( "+xpCls+" ) = "+ancs);
		for (OWLClass anc : ancs) {
			//LOG.info("anc = "+anc);
			if (isPrecomposed(anc)) {
				if (!anc.equals(orig) && !origAncs.contains(anc)) {
					mscs.add(anc);
				}
			}
		}

		return mscs;
	}

	private boolean isPrecomposed(OWLClass c) {
		return !folder.foldedClassMap.containsKey(c);
	}

	protected Prediction getPrediction(GeneAnnotation ann, OWLClass c, String bioentity, String with) {
		GeneAnnotation annP = new GeneAnnotation(ann);
		annP.setBioentity(bioentity);
		annP.setCls(getGraph().getIdentifier(c));
		annP.setEvidenceCls("IC");
		annP.setWithExpression(with);
		// TODO - evidence
		Prediction prediction = new Prediction(annP);
//		LOG.info("prediction="+prediction);
		return prediction;
	}

	@Override
	public void dispose() {
		if (reasoner != null) {
			reasoner.dispose();
		}
	}

}
