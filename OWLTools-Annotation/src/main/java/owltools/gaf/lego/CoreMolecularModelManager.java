package owltools.gaf.lego;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.AddOntologyAnnotation;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.RemoveAxiom;
import org.semanticweb.owlapi.model.RemoveImport;
import org.semanticweb.owlapi.model.RemoveOntologyAnnotation;
import org.semanticweb.owlapi.model.SetOntologyID;

import owltools.gaf.eco.EcoMapper;
import owltools.graph.OWLGraphWrapper;
import owltools.vocab.OBOUpperVocabulary;

/**
 * Manager and core operations for in memory MolecularModels (aka lego diagrams).
 * 
 * Any number of models can be loaded at any time <br>
 * TODO - impose some limit to avoid using too much memory
 * 
 * each model has a generator, an OWLOntology (containing the set of class assertions)
 * and a reasoner associated with it<br>
 * TODO - test memory requirements
 * 
 * @param <METADATA> object for holding meta data associated with each operation
 */
public abstract class CoreMolecularModelManager<METADATA> {
	
	private static Logger LOG = Logger.getLogger(CoreMolecularModelManager.class);

	final OWLGraphWrapper graph;
	final Map<String, LegoModelGenerator> modelMap = new HashMap<String, LegoModelGenerator>();
	Set<IRI> additionalImports;
	final Set<IRI> obsoleteImports = new HashSet<IRI>();

	// TODO: Temporarily for keeping instances unique (search for "unique" below).
	static String uniqueTop = Long.toHexString((System.currentTimeMillis()/1000));
	static long instanceCounter = 0;
	
	private static String localUnique(){
		instanceCounter++;
		String unique = uniqueTop + String.format("%07d", instanceCounter);
		return unique;		
	}
	
	static String generateId(CharSequence...prefixes) {
		StringBuilder sb = new StringBuilder();
		for (CharSequence prefix : prefixes) {
			sb.append(prefix);
		}
		/*
		 * TODO finalize identifier policy
		 */
		sb.append(localUnique());
		return sb.toString();
	}

	/**
	 * @param graph
	 * @throws OWLOntologyCreationException
	 */
	public CoreMolecularModelManager(OWLGraphWrapper graph) throws OWLOntologyCreationException {
		super();
		this.graph = graph;
		init();
	}

	/**
	 * @throws OWLOntologyCreationException
	 */
	protected void init() throws OWLOntologyCreationException {
		// set default imports
		additionalImports = new HashSet<IRI>();
		additionalImports.add(IRI.create("http://purl.obolibrary.org/obo/ro.owl"));
		additionalImports.add(IRI.create("http://purl.obolibrary.org/obo/go/extensions/ro_pending.owl"));
		additionalImports.add(EcoMapper.ECO_PURL_IRI);
	}


	/**
	 * @return graph wrapper for core/source ontology
	 */
	public OWLGraphWrapper getGraph() {
		return graph;
	}

	/**
	 * @return core/source ontology
	 */
	public OWLOntology getOntology() {
		return graph.getSourceOntology();
	}


	/**
	 * Add additional import declarations for any newly generated model.
	 * 
	 * @param imports
	 */
	public void addImports(Iterable<String> imports) {
		if (imports != null) {
			for (String importIRIString : imports) {
				additionalImports.add(IRI.create(importIRIString));
			}
		}
	}
	
	/**
	 * Mark the given imports as obsolete.
	 * 
	 * @param obsoletes
	 */
	public void addObsoleteImports(Iterable<String> obsoletes) {
		if (obsoletes != null) {
			for (String obsolete : obsoletes) {
				obsoleteImports.add(IRI.create(obsolete));
			}
		}
	}
	
	public Collection<IRI> getImports() {
		return Collections.unmodifiableCollection(additionalImports);
	}

	/**
	 * 
	 * @param modelId
	 * @return all individuals in the model
	 */
	public Set<OWLNamedIndividual> getIndividuals(String modelId) {
		LegoModelGenerator mod = getModel(modelId);
		return mod.getAboxOntology().getIndividualsInSignature();
	}

	
	/**
	 * @param modelId
	 * @param q
	 * @return all individuals in the model that satisfy q
	 */
	public Set<OWLNamedIndividual> getIndividualsByQuery(String modelId, OWLClassExpression q) {
		LegoModelGenerator mod = getModel(modelId);
		return mod.getReasoner().getInstances(q, false).getFlattened();
	}

	/**
	 * Only use for testing.
	 * 
	 * @param modelId
	 * @return List of key-val pairs ready for Gson
	 */
	List<Map<Object, Object>> getIndividualObjects(String modelId) {
		LegoModelGenerator mod = getModel(modelId);
		MolecularModelJsonRenderer renderer = new MolecularModelJsonRenderer(mod.getAboxOntology());
		OWLOntology ont = mod.getAboxOntology();
		List<Map<Object, Object>> objs = new ArrayList<Map<Object, Object>>();
		for (OWLNamedIndividual i : ont.getIndividualsInSignature()) {
			objs.add(renderer.renderObject(i));
		}
		return objs;
	}
	
	/**
	 * @param modelId
	 * @param model
	 * @param c
	 * @param metadata
	 * @return individual
	 */
	public OWLNamedIndividual createIndividual(String modelId, LegoModelGenerator model, OWLClass c, METADATA metadata) {
		OWLNamedIndividual individual = createIndividual(modelId, model, c, null, true, metadata);
		return individual;
	}
	
	OWLNamedIndividual createIndividual(String modelId, LegoModelGenerator model, OWLClass c, Set<OWLAnnotation> annotations, boolean flushReasoner, METADATA metadata) {
		LOG.info("Creating individual of type: "+c);
		OWLGraphWrapper graph = new OWLGraphWrapper(model.getAboxOntology());
		String cid = graph.getIdentifier(c).replaceAll(":","-"); // e.g. GO-0123456

		// Make something unique to tag onto the generated IDs.
		String iid = generateId(modelId, "-", cid, "-");
		LOG.info("  new OD: "+iid);

		IRI iri = MolecularModelJsonRenderer.getIRI(iid, graph);
		OWLDataFactory f = model.getOWLDataFactory();
		OWLNamedIndividual i = f.getOWLNamedIndividual(iri);
		
		// create axioms
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		// declaration
		axioms.add(f.getOWLDeclarationAxiom(i));
		// annotation assertions
		if(annotations != null) {
			for(OWLAnnotation annotation : annotations) {
				axioms.add(f.getOWLAnnotationAssertionAxiom(iri, annotation));
			}
		}
		
		addAxioms(modelId, model, axioms, flushReasoner, metadata);
		addType(modelId, model, i, c, flushReasoner, metadata);
		return i;
	}
	
	/**
	 * Deletes an individual
	 * 
	 * @param modelId
	 * @param model
	 * @param i
	 * @param metadata
	 */
	public void deleteIndividual(String modelId, LegoModelGenerator model, OWLNamedIndividual i, METADATA metadata) {
		deleteIndividual(modelId, model, i, true, metadata);
	}
	
	/**
	 * Deletes an individual
	 * 
	 * @param modelId 
	 * @param model
	 * @param i
	 * @param flushReasoner
	 * @param metadata
	 */
	void deleteIndividual(String modelId, LegoModelGenerator model, OWLNamedIndividual i, boolean flushReasoner, METADATA metadata) {
		Set<OWLAxiom> toRemoveAxioms = new HashSet<OWLAxiom>();
		
		OWLOntology ont = model.getAboxOntology();
		
		// Declaration axiom
		toRemoveAxioms.add(model.getOWLDataFactory().getOWLDeclarationAxiom(i));
		
		// Logic axiom
		for (OWLAxiom ax : ont.getAxioms(i)) {
			toRemoveAxioms.add(ax);
		}
		
		// OWLObjectPropertyAssertionAxiom
		Set<OWLObjectPropertyAssertionAxiom> allAssertions = ont.getAxioms(AxiomType.OBJECT_PROPERTY_ASSERTION);
		for (OWLObjectPropertyAssertionAxiom ax : allAssertions) {
			if (toRemoveAxioms.contains(ax) == false) {
				Set<OWLNamedIndividual> currentIndividuals = ax.getIndividualsInSignature();
				if (currentIndividuals.contains(i)) {
					toRemoveAxioms.add(ax);
				}
			}
		}
		// OWLAnnotationAssertionAxiom
		toRemoveAxioms.addAll(ont.getAnnotationAssertionAxioms(i.getIRI()));
		
		removeAxioms(modelId, model, toRemoveAxioms, flushReasoner, metadata);
	}
	
	public void addAnnotations(String modelId, LegoModelGenerator model, OWLNamedIndividual i, Collection<OWLAnnotation> annotations, METADATA metadata) {
		addAnnotations(modelId, model, i.getIRI(), annotations, metadata);
	}
	
	void addAnnotations(String modelId, LegoModelGenerator model, IRI subject, Collection<OWLAnnotation> annotations, METADATA metadata) {
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		OWLDataFactory f = model.getOWLDataFactory();
		for (OWLAnnotation annotation : annotations) {
			axioms.add(f.getOWLAnnotationAssertionAxiom(subject, annotation));
		}
		addAxioms(modelId, model, axioms, false, metadata);
	}
	
	void addAnnotations(String modelId, LegoModelGenerator model, Collection<OWLAnnotation> annotations, METADATA metadata) {
		OWLOntology aBox = model.getAboxOntology();
		List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
		for (OWLAnnotation annotation : annotations) {
			changes.add(new AddOntologyAnnotation(aBox, annotation));
		}
		applyChanges(modelId, model, changes, false, metadata);
	}

	public void removeAnnotations(String modelId, LegoModelGenerator model, OWLNamedIndividual i, Collection<OWLAnnotation> annotations, METADATA metadata) {
		removeAnnotations(modelId, model, i.getIRI(), annotations, metadata);
	}
	
	void removeAnnotations(String modelId, LegoModelGenerator model, IRI subject, Collection<OWLAnnotation> annotations, METADATA metadata) {
		OWLOntology ont = model.getAboxOntology();
		Set<OWLAxiom> toRemove = new HashSet<OWLAxiom>();
		Set<OWLAnnotationAssertionAxiom> candidates = ont.getAnnotationAssertionAxioms(subject);
		for (OWLAnnotationAssertionAxiom axiom : candidates) {
			OWLAnnotation annotation = axiom.getAnnotation();
			if (annotations.contains(annotation)) {
				toRemove.add(axiom);
			}
		}
		removeAxioms(modelId, model, toRemove, false, metadata);
	}

	void removeAnnotations(String modelId, LegoModelGenerator model, Collection<OWLAnnotation> annotations, METADATA metadata) {
		OWLOntology aBox = model.getAboxOntology();
		List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
		for (OWLAnnotation annotation : annotations) {
			changes.add(new RemoveOntologyAnnotation(aBox, annotation));
		}
		applyChanges(modelId, model, changes, false, metadata);
	}
	
	/**
	 * Fetches a model by its Id
	 * 
	 * @param id
	 * @return wrapped model
	 */
	public LegoModelGenerator getModel(String id)  {
		if (!modelMap.containsKey(id)) {
			try {
				loadModel(id, false);
			} catch (OWLOntologyCreationException e) {
				LOG.info("Could not load model with id: "+id, e);
			}
		}
		return modelMap.get(id);
	}
	/**
	 * @param id
	 */
	public void unlinkModel(String id) {
		LegoModelGenerator model = modelMap.get(id);
		model.dispose();
		modelMap.remove(id);
	}
	
	/**
	 * @return ids for all loaded models
	 */
	public Set<String> getModelIds() {
		return modelMap.keySet();
	}
	
	/**
	 * internal method to cleanup this instance
	 */
	public void dispose() {
		Set<String> ids = new HashSet<String>(getModelIds());
		for (String id : ids) {
			unlinkModel(id);
		}
	}

	/**
	 * Export the ABox, will try to set the ontologyID to the given modelId (to
	 * ensure import assumptions are met)
	 * 
	 * @param modelId
	 * @param model
	 * @param ontologyFormat
	 * @return modelContent
	 * @throws OWLOntologyStorageException
	 */
	public String exportModel(String modelId, LegoModelGenerator model, OWLOntologyFormat ontologyFormat) throws OWLOntologyStorageException {
		final OWLOntology aBox = model.getAboxOntology();
		final OWLOntologyManager manager = aBox.getOWLOntologyManager();
		
		// make sure the exported ontology has an ontologyId and that it maps to the modelId
		final IRI expectedABoxIRI = MolecularModelJsonRenderer.getIRI(modelId, graph);
		OWLOntologyID ontologyID = aBox.getOntologyID();
		if (ontologyID == null) {
			manager.applyChange(new SetOntologyID(aBox, expectedABoxIRI));
		}
		else {
			IRI currentABoxIRI = ontologyID.getOntologyIRI();
			if (expectedABoxIRI.equals(currentABoxIRI) == false) {
				ontologyID = new OWLOntologyID(expectedABoxIRI, ontologyID.getVersionIRI());
				manager.applyChange(new SetOntologyID(aBox, ontologyID));
			}
		}

		// write the model into a buffer
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		if (ontologyFormat != null) {
			manager.saveOntology(aBox, ontologyFormat, outputStream);
		}
		else {
			manager.saveOntology(aBox, outputStream);
		}
		
		// extract the string from the buffer
		String modelString = outputStream.toString();
		return modelString;
	}
	
	/**
	 * Try to load (or replace) a model with the given ontology. It is expected
	 * that the content is an A-Box ontology, which imports the T-BOX. Also the
	 * ontology ID is used to extract the modelId.<br>
	 * <br>
	 * This method will currently <b>NOT<b> work due to a bug in the OWL-API.
	 * The functional syntax parser does not properly report the exceptions and
	 * will return an ontology with an wrong ontology ID!
	 * 
	 * @param modelData
	 * @return modelId
	 * @throws OWLOntologyCreationException
	 */
	public String importModel(String modelData) throws OWLOntologyCreationException {
		// load data from String
		final OWLOntologyManager manager = graph.getManager();
		final OWLOntologyDocumentSource documentSource = new StringDocumentSource(modelData);
		OWLOntology modelOntology;
		try {
			modelOntology = manager.loadOntologyFromOntologyDocument(documentSource);
		}
		catch (OWLOntologyAlreadyExistsException e) {
			// exception is thrown if there is an ontology with the same ID already in memory 
			OWLOntologyID id = e.getOntologyID();
			String existingModelId = MolecularModelJsonRenderer.getId(id.getOntologyIRI());

			// remove the existing memory model
			unlinkModel(existingModelId);

			// try loading the import version (again)
			modelOntology = manager.loadOntologyFromOntologyDocument(documentSource);
		}
		
		// try to extract modelId
		String modelId = null;
		OWLOntologyID ontologyId = modelOntology.getOntologyID();
		if (ontologyId != null) {
			IRI iri = ontologyId.getOntologyIRI();
			if (iri != null) {
				modelId = MolecularModelJsonRenderer.getId(iri);
			}
		}
		if (modelId == null) {
			throw new OWLOntologyCreationException("Could not extract the modelId from the given model");
		}
		// paranoia check
		LegoModelGenerator existingModel = modelMap.get(modelId);
		if (existingModel != null) {
			unlinkModel(modelId);
		}
		
		// add to internal model
		LegoModelGenerator newModel = addModel(modelId, modelOntology);
		
		// update imports
		updateImports(newModel);
		
		return modelId;
	}
	
	/**
	 * Retrieve a collection of all model ids currently in memory.<br>
	 * 
	 * @return set of modelids.
	 * @throws IOException
	 */
	public Set<String> getCurrentModelIds() throws IOException {
		Set<String> allModelIds = new HashSet<String>();
		// add all model ids currently in memory
		allModelIds.addAll(modelMap.keySet());
		return allModelIds;
	}
	
	protected abstract void loadModel(String modelId, boolean isOverride) throws OWLOntologyCreationException;

	LegoModelGenerator addModel(String modelId, OWLOntology abox) throws OWLOntologyCreationException {
		OWLOntology tbox = graph.getSourceOntology();
		LegoModelGenerator m = new LegoModelGenerator(tbox, abox);
		modelMap.put(modelId, m);
		return m;
	}

	
	/**
	 * 
	 * @param modelId
	 * @return true if the ontology formed by the specified model is inconsistent
	 */
	public boolean isConsistent(String modelId) {
		LegoModelGenerator model = getModel(modelId);
		// TODO - is it scalable to have each model have its own reasoner?
		// may make more sense to have a single reasoner instance operating over entire kb;
		// this would mean the entire kb should be kept consistent - an inconsistency in one
		// model would mean the entire kb is inconsistent
		return model.getReasoner().isConsistent();
	}

	/**
	 * @param modelId
	 * @return data factory for the specified model
	 */
	public OWLDataFactory getOWLDataFactory(String modelId) {
		LegoModelGenerator model = getModel(modelId);
		return model.getOWLDataFactory();
	}

	protected OWLOntologyManager getOWLOntologyManager(String modelId) {
		LegoModelGenerator model = getModel(modelId);
		return model.getAboxOntology().getOWLOntologyManager();
	}

	/**
	 * Adds ClassAssertion(c,i) to specified model
	 * 
	 * @param modelId
	 * @param i
	 * @param c
	 * @param metadata
	 */
	public void addType(String modelId, OWLNamedIndividual i, OWLClass c, METADATA metadata) {
		LegoModelGenerator model = getModel(modelId);
		addType(modelId, model, i, c, true, metadata);
	}
	
	/**
	 * Adds ClassAssertion(c,i) to specified model
	 * 
	 * @param modelId 
	 * @param model
	 * @param i
	 * @param c
	 * @param flushReasoner
	 * @param metadata
	 */
	void addType(String modelId, LegoModelGenerator model, OWLIndividual i, 
			OWLClassExpression c, boolean flushReasoner, METADATA metadata) {
		OWLClassAssertionAxiom axiom = model.getOWLDataFactory().getOWLClassAssertionAxiom(c,i);
		addAxiom(modelId, model, axiom, flushReasoner, metadata);
	}

	/**
	 * Adds a ClassAssertion, where the class expression instantiated is an
	 * ObjectSomeValuesFrom expression
	 * 
	 * Example: Individual: i Type: enabledBy some PRO_123 
	 * 
	 * @param modelId
	 * @param i
	 * @param p
	 * @param filler
	 * @param metadata
	 */
	public void addType(String modelId,
			OWLNamedIndividual i, 
			OWLObjectPropertyExpression p,
			OWLClassExpression filler,
			METADATA metadata) {
		LegoModelGenerator model = getModel(modelId);
		addType(modelId, model, i, p, filler, true, metadata);
	}
	
	/**
	 * Adds a ClassAssertion, where the class expression instantiated is an
	 * ObjectSomeValuesFrom expression
	 * 
	 * Example: Individual: i Type: enabledBy some PRO_123
	 *  
	 * @param modelId 
	 * @param model
	 * @param i
	 * @param p
	 * @param filler
	 * @param flushReasoner
	 * @param metadata
	 */
	void addType(String modelId,
			LegoModelGenerator model,
			OWLIndividual i, 
			OWLObjectPropertyExpression p,
			OWLClassExpression filler,
			boolean flushReasoner,
			METADATA metadata) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Adding "+i+ " type "+p+" some "+filler);	
		}
		OWLDataFactory f = model.getOWLDataFactory();
		OWLObjectSomeValuesFrom c = f.getOWLObjectSomeValuesFrom(p, filler);
		OWLClassAssertionAxiom axiom = f.getOWLClassAssertionAxiom(c, i);
		addAxiom(modelId, model, axiom, flushReasoner, metadata);
	}
	
	/**
	 * remove ClassAssertion(c,i) to specified model
	 * 
	 * @param modelId
	 * @param i
	 * @param c
	 * @param metadata
	 */
	public void removeType(String modelId, OWLNamedIndividual i, OWLClass c, METADATA metadata) {
		LegoModelGenerator model = getModel(modelId);
		removeType(modelId, model, i, c, true, metadata);
	}

	/**
	 * remove ClassAssertion(c,i) from the model
	 * 
	 * @param modelId 
	 * @param model
	 * @param i
	 * @param ce
	 * @param flushReasoner
	 * @param metadata
	 */
	void removeType(String modelId, LegoModelGenerator model, OWLIndividual i, 
			OWLClassExpression ce, boolean flushReasoner, METADATA metadata) {
		Set<OWLClassAssertionAxiom> allAxioms = model.getAboxOntology().getClassAssertionAxioms(i);
		// use search to remove also axioms with annotations
		for (OWLClassAssertionAxiom ax : allAxioms) {
			if (ce.equals(ax.getClassExpression())) {
				removeAxiom(modelId, model, ax, flushReasoner, metadata);
			}
		}
		
	}
	
	/**
	 * Removes a ClassAssertion, where the class expression instantiated is an
	 * ObjectSomeValuesFrom expression
	 * 
	 * @param modelId
	 * @param i
	 * @param p
	 * @param filler
	 * @param metadata
	 */
	public void removeType(String modelId,
			OWLNamedIndividual i, 
			OWLObjectPropertyExpression p,
			OWLClassExpression filler,
			METADATA metadata) {
		LegoModelGenerator model = getModel(modelId);
		removeType(modelId, model, i, p, filler, true, metadata);
	}
	
	
	void removeType(String modelId,
			LegoModelGenerator model,
			OWLIndividual i, 
			OWLObjectPropertyExpression p,
			OWLClassExpression filler,
			boolean flushReasoner,
			METADATA metadata) {
		OWLDataFactory f = model.getOWLDataFactory();
		OWLClassAssertionAxiom axiom = f.getOWLClassAssertionAxiom(f.getOWLObjectSomeValuesFrom(p, filler), i);
		removeAxiom(modelId, model, axiom, flushReasoner, metadata);
	}

	/**
	 * Adds triple (i,p,j) to specified model
	 * 
	 * @param modelId
	 * @param p
	 * @param i
	 * @param j
	 * @param annotations
	 * @param metadata
	 */
	public void addFact(String modelId, OWLObjectPropertyExpression p,
			OWLNamedIndividual i, OWLNamedIndividual j, Set<OWLAnnotation> annotations, METADATA metadata) {
		LegoModelGenerator model = getModel(modelId);
		addFact(modelId, model, p, i, j, annotations, true, metadata);
	}
	
	/**
	 * Convenience wrapper for {@link #addFact(String, OWLObjectPropertyExpression, OWLNamedIndividual, OWLNamedIndividual, Set, Object)}
	 *	
	 * @param modelId
	 * @param vocabElement
	 * @param i
	 * @param j
	 * @param annotations
	 * @param metadata
	 */
	public void addFact(String modelId, OBOUpperVocabulary vocabElement,
			OWLNamedIndividual i, OWLNamedIndividual j, Set<OWLAnnotation> annotations, METADATA metadata) {
		LegoModelGenerator model = getModel(modelId);
		OWLObjectProperty p = vocabElement.getObjectProperty(model.getAboxOntology());
		addFact(modelId, model, p, i, j, annotations, true, metadata);
	}

	void addFact(String modelId, LegoModelGenerator model, OWLObjectPropertyExpression p,
			OWLIndividual i, OWLIndividual j, Set<OWLAnnotation> annotations, boolean flushReasoner, METADATA metadata) {
		OWLDataFactory f = model.getOWLDataFactory();
		final OWLObjectPropertyAssertionAxiom axiom;
		if (annotations != null && !annotations.isEmpty()) {
			axiom = f.getOWLObjectPropertyAssertionAxiom(p, i, j, annotations);	
		}
		else {
			axiom = f.getOWLObjectPropertyAssertionAxiom(p, i, j);
		}
		addAxiom(modelId, model, axiom, flushReasoner, metadata);
	}

	public void removeFact(String modelId, OWLObjectPropertyExpression p,
			OWLNamedIndividual i, OWLNamedIndividual j, METADATA metadata) {
		LegoModelGenerator model = getModel(modelId);
		removeFact(modelId, model, p, i, j, true, metadata);
	}

	void removeFact(String modelId, LegoModelGenerator model, OWLObjectPropertyExpression p,
			OWLIndividual i, OWLIndividual j, boolean flushReasoner, METADATA metadata) {
		OWLDataFactory f = model.getOWLDataFactory();
		
		OWLOntology ont = model.getAboxOntology();
		OWLAxiom toRemove = null;
		Set<OWLObjectPropertyAssertionAxiom> candidates = ont.getObjectPropertyAssertionAxioms(i);
		for (OWLObjectPropertyAssertionAxiom axiom : candidates) {
			if (p.equals(axiom.getProperty()) && j.equals(axiom.getObject())) {
				toRemove = axiom;
				break;
			}
		}
		if (toRemove == null) {
			// fall back solution
			toRemove = f.getOWLObjectPropertyAssertionAxiom(p, i, j);
		}
		removeAxiom(modelId, model, toRemove, flushReasoner, metadata);
	}
	
	public void addAnnotations(String modelId, OWLObjectPropertyExpression p,
			OWLNamedIndividual i, OWLNamedIndividual j, Set<OWLAnnotation> annotations, METADATA metadata) {
		LegoModelGenerator model = getModel(modelId);
		addAnnotations(modelId, model, p, i, j, annotations, true, metadata);
	}
	
	void addAnnotations(String modelId, LegoModelGenerator model, OWLObjectPropertyExpression p,
			OWLNamedIndividual i, OWLNamedIndividual j, Set<OWLAnnotation> annotations,
			boolean flushReasoner, METADATA metadata) {
		OWLOntology ont = model.getAboxOntology();
		Set<OWLObjectPropertyAssertionAxiom> axioms = ont.getObjectPropertyAssertionAxioms(i);
		OWLObjectPropertyAssertionAxiom toModify = null;
		for (OWLObjectPropertyAssertionAxiom axiom : axioms) {
			if (p.equals(axiom.getProperty()) && j.equals(axiom.getObject())) {
				toModify = axiom;
				break;
			}
		}
		if (toModify != null) {
			OWLDataFactory f = model.getOWLDataFactory();
			Set<OWLAnnotation> combindedAnnotations = new HashSet<OWLAnnotation>(annotations);
			combindedAnnotations.addAll(toModify.getAnnotations());
			List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>(2);
			changes.add(new RemoveAxiom(ont, toModify));
			OWLAxiom newAxiom = f.getOWLObjectPropertyAssertionAxiom(p, i, j, combindedAnnotations);
			changes.add(new AddAxiom(ont, newAxiom));
			applyChanges(modelId, model, changes, flushReasoner, metadata);
		}
	}
	
	void removeAnnotations(String modelId, LegoModelGenerator model, OWLObjectPropertyExpression p,
			OWLNamedIndividual i, OWLNamedIndividual j, Set<OWLAnnotation> annotations,
			boolean flushReasoner, METADATA metadata) {
		OWLOntology ont = model.getAboxOntology();
		Set<OWLObjectPropertyAssertionAxiom> axioms = ont.getObjectPropertyAssertionAxioms(i);
		OWLObjectPropertyAssertionAxiom toModify = null;
		for (OWLObjectPropertyAssertionAxiom axiom : axioms) {
			if (p.equals(axiom.getProperty()) && j.equals(axiom.getObject())) {
				toModify = axiom;
				break;
			}
		}
		if (toModify != null) {
			OWLDataFactory f = model.getOWLDataFactory();
			Set<OWLAnnotation> combindedAnnotations = new HashSet<OWLAnnotation>(toModify.getAnnotations());
			combindedAnnotations.removeAll(annotations);
			List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>(2);
			changes.add(new RemoveAxiom(ont, toModify));
			OWLAxiom newAxiom = f.getOWLObjectPropertyAssertionAxiom(p, i, j, combindedAnnotations);
			changes.add(new AddAxiom(ont, newAxiom));
			applyChanges(modelId, model, changes, flushReasoner, metadata);
		}
	}
	
	void addAxiom(String modelId, LegoModelGenerator model, OWLAxiom axiom, 
			boolean flushReasoner, METADATA metadata) {
		OWLOntology ont = model.getAboxOntology();
		List<OWLOntologyChange> changes = Collections.<OWLOntologyChange>singletonList(new AddAxiom(ont, axiom));
		synchronized (ont) {
			/*
			 * all changes to the ontology are synchronized via the ontology object
			 */
			applyChanges(modelId, model, ont.getOWLOntologyManager(), changes, metadata);	
		}
		if (flushReasoner) {
			model.getReasoner().flush();
		}
	}
	
	void addAxioms(String modelId, LegoModelGenerator model, Set<OWLAxiom> axioms, 
			boolean flushReasoner, METADATA metadata) {
		OWLOntology ont = model.getAboxOntology();
		List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>(axioms.size());
		for(OWLAxiom axiom : axioms) {
			changes.add(new AddAxiom(ont, axiom));
		}
		synchronized (ont) {
			/*
			 * all changes to the ontology are synchronized via the ontology object
			 */
			applyChanges(modelId, model, ont.getOWLOntologyManager(), changes, metadata);
		}
		if (flushReasoner) {
			model.getReasoner().flush();
		}
	}
	
	void removeAxiom(String modelId, LegoModelGenerator model, OWLAxiom axiom, 
			boolean flushReasoner, METADATA metadata) {
		OWLOntology ont = model.getAboxOntology();
		List<OWLOntologyChange> changes = Collections.<OWLOntologyChange>singletonList(new RemoveAxiom(ont, axiom));
		synchronized (ont) {
			/*
			 * all changes to the ontology are synchronized via the ontology object
			 */
			applyChanges(modelId, model, ont.getOWLOntologyManager(), changes, metadata);
		}
		if (flushReasoner) {
			model.getReasoner().flush();
		}
	}

	void removeAxioms(String modelId, Set<OWLAxiom> axioms, boolean flushReasoner, METADATA metadata) {
		LegoModelGenerator model = getModel(modelId);
		removeAxioms(modelId, model, axioms, flushReasoner, metadata);
	}
	
	void removeAxioms(String modelId, LegoModelGenerator model, Set<OWLAxiom> axioms, 
			boolean flushReasoner, METADATA metadata) {
		OWLOntology ont = model.getAboxOntology();
		List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>(axioms.size());
		for(OWLAxiom axiom : axioms) {
			changes.add(new RemoveAxiom(ont, axiom));
		}
		synchronized (ont) {
			/*
			 * all changes to the ontology are synchronized via the ontology object
			 */
			applyChanges(modelId, model, ont.getOWLOntologyManager(), changes, metadata);
		}
		if (flushReasoner) {
			model.getReasoner().flush();
		}
	}

	private void applyChanges(String modelId, LegoModelGenerator model, 
			List<OWLOntologyChange> changes, boolean flushReasoner, METADATA metadata) {
		OWLOntology ont = model.getAboxOntology();
		synchronized (ont) {
			/*
			 * all changes to the ontology are synchronized via the ontology object
			 */
			applyChanges(modelId, model, ont.getOWLOntologyManager(), changes, metadata);
		}
		if (flushReasoner) {
			model.getReasoner().flush();
		}
	}
	
	private void applyChanges(String modelId, LegoModelGenerator model, OWLOntologyManager m, 
			List<? extends OWLOntologyChange> changes, METADATA metadata) {
		List<OWLOntologyChange> appliedChanges = m.applyChanges(changes);
		addToHistory(modelId, model, appliedChanges, metadata);
	}
	
	/**
	 * Hook for implementing an undo and redo.
	 * 
	 * @param modelId
	 * @param model
	 * @param appliedChanges
	 * @param metadata
	 */
	protected void addToHistory(String modelId, LegoModelGenerator model, 
			List<OWLOntologyChange> appliedChanges, METADATA metadata) {
		// do nothing, for now
	}
	
	/**
	 * This method will check the given model and update the import declarations.
	 * It will add missing IRIs and remove obsolete ones.
	 * 
	 * @param modelId 
	 * @param model
	 * @see #additionalImports
	 * @see #addImports(Iterable)
	 * @see #obsoleteImports
	 * @see #addObsoleteImports(Iterable)
	 */
	public void updateImports(String modelId, LegoModelGenerator model) {
		updateImports(model);
	}
	
	private void updateImports(final LegoModelGenerator model) {
		final OWLOntology aboxOntology = model.getAboxOntology();
		List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
		
		Set<IRI> missingImports = new HashSet<IRI>(additionalImports);
		Set<OWLImportsDeclaration> importsDeclarations = aboxOntology.getImportsDeclarations();
		for (OWLImportsDeclaration decl : importsDeclarations) {
			IRI iri = decl.getIRI();
			if (obsoleteImports.contains(iri)) {
				changes.add(new RemoveImport(aboxOntology, decl));
			}
			else {
				missingImports.remove(iri);
			}
		}
		final OWLOntologyManager m = aboxOntology.getOWLOntologyManager();
		if (!missingImports.isEmpty()) {
			OWLDataFactory f = m.getOWLDataFactory();
			for(IRI missingImport : missingImports) {
				OWLImportsDeclaration decl = f.getOWLImportsDeclaration(missingImport);
				changes.add(new AddImport(aboxOntology, decl));
			}
		}
		
		if (!changes.isEmpty()) {
			m.applyChanges(changes);
		}
	}
	
}
