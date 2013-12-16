package owltools.gaf.lego;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.geneontology.lego.dot.LegoDotWriter;
import org.geneontology.lego.dot.LegoRenderer;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import owltools.gaf.GafDocument;
import owltools.gaf.GafObjectsBuilder;
import owltools.gaf.lego.MolecularModelJsonRenderer.KEY;
import owltools.graph.OWLGraphWrapper;

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

	LegoModelGenerator molecularModelGenerator;
	OWLGraphWrapper graph;
	boolean isPrecomputePropertyClassCombinations;
	Map<String, GafDocument> dbToGafdoc = new HashMap<String, GafDocument>();
	Map<String, LegoModelGenerator> modelMap = new HashMap<String, LegoModelGenerator>();
	String pathToGafs = "";
	GafObjectsBuilder builder = new GafObjectsBuilder();

	/**
	 * Represents the reponse to a requested translation on an
	 * ontology/model
	 * 
	 */
	public class OWLOperationResponse {
		boolean isSuccess = true;
		boolean isResultsInInconsistency = false;
		
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
		molecularModelGenerator = 
				new LegoModelGenerator(graph.getSourceOntology(), new ElkReasonerFactory());
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
	 * loads/register a Gaf document
	 * 
	 * @param db
	 * @return Gaf document
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public GafDocument loadGaf(String db) throws IOException, URISyntaxException {
		if (!dbToGafdoc.containsKey(db)) {

			GafDocument gafdoc = builder.buildDocument(pathToGafs + "/" + db + ".gz");
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
	 * Generates a 
	 * 
	 * See {@link LegoModelGenerator#buildNetwork(OWLClass, java.util.Collection)}
	 * 
	 * @param processCls
	 * @param db
	 * @return modelId
	 * @throws OWLOntologyCreationException
	 * @throws URISyntaxException 
	 * @throws IOException 
	 */
	public String generateModel(OWLClass processCls, String db) throws OWLOntologyCreationException, IOException, URISyntaxException {

		molecularModelGenerator.setPrecomputePropertyClassCombinations(isPrecomputePropertyClassCombinations);
		GafDocument gafdoc = getGaf(db);
		molecularModelGenerator.initialize(gafdoc, graph);

		Set<String> seedGenes = new HashSet<String>();
		String p = graph.getIdentifier(processCls);
		seedGenes.addAll(molecularModelGenerator.getGenes(processCls));

		molecularModelGenerator.buildNetwork(p, seedGenes);

		//OWLOntology model = molecularModelGenerator.getAboxOntology();
		String modelId = getModelId(p, db);
		modelMap.put(modelId, molecularModelGenerator);
		return modelId;

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
	 * @return
	 */
	public List<Map> getIndividualObjects(String modelId) {
		LegoModelGenerator mod = getModel(modelId);
		MolecularModelJsonRenderer renderer = new MolecularModelJsonRenderer();
		OWLOntology ont = mod.getAboxOntology();
		List<Map> objs = new ArrayList();
		for (OWLNamedIndividual i : ont.getIndividualsInSignature()) {
			objs.add(renderer.renderObject(ont, i));
		}
		return objs;
	}


	/**
	 * TODO - autogenerate a label
	 * 
	 * @param modelId
	 * @param c
	 * @return id of created individual
	 */
	public String createIndividual(String modelId, OWLClass c) {
		String cid = graph.getIdentifier(c).replaceAll(":","_");
		String iid = modelId+"-"+cid;
		IRI iri = graph.getIRIByIdentifier(iid);
		OWLNamedIndividual i = getOWLDataFactory(modelId).getOWLNamedIndividual(iri);
		addType(modelId, i, c);
		return iid;
	}

	/**
	 * @param modelId
	 * @param c
	 * @return id of created individual
	 */
	public String createActivityIndividual(String modelId, OWLClass c) {
		return createIndividual(modelId, c);
	}

	/**
	 * @param modelId
	 * @param c
	 * @return id of created individual
	 */
	public String createProcessIndividual(String modelId, OWLClass c) {
		return createIndividual(modelId, c);
	}

	/**
	 * Fetches a model by its Id
	 * 
	 * @param id
	 * @return wrapped model
	 */
	public LegoModelGenerator getModel(String id) {
		if (!modelMap.containsKey(id)) {
			// TODO - retrieve from persistent store
		}
		return modelMap.get(id);
	}
	/**
	 * @param id
	 */
	public void unlinkModel(String id) {
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
	 * @param id
	 */
	public void saveModel(String id) {
		// TODO - save to persistent store
	}
	
	private OWLIndividual getIndividual(String iid) {
		return graph.getOWLIndividualByIdentifier(iid);
	}
	private OWLClass getClass(String cid) {
		return graph.getOWLClassByIdentifier(cid);
	}
	private OWLObjectProperty getObjectProperty(String pid) {
		return graph.getOWLObjectPropertyByIdentifier(pid);
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
		return addAxiom(modelId, axiom);
	}
	
	/**
	 * Convenience wrapper for {@link #addType(String, OWLIndividual, OWLClass)}
	 * 
	 * @param modelId
	 * @param iid
	 * @param cid
	 * @return response info
	 */
	public OWLOperationResponse addType(String modelId,
			String iid, String cid) {
		return addType(modelId, getIndividual(iid), getClass(cid));
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
			OWLClass filler) {
		OWLClassAssertionAxiom axiom = 
				getOWLDataFactory(modelId).getOWLClassAssertionAxiom(
						getOWLDataFactory(modelId).getOWLObjectSomeValuesFrom(p, filler),
						i);
		return addAxiom(modelId, axiom);
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
		return addAxiom(modelId, axiom);
	}
	
	/**
	 * Convenience wrapper for {@link #addFact(String, OWLObjectPropertyExpression, OWLIndividual, OWLIndividual)}
	 * 
	 * @param modelId
	 * @param pid
	 * @param iid
	 * @param jid
	 * @return response info
	 */
	public OWLOperationResponse addFact(String modelId, String pid,
			String iid, String jid) {
		return addFact(modelId, getObjectProperty(pid), getIndividual(iid), getIndividual(jid));
	}

	
	/**
	 * @param modelId
	 * @param axiom
	 * @return response info
	 */
	public OWLOperationResponse addAxiom(String modelId, OWLAxiom axiom) {
		LegoModelGenerator model = getModel(modelId);
		OWLOntology ont = model.getAboxOntology();
		boolean isConsistentAtStart = model.getReasoner().isConsistent();
		ont.getOWLOntologyManager().addAxiom(ont, axiom);
		// TODO - track axioms to allow redo
		model.getReasoner().flush();
		boolean isConsistentAtEnd = model.getReasoner().isConsistent();
		if (isConsistentAtStart && !isConsistentAtEnd) {
			// rollback
			ont.getOWLOntologyManager().removeAxiom(ont, axiom);
			return new OWLOperationResponse(false, true);
			
		}
		else {
			return new OWLOperationResponse(true);
		}
		
	}

	/**
	 * @param p
	 * @param db
	 * @return
	 */
	private String getModelId(String p, String db) {
		return "gomodel:"+db + "-"+p.replaceAll(":", "_");
	}

	/**
	 * @param ontology
	 * @param output
	 * @param name
	 * @throws Exception
	 */
	public void writeLego(OWLOntology ontology, final String output, String name) throws Exception {

		Set<OWLNamedIndividual> individuals = ontology.getIndividualsInSignature(true);


		LegoRenderer renderer = 
				new LegoDotWriter(graph, molecularModelGenerator.getReasoner()) {

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

}
