package owltools.gaf.lego;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxOntologyFormat;
import org.geneontology.lego.dot.LegoDotWriter;
import org.geneontology.lego.dot.LegoRenderer;
import org.geneontology.lego.model.LegoTools.UnExpectedStructureException;
import org.obolibrary.obo2owl.Owl2Obo;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLAxiomChange;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.RemoveAxiom;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.gaf.GafDocument;
import owltools.gaf.GafObjectsBuilder;
import owltools.gaf.lego.MolecularModelManager.OWLOperationResponse;
import owltools.graph.OWLGraphWrapper;
import owltools.vocab.OBOUpperVocabulary;

/**
 * Manager object for operations on collections of MolecularModels (aka lego diagrams)
 * 
 * any number of models can be loaded at any time (todo - impose some limit to avoid
 * using too much memory)
 * 
 * each model has a generator, an OWLOntology (containing the set of class assertions)
 * and a reasoner associated with it (todo - test memory requirements)
 * 
 * This manager is designed to be used within a web server. Multiple clients can
 * contact the same manager instance through services
 *
 */
public class MolecularModelManager {
	
	private static Logger LOG = Logger.getLogger(MolecularModelManager.class);


	OWLGraphWrapper graph;
	boolean isPrecomputePropertyClassCombinations;
	Map<String, GafDocument> dbToGafdoc = new HashMap<String, GafDocument>();
	Map<String, LegoModelGenerator> modelMap = new HashMap<String, LegoModelGenerator>();
	String pathToGafs = "gene-associations";
	String pathToOWLFiles = "owl-models";
	GafObjectsBuilder builder = new GafObjectsBuilder();
	OWLOntologyFormat ontologyFormat = new ManchesterOWLSyntaxOntologyFormat();

	// TODO: Temporarily for keeping instances unique (search for "unique" below).
	String uniqueTop = Long.toHexString((System.currentTimeMillis()/1000));
	long instanceCounter = 0;

	/**
	 * Represents the reponse to a requested translation on an
	 * ontology/model
	 * 
	 */
	public class OWLOperationResponse {
		OWLAxiomChange change;
		int changeId;
		boolean isSuccess = true;
		boolean isResultsInInconsistency = false;
		List<Map<Object, Object>> modelData = null;
		List<String> individualIds = null;
		List<OWLNamedIndividual> individuals = null;
		
		/**
		 * @param isSuccess
		 */
		public OWLOperationResponse(boolean isSuccess) {
			super();
			this.isSuccess = isSuccess;
		}
		
		/**
		 * @param isSuccess
		 * @param isResultsInInconsistency
		 */
		public OWLOperationResponse(boolean isSuccess,
				boolean isResultsInInconsistency) {
			super();
			this.isSuccess = isSuccess;
			this.isResultsInInconsistency = isResultsInInconsistency;
		}
		
		public OWLOperationResponse(OWLAxiomChange change, boolean isSuccess,
				boolean isResultsInInconsistency) {
			super();
			this.isSuccess = isSuccess;
			this.isResultsInInconsistency = isResultsInInconsistency;
			this.change = change;
		}
		
		/**
		 * @return the isSuccess
		 */
		public boolean isSuccess() {
			return isSuccess;
		}
		
		/**
		 * @return the isResultsInInconsistency
		 */
		public boolean isResultsInInconsistency() {
			return isResultsInInconsistency;
		}
		
		/**
		 * @return the modelData
		 */
		public List<Map<Object, Object>> getModelData() {
			return modelData;
		}
		
		/**
		 * @param modelData the modelData to set
		 */
		public void setModelData(List<Map<Object, Object>> modelData) {
			this.modelData = modelData;
		}
		
		/**
		 * @return the individualIds
		 */
		public List<String> getIndividualIds() {
			return individualIds;
		}
		
		/**
		 * @param individualIds the individualIds to set
		 */
		public void setIndividualIds(List<String> individualIds) {
			this.individualIds = individualIds;
		}

		/**
		 * @return the individuals
		 */
		public List<OWLNamedIndividual> getIndividuals() {
			return individuals;
		}

		/**
		 * @param individuals the individuals to set
		 */
		public void setIndividuals(List<OWLNamedIndividual> individuals) {
			this.individuals = individuals;
		}
		
		
//		/**
//		 * Combine two responses together.
//		 * 
//		 * @param response
//		 */
//		public void mergeIn(OWLOperationResponse toMerge) {
//
//			// success and inconsistency dominate false and true respectively.
//			if( ! toMerge.isSuccess ) this.isSuccess = false;
//			if( ! toMerge.isResultsInInconsistency() ) this.isResultsInInconsistency = true;
//			
//			// Merge model data.
//			Map<Object, Object> newMap = new HashMap<Object,Object>();
//			List<Map<Object, Object>> toLoop = this.getModelData(); // add this response
//			toLoop.addAll(toMerge.getModelData()); // add merging response
//			for( Map<Object, Object>mergable : toLoop){
//				// If new, add to new.
//				if( mergable.containsKey("id") ){
//					
//				}
//			}
//			
//			// Merge ID list via Set (no dupes).
//		    Set<String> set = new HashSet<String>();
//		    set.addAll(this.getIndividualIds());
//		    set.addAll(toMerge.getIndividualIds());
//			this.individualIds = new ArrayList<String>(set);
//		}
		
	}

	public static class UnknownIdentifierException extends Exception {

		// generated
		private static final long serialVersionUID = -847970910712518838L;

		/**
		 * @param message
		 * @param cause
		 */
		public UnknownIdentifierException(String message, Throwable cause) {
			super(message, cause);
		}

		/**
		 * @param message
		 */
		public UnknownIdentifierException(String message) {
			super(message);
		}

	}
	
	/**
	 * @param graph
	 * @throws OWLOntologyCreationException
	 */
	public MolecularModelManager(OWLGraphWrapper graph) throws OWLOntologyCreationException {
		super();
		this.graph = graph;
		init();
	}
	/**
	 * @param ont
	 * @throws OWLOntologyCreationException
	 */
	public MolecularModelManager(OWLOntology ont) throws OWLOntologyCreationException {
		super();
		this.graph = new OWLGraphWrapper(ont);
		init();
	}

	protected void init() throws OWLOntologyCreationException {
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
	 * @return path to gafs direcory
	 */
	public String getPathToGafs() {
		return pathToGafs;
	}
	/**
	 * Can either be an HTTP prefix, or an absolute file path
	 * 
	 * @param pathToGafs
	 */
	public void setPathToGafs(String pathToGafs) {
		this.pathToGafs = pathToGafs;
	}
	
	
	
	/**
	 * Note this may move to an implementation-specific subclass in future
	 * 
	 * @return path to owl on server
	 */
	public String getPathToOWLFiles() {
		return pathToOWLFiles;
	}
	/**
	 * @param pathToOWLFiles
	 */
	public void setPathToOWLFiles(String pathToOWLFiles) {
		this.pathToOWLFiles = pathToOWLFiles;
	}
	/**
	 * loads/register a Gaf document
	 * 
	 * @param db
	 * @return Gaf document
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public GafDocument loadGaf(String db) throws IOException, URISyntaxException {
		if (!dbToGafdoc.containsKey(db)) {

			GafDocument gafdoc = builder.buildDocument(pathToGafs + "/gene_association." + db + ".gz");
			dbToGafdoc.put(db, gafdoc);
		}
		return dbToGafdoc.get(db);
	}

	/**
	 * Loads and caches a GAF document from a specified location
	 * 
	 * @param db
	 * @param gafFile
	 * @return Gaf document
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public GafDocument loadGaf(String db, File gafFile) throws IOException, URISyntaxException {
		if (!dbToGafdoc.containsKey(db)) {

			GafDocument gafdoc = builder.buildDocument(gafFile);
			dbToGafdoc.put(db, gafdoc);
		}
		return dbToGafdoc.get(db);
	}


	/**
	 * @param db
	 * @return Gaf document for db
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public GafDocument getGaf(String db) throws IOException, URISyntaxException {
		return loadGaf(db);
	}


	/**
	 * Generates a new model taking as input a biological process P and a database D.
	 * The axioms from P and annotations to P from D are used to seed a new model
	 * 
	 * See {@link LegoModelGenerator#buildNetwork(OWLClass, java.util.Collection)}
	 * And also https://docs.google.com/document/d/1TV8Eb9sSvFY-weVZaIfzCxF1qbnmkUaiUhTm9Bs3gRE/edit
	 * 
	 * Note the resulting model is uniquely identified by the modeId, which is currently constructed
	 * as a concatenation of the db and the P id. This means that if there is an existing model by
	 * this ID it will be overwritten
	 * 
	 * @param processCls
	 * @param db
	 * @return modelId
	 * @throws OWLOntologyCreationException
	 * @throws URISyntaxException 
	 * @throws IOException 
	 */
	public String generateModel(OWLClass processCls, String db) throws OWLOntologyCreationException, IOException, URISyntaxException {

		LegoModelGenerator molecularModelGenerator = new LegoModelGenerator(graph.getSourceOntology(), new ElkReasonerFactory());

		molecularModelGenerator.setPrecomputePropertyClassCombinations(isPrecomputePropertyClassCombinations);
		GafDocument gafdoc = getGaf(db);
		molecularModelGenerator.initialize(gafdoc, graph);

		Set<String> seedGenes = new HashSet<String>();
		String p = graph.getIdentifier(processCls);
		seedGenes.addAll(molecularModelGenerator.getGenes(processCls));
		molecularModelGenerator.setContextualizingSuffix(db);
		molecularModelGenerator.buildNetwork(p, seedGenes);

		//OWLOntology model = molecularModelGenerator.getAboxOntology();
		String modelId = getModelId(p, db);
		if (modelMap.containsKey(modelId)) {
			throw new OWLOntologyCreationException("A model already exists for this process and db: "+modelId);
		}
		modelMap.put(modelId, molecularModelGenerator);
		return modelId;

	}
	
	/**
	 * wrapper for {@link #generateModel(OWLClass, String)}
	 * 
	 * @param pid
	 * @param db
	 * @return modelId
	 * @throws OWLOntologyCreationException
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws UnknownIdentifierException
	 */
	public String generateModel(String pid, String db) throws OWLOntologyCreationException, IOException, URISyntaxException, UnknownIdentifierException {
		OWLClass cls = getClass(pid);
		if (cls == null) {
			throw new UnknownIdentifierException("Could not find a class for id: "+pid);
		}
		return generateModel(cls, db);
	}

	/**
	 * Adds a process individual (and inferred individuals) to a model
	 * 
	 * @param modelId
	 * @param processCls
	 * @return null TODO
	 * @throws OWLOntologyCreationException
	 */
	public String addProcess(String modelId, OWLClass processCls) throws OWLOntologyCreationException {
		LegoModelGenerator mod = getModel(modelId);
		Set<String> genes = new HashSet<String>();
		mod.buildNetwork(processCls, genes);
		return null;
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
	 * Only use for testing.
	 * 
	 * @param modelId
	 * @return List of key-val pairs ready for Gson
	 */
	List<Map<Object, Object>> getIndividualObjects(String modelId) {
		LegoModelGenerator mod = getModel(modelId);
		MolecularModelJsonRenderer renderer = new MolecularModelJsonRenderer(graph);
		OWLOntology ont = mod.getAboxOntology();
		List<Map<Object, Object>> objs = new ArrayList<Map<Object, Object>>();
		for (OWLNamedIndividual i : ont.getIndividualsInSignature()) {
			objs.add(renderer.renderObject(ont, i));
		}
		return objs;
	}
	
	/**
	 * @param modelId
	 * @return Map object ready for Gson
	 */
	public Map<Object, Object> getModelObject(String modelId) {
		LegoModelGenerator mod = getModel(modelId);
		MolecularModelJsonRenderer renderer = new MolecularModelJsonRenderer(graph);
		return renderer.renderObject(mod.getAboxOntology());
	}

	/**
	 * Given an instance, generate the most specific class instance that classifies
	 * this instance, and add this as a class to the model ontology
	 * 
	 * @param modelId
	 * @param individualId
	 * @param newClassId
	 * @return newClassId
	 */
	public String createMostSpecificClass(String modelId, String individualId, String newClassId) {
		LegoModelGenerator mod = getModel(modelId);
		OWLIndividual ind = getIndividual(modelId, individualId);
		OWLClassExpression msce = mod.getMostSpecificClassExpression((OWLNamedIndividual) ind);
		OWLClass c = this.getClass(newClassId);
		addAxiom(modelId, getOWLDataFactory(modelId).getOWLEquivalentClassesAxiom(msce, c));
		return newClassId;
	}

	/**
	 * TODO - autogenerate a label?
	 * TODO - finalize identifier policy. Currently concatenates model and class IDs
	 * 
	 * @param modelId
	 * @param c
	 * @return id of created individual
	 */
	public OWLOperationResponse createIndividual(String modelId, OWLClass c) {
		LOG.info("Creating individual of type: "+c);
		String cid = graph.getIdentifier(c).replaceAll(":","-"); // e.g. GO-0123456

		// Make something unique to tag onto the generated IDs.
		instanceCounter++;
		String unique = uniqueTop + String.format("%07d", instanceCounter);
		String iid = modelId+"-"+ cid + "-" + unique; // TODO - unique-ify in a way that makes Chris happy
		LOG.info("  new OD: "+iid);

		IRI iri = MolecularModelJsonRenderer.getIRI(iid, graph);
		OWLNamedIndividual i = getOWLDataFactory(modelId).getOWLNamedIndividual(iri);
		addAxiom(modelId, getOWLDataFactory(modelId).getOWLDeclarationAxiom(i));
		OWLOperationResponse resp = addType(modelId, i, c);
		resp.setIndividuals(Collections.singletonList(i));
		return resp;
	}
	
	/**
	 * Shortcut for {@link #createIndividual(String, OWLClass)}
	 * 
	 * @param modelId
	 * @param cid
	 * @return id of created individual
	 * @throws UnknownIdentifierException 
	 */
	public OWLOperationResponse createIndividual(String modelId, String cid) throws UnknownIdentifierException {
		OWLClass cls = getClass(cid);
		if (cls == null) {
			throw new UnknownIdentifierException("Could not find a class for id: "+cid);
		}
		return createIndividual(modelId, cls);
	}

	/**
	 * Get the individual information for return.
	 * 
	 * @param modelId
	 * @param iid
	 * @return response concerning individual data
	 */
	public OWLOperationResponse getIndividualById(String modelId, String iid) {
		IRI iri = MolecularModelJsonRenderer.getIRI(iid, graph);
		OWLNamedIndividual i = getOWLDataFactory(modelId).getOWLNamedIndividual(iri);
		//LegoModelGenerator model = getModel(modelId);
		OWLOperationResponse resp = new OWLOperationResponse(true, false); // no op, so this
		addIndividualsData(resp, getModel(modelId), i);
		return resp;
	}
	
	/**
	 * Deletes an individual
	 * 
	 * @param modelId
	 * @param iid
	 * @return response into
	 */
	public OWLOperationResponse deleteIndividual(String modelId,String iid) {
		OWLNamedIndividual i = (OWLNamedIndividual) getIndividual(modelId, iid);
		removeAxiom(modelId, getOWLDataFactory(modelId).getOWLDeclarationAxiom(i));
		LegoModelGenerator m = getModel(modelId);
		OWLOntology ont = m.getAboxOntology();
		for (OWLAxiom ax : ont.getAxioms(i)) {
			removeAxiom(modelId, ax);
		}
		return new OWLOperationResponse(true);
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
				// TODO Auto-generated catch block
				e.printStackTrace();
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
	 * @param id
	 */
	public void deleteModel(String id) {
		// TODO - retrieve from persistent store
		modelMap.remove(id);
	}
	
	/**
	 * @return ids for all loaded models
	 */
	public Set<String> getModelIds() {
		return modelMap.keySet();
	}

	/**
	 * TODO - locking
	 * 
	 * @param modelId 
	 * @throws OWLOntologyStorageException 
	 * @throws OWLOntologyCreationException 
	 */
	public void saveModel(String modelId) throws OWLOntologyStorageException, OWLOntologyCreationException {
		// TODO - delegate to a bridge object, allow different impls (triplestore, filesystem, etc)
		LegoModelGenerator m = getModel(modelId);
		OWLOntology ont = m.getAboxOntology();
		String file = getPathToModelOWL(modelId);
		getOWLOntologyManager(modelId).saveOntology(ont, ontologyFormat, IRI.create(new File(file)));
	}
	
	/**
	 * @throws OWLOntologyStorageException
	 * @throws OWLOntologyCreationException
	 */
	public void saveAllModels() throws OWLOntologyStorageException, OWLOntologyCreationException {
		for (String modelId : modelMap.keySet()) {
			saveModel(modelId);
		}
	}
	
	/**
	 * Retrieve a collection of all file/stored model ids.<br>
	 * Note: Models may not be loaded at this point.
	 * 
	 * @return set of modelids.
	 * @throws IOException
	 */
	public Set<String> getStoredModelIds() throws IOException {
		Set<String> allModelIds = new HashSet<String>();
		// look for all owl file in the model folder
		File modelFolder = new File(pathToOWLFiles);
		File[] modelFiles = modelFolder.listFiles(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				if (name.endsWith(".owl")) {
					return true;
				}
				return false;
			}
		});
		for (File modelFile : modelFiles) {
			String modelFileName = modelFile.getName();
			String modelId = FilenameUtils.removeExtension(modelFileName);
			allModelIds.add(modelId);
		}
		return allModelIds;
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

	/**
	 * Retrieve a collection of all available model ids.<br>
	 * Note: Models may not be loaded at this point.
	 * 
	 * @return set of modelids.
	 * @throws IOException
	 */
	public Set<String> getAvailableModelIds() throws IOException {
		Set<String> allModelIds = new HashSet<String>();
		allModelIds.addAll(this.getStoredModelIds());
		allModelIds.addAll(this.getCurrentModelIds());
		return allModelIds;
	}
	
	// TODO - ensure load/save are synchronized
	protected void loadModel(String modelId, boolean isOverride) throws OWLOntologyCreationException {
		if (modelMap.containsKey(modelId)) {
			if (!isOverride) {
				throw new OWLOntologyCreationException("Model already esxists: "+modelId);
			}
		}
		String file = getPathToModelOWL(modelId);
		
		OWLOntology ont = graph.getManager().loadOntologyFromOntologyDocument(IRI.create(new File(file)));
		LegoModelGenerator m = new LegoModelGenerator(graph.getSourceOntology());
		m.setAboxOntology(ont);
		modelMap.put(modelId, m);
	}

	
	private String getPathToModelOWL(String modelId) {
		return pathToOWLFiles + "/" + modelId + ".owl";
	}
	private OWLIndividual getIndividual(String modelId, String indId) {
		IRI iri = MolecularModelJsonRenderer.getIRI(indId, graph);
		return getOWLDataFactory(modelId).getOWLNamedIndividual(iri);
	}
	private OWLClass getClass(String cid) {
		IRI iri = MolecularModelJsonRenderer.getIRI(cid, graph);
		return graph.getOWLClass(iri);
	}
	private OWLClass getGeneClass(String modelId, String cid) {
		IRI iri = MolecularModelJsonRenderer.getIRI(cid, graph);
		return getOWLDataFactory(modelId).getOWLClass(iri);
	}
	private OWLObjectProperty getObjectProperty(String pid) {
		IRI iri = MolecularModelJsonRenderer.getIRI(pid, graph);
		return graph.getOWLObjectProperty(iri);
	}

	private OWLObjectPropertyExpression getObjectProperty(
			OBOUpperVocabulary vocabElement) {
		return vocabElement.getObjectProperty(getOntology());
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
	 * @return response info
	 */
	public OWLOperationResponse addType(String modelId,
			OWLIndividual i, OWLClass c) {
		OWLClassAssertionAxiom axiom = getOWLDataFactory(modelId).getOWLClassAssertionAxiom(c,i);
		OWLOperationResponse response = addAxiom(modelId, axiom);
		addIndividualsData(response, getModel(modelId), i);
		return response;
	}
	
	private void addIndividualsData(OWLOperationResponse resp, LegoModelGenerator mod, OWLIndividual...individuals) {
		MolecularModelJsonRenderer renderer = new MolecularModelJsonRenderer(graph);
		OWLOntology ont = mod.getAboxOntology();
		List<Map<Object, Object>> objs = new ArrayList<Map<Object, Object>>();
		List<String> individualIds = new ArrayList<String>();
		for (OWLIndividual i : individuals) {
			if (i instanceof OWLNamedIndividual) {
				OWLNamedIndividual named = (OWLNamedIndividual)i;
				objs.add(renderer.renderObject(ont, named));
				individualIds.add(Owl2Obo.getIdentifier(named.getIRI()));
			}
		}
		resp.setModelData(objs);
		resp.setIndividualIds(individualIds);
	}

	/**
	 * Convenience wrapper for {@link #addType(String, OWLIndividual, OWLClass)}
	 * 
	 * @param modelId
	 * @param iid
	 * @param cid
	 * @return response info
	 * @throws UnknownIdentifierException 
	 */
	public OWLOperationResponse addType(String modelId,
			String iid, String cid) throws UnknownIdentifierException {
		OWLIndividual individual = getIndividual(modelId, iid);
		if (individual == null) {
			throw new UnknownIdentifierException("Could not find a individual for id: "+iid);
		}
		OWLClass cls = getClass(cid);
		if (cls == null) {
			throw new UnknownIdentifierException("Could not find a class for id: "+cid);
		}
		return addType(modelId, individual, cls);
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
	 * @return response info
	 */
	public OWLOperationResponse addType(String modelId,
			OWLIndividual i, 
			OWLObjectPropertyExpression p,
			OWLClassExpression filler) {
		LOG.info("Adding "+i+ " type "+p+" some "+filler);
		OWLObjectSomeValuesFrom c = getOWLDataFactory(modelId).getOWLObjectSomeValuesFrom(p, filler);
		OWLClassAssertionAxiom axiom = 
				getOWLDataFactory(modelId).getOWLClassAssertionAxiom(
						c,
						i);
		OWLOperationResponse resp = addAxiom(modelId, axiom);
		addIndividualsData(resp, getModel(modelId), i);
		return resp;
	}
	
	/**
	 * Convenience wrapper for
	 *  {@link #addType(String, OWLIndividual, OWLObjectPropertyExpression, OWLClassExpression)}
	 * 
	 * @param modelId
	 * @param iid
	 * @param pid
	 * @param cid
	 * @return response info
	 * @throws UnknownIdentifierException 
	 */
	public OWLOperationResponse addType(String modelId,
			String iid, String pid, String cid) throws UnknownIdentifierException {
		OWLIndividual individual = getIndividual(modelId, iid);
		if (individual == null) {
			throw new UnknownIdentifierException("Could not find a individual for id: "+iid);
		}
		OWLObjectProperty property = getObjectProperty(pid);
		if (property == null) {
			throw new UnknownIdentifierException("Could not find a property for id: "+pid);
		}
		OWLClass cls = getClass(cid);
		if (cls == null) {
			throw new UnknownIdentifierException("Could not find a class for id: "+cid);
		}
		return addType(modelId, individual, property, cls);
	}

	/**
	 * Adds ClassAssertion(c,i) to specified model
	 * 
	 * @param modelId
	 * @param i
	 * @param c
	 * @return response info
	 */
	public OWLOperationResponse removeType(String modelId,
			OWLIndividual i, OWLClass c) {
		OWLClassAssertionAxiom axiom = getOWLDataFactory(modelId).getOWLClassAssertionAxiom(c,i);
		OWLOperationResponse resp = addAxiom(modelId, axiom);
		addIndividualsData(resp, getModel(modelId), i);
		return resp;
	}

	/**
	 * Convenience wrapper for {@link #removeType(String, OWLIndividual, OWLClass)}
	 * 
	 * @param modelId
	 * @param iid
	 * @param cid
	 * @return response info
	 */
	public OWLOperationResponse removeType(String modelId,
			String iid, String cid) {
		return removeType(modelId, getIndividual(modelId, iid), getClass(cid));
	}
	
	/**
	 * Removes a ClassAssertion, where the class expression instantiated is an
	 * ObjectSomeValuesFrom expression
	 * 
	 * TODO - in fuure it should be possible to remove multiple assertions by leaving some fields null
	 * 
	 * @param modelId
	 * @param i
	 * @param p
	 * @param filler
	 * @return response info
	 */
	public OWLOperationResponse removeType(String modelId,
			OWLIndividual i, 
			OWLObjectPropertyExpression p,
			OWLClassExpression filler) {
		OWLClassAssertionAxiom axiom = 
				getOWLDataFactory(modelId).getOWLClassAssertionAxiom(
						getOWLDataFactory(modelId).getOWLObjectSomeValuesFrom(p, filler),
						i);
		OWLOperationResponse resp = removeAxiom(modelId, axiom);
		addIndividualsData(resp, getModel(modelId), i);
		return resp;
	}
	
	
	// TODO
//	public OWLOperationResponse removeTypes(String modelId,
//			OWLIndividual i, 
//			OWLObjectPropertyExpression p) {
//		return removeType(modelId, i, p, null);
//	}


	

	/**
	 * Convenience wrapper for {@link #addOccursIn(String, OWLIndividual, OWLClassExpression)}
	 * 
	 * @param modelId
	 * @param iid
	 * @param eid - e.g. PR:P12345
	 * @return response info
	 */
	public OWLOperationResponse addOccursIn(String modelId,
			String iid, String eid) {
		return addOccursIn(modelId, getIndividual(modelId, iid), getClass(eid));
	}

	/**
	 * Adds a ClassAssertion to the model, connecting an activity instance to the class of molecule
	 * that enables the activity.
	 * 
	 * Example: FGFR receptor activity occursIn some UniProtKB:FGF
	 * 
	 * The reasoner may detect an inconsistency under different scenarios:
	 *  - i may be an instance of a class that is disjoint with a bfo process
	 *  - the enabled may be an instance of a class that is disjoint with molecular entity
	 *  
	 *  Under these circumstances, no error is thrown, but the response code indicates that no operation
	 *  was performed on the kb, and the response object indicates the operation caused an inconsistency
	 * 
	 * @param modelId
	 * @param i
	 * @param enabler
	 * @return response info
	 */
	public OWLOperationResponse addOccursIn(String modelId,
			OWLIndividual i, 
			OWLClassExpression enabler) {
		return addType(modelId, i, OBOUpperVocabulary.BFO_occurs_in.getObjectProperty(getOntology()), enabler);
	}	

	/**
	 * Convenience wrapper for {@link #addEnabledBy(String, OWLIndividual, OWLClassExpression)}
	 * 
	 * @param modelId
	 * @param iid
	 * @param eid - e.g. PR:P12345
	 * @return response info
	 * @throws UnknownIdentifierException 
	 */
	public OWLOperationResponse addEnabledBy(String modelId,
			String iid, String eid) throws UnknownIdentifierException {
		OWLIndividual individual = getIndividual(modelId, iid);
		if (individual == null) {
			throw new UnknownIdentifierException("Could not find a individual for id: "+iid);
		}
		OWLClass cls = getClass(eid);
		if (cls == null) {
			throw new UnknownIdentifierException("Could not find a class for id: "+eid);
		}
		return addEnabledBy(modelId, individual, cls);
	}

	/**
	 * Adds a ClassAssertion to the model, connecting an activity instance to the class of molecule
	 * that enables the activity.
	 * 
	 * Example: FGFR receptor activity enabledBy some UniProtKB:FGF
	 * 
	 * The reasoner may detect an inconsistency under different scenarios:
	 *  - i may be an instance of a class that is disjoint with a bfo process
	 *  - the enabled may be an instance of a class that is disjoint with molecular entity
	 *  
	 *  Under these circumstances, no error is thrown, but the response code indicates that no operation
	 *  was performed on the kb, and the response object indicates the operation caused an inconsistency
	 * 
	 * @param modelId
	 * @param i
	 * @param enabler
	 * @return response info
	 */
	public OWLOperationResponse addEnabledBy(String modelId,
			OWLIndividual i, 
			OWLClassExpression enabler) {
		return addType(modelId, i, OBOUpperVocabulary.GOREL_enabled_by.getObjectProperty(getOntology()), enabler);
	}	


	/**
	 * Adds triple (i,p,j) to specified model
	 * 
	 * @param modelId
	 * @param p
	 * @param i
	 * @param j
	 * @return response info
	 */
	public OWLOperationResponse addFact(String modelId, OWLObjectPropertyExpression p,
			OWLIndividual i, OWLIndividual j) {
		OWLObjectPropertyAssertionAxiom axiom = getOWLDataFactory(modelId).getOWLObjectPropertyAssertionAxiom(p, i, j);
		OWLOperationResponse response = addAxiom(modelId, axiom);
		addIndividualsData(response, getModel(modelId), i, j);
		return response;
	}

	/**
	 * Convenience wrapper for {@link #addFact(String, OWLObjectPropertyExpression, OWLIndividual, OWLIndividual)}
	 *	
	 * @param modelId
	 * @param vocabElement
	 * @param i
	 * @param j
	 * @return response info
	 */
	public OWLOperationResponse addFact(String modelId, OBOUpperVocabulary vocabElement,
			OWLIndividual i, OWLIndividual j) {
		OWLObjectProperty p = vocabElement.getObjectProperty(getOntology());
		return addFact(modelId, p, i, j);
	}

	/**
	 * Convenience wrapper for {@link #addFact(String, OWLObjectPropertyExpression, OWLIndividual, OWLIndividual)}
	 * 
	 * @param modelId
	 * @param pid
	 * @param iid
	 * @param jid
	 * @return response info
	 * @throws UnknownIdentifierException 
	 */
	public OWLOperationResponse addFact(String modelId, String pid,
			String iid, String jid) throws UnknownIdentifierException {
		OWLObjectProperty property = getObjectProperty(pid);
		if (property == null) {
			throw new UnknownIdentifierException("Could not find a property for id: "+pid);
		}
		OWLIndividual individual1 = getIndividual(modelId, iid);
		if (individual1 == null) {
			throw new UnknownIdentifierException("Could not find a individual (1) for id: "+iid);
		}
		OWLIndividual individual2 = getIndividual(modelId, jid);
		if (individual2 == null) {
			throw new UnknownIdentifierException("Could not find a individual (2) for id: "+jid);
		}
		return addFact(modelId, property, individual1, individual2);
	}

	/**
	 * Convenience wrapper for {@link #addFact(String, OWLObjectPropertyExpression, OWLIndividual, OWLIndividual)}
	 * 
	 * @param modelId
	 * @param vocabElement
	 * @param iid
	 * @param jid
	 * @return response info
	 * @throws UnknownIdentifierException
	 */
	public OWLOperationResponse addFact(String modelId, OBOUpperVocabulary vocabElement,
			String iid, String jid) throws UnknownIdentifierException {
		OWLObjectPropertyExpression property = getObjectProperty(vocabElement);
		if (property == null) {
			throw new UnknownIdentifierException("Could not find a individual for id: "+vocabElement);
		}
		OWLIndividual individual1 = getIndividual(modelId, iid);
		if (individual1 == null) {
			throw new UnknownIdentifierException("Could not find a individual for id: "+iid);
		}
		OWLIndividual individual2 = getIndividual(modelId, jid);
		if (individual2 == null) {
			throw new UnknownIdentifierException("Could not find a individual for id: "+jid);
		}
		return addFact(modelId, property, individual1, individual2);
	}
	
	/**
	 * @param modelId
	 * @param p
	 * @param i
	 * @param j
	 * @return response info
	 */
	public OWLOperationResponse removeFact(String modelId, OWLObjectPropertyExpression p,
			OWLIndividual i, OWLIndividual j) {
		OWLObjectPropertyAssertionAxiom axiom = getOWLDataFactory(modelId).getOWLObjectPropertyAssertionAxiom(p, i, j);
		OWLOperationResponse resp = removeAxiom(modelId, axiom);
		addIndividualsData(resp, getModel(modelId), i, j);
		return resp;
	}

	/**
	 * @param modelId
	 * @param pid
	 * @param iid
	 * @param jid
	 * @return response info
	 * @throws UnknownIdentifierException 
	 */
	public OWLOperationResponse removeFact(String modelId, String pid,
			String iid, String jid) throws UnknownIdentifierException {
		OWLObjectProperty property = getObjectProperty(pid);
		if (property == null) {
			throw new UnknownIdentifierException("Could not find a individual for id: "+pid);
		}
		OWLIndividual individual1 = getIndividual(modelId, iid);
		if (individual1 == null) {
			throw new UnknownIdentifierException("Could not find a individual for id: "+iid);
		}
		OWLIndividual individual2 = getIndividual(modelId, jid);
		if (individual2 == null) {
			throw new UnknownIdentifierException("Could not find a individual for id: "+jid);
		}
		return removeFact(modelId, property, individual1, individual2);
	}


	
	/**
	 * Convenience wrapper for {@link #addPartOf(String, OWLIndividual, OWLIndividual)}
	 *
	 * @param modelId
	 * @param iid
	 * @param jid
	 * @return response info
	 */
	public OWLOperationResponse addPartOf(String modelId, 
			String iid, String jid) {
		return addPartOf(modelId, getIndividual(modelId, iid), getIndividual(modelId, jid));
	}

	/**
	 * Adds an OWL ObjectPropertyAssertion connecting i to j via part_of
	 * 
	 * Note that the inverse assertion is entailed, but not asserted
	 * 
	 * @param modelId
	 * @param i
	 * @param j
	 * @return response info
	 */
	public OWLOperationResponse addPartOf(String modelId,
			OWLIndividual i, OWLIndividual j) {
		return addFact(modelId, getObjectProperty(OBOUpperVocabulary.BFO_part_of), i, j);
	}



	/**
	 * In general, should not be called directly - use a wrapper method
	 * 
	 * @param modelId
	 * @param axiom
	 * @return response info
	 */
	public OWLOperationResponse addAxiom(String modelId, OWLAxiom axiom) {
		LegoModelGenerator model = getModel(modelId);
		OWLOntology ont = model.getAboxOntology();
		boolean isConsistentAtStart = model.getReasoner().isConsistent();
		AddAxiom change = new AddAxiom(ont, axiom);
		ont.getOWLOntologyManager().applyChange(change);
		// TODO - track axioms to allow redo
		model.getReasoner().flush();
		boolean isConsistentAtEnd = model.getReasoner().isConsistent();
		if (isConsistentAtStart && !isConsistentAtEnd) {
			// rollback
			ont.getOWLOntologyManager().removeAxiom(ont, axiom);
			return new OWLOperationResponse(change, false, true);

		}
		else {
			return new OWLOperationResponse(change, true, false);
		}

	}
	
	/**
	 * In general, should not be called directly - use a wrapper method
	 * 
	 * TODO: an error should be returned if the user attempts to remove
	 * any inferred axiom. For example, if f1 part_of p1, and p1 Type occurs_in some cytosol,
	 * and the user attempts to delete "located in cytosol", the axiom will "come back"
	 * as it is inferred. 
	 * 
	 * @param modelId
	 * @param axiom
	 * @return response info
	 */
	public OWLOperationResponse removeAxiom(String modelId, OWLAxiom axiom) {
		LegoModelGenerator model = getModel(modelId);
		OWLOntology ont = model.getAboxOntology();
		// TODO - check axiom exists
		RemoveAxiom change = new RemoveAxiom(ont, axiom);
		ont.getOWLOntologyManager().applyChange(change);
		// TODO - track axioms to allow redo
		return new OWLOperationResponse(true);
	}	

	public OWLOperationResponse undo(String modelId, String changeId) {
		LOG.error("Not implemented");
		return null;
	}
	
	/**
	 * TODO: decide identifier policy for models
	 * 
	 * @param p
	 * @param db
	 * @return identifier
	 */
	private String getModelId(String p, String db) {
		return "gomodel:"+db + "-"+p.replaceAll(":", "-");
	}
	
	
	@Deprecated
	protected abstract class LegoStringDotRenderer extends LegoDotWriter {
		public LegoStringDotRenderer(OWLGraphWrapper graph, OWLReasoner reasoner) {
			super(graph, reasoner);
			// TODO Auto-generated constructor stub
		}

		public StringBuffer sb = new StringBuffer();
		
	}
	
	/**
	 * For testing purposes - may be obsoleted with rendering moved to client
	 * 
	 * @param modelId
	 * @return dot string
	 * @throws IOException
	 * @throws UnExpectedStructureException
	 */
	@Deprecated
	public String generateDot(String modelId) throws IOException, UnExpectedStructureException {
		LegoModelGenerator m = getModel(modelId);
		Set<OWLNamedIndividual> individuals = getIndividuals(modelId);
	
		LegoStringDotRenderer renderer = 
				new LegoStringDotRenderer(graph, m.getReasoner()) {


			@Override
			protected void open() throws IOException {
				// do nothing
			}

			@Override
			protected void close() {
				// do nothing
			}

			@Override
			protected void appendLine(CharSequence line) throws IOException {
				//System.out.println(line);
				sb.append(line).append('\n');
			}
		};
		renderer.render(individuals, modelId, true);
		return renderer.sb.toString();
	}
	
	/**
	 * @param modelId
	 * @return png File
	 * @throws IOException 
	 * @throws UnExpectedStructureException 
	 * @throws InterruptedException 
	 */
	public File generateImage(String modelId) throws IOException, UnExpectedStructureException, InterruptedException {
		final File dotFile = File.createTempFile("LegoAnnotations", ".dot");
		final File pngFile = File.createTempFile("LegoAnnotations", ".png");

		LegoModelGenerator m = getModel(modelId);
		Set<OWLNamedIndividual> individuals = getIndividuals(modelId);
		OWLReasoner reasoner = m.getReasoner();
		String dotPath = "/opt/local/bin/dot"; // TODO
		try {
			// Step 1: render dot file
			LegoRenderer dotWriter = new LegoDotWriter(graph, reasoner) {
				
				private PrintWriter writer = null;
				
				@Override
				protected void open() throws IOException {
					writer = new PrintWriter(dotFile);
				}
				
				@Override
				protected void appendLine(CharSequence line) throws IOException {
					writer.println(line);
				}

				@Override
				protected void close() {
					IOUtils.closeQuietly(writer);
				}
				
			};
			dotWriter.render(individuals, null, true);
			
			// Step 2: render png file using graphiz (i.e. dot)
			Runtime r = Runtime.getRuntime();

			final String in = dotFile.getAbsolutePath();
			final String out = pngFile.getAbsolutePath();
			
			Process process = r.exec(dotPath + " " + in + " -Tpng -q -o " + out);

			process.waitFor();
			
			return pngFile;
		} finally {
			// delete temp files, do not rely on deleteOnExit
			FileUtils.deleteQuietly(dotFile);
			FileUtils.deleteQuietly(pngFile);
		}
	
	}

	/**
	 * @param ontology
	 * @param output
	 * @param name
	 * @throws Exception
	 */
	@Deprecated
	public void writeLego(OWLOntology ontology, final String output, String name) throws Exception {

		Set<OWLNamedIndividual> individuals = ontology.getIndividualsInSignature(true);


		LegoRenderer renderer = 
				new LegoDotWriter(graph, getModel(name).getReasoner()) {

			BufferedWriter fileWriter = null;

			@Override
			protected void open() throws IOException {
				fileWriter = new BufferedWriter(new FileWriter(new File(output)));
			}

			@Override
			protected void close() {
				IOUtils.closeQuietly(fileWriter);
			}

			@Override
			protected void appendLine(CharSequence line) throws IOException {
				//System.out.println(line);
				fileWriter.append(line).append('\n');
			}
		};
		renderer.render(individuals, name, true);

	}
	
	/**
	 * A simple wrapping function that captures the most basic type of editting.
	 * 
	 * @param modelId
	 * @param classId
	 * @param enabledById
	 * @param occursInId
	 * @return
	 * @throws UnknownIdentifierException
	 */
	public OWLOperationResponse addCompositeIndividual(String modelId, String classId,
			String enabledById, String occursInId) throws UnknownIdentifierException {

		// Create the base individual.
		OWLClass cls = getClass(classId);
		if (cls == null) {
			throw new UnknownIdentifierException("Could not find a class for id: "+classId);
		}
		// Bail out early if it looks like there are any problems.
		OWLOperationResponse resp = createIndividual(modelId, cls);
		if( resp.isSuccess == false || resp.isResultsInInconsistency() ){
			return resp;
		}
		
		// Should just be the one--extract the individual that we created.
		List<OWLNamedIndividual> individuals = resp.getIndividuals();
		if( individuals == null || individuals.isEmpty() ){
			return resp;
		}
		OWLNamedIndividual i = individuals.get(0);
		
		// Optionally, add occurs_in.
		if( occursInId != null ){
			OWLClass occCls = getClass(occursInId);
			if (occCls == null) {
				throw new UnknownIdentifierException("Could not find a class for id: "+occursInId);
			}
			resp = addOccursIn(modelId, i, occCls);
			// Bail out early if it looks like there are any problems.
			if( resp.isSuccess == false || resp.isResultsInInconsistency() ){
				return resp;
			}
		}
		
		// Optionally, add enabled_by.
		if( enabledById != null ){
			OWLClass enbCls = getGeneClass(modelId, enabledById);
			resp = addEnabledBy(modelId, i, enbCls);
			// Bail out early if it looks like there are any problems.
			if( resp.isSuccess == false || resp.isResultsInInconsistency() ){
				return resp;
			}
		}
		
		return resp;
	}
	

}
