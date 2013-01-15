package owltools.gaf.owl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

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

public class AnnotationExtensionFolder extends GAFOWLBridge {

	int lastId = 0;

	public AnnotationExtensionFolder(OWLGraphWrapper g) {
		super(g);
	}

	public void fold(GafDocument gdoc) {
		List<GeneAnnotation> newAnns = new ArrayList<GeneAnnotation>();
		for (GeneAnnotation ann : gdoc.getGeneAnnotations()) {
			if (ann.getExtensionExpressions().size() > 0) {
				newAnns.addAll(fold(gdoc, ann));
			}
			else {
				newAnns.add(ann);
			}
		}
		gdoc.setGeneAnnotations(newAnns);
	}

	public Collection<GeneAnnotation> fold(GafDocument gdoc, GeneAnnotation ann) {
		List<GeneAnnotation> newAnns = new ArrayList<GeneAnnotation>();
		OWLDataFactory fac = graph.getDataFactory();
		OWLClass annotatedToClass = getOWLClass(ann.getCls());
		// c16
		Collection<ExtensionExpression> exts = ann.getExtensionExpressions();
		if (exts != null && !exts.isEmpty()) {

			// TODO - fix model so that disjunctions and conjunctions are supported.
			//  treat all as disjunction for now
			for (ExtensionExpression ext : exts) {
				HashSet<OWLClassExpression> ops = new HashSet<OWLClassExpression>();
				ops.add(annotatedToClass);
				OWLObjectProperty p = getObjectPropertyByShorthand(ext.getRelation());
				OWLClass filler = getOWLClass(ext.getCls());
				//LOG.info(" EXT:"+p+" "+filler);
				ops.add(fac.getOWLObjectSomeValuesFrom(p, filler));

				OWLClassExpression cx = fac.getOWLObjectIntersectionOf(ops);

				String idExt = ext.getRelation()+"-"+ext.getCls();
				idExt = idExt.replaceAll(":", "_");
				//IRI ncIRI = IRI.create(annotatedToClass.getIRI().toString()+"-"+idExt);
				lastId++;
				IRI ncIRI = IRI.create("http://purl.obolibrary.org/obo/GOTEMP_"+lastId);
				OWLClass nc = fac.getOWLClass(ncIRI);
				OWLEquivalentClassesAxiom eca = fac.getOWLEquivalentClassesAxiom(nc, cx);
				graph.getManager().addAxiom(graph.getSourceOntology(), eca);
				String annLabel = graph.getLabel(annotatedToClass);
				OWLAnnotationAssertionAxiom aaa = fac.getOWLAnnotationAssertionAxiom(
						fac.getRDFSLabel(),
						ncIRI,
						fac.getOWLLiteral(annLabel+" "+idExt));
				graph.getManager().addAxiom(graph.getSourceOntology(), aaa);
				graph.getManager().addAxiom(graph.getSourceOntology(), fac.getOWLDeclarationAxiom(nc));
				GeneAnnotation newAnn = new GeneAnnotation(ann);
				newAnn.setExtensionExpression("");
				newAnn.setCls(graph.getIdentifier(ncIRI));
				newAnns.add(newAnn);
			}
		}
		return newAnns;

	}

}
