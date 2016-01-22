package owltools.gaf.owl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import owltools.gaf.ExtensionExpression;
import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.graph.OWLGraphWrapper;

/**
 * @author cjm
 * given an annotation to a pre-existing term, 
 * this will return a set of zero or more annotations to new terms that are generated from
 * folding the annotation extensions into newly created term 
 * 
 * See:
 * http://code.google.com/p/owltools/wiki/AnnotationExtensionFolding
 * 
 */
public class AnnotationExtensionFolder extends GAFOWLBridge {
	
	private static final Logger LOG = Logger.getLogger(AnnotationExtensionFolder.class);

	int lastId = 0;
	public Map <OWLClass,OWLClassExpression> foldedClassMap = null;
	Map <OWLClassExpression,OWLClass> revFoldedClassMap = null;

	public AnnotationExtensionFolder(OWLGraphWrapper g) {
		super(g);
	}

	public void fold(GafDocument gdoc) {
		fold(gdoc, true);
	}
	public void fold(GafDocument gdoc, boolean isReplace) {

		List<GeneAnnotation> newAnns = new ArrayList<GeneAnnotation>();
		for (GeneAnnotation ann : gdoc.getGeneAnnotations()) {
			if (ann.getExtensionExpressions().size() > 0) {
				newAnns.addAll(fold(gdoc, ann));
			}
			else {
				newAnns.add(ann);
			}
		}
		if (isReplace) {
			gdoc.setGeneAnnotations(newAnns);
		}
	}


	/**
	 * given an annotation to a pre-existing term, 
	 * this will return a set of zero or more annotations to new terms that are generated from
	 * folding the annotation extensions into newly created term 
	 * 
	 * @param gdoc
	 * @param ann
	 * @return annotations
	 */
	public Collection<GeneAnnotation> fold(GafDocument gdoc, GeneAnnotation ann) {

		if (foldedClassMap == null) {
			foldedClassMap = new HashMap <OWLClass,OWLClassExpression>();
			revFoldedClassMap = new HashMap <OWLClassExpression, OWLClass>();
		}

		List<GeneAnnotation> newAnns = new ArrayList<GeneAnnotation>();
		OWLClass annotatedToClass = getOWLClass(ann.getCls());
		// c16
		List<List<ExtensionExpression>> extsGroups = ann.getExtensionExpressions();
		if (annotatedToClass != null && extsGroups != null && !extsGroups.isEmpty()) {
			for(List<ExtensionExpression> group : extsGroups) {
				OWLClass nc = mapExt(annotatedToClass, group);
				if (nc == null) {
					continue;
				}
				GeneAnnotation newAnn = new GeneAnnotation(ann);
				newAnn.setExtensionExpressions(Collections.<List<ExtensionExpression>>emptyList());
				newAnn.setCls(graph.getIdentifier(nc));
				newAnns.add(newAnn);

				LOG.debug("NEW: "+newAnn);
			}
		}
		return newAnns;

	}

	public OWLClass mapExt(OWLClass annotatedToClass, List<ExtensionExpression> exts) {
		HashSet<OWLClassExpression> ops = new HashSet<OWLClassExpression>();
		ops.add(annotatedToClass);
		OWLDataFactory fac = graph.getDataFactory();

		for (ExtensionExpression ext : exts) {
			OWLObjectProperty p = getObjectPropertyByShorthand(ext.getRelation());
			if (p == null) {
				return null;
			}
			OWLClass filler = getOWLClass(ext.getCls());
			if (filler == null) {
				return null;
			}
			ops.add(fac.getOWLObjectSomeValuesFrom(p, filler));
		}

		OWLClassExpression cx = fac.getOWLObjectIntersectionOf(ops);

		OWLClass nc;
		IRI	ncIRI;
		if (revFoldedClassMap.containsKey(cx)) {
			nc = revFoldedClassMap.get(cx);
			ncIRI = nc.getIRI();
		}
		else {

			StringBuilder labelBuilder = new StringBuilder();
			labelBuilder.append(graph.getLabelOrDisplayId(annotatedToClass));
			labelBuilder.append(" ");
			for(ExtensionExpression ext : exts) {
				labelBuilder.append(" and ");
				labelBuilder.append(ext.getRelation());
				labelBuilder.append(" some ");
				OWLClass xc = graph.getOWLClassByIdentifierNoAltIds(ext.getCls());
				if (xc != null) {
					labelBuilder.append(graph.getLabelOrDisplayId(xc));
				}
				else {
					labelBuilder.append(ext.getCls());
				}
			}

			lastId++;
			ncIRI = IRI.create("http://purl.obolibrary.org/obo/GOTEMP_"+lastId);
			nc = fac.getOWLClass(ncIRI);
			OWLEquivalentClassesAxiom eca = fac.getOWLEquivalentClassesAxiom(nc, cx);
			graph.getManager().addAxiom(graph.getSourceOntology(), eca);
			
			OWLAnnotationAssertionAxiom aaa = fac.getOWLAnnotationAssertionAxiom(
					fac.getRDFSLabel(),
					ncIRI,
					fac.getOWLLiteral(labelBuilder.toString()));
			graph.getManager().addAxiom(graph.getSourceOntology(), aaa);
			graph.getManager().addAxiom(graph.getSourceOntology(), fac.getOWLDeclarationAxiom(nc));
			revFoldedClassMap.put(cx, nc);
			foldedClassMap.put(nc, cx); // 
		}

		return nc;
	}

	public Map<OWLClass, OWLClassExpression> getFoldedClassMap() {
		return foldedClassMap;
	}

	public void setFoldedClassMap(Map<OWLClass, OWLClassExpression> foldedClassMap) {
		this.foldedClassMap = foldedClassMap;
	}



}
