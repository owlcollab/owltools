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
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import owltools.gaf.*;
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
	final OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();

	OWLReasoner reasoner;
	public FoldBasedPredictor(GafDocument gafDocument,
			OWLGraphWrapper graph) {
		super(gafDocument, graph);
		fold();
	}

	public void fold() {
		folder = new AnnotationExtensionFolder(this.getGraph());
		folder.fold(getGafDocument(), false); // do not replace
		LOG.info("Candidate classes to deepen: "+folder.rewriteMap.size());
		reasoner = reasonerFactory.createReasoner(getGraph().getSourceOntology());
	}

	public Set<Prediction> predict(String bioentity) {
		Set<Prediction> predictions = new HashSet<Prediction>();
		Set<GeneAnnotation> anns = getGafDocument().getGeneAnnotations(bioentity);
		//LOG.info("N="+anns.size());
		for (GeneAnnotation ann : anns) {
			//LOG.info(ann);
			if (ann.getEvidenceCls().equals("ND")) {
				continue;
			}
			OWLClass annotatedToClass = getGraph().getOWLClassByIdentifier(ann.getCls());
			//LOG.info("?:"+annotatedToClass+" "+ann.getCls());
			if (folder.rewriteMap.containsKey(annotatedToClass)) {
				OWLClass rwCls = folder.rewriteMap.get(annotatedToClass);
				//LOG.info("Test:"+annotatedToClass);			
				//LOG.info("EQ:"+folder.foldedClassMap.get(rwCls));
				Set<OWLClass> newClasses = getMSCs(annotatedToClass, rwCls);
				for (OWLClass c : newClasses) {
					LOG.info("MSC:"+c);
					predictions.add(getPrediction(c, bioentity, ann.getCls()));
				}
			}
			//Collection<GeneAnnotation> newAnns = folder.fold(getGafDocument(), ann);
		}


		return predictions;
	}

	private Set<OWLClass> getMSCs(OWLClass orig, OWLClass c) {
		Set<OWLClass> origAncs = reasoner.getSuperClasses(orig, false).getFlattened();
		Set<OWLClass> ancs = reasoner.getSuperClasses(c, false).getFlattened();
		ancs.addAll(reasoner.getEquivalentClasses(c).getEntities());
		Set<OWLClass> mscs = new HashSet<OWLClass>();
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
