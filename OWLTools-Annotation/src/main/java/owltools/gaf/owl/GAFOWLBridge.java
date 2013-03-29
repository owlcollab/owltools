package owltools.gaf.owl;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationValue;

import org.apache.log4j.Logger;
import org.obolibrary.obo2owl.Obo2OWLConstants;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.gaf.Bioentity;
import owltools.gaf.ExtensionExpression;
import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.graph.OWLGraphWrapper;

public class GAFOWLBridge {

	private static final Logger LOG = Logger.getLogger(GAFOWLBridge.class);

	private OWLOntology targetOntology;
	protected OWLGraphWrapper graph;
	private Map<Vocab,IRI> vocabMap = new HashMap<Vocab,IRI>();
	private Map<String,OWLObjectProperty> shorthandMap = new HashMap<String,OWLObjectProperty>();

	public enum BioentityMapping { NONE, CLASS_EXPRESSION, NAMED_CLASS, INDIVIDUAL };

	// config
	private BioentityMapping bioentityMapping = BioentityMapping.CLASS_EXPRESSION;
	private boolean isGenerateIndividuals = true;

	public static IRI GAF_LINE_NUMBER_ANNOTATION_PROPERTY_IRI = IRI.create("http://gaf/line_number");

	public enum Vocab {
		ACTIVELY_PARTICIPATES_IN, PART_OF,
		DESCRIBES, SOURCE, PROTOTYPICALLY,
		IN_TAXON, ENABLED_BY, INVOLVED_IN, CONTRIBUTES_TO, COLOCALIZES_WITH
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
		addVocabMap(Vocab.INVOLVED_IN, "RO_0002331", "involved in");
		addVocabMap(Vocab.ENABLED_BY, "RO_0002333", "enabled by"); 
		addVocabMap(Vocab.COLOCALIZES_WITH, "RO_0002325", "colocalizes with");
		addVocabMap(Vocab.CONTRIBUTES_TO, "RO_0002326", "contributes to"); 
		addVocabMap(Vocab.DESCRIBES, "IAO_0000136", "is about");
		addVocabMap(Vocab.IN_TAXON, "RO_0002162", "in taxon");
	}

	private void addVocabMap(Vocab v, String s) {
		vocabMap.put(v, IRI.create(Obo2OWLConstants.DEFAULT_IRI_PREFIX+s));
	}

	private void addVocabMap(Vocab v, String s, String label) {
		IRI iri = IRI.create(Obo2OWLConstants.DEFAULT_IRI_PREFIX+s);
		vocabMap.put(v, iri);
		OWLDataFactory fac = graph.getDataFactory();
		addAxiom(fac.getOWLAnnotationAssertionAxiom(fac.getRDFSLabel(),
				iri,
				fac.getOWLLiteral(label)));
	}

	/**
	 * @return the bioentityMapping
	 */
	public BioentityMapping getBioentityMapping() {
		return bioentityMapping;
	}

	/**
	 * @param bioentityMapping the bioentityMapping to set
	 */
	public void setBioentityMapping(BioentityMapping bioentityMapping) {
		this.bioentityMapping = bioentityMapping;
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
	 * @return translated ontology
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
		// c16 - TODO - split '|'s into separate annotations
		Collection<ExtensionExpression> exts = a.getExtensionExpressions();
		if (exts != null && !exts.isEmpty()) {
			HashSet<OWLClassExpression> ops = new HashSet<OWLClassExpression>();
			ops.add(annotatedToClass);
			for (ExtensionExpression ext : exts) {
				OWLObjectProperty p = getObjectPropertyByShorthand(ext.getRelation());
				if (p == null) {
					LOG.error("cannot match: "+ext.getRelation());
					p = fac.getOWLObjectProperty(IRI.create("http://purl.obolibrary.org/obo/go/unstable/"+ext.getRelation()));
				}
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
			if (bioentityMapping == BioentityMapping.INDIVIDUAL) {
				//  E.g. Shh[cls] SubClassOf has_proto VALUE _:x, where _:x Type act_ptpt_in SOME 'heart dev'
				OWLAnonymousIndividual anonInd = fac.getOWLAnonymousIndividual();
				axioms.add(fac.getOWLClassAssertionAxiom(r, anonInd));
				OWLClassExpression ce = fac.getOWLObjectHasValue(pProto, anonInd);
				axioms.add(fac.getOWLSubClassOfAxiom(e, ce));
			}
			else if (bioentityMapping == BioentityMapping.NAMED_CLASS) {
				IRI iri = graph.getIRIByIdentifier(geneAnnotationId);
				OWLClass owlClass = fac.getOWLClass(iri);
				axioms.add(fac.getOWLDeclarationAxiom(owlClass));
				
				// line number
				int lineNumber = a.getSource().getLineNumber();
				OWLAnnotationProperty property = fac.getOWLAnnotationProperty(GAF_LINE_NUMBER_ANNOTATION_PROPERTY_IRI);
				OWLAnnotation annotation = fac.getOWLAnnotation(property, fac.getOWLLiteral(lineNumber));
				axioms.add(fac.getOWLAnnotationAssertionAxiom(owlClass.getIRI(), annotation));
				
				// label
				Bioentity bioentity = a.getBioentityObject();
				String fullName = bioentity.getFullName();
				if (fullName == null) {
					fullName = bioentity.getId();
				}
				else {
					fullName = "'"+fullName+"' ("+bioentity.getId()+")";
				}
				String clsId = a.getCls();
				
				String clsLabel = clsId;
				final OWLObject owlObject = graph.getOWLObjectByIdentifier(clsId);
				if (owlObject != null) {
					String s = graph.getLabel(owlObject);
					if (s != null) {
						clsLabel = "'"+s+"' ("+clsId+")";
					}
				}
						
				
				String label = fullName + " - " + clsLabel;
				annotation = fac.getOWLAnnotation(fac.getRDFSLabel(), fac.getOWLLiteral(label));
				axioms.add(fac.getOWLAnnotationAssertionAxiom(owlClass.getIRI(), annotation));
				
				// logical definition
				axioms.add(fac.getOWLEquivalentClassesAxiom(owlClass, x));
			}
			else {
				OWLClassExpression ce = fac.getOWLObjectSomeValuesFrom(pProto, r);
				axioms.add(fac.getOWLSubClassOfAxiom(e, ce));
			}
			
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


	protected OWLObjectProperty getObjectPropertyByShorthand(String id) {
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
		//LOG.info("Mapping: "+relation);
		Vocab v = null;
		try {
			v = Vocab.valueOf(relation.toUpperCase());
		} catch (IllegalArgumentException e) {
			// ignore error
			// this is thrown, if there is no corresponding constant for the input 
		}
		if (v != null)
			return getGeneAnnotationObjectProperty(v);
		OWLObjectProperty op = graph.getOWLObjectPropertyByIdentifier(relation);
		if (op != null)
			return op;
		// TODO
		return getGeneAnnotationObjectProperty(Vocab.INVOLVED_IN);
	}


	protected OWLClass getOWLClass(String id) {
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
