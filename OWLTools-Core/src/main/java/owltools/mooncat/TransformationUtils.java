package owltools.mooncat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import owltools.graph.OWLGraphWrapper;

/**
 * Static methods for performing common logical transforms and view creation on OWL ontologies
 * 
 * @author cjm
 *
 */
public class TransformationUtils {

	//	public static void nameAnonymousExpressions(OWLOntology srcOntology,
	//			OWLOntology tgtOntology,
	//			Map<OWLClass,OWLClassExpression> qmap,
	//			ClassExpressionType cetype) {
	//
	//		OWLOntologyManager mgr = srcOntology.getOWLOntologyManager();
	//		for (OWLOntology ont : srcOntology.getImportsClosure()) {
	//			for (OWLAxiom ax : srcOntology.getAxioms()) {
	//				for (OWLClassExpression x : ax.getNestedClassExpressions()) {
	//					if (x.isAnonymous()) {
	//						if (x.getClassExpressionType().equals(cetype)) {
	//							IRI iri  = getSkolemIRI(x);
	//						}
	//					}
	//				}
	//			}
	//		}
	//		
	//	}

	/**
	 * Names all inner ObjectSomeValuesFrom expressions
	 * 
	 * @param srcOntology
	 * @param tgtOntology
	 * @param qmap
	 * @param isAddLabels
	 * @return
	 */
	public static Map<OWLClass, OWLClassExpression> nameObjectSomeValuesFrom(OWLOntology srcOntology,
			OWLOntology tgtOntology,
			Map<OWLClass,OWLClassExpression> qmap,
			boolean isAddLabels) {

		if (qmap == null) 
			qmap = new HashMap<OWLClass, OWLClassExpression>();
		OWLOntologyManager mgr = srcOntology.getOWLOntologyManager();
		OWLDataFactory df = mgr.getOWLDataFactory();
		for (OWLOntology ont : srcOntology.getImportsClosure()) {
			for (OWLAxiom ax : srcOntology.getAxioms()) {
				for (OWLClassExpression x : ax.getNestedClassExpressions()) {
					if (x instanceof OWLObjectSomeValuesFrom) {
						OWLObjectSomeValuesFrom svf = (OWLObjectSomeValuesFrom)x;
						OWLClass filler = (OWLClass) svf.getFiller();
						OWLObjectProperty p = (OWLObjectProperty) svf.getProperty();
						IRI iri = getSkolemIRI(filler, p);
						OWLClass c = df.getOWLClass(iri);
						mgr.addAxiom(tgtOntology, df.getOWLEquivalentClassesAxiom(c, svf));
						qmap.put(c, svf);
						if (isAddLabels) {
							for (OWLAnnotation ann : filler.getAnnotations(srcOntology, df.getRDFSLabel())) {
								mgr.addAxiom(tgtOntology, df.getOWLAnnotationAssertionAxiom(c.getIRI(), ann));
							}
						}
					}
				}
			}
		}
		return qmap;

	}

	public static Map<OWLClass, OWLClass> createObjectPropertyView(OWLOntology srcOntology,
			OWLOntology tgtOntology,
			OWLObjectProperty p,
			Map<OWLClass,OWLClass> qmap,
			boolean isAddLabels) {

		if (qmap == null) 
			qmap = new HashMap<OWLClass, OWLClass>();
		OWLOntologyManager mgr = srcOntology.getOWLOntologyManager();
		OWLDataFactory df = mgr.getOWLDataFactory();
		
		String plabel = "";
		for (OWLAnnotation ann : p.getAnnotations(srcOntology, df.getRDFSLabel())) {
			plabel = ((OWLLiteral) ann.getValue()).getLiteral();
		}
		for (OWLClass filler : srcOntology.getClassesInSignature(true)) {
			IRI iri = getSkolemIRI(filler, p);
			OWLClass c = df.getOWLClass(iri);
			OWLObjectSomeValuesFrom svf = df.getOWLObjectSomeValuesFrom(p, filler);
			mgr.addAxiom(tgtOntology, df.getOWLEquivalentClassesAxiom(c, svf));
			qmap.put(filler, c);
			if (isAddLabels) {
				for (OWLAnnotation ann : filler.getAnnotations(srcOntology, df.getRDFSLabel())) {
					String label = ((OWLLiteral) ann.getValue()).getLiteral();
					mgr.addAxiom(tgtOntology, df.getOWLAnnotationAssertionAxiom(ann.getProperty(),
							c.getIRI(), 
							df.getOWLLiteral(plabel +" "+label)));
				}

			}
		}
		return qmap;

	}

	//	protected static IRI getSkolemIRI(OWLClassExpression x) {
	//		if (x instanceof OWLObjectSomeValuesFrom) {
	//			OWLObjectSomeValuesFrom y = (OWLObjectSomeValuesFrom)x;
	//			return getSkolemIRI(y.getFiller(), y.getProperty());
	//		}
	//		
	//	}

	protected static IRI getSkolemIRI(OWLEntity... objsArr) {
		return getSkolemIRI(new ArrayList<OWLEntity>(Arrays.asList(objsArr)));
	}
	protected static IRI getSkolemIRI(List<OWLEntity> objs) {
		// 
		IRI iri;
		StringBuffer sb = new StringBuffer();
		for (OWLEntity obj : objs) {
			sb.append("_"+getFragmentID(obj));
		}
		iri = IRI.create("http://x.org"+sb.toString());
		return iri;
	}

	protected static String getFragmentID(OWLObject obj) {
		if (obj instanceof OWLNamedObject) {
			return ((OWLNamedObject) obj).getIRI().toString().replaceAll(".*/", "");
		}
		return UUID.randomUUID().toString();
	}

	public static void addLabel(OWLNamedIndividual i, OWLGraphWrapper g, OWLReasoner reasoner) {
		OWLOntology ontology = g.getSourceOntology();
		Set<OWLClass> types = new HashSet<OWLClass>();
		if (reasoner == null) {
			for (OWLClassExpression x : i.getTypes(ontology)) {
				if (!x.isAnonymous()) {
					types.add((OWLClass) x);
				}
			}
		}
		else {
			 types = reasoner.getTypes(i, true).getFlattened();
		}
		StringBuffer iLabel = null;
		for (OWLClass type : types) {
			String label = g.getLabel(type);
			if (iLabel == null)
				iLabel = new StringBuffer("a");
			else
				iLabel.append(" & ");
			iLabel.append(" "+label);
		}
		OWLDataFactory df = g.getDataFactory();
		OWLAxiom ax =
				df.getOWLAnnotationAssertionAxiom(df.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI()),
				i.getIRI(), 
				df.getOWLLiteral(iLabel.toString()));
		g.getManager().addAxiom(ontology,
				ax);
	}

}
