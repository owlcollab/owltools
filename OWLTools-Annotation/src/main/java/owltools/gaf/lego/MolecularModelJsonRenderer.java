package owltools.gaf.lego;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
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
import owltools.gaf.lego.MolecularModelManager.LegoAnnotationType;
import owltools.graph.OWLGraphWrapper;
import owltools.util.ModelContainer;
import owltools.vocab.OBOUpperVocabulary;

import com.google.gson.Gson;

/**
 * A Renderer that takes a MolecularModel (an OWL ABox) and generates Map objects
 * that can be translated to JSON using Gson.
 * 
 * TODO - make this inherit from a generic renderer - use OWLAPI visitor?
 * TODO - abstract some of this into a generic OWL to JSON-LD converter
 * TODO - include inferred types
 * 
 * @author cjm
 *
 */
public class MolecularModelJsonRenderer {
	
	private static Logger LOG = Logger.getLogger(MolecularModelJsonRenderer.class);

	public static final String KEY_FACTS = "facts";
	public static final String KEY_INDIVIDUALS = "individuals";
	public static final String KEY_INDIVIDUALS_INFERENCES = "individuals_i";
	public static final String KEY_PROPERTIES = "properties";

	private final OWLOntology ont;
	private final OWLGraphWrapper graph;

	/**
	 * JSON-LD keywords for elements of different vocabularies:
	 * 
	 *<li>RDF
	 *<li>OWL
	 *<li>GO
	 *<li>RO
	 *
	 * TODO: use a complex enum to generate IRIs for each
	 *
	 */
	public enum KEY {
		id,
		label,
		type,
		enabledBy,
		occursIn,
		
		onProperty,
		someValuesFrom,
		
		intersectionOf,
		unionOf,
		
		subject,
		property,
		object,
		
		annotations // TODO final name?
	}
	
	/**
	 * merge with KEY?
	 *
	 */
	public enum VAL {
		Restriction,
		someValueFrom
	}
	
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
	public Map<Object, Object> renderModel() {
		Map<Object, Object> model = new HashMap<Object, Object>();
		
		// per-Individual
		List<Map<Object, Object>> iObjs = new ArrayList<Map<Object, Object>>();
		for (OWLNamedIndividual i : ont.getIndividualsInSignature()) {
			iObjs.add(renderObject(i));
		}
		model.put(KEY_INDIVIDUALS, iObjs);
		
		// per-Assertion
		Set<OWLObjectProperty> usedProps = new HashSet<OWLObjectProperty>();
		
		List<Map<Object, Object>> aObjs = new ArrayList<Map<Object, Object>>();
		for (OWLObjectPropertyAssertionAxiom opa : 
			ont.getAxioms(AxiomType.OBJECT_PROPERTY_ASSERTION)) {
			aObjs.add(renderObject(opa));
			usedProps.addAll(opa.getObjectPropertiesInSignature());
		}
		model.put(KEY_FACTS, aObjs);

		// per-Property
		List<Map<Object, Object>> pObjs = new ArrayList<Map<Object, Object>>();
		for (OWLObjectProperty p : ont.getObjectPropertiesInSignature(true)) {
			if (usedProps.contains(p)) {
				pObjs.add(renderObject(p));
			}
		}
		model.put(KEY_PROPERTIES, pObjs);

		List<Object> anObjs = renderAnnotations(ont.getAnnotations());
		if (!anObjs.isEmpty()) {
			model.put(KEY.annotations.name(), anObjs);
		}
		
		return model;
		
	}
	
	/**
	 * Add the available inferences to the given JSON map.
	 * 
	 * @param map model info
	 * @param reasoner
	 */
	public void renderModelInferences(Map<Object,Object> map, OWLReasoner reasoner) {
		if (reasoner.isConsistent() == false) {
			return;
		}
		if (map != null) {
			Set<OWLNamedIndividual> individuals = ont.getIndividualsInSignature();
			renderInferences(individuals, map, reasoner);
		}
	}
	
	public static List<Object> renderModelAnnotations(OWLOntology ont) {
		List<Object> anObjs = renderAnnotations(ont.getAnnotations());
		return anObjs;
	}
	
	private static List<Object> renderAnnotations(Set<OWLAnnotation> annotations) {
		List<Object> anObjs = new ArrayList<Object>();
		for (OWLAnnotation annotation : annotations) {
			OWLAnnotationProperty p = annotation.getProperty();
			LegoAnnotationType legoType = LegoAnnotationType.getLegoType(p.getIRI());
			if (legoType != null) {
				OWLAnnotationValue v = annotation.getValue();
				if (LegoAnnotationType.evidence.equals(legoType)) {
					IRI iri = (IRI) v;
					anObjs.add(Collections.singletonMap(legoType.name(), getId(iri)));
				}
				else {
					OWLLiteral literal = (OWLLiteral) v;
					anObjs.add(Collections.singletonMap(legoType.name(), literal.getLiteral()));
				}
			}
		}
		return anObjs;
	}
	
	public Map<Object, Object> renderIndividuals(Collection<OWLNamedIndividual> individuals) {
		OWLOntology ont = graph.getSourceOntology();
		Map<Object, Object> map = new HashMap<Object, Object>();
		List<Map<Object, Object>> iObjs = new ArrayList<Map<Object, Object>>();
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
		map.put(KEY_INDIVIDUALS, iObjs);
		
		List<Map<Object, Object>> aObjs = new ArrayList<Map<Object, Object>>();
		for (OWLObjectPropertyAssertionAxiom opa : opAxioms) {
			aObjs.add(renderObject(opa));
		}
		map.put(KEY_FACTS, aObjs);
		
		// TODO decide on properties
		
		return map;
	}
	
	/**
	 * Add the inferences for the given individuals to the JSON map.
	 * 
	 * @param individuals
	 * @param map
	 * @param reasoner
	 */
	public void renderInferences(Collection<OWLNamedIndividual> individuals, Map<Object, Object> map, OWLReasoner reasoner) {
		if (reasoner.isConsistent() == false) {
			return;
		}
		if (individuals != null && map != null) {
			List<Map<Object,Object>> iObjs = new ArrayList<Map<Object,Object>>(individuals.size());
			for (OWLNamedIndividual i : individuals) {
				Map<Object, Object> iObj = new HashMap<Object, Object>();
				iObj.put(KEY.id, getId(i, graph));
				Set<OWLClass> types = reasoner.getTypes(i, true).getFlattened();
				List<Object> typeObjs = new ArrayList<Object>(types.size());
				for (OWLClass x : types) {
					if (x.isBuiltIn()) {
						continue;
					}
					typeObjs.add(renderObject(x));
				}
				iObj.put(KEY.type, typeObjs);
				iObjs.add(iObj);
			}
			map.put(KEY_INDIVIDUALS_INFERENCES, iObjs);
		}
	}
	
	/**
	 * @param i
	 * @return Map to be passed to Gson
	 */
	public Map<Object, Object> renderObject(OWLNamedIndividual i) {
		Map<Object, Object> iObj = new HashMap<Object, Object>();
		String id = getId(i, graph);
		iObj.put(KEY.id, id);
		iObj.put(KEY.label, getLabel(i, id));
		
		List<Object> typeObjs = new ArrayList<Object>();		
		for (OWLClassExpression x : i.getTypes(ont)) {
			typeObjs.add(renderObject(x));
		}
		Map<OWLObjectPropertyExpression, Set<OWLIndividual>> pvs = i.getObjectPropertyValues(ont);
		for (OWLObjectPropertyExpression p : pvs.keySet()) {
			List<Object> valObjs = new ArrayList<Object>();
			for (OWLIndividual v : pvs.get(p)) {
				valObjs.add(getAtom((OWLNamedObject) v));
			}
			iObj.put(getId((OWLNamedObject) p, graph), valObjs);
		}
		iObj.put(KEY.type, typeObjs);
		List<Object> anObjs = new ArrayList<Object>();
		Set<OWLAnnotationAssertionAxiom> annotationAxioms = ont.getAnnotationAssertionAxioms(i.getIRI());
		for (OWLAnnotationAssertionAxiom ax : annotationAxioms) {
			OWLAnnotationProperty p = ax.getProperty();
			LegoAnnotationType legoType = LegoAnnotationType.getLegoType(p.getIRI());
			if (legoType != null) {
				OWLAnnotationValue v = ax.getValue();
				if (LegoAnnotationType.evidence.equals(legoType)) {
					IRI iri = (IRI) v;
					anObjs.add(Collections.singletonMap(legoType.name(), getId(iri)));
				}
				else {
					OWLLiteral literal = (OWLLiteral) v;
					anObjs.add(Collections.singletonMap(legoType.name(), literal.getLiteral()));
				}
			}
		}
		if (!anObjs.isEmpty()) {
			iObj.put(KEY.annotations, anObjs);
		}
		return iObj;
	}
	
	/**
	 * @param opa
	 * @return Map to be passed to Gson
	 */
	public Map<Object, Object> renderObject(OWLObjectPropertyAssertionAxiom opa) {
		Map<Object, Object> aObj = new HashMap<Object, Object>();
		OWLNamedIndividual subject;
		OWLObjectProperty property;
		OWLNamedIndividual object;

		subject = (OWLNamedIndividual) opa.getSubject();
		property = (OWLObjectProperty) opa.getProperty();
		object = (OWLNamedIndividual) opa.getObject();

		aObj.put(KEY.subject, getId(subject, graph));
		aObj.put(KEY.property, getId(property, graph));
		aObj.put(KEY.object, getId(object, graph));
		
		List<Object> anObjs = renderAnnotations(opa.getAnnotations());
		if (!anObjs.isEmpty()) {
			aObj.put(KEY.annotations, anObjs);
		}
		return aObj;
	}

	/**
	 * @param p
	 * @return Map to be passed to Gson
	 */
	public Map<Object, Object> renderObject(OWLObjectProperty p) {
		Map<Object, Object> iObj = new HashMap<Object, Object>();
		String id = getId(p, graph);
		iObj.put(KEY.id, id);
		iObj.put(KEY.label, getLabel(p, id));
		
		iObj.put(KEY.type, "ObjectProperty");
		return iObj;
	}
	/**
	 * @param x
	 * @return  Object to be passed to Gson
	 */
	private Object renderObject(OWLClassExpression x) {
		Map<Object, Object> xObj = new HashMap<Object, Object>();
		if (x.isAnonymous()) {
			if (x instanceof OWLObjectIntersectionOf) {
				List<Object> yObjs = new ArrayList<Object>();		
				for (OWLClassExpression y : ((OWLObjectIntersectionOf)x).getOperands()) {
					yObjs.add(renderObject(y));
				}
				xObj.put(KEY.intersectionOf, yObjs);
			}
			else if (x instanceof OWLObjectUnionOf) {
				List<Object> yObjs = new ArrayList<Object>();		
				for (OWLClassExpression y : ((OWLObjectUnionOf)x).getOperands()) {
					yObjs.add(renderObject(y));
				}
				xObj.put(KEY.unionOf, yObjs);
			}
			else if (x instanceof OWLObjectSomeValuesFrom) {
				OWLObjectSomeValuesFrom svf = (OWLObjectSomeValuesFrom)x;
				xObj.put(KEY.type, VAL.Restriction);
				xObj.put(KEY.onProperty, renderObject(svf.getProperty()));
				xObj.put(KEY.someValuesFrom, renderObject(svf.getFiller()));				
			}
			else {
				// TODO
			}
		}
		else {
			return getAtom((OWLNamedObject)x);
		}
		return xObj;
	}

	private Object renderObject(OWLObjectPropertyExpression px) {
		if (px.isAnonymous()) {
			return null; // TODO
		}
		else {
			return getAtom((OWLNamedObject)px);
		}
	}

	protected Object getLabel(OWLNamedObject i, String id) {
		return graph.getLabel(i);
	}
	
	private Object getAtom(OWLNamedObject i) {
		Map<Object, Object> xObj = new HashMap<Object, Object>();
		
		String id = getId(i, graph);
		if (LOG.isDebugEnabled()) {
			LOG.debug("atom: "+i+" "+id);
		}
		xObj.put(KEY.id, id);
		xObj.put(KEY.label, getLabel(i, id));
		String type = null;
		if (i instanceof OWLNamedIndividual) {
			type = "NamedIndividual";
		}
		else if (i instanceof OWLClass) {
			type = "Class";
		}
		else if (i instanceof OWLObjectProperty) {
			type = "ObjectProperty";
		}
		xObj.put(KEY.type, type);
		return xObj;
	}


	/**
	 * @param i
	 * @param graph 
	 * @return id
	 * 
	 * @see MolecularModelJsonRenderer#getIRI
	 */
	public static String getId(OWLNamedObject i, OWLGraphWrapper graph) {
		if (i instanceof OWLObjectProperty) {
			String relId = graph.getIdentifier(i);
			return relId;
		}
		IRI iri = i.getIRI();
		return getId(iri);
	}

	/**
	 * @param iri
	 * @return id
	 */
	public static String getId(IRI iri) {
		String iriString = iri.toString();
		// remove obo prefix from IRI
		String full = StringUtils.removeStart(iriString, OBOUpperVocabulary.OBO);
		String replaced;
		if (full.startsWith("#")) {
			replaced = StringUtils.removeStart(full, "#");
		}
		else {
			// replace first '_' char with ':' char
			replaced = StringUtils.replaceOnce(full, "_", ":");
		}
		return replaced;
	}
	
	/**
	 * Inverse method to {@link #getId}
	 * 
	 * @param id
	 * @param graph
	 * @return IRI
	 * 
	 * @see MolecularModelJsonRenderer#getId
	 */
	public static IRI getIRI(String id, OWLGraphWrapper graph) {
		if (id.indexOf(':') < 0) {
			return graph.getIRIByIdentifier(id);
		}
		if(id.startsWith(OBOUpperVocabulary.OBO) ){
			return IRI.create(id);
		}
		String fullIRI = OBOUpperVocabulary.OBO + StringUtils.replaceOnce(id, ":", "_");
		return IRI.create(fullIRI);
	}
	
	public static List<Map<Object, Object>> renderRelations(MolecularModelManager<?> mmm, Set<String> relevantRelations) throws OWLOntologyCreationException {
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
		List<Map<Object, Object>> relList = new ArrayList<Map<Object,Object>>();
		for (OWLObjectProperty p : propertyList) {
			if (p.isBuiltIn()) {
				// skip owl:topObjectProperty
				continue;
			}
			String identifier = MolecularModelJsonRenderer.getId(p, wrapper);
			String label = wrapper.getLabel(p);
			Map<Object, Object> entry = new HashMap<Object, Object>();
			entry.put("id", identifier);
			entry.put("label", label);
			if (relevantRelations != null && (relevantRelations.contains(label) || (relevantRelations.contains(identifier)))) {
				entry.put("relevant", "true");
			}
			relList.add(entry);
		}
		return relList;
	}
	
	public static List<Map<Object, Object>> renderEvidences(MolecularModelManager<?> mmm) throws OWLException, IOException {
		return renderEvidences(mmm.getGraph().getManager());
	}
	
	public static List<Map<Object, Object>> renderEvidences(OWLOntologyManager manager) throws OWLException, IOException {
		OntologyMapperPair<EcoMapper> pair = EcoMapperFactory.createEcoMapper(manager);
		final OWLGraphWrapper graph = pair.getGraph();
		final EcoMapper mapper = pair.getMapper();
		Set<OWLClass> ecoClasses = graph.getAllOWLClasses();
		Map<OWLClass, String> codesForEcoClasses = mapper.getCodesForEcoClasses();
		List<Map<Object, Object>> relList = new ArrayList<Map<Object,Object>>();
		for (OWLClass ecoClass : ecoClasses) {
			if (ecoClass.isBuiltIn()) {
				continue;
			}
			String identifier = MolecularModelJsonRenderer.getId(ecoClass, graph);
			String label = graph.getLabel(ecoClass);
			Map<Object, Object> entry = new HashMap<Object, Object>();
			entry.put("id", identifier);
			entry.put("label", label);
			String code = codesForEcoClasses.get(ecoClass);
			if (code != null) {
				entry.put("code", code);
			}
			relList.add(entry);
		}
		return relList;
	}

	private static final Gson gson = new Gson();
	
	public static String renderToJson(OWLOntology ont) {
		MolecularModelJsonRenderer r = new MolecularModelJsonRenderer(ont);
		Map<Object, Object> obj = r.renderModel();
		return gson.toJson(obj);
	}

}
