package owltools.gaf.lego;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import org.obolibrary.macro.ManchesterSyntaxTool;
import org.semanticweb.owlapi.expression.ParserException;
import org.semanticweb.owlapi.io.OWLFunctionalSyntaxOntologyFormat;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLAxiomChange;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.SetOntologyID;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import owltools.gaf.GafDocument;
import owltools.gaf.GafObjectsBuilder;
import owltools.gaf.eco.EcoMapper;
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
	boolean isPrecomputePropertyClassCombinations = false;
	Map<String, GafDocument> dbToGafdoc = new HashMap<String, GafDocument>();
	Map<String, LegoModelGenerator> modelMap = new HashMap<String, LegoModelGenerator>();
	String pathToGafs = "gene-associations";
	String pathToOWLFiles = "owl-models";
	GafObjectsBuilder builder = new GafObjectsBuilder();
	// WARNING: Do *NOT* switch to functional syntax until the OWL-API has fixed a bug.
	OWLOntologyFormat ontologyFormat = new ManchesterOWLSyntaxOntologyFormat();
	List<IRI> additionalImports;

	// TODO: Temporarily for keeping instances unique (search for "unique" below).
	static String uniqueTop = Long.toHexString((System.currentTimeMillis()/1000));
	static long instanceCounter = 0;
	private static String localUnique(){
		instanceCounter++;
		String unique = uniqueTop + String.format("%07d", instanceCounter);
		return unique;		
	}

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
		Object modelData = null;
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
		public Object getModelData() {
			return modelData;
		}
		
		/**
		 * @param modelData the modelData to set
		 */
		public void setModelData(Object modelData) {
			this.modelData = modelData;
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

	/**
	 * @throws OWLOntologyCreationException
	 */
	protected void init() throws OWLOntologyCreationException {
		// set default imports
		additionalImports = new ArrayList<IRI>();
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
	
	public Collection<IRI> getImports() {
		return Collections.unmodifiableCollection(additionalImports);
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
		// quick check, only generate a model if it does not already exists.
		final String p = graph.getIdentifier(processCls);
		String modelId = getModelId(p, db);
		if (modelMap.containsKey(modelId)) {
			throw new OWLOntologyCreationException("A model already exists for this process and db: "+modelId);
		}
		OWLOntology tbox = graph.getSourceOntology();
		OWLOntology abox;
		
		// create empty ontology
		// use model id as ontology IRI
		OWLOntologyManager m = graph.getManager();
		IRI iri = MolecularModelJsonRenderer.getIRI(modelId, graph);
		abox = m.createOntology(iri);
		
		// setup model ontology to import the source ontology and other imports
		createImports(abox, tbox.getOntologyID());
		
		// create generator
		LegoModelGenerator model = new LegoModelGenerator(tbox, abox);
		
		model.setPrecomputePropertyClassCombinations(isPrecomputePropertyClassCombinations);
		Set<String> seedGenes = new HashSet<String>();
		// only look for genes if a GAF is available
		if (db != null) {
			GafDocument gafdoc = getGaf(db);
			model.initialize(gafdoc, graph);
			seedGenes.addAll(model.getGenes(processCls));
		}
		model.setContextualizingSuffix(db);
		model.buildNetwork(p, seedGenes);

		modelMap.put(modelId, model);
		return modelId;

	}

	private void createImports(OWLOntology ont, OWLOntologyID tboxId) throws OWLOntologyCreationException {
		OWLOntologyManager m = ont.getOWLOntologyManager();
		OWLDataFactory f = m.getOWLDataFactory();
		
		// import T-Box
		OWLImportsDeclaration tBoxImportDeclaration = f.getOWLImportsDeclaration(tboxId.getOntologyIRI());
		m.applyChange(new AddImport(ont, tBoxImportDeclaration));
		
		// import additional ontologies via IRI
		for (IRI importIRI : additionalImports) {
			OWLImportsDeclaration importDeclaration = f.getOWLImportsDeclaration(importIRI);
			m.loadOntology(importIRI);
			m.applyChange(new AddImport(ont, importDeclaration));
		}
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
		OWLClass cls = getClass(pid, graph);
		if (cls == null) {
			throw new UnknownIdentifierException("Could not find a class for id: "+pid);
		}
		return generateModel(cls, db);
	}

	/**
	 * Generates a new model taking as input a database D.
	 * 
	 * Note that the resulting model is uniquely identified by the modeId, which is currently constructed
	 * as a concatenation of the db and a hidden UUID state. This means that in the unlikely case that
	 * there is an existing model by this ID, it will be overwritten,
	 * 
	 * @param db
	 * @return modelId
	 * @throws OWLOntologyCreationException
	 * @throws URISyntaxException 
	 * @throws IOException 
	 */
	public String generateBlankModel(String db) throws OWLOntologyCreationException, IOException, URISyntaxException {

		// Create an arbitrary unique ID and add it to the system.
		String modelId;
		if (db != null) {
			modelId = "gomodel:" + db +"-"+ localUnique(); // TODO: another case of our temporary identifiers.
		}
		else{
			modelId = "gomodel:" + localUnique(); // TODO: another case of our temporary identifiers.
		}
		if (modelMap.containsKey(modelId)) {
			throw new OWLOntologyCreationException("A model already exists for this db: "+modelId);
		}

		// create empty ontology, use model id as ontology IRI
		final OWLOntologyManager m = graph.getManager();
		IRI aBoxIRI = MolecularModelJsonRenderer.getIRI(modelId, graph);
		final OWLOntology tbox = graph.getSourceOntology();
		final OWLOntology abox = m.createOntology(aBoxIRI);

		// add imports to T-Box and additional ontologies via IRI
		createImports(abox, tbox.getOntologyID());
		
		// generate model
		LegoModelGenerator model = new LegoModelGenerator(tbox, abox);
		
		model.setPrecomputePropertyClassCombinations(isPrecomputePropertyClassCombinations);
		if (db != null) {
			GafDocument gafdoc = getGaf(db);
			model.initialize(gafdoc, graph);
			model.setContextualizingSuffix(db);
		}
		
		// add to internal map
		modelMap.put(modelId, model);
		return modelId;
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
	 * @param modelId
	 * @param q
	 * @return all individuals in the model that satisfy q
	 */
	public Set<OWLNamedIndividual> getIndividualsByQuery(String modelId, OWLClassExpression q) {
		LegoModelGenerator mod = getModel(modelId);
		return mod.getReasoner().getInstances(q, false).getFlattened();
	}

	/**
	 * @param modelId
	 * @param qs
	 * @return all individuals in the model that satisfy q
	 */
	public Set<OWLNamedIndividual> getIndividualsByQuery(String modelId, String qs) {
		LegoModelGenerator mod = getModel(modelId);
		ManchesterSyntaxTool mst = new ManchesterSyntaxTool(mod.getAboxOntology(), null, true);
		OWLClassExpression q = mst.parseManchesterExpression(qs);
		return getIndividualsByQuery(modelId, q);
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
		MolecularModelJsonRenderer renderer = new MolecularModelJsonRenderer(mod.getAboxOntology());
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
		OWLIndividual ind = getIndividual(individualId, mod);
		OWLClassExpression msce = mod.getMostSpecificClassExpression((OWLNamedIndividual) ind);
		OWLClass c = getClass(newClassId, mod);
		addAxiom(mod, mod.getOWLDataFactory().getOWLEquivalentClassesAxiom(msce, c), true);
		return newClassId;
	}

	/**
	 * @param modelId
	 * @param c
	 * @return id of created individual
	 */
	public OWLOperationResponse createIndividual(String modelId, OWLClass c) {
		LegoModelGenerator model = getModel(modelId);
		OWLNamedIndividual individual = createIndividual(model, modelId, c, null, true);
		
		return createResponse(true, model, individual);
	}
	
	private OWLOperationResponse createResponse(boolean success, LegoModelGenerator model, OWLNamedIndividual...individuals) {
		OWLOperationResponse resp = new OWLOperationResponse(true, model.getReasoner().isConsistent());
		resp.setIndividuals(Arrays.asList(individuals));
		return resp;
	}
	
	/*
	 * TODO - finalize identifier policy. Currently concatenates model and class IDs
	 */
	private OWLNamedIndividual createIndividual(LegoModelGenerator model, String modelId, OWLClass c, Set<OWLAnnotation> annotations, boolean flushReasoner) {
		LOG.info("Creating individual of type: "+c);
		OWLGraphWrapper graph = new OWLGraphWrapper(model.getAboxOntology());
		String cid = graph.getIdentifier(c).replaceAll(":","-"); // e.g. GO-0123456

		// Make something unique to tag onto the generated IDs.
		String unique = localUnique();
		String iid = modelId +"-"+ cid +"-"+ unique; // TODO - unique-ify in a way that makes Chris happy
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
		
		addAxioms(model, axioms, flushReasoner);
		addType(model, i, c, flushReasoner);
		return i;
	}
	
	/**
	 * Shortcut for {@link #createIndividual(String, OWLClass)}
	 * 
	 * @param modelId
	 * @param cid
	 * @param annotations
	 * @return id of created individual
	 * @throws UnknownIdentifierException 
	 */
	public OWLOperationResponse createIndividual(String modelId, String cid, Collection<Pair<String, String>> annotations) throws UnknownIdentifierException {
		LegoModelGenerator model = getModel(modelId);
		OWLClass cls = getClass(cid, model);
		if (cls == null) {
			throw new UnknownIdentifierException("Could not find a class for id: "+cid);
		}
		Set<OWLAnnotation> owlAnnotations = createAnnotations(annotations, model);
		OWLNamedIndividual individual = createIndividual(model, modelId, cls, owlAnnotations , true);
		return createResponse(true, model, individual);
	}
	
	private Set<OWLAnnotation> createAnnotations(Collection<Pair<String, String>> pairs, LegoModelGenerator model) throws UnknownIdentifierException {
		OWLDataFactory f = model.getOWLDataFactory();
		Set<OWLAnnotation> owlAnnotations = null;
		if (pairs != null && !pairs.isEmpty()) {
			owlAnnotations = new HashSet<OWLAnnotation>();
			for(Pair<String, String> pair : pairs) {
				final LegoAnnotationType type = LegoAnnotationType.getLegoType(pair.getKey());
				if (type == null) {
					throw new UnknownIdentifierException("Could not map: '"+pair.getKey()+"' to a know annotation property");
				}
				OWLAnnotationValue value;
				if (type == LegoAnnotationType.evidence) {
					String v = pair.getValue();
					OWLClass eco = getClass(v, model);
					if (eco == null) {
						throw new UnknownIdentifierException("Could not find a class for id: "+v);
					}
					value = eco.getIRI();
				}
				else {
					value = f.getOWLLiteral(pair.getValue());
				}
				OWLAnnotationProperty property = f.getOWLAnnotationProperty(type.getAnnotationProperty());
				OWLAnnotation annotation = f.getOWLAnnotation(property, value);
				owlAnnotations.add(annotation);
			}
		}
		return owlAnnotations;
	}
	
	static enum LegoAnnotationType {
		
		comment(OWLRDFVocabulary.RDFS_COMMENT.getIRI()), // arbitrary String
		evidence(IRI.create("http://geneontology.org/lego/evidence")), // eco class iri
		date(IRI.create("http://purl.org/dc/elements/1.1/date")), // arbitrary string at the moment, define data format?
		source(IRI.create("http://purl.org/dc/elements/1.1/source")), // arbitrary string, such as PMID:000000
		contributor(IRI.create("http://purl.org/dc/elements/1.1/contributor")); // who contributed to the annotation
		
		private final IRI annotationProperty;
		
		LegoAnnotationType(IRI annotationProperty) {
			this.annotationProperty = annotationProperty;
		}
		
		public IRI getAnnotationProperty() {
			return annotationProperty;
		}
		
		
		static LegoAnnotationType getLegoType(IRI iri) {
			for (LegoAnnotationType type : LegoAnnotationType.values()) {
				if (type.annotationProperty.equals(iri)) {
					return type;
				}
			}
			return null;
		}
		
		static LegoAnnotationType getLegoType(String name) {
			for (LegoAnnotationType type : LegoAnnotationType.values()) {
				if (type.name().equals(name)) {
					return type;
				}
			}
			return null;
		}
	}
	
	/**
	 * Shortcut for {@link #createIndividual(String, OWLClass)}
	 * 
	 * @param modelId
	 * @param cid
	 * @param annotations
	 * @return id of created individual
	 * @throws UnknownIdentifierException 
	 */
	public Pair<String, OWLNamedIndividual> createIndividualNonReasoning(String modelId, String cid, Collection<Pair<String, String>> annotations) throws UnknownIdentifierException {
		LegoModelGenerator model = getModel(modelId);
		OWLClass cls = getClass(cid, model);
		if (cls == null) {
			throw new UnknownIdentifierException("Could not find a class for id: "+cid);
		}
		Set<OWLAnnotation> owlAnnotations = createAnnotations(annotations, model);
		OWLNamedIndividual i = createIndividual(model, modelId, cls, owlAnnotations, false);
		return Pair.of(MolecularModelJsonRenderer.getId(i.getIRI()), i);
	}

	/**
	 * Get the individual information for return.
	 * 
	 * @param modelId
	 * @param iid
	 * @return response concerning individual data
	 * @throws UnknownIdentifierException
	 */
	public OWLOperationResponse getIndividualById(String modelId, String iid) throws UnknownIdentifierException {
		LegoModelGenerator model = getModel(modelId);
		OWLNamedIndividual i = getIndividual(iid, model);
		if (i == null) {
			throw new UnknownIdentifierException("Could not find a individual for id: "+iid);
		}
		OWLOperationResponse resp = new OWLOperationResponse(true, false); // no op, so this
		addIndividualsData(resp, model, i);
		return resp;
	}
	
	public OWLNamedIndividual getNamedIndividual(String modelId, String iid) throws UnknownIdentifierException {
		LegoModelGenerator model = getModel(modelId);
		OWLNamedIndividual i = getIndividual(iid, model);
		if (i == null) {
			throw new UnknownIdentifierException("Could not find a individual for id: "+iid);
		}
		return i;
	}
	
	/**
	 * Deletes an individual
	 * 
	 * @param modelId
	 * @param iid
	 * @return response into
	 * @throws UnknownIdentifierException
	 */
	public OWLOperationResponse deleteIndividual(String modelId, String iid) throws UnknownIdentifierException {
		LegoModelGenerator model = getModel(modelId);
		OWLNamedIndividual i = getIndividual(iid, model);
		if (i == null) {
			throw new UnknownIdentifierException("Could not find a individual for id: "+iid);
		}
		deleteIndividual(model, i, true);
		return createResponse(true, model);
	}
	
	/**
	 * Deletes an individual
	 * 
	 * @param modelId
	 * @param i
	 * @return response into
	 */
	public OWLOperationResponse deleteIndividual(String modelId, OWLNamedIndividual i) {
		LegoModelGenerator model = getModel(modelId);
		deleteIndividual(model, i, true);
		return createResponse(true, model);
	}
	
	/**
	 * Deletes an individual
	 * 
	 * @param model
	 * @param i
	 * @param flushReasoner
	 */
	private void deleteIndividual(LegoModelGenerator model, OWLNamedIndividual i, boolean flushReasoner) {
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
		
		removeAxioms(model, toRemoveAxioms, flushReasoner);
	}
	
	public OWLNamedIndividual addAnnotations(String modelId, String iid, 
			Collection<Pair<String, String>> pairs) throws UnknownIdentifierException {
		LegoModelGenerator model = getModel(modelId);
		OWLNamedIndividual i = getIndividual(iid, model);
		if (i == null) {
			throw new UnknownIdentifierException("Could not find a individual for id: "+iid);
		}
		if (pairs != null) {
			Collection<OWLAnnotation> annotations = createAnnotations(pairs, model);
			addAnnotations(model, i.getIRI(), annotations);
		}
		return i;
	}
	
	public void addAnnotations(String modelId, OWLNamedIndividual i, Collection<OWLAnnotation> annotations) {
		LegoModelGenerator model = getModel(modelId);
		addAnnotations(model, i.getIRI(), annotations);
	}
	
	private void addAnnotations(LegoModelGenerator model, IRI subject, Collection<OWLAnnotation> annotations) {
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		OWLDataFactory f = model.getOWLDataFactory();
		for (OWLAnnotation annotation : annotations) {
			axioms.add(f.getOWLAnnotationAssertionAxiom(subject, annotation));
		}
		addAxioms(model, axioms, false);
	}

	public OWLNamedIndividual removeAnnotations(String modelId, String iid,
			Collection<Pair<String, String>> pairs) throws UnknownIdentifierException {
		LegoModelGenerator model = getModel(modelId);
		OWLNamedIndividual i = getIndividual(iid, model);
		if (i == null) {
			throw new UnknownIdentifierException("Could not find a individual for id: "+iid);
		}
		if (pairs != null) {
			Collection<OWLAnnotation> annotations = createAnnotations(pairs, model);
			removeAnnotations(model, i.getIRI(), annotations);
		}
		return i;
	}
	
	public void removeAnnotations(String modelId, OWLNamedIndividual i, Collection<OWLAnnotation> annotations) {
		LegoModelGenerator model = getModel(modelId);
		removeAnnotations(model, i.getIRI(), annotations);
	}
	
	private void removeAnnotations(LegoModelGenerator model, IRI subject, Collection<OWLAnnotation> annotations) {
		OWLOntology ont = model.getAboxOntology();
		Set<OWLAxiom> toRemove = new HashSet<OWLAxiom>();
		Set<OWLAnnotationAssertionAxiom> candidates = ont.getAnnotationAssertionAxioms(subject);
		for (OWLAnnotationAssertionAxiom axiom : candidates) {
			OWLAnnotation annotation = axiom.getAnnotation();
			if (annotations.contains(annotation)) {
				toRemove.add(axiom);
			}
		}
		removeAxioms(model, toRemove, false);
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
	 * internal method to cleanup this instance
	 */
	public void dispose() {
		Set<String> ids = new HashSet<String>(getModelIds());
		for (String id : ids) {
			unlinkModel(id);
		}
	}

	/**
	 * TODO - locking
	 * 
	 * @param modelId 
	 * @throws OWLOntologyStorageException 
	 * @throws OWLOntologyCreationException 
	 * @throws IOException
	 */
	public void saveModel(String modelId) throws OWLOntologyStorageException, OWLOntologyCreationException, IOException {
		// TODO - delegate to a bridge object, allow different impls (triplestore, filesystem, etc)
		LegoModelGenerator m = getModel(modelId);
		OWLOntology ont = m.getAboxOntology();
		String file = getPathToModelOWL(modelId);
		// prelimiary checks for the target file
		File finalFile = new File(file).getCanonicalFile();
		if (finalFile.exists()) {
			if (finalFile.isFile() == false) {
				throw new IOException("For modelId: '"+modelId+"', the resulting path is not a file: "+finalFile.getAbsolutePath());
			}
			if (finalFile.canWrite() == false) {
				throw new IOException("For modelId: '"+modelId+"', Cannot write to the file: "+finalFile.getAbsolutePath());
			}
		}
		else {
			File targetFolder = finalFile.getParentFile();
			FileUtils.forceMkdir(targetFolder);
		}
		File tempFile = null;
		try {
			// create tempFile
			tempFile = File.createTempFile(modelId, ".owl");
		
			// write to a temp file
			getOWLOntologyManager(modelId).saveOntology(ont, ontologyFormat, IRI.create(tempFile));
			
			// copy temp file to the finalFile
			FileUtils.copyFile(tempFile, finalFile);
		}
		finally {
			// delete temp file
			FileUtils.deleteQuietly(tempFile);
		}
	}
	
	/**
	 * @throws OWLOntologyStorageException
	 * @throws OWLOntologyCreationException
	 * @throws IOException 
	 */
	public void saveAllModels() throws OWLOntologyStorageException, OWLOntologyCreationException, IOException {
		for (String modelId : modelMap.keySet()) {
			saveModel(modelId);
		}
	}
	
	/**
	 * Export the ABox for the given modelId in the default {@link OWLOntologyFormat}.
	 * 
	 * @param modelId
	 * @return modelContent
	 * @throws OWLOntologyStorageException
	 */
	public String exportModel(String modelId) throws OWLOntologyStorageException {
		return exportModel(modelId, ontologyFormat);
	}
	
	/**
	 * Export the ABox for the given modelId in the given ontology format.<br>
	 * Warning: The mapping from String to {@link OWLOntologyFormat} does not map every format!
	 * 
	 * @param modelId
	 * @param format
	 * @return modelContent
	 * @throws OWLOntologyStorageException
	 */
	public String exportModel(String modelId, String format) throws OWLOntologyStorageException {
		OWLOntologyFormat ontologyFormat = getOWLOntologyFormat(format);
		if (ontologyFormat == null) {
			ontologyFormat = this.ontologyFormat;
		}
		return exportModel(modelId, ontologyFormat);
	}
	
	/**
	 * Export the ABox, will try to set the ontologyID to the given modelId (to
	 * ensure import assumptions are met)
	 * 
	 * @param modelId
	 * @param ontologyFormat
	 * @return modelContent
	 * @throws OWLOntologyStorageException
	 */
	public String exportModel(String modelId, OWLOntologyFormat ontologyFormat) throws OWLOntologyStorageException {
		final LegoModelGenerator model = getModel(modelId);
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
	
	private OWLOntologyFormat getOWLOntologyFormat(String fmt) {
		OWLOntologyFormat ofmt = null;
		if (fmt != null) {
			fmt = fmt.toLowerCase();
			if (fmt.equals("rdfxml"))
				ofmt = new RDFXMLOntologyFormat();
			else if (fmt.equals("owl"))
				ofmt = new RDFXMLOntologyFormat();
			else if (fmt.equals("rdf"))
				ofmt = new RDFXMLOntologyFormat();
			else if (fmt.equals("owx"))
				ofmt = new OWLXMLOntologyFormat();
			else if (fmt.equals("owf"))
				ofmt = new OWLFunctionalSyntaxOntologyFormat();
			else if (fmt.equals("owm"))
				ofmt = new ManchesterOWLSyntaxOntologyFormat();
		}
		return ofmt;
	}
	
	/**
	 *  Try to load (or replace) a model with the given ontology. It is expected
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
		addModel(modelId, modelOntology);
		return modelId;
	}
	
	/*
	 * look for all owl files in the give model folder.
	 */
	private Set<String> getOWLFilesFromPath(String pathTo) {
		Set<String> allModelIds = new HashSet<String>();
		File modelFolder = new File(pathTo);
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
	 * Retrieve a collection of all file/stored model ids found in the repo.<br>
	 * Note: Models may not be loaded at this point.
	 * 
	 * @return set of modelids.
	 * @throws IOException
	 */
	public Set<String> getStoredModelIds() throws IOException {
		return getOWLFilesFromPath(this.pathToOWLFiles);
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
				throw new OWLOntologyCreationException("Model already exists: "+modelId);
			}
			unlinkModel(modelId);
		}
		String file = getPathToModelOWL(modelId);
		IRI sourceIRI = IRI.create(new File(file));
		OWLOntology abox = graph.getManager().loadOntologyFromOntologyDocument(sourceIRI);
		addModel(modelId, abox);
	}

	private void addModel(String modelId, OWLOntology abox) throws OWLOntologyCreationException {
		OWLOntology tbox = graph.getSourceOntology();
		LegoModelGenerator m = new LegoModelGenerator(tbox, abox);
		modelMap.put(modelId, m);
	}

	
	private String getPathToModelOWL(String modelId) {
		return pathToOWLFiles + "/" + modelId + ".owl";
	}
	private OWLNamedIndividual getIndividual(String indId, LegoModelGenerator model) {
		OWLGraphWrapper graph = new OWLGraphWrapper(model.getAboxOntology());
		IRI iri = MolecularModelJsonRenderer.getIRI(indId, graph);
		return model.getOWLDataFactory().getOWLNamedIndividual(iri);
	}
	private OWLClass getClass(String cid, LegoModelGenerator model) {
		OWLGraphWrapper graph = new OWLGraphWrapper(model.getAboxOntology());
		return getClass(cid, graph);
	}
	private OWLClass getClass(String cid, OWLGraphWrapper graph) {
		IRI iri = MolecularModelJsonRenderer.getIRI(cid, graph);
		return graph.getOWLClass(iri);
	}
	private OWLClass getGeneClass(String cid, LegoModelGenerator model) {
		OWLGraphWrapper graph = new OWLGraphWrapper(model.getAboxOntology());
		IRI iri = MolecularModelJsonRenderer.getIRI(cid, graph);
		return model.getOWLDataFactory().getOWLClass(iri);
	}
	private OWLObjectProperty getObjectProperty(String pid, LegoModelGenerator model) {
		OWLGraphWrapper graph = new OWLGraphWrapper(model.getAboxOntology());
		IRI iri = MolecularModelJsonRenderer.getIRI(pid, graph);
		return graph.getOWLObjectProperty(iri);
	}

	private OWLObjectPropertyExpression getObjectProperty(OBOUpperVocabulary vocabElement,
			LegoModelGenerator model) {
		return vocabElement.getObjectProperty(model.getAboxOntology());
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
	 * TODO better re-use of MolecularModelJsonRenderer.
	 * At the moment this method is a partial copy of
	 * {@link MolecularModelJsonRenderer#renderObject(OWLOntology)}
	 * 
	 * TODO rename to reflect extended task, i.e. facts
	 * @param resp
	 * @param mod
	 * @param individuals
	 */
	private void addIndividualsData(OWLOperationResponse resp, LegoModelGenerator mod, OWLIndividual...individuals) {
		List<OWLNamedIndividual> individualIds = new ArrayList<OWLNamedIndividual>();
		for (OWLIndividual i : individuals) {
			if (i instanceof OWLNamedIndividual) {
				individualIds.add((OWLNamedIndividual)i);
			}
		}
		MolecularModelJsonRenderer renderer = new MolecularModelJsonRenderer(mod.getAboxOntology());
		Map<Object, Object> map = renderer.renderIndividuals(individualIds);
		resp.setModelData(map);
		resp.setIndividuals(individualIds);
	}
	
	/**
	 * Adds ClassAssertion(c,i) to specified model
	 * 
	 * @param modelId
	 * @param i
	 * @param c
	 * @return response info
	 */
	public OWLOperationResponse addType(String modelId, OWLNamedIndividual i, OWLClass c) {
		LegoModelGenerator model = getModel(modelId);
		addType(model, i, c, true);
		return createResponse(true, model, i);
	}
	/**
	 * Convenience wrapper for {@link #addType(String, OWLNamedIndividual, OWLClass)}
	 * 
	 * @param modelId
	 * @param iid
	 * @param cid
	 * @return response info
	 * @throws UnknownIdentifierException 
	 */
	public OWLOperationResponse addType(String modelId, String iid, String cid) throws UnknownIdentifierException {
		LegoModelGenerator model = getModel(modelId);
		OWLNamedIndividual individual = getIndividual(iid, model);
		if (individual == null) {
			throw new UnknownIdentifierException("Could not find a individual for id: "+iid);
		}
		OWLClass cls = getClass(cid, model);
		if (cls == null) {
			throw new UnknownIdentifierException("Could not find a class for id: "+cid);
		}
		addType(model, individual, cls, true);
		return createResponse(true, model, individual);
	}
	
	/**
	 * @param modelId
	 * @param iid
	 * @param cid
	 * @return individual
	 * @throws UnknownIdentifierException 
	 */
	public OWLNamedIndividual addTypeNonReasoning(String modelId, String iid, String cid) throws UnknownIdentifierException {
		LegoModelGenerator model = getModel(modelId);
		OWLNamedIndividual individual = getIndividual(iid, model);
		if (individual == null) {
			throw new UnknownIdentifierException("Could not find a individual for id: "+iid);
		}
		OWLClass cls = getClass(cid, model);
		if (cls == null) {
			throw new UnknownIdentifierException("Could not find a class for id: "+cid);
		}
		addType(model, individual, cls, false);
		return individual;
	}
	
	/**
	 * Adds ClassAssertion(c,i) to specified model
	 * 
	 * @param model
	 * @param i
	 * @param c
	 * @param flushReasoner
	 */
	private void addType(LegoModelGenerator model, OWLIndividual i, OWLClass c, boolean flushReasoner) {
		OWLClassAssertionAxiom axiom = model.getOWLDataFactory().getOWLClassAssertionAxiom(c,i);
		addAxiom(model, axiom, flushReasoner);
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
			OWLNamedIndividual i, 
			OWLObjectPropertyExpression p,
			OWLClassExpression filler) {
		LegoModelGenerator model = getModel(modelId);
		addType(model, i, p, filler, true);
		return createResponse(true, model, i);
	}
	
	/**
	 * Convenience wrapper for
	 *  {@link #addType(String, OWLNamedIndividual, OWLObjectPropertyExpression, OWLClassExpression)}
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
		LegoModelGenerator model = getModel(modelId);
		OWLNamedIndividual individual = getIndividual(iid, model);
		if (individual == null) {
			throw new UnknownIdentifierException("Could not find a individual for id: "+iid);
		}
		OWLObjectProperty property = getObjectProperty(pid, model);
		if (property == null) {
			throw new UnknownIdentifierException("Could not find a property for id: "+pid);
		}
		OWLClass cls = getClass(cid, model);
		if (cls == null) {
			throw new UnknownIdentifierException("Could not find a class for id: "+cid);
		}
		addType(model, individual, property, cls, true);
		return createResponse(true, model, individual);
	}
	
	public OWLNamedIndividual addTypeNonReasoning(String modelId,
			String iid, String pid, String cid) throws UnknownIdentifierException {
		LegoModelGenerator model = getModel(modelId);
		OWLNamedIndividual individual = getIndividual(iid, model);
		if (individual == null) {
			throw new UnknownIdentifierException("Could not find a individual for id: "+iid);
		}
		OWLObjectProperty property = getObjectProperty(pid, model);
		if (property == null) {
			throw new UnknownIdentifierException("Could not find a property for id: "+pid);
		}
		OWLClass cls = getClass(cid, model);
		if (cls == null) {
			throw new UnknownIdentifierException("Could not find a class for id: "+cid);
		}
		addType(model, individual, property, cls, false);
		return individual;
	}

	/**
	 * Adds a ClassAssertion, where the class expression instantiated is an
	 * ObjectSomeValuesFrom expression
	 * 
	 * Example: Individual: i Type: enabledBy some PRO_123 
	 * 
	 * @param model
	 * @param i
	 * @param p
	 * @param filler
	 * @param flushReasoner
	 */
	private void addType(LegoModelGenerator model,
			OWLIndividual i, 
			OWLObjectPropertyExpression p,
			OWLClassExpression filler,
			boolean flushReasoner) {
		LOG.info("Adding "+i+ " type "+p+" some "+filler);
		OWLDataFactory f = model.getOWLDataFactory();
		OWLObjectSomeValuesFrom c = f.getOWLObjectSomeValuesFrom(p, filler);
		OWLClassAssertionAxiom axiom = f.getOWLClassAssertionAxiom(c, i);
		addAxiom(model, axiom, flushReasoner);
	}
	
	/**
	 * remove ClassAssertion(c,i) to specified model
	 * 
	 * @param modelId
	 * @param i
	 * @param c
	 * @return response info
	 */
	public OWLOperationResponse removeType(String modelId, OWLNamedIndividual i, OWLClass c) {
		LegoModelGenerator model = getModel(modelId);
		removeType(model, i, c, true);
		return createResponse(true, model, i);
	}

	/**
	 * Convenience wrapper for {@link #removeType(String, OWLNamedIndividual, OWLClass)}
	 * 
	 * @param modelId
	 * @param iid
	 * @param cid
	 * @return response info
	 * @throws UnknownIdentifierException 
	 */
	public OWLOperationResponse removeType(String modelId, String iid, String cid) throws UnknownIdentifierException {
		LegoModelGenerator model = getModel(modelId);
		OWLNamedIndividual individual = getIndividual(iid, model);
		if (individual == null) {
			throw new UnknownIdentifierException("Could not find a individual for id: "+iid);
		}
		OWLClass cls = getClass(cid, model);
		if (cls == null) {
			throw new UnknownIdentifierException("Could not find a class for id: "+cid);
		}
		removeType(model, individual, cls, true);
		return createResponse(true, model, individual);
	}
	
	public OWLNamedIndividual removeTypeNonReasoning(String modelId, String iid, String cid) throws UnknownIdentifierException {
		LegoModelGenerator model = getModel(modelId);
		OWLNamedIndividual individual = getIndividual(iid, model);
		if (individual == null) {
			throw new UnknownIdentifierException("Could not find a individual for id: "+iid);
		}
		OWLClass cls = getClass(cid, model);
		if (cls == null) {
			throw new UnknownIdentifierException("Could not find a class for id: "+cid);
		}
		removeType(model, individual, cls, false);
		return individual;
	}
	
	public OWLNamedIndividual removeTypeNonReasoning(String modelId, String iid, String pid, String cid) throws UnknownIdentifierException {
		LegoModelGenerator model = getModel(modelId);
		OWLNamedIndividual individual = getIndividual(iid, model);
		if (individual == null) {
			throw new UnknownIdentifierException("Could not find a individual for id: "+iid);
		}
		OWLObjectProperty property = getObjectProperty(pid, model);
		if (property == null) {
			throw new UnknownIdentifierException("Could not find a property for id: "+pid);
		}
		OWLClass cls = getClass(cid, model);
		if (cls == null) {
			throw new UnknownIdentifierException("Could not find a class for id: "+cid);
		}
		removeType(model, individual, property, cls, false);
		return individual;
	}
	
	/**
	 * remove ClassAssertion(c,i) from the model
	 * 
	 * @param model
	 * @param i
	 * @param c
	 * @param flushReasoner
	 */
	private void removeType(LegoModelGenerator model, OWLIndividual i, OWLClass c, boolean flushReasoner) {
		OWLDataFactory f = model.getOWLDataFactory();
		OWLClassAssertionAxiom axiom = f.getOWLClassAssertionAxiom(c,i);
		removeAxiom(model, axiom, flushReasoner);
	}
	
	/**
	 * Removes a ClassAssertion, where the class expression instantiated is an
	 * ObjectSomeValuesFrom expression
	 * 
	 * TODO - in future it should be possible to remove multiple assertions by leaving some fields null
	 * 
	 * @param modelId
	 * @param i
	 * @param p
	 * @param filler
	 * @return response info
	 */
	public OWLOperationResponse removeType(String modelId,
			OWLNamedIndividual i, 
			OWLObjectPropertyExpression p,
			OWLClassExpression filler) {
		LegoModelGenerator model = getModel(modelId);
		removeType(model, i, p, filler, true);
		return createResponse(true, model, i);
	}
	
	
	// TODO
//	public OWLOperationResponse removeTypes(String modelId,
//			OWLIndividual i, 
//			OWLObjectPropertyExpression p) {
//		return removeType(modelId, i, p, null);
//	}

	private void removeType(LegoModelGenerator model,
			OWLIndividual i, 
			OWLObjectPropertyExpression p,
			OWLClassExpression filler,
			boolean flushReasoner) {
		OWLDataFactory f = model.getOWLDataFactory();
		OWLClassAssertionAxiom axiom = f.getOWLClassAssertionAxiom(f.getOWLObjectSomeValuesFrom(p, filler), i);
		removeAxiom(model, axiom, flushReasoner);
	}
	

	/**
	 * Convenience wrapper for {@link #addOccursIn(String, OWLNamedIndividual, OWLClassExpression)}
	 * 
	 * @param modelId
	 * @param iid
	 * @param eid - e.g. PR:P12345
	 * @return response info
	 * @throws UnknownIdentifierException
	 */
	public OWLOperationResponse addOccursIn(String modelId,
			String iid, String eid) throws UnknownIdentifierException {
		LegoModelGenerator model = getModel(modelId);
		OWLNamedIndividual individual = getIndividual(iid, model);
		if (individual == null) {
			throw new UnknownIdentifierException("Could not find a individual for id: "+iid);
		}
		OWLClass cls = getClass(eid, model);
		if (cls == null) {
			throw new UnknownIdentifierException("Could not find a class for id: "+eid);
		}
		return addOccursIn(model, individual, cls);
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
			OWLNamedIndividual i, 
			OWLClassExpression enabler) {
		LegoModelGenerator model = getModel(modelId);
		return addOccursIn(model, i, enabler);
	}
	
	private OWLOperationResponse addOccursIn(LegoModelGenerator model,
			OWLNamedIndividual i, 
			OWLClassExpression enabler) {
		addType(model, i, OBOUpperVocabulary.BFO_occurs_in.getObjectProperty(getOntology()), enabler, true);
		return createResponse(true, model, i);
	} 

	/**
	 * Convenience wrapper for {@link #addEnabledBy(String, OWLNamedIndividual, OWLClassExpression)}
	 * 
	 * @param modelId
	 * @param iid
	 * @param eid - e.g. PR:P12345
	 * @return response info
	 * @throws UnknownIdentifierException 
	 * @throws OWLException
	 */
	public OWLOperationResponse addEnabledBy(String modelId,
			String iid, String eid) throws UnknownIdentifierException,
			OWLException {
		LegoModelGenerator model = getModel(modelId);
		OWLNamedIndividual individual = getIndividual(iid, model);
		if (individual == null) {
			throw new UnknownIdentifierException("Could not find a individual for id: "+iid);
		}
		OWLClassExpression clsExpr;
		if (eid.contains(" ")) {
			clsExpr = parseClassExpression(eid, model);
		}
		else {
			clsExpr = getGeneClass(eid, model);
			if (clsExpr == null) {
				throw new UnknownIdentifierException("Could not find a class for id: "+eid);
			}
		}
		return addEnabledBy(model, individual, clsExpr);
	}

	private OWLClassExpression parseClassExpression(String expression, LegoModelGenerator model) throws OWLException {
		ManchesterSyntaxTool syntaxTool = null;
		try {
			syntaxTool = new ManchesterSyntaxTool(model.getAboxOntology());
			OWLClassExpression clsExpr = syntaxTool.parseManchesterExpression(expression);
			return clsExpr;
		}
		catch (ParserException e) {
			// wrap in an Exception (not RuntimeException) to enable proper error handling
			throw new OWLException("Could not parse expression: \""+expression+"\"", e) {

				private static final long serialVersionUID = -9158071212925724138L;
			};
		}
		finally {
			if (syntaxTool != null) {
				syntaxTool.dispose();
			}
		}
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
			OWLNamedIndividual i, 
			OWLClassExpression enabler) {
		LegoModelGenerator model = getModel(modelId);
		return addEnabledBy(model, i, enabler);
	}
	
	private OWLOperationResponse addEnabledBy(LegoModelGenerator model,
			OWLNamedIndividual i, 
			OWLClassExpression enabler) {
		OWLObjectProperty p = OBOUpperVocabulary.GOREL_enabled_by.getObjectProperty(model.getAboxOntology());
		addType(model, i, p, enabler, true);
		return createResponse(true, model, i);
	}


	/**
	 * Adds triple (i,p,j) to specified model
	 * 
	 * @param modelId
	 * @param p
	 * @param i
	 * @param j
	 * @param annotations
	 * @return response info
	 */
	public OWLOperationResponse addFact(String modelId, OWLObjectPropertyExpression p,
			OWLNamedIndividual i, OWLNamedIndividual j, Set<OWLAnnotation> annotations) {
		LegoModelGenerator model = getModel(modelId);
		addFact(model, p, i, j, annotations, true);
		return createResponse(true, model, i, j);
	}
	
	/**
	 * Convenience wrapper for {@link #addFact(String, OWLObjectPropertyExpression, OWLNamedIndividual, OWLNamedIndividual, Set)}
	 *	
	 * @param modelId
	 * @param vocabElement
	 * @param i
	 * @param j
	 * @param annotations
	 * @return response info
	 */
	public OWLOperationResponse addFact(String modelId, OBOUpperVocabulary vocabElement,
			OWLNamedIndividual i, OWLNamedIndividual j, Set<OWLAnnotation> annotations) {
		LegoModelGenerator model = getModel(modelId);
		OWLObjectProperty p = vocabElement.getObjectProperty(model.getAboxOntology());
		addFact(model, p, i, j, annotations, true);
		return createResponse(true, model, i, j);
	}

	/**
	 * Convenience wrapper for {@link #addFact(String, OWLObjectPropertyExpression, OWLNamedIndividual, OWLNamedIndividual, Set)}
	 * 
	 * @param modelId
	 * @param pid
	 * @param iid
	 * @param jid
	 * @param pairs 
	 * @return response info
	 * @throws UnknownIdentifierException 
	 */
	public OWLOperationResponse addFact(String modelId, String pid,	String iid, String jid,
			Collection<Pair<String,String>> pairs) throws UnknownIdentifierException {
		LegoModelGenerator model = getModel(modelId);
		OWLObjectProperty property = getObjectProperty(pid, model);
		if (property == null) {
			throw new UnknownIdentifierException("Could not find a property for id: "+pid);
		}
		OWLNamedIndividual individual1 = getIndividual(iid, model);
		if (individual1 == null) {
			throw new UnknownIdentifierException("Could not find a individual (1) for id: "+iid);
		}
		OWLNamedIndividual individual2 = getIndividual(jid, model);
		if (individual2 == null) {
			throw new UnknownIdentifierException("Could not find a individual (2) for id: "+jid);
		}
		Set<OWLAnnotation> annotations = createAnnotations(pairs, model);
		addFact(model, property, individual1, individual2, annotations, true);
		return createResponse(true, model, individual1, individual2);
	}

	/**
	 * Convenience wrapper for {@link #addFact(String, OWLObjectPropertyExpression, OWLNamedIndividual, OWLNamedIndividual, Set)}
	 * 
	 * @param modelId
	 * @param pid
	 * @param iid
	 * @param jid
	 * @param pairs 
	 * @return relevant individuals
	 * @throws UnknownIdentifierException 
	 */
	public List<OWLNamedIndividual> addFactNonReasoning(String modelId, String pid,	String iid, String jid,
			Collection<Pair<String,String>> pairs) throws UnknownIdentifierException {
		LegoModelGenerator model = getModel(modelId);
		OWLObjectProperty property = getObjectProperty(pid, model);
		if (property == null) {
			throw new UnknownIdentifierException("Could not find a property for id: "+pid);
		}
		OWLNamedIndividual individual1 = getIndividual(iid, model);
		if (individual1 == null) {
			throw new UnknownIdentifierException("Could not find a individual (1) for id: "+iid);
		}
		OWLNamedIndividual individual2 = getIndividual(jid, model);
		if (individual2 == null) {
			throw new UnknownIdentifierException("Could not find a individual (2) for id: "+jid);
		}
		Set<OWLAnnotation> annotations = createAnnotations(pairs, model);
		addFact(model, property, individual1, individual2, annotations, false);
		return Arrays.asList(individual1, individual2);
	}
	
	/**
	 * Convenience wrapper for {@link #addFact(String, OWLObjectPropertyExpression, OWLNamedIndividual, OWLNamedIndividual, Set)}
	 * 
	 * @param modelId
	 * @param vocabElement
	 * @param iid
	 * @param jid
	 * @param pairs
	 * @return response info
	 * @throws UnknownIdentifierException
	 */
	public OWLOperationResponse addFact(String modelId, OBOUpperVocabulary vocabElement,
			String iid, String jid, Collection<Pair<String,String>> pairs) throws UnknownIdentifierException {
		LegoModelGenerator model = getModel(modelId);
		OWLObjectPropertyExpression property = getObjectProperty(vocabElement, model);
		if (property == null) {
			throw new UnknownIdentifierException("Could not find a individual for id: "+vocabElement);
		}
		OWLNamedIndividual individual1 = getIndividual(iid, model);
		if (individual1 == null) {
			throw new UnknownIdentifierException("Could not find a individual for id: "+iid);
		}
		OWLNamedIndividual individual2 = getIndividual(jid, model);
		if (individual2 == null) {
			throw new UnknownIdentifierException("Could not find a individual for id: "+jid);
		}
		Set<OWLAnnotation> annotations = createAnnotations(pairs, model);
		addFact(model, property, individual1, individual2, annotations, true);
		return createResponse(true, model, individual1, individual2);
	}
	
	/**
	 * @param model
	 * @param p
	 * @param i
	 * @param j
	 * @param annotations 
	 * @param flushReasoner
	 */
	private void addFact(LegoModelGenerator model, OWLObjectPropertyExpression p,
			OWLIndividual i, OWLIndividual j, Set<OWLAnnotation> annotations, boolean flushReasoner) {
		OWLDataFactory f = model.getOWLDataFactory();
		final OWLObjectPropertyAssertionAxiom axiom;
		if (annotations != null && !annotations.isEmpty()) {
			axiom = f.getOWLObjectPropertyAssertionAxiom(p, i, j, annotations);	
		}
		else {
			axiom = f.getOWLObjectPropertyAssertionAxiom(p, i, j);
		}
		addAxiom(model, axiom, flushReasoner);
	}

	/**
	 * @param modelId
	 * @param p
	 * @param i
	 * @param j
	 * @return response info
	 */
	public OWLOperationResponse removeFact(String modelId, OWLObjectPropertyExpression p,
			OWLNamedIndividual i, OWLNamedIndividual j) {
		LegoModelGenerator model = getModel(modelId);
		removeFact(model, p, i, j, true);
		return createResponse(true, model, i, j);
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
		LegoModelGenerator model = getModel(modelId);
		OWLObjectProperty property = getObjectProperty(pid, model);
		if (property == null) {
			throw new UnknownIdentifierException("Could not find a individual for id: "+pid);
		}
		OWLNamedIndividual individual1 = getIndividual(iid, model);
		if (individual1 == null) {
			throw new UnknownIdentifierException("Could not find a individual for id: "+iid);
		}
		OWLNamedIndividual individual2 = getIndividual(jid, model);
		if (individual2 == null) {
			throw new UnknownIdentifierException("Could not find a individual for id: "+jid);
		}
		removeFact(model, property, individual1, individual2, true);
		return createResponse(true, model, individual1, individual2);
	}
	
	public List<OWLNamedIndividual> removeFactNonReasoning(String modelId, String pid,
			String iid, String jid) throws UnknownIdentifierException {
		LegoModelGenerator model = getModel(modelId);
		OWLObjectProperty property = getObjectProperty(pid, model);
		if (property == null) {
			throw new UnknownIdentifierException("Could not find a individual for id: "+pid);
		}
		OWLNamedIndividual individual1 = getIndividual(iid, model);
		if (individual1 == null) {
			throw new UnknownIdentifierException("Could not find a individual for id: "+iid);
		}
		OWLNamedIndividual individual2 = getIndividual(jid, model);
		if (individual2 == null) {
			throw new UnknownIdentifierException("Could not find a individual for id: "+jid);
		}
		removeFact(model, property, individual1, individual2, false);
		return Arrays.asList(individual1, individual2);
	}

	private void removeFact(LegoModelGenerator model, OWLObjectPropertyExpression p,
			OWLIndividual i, OWLIndividual j, boolean flushReasoner) {
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
		removeAxiom(model, toRemove, flushReasoner);
	}
	
	public List<OWLNamedIndividual> addAnnotations(String modelId, String pid, 
			String iid, String jid, Collection<Pair<String,String>> pairs) throws UnknownIdentifierException {
		LegoModelGenerator model = getModel(modelId);
		OWLObjectProperty property = getObjectProperty(pid, model);
		if (property == null) {
			throw new UnknownIdentifierException("Could not find a property for id: "+pid);
		}
		OWLNamedIndividual individual1 = getIndividual(iid, model);
		if (individual1 == null) {
			throw new UnknownIdentifierException("Could not find a individual (1) for id: "+iid);
		}
		OWLNamedIndividual individual2 = getIndividual(jid, model);
		if (individual2 == null) {
			throw new UnknownIdentifierException("Could not find a individual (2) for id: "+jid);
		}
		Set<OWLAnnotation> annotations = createAnnotations(pairs, model);

		addAnnotations(model, property, individual1, individual2, annotations, false);

		return Arrays.asList(individual1, individual2);
	}
	
	public OWLOperationResponse addAnnotations(String modelId, OWLObjectPropertyExpression p,
			OWLNamedIndividual i, OWLNamedIndividual j, Set<OWLAnnotation> annotations) {
		LegoModelGenerator model = getModel(modelId);
		addAnnotations(model, p, i, j, annotations, true);
		return createResponse(true, model, i, j);
	}
	
	private void addAnnotations(LegoModelGenerator model, OWLObjectPropertyExpression p,
			OWLNamedIndividual i, OWLNamedIndividual j, Set<OWLAnnotation> annotations,
			boolean flushReasoner) {
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
			removeAxiom(model, toModify, false);
			OWLAxiom newAxiom = f.getOWLObjectPropertyAssertionAxiom(p, i, j, combindedAnnotations);
			addAxiom(model, newAxiom , flushReasoner);
		}
	}
	
	public List<OWLNamedIndividual> removeAnnotations(String modelId, String pid, 
			String iid, String jid, Collection<Pair<String,String>> pairs) throws UnknownIdentifierException {
		LegoModelGenerator model = getModel(modelId);
		OWLObjectProperty property = getObjectProperty(pid, model);
		if (property == null) {
			throw new UnknownIdentifierException("Could not find a property for id: "+pid);
		}
		OWLNamedIndividual individual1 = getIndividual(iid, model);
		if (individual1 == null) {
			throw new UnknownIdentifierException("Could not find a individual (1) for id: "+iid);
		}
		OWLNamedIndividual individual2 = getIndividual(jid, model);
		if (individual2 == null) {
			throw new UnknownIdentifierException("Could not find a individual (2) for id: "+jid);
		}
		Set<OWLAnnotation> annotations = createAnnotations(pairs, model);

		removeAnnotations(model, property, individual1, individual2, annotations, false);

		return Arrays.asList(individual1, individual2);
	}
	
	private void removeAnnotations(LegoModelGenerator model, OWLObjectPropertyExpression p,
			OWLNamedIndividual i, OWLNamedIndividual j, Set<OWLAnnotation> annotations,
			boolean flushReasoner) {
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
			removeAxiom(model, toModify, false);
			OWLAxiom newAxiom = f.getOWLObjectPropertyAssertionAxiom(p, i, j, combindedAnnotations);
			addAxiom(model, newAxiom , flushReasoner);
		}
	}
	
	/**
	 * Convenience wrapper for {@link #addPartOf(String, OWLNamedIndividual, OWLNamedIndividual, Set)}
	 *
	 * @param modelId
	 * @param iid
	 * @param jid
	 * @param pairs 
	 * @return response info
	 * @throws UnknownIdentifierException
	 */
	public OWLOperationResponse addPartOf(String modelId,  String iid,
			String jid, Collection<Pair<String, String>> pairs) throws UnknownIdentifierException {
		LegoModelGenerator model = getModel(modelId);
		OWLNamedIndividual individual1 = getIndividual(iid, model);
		if (individual1 == null) {
			throw new UnknownIdentifierException("Could not find a individual for id: "+iid);
		}
		OWLNamedIndividual individual2 = getIndividual(jid, model);
		if (individual2 == null) {
			throw new UnknownIdentifierException("Could not find a individual for id: "+jid);
		}
		Set<OWLAnnotation> annotations = createAnnotations(pairs, model);
		return addPartOf(modelId, individual1, individual2, annotations);
	}

	/**
	 * Adds an OWL ObjectPropertyAssertion connecting i to j via part_of
	 * 
	 * Note that the inverse assertion is entailed, but not asserted
	 * 
	 * @param modelId
	 * @param i
	 * @param j
	 * @param annotations
	 * @return response info
	 */
	public OWLOperationResponse addPartOf(String modelId, OWLNamedIndividual i,
			OWLNamedIndividual j, Set<OWLAnnotation> annotations) {
		LegoModelGenerator model = getModel(modelId);
		addPartOf(model, i, j, annotations, true);
		return createResponse(true, model, i, j);
	}
	
	private void addPartOf(LegoModelGenerator model, OWLNamedIndividual i, 
			OWLNamedIndividual j, Set<OWLAnnotation> annotations, boolean flushReasoner) {
		addFact(model, getObjectProperty(OBOUpperVocabulary.BFO_part_of, model), i, j, annotations, flushReasoner);
	}



	/**
	 * In general, should not be called directly - use a wrapper method
	 * 
	 * @param model
	 * @param axiom
	 * @param flushReasoner
	 */
	void addAxiom(LegoModelGenerator model, OWLAxiom axiom, boolean flushReasoner) {
		OWLOntology ont = model.getAboxOntology();
		ont.getOWLOntologyManager().addAxiom(ont, axiom);
		if (flushReasoner) {
			model.getReasoner().flush();
		}
	}
	
	/**
	 * In general, should not be called directly - use a wrapper method
	 * 
	 * @param model
	 * @param axioms
	 * @param flushReasoner
	 */
	void addAxioms(LegoModelGenerator model, Set<OWLAxiom> axioms, boolean flushReasoner) {
		OWLOntology ont = model.getAboxOntology();
		ont.getOWLOntologyManager().addAxioms(ont, axioms);
		if (flushReasoner) {
			model.getReasoner().flush();
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
	 * @param model
	 * @param axiom
	 * @param flushReasoner
	 */
	void removeAxiom(LegoModelGenerator model, OWLAxiom axiom, boolean flushReasoner) {
		OWLOntology ont = model.getAboxOntology();
		ont.getOWLOntologyManager().removeAxiom(ont, axiom);
		if (flushReasoner) {
			model.getReasoner().flush();
		}
	}

	/**
	 * In general, should not be called directly - use a wrapper method
	 * 
	 * @param modelId
	 * @param axioms
	 * @param flushReasoner
	 */
	void removeAxioms(String modelId, Set<OWLAxiom> axioms, boolean flushReasoner) {
		LegoModelGenerator model = getModel(modelId);
		removeAxioms(model, axioms, flushReasoner);
	}
	
	private void removeAxioms(LegoModelGenerator model, Set<OWLAxiom> axioms, boolean flushReasoner) {
		OWLOntology ont = model.getAboxOntology();
		ont.getOWLOntologyManager().removeAxioms(ont, axioms);
		
		if (flushReasoner) {
			model.getReasoner().flush();
		}
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
		return "gomodel:" + db + "-"+ p.replaceAll(":", "-");
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
	@Deprecated
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
	 * A simple wrapping function that captures the most basic type of editing.
	 * 
	 * @param modelId
	 * @param classId
	 * @param enabledById
	 * @param occursInId
	 * @return response
	 * @throws UnknownIdentifierException
	 */
	public OWLOperationResponse addCompositeIndividual(String modelId, String classId,
			String enabledById, String occursInId) throws UnknownIdentifierException {
		LegoModelGenerator model = getModel(modelId);
		// Create the base individual.
		OWLClass cls = getClass(classId, model);
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
			OWLClass occCls = getClass(occursInId, model);
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
			OWLClass enbCls = getGeneClass(enabledById, model);
			resp = addEnabledBy(modelId, i, enbCls);
			// Bail out early if it looks like there are any problems.
			if( resp.isSuccess == false || resp.isResultsInInconsistency() ){
				return resp;
			}
		}
		
		return resp;
	}
	

}
