package owltools.mooncat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

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
		for (OWLClass filler : srcOntology.getClassesInSignature(true)) {
			IRI iri = getSkolemIRI(filler, p);
			OWLClass c = df.getOWLClass(iri);
			OWLObjectSomeValuesFrom svf = df.getOWLObjectSomeValuesFrom(p, filler);
			mgr.addAxiom(tgtOntology, df.getOWLEquivalentClassesAxiom(c, svf));
			qmap.put(filler, c);
			if (isAddLabels) {// TODO - rewrite
				for (OWLAnnotation ann : filler.getAnnotations(srcOntology, df.getRDFSLabel())) {
					mgr.addAxiom(tgtOntology, df.getOWLAnnotationAssertionAxiom(c.getIRI(), ann));
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
		return getSkolemIRI(new HashSet<OWLEntity>(Arrays.asList(objsArr)));
	}
	protected static IRI getSkolemIRI(Set<OWLEntity> objs) {
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


}
