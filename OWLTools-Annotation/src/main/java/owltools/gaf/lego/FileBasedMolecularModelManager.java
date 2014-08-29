package owltools.gaf.lego;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxOntologyFormat;
import org.semanticweb.owlapi.io.OWLFunctionalSyntaxOntologyFormat;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyDocumentAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import owltools.gaf.GafDocument;
import owltools.gaf.bioentities.ProteinTools;
import owltools.gaf.parser.GafObjectsBuilder;
import owltools.graph.OWLGraphWrapper;
import owltools.io.CatalogXmlIRIMapper;
import owltools.util.ModelContainer;

/**
 * Layer for retrieving and storing models as OWL files.
 * 
 * @param <METADATA> 
 * @see CoreMolecularModelManager
 */
public class FileBasedMolecularModelManager<METADATA> extends CoreMolecularModelManager<METADATA> {
	
	private static Logger LOG = Logger.getLogger(FileBasedMolecularModelManager.class);

	boolean isPrecomputePropertyClassCombinations = false;
	Map<String, GafDocument> dbToGafdoc = new HashMap<String, GafDocument>();
	String pathToGafs = "gene-associations";
	String pathToOWLFiles = "owl-models";
	String pathToProteinFiles = null;
	Map<String, String> dbToTaxon = null;
	OWLOntologyIRIMapper proteinMapper = null;
	
	GafObjectsBuilder builder = new GafObjectsBuilder();
	// WARNING: Do *NOT* switch to functional syntax until the OWL-API has fixed a bug.
	OWLOntologyFormat ontologyFormat = new ManchesterOWLSyntaxOntologyFormat();

	/**
	 * @param graph
	 * @throws OWLOntologyCreationException
	 */
	public FileBasedMolecularModelManager(OWLGraphWrapper graph) throws OWLOntologyCreationException {
		super(graph);
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
	 * @param pathToProteinFiles
	 * @throws IOException 
	 */
	public synchronized void setPathToProteinFiles(String pathToProteinFiles) throws IOException {
		setPathToProteinFiles(pathToProteinFiles, "catalog-v001.xml");
	}
	
	/**
	 * @param pathToProteinFiles
	 * @param catalogXML
	 * @throws IOException 
	 */
	public synchronized void setPathToProteinFiles(String pathToProteinFiles, String catalogXML) throws IOException {
		if (proteinMapper != null) {
			graph.getManager().removeIRIMapper(proteinMapper);
		}
		this.pathToProteinFiles = pathToProteinFiles;
		proteinMapper = new CatalogXmlIRIMapper(new File(pathToProteinFiles, catalogXML));
		graph.getManager().addIRIMapper(proteinMapper);
	}
	
	public String getPathToProteinFiles() {
		return pathToProteinFiles;
	}
	
	/**
	 * @return the dbToTaxon
	 */
	public Map<String, String> getDbToTaxon() {
		return dbToTaxon;
	}
	/**
	 * @param dbToTaxon the dbToTaxon to set
	 */
	public void setDbToTaxon(Map<String, String> dbToTaxon) {
		this.dbToTaxon = dbToTaxon;
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
			LOG.info("Loading GAF for db: "+db);
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
	 * @param metadata
	 * @return modelId
	 * @throws OWLOntologyCreationException
	 * @throws URISyntaxException 
	 * @throws IOException 
	 */
	public String generateModel(OWLClass processCls, String db, METADATA metadata) throws OWLOntologyCreationException, IOException, URISyntaxException {
		// quick check, only generate a model if it does not already exists.
		final String p = graph.getIdentifier(processCls);
		String modelId = getModelId(p, db);
		if (modelMap.containsKey(modelId)) {
			throw new OWLOntologyCreationException("A model already exists for this process and db: "+modelId);
		}
		LOG.info("Generating model for Class: "+p+" and db: "+db);
		OWLOntology tbox = graph.getSourceOntology();
		OWLOntology abox = null;
		ModelContainer model = null;
		
		// create empty ontology
		// use model id as ontology IRI
		OWLOntologyManager m = graph.getManager();
		IRI iri = MolecularModelJsonRenderer.getIRI(modelId, graph);
		try {
			abox = m.createOntology(iri);
			
			// setup model ontology to import the source ontology and other imports
			createImports(abox, tbox.getOntologyID(), db, metadata);
			
			// create generator
			model = new ModelContainer(tbox, abox);
			LegoModelGenerator generator = new LegoModelGenerator(model);
			
			generator.setPrecomputePropertyClassCombinations(isPrecomputePropertyClassCombinations);
			Set<String> seedGenes = new HashSet<String>();
			// only look for genes if a GAF is available
			if (db != null) {
				GafDocument gafdoc = getGaf(db);
				generator.initialize(gafdoc, graph);
				seedGenes.addAll(generator.getGenes(processCls));
			}
			generator.setContextualizingSuffix(db);
			generator.buildNetwork(p, seedGenes);
		}
		catch (OWLOntologyCreationException exception) {
			if (model != null) {
				model.dispose();
			} else if (abox != null) {
				m.removeOntology(abox);
			}
			throw exception;
		}
		catch (IOException exception) {
			if (model != null) {
				model.dispose();
			}
			throw exception;
		}
		catch (URISyntaxException exception) {
			if (model != null) {
				model.dispose();
			}
			throw exception;
		}
		modelMap.put(modelId, model);
		return modelId;
	}

	private void createImports(OWLOntology ont, OWLOntologyID tboxId, String db, METADATA metadata) throws OWLOntologyCreationException {
		String taxonId = mapDbToTaxonId(db);
		createImportsWithTaxon(ont, tboxId, taxonId, metadata);
	}

	/**
	 * @param db
	 * @return taxon id or null
	 */
	private String mapDbToTaxonId(String db) {
		String taxonId = null;
		// check for protein ontology
		if (db != null && pathToProteinFiles != null && proteinMapper != null) {
			taxonId = null;
			if (dbToTaxon != null) {
				taxonId = dbToTaxon.get(db);
			}
			if (taxonId == null) {
				// fallback
				taxonId = db;
			}
		}
		return taxonId;
	}
	
	private void createImportsWithTaxon(OWLOntology ont, OWLOntologyID tboxId, String taxonId, METADATA metadata) throws OWLOntologyCreationException {
		OWLOntologyManager m = ont.getOWLOntologyManager();
		OWLDataFactory f = m.getOWLDataFactory();
		
		// import T-Box
		OWLImportsDeclaration tBoxImportDeclaration = f.getOWLImportsDeclaration(tboxId.getOntologyIRI());
		m.applyChange(new AddImport(ont, tBoxImportDeclaration));
		
		// import additional ontologies via IRI
		for (IRI importIRI : additionalImports) {
			OWLImportsDeclaration importDeclaration = f.getOWLImportsDeclaration(importIRI);
			// check that the import ontology is available
			OWLOntology importOntology = m.getOntology(importIRI);
			if (importOntology == null) {
				// only try to load it, if it isn't already loaded
				try {
					m.loadOntology(importIRI);
				} catch (OWLOntologyDocumentAlreadyExistsException e) {
					// ignore
				} catch (OWLOntologyAlreadyExistsException e) {
					// ignore
				}
			}
			m.applyChange(new AddImport(ont, importDeclaration));
		}
		
		// check for protein ontology
		if (taxonId != null && pathToProteinFiles != null && proteinMapper != null) {
			IRI proteinIRI = ProteinTools.createProteinOntologyIRI(taxonId);
			IRI mapped = proteinMapper.getDocumentIRI(proteinIRI);
			if (mapped != null) {
				OWLImportsDeclaration importDeclaration = f.getOWLImportsDeclaration(proteinIRI);
				m.loadOntology(proteinIRI);
				m.applyChange(new AddImport(ont, importDeclaration));
			}
		}
	}
	
	/**
	 * Generates a new model taking as input a database D.
	 * 
	 * Note that the resulting model is uniquely identified by the modeId, which is currently constructed
	 * as a concatenation of the db and a hidden UUID state. This means that in the unlikely case that
	 * there is an existing model by this ID, it will be overwritten,
	 * 
	 * @param db
	 * @param metadata
	 * @return modelId
	 * @throws OWLOntologyCreationException
	 * @throws URISyntaxException 
	 * @throws IOException 
	 */
	public String generateBlankModel(String db, METADATA metadata) throws OWLOntologyCreationException, IOException, URISyntaxException {

		// Create an arbitrary unique ID and add it to the system.
		String modelId;
		if (db != null) {
			modelId = generateId("gomodel:", db, "-");
		}
		else{
			modelId = generateId("gomodel:");
		}
		if (modelMap.containsKey(modelId)) {
			throw new OWLOntologyCreationException("A model already exists for this db: "+modelId);
		}
		LOG.info("Generating blank model for new modelId: "+modelId);

		// create empty ontology, use model id as ontology IRI
		final OWLOntologyManager m = graph.getManager();
		IRI aBoxIRI = MolecularModelJsonRenderer.getIRI(modelId, graph);
		final OWLOntology tbox = graph.getSourceOntology();
		OWLOntology abox = null;
		ModelContainer model = null;
		try {
			abox = m.createOntology(aBoxIRI);
	
			// add imports to T-Box and additional ontologies via IRI
			createImports(abox, tbox.getOntologyID(), db, metadata);
			
			// generate model
			model = new ModelContainer(tbox, abox);
		}
		catch (OWLOntologyCreationException exception) {
			if (abox != null) {
				m.removeOntology(abox);
			}
			throw exception;
		}
		// add to internal map
		modelMap.put(modelId, model);
		return modelId;
	}
	
	/**
	 * Generates a new blank model, add protein label for the given as import.
	 * 
	 * @param taxonId
	 * @param metadata
	 * @return modelId
	 * @throws OWLOntologyCreationException
	 * @throws URISyntaxException 
	 * @throws IOException 
	 */
	public String generateBlankModelWithTaxon(String taxonId, METADATA metadata) throws OWLOntologyCreationException, IOException, URISyntaxException {

		// Create an arbitrary unique ID and add it to the system.
		String modelId;
		if (taxonId != null) {
			taxonId = normalizeTaxonId(taxonId);
			modelId = generateId("gomodel:taxon_", taxonId, "-");
		}
		else{
			modelId = generateId("gomodel:");
		}
		if (modelMap.containsKey(modelId)) {
			throw new OWLOntologyCreationException("A model already exists for this db: "+modelId);
		}
		LOG.info("Generating blank model for new modelId: "+modelId);

		// create empty ontology, use model id as ontology IRI
		final OWLOntologyManager m = graph.getManager();
		IRI aBoxIRI = MolecularModelJsonRenderer.getIRI(modelId, graph);
		final OWLOntology tbox = graph.getSourceOntology();
		OWLOntology abox = null;
		ModelContainer model = null;
		try {
			abox = m.createOntology(aBoxIRI);
	
			// add imports to T-Box and additional ontologies via IRI
			createImportsWithTaxon(abox, tbox.getOntologyID(), taxonId, metadata);
			
			// generate model
			model = new ModelContainer(tbox, abox);
		}
		catch (OWLOntologyCreationException exception) {
			if (abox != null) {
				m.removeOntology(abox);
			}
			throw exception;
		}
		// add to internal map
		modelMap.put(modelId, model);
		return modelId;
	}
	
	private String normalizeTaxonId(String taxonId) {
		// only take the numeric part
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < taxonId.length(); i++) {
			char c = taxonId.charAt(i);
			if (Character.isDigit(c)) {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	public String generateDerivedModel(String sourceModelId, METADATA metadata) throws OWLOntologyCreationException, IOException, URISyntaxException {
		LOG.info("Generating derived model from "+sourceModelId);
		ModelContainer sourceModel = this.getModel(sourceModelId); 
		String modelId = this.generateBlankModel(null, metadata);
		ModelContainer model = this.getModel(modelId);
		// TODO - populate, adding metadata
		Set<OWLNamedIndividual> sourceInds = this.getIndividuals(sourceModelId);
		for (OWLNamedIndividual sourceInd : sourceInds) {
			// clone sourceInd
		}
		return modelId;
	}
	
	/**
	 * Save all models to disk. The optional annotations may be used to set saved_by and other meta data. 
	 * 
	 * @param annotations
	 * @param metadata
	 * 
	 * @throws OWLOntologyStorageException
	 * @throws OWLOntologyCreationException
	 * @throws IOException 
	 */
	public void saveAllModels(Collection<Pair<String, String>> annotations, METADATA metadata) throws OWLOntologyStorageException, OWLOntologyCreationException, IOException {
		for (Entry<String, ModelContainer> entry : modelMap.entrySet()) {
			saveModel(entry.getKey(), entry.getValue(), annotations, metadata);
		}
	}
	
	/**
	 * Save a model to disk.
	 * 
	 * @param modelId 
	 * @param m 
	 * @param annotations 
	 * @param metadata
	 *
	 * @throws OWLOntologyStorageException 
	 * @throws OWLOntologyCreationException 
	 * @throws IOException
	 */
	public void saveModel(String modelId, ModelContainer m, Collection<Pair<String, String>> annotations, METADATA metadata) throws OWLOntologyStorageException, OWLOntologyCreationException, IOException {
		final OWLOntology ont = m.getAboxOntology();
		final OWLOntologyManager manager = ont.getOWLOntologyManager();
		
		// prelimiary checks for the target file
		File targetFile = getOwlModelFile(modelId).getCanonicalFile();
		if (targetFile.exists()) {
			if (targetFile.isFile() == false) {
				throw new IOException("For modelId: '"+modelId+"', the resulting path is not a file: "+targetFile.getAbsolutePath());
			}
			if (targetFile.canWrite() == false) {
				throw new IOException("For modelId: '"+modelId+"', Cannot write to the file: "+targetFile.getAbsolutePath());
			}
		}
		else {
			File targetFolder = targetFile.getParentFile();
			FileUtils.forceMkdir(targetFolder);
		}
		File tempFile = null;
		try {
			// create tempFile
			tempFile = File.createTempFile(modelId, ".owl");
		
			// write to a temp file
			synchronized (ont) {
				saveToFile(ont, manager, tempFile, annotations, metadata);	
			}
			
			// copy temp file to the finalFile
			FileUtils.copyFile(tempFile, targetFile);
		}
		finally {
			// delete temp file
			FileUtils.deleteQuietly(tempFile);
		}
	}

	private void saveToFile(final OWLOntology ont, final OWLOntologyManager manager,
			final File outfile, final Collection<Pair<String,String>> annotations, METADATA metadata)
			throws OWLOntologyStorageException {
		// check that the annotations contain relevant meta data
		final Set<OWLAxiom> metadataAxioms = new HashSet<OWLAxiom>();
		if (annotations != null) {
//			for (Pair<String,String> pair : annotations) {
				// TODO saved by
//			}
		}
		// TODO save date?
		
		List<OWLOntologyChange> changes = null;
		final IRI outfileIRI = IRI.create(outfile);
		try {
			if (metadataAxioms.isEmpty() == false) {
				changes = manager.addAxioms(ont, metadataAxioms);
			}
			manager.saveOntology(ont, ontologyFormat, outfileIRI);
		}
		finally {
			if (changes != null) {
				List<OWLOntologyChange> invertedChanges = ReverseChangeGenerator.invertChanges(changes);
				if (invertedChanges != null && !invertedChanges.isEmpty()) {
					manager.applyChanges(invertedChanges);
				}
			}
		}
	}
	
	/**
	 * Export the ABox for the given modelId in the default {@link OWLOntologyFormat}.
	 * 
	 * @param modelId
	 * @param model
	 * @return modelContent
	 * @throws OWLOntologyStorageException
	 */
	public String exportModel(String modelId, ModelContainer model) throws OWLOntologyStorageException {
		return exportModel(modelId, model, ontologyFormat);
	}
	
	/**
	 * Export the ABox for the given modelId in the given ontology format.<br>
	 * Warning: The mapping from String to {@link OWLOntologyFormat} does not map every format!
	 * 
	 * @param modelId
	 * @param model
	 * @param format
	 * @return modelContent
	 * @throws OWLOntologyStorageException
	 */
	public String exportModel(String modelId, ModelContainer model, String format) throws OWLOntologyStorageException {
		OWLOntologyFormat ontologyFormat = getOWLOntologyFormat(format);
		if (ontologyFormat == null) {
			ontologyFormat = this.ontologyFormat;
		}
		return exportModel(modelId, model, ontologyFormat);
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

	@Override
	protected void loadModel(String modelId, boolean isOverride) throws OWLOntologyCreationException {
		LOG.info("Load model: "+modelId+" from file");
		if (modelMap.containsKey(modelId)) {
			if (!isOverride) {
				throw new OWLOntologyCreationException("Model already exists: "+modelId);
			}
			unlinkModel(modelId);
		}
		File modelFile = getOwlModelFile(modelId);
		IRI sourceIRI = IRI.create(modelFile);
		OWLOntology abox = graph.getManager().loadOntologyFromOntologyDocument(sourceIRI);
		ModelContainer model = addModel(modelId, abox);
		updateImports(modelId, model);
	}

	private File getOwlModelFile(String modelId) {
		return new File(pathToOWLFiles, modelId + ".owl").getAbsoluteFile();
	}

	/**
	 * TODO decide identifier policy for models
	 * 
	 * @param p
	 * @param db
	 * @return identifier
	 */
	String getModelId(String p, String db) {
		return "gomodel:" + db + "-"+ p.replaceAll(":", "-");
	}
	
}
