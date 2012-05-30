package owltools.gaf.owl;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationValue;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.gaf.Bioentity;
import owltools.gaf.ExtensionExpression;
import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.graph.OWLGraphWrapper;

public class GAFOWLBridge {

	private Logger LOG = Logger.getLogger(GAFOWLBridge.class);


	private OWLOntology targetOntology;
	private OWLGraphWrapper graph;
	private Map<Vocab,IRI> vocabMap = new HashMap<Vocab,IRI>();
	private Map<String,OWLObjectProperty> shorthandMap = new HashMap<String,OWLObjectProperty>();

	private enum BioentityMapping { NONE, CLASS_EXPRESSION, INDIVIDUAL };

	// config
	private BioentityMapping bioentityMapping = BioentityMapping.CLASS_EXPRESSION;
	private boolean isGenerateIndividuals = true;


	public enum Vocab {
		ACTIVELY_PARTICIPATES_IN, PART_OF,
		DESCRIBES, SOURCE, PROTOTYPICALLY,
		IN_TAXON
	}

	public GAFOWLBridge(OWLGraphWrapper g) {
		graph = g;
		targetOntology = g.getSourceOntology();
		addVocabMapDefaults();
		makeShorthandMap();
	}

	/**
	 * The ontology generated from the gaf will be placed in tgtOnt
	 * 
	 * The graphwrapper object should include ontologies required to resolve certain entities,
	 * including the relations used in col16. In future it will also be used to translate GAF evidence
	 * codes into ECO class IRIs.
	 * 
	 * These ontologies could be the main ontology or support ontologies. A standard pattern is to have
	 * GO as the main, ro.owl and go/extensions/gorel.owl as support. (gorel is where many of the c16 relations
	 * are declared)
	 * 
	 * @param g
	 * @param tgtOnt
	 */
	public GAFOWLBridge(OWLGraphWrapper g, OWLOntology tgtOnt) {
		this(g);
		targetOntology = tgtOnt;
		makeShorthandMap();
	}

	private void addVocabMapDefaults() {
		addVocabMap(Vocab.PART_OF, "BFO_0000050");
		addVocabMap(Vocab.ACTIVELY_PARTICIPATES_IN, "RO_0002217", "actively participates in");
		addVocabMap(Vocab.PROTOTYPICALLY, "RO_0002214", "has prototype"); // canonically?
		addVocabMap(Vocab.DESCRIBES, "IAO_0000136", "is about");
		addVocabMap(Vocab.IN_TAXON, "RO_0002162", "is about");
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


	public boolean isGenerateIndividuals() {
		return isGenerateIndividuals;
	}

	public void setGenerateIndividuals(boolean isGenerateIndividuals) {
		this.isGenerateIndividuals = isGenerateIndividuals;
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

	private String getAnnotationDescription(GeneAnnotation a) {
		String clsDesc = a.getCls();
		OWLClass owlCls = graph.getOWLClassByIdentifier(a.getCls());
		if (owlCls != null) {
			clsDesc = graph.getLabelOrDisplayId(owlCls);
		}
		return "annotation of "+a.getBioentityObject().getSymbol() + " to " + clsDesc;
	}

	private void translateGeneAnnotation(GeneAnnotation a) {
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		OWLDataFactory fac = graph.getDataFactory();
		OWLClass e = getOWLClass(a.getBioentity());
		OWLClassExpression annotatedToClass = getOWLClass(a.getCls());
		Collection<ExtensionExpression> exts = a.getExtensionExpressions();
		if (exts != null && !exts.isEmpty()) {
			HashSet<OWLClassExpression> ops = new HashSet<OWLClassExpression>();
			ops.add(annotatedToClass);
			for (ExtensionExpression ext : exts) {
				OWLObjectProperty p = getObjectPropertyByShorthand(ext.getRelation());
				OWLClass filler = getOWLClass(ext.getCls());
				//LOG.info(" EXT:"+p+" "+filler);
				ops.add(fac.getOWLObjectSomeValuesFrom(p, filler));
			}
			annotatedToClass = fac.getOWLObjectIntersectionOf(ops);
		}

		OWLObjectProperty p = getGeneAnnotationRelation(a);
		OWLObjectSomeValuesFrom r =
			fac.getOWLObjectSomeValuesFrom(p, annotatedToClass);
		// e.g. Shh and actively_participates_in some 'heart development'
		// todo - product
		OWLClassExpression x =
			fac.getOWLObjectIntersectionOf(e, r);

		// create or fetch the unique Id of the gene annotation
		String geneAnnotationId = getAnnotationId(a);

		// the gene annotation instances _describes_ a gene/product in some context
		OWLObjectProperty pDescribes = getGeneAnnotationObjectProperty(Vocab.DESCRIBES);

		if (this.isGenerateIndividuals) {
			// Create an instance for every gene annotation
			OWLNamedIndividual iAnn = fac.getOWLNamedIndividual(graph.getIRIByIdentifier(geneAnnotationId));

			OWLObjectSomeValuesFrom dx =
				fac.getOWLObjectSomeValuesFrom(pDescribes, x);		
			axioms.add(fac.getOWLClassAssertionAxiom(dx, iAnn));
			OWLAnnotationProperty labelProperty = fac.getRDFSLabel();
			String desc = this.getAnnotationDescription(a);
			OWLAnnotation labelAnnotation = fac.getOWLAnnotation(labelProperty, fac.getOWLLiteral(desc));
			axioms.add(fac.getOWLAnnotationAssertionAxiom(iAnn.getIRI(), labelAnnotation));
			// TODO - annotations on iAnn; evidence etc
		}

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

		// experimental: annotation assertions
		if (false) {
			// TODO
			AnnotationValue v;
		}

		addAxioms(axioms);
	}

	private void makeShorthandMap() {
		OWLAnnotationProperty shp = 
			graph.getDataFactory().getOWLAnnotationProperty(IRI.create("http://www.geneontology.org/formats/oboInOwl#shorthand"));
		for (OWLOntology o : graph.getAllOntologies()) {
			for (OWLObjectProperty q : o.getObjectPropertiesInSignature(true)) {
				String v = graph.getAnnotationValue(q, shp);
				if (v != null)
					shorthandMap.put(v, q);
			}
		}
	}


	private OWLObjectProperty getObjectPropertyByShorthand(String id) {
		if (shorthandMap.containsKey(id)) {
			return shorthandMap.get(id);
		}
		OWLObjectProperty p = graph.getOWLObjectPropertyByIdentifier(id);
		return p;
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

		// --label---
		axioms.add(fac.getOWLAnnotationAssertionAxiom(fac.getRDFSLabel(),
				cls.getIRI(),
				fac.getOWLLiteral(e.getSymbol())));
		
		// --taxon--
		OWLClass taxCls = getOWLClass(e.getNcbiTaxonId()); // todo - cache
		axioms.add(fac.getOWLSubClassOfAxiom(cls, 
				fac.getOWLObjectSomeValuesFrom(getGeneAnnotationObjectProperty(Vocab.IN_TAXON), 
						taxCls)));
		
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
