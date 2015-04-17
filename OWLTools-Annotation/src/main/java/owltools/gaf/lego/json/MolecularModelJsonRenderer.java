package owltools.gaf.lego.json;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAnnotationValueVisitorEx;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLIndividualAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyDocumentAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.gaf.eco.EcoMapper;
import owltools.gaf.eco.EcoMapperFactory;
import owltools.gaf.eco.EcoMapperFactory.OntologyMapperPair;
import owltools.gaf.lego.IdStringManager;
import owltools.gaf.lego.IdStringManager.AnnotationShorthand;
import owltools.gaf.lego.MolecularModelManager;
import owltools.graph.OWLGraphWrapper;
import owltools.util.ModelContainer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * A Renderer that takes a MolecularModel (an OWL ABox) and generates Map objects
 * that can be translated to JSON using Gson.
 * 
 * TODO - make this inherit from a generic renderer - use OWLAPI visitor?
 * TODO - abstract some of this into a generic OWL to JSON-LD converter
 * 
 * @author cjm
 *
 */
public class MolecularModelJsonRenderer {
	
	private static Logger LOG = Logger.getLogger(MolecularModelJsonRenderer.class);

	private final OWLOntology ont;
	private final OWLGraphWrapper graph;
	
//	private boolean includeObjectPropertyValues = true;

	public static final ThreadLocal<DateFormat> AnnotationTypeDateFormat = new ThreadLocal<DateFormat>(){

		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd");
		}
		
	};
	
	/**
	 * @param model
	 */
	public MolecularModelJsonRenderer(ModelContainer model) {
		this(model.getAboxOntology(), new OWLGraphWrapper(model.getAboxOntology()));
	}
	
	/**
	 * @param ontology
	 */
	public MolecularModelJsonRenderer(OWLOntology ontology) {
		this(ontology, new OWLGraphWrapper(ontology));
	}
	
	/**
	 * @param graph
	 */
	public MolecularModelJsonRenderer(OWLGraphWrapper graph) {
		this(graph.getSourceOntology(), graph);
	}

	private MolecularModelJsonRenderer(OWLOntology ont, OWLGraphWrapper graph) {
		super();
		this.ont = ont;
		this.graph = graph;
	}
	
	/**
	 * @return Map to be passed to Gson
	 */
	public JsonModel renderModel() {
		JsonModel json = new JsonModel();
		
		// per-Individual
		List<JsonOwlIndividual> iObjs = new ArrayList<JsonOwlIndividual>();
		for (OWLNamedIndividual i : ont.getIndividualsInSignature()) {
			iObjs.add(renderObject(i));
		}
		json.individuals = iObjs.toArray(new JsonOwlIndividual[iObjs.size()]);
		
		// per-Assertion
		Set<OWLObjectProperty> usedProps = new HashSet<OWLObjectProperty>();
		
		List<JsonOwlFact> aObjs = new ArrayList<JsonOwlFact>();
		for (OWLObjectPropertyAssertionAxiom opa : 
			ont.getAxioms(AxiomType.OBJECT_PROPERTY_ASSERTION)) {
			aObjs.add(renderObject(opa));
			usedProps.addAll(opa.getObjectPropertiesInSignature());
		}
		json.facts = aObjs.toArray(new JsonOwlFact[aObjs.size()]);

		// per-Property
		List<JsonOwlObject> pObjs = new ArrayList<JsonOwlObject>();
		for (OWLObjectProperty p : usedProps) {
			pObjs.add(renderObject(p));
		}
		json.properties  = pObjs.toArray(new JsonOwlObject[pObjs.size()]);

		JsonAnnotation[] anObjs = renderAnnotations(ont.getAnnotations());
		if (anObjs != null && anObjs.length > 0) {
			json.annotations = anObjs;
		}
		
		return json;
		
	}
	
	/**
	 * Add the available inferences to the given JSON map.
	 * 
	 * @param reasoner
	 * @return inferences or null
	 */
	public JsonOwlIndividual[] renderModelInferences(OWLReasoner reasoner) {
		JsonOwlIndividual[] inferences = null;
		if (reasoner.isConsistent()) {
			Set<OWLNamedIndividual> individuals = ont.getIndividualsInSignature();
			inferences = renderInferences(individuals, reasoner);
		}
		return inferences;
	}
	
	public static JsonAnnotation[] renderModelAnnotations(OWLOntology ont) {
		JsonAnnotation[] anObjs = renderAnnotations(ont.getAnnotations());
		return anObjs;
	}
	
	private static JsonAnnotation[] renderAnnotations(Set<OWLAnnotation> annotations) {
		List<JsonAnnotation> anObjs = new ArrayList<JsonAnnotation>();
		for (OWLAnnotation annotation : annotations) {
			OWLAnnotationProperty p = annotation.getProperty();
			AnnotationShorthand annotationShorthand = AnnotationShorthand.getShorthand(p.getIRI());
			if (annotationShorthand != null) {
				final String stringValue = getAnnotationStringValue(annotation.getValue());
				if (stringValue != null) {
					anObjs.add(JsonAnnotation.create(annotationShorthand.name(), stringValue));
				}
			}
			else {
				// TODO render without the use of the shorthand
			}
		}
		return anObjs.toArray(new JsonAnnotation[anObjs.size()]);
	}
	
	public Pair<JsonOwlIndividual[], JsonOwlFact[]> renderIndividuals(Collection<OWLNamedIndividual> individuals) {
		OWLOntology ont = graph.getSourceOntology();
		List<JsonOwlIndividual> iObjs = new ArrayList<JsonOwlIndividual>();
		List<OWLNamedIndividual> individualIds = new ArrayList<OWLNamedIndividual>();
		Set<OWLObjectPropertyAssertionAxiom> opAxioms = new HashSet<OWLObjectPropertyAssertionAxiom>();
		for (OWLIndividual i : individuals) {
			if (i instanceof OWLNamedIndividual) {
				OWLNamedIndividual named = (OWLNamedIndividual)i;
				iObjs.add(renderObject(named));
				individualIds.add(named);
				
				Set<OWLIndividualAxiom> iAxioms = ont.getAxioms(i);
				for (OWLIndividualAxiom owlIndividualAxiom : iAxioms) {
					if (owlIndividualAxiom instanceof OWLObjectPropertyAssertionAxiom) {
						opAxioms.add((OWLObjectPropertyAssertionAxiom) owlIndividualAxiom);
					}
				}
			}
		}
		List<JsonOwlFact> aObjs = new ArrayList<JsonOwlFact>();
		for (OWLObjectPropertyAssertionAxiom opa : opAxioms) {
			aObjs.add(renderObject(opa));
		}
		
		return Pair.of(iObjs.toArray(new JsonOwlIndividual[iObjs.size()]), 
				aObjs.toArray(new JsonOwlFact[aObjs.size()]));
	}
	
	/**
	 * Retrieve the inferences for the given individuals.
	 * 
	 * @param individuals
	 * @param reasoner
	 * @return individual inferences or null
	 */
	public JsonOwlIndividual[] renderInferences(Collection<OWLNamedIndividual> individuals, OWLReasoner reasoner) {
		if (individuals != null && reasoner.isConsistent()) {
			List<JsonOwlIndividual> iObjs = new ArrayList<JsonOwlIndividual>(individuals.size());
			for (OWLNamedIndividual i : individuals) {
				JsonOwlIndividual json = new JsonOwlIndividual();
				json.id = IdStringManager.getId(i, graph);
				Set<OWLClass> types = reasoner.getTypes(i, true).getFlattened();
				List<JsonOwlObject> typeObjs = new ArrayList<JsonOwlObject>(types.size());
				for (OWLClass x : types) {
					if (x.isBuiltIn()) {
						continue;
					}
					typeObjs.add(renderObject(x));
				}
				json.type = typeObjs.toArray(new JsonOwlObject[typeObjs.size()]);
				iObjs.add(json);
			}
			return iObjs.toArray(new JsonOwlIndividual[iObjs.size()]);
		}
		return null;
	}
	
	/**
	 * @param i
	 * @return Map to be passed to Gson
	 */
	public JsonOwlIndividual renderObject(OWLNamedIndividual i) {
		JsonOwlIndividual json = new JsonOwlIndividual();
		json.id = IdStringManager.getId(i, graph);
		json.label = getLabel(i, json.id);
		
		List<JsonOwlObject> typeObjs = new ArrayList<JsonOwlObject>();
		for (OWLClassExpression x : i.getTypes(ont)) {
			typeObjs.add(renderObject(x));
		}
//		if (includeObjectPropertyValues) {
//			Map<OWLObjectPropertyExpression, Set<OWLIndividual>> pvs = i.getObjectPropertyValues(ont);
//			for (OWLObjectPropertyExpression p : pvs.keySet()) {
//				List<Object> valObjs = new ArrayList<Object>();
//				for (OWLIndividual v : pvs.get(p)) {
//					if (v.isNamed()) {
//						valObjs.add(renderObject(v.asOWLNamedIndividual()));
//					}
//				}
//				iObj.put(getId((OWLNamedObject) p, graph), valObjs);
//			}
//		}
		json.type = typeObjs.toArray(new JsonOwlObject[typeObjs.size()]);
		List<JsonAnnotation> anObjs = new ArrayList<JsonAnnotation>();
		Set<OWLAnnotationAssertionAxiom> annotationAxioms = ont.getAnnotationAssertionAxioms(i.getIRI());
		for (OWLAnnotationAssertionAxiom ax : annotationAxioms) {
			OWLAnnotationProperty p = ax.getProperty();
			AnnotationShorthand annotationShorthand = AnnotationShorthand.getShorthand(p.getIRI());
			if (annotationShorthand != null) {
				final String stringValue = getAnnotationStringValue(ax.getValue());
				if (stringValue != null) {
					anObjs.add(JsonAnnotation.create(annotationShorthand.name(), stringValue));
				}
			}
			else {
				// TODO render non-shorthand annotations
			}
		}
		if (anObjs.isEmpty() == false) {
			json.annotations = anObjs.toArray(new JsonAnnotation[anObjs.size()]);
		}
		return json;
	}
	
	public static String getAnnotationStringValue(OWLAnnotationValue v) {
		final String stringValue = v.accept(new OWLAnnotationValueVisitorEx<String>() {

			@Override
			public String visit(IRI iri) {
				return IdStringManager.getId(iri);
			}

			@Override
			public String visit(OWLAnonymousIndividual individual) {
				return null; // Do nothing
			}

			@Override
			public String visit(OWLLiteral literal) {
				return literal.getLiteral();
			}
		});
		return stringValue;
	}
	
	/**
	 * @param opa
	 * @return Map to be passed to Gson
	 */
	public JsonOwlFact renderObject(OWLObjectPropertyAssertionAxiom opa) {
		OWLNamedIndividual subject;
		OWLObjectProperty property;
		OWLNamedIndividual object;

		subject = (OWLNamedIndividual) opa.getSubject();
		property = (OWLObjectProperty) opa.getProperty();
		object = (OWLNamedIndividual) opa.getObject();

		JsonOwlFact fact = new JsonOwlFact();
		fact.subject = IdStringManager.getId(subject, graph);
		fact.property = IdStringManager.getId(property, graph);
		fact.object = IdStringManager.getId(object, graph);
		
		JsonAnnotation[] anObjs = renderAnnotations(opa.getAnnotations());
		if (anObjs != null && anObjs.length > 0) {
			fact.annotations = anObjs;
		}
		return fact;
	}

	/**
	 * @param p
	 * @return Map to be passed to Gson
	 */
	public JsonOwlObject renderObject(OWLObjectProperty p) {
		String id = IdStringManager.getId(p, graph);
		String label = getLabel(p, id);
		JsonOwlObject json = JsonOwlObject.createProperty(id, label);
		return json;
	}
	/**
	 * @param x
	 * @return  Object to be passed to Gson
	 */
	private JsonOwlObject renderObject(OWLClassExpression x) {
		if (x.isAnonymous()) {
			JsonOwlObject json = null;
			if (x instanceof OWLObjectIntersectionOf) {
				List<JsonOwlObject> expressions = new ArrayList<JsonOwlObject>();
				for (OWLClassExpression y : ((OWLObjectIntersectionOf)x).getOperands()) {
					expressions.add(renderObject(y));
				}
				json = JsonOwlObject.createIntersection(expressions);
			}
			else if (x instanceof OWLObjectUnionOf) {
				List<JsonOwlObject> expressions = new ArrayList<JsonOwlObject>();
				for (OWLClassExpression y : ((OWLObjectUnionOf)x).getOperands()) {
					expressions.add(renderObject(y));
				}
				json = JsonOwlObject.createUnion(expressions);
			}
			else if (x instanceof OWLObjectSomeValuesFrom) {
				OWLObjectSomeValuesFrom svf = (OWLObjectSomeValuesFrom)x;
				String prop = renderObjectId(svf.getProperty());
				JsonOwlObject filler = renderObject(svf.getFiller());
				if (prop != null && filler != null) {
					json = JsonOwlObject.createSvf(prop, filler);
				}
			}
			else {
				// TODO
			}
			return json;
		}
		else {
			return renderObject(x.asOWLClass());
		}
	}

	private JsonOwlObject renderObject(OWLClass cls) {
		String id = IdStringManager.getId(cls, graph);
		JsonOwlObject json = JsonOwlObject.createCls(id, getLabel(cls, id));
		return json;
	}

	private String renderObjectId(OWLObjectPropertyExpression px) {
		if (px.isAnonymous()) {
			return null; // TODO
		}
		else {
			return IdStringManager.getId(px.asOWLObjectProperty(), graph);
		}
	}

	protected String getLabel(OWLNamedObject i, String id) {
		return graph.getLabel(i);
	}
	
//	/**
//	 * @param includeObjectPropertyValues the includeObjectPropertyValues to set
//	 */
//	public void setIncludeObjectPropertyValues(boolean includeObjectPropertyValues) {
//		this.includeObjectPropertyValues = includeObjectPropertyValues;
//	}

	public static List<JsonRelationInfo> renderRelations(MolecularModelManager<?> mmm, Set<OWLObjectProperty> importantRelations) throws OWLOntologyCreationException {
		/* [{
		 *   id: {String}
		 *   label: {String}
		 *   relevant: {boolean} // flag to indicate if this is a relation to be used in the model
		 *   ?color: {String} // TODO in the future
		 *   ?glyph: {String} // TODO in the future
		 * }]
		 */
		// retrieve (or load) all ontologies
		// put in a new wrapper
		OWLGraphWrapper wrapper = new OWLGraphWrapper(mmm.getOntology());
		Collection<IRI> imports = mmm.getImports();
		OWLOntologyManager manager = wrapper.getManager();
		for (IRI iri : imports) {
			OWLOntology ontology = manager.getOntology(iri);
			if (ontology == null) {
				// only try to load it, if it isn't already loaded
				try {
					ontology = manager.loadOntology(iri);
				} catch (OWLOntologyDocumentAlreadyExistsException e) {
					IRI existing = e.getOntologyDocumentIRI();
					ontology = manager.getOntology(existing);
				} catch (OWLOntologyAlreadyExistsException e) {
					OWLOntologyID id = e.getOntologyID();
					ontology = manager.getOntology(id);
				}
			}
			if (ontology == null) {
				LOG.warn("Could not find an ontology for IRI: "+iri);
			}
			else {
				wrapper.addSupportOntology(ontology);
			}
		}
	
		// get all properties from all loaded ontologies
		Set<OWLObjectProperty> properties = new HashSet<OWLObjectProperty>();
		Set<OWLOntology> allOntologies = wrapper.getAllOntologies();
		for(OWLOntology o : allOntologies) {
			properties.addAll(o.getObjectPropertiesInSignature());
		}
		
		// sort properties
		List<OWLObjectProperty> propertyList = new ArrayList<OWLObjectProperty>(properties);
		Collections.sort(propertyList);


		// retrieve id and label for all properties
		List<JsonRelationInfo> relList = new ArrayList<JsonRelationInfo>();
		for (OWLObjectProperty p : propertyList) {
			if (p.isBuiltIn()) {
				// skip owl:topObjectProperty
				continue;
			}
			JsonRelationInfo json = new JsonRelationInfo();
			json.id = IdStringManager.getId(p, wrapper);
			json.label = wrapper.getLabel(p);
			if (importantRelations != null && (importantRelations.contains(p))) {
				json.relevant = true;
			}
			else {
				json.relevant = false;
			}
			relList.add(json);
		}
		return relList;
	}
	
	public static List<JsonEvidenceInfo> renderEvidences(MolecularModelManager<?> mmm) throws OWLException, IOException {
		return renderEvidences(mmm.getGraph().getManager());
	}
	
	public static List<JsonEvidenceInfo> renderEvidences(OWLOntologyManager manager) throws OWLException, IOException {
		// TODO remove the hard coded ECO dependencies
		OntologyMapperPair<EcoMapper> pair = EcoMapperFactory.createEcoMapper(manager);
		final OWLGraphWrapper graph = pair.getGraph();
		final EcoMapper mapper = pair.getMapper();
		Set<OWLClass> ecoClasses = graph.getAllOWLClasses();
		Map<OWLClass, String> codesForEcoClasses = mapper.getCodesForEcoClasses();
		List<JsonEvidenceInfo> relList = new ArrayList<JsonEvidenceInfo>();
		for (OWLClass ecoClass : ecoClasses) {
			if (ecoClass.isBuiltIn()) {
				continue;
			}
			JsonEvidenceInfo json = new JsonEvidenceInfo();
			json.id = IdStringManager.getId(ecoClass, graph);
			json.label = graph.getLabel(ecoClass);
			String code = codesForEcoClasses.get(ecoClass);
			if (code != null) {
				json.code = code;
			}
			relList.add(json);
		}
		return relList;
	}

	public static String renderToJson(OWLOntology ont) {
		return renderToJson(ont, false);
	}
	
	public static String renderToJson(OWLOntology ont, boolean allIndividuals) {
		return renderToJson(ont, allIndividuals, false);
	}
	
	public static String renderToJson(OWLOntology ont, boolean allIndividuals, boolean prettyPrint) {
		MolecularModelJsonRenderer r = new MolecularModelJsonRenderer(ont);
//		if (allIndividuals) {
//			r.includeObjectPropertyValues = false;
//		}
		JsonModel model = r.renderModel();
		return renderToJson(model, prettyPrint);
	}
	
	public static String renderToJson(Object model, boolean prettyPrint) {
		GsonBuilder builder = new GsonBuilder();
		if (prettyPrint) {
			builder = builder.setPrettyPrinting();
		}
		Gson gson = builder.create();
		String json = gson.toJson(model);
		return json;
	}
	
	public static <T> T parseFromJson(String json, Class<T> type) {
		Gson gson = new GsonBuilder().create();
		T result = gson.fromJson(json, type);
		return result;
	}

	public static <T> T[] parseFromJson(String requestString, Type requestType) {
		Gson gson = new GsonBuilder().create();
		return gson.fromJson(requestString, requestType);
	}

}
