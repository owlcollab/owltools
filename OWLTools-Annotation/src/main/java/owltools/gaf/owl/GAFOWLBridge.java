package owltools.gaf.owl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectHasValue;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLPropertyExpression;

import owltools.gaf.Bioentity;
import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.graph.OWLGraphWrapper;

public class GAFOWLBridge {

	private OWLOntology targetOntology;
	private OWLGraphWrapper graph;
	private Map<Vocab,IRI> vocabMap = new HashMap<Vocab,IRI>();
	
	private enum BioentityMapping { NONE, CLASS_EXPRESSION, INDIVIDUAL };
	
	// config
	private BioentityMapping bioentityMapping = BioentityMapping.CLASS_EXPRESSION;
	

	public enum Vocab {
		ACTIVELY_PARTICIPATES_IN, PART_OF,
		DESCRIBES, SOURCE, PROTOTYPICALLY
	}

	public GAFOWLBridge(OWLGraphWrapper g) {
		graph = g;
		targetOntology = g.getSourceOntology();
		addVocabMapDefaults();
	}

	public GAFOWLBridge(OWLGraphWrapper g, OWLOntology tgtOnt) {
		this(g);
		targetOntology = tgtOnt;
	}

	private void addVocabMapDefaults() {
		addVocabMap(Vocab.PART_OF, "BFO_0000050");
		addVocabMap(Vocab.ACTIVELY_PARTICIPATES_IN, "RO_0002217", "actively participates in");
		addVocabMap(Vocab.PROTOTYPICALLY, "RO_0002214", "has prototype"); // canonically?
		addVocabMap(Vocab.DESCRIBES, "IAO_0000136", "is about");
	}

	private void addVocabMap(Vocab v, String s) {
		vocabMap.put(v, IRI.create("http://purl.obolibrary.org/obo/"+s));
	}

	private void addVocabMap(Vocab v, String s, String label) {
		IRI iri = IRI.create("http://purl.obolibrary.org/obo/"+s);
		vocabMap.put(v, iri);
		OWLDataFactory fac = graph.getDataFactory();
		addAxiom(fac.getOWLAnnotationAssertionAxiom(fac.getRDFSLabel(),
				iri,
				fac.getOWLLiteral(label)));
	}
	

	public OWLOntology getTargetOntology() {
		return targetOntology;
	}

	public void setTargetOntology(OWLOntology targetOntology) {
		this.targetOntology = targetOntology;
	}

	/**
	 * @param gafdoc
	 * @return
	 */
	public OWLOntology translate(GafDocument gafdoc) {

		translateBioentities(gafdoc);
		translateGeneAnnotations(gafdoc);

		return targetOntology;
	}

	private void translateGeneAnnotations(GafDocument gafdoc) {
		for (GeneAnnotation a : gafdoc.getGeneAnnotations()) {
			translateGeneAnnotation(a);
		}

	}


	private void translateBioentities(GafDocument gafdoc) {
		for (Bioentity e : gafdoc.getBioentities()) {
			translateBioentity(e);
		}
	}

	private String getAnnotationId(GeneAnnotation a) {
		return a.getBioentity() + "-" + a.getCls();
	}

	private void translateGeneAnnotation(GeneAnnotation a) {
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		OWLDataFactory fac = graph.getDataFactory();
		OWLClass e = getOWLClass(a.getBioentity());
		OWLClass t = getOWLClass(a.getCls());
		OWLObjectProperty p = getGeneAnnotationRelation(a);
		OWLObjectSomeValuesFrom r =
			fac.getOWLObjectSomeValuesFrom(p, t);
		// e.g. Shh and actively_participates_in some 'heart development'
		// todo - product
		OWLClassExpression x =
			fac.getOWLObjectIntersectionOf(e, r);
		OWLObjectProperty pDescribes = getGeneAnnotationObjectProperty(Vocab.DESCRIBES);
		String aid = getAnnotationId(a);
		OWLNamedIndividual iAnn = fac.getOWLNamedIndividual(graph.getIRIByIdentifier(aid));
		OWLObjectSomeValuesFrom dx =
			fac.getOWLObjectSomeValuesFrom(pDescribes, x);		
		axioms.add(fac.getOWLClassAssertionAxiom(dx, iAnn));
		// TODO - text description of annotation

		if (bioentityMapping != BioentityMapping.NONE) {
			// PROTOTYPE RELATIONSHIP
			OWLObjectProperty pProto = getGeneAnnotationObjectProperty(Vocab.PROTOTYPICALLY);
			OWLClassExpression ce = null;
			if (bioentityMapping == BioentityMapping.INDIVIDUAL) {
				//  E.g. Shh[cls] SubClassOf has_proto VALUE _:x, where _:x Type act_ptpt_in SOME 'heart dev'
				OWLAnonymousIndividual anonInd = fac.getOWLAnonymousIndividual();
				axioms.add(fac.getOWLClassAssertionAxiom(r, anonInd));
				ce = fac.getOWLObjectHasValue(pProto, anonInd);
			}
			else {
				ce = fac.getOWLObjectSomeValuesFrom(pProto, r);
			}
			axioms.add(fac.getOWLSubClassOfAxiom(e, ce));
		}

		addAxioms(axioms);
	}

	private OWLAnnotationProperty getGeneAnnotationAnnotationProperty(Vocab v) {
		return graph.getDataFactory().getOWLAnnotationProperty(getGeneAnnotationVocabIRI(v));
	}

	private OWLObjectProperty getGeneAnnotationObjectProperty(Vocab v) {
		return graph.getDataFactory().getOWLObjectProperty(getGeneAnnotationVocabIRI(v));
	}

	private IRI getGeneAnnotationVocabIRI(Vocab v) {
		return vocabMap.get(v);
	}

	private OWLObjectProperty getGeneAnnotationRelation(GeneAnnotation a) {
		String relation = a.getRelation();
		Vocab v = Vocab.valueOf(relation.toUpperCase());
		if (v != null)
			return getGeneAnnotationObjectProperty(v);
		OWLObjectProperty op = graph.getOWLObjectPropertyByIdentifier(relation);
		if (op != null)
			return op;
		// TODO
		return getGeneAnnotationObjectProperty(Vocab.ACTIVELY_PARTICIPATES_IN);
	}
	

	private OWLClass getOWLClass(String id) {
		IRI iri = graph.getIRIByIdentifier(id);
		return graph.getDataFactory().getOWLClass(iri);
	}

	private void translateBioentity(Bioentity e) {
		OWLDataFactory fac = graph.getDataFactory();
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		OWLClass cls = getOWLClass(e.getId());
		axioms.add(fac.getOWLAnnotationAssertionAxiom(fac.getRDFSLabel(),
				cls.getIRI(),
				fac.getOWLLiteral(e.getSymbol())));
		// TODO - other properties
		addAxioms(axioms);


	}

	private void addAxioms(Set<OWLAxiom> axioms) {
		graph.getManager().addAxioms(targetOntology, axioms);
	}

	private void addAxiom(OWLAxiom axiom) {
		graph.getManager().addAxiom(targetOntology, axiom);
	}
}
