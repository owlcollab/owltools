package owltools.solrj;

import java.io.Closeable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.obolibrary.obo2owl.Obo2OWLConstants;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAnnotationValueVisitorEx;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.graph.OWLGraphWrapper;
import owltools.graph.shunt.OWLShuntEdge;
import owltools.graph.shunt.OWLShuntGraph;
import owltools.graph.shunt.OWLShuntNode;
import owltools.io.OWLPrettyPrinter;
import owltools.vocab.OBOUpperVocabulary;

/**
 * A very specific class for the specific use case of loading in complex annotations from owl.
 */
public class ModelAnnotationSolrDocumentLoader extends AbstractSolrLoader implements Closeable {

	private static Logger LOG = Logger.getLogger(ModelAnnotationSolrDocumentLoader.class);
	int doc_limit_trigger = 1000; // the number of documents to add before pushing out to solr
	int current_doc_number;

	private final OWLReasoner reasoner;
	private final OWLOntology model;
	private final String modelUrl;

	private final OWLObjectProperty partOf;
	private final List<String> defaultClosureRelations;
	private final OWLObjectProperty occursIn;
	private final OWLObjectProperty enabledBy;

	private final OWLAnnotationProperty title;
	private final OWLAnnotationProperty source;
	private final OWLAnnotationProperty contributor;
	private final OWLAnnotationProperty date;
	private final OWLAnnotationProperty evidence;
	private final OWLAnnotationProperty modelState;
	private final OWLAnnotationProperty comment;
	private final OWLAnnotationProperty with;
	private final OWLAnnotationProperty layoutHintX;
	private final OWLAnnotationProperty layoutHintY;
	private final OWLAnnotationProperty templatestate;
	
	private final OWLAnnotationProperty displayLabelProp;
	private final OWLAnnotationProperty shortIdProp;
	
	private final OWLAnnotationProperty jsonProp;

	private final Set<OWLClass> bpSet;
	private final Set<String> requiredModelStates;
	private boolean skipDeprecatedModels;
	private boolean skipTemplateModels;

	public ModelAnnotationSolrDocumentLoader(String golrUrl, OWLOntology model, OWLReasoner r, String modelUrl, 
			Set<String> modelFilter, boolean skipDeprecatedModels, boolean skipTemplateModels) throws MalformedURLException {
		this(createDefaultServer(golrUrl), model, r, modelUrl, modelFilter, skipDeprecatedModels, skipTemplateModels);
	}
	
	public ModelAnnotationSolrDocumentLoader(SolrServer server, OWLOntology model, OWLReasoner r, String modelUrl, 
			Set<String> modelFilter, boolean skipDeprecatedModels, boolean skipTemplateModels) {
		super(server);
		this.model = model;
		this.reasoner = r;
		this.requiredModelStates = modelFilter;
		this.skipDeprecatedModels = skipDeprecatedModels;
		this.skipTemplateModels = skipTemplateModels;
		this.graph = new OWLGraphWrapper(model);
		this.modelUrl = modelUrl;
		current_doc_number = 0;
		OWLDataFactory df = graph.getDataFactory();
		partOf = OBOUpperVocabulary.BFO_part_of.getObjectProperty(df);
		occursIn = OBOUpperVocabulary.BFO_occurs_in.getObjectProperty(df);
		
		defaultClosureRelations = new ArrayList<String>(1);
		defaultClosureRelations.add(graph.getIdentifier(partOf));
		
		enabledBy = OBOUpperVocabulary.GOREL_enabled_by.getObjectProperty(df);
		
		title = df.getOWLAnnotationProperty(IRI.create("http://purl.org/dc/elements/1.1/title"));
		source = df.getOWLAnnotationProperty(IRI.create("http://purl.org/dc/elements/1.1/source"));
		contributor = df.getOWLAnnotationProperty(IRI.create("http://purl.org/dc/elements/1.1/contributor"));
		date = df.getOWLAnnotationProperty(IRI.create("http://purl.org/dc/elements/1.1/date"));
		evidence = df.getOWLAnnotationProperty(IRI.create("http://geneontology.org/lego/evidence"));
		modelState = df.getOWLAnnotationProperty(IRI.create("http://geneontology.org/lego/modelstate"));
		comment = df.getRDFSComment();
		with = df.getOWLAnnotationProperty(IRI.create("http://geneontology.org/lego/evidence-with"));
		layoutHintX = df.getOWLAnnotationProperty(IRI.create("http://geneontology.org/lego/hint/layout/x"));
		layoutHintY = df.getOWLAnnotationProperty(IRI.create("http://geneontology.org/lego/hint/layout/y"));
		templatestate = df.getOWLAnnotationProperty(IRI.create("http://geneontology.org/lego/templatestate"));
		
		displayLabelProp = df.getRDFSLabel();
		shortIdProp = df.getOWLAnnotationProperty(IRI.create(Obo2OWLConstants.OIOVOCAB_IRI_PREFIX+"id"));
		
		jsonProp = df.getOWLAnnotationProperty(IRI.create("http://geneontology.org/lego/json-model"));
		
		bpSet = getAspect(graph, "biological_process");
	}

	static Set<OWLClass> getAspect(OWLGraphWrapper graph, String aspect) {
		Set<OWLClass> result = new HashSet<OWLClass>();
		for(OWLClass cls : graph.getAllOWLClasses()) {
			if (cls.isBuiltIn()) {
				continue;
			}
			String id = graph.getIdentifier(cls);
			if (id.startsWith("GO:") == false) {
				continue;
			}
			String namespace = graph.getNamespace(cls);
			if (namespace != null && namespace.equals(aspect)) {
				result.add(cls);
			}
		}
		return result;
	}
	
	@Override
	public void close() throws IOException {
		graph.close();
        if (reasoner != null) {
            reasoner.dispose();
        }

	}

	@Override
	public void load() throws SolrServerException, IOException {
		LOG.info("Loading complex annotation document...");
		final OWLShuntGraph shuntGraph = createShuntGraph(graph);
		
		String modelId = null;
		String modelDate = null;
		String title = null;
		String state = null;
		String modelComment = null;
		boolean isTemplate = false;
		boolean isDeprecated = false;
		String jsonModel = null;
		Set<String> modelAnnotations = new HashSet<String>();
		for(OWLAnnotation ann : model.getAnnotations()) {
			OWLAnnotationProperty p = ann.getProperty();
			String literal = getLiteralValue(ann.getValue());
			if (literal != null) {
				if (this.contributor.equals(p)) {
					// skip certain annotations as they are axiom specific
					continue;
				}
				else if (this.date.equals(p) ) {
					modelDate = literal;
				}
				else if (this.title.equals(p)) {
					title = literal;
				}
				else if (this.modelState.equals(p)) {
					state = literal;
				}
				else if (this.shortIdProp.equals(p)) {
					modelId = literal;
				}
				else if (this.comment.equals(p)) {
					modelComment = literal;
				}
				else if (this.jsonProp.equals(p)) {
					jsonModel = literal;
				}
				else if (this.templatestate.equals(p)) {
					isTemplate = "true".equalsIgnoreCase(literal);
				}
				else if (p.isDeprecated()) {
					isDeprecated = "true".equalsIgnoreCase(literal);
				}
				else {
					modelAnnotations.add(literal);
				}
			}
		}
		if (modelId == null && model.getOntologyID().getOntologyIRI().isPresent()) {
			// fallback
			modelId = model.getOntologyID().getOntologyIRI().get().toString();
		}
		
		if (requiredModelStates != null && state != null) {
			boolean contains = requiredModelStates.contains(state);
			if (contains == false) {
				LOG.info("skipping model "+modelId+" due to model state, is: '"+state+"' required: "+requiredModelStates);
				return;
			}
		}
		if (skipDeprecatedModels && isDeprecated) {
			LOG.info("Skipping model '"+modelId+"' model is deprecated");
			return;
		}
		if (skipTemplateModels && isTemplate) {
			LOG.info("Skipping model '"+modelId+"' model is a template");
			return;
		}

		Set<OWLObjectPropertyAssertionAxiom> axioms = model.getAxioms(AxiomType.OBJECT_PROPERTY_ASSERTION);
		for (OWLObjectPropertyAssertionAxiom axiom : axioms) {
			if (enabledBy.equals(axiom.getProperty())) {
				// found an enabled by, assume object is gene or protein
				OWLIndividual mf = axiom.getSubject();
				OWLIndividual gp = axiom.getObject();
				if (mf.isNamed() && gp.isNamed()) {
					final OWLNamedIndividual gpNamed = gp.asOWLNamedIndividual();
					final Set<OWLClass> gpTypes = getTypes(gpNamed);
					final Set<OWLAnnotation> gpAnnotations = getAnnotations(axiom, gpNamed);
					final OWLNamedIndividual mfNamed = mf.asOWLNamedIndividual();
					final Set<OWLClass> mfTypes = getTypes(mfNamed);
					final Set<OWLAnnotation> mfAnnotations = getAnnotations(null, mfNamed);
					Map<OWLClass, Pair<OWLNamedIndividual, Set<OWLAnnotation>>> processes = findProcesses(mfNamed);
					Map<OWLClass, Pair<OWLNamedIndividual, Set<OWLAnnotation>>> locations = findLocations(mfNamed);
					for(OWLClass gpType : gpTypes) {
						for(OWLClass mfType : mfTypes) {
							if (processes.isEmpty() == false) {
								for(Entry<OWLClass, Pair<OWLNamedIndividual, Set<OWLAnnotation>>> entry : processes.entrySet()) {
									SolrInputDocument doc = collectUnitInfo(
											mfNamed, mfType, mfAnnotations,
											gpNamed, gpType, gpAnnotations,
											entry.getValue().getLeft(), entry.getKey(), entry.getValue().getRight(),
											locations,
											modelId, title, state, modelDate, modelUrl,
											modelAnnotations, modelComment, shuntGraph,
											jsonModel);
									addDoc(doc);
								}
							}
							else {
								SolrInputDocument doc = collectUnitInfo(
										mfNamed, mfType, mfAnnotations,
										gpNamed, gpType, gpAnnotations,
										null, null, null,
										locations,
										modelId, title, state, modelDate, modelUrl,
										modelAnnotations, modelComment, shuntGraph,
										jsonModel);
								addDoc(doc);
							}
						}
					}
				}
			}
		}
		// Get the remainder of the docs in.
		LOG.info("Doing clean-up (final) commit at " + current_doc_number + " complex annotation documents...");
		addAllAndCommit();
		LOG.info("Done.");
	}
	
	private void addDoc(SolrInputDocument doc) throws SolrServerException, IOException {
		if( doc != null ){
			add(doc);
			// Incremental commits.
			current_doc_number++;
			if( current_doc_number % doc_limit_trigger == 0 ){
				LOG.info("Processed " + doc_limit_trigger + " general ontology docs at " + current_doc_number + " and committing...");
				incrementalAddAndCommit();
			}
		}
	}
	
	private Set<OWLAnnotation> getAnnotations(OWLAxiom ax, OWLNamedIndividual i) {
		Set<OWLAnnotation> all = new HashSet<OWLAnnotation>();
		if (ax != null) {
			all.addAll(ax.getAnnotations());
		}
		if (i != null) {
			Set<OWLAnnotationAssertionAxiom> aaas = model.getAnnotationAssertionAxioms(i.getIRI());
			for(OWLAnnotationAssertionAxiom aaa : aaas) {
				all.add(aaa.getAnnotation());
			}
		}
		return all;
	}

	private Map<OWLClass, Pair<OWLNamedIndividual, Set<OWLAnnotation>>> findProcesses(OWLNamedIndividual mf) {
		Map<OWLClass, Pair<OWLNamedIndividual, Set<OWLAnnotation>>> result = new HashMap<OWLClass, Pair<OWLNamedIndividual,Set<OWLAnnotation>>>();
		Set<OWLObjectPropertyAssertionAxiom> axioms = model.getObjectPropertyAssertionAxioms(mf);
		for (OWLObjectPropertyAssertionAxiom axiom : axioms) {
			if (partOf.equals(axiom.getProperty()) && mf.equals(axiom.getSubject())) {
				// relevant axiom
				OWLIndividual bpCandidate = axiom.getObject();
				if (bpCandidate.isNamed()) {
					final OWLNamedIndividual named = bpCandidate.asOWLNamedIndividual();
					Set<OWLClass> bpTypes = getTypes(named);
					for (OWLClass bpType : bpTypes) {
						if (bpSet.contains(bpType) == false) {
							continue;
						}
						result.put(bpType, Pair.of(named, getAnnotations(axiom, named)));
					}
				}
			}
		}
		return result;
	}
	
	private Map<OWLClass, Pair<OWLNamedIndividual, Set<OWLAnnotation>>> findLocations(OWLNamedIndividual mf) {
		Map<OWLClass, Pair<OWLNamedIndividual, Set<OWLAnnotation>>> result = new HashMap<OWLClass, Pair<OWLNamedIndividual,Set<OWLAnnotation>>>();
		Set<OWLObjectPropertyAssertionAxiom> axioms = model.getObjectPropertyAssertionAxioms(mf);
		for (OWLObjectPropertyAssertionAxiom axiom : axioms) {
			if (occursIn.equals(axiom.getProperty()) && mf.equals(axiom.getSubject())) {
				// relevant axiom
				OWLIndividual locationCandidate = axiom.getObject();
				if (locationCandidate.isNamed()) {
					OWLNamedIndividual named = locationCandidate.asOWLNamedIndividual();
					Set<OWLClass> locationTypes = getTypes(named);
					for (OWLClass locationType : locationTypes) {
						result.put(locationType, Pair.of(named, getAnnotations(axiom, named)));
					}
				}
			}
		}
		return result;
	}
	
	private Set<OWLClass> getTypes(OWLNamedIndividual i) {
		final Set<OWLClass> results = new HashSet<OWLClass>();
		if (reasoner != null && reasoner.isConsistent()) {
			Set<OWLClass> inferredTypes = reasoner.getTypes(i, true).getFlattened();
			for (OWLClass cls : inferredTypes) {
				if (cls.isBuiltIn() == false) {
					results.add(cls);
				}
			}
		}
		else {
			Set<OWLClassAssertionAxiom> axioms = model.getClassAssertionAxioms(i);
			for (OWLClassAssertionAxiom axiom : axioms) {
				OWLClassExpression ce = axiom.getClassExpression();
				if (ce.isAnonymous() == false) {
					OWLClass cls = ce.asOWLClass();
					if (cls.isBuiltIn() == false) {
						results.add(cls);
					}
				}
			}
		}
		
		return results;
	}

	private Map<String,String> renderAnnotations(Set<OWLAnnotation> annotations) {
		Map<String,String> result = null;
		if (annotations != null && !annotations.isEmpty()) {
			for (OWLAnnotation annotation : annotations) {
				OWLAnnotationProperty prop = annotation.getProperty();
				String literal = getLiteralValue(annotation.getValue());
				if (literal != null) {
					if (result == null) {
						result = new HashMap<String, String>();
					}
					result.put(prop.toStringID(), literal);
				}
			}
		}
		return result;
	}

	private Map<String, Object> renderAnnotationAxioms(Set<OWLAnnotationAssertionAxiom> annotations) {
		Map<String, Object> result = null;
		if (annotations != null && !annotations.isEmpty()) {
			for (OWLAnnotationAssertionAxiom ax : annotations) {
				OWLAnnotationProperty prop = ax.getProperty();
				String literal = getLiteralValue(ax.getValue());
				if (literal != null) {
					if (result == null) {
						result = new HashMap<String, Object>();
					}
					result.put(prop.toStringID(), literal);
				}
			}
		}
		return result;
	}

	private static String getLiteralValue(OWLAnnotationValue v) {
		String literal = v.accept(new OWLAnnotationValueVisitorEx<String>() {
			@Override
			public String visit(IRI iri) {
				return null;
			}

			@Override
			public String visit(OWLAnonymousIndividual individual) {
				return null;
			}

			@Override
			public String visit(OWLLiteral literal) {
				return literal.getLiteral();
			}
		});
		return literal;
	}

	private OWLShuntGraph createShuntGraph(OWLGraphWrapper graph) {
		// Assemble the group shunt graph from available information.
		// Most of the interesting stuff is happening with the meta-information.
		OWLShuntGraph shuntGraph = new OWLShuntGraph();
		
		OWLOntology source = graph.getSourceOntology();
		OWLPrettyPrinter pp = new OWLPrettyPrinter(graph);
		
		Set<OWLNamedIndividual> relevant = new HashSet<OWLNamedIndividual>();
		
		// links
		Set<OWLObjectPropertyAssertionAxiom> objectPropertyAxioms = source.getAxioms(AxiomType.OBJECT_PROPERTY_ASSERTION);
		for(OWLObjectPropertyAssertionAxiom ax : objectPropertyAxioms) {
			OWLIndividual subject = ax.getSubject();
			OWLIndividual object = ax.getObject();
			OWLObjectPropertyExpression property = ax.getProperty();
			if (subject.isNamed() && object.isNamed() && property.isAnonymous() == false) {
				OWLNamedIndividual subjectNamed = subject.asOWLNamedIndividual();
				String subjectId = subjectNamed.getIRI().toString();
				relevant.add(subjectNamed);
				OWLNamedIndividual objectNamed = object.asOWLNamedIndividual();
				String objectId = objectNamed.getIRI().toString();
				relevant.add(objectNamed);
				String propId = graph.getIdentifier(property.asOWLObjectProperty());
				
				OWLShuntEdge shuntEdge = new OWLShuntEdge(subjectId, objectId, propId);
				shuntEdge.setMetadata(renderAnnotations(ax.getAnnotations()));
				shuntGraph.addEdge(shuntEdge);
			}
		}

		// nodes
		for(OWLNamedIndividual individual : relevant) {
			String nodeId = individual.getIRI().toString();
			
			final StringBuilder sb = new StringBuilder();
			Set<OWLClassAssertionAxiom> declaredTypes = source.getClassAssertionAxioms(individual);
			for (OWLClassAssertionAxiom ax : declaredTypes) {
				OWLClassExpression classExpression = ax.getClassExpression();
				if (sb.length() > 0) {
					sb.append(" | ");
				}
				sb.append(pp.render(classExpression));
			}
			OWLShuntNode shuntNode = new OWLShuntNode(nodeId, sb.toString());
			
			Set<OWLAnnotationAssertionAxiom> annotationAxioms = source.getAnnotationAssertionAxioms(individual.getIRI());
			shuntNode.setMetadata(renderAnnotationAxioms(annotationAxioms));
			shuntGraph.addNode(shuntNode);
		}
		return shuntGraph;
	}

	private String getLabel(OWLObject oc, OWLGraphWrapper graph){
		String label = "???";
		if( oc != null ){
			label = graph.getAnnotationValue(oc, displayLabelProp);
			if( label == null ){
				label = getId(oc, graph);
			}
		}
		
		return label;
	}

	private String getId(OWLObject cls, OWLGraphWrapper graph) {
		String shortId = "???";
		if (cls != null) {
			shortId = graph.getAnnotationValue(cls, shortIdProp);
			if (shortId == null) {
				shortId = graph.getIdentifier(cls);
			}
		}
		return shortId;
	}
	
	static void addField(SolrInputDocument doc, String field, Object value) {
		if (value != null && field != null) {
			doc.addField(field, value);
		}
	}

	/**
	 * Take args and add it index (no commits)
	 * Main wrapping for adding complex annotation documents to GOlr.
	 * 
	 * @param mf
	 * @param mfType
	 * @param mfAnnotations
	 * @param gp
	 * @param gpType
	 * @param gpAnnotations
	 * @param bp
	 * @param bpType
	 * @param bpAnnotations
	 * @param locations
	 * @param modelId
	 * @param title
	 * @param shuntGraph
	 * @param modelAnnotations
	 * @param state
	 * @param modelDate
	 * @param modelUrl
	 * @param modelComment
	 * @param jsonModel
	 *
	 * @return an input doc for add()
	 */
	SolrInputDocument collectUnitInfo(
			OWLNamedIndividual mf, OWLClass mfType, Set<OWLAnnotation> mfAnnotations,
			OWLNamedIndividual gp, OWLClass gpType, Set<OWLAnnotation> gpAnnotations,
			OWLNamedIndividual bp, OWLClass bpType, Set<OWLAnnotation> bpAnnotations,
			Map<OWLClass, Pair<OWLNamedIndividual, Set<OWLAnnotation>>> locations,
			String modelId, String title, String state, String modelDate, String modelUrl, 
			Set<String> modelAnnotations, String modelComment, OWLShuntGraph shuntGraph,
			String jsonModel) {

		final Set<String> allComments = new HashSet<String>(); // unused until schema can be fixed
		if (modelComment != null) {
			allComments.add(modelComment);
		}
		final Set<String> allOtherAnnotationValues = new HashSet<String>(modelAnnotations);
		final Set<String> allContributors = new HashSet<String>();
		OWLClass ecoClass = null; // Why do we only have one eco class, we have multiple annotations?
		final Set<String> allReferences = new HashSet<String>();
		final Set<String> allWiths = new HashSet<String>();
		
		ecoClass = processAnnotations(gpAnnotations, ecoClass, allReferences, allWiths, allComments, allContributors, allOtherAnnotationValues);
		ecoClass = processAnnotations(mfAnnotations, ecoClass, allReferences, allWiths, allComments, allContributors, allOtherAnnotationValues);
		ecoClass = processAnnotations(bpAnnotations, ecoClass, allReferences, allWiths, allComments, allContributors, allOtherAnnotationValues);
		
		final SolrInputDocument doc = new SolrInputDocument();
		
		final String gpId = getId(gpType, graph);
		final String gpLabel = getLabel(gpType, graph);
		final String mfId = getId(mfType, graph);
		final String mfLabel = getLabel(mfType, graph);
		final Map<String, String> mfClosureMap = graph.getRelationClosureMap(mfType, defaultClosureRelations);
		final Set<String> mfClosure = mfClosureMap.keySet();
		final Set<String> mfClosureLabel = new HashSet<String>(mfClosureMap.values());


		doc.addField("document_category", "model_annotation");
		
		StringBuilder unitIdBuilder = new StringBuilder(modelId);
		unitIdBuilder.append('|').append(gp.getIRI()).append('|').append(gpId).append('|').append(mfId).append('|');
		if (bpType != null) {
			unitIdBuilder.append(getId(bpType, graph));
		}
		StringBuilder unitLabelBuilder = new StringBuilder(modelId);
		unitLabelBuilder.append(' ').append(gpLabel).append(' ').append(mfId);
		if (bpType != null) {
			unitLabelBuilder.append(' ').append(getId(bpType, graph));
		}
		final String unitId = unitIdBuilder.toString();
		final String unitLabel = unitLabelBuilder.toString();
		//  - id: id
		//    description: A unique (and internal) thing.
		doc.addField("id", unitId);
		
		//  - id: annotation_unit
		//    type: string
		doc.addField("annotation_unit", unitId);

		//  - id: annotation_unit_label
		//    display_name: Annotation unit
		//    type: string
		//    searchable: true
		doc.addField("annotation_unit_label", unitLabel);

		//  - id: model
		//    display_name: model
		//    type: string
		doc.addField("model", modelId);

		//  - id: model_label
		//    display_name: model
		//    type: string
		//    searchable: true
		addField(doc, "model_label", title);
		addField(doc, "model_label_searchable", title);

		//  - id: model_url
		//    display_name: model URL
		//    type: string
		addField(doc, "model_url", modelUrl);

		//  - id: model_state
		//    display_name: state
		//    type: string
		addField(doc, "model_state", state);

		//  - id: model_date
		//    description: Last modified
		//    display_name: modified
		//    type: string
		//    searchable: true
		addField(doc, "model_date", modelDate);
		addField(doc, "model_date_searchable", modelDate);

		//## Gene Product
		//  - id: enabled_by
		//    type: string
		//    searchable: true
		doc.addField("enabled_by", gpId);

		//  - id: enabled_by_label
		//    type: string
		//    searchable: true
		doc.addField("enabled_by_label",gpLabel);
		doc.addField("enabled_by_label_searchable",gpLabel);

		// Not used at the moment
		//## PANTHER
		//  - id: panther_family
		//    description: PANTHER family IDs that are associated with this entity.
		//    display_name: PANTHER family
		//    type: string
		//    searchable: true
		//  - id: panther_family_label
		//    description: PANTHER families that are associated with this entity.
		//    display_name: PANTHER family
		//    type: string
		//    searchable: true

		// Not used at the moment
		//## Taxon
		//  - id: taxon
		//    description: "GAF column 13 (taxon)."
		//    type: string
		//  - id: taxon_label
		//    description: "Taxon derived from GAF column 13 and ncbi_taxonomy.obo."
		//    type: string
		//    searchable: true
		//  - id: taxon_closure
		//    description: "Taxon IDs derived from GAF column 13 and ncbi_taxonomy.obo."
		//    type: string
		//    cardinality: multi
		//  - id: taxon_closure_label
		//    description: "Taxon label closure derived from GAF column 13 and ncbi_taxonomy.obo."
		//    type: string
		//    cardinality: multi
		//    searchable: true

		//## Function
		//  - id: function_class
		//    description: Function acc/ID.
		//    display_name: Function
		//    type: string
		addField(doc, "function_class", mfId);

		//  - id: function_class_label
		//    description: Common function name.
		//    display_name: Function
		//    type: string
		//    searchable: true
		addField(doc, "function_class_label", mfLabel);
		addField(doc, "function_class_label_searchable", mfLabel);

		//  - id: function_class_closure
		//    display_name: Function
		//    type: string
		//    cardinality: multi
		addField(doc, "function_class_closure", mfClosure);

		//  - id: function_class_closure_label
		//    display_name: Function
		//    type: string
		//    cardinality: multi
		//    searchable: true
		addField(doc, "function_class_closure_label", mfClosureLabel);
		addField(doc, "function_class_closure_label_searchable", mfClosureLabel);
		
		addField(doc, "function_class_closure_map", mfClosureMap);

		//## Process
		if (bpType != null) {
			final String bpId = getId(bpType, graph);
			final String bpLabel = getLabel(bpType, graph);
			final Map<String, String> bpClosureMap = graph.getRelationClosureMap(bpType, defaultClosureRelations);
			final Set<String> bpClosure = bpClosureMap.keySet();
			final Set<String> bpClosureLabels = new HashSet<String>(bpClosureMap.values());
			//  - id: process_class
			//    description: Process acc/ID.
			//    display_name: Process
			//    type: string
			addField(doc, "process_class", bpId);
	
			//  - id: process_class_label
			//    description: Common process name.
			//    display_name: Process
			//    type: string
			//    searchable: true
			addField(doc, "process_class_label", bpLabel);
			addField(doc, "process_class_label_searchable", bpLabel);
	
			//  - id: process_class_closure
			//    display_name: Process
			//    type: string
			//    cardinality: multi
			addField(doc, "process_class_closure", bpClosure);
	
			//  - id: process_class_closure_label
			//    display_name: Process
			//    type: string
			//    cardinality: multi
			//    searchable: true
			addField(doc, "process_class_closure_label", bpClosureLabels);
			addField(doc, "process_class_closure_label_searchable", bpClosureLabels);
			
			addField(doc, "process_class_closure_map", bpClosureMap);
		}

		//## Location
		if (locations != null && !locations.isEmpty()) {
			List<String> locationList = new ArrayList<String>();
			List<String> locationLabelList = new ArrayList<String>();
			Map<String, String> locationClosureMap = new HashMap<String, String>();
			for(Entry<OWLClass, Pair<OWLNamedIndividual, Set<OWLAnnotation>>> entry : locations.entrySet()) {
				OWLClass location = entry.getKey();
				String locationId = getId(location, graph);
				String locationLabel = getLabel(location, graph);
				locationList.add(locationId);
				locationLabelList.add(locationLabel);
				locationClosureMap.putAll(graph.getRelationClosureMap(location, defaultClosureRelations));
				ecoClass = processAnnotations(entry.getValue().getRight(), ecoClass, allReferences, allWiths, allComments, allContributors, allOtherAnnotationValues);
			}
			Set<String> locationClosure = locationClosureMap.keySet();
			Set<String> locationClosureLabels = new HashSet<String>(locationClosureMap.values());
			//  - id: location_list
			//    display_name: Location
			//    type: string
			//    cardinality: multi
			addField(doc, "location_list", locationList);

			//  - id: location_list_label
			//    display_name: Location
			//    type: string
			//    cardinality: multi
			addField(doc, "location_list_label", locationLabelList);

			//  - id: location_list_closure
			//    display_name: Location
			//    type: string
			//    cardinality: multi
			addField(doc, "location_list_closure", locationClosure);

			//  - id: location_list_closure_label
			//    display_name: Location
			//    type: string
			//    cardinality: multi
			addField(doc, "location_list_closure_label", locationClosureLabels);
			
			addField(doc, "location_list_closure_map", locationClosureMap);
		}
		
		//  - id: owl_blob_json
		//    type: string
		//    indexed: false
		if (jsonModel != null) {
			addField(doc, "owl_blob_json", jsonModel);
		}
		
		//## Topology
		//  - id: topology_graph_json
		//    description: JSON blob form of the local stepwise topology graph.
		//    display_name: Topology graph (JSON)
		//    type: string
		//    indexed: false
		addField(doc, "topology_graph_json", shuntGraph.toJSON());

		//## Evidence and related
		if (ecoClass != null) {
			String evidenceId = getId(ecoClass, graph);
			String evidenceLabel = getLabel(ecoClass, graph);
			Map<String, String> evidenceClosureMap = graph.getRelationClosureMap(ecoClass, defaultClosureRelations);
			Set<String> evidenceClosure = evidenceClosureMap.keySet();
			Set<String> evidenceClosureLabels = new HashSet<String>(evidenceClosureMap.values());
			
			//  - id: evidence_type
			//    description: "Evidence type."
			//    display_name: Evidence
			//    type: string
			addField(doc, "evidence_type", evidenceId);
			
			//label
			addField(doc, "evidence_type_label", evidenceLabel);
			addField(doc, "evidence_type_label_searchable", evidenceLabel);
			
			//  - id: evidence_type_closure
			//    description: "All evidence (evidence closure) for this annotation"
			//    display_name: Evidence type
			//    type: string
			//    cardinality: multi
			addField(doc, "evidence_type_closure", evidenceClosure);
			
			addField(doc, "evidence_type_closure_label", evidenceClosureLabels);
			addField(doc, "evidence_type_closure_label_searchable", evidenceClosureLabels);
			
			addField(doc, "evidence_type_closure_map", evidenceClosureMap);
			
			//  - id: evidence_with
			//    description: "Evidence with/from."
			//    display_name: Evidence with
			//    type: string
			//    cardinality: multi
			addField(doc, "evidence_with", allWiths);
			addField(doc, "evidence_with_searchable", allWiths);
			
			//  - id: reference
			//    description: "Database reference."
			//    display_name: Reference
			//    type: string
			//    cardinality: multi
			addField(doc, "reference", allReferences);
			addField(doc, "reference_searchable", allReferences);
		}

		//  - id: comment
		//    display_name: comment
		//    type: string
		//    cardinality: multi
		//    searchable: true
		addField(doc, "comment", modelComment);
		addField(doc, "comment_searchable", modelComment);
		
		//  - id: contributor
		//    display_name: contributor
		//    type: string
		//    cardinality: multi
		//    searchable: true
		addField(doc, "contributor", allContributors);
		addField(doc, "contributor_searchable", allContributors);

		//  - id: annotation_value
		//    description: set of all literal values of all annotation assertions in model
		//    cardinality: multi
		//    display_name: texts
		//    type: string
		addField(doc, "annotation_value", allOtherAnnotationValues);
		return doc;
	}
	
	OWLClass processAnnotations(final Set<OWLAnnotation> annotations, OWLClass eco, 
			final Set<String> allReferences, final Set<String> allWiths, final Set<String> allComments,
			final Set<String> allContributors, final Set<String> allOtherAnnotationValues) {
		if (annotations != null) {
			for (OWLAnnotation annotation : annotations) {
				OWLAnnotationProperty p = annotation.getProperty();
				OWLAnnotationValue value = annotation.getValue();
				if (evidence.equals(p)) {
					OWLNamedIndividual relevant = findEvidenceIndividual(value);
					if (eco == null) {
						eco = findFirstType(relevant);
					}
					if (relevant != null) {
						Set<OWLAnnotationAssertionAxiom> axioms = model.getAnnotationAssertionAxioms(relevant.getIRI());
						for (OWLAnnotationAssertionAxiom axiom : axioms) {
							if (source.equals(axiom.getProperty())) {
								String sourceValue = getLiteralValue(axiom.getValue());
								if (sourceValue != null) {
									allReferences.add(sourceValue);
								}
							}
							else if (with.equals(axiom.getProperty())) {
								String withValue = getLiteralValue(axiom.getValue());
								if (withValue != null) {
									allWiths.add(withValue);
								}
							}
							else if (contributor.equals(axiom.getProperty())) {
								String contrib = getLiteralValue(axiom.getValue());
								if (contrib != null) {
									allContributors.add(contrib);
								}
							}
							else if (comment.equals(axiom.getProperty())) {
								String literal = getLiteralValue(axiom.getValue());
								if (literal != null) {
									allComments.add(literal);
								}
							}
						}
					}
				}
				else if (comment.equals(p)) {
					String literal = getLiteralValue(value);
					if (literal != null) {
						allComments.add(literal);
					}
				}
				else if (contributor.equals(p)) {
					String literal = getLiteralValue(value);
					if (literal != null) {
						allContributors.add(literal);
					}
				}
				else if (layoutHintX.equals(p)||
						layoutHintY.equals(p) || 
						date.equals(p)) {
					// ignore
					continue;
				}
				else {
					String literal = getLiteralValue(value);
					if (literal != null) {
						allOtherAnnotationValues.add(literal);
					}
				}
			}
		}
		return eco;
	}

	private OWLClass findFirstType(OWLNamedIndividual relevant) {
		Set<OWLClassAssertionAxiom> axioms = model.getClassAssertionAxioms(relevant);
		for (OWLClassAssertionAxiom axiom : axioms) {
			OWLClassExpression ce = axiom.getClassExpression();
			if (ce.isAnonymous() == false) {
				return ce.asOWLClass();
			}
		}
		return null;
	}

	private OWLNamedIndividual findEvidenceIndividual(OWLAnnotationValue value) {
		return value.accept(new OWLAnnotationValueVisitorEx<OWLNamedIndividual>() {

			@Override
			public OWLNamedIndividual visit(final IRI iri) {
				OWLNamedIndividual i = null;
				for(OWLNamedIndividual current : model.getIndividualsInSignature()) {
					if (current.getIRI().equals(iri)) {
						i = current;
						break;
					}
				}
				return i;
			}

			@Override
			public OWLNamedIndividual visit(OWLAnonymousIndividual individual) {
				return null;
			}

			@Override
			public OWLNamedIndividual visit(OWLLiteral literal) {
				return null;
			}
		});
	}

}
