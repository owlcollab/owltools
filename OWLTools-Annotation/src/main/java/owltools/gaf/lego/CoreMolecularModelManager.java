package owltools.gaf.lego;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.IRIDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.AddOntologyAnnotation;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationSubject;
import org.semanticweb.owlapi.model.OWLAnnotationSubjectVisitor;
import org.semanticweb.owlapi.model.OWLAnnotationValueVisitor;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
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
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.RemoveAxiom;
import org.semanticweb.owlapi.model.RemoveImport;
import org.semanticweb.owlapi.model.RemoveOntologyAnnotation;
import org.semanticweb.owlapi.model.SetOntologyID;

import owltools.gaf.BioentityDocument;
import owltools.gaf.GafDocument;
import owltools.gaf.eco.EcoMapperFactory;
import owltools.gaf.eco.SimpleEcoMapper;
import owltools.gaf.io.GafWriter;
import owltools.gaf.io.GpadWriter;
import owltools.gaf.lego.legacy.LegoToGeneAnnotationTranslator;
import owltools.graph.OWLGraphWrapper;
import owltools.util.ModelContainer;
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
	private final IRI tboxIRI;
	final Map<String, ModelContainer> modelMap = new HashMap<String, ModelContainer>();
	Set<IRI> additionalImports;
	final Map<IRI, OWLOntology> obsoleteOntologies = new HashMap<IRI, OWLOntology>();
	final Set<IRI> obsoleteImports = new HashSet<IRI>();
	private volatile SimpleEcoMapper simpleEcoMapper = null;

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
		tboxIRI = getTboxIRI(graph);
		init();
	}
	
	/**
	 * Executed before the init call {@link #init()}.
	 * 
	 * @param graph
	 * @return IRI, never null
	 * @throws OWLOntologyCreationException
	 */
	protected IRI getTboxIRI(OWLGraphWrapper graph) throws OWLOntologyCreationException {
		OWLOntology tbox = graph.getSourceOntology();
		OWLOntologyID ontologyID = tbox.getOntologyID();
		if (ontologyID != null) {
			IRI ontologyIRI = ontologyID.getOntologyIRI();
			if (ontologyIRI != null) {
				return ontologyIRI;
			}
		}
		throw new OWLOntologyCreationException("No ontology id available for tbox. An ontology IRI is required for the import into the abox.");
	}

	/**
	 * @throws OWLOntologyCreationException
	 */
	protected void init() throws OWLOntologyCreationException {
		// set default imports
		additionalImports = new HashSet<IRI>();
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
				addObsoleteImport(IRI.create(obsolete));
			}
		}
	}
	
	/**
	 * Mark the given imports as obsolete.
	 * 
	 * @param obsoleteImports
	 */
	public void addObsoleteImportIRIs(Collection<IRI> obsoleteImports) {
		if (obsoleteImports != null) {
			for (IRI obsoleteImport : obsoleteImports) {
				addObsoleteImport(obsoleteImport);
			}
		}
	}
	
	private void addObsoleteImport(IRI obsoleteImport) {
		// complete list
		obsoleteImports.add(obsoleteImport);
		
		// check if we need to create an empty importer
		final OWLOntologyManager m = graph.getManager();
		OWLOntology obsoleteOntology = m.getOntology(obsoleteImport);
		if (obsoleteOntology == null) {
			try {
				// add mapper to provide empty ontologies for obsolete imports
				OWLOntology empty = m.createOntology(obsoleteImport);
				obsoleteOntologies.put(obsoleteImport, empty);
			} catch (OWLOntologyCreationException e) {
				// ignore for now, just log as warning
				LOG.warn("Could not create empty dummy ontology for obsolete import: "+obsoleteImport, e);
			}
		}
	}
	
	public Collection<IRI> getImports() {
		Set<IRI> allImports = new HashSet<IRI>();
		allImports.add(tboxIRI);
		allImports.addAll(additionalImports);
		return allImports;
	}

	/**
	 * 
	 * @param modelId
	 * @return all individuals in the model
	 */
	public Set<OWLNamedIndividual> getIndividuals(String modelId) {
		ModelContainer mod = getModel(modelId);
		return mod.getAboxOntology().getIndividualsInSignature();
	}

	
	/**
	 * @param modelId
	 * @param q
	 * @return all individuals in the model that satisfy q
	 */
	public Set<OWLNamedIndividual> getIndividualsByQuery(String modelId, OWLClassExpression q) {
		ModelContainer mod = getModel(modelId);
		return mod.getReasoner().getInstances(q, false).getFlattened();
	}

	/**
	 * @param modelId
	 * @param model
	 * @param ce
	 * @param metadata
	 * @return individual
	 */
	public OWLNamedIndividual createIndividual(String modelId, ModelContainer model, OWLClassExpression ce, METADATA metadata) {
		OWLNamedIndividual individual = createIndividual(modelId, model, ce, null, true, metadata);
		return individual;
	}
	
	OWLNamedIndividual createIndividual(String modelId, ModelContainer model, OWLClassExpression ce, Set<OWLAnnotation> annotations, boolean flushReasoner, METADATA metadata) {
		LOG.info("Creating individual of type: "+ce);
		Pair<OWLNamedIndividual, Set<OWLAxiom>> pair = createIndividual(modelId, model.getAboxOntology(), ce, annotations);
		addAxioms(modelId, model, pair.getRight(), flushReasoner, metadata);
		return pair.getLeft();
	}
	
	public static Pair<OWLNamedIndividual, Set<OWLAxiom>> createIndividual(String modelId, OWLOntology abox, OWLClassExpression ce, Set<OWLAnnotation> annotations) {
		OWLGraphWrapper graph = new OWLGraphWrapper(abox);
		String iid;
		if (ce instanceof OWLClass) {
			String cid = graph.getIdentifier(ce).replaceAll(":","-"); // e.g. GO-0123456
			// Make something unique to tag onto the generated IDs.
			iid = generateId(modelId, "-", cid, "-");
		}
		else {
			iid = generateId(modelId, "-");
		}

		IRI iri = IdStringManager.getIRI(iid, graph);
		OWLDataFactory f = graph.getDataFactory();
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
		
		OWLClassAssertionAxiom typeAxiom = createType(f, i, ce);
		if (typeAxiom != null) {
			axioms.add(typeAxiom);
		}
		
		return Pair.of(i, axioms);
	}
	
	/**
	 * Deletes an individual and return all IRIs used as an annotation value
	 * 
	 * @param modelId
	 * @param model
	 * @param i
	 * @param metadata
	 * @return set of IRIs used in annotations
	 */
	public DeleteInformation deleteIndividual(String modelId, ModelContainer model, OWLNamedIndividual i, METADATA metadata) {
		return deleteIndividual(modelId, model, i, true, metadata);
	}
	
	public static class DeleteInformation {
		public final Set<IRI> usedIRIs = new HashSet<IRI>();
		public final Set<OWLObjectPropertyAssertionAxiom> updated = new HashSet<OWLObjectPropertyAssertionAxiom>();
		public final Set<IRI> touched = new HashSet<IRI>();
	}
	
	/**
	 * Deletes an individual and return all IRIs used as an annotation value.
	 * Also tries to delete all annotations (OWLObjectPropertyAssertionAxiom
	 * annotations and OWLAnnotationAssertionAxiom) with the individual IRI as
	 * value.
	 * 
	 * @param modelId
	 * @param model
	 * @param i
	 * @param flushReasoner
	 * @param metadata
	 * @return set of IRIs used in annotations
	 */
	DeleteInformation deleteIndividual(String modelId, ModelContainer model, OWLNamedIndividual i, boolean flushReasoner, METADATA metadata) {
		Set<OWLAxiom> toRemoveAxioms = new HashSet<OWLAxiom>();
		final DeleteInformation deleteInformation = new DeleteInformation();
		
		final OWLOntology ont = model.getAboxOntology();
		final OWLDataFactory f = model.getOWLDataFactory();
		
		// Declaration axiom
		toRemoveAxioms.add(model.getOWLDataFactory().getOWLDeclarationAxiom(i));
		
		// Logic axiom
		for (OWLAxiom ax : ont.getAxioms(i)) {
			extractIRIValues(ax.getAnnotations(), deleteInformation.usedIRIs);
			toRemoveAxioms.add(ax);
		}
		
		// OWLObjectPropertyAssertionAxiom
		Set<OWLObjectPropertyAssertionAxiom> allAssertions = ont.getAxioms(AxiomType.OBJECT_PROPERTY_ASSERTION);
		final IRI iIRI = i.getIRI();
		for (OWLObjectPropertyAssertionAxiom ax : allAssertions) {
			if (toRemoveAxioms.contains(ax) == false) {
				Set<OWLNamedIndividual> currentIndividuals = ax.getIndividualsInSignature();
				if (currentIndividuals.contains(i)) {
					extractIRIValues(ax.getAnnotations(), deleteInformation.usedIRIs);
					toRemoveAxioms.add(ax);
					continue;
				}
				// check annotations for deleted individual IRI
				Set<OWLAnnotation> annotations = ax.getAnnotations();
				Set<OWLAnnotation> removeAnnotations = new HashSet<OWLAnnotation>();
				for (OWLAnnotation annotation : annotations) {
					if (iIRI.equals(annotation.getValue())) {
						removeAnnotations.add(annotation);
					}
				}
				// if there is an annotations that needs to be removed, 
				// recreate axiom with cleaned annotation set
				if (removeAnnotations.isEmpty() == false) {
					annotations.removeAll(removeAnnotations);
					toRemoveAxioms.add(ax);
					deleteInformation.updated.add(f.
							getOWLObjectPropertyAssertionAxiom(
									ax.getProperty(), ax.getSubject(), ax.getObject(), annotations));
				}
			}
		}
		// OWLAnnotationAssertionAxiom
		Set<OWLAnnotationAssertionAxiom> annotationAssertionAxioms = ont.getAnnotationAssertionAxioms(i.getIRI());
		for (OWLAnnotationAssertionAxiom axiom : annotationAssertionAxioms) {
			extractIRIValues(axiom.getAnnotation(), deleteInformation.usedIRIs);
			toRemoveAxioms.add(axiom);	
		}
		
		// search for all annotations which use individual IRI as value
		Set<OWLAnnotationAssertionAxiom> axioms = ont.getAxioms(AxiomType.ANNOTATION_ASSERTION);
		for (OWLAnnotationAssertionAxiom ax : axioms) {
			if (toRemoveAxioms.contains(ax) == false) {
				if (iIRI.equals(ax.getValue())) {
					toRemoveAxioms.add(ax);
					OWLAnnotationSubject subject = ax.getSubject();
					subject.accept(new OWLAnnotationSubjectVisitor() {
						
						@Override
						public void visit(OWLAnonymousIndividual individual) {
							// do nothing
						}
						
						@Override
						public void visit(IRI iri) {
							// check if they subject is a declared named individual
							if (ont.containsIndividualInSignature(iri)) {
								deleteInformation.touched.add(iri);
							}
						}
					});
				}
			}
		}
		
		removeAxioms(modelId, model, toRemoveAxioms, flushReasoner, metadata);
		if (deleteInformation.updated.isEmpty() == false) {
			addAxioms(modelId, model, deleteInformation.updated, flushReasoner, metadata);
		}
		
		return deleteInformation;
	}
	
	public static Set<IRI> extractIRIValues(Set<OWLAnnotation> annotations) {
		if (annotations == null || annotations.isEmpty()) {
			return Collections.emptySet();
		}
		Set<IRI> iriSet = new HashSet<IRI>();
		extractIRIValues(annotations, iriSet);
		return iriSet;
	}
	
	private static void extractIRIValues(Set<OWLAnnotation> annotations, final Set<IRI> iriSet) {
		if (annotations != null) {
			for (OWLAnnotation annotation : annotations) {
				extractIRIValues(annotation, iriSet);
			}
		}
	}
	
	private static void extractIRIValues(OWLAnnotation annotation, final Set<IRI> iriSet) {
		if (annotation != null) {
			annotation.getValue().accept(new OWLAnnotationValueVisitor() {

				@Override
				public void visit(OWLLiteral literal) {
					// ignore
				}

				@Override
				public void visit(OWLAnonymousIndividual individual) {
					// ignore
				}

				@Override
				public void visit(IRI iri) {
					iriSet.add(iri);
				}
			});
		}
	}
	
	public void addAnnotations(String modelId, ModelContainer model, OWLNamedIndividual i, Collection<OWLAnnotation> annotations, METADATA metadata) {
		addAnnotations(modelId, model, i.getIRI(), annotations, metadata);
	}
	
	void addAnnotations(String modelId, ModelContainer model, IRI subject, Collection<OWLAnnotation> annotations, METADATA metadata) {
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		OWLDataFactory f = model.getOWLDataFactory();
		for (OWLAnnotation annotation : annotations) {
			axioms.add(f.getOWLAnnotationAssertionAxiom(subject, annotation));
		}
		addAxioms(modelId, model, axioms, false, metadata);
	}
	
	void updateAnnotation(String modelId, ModelContainer model, IRI subject, OWLAnnotation update, METADATA metadata) {
		Set<OWLAxiom> removeAxioms = new HashSet<OWLAxiom>();
		OWLDataFactory f = model.getOWLDataFactory();
		Set<OWLAnnotationAssertionAxiom> existing = model.getAboxOntology().getAnnotationAssertionAxioms(subject);
		OWLAnnotationProperty target = update.getProperty();
		for (OWLAnnotationAssertionAxiom axiom : existing) {
			if (target.equals(axiom.getProperty())) {
				removeAxioms.add(axiom);
			}
		}
		removeAxioms(modelId, model, removeAxioms, false, metadata);
		addAxiom(modelId, model, f.getOWLAnnotationAssertionAxiom(subject, update), false, metadata);
	}
	
	void addAnnotations(String modelId, ModelContainer model, Collection<OWLAnnotation> annotations, METADATA metadata) {
		OWLOntology aBox = model.getAboxOntology();
		List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
		for (OWLAnnotation annotation : annotations) {
			changes.add(new AddOntologyAnnotation(aBox, annotation));
		}
		applyChanges(modelId, model, changes, false, metadata);
	}
	
	void updateAnnotation(String modelId, ModelContainer model, OWLAnnotation update, METADATA metadata) {
		OWLOntology aBox = model.getAboxOntology();
		List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
		Set<OWLAnnotation> existing = model.getAboxOntology().getAnnotations();
		OWLAnnotationProperty target = update.getProperty();
		for (OWLAnnotation annotation : existing) {
			if (target.equals(annotation.getProperty())) {
				changes.add(new RemoveOntologyAnnotation(aBox, annotation));
			}
		}
		changes.add(new AddOntologyAnnotation(aBox, update));
		applyChanges(modelId, model, changes, false, metadata);
	}

	public void removeAnnotations(String modelId, ModelContainer model, OWLNamedIndividual i, Collection<OWLAnnotation> annotations, METADATA metadata) {
		removeAnnotations(modelId, model, i.getIRI(), annotations, metadata);
	}
	
	void removeAnnotations(String modelId, ModelContainer model, IRI subject, Collection<OWLAnnotation> annotations, METADATA metadata) {
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

	void removeAnnotations(String modelId, ModelContainer model, Collection<OWLAnnotation> annotations, METADATA metadata) {
		OWLOntology aBox = model.getAboxOntology();
		List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
		for (OWLAnnotation annotation : annotations) {
			changes.add(new RemoveOntologyAnnotation(aBox, annotation));
		}
		applyChanges(modelId, model, changes, false, metadata);
	}
	
	public void addDataProperty(String modelId, ModelContainer model,
			OWLNamedIndividual i, OWLDataProperty prop, OWLLiteral literal,
			boolean flushReasoner, METADATA metadata) {
		OWLAxiom axiom = model.getOWLDataFactory().getOWLDataPropertyAssertionAxiom(prop, i, literal);
		addAxiom(modelId, model, axiom, flushReasoner, metadata);
	}
	
	public void removeDataProperty(String modelId, ModelContainer model,
			OWLNamedIndividual i, OWLDataProperty prop, OWLLiteral literal,
			boolean flushReasoner, METADATA metadata) {
		OWLAxiom toRemove = null;
		Set<OWLDataPropertyAssertionAxiom> existing = model.getAboxOntology().getDataPropertyAssertionAxioms(i);
		for (OWLDataPropertyAssertionAxiom ax : existing) {
			if (prop.equals(ax.getProperty()) && literal.equals(ax.getObject())) {
				toRemove = ax;
				break;
			}
		}
		
		if (toRemove != null) {
			removeAxiom(modelId, model, toRemove, flushReasoner, metadata);
		}
	}
	
	/**
	 * Fetches a model by its Id
	 * 
	 * @param id
	 * @return wrapped model
	 */
	public ModelContainer getModel(String id)  {
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
	 * Retrieve the abox ontology. May skip loading the imports.
	 * This method is mostly intended to read metadata from a model.
	 * 
	 * @param id
	 * @return abox, maybe without any imports loaded
	 */
	public OWLOntology getModelAbox(String id) {
		ModelContainer model = modelMap.get(id);
		if (model != null) {
			return model.getAboxOntology();
		}
		OWLOntology abox = null;
		try {
			abox = loadModelABox(id);
		} catch (OWLOntologyCreationException e) {
			LOG.info("Could not load model with id: "+id, e);
		}
		return abox;
	}
	
	/**
	 * @param modelId
	 * @return ontology
	 * @throws OWLOntologyCreationException
	 */
	protected abstract OWLOntology loadModelABox(String modelId) throws OWLOntologyCreationException;
	
	/**
	 * @param id
	 */
	public void unlinkModel(String id) {
		ModelContainer model = modelMap.get(id);
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

	private synchronized SimpleEcoMapper getSimpleEcoMapper() throws IOException {
		if (simpleEcoMapper == null) {
			simpleEcoMapper = EcoMapperFactory.createSimple();
		}
		return simpleEcoMapper;
	}
	
	/**
	 * Export the model (ABox) in a legacy format, such as GAF or GPAD.
	 * 
	 * @param modelId
	 * @param model
	 * @param format format name or null for default
	 * @return modelContent
	 * @throws IOException
	 */
	public String exportModelLegacy(String modelId, ModelContainer model, String format) throws IOException {
		final OWLOntology aBox = model.getAboxOntology();
		SimpleEcoMapper ecoMapper = getSimpleEcoMapper();
		LegoToGeneAnnotationTranslator translator = new LegoToGeneAnnotationTranslator(graph, model.getReasoner(), ecoMapper);
		Pair<GafDocument,BioentityDocument> pair = translator.translate(modelId, aBox, null);
		ByteArrayOutputStream outputStream = null;
		try {
			outputStream = new ByteArrayOutputStream();
			if (format == null || "gaf".equalsIgnoreCase(format)) {
				// GAF
				GafWriter writer = new GafWriter();
				try {
					writer.setStream(new PrintStream(outputStream));
					GafDocument gafdoc = pair.getLeft();
					writer.write(gafdoc);
				}
				finally {
					writer.close();
				}

			}
			else if ("gpad".equalsIgnoreCase(format)) {
				// GPAD version 1.2
				GpadWriter writer = new GpadWriter(new PrintWriter(outputStream) , 1.2);
				writer.write(pair.getLeft());
			}
			else {
				throw new IOException("Unknown legacy format: "+format);
			}
			return outputStream.toString();
		}
		finally {
			IOUtils.closeQuietly(outputStream);
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
	public String exportModel(String modelId, ModelContainer model, OWLOntologyFormat ontologyFormat) throws OWLOntologyStorageException {
		final OWLOntology aBox = model.getAboxOntology();
		final OWLOntologyManager manager = aBox.getOWLOntologyManager();
		
		// make sure the exported ontology has an ontologyId and that it maps to the modelId
		final IRI expectedABoxIRI = IdStringManager.getIRI(modelId, graph);
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
			String existingModelId = IdStringManager.getId(id.getOntologyIRI());

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
				modelId = IdStringManager.getId(iri);
			}
		}
		if (modelId == null) {
			throw new OWLOntologyCreationException("Could not extract the modelId from the given model");
		}
		// paranoia check
		ModelContainer existingModel = modelMap.get(modelId);
		if (existingModel != null) {
			unlinkModel(modelId);
		}
		
		// add to internal model
		ModelContainer newModel = addModel(modelId, modelOntology);
		
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

	ModelContainer addModel(String modelId, OWLOntology abox) throws OWLOntologyCreationException {
		OWLOntology tbox = graph.getSourceOntology();
		ModelContainer m = new ModelContainer(tbox, abox);
		modelMap.put(modelId, m);
		return m;
	}

	
	/**
	 * 
	 * @param modelId
	 * @return true if the ontology formed by the specified model is inconsistent
	 */
	public boolean isConsistent(String modelId) {
		ModelContainer model = getModel(modelId);
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
		ModelContainer model = getModel(modelId);
		return model.getOWLDataFactory();
	}

	protected OWLOntologyManager getOWLOntologyManager(String modelId) {
		ModelContainer model = getModel(modelId);
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
		ModelContainer model = getModel(modelId);
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
	void addType(String modelId, ModelContainer model, OWLIndividual i, 
			OWLClassExpression c, boolean flushReasoner, METADATA metadata) {
		OWLClassAssertionAxiom axiom = createType(model.getOWLDataFactory(), i, c);
		addAxiom(modelId, model, axiom, flushReasoner, metadata);
	}

	/**
	 * @param f
	 * @param i
	 * @param c
	 * @return axiom
	 */
	public static OWLClassAssertionAxiom createType(OWLDataFactory f, OWLIndividual i, OWLClassExpression c) {
		OWLClassAssertionAxiom axiom = f.getOWLClassAssertionAxiom(c,i);
		return axiom;
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
		ModelContainer model = getModel(modelId);
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
			ModelContainer model,
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
		ModelContainer model = getModel(modelId);
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
	void removeType(String modelId, ModelContainer model, OWLIndividual i, 
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
		ModelContainer model = getModel(modelId);
		removeType(modelId, model, i, p, filler, true, metadata);
	}
	
	
	void removeType(String modelId,
			ModelContainer model,
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
		ModelContainer model = getModel(modelId);
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
		ModelContainer model = getModel(modelId);
		OWLObjectProperty p = vocabElement.getObjectProperty(model.getAboxOntology());
		addFact(modelId, model, p, i, j, annotations, true, metadata);
	}

	void addFact(String modelId, ModelContainer model, OWLObjectPropertyExpression p,
			OWLIndividual i, OWLIndividual j, Set<OWLAnnotation> annotations, boolean flushReasoner, METADATA metadata) {
		OWLObjectPropertyAssertionAxiom axiom = createFact(model.getOWLDataFactory(), p, i,	j, annotations);
		addAxiom(modelId, model, axiom, flushReasoner, metadata);
	}

	/**
	 * @param f
	 * @param p
	 * @param i
	 * @param j
	 * @param annotations
	 * @return axiom
	 */
	public static OWLObjectPropertyAssertionAxiom createFact(OWLDataFactory f,
			OWLObjectPropertyExpression p, OWLIndividual i, OWLIndividual j,
			Set<OWLAnnotation> annotations) {
		final OWLObjectPropertyAssertionAxiom axiom;
		if (annotations != null && !annotations.isEmpty()) {
			axiom = f.getOWLObjectPropertyAssertionAxiom(p, i, j, annotations);	
		}
		else {
			axiom = f.getOWLObjectPropertyAssertionAxiom(p, i, j);
		}
		return axiom;
	}

	public void removeFact(String modelId, OWLObjectPropertyExpression p,
			OWLNamedIndividual i, OWLNamedIndividual j, METADATA metadata) {
		ModelContainer model = getModel(modelId);
		removeFact(modelId, model, p, i, j, true, metadata);
	}

	Set<IRI> removeFact(String modelId, ModelContainer model, OWLObjectPropertyExpression p,
			OWLIndividual i, OWLIndividual j, boolean flushReasoner, METADATA metadata) {
		OWLDataFactory f = model.getOWLDataFactory();
		
		OWLOntology ont = model.getAboxOntology();
		OWLAxiom toRemove = null;
		Set<IRI> iriSet = new HashSet<IRI>();
		Set<OWLObjectPropertyAssertionAxiom> candidates = ont.getObjectPropertyAssertionAxioms(i);
		for (OWLObjectPropertyAssertionAxiom axiom : candidates) {
			if (p.equals(axiom.getProperty()) && j.equals(axiom.getObject())) {
				toRemove = axiom;
				extractIRIValues(axiom.getAnnotations(), iriSet);
				break;
			}
		}
		if (toRemove == null) {
			// fall back solution
			toRemove = f.getOWLObjectPropertyAssertionAxiom(p, i, j);
		}
		removeAxiom(modelId, model, toRemove, flushReasoner, metadata);
		return iriSet;
	}
	
	public void addAnnotations(String modelId, OWLObjectPropertyExpression p,
			OWLNamedIndividual i, OWLNamedIndividual j, Set<OWLAnnotation> annotations, METADATA metadata) {
		ModelContainer model = getModel(modelId);
		addAnnotations(modelId, model, p, i, j, annotations, true, metadata);
	}
	
	void addAnnotations(String modelId, ModelContainer model, OWLObjectPropertyExpression p,
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
		addAnnotations(modelId, model, toModify, annotations, flushReasoner, metadata);
	}
	
	void addAnnotations(String modelId, ModelContainer model, OWLObjectPropertyAssertionAxiom toModify,
			Set<OWLAnnotation> annotations, boolean flushReasoner, METADATA metadata) {
		if (toModify != null) {
			Set<OWLAnnotation> combindedAnnotations = new HashSet<OWLAnnotation>(annotations);
			combindedAnnotations.addAll(toModify.getAnnotations());
			modifyAnnotations(toModify, combindedAnnotations, modelId, model, flushReasoner, metadata);
		}
	}
	
	void updateAnnotation(String modelId, ModelContainer model, OWLObjectPropertyExpression p,
			OWLNamedIndividual i, OWLNamedIndividual j, OWLAnnotation update,
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
		updateAnnotation(modelId, model, toModify, update, flushReasoner, metadata);
	}
	
	OWLObjectPropertyAssertionAxiom updateAnnotation(String modelId, ModelContainer model, 
			OWLObjectPropertyAssertionAxiom toModify, OWLAnnotation update,
			boolean flushReasoner, METADATA metadata) {
		OWLObjectPropertyAssertionAxiom newAxiom = null;
		if (toModify != null) {
			Set<OWLAnnotation> combindedAnnotations = new HashSet<OWLAnnotation>();
			OWLAnnotationProperty target = update.getProperty();
			for(OWLAnnotation existing : toModify.getAnnotations()) {
				if (target.equals(existing.getProperty()) == false) {
					combindedAnnotations.add(existing);
				}
			}
			combindedAnnotations.add(update);
			newAxiom = modifyAnnotations(toModify, combindedAnnotations, modelId, model, flushReasoner, metadata);
		}
		return newAxiom;
	}
	
	OWLObjectPropertyAssertionAxiom removeAnnotations(String modelId, ModelContainer model, OWLObjectPropertyExpression p,
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
		OWLObjectPropertyAssertionAxiom newAxiom = null;
		if (toModify != null) {
			Set<OWLAnnotation> combindedAnnotations = new HashSet<OWLAnnotation>(toModify.getAnnotations());
			combindedAnnotations.removeAll(annotations);
			newAxiom = modifyAnnotations(toModify, combindedAnnotations, modelId, model, flushReasoner, metadata);
		}
		return newAxiom;
	}
	
	private OWLObjectPropertyAssertionAxiom modifyAnnotations(OWLObjectPropertyAssertionAxiom axiom, 
			Set<OWLAnnotation> replacement, 
			String modelId, ModelContainer model, boolean flushReasoner, METADATA metadata) {
		OWLOntology ont = model.getAboxOntology();
		OWLDataFactory f = model.getOWLDataFactory();
		List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>(2);
		changes.add(new RemoveAxiom(ont, axiom));
		OWLObjectPropertyAssertionAxiom newAxiom = 
				f.getOWLObjectPropertyAssertionAxiom(axiom.getProperty(), axiom.getSubject(), axiom.getObject(), replacement);
		changes.add(new AddAxiom(ont, newAxiom));
		applyChanges(modelId, model, changes, flushReasoner, metadata);
		return newAxiom;
	}
	
	public void addAxiom(String modelId, ModelContainer model, OWLAxiom axiom, 
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
	
	void addAxioms(String modelId, ModelContainer model, Set<? extends OWLAxiom> axioms, 
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
	
	void removeAxiom(String modelId, ModelContainer model, OWLAxiom axiom, 
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
		ModelContainer model = getModel(modelId);
		removeAxioms(modelId, model, axioms, flushReasoner, metadata);
	}
	
	void removeAxioms(String modelId, ModelContainer model, Set<OWLAxiom> axioms, 
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

	private void applyChanges(String modelId, ModelContainer model, 
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
	
	private void applyChanges(String modelId, ModelContainer model, OWLOntologyManager m, 
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
	protected void addToHistory(String modelId, ModelContainer model, 
			List<OWLOntologyChange> appliedChanges, METADATA metadata) {
		// do nothing, for now
	}
	
	protected OWLOntology loadOntologyIRI(final IRI sourceIRI, boolean minimal) throws OWLOntologyCreationException {
		// silence the OBO parser in the OWL-API
		java.util.logging.Logger.getLogger("org.obolibrary").setLevel(java.util.logging.Level.SEVERE);
		
		// load model from source
		OWLOntologyDocumentSource source = new IRIDocumentSource(sourceIRI);
		if (minimal == false) {
			// add the obsolete imports to the ignored imports
			OWLOntology abox = graph.getManager().loadOntologyFromOntologyDocument(source);
			return abox;
		}
		else {
			// only load the model, skip imports
			// approach: return an empty ontology IRI for any IRI mapping request using.
			final OWLOntologyManager m = OWLManager.createOWLOntologyManager();
			final Set<IRI> emptyOntologies = new HashSet<IRI>();
			m.addIRIMapper(new OWLOntologyIRIMapper() {
				
				@Override
				public IRI getDocumentIRI(IRI ontologyIRI) {
					
					// quick check:
					// do nothing for the original IRI and known empty ontologies
					if (sourceIRI.equals(ontologyIRI) || emptyOntologies.contains(ontologyIRI)) {
						return null;
					}
					emptyOntologies.add(ontologyIRI);
					try {
						OWLOntology emptyOntology = m.createOntology(ontologyIRI);
						return emptyOntology.getOntologyID().getDefaultDocumentIRI();
					} catch (OWLOntologyCreationException e) {
						throw new RuntimeException(e);
					}
				}
			});
			OWLOntology minimalAbox = m.loadOntologyFromOntologyDocument(source);
			return minimalAbox;
		}
	}
	
	/**
	 * This method will check the given model and update the import declarations.
	 * It will add missing IRIs and remove obsolete ones.
	 * 
	 * @param modelId 
	 * @param model
	 * @see #additionalImports
	 * @see #addImports(Iterable)
	 * @see #obsoleteOntologies
	 * @see #addObsoleteImports(Iterable)
	 */
	public void updateImports(String modelId, ModelContainer model) {
		updateImports(model);
	}
	
	private void updateImports(ModelContainer model) {
		updateImports(model.getAboxOntology(), tboxIRI, additionalImports, obsoleteImports);
	}
	
	static void updateImports(final OWLOntology aboxOntology, IRI tboxIRI, Set<IRI> additionalImports, Set<IRI> obsoleteImports) {
		List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
		
		Set<IRI> missingImports = new HashSet<IRI>();
		missingImports.add(tboxIRI);
		missingImports.addAll(additionalImports);
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
