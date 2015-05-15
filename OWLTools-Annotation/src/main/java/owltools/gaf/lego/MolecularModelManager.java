package owltools.gaf.lego;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.semanticweb.owlapi.expression.ParserException;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import owltools.graph.OWLGraphWrapper;
import owltools.util.ModelContainer;
import owltools.vocab.OBOUpperVocabulary;

/**
 * Convenience layer for operations on collections of MolecularModels (aka lego diagrams)
 * 
 * This manager is intended to be used within a web server. Multiple clients can
 * contact the same manager instance through services
 * 
 * @param <METADATA> 
 * @see CoreMolecularModelManager
 * @see FileBasedMolecularModelManager
 */
public class MolecularModelManager<METADATA> extends FileBasedMolecularModelManager<METADATA> {
	
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
		super(graph);
	}

	/**
	 * wrapper for {@link #generateModel(OWLClass, String, Object)}
	 * 
	 * @param pid
	 * @param db
	 * @param metadata 
	 * @return modelId
	 * @throws OWLOntologyCreationException
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws UnknownIdentifierException
	 */
	public String generateModel(String pid, String db, METADATA metadata) throws OWLOntologyCreationException, IOException, URISyntaxException, UnknownIdentifierException {
		OWLClass cls = getClass(pid, graph);
		if (cls == null) {
			throw new UnknownIdentifierException("Could not find a class for id: "+pid);
		}
		return generateModel(cls, db, metadata);
	}

//	/**
//	 * Given an instance, generate the most specific class instance that classifies
//	 * this instance, and add this as a class to the model ontology
//	 * 
//	 * @param modelId
//	 * @param individualId
//	 * @param newClassId
//	 * @param metadata
//	 * @return newClassId
//	 * @throws UnknownIdentifierException 
//	 */
//	public String createMostSpecificClass(String modelId, String individualId, String newClassId, METADATA metadata) throws UnknownIdentifierException {
//		ModelContainer mod = checkModelId(modelId);
//		OWLIndividual ind = getIndividual(individualId, mod);
//		OWLClassExpression msce = mod.getMostSpecificClassExpression((OWLNamedIndividual) ind);
//		OWLClass c = getClass(newClassId, mod);
//		addAxiom(modelId, mod, mod.getOWLDataFactory().getOWLEquivalentClassesAxiom(msce, c), true, metadata);
//		return newClassId;
//	}

//	/**
//	 * Adds a process individual (and inferred individuals) to a model
//	 * 
//	 * @param modelId
//	 * @param processCls
//	 * @return null
//	 * @throws OWLOntologyCreationException
//	 * @throws UnknownIdentifierException
//	 * 
//	 * @Deprecated problematic return type
//	 */
//	@Deprecated
//	public String addProcess(String modelId, OWLClass processCls) throws OWLOntologyCreationException, UnknownIdentifierException {
//		ModelContainer mod = checkModelId(modelId);
//		Set<String> genes = new HashSet<String>();
//		mod.buildNetwork(processCls, genes);
//		return null;
//	}

	/**
	 * @param modelId
	 * @param qs
	 * @return all individuals in the model that satisfy q
	 * @throws UnknownIdentifierException
	 */
	public Set<OWLNamedIndividual> getIndividualsByQuery(String modelId, String qs) throws UnknownIdentifierException {
		ModelContainer mod = checkModelId(modelId);
		ManchesterSyntaxTool mst = new ManchesterSyntaxTool(new OWLGraphWrapper(mod.getAboxOntology()), false);
		OWLClassExpression q = mst.parseManchesterExpression(qs);
		return getIndividualsByQuery(modelId, q);
	}

	/**
	 * Shortcut for {@link CoreMolecularModelManager#createIndividual}
	 * 
	 * @param modelId
	 * @param cid
	 * @param annotations
	 * @param metadata
	 * @return id and individual
	 * @throws UnknownIdentifierException 
	 */
	public Pair<String, OWLNamedIndividual> createIndividual(String modelId, String cid, Set<OWLAnnotation> annotations, METADATA metadata) throws UnknownIdentifierException {
		ModelContainer model = checkModelId(modelId);
		OWLClass cls = getClass(cid, model);
		if (cls == null) {
			throw new UnknownIdentifierException("Could not find a class for id: "+cid);
		}
		OWLNamedIndividual individual = createIndividual(modelId, model, cls, annotations , true, metadata);
		return Pair.of(IdStringManager.getId(individual.getIRI()), individual);
	}
	
	
	/**
	 * Shortcut for {@link CoreMolecularModelManager#createIndividual}.
	 * 
	 * @param modelId
	 * @param cid
	 * @param annotations
	 * @param metadata
	 * @return id and created individual
	 * @throws UnknownIdentifierException 
	 */
	public Pair<String, OWLNamedIndividual> createIndividualNonReasoning(String modelId, String cid, Set<OWLAnnotation> annotations, METADATA metadata) throws UnknownIdentifierException {
		ModelContainer model = checkModelId(modelId);
		OWLClass cls = getClass(cid, model);
		if (cls == null) {
			throw new UnknownIdentifierException("Could not find a class for id: "+cid);
		}
		return createIndividualNonReasoning(modelId, cls, annotations, metadata);
	}
	
	/**
	 * Shortcut for {@link CoreMolecularModelManager#createIndividual}.
	 * 
	 * @param modelId
	 * @param ce
	 * @param annotations
	 * @param metadata
	 * @return id and created individual
	 * @throws UnknownIdentifierException 
	 */
	public Pair<String, OWLNamedIndividual> createIndividualNonReasoning(String modelId, OWLClassExpression ce, Set<OWLAnnotation> annotations, METADATA metadata) throws UnknownIdentifierException {
		ModelContainer model = checkModelId(modelId);
		OWLNamedIndividual i = createIndividual(modelId, model, ce, annotations, false, metadata);
		return Pair.of(IdStringManager.getId(i.getIRI()), i);
	}

	public OWLNamedIndividual getNamedIndividual(String modelId, String iid) throws UnknownIdentifierException {
		ModelContainer model = checkModelId(modelId);
		OWLNamedIndividual i = getIndividual(iid, model);
		if (i == null) {
			throw new UnknownIdentifierException("Could not find a individual for id: "+iid);
		}
		return i;
	}
	
	/**
	 * Deletes an individual and return all IRIs used as an annotation value
	 * 
	 * @param modelId
	 * @param iid
	 * @param metadata
	 * @return delete information
	 * @throws UnknownIdentifierException
	 */
	public DeleteInformation deleteIndividual(String modelId, String iid, METADATA metadata) throws UnknownIdentifierException {
		ModelContainer model = checkModelId(modelId);
		OWLNamedIndividual i = getIndividual(iid, model);
		if (i == null) {
			throw new UnknownIdentifierException("Could not find a individual for id: "+iid);
		}
		return deleteIndividual(modelId, model, i, true, metadata);
	}
	/**
	 * Deletes an individual and return all IRIs used as an annotation value
	 * 
	 * @param modelId
	 * @param iid
	 * @param metadata
	 * @return delete information
	 * @throws UnknownIdentifierException
	 */
	public DeleteInformation deleteIndividualNonReasoning(String modelId, String iid, METADATA metadata) throws UnknownIdentifierException {
		ModelContainer model = checkModelId(modelId);
		OWLNamedIndividual i = getIndividual(iid, model);
		if (i == null) {
			throw new UnknownIdentifierException("Could not find a individual for id: "+iid);
		}
		return deleteIndividual(modelId, model, i, false, metadata);
	}
	
	/**
	 * Deletes an individual
	 * 
	 * @param modelId
	 * @param iri
	 * @param metadata
	 * @throws UnknownIdentifierException
	 */
	public void deleteIndividualNonReasoning(String modelId, IRI iri, METADATA metadata) throws UnknownIdentifierException {
		ModelContainer model = checkModelId(modelId);
		OWLNamedIndividual i = getIndividual(iri, model);
		if (i == null) {
			throw new UnknownIdentifierException("Could not find a individual for id: "+iri);
		}
		deleteIndividual(modelId, model, i, false, metadata);
	}
	
	public void addAnnotations(String modelId, Set<OWLAnnotation> annotations, METADATA metadata)
			throws UnknownIdentifierException {
		ModelContainer model = checkModelId(modelId);
		if (annotations != null && !annotations.isEmpty()) {
			addAnnotations(modelId, model, annotations, metadata);
		}
	}
	
	public void updateAnnotation(String modelId, OWLAnnotation annotation, METADATA metadata)
			throws UnknownIdentifierException {
		ModelContainer model = checkModelId(modelId);
		if (annotation != null) {
			updateAnnotation(modelId, model, annotation, metadata);
		}
	}
	
	public OWLNamedIndividual addAnnotations(String modelId, String iid, 
			Set<OWLAnnotation> annotations, METADATA metadata) throws UnknownIdentifierException {
		ModelContainer model = checkModelId(modelId);
		OWLNamedIndividual i = getIndividual(iid, model);
		if (i == null) {
			throw new UnknownIdentifierException("Could not find a individual for id: "+iid);
		}
		if (annotations != null && !annotations.isEmpty()) {
			addAnnotations(modelId, model, i.getIRI(), annotations, metadata);
		}
		return i;
	}
	
	public void addAnnotations(String modelId, IRI subject, 
			Set<OWLAnnotation> annotations, METADATA metadata) throws UnknownIdentifierException {
		if (annotations != null && !annotations.isEmpty()) {
			ModelContainer model = checkModelId(modelId);
			addAnnotations(modelId, model, subject, annotations, metadata);
		}
	}
	
	public OWLNamedIndividual updateAnnotation(String modelId, String iid, 
			OWLAnnotation annotation, METADATA metadata) throws UnknownIdentifierException {
		ModelContainer model = checkModelId(modelId);
		OWLNamedIndividual i = getIndividual(iid, model);
		if (i == null) {
			throw new UnknownIdentifierException("Could not find a individual for id: "+iid);
		}
		if (annotation != null) {
			updateAnnotation(modelId, model, i.getIRI(), annotation, metadata);
		}
		return i;
	}
	
	public void updateAnnotation(String modelId, IRI subject, 
			OWLAnnotation annotation, METADATA metadata) throws UnknownIdentifierException {
		ModelContainer model = checkModelId(modelId);
		if (annotation != null) {
			updateAnnotation(modelId, model, subject, annotation, metadata);
		}
	}
	
	public OWLNamedIndividual removeAnnotations(String modelId, String iid,
			Set<OWLAnnotation> annotations, METADATA metadata) throws UnknownIdentifierException {
		ModelContainer model = checkModelId(modelId);
		OWLNamedIndividual i = getIndividual(iid, model);
		if (i == null) {
			throw new UnknownIdentifierException("Could not find a individual for id: "+iid);
		}
		if (annotations != null && !annotations.isEmpty()) {
			removeAnnotations(modelId, model, i.getIRI(), annotations, metadata);
		}
		return i;
	}
	
	public void removeAnnotations(String modelId, Set<OWLAnnotation> annotations, METADATA metadata)
			throws UnknownIdentifierException {
		ModelContainer model = checkModelId(modelId);
		if (annotations != null && !annotations.isEmpty()) {
			removeAnnotations(modelId, model, annotations, metadata);
		}
	}
	
	/**
	 * @param id
	 */
	public void deleteModel(String id) {
		// TODO - retrieve from persistent store
		modelMap.remove(id);
	}
	
	public Set<String> searchModels(Collection<String> ids) throws IOException {
		final Set<String> resultSet = new HashSet<String>();
		// create IRIs
		Set<IRI> searchIRIs = new HashSet<IRI>();
		for(String id : ids) {
			searchIRIs.add(graph.getIRIByIdentifier(id));
		}
		
		if (!searchIRIs.isEmpty()) {
			// search for IRI usage in models
			final Set<String> allModelIds = getAvailableModelIds();
			for (final String modelId : allModelIds) {
				final ModelContainer model = getModel(modelId);
				final OWLOntology aboxOntology = model.getAboxOntology();
				Set<OWLEntity> signature = aboxOntology.getSignature();
				for (OWLEntity entity : signature) {
					if (searchIRIs.contains(entity.getIRI())) {
						resultSet.add(modelId);
						break;
					}
				}
			}
		}
		// return results
		return resultSet;
	}
	
	/**
	 * Save a model to disk.
	 * 
	 * @param modelId 
	 * @param annotations 
	 * @param metadata
	 *
	 * @throws OWLOntologyStorageException 
	 * @throws OWLOntologyCreationException 
	 * @throws IOException
	 * @throws UnknownIdentifierException
	 */
	public void saveModel(String modelId, Set<OWLAnnotation> annotations, METADATA metadata) throws OWLOntologyStorageException, OWLOntologyCreationException, IOException, UnknownIdentifierException {
		ModelContainer m = checkModelId(modelId);
		saveModel(modelId, m , annotations, metadata);
	}
	
	/**
	 * Export the ABox for the given modelId in the default {@link OWLOntologyFormat}.
	 * 
	 * @param modelId
	 * @return modelContent
	 * @throws OWLOntologyStorageException
	 * @throws UnknownIdentifierException 
	 */
	public String exportModel(String modelId) throws OWLOntologyStorageException, UnknownIdentifierException {
		ModelContainer model = checkModelId(modelId);
		return exportModel(modelId, model);
	}
	
	/**
	 * Export the model (ABox) for the given modelId in a legacy format, such as GAF or GPAD.
	 * 
	 * @param modelId
	 * @param format name or null for default
	 * @return model data in legacy format
	 * @throws UnknownIdentifierException 
	 * @throws IOException
	 */
	public String exportModelLegacy(String modelId, String format) throws UnknownIdentifierException, IOException {
		ModelContainer model = checkModelId(modelId);
		return exportModelLegacy(modelId, model, format);
	}
	
	private OWLNamedIndividual getIndividual(String indId, ModelContainer model) {
		OWLGraphWrapper graph = new OWLGraphWrapper(model.getAboxOntology());
		IRI iri = IdStringManager.getIRI(indId, graph);
		return getIndividual(iri, model);
	}
	private OWLNamedIndividual getIndividual(IRI iri, ModelContainer model) {
		// check that individual is actually declared
		boolean containsIRI = model.getAboxOntology().containsEntityInSignature(iri);
		if (containsIRI == false) {
			return null;
		}
		OWLNamedIndividual individual = model.getOWLDataFactory().getOWLNamedIndividual(iri);
		return individual;
	}
	private OWLClass getClass(String cid, ModelContainer model) {
		OWLGraphWrapper graph = new OWLGraphWrapper(model.getAboxOntology());
		return getClass(cid, graph);
	}
	private OWLClass getClass(String cid, OWLGraphWrapper graph) {
		IRI iri = IdStringManager.getIRI(cid, graph);
		return graph.getOWLClass(iri);
	}
	@Deprecated
	private OWLClass getGeneClass(String cid, ModelContainer model) {
		OWLGraphWrapper graph = new OWLGraphWrapper(model.getAboxOntology());
		IRI iri = IdStringManager.getIRI(cid, graph);
		return model.getOWLDataFactory().getOWLClass(iri);
	}
	private OWLObjectProperty getObjectProperty(String pid, ModelContainer model) {
		OWLGraphWrapper graph = new OWLGraphWrapper(model.getAboxOntology());
		IRI iri = IdStringManager.getIRI(pid, graph);
		return graph.getOWLObjectProperty(iri);
	}
	
	public ModelContainer checkModelId(String modelId) throws UnknownIdentifierException {
		ModelContainer model = getModel(modelId);
		if (model == null) {
			throw new UnknownIdentifierException("Could not find a model for id: "+modelId);
		}
		return model;
	}

	private OWLObjectPropertyExpression getObjectProperty(OBOUpperVocabulary vocabElement,
			ModelContainer model) {
		return vocabElement.getObjectProperty(model.getAboxOntology());
	}

	/**
	 * Convenience wrapper for {@link CoreMolecularModelManager#addType}
	 * 
	 * @param modelId
	 * @param iid
	 * @param cid
	 * @param metadata
	 * @throws UnknownIdentifierException 
	 */
	public void addType(String modelId, String iid, String cid, METADATA metadata) throws UnknownIdentifierException {
		ModelContainer model = checkModelId(modelId);
		OWLNamedIndividual individual = getIndividual(iid, model);
		if (individual == null) {
			throw new UnknownIdentifierException("Could not find a individual for id: "+iid);
		}
		OWLClass cls = getClass(cid, model);
		if (cls == null) {
			throw new UnknownIdentifierException("Could not find a class for id: "+cid);
		}
		addType(modelId, model, individual, cls, true, metadata);
	}
	
	/**
	 * @param modelId
	 * @param iid
	 * @param clsExp
	 * @param metadata
	 * @return individual
	 * @throws UnknownIdentifierException 
	 */
	public OWLNamedIndividual addTypeNonReasoning(String modelId, String iid, OWLClassExpression clsExp, METADATA metadata) throws UnknownIdentifierException {
		ModelContainer model = checkModelId(modelId);
		OWLNamedIndividual individual = getIndividual(iid, model);
		if (individual == null) {
			throw new UnknownIdentifierException("Could not find a individual for id: "+iid);
		}
		addType(modelId, model, individual, clsExp, false, metadata);
		return individual;
	}
	
	/**
	 * Convenience wrapper for {@link CoreMolecularModelManager#addType}.
	 * 
	 * @param modelId
	 * @param iid
	 * @param pid
	 * @param cid
	 * @param metadata
	 * @throws UnknownIdentifierException 
	 */
	public void addType(String modelId,
			String iid, String pid, String cid, METADATA metadata) throws UnknownIdentifierException {
		ModelContainer model = checkModelId(modelId);
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
		addType(modelId, model, individual, property, cls, true, metadata);
	}
	
	public OWLNamedIndividual addTypeNonReasoning(String modelId,
			String iid, String pid, OWLClassExpression ce, METADATA metadata) throws UnknownIdentifierException {
		ModelContainer model = checkModelId(modelId);
		OWLNamedIndividual individual = getIndividual(iid, model);
		if (individual == null) {
			throw new UnknownIdentifierException("Could not find a individual for id: "+iid);
		}
		OWLObjectProperty property = getObjectProperty(pid, model);
		if (property == null) {
			throw new UnknownIdentifierException("Could not find a property for id: "+pid);
		}
		addType(modelId, model, individual, property, ce, false, metadata);
		return individual;
	}
	
	/**
	 * Convenience wrapper for {@link CoreMolecularModelManager#removeType}
	 * 
	 * @param modelId
	 * @param iid
	 * @param cid
	 * @param metadata
	 * @throws UnknownIdentifierException 
	 */
	public void removeType(String modelId, String iid, String cid, METADATA metadata) throws UnknownIdentifierException {
		ModelContainer model = checkModelId(modelId);
		OWLNamedIndividual individual = getIndividual(iid, model);
		if (individual == null) {
			throw new UnknownIdentifierException("Could not find a individual for id: "+iid);
		}
		OWLClass cls = getClass(cid, model);
		if (cls == null) {
			throw new UnknownIdentifierException("Could not find a class for id: "+cid);
		}
		removeType(modelId, model, individual, cls, true, metadata);
	}
	
	public OWLNamedIndividual removeTypeNonReasoning(String modelId, String iid, OWLClassExpression clsExp, METADATA metadata) throws UnknownIdentifierException {
		ModelContainer model = checkModelId(modelId);
		OWLNamedIndividual individual = getIndividual(iid, model);
		if (individual == null) {
			throw new UnknownIdentifierException("Could not find a individual for id: "+iid);
		}
		removeType(modelId, model, individual, clsExp, false, metadata);
		return individual;
	}
	
	/**
	 * Convenience wrapper for {@link #addOccursIn(String, OWLNamedIndividual, OWLClassExpression)}
	 * 
	 * @param modelId
	 * @param iid
	 * @param eid - e.g. PR:P12345
	 * @throws UnknownIdentifierException
	 */
	@Deprecated
	public void addOccursIn(String modelId,
			String iid, String eid) throws UnknownIdentifierException {
		ModelContainer model = checkModelId(modelId);
		OWLNamedIndividual individual = getIndividual(iid, model);
		if (individual == null) {
			throw new UnknownIdentifierException("Could not find a individual for id: "+iid);
		}
		OWLClass cls = getClass(eid, model);
		if (cls == null) {
			throw new UnknownIdentifierException("Could not find a class for id: "+eid);
		}
		addOccursIn(modelId, model, individual, cls);
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
	 * @throws UnknownIdentifierException
	 */
	@Deprecated
	public void addOccursIn(String modelId,
			OWLNamedIndividual i, 
			OWLClassExpression enabler) throws UnknownIdentifierException {
		ModelContainer model = checkModelId(modelId);
		addOccursIn(modelId, model, i, enabler);
	}
	
	@Deprecated
	private void addOccursIn(String modelId, ModelContainer model,
			OWLNamedIndividual i, 
			OWLClassExpression enabler) {
		addType(modelId, model, i, OBOUpperVocabulary.BFO_occurs_in.getObjectProperty(getOntology()), enabler, true, null);
	} 

	/**
	 * Convenience wrapper for {@link #addEnabledBy(String, OWLNamedIndividual, OWLClassExpression)}
	 * 
	 * @param modelId
	 * @param iid
	 * @param eid - e.g. PR:P12345
	 * @throws UnknownIdentifierException 
	 * @throws OWLException
	 */
	@Deprecated
	public void addEnabledBy(String modelId,
			String iid, String eid) throws UnknownIdentifierException,
			OWLException {
		ModelContainer model = checkModelId(modelId);
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
		addEnabledBy(modelId, model, individual, clsExpr);
	}

	@Deprecated
	private OWLClassExpression parseClassExpression(String expression, ModelContainer model) throws OWLException {
		OWLGraphWrapper g = new OWLGraphWrapper(model.getAboxOntology());
		return parseClassExpression(expression, g);
	}
	
	@Deprecated
	public static OWLClassExpression parseClassExpression(String expression, OWLGraphWrapper g) throws OWLException {
		try {
			ManchesterSyntaxTool syntaxTool = new ManchesterSyntaxTool(g, true);
			OWLClassExpression clsExpr = syntaxTool.parseManchesterExpression(expression);
			return clsExpr;
		}
		catch (ParserException e) {
			// wrap in an Exception (not RuntimeException) to enable proper error handling
			throw new OWLException("Could not parse expression: \""+expression+"\"", e) {

				private static final long serialVersionUID = -9158071212925724138L;
			};
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
	 * @throws UnknownIdentifierException
	 */
	@Deprecated
	public void addEnabledBy(String modelId,
			OWLNamedIndividual i, 
			OWLClassExpression enabler) throws UnknownIdentifierException {
		ModelContainer model = checkModelId(modelId);
		addEnabledBy(modelId, model, i, enabler);
	}
	
	@Deprecated
	private void addEnabledBy(String modelId, ModelContainer model,
			OWLNamedIndividual i, 
			OWLClassExpression enabler) {
		OWLObjectProperty p = OBOUpperVocabulary.GOREL_enabled_by.getObjectProperty(model.getAboxOntology());
		addType(modelId, model, i, p, enabler, true, null);
	}

	/**
	 * Convenience wrapper for {@link CoreMolecularModelManager#addFact}
	 * 
	 * @param modelId
	 * @param pid
	 * @param iid
	 * @param jid
	 * @param annotations 
	 * @param metadata
	 * @return relevant individuals
	 * @throws UnknownIdentifierException 
	 */
	public List<OWLNamedIndividual> addFact(String modelId, String pid,	String iid, String jid,
			Set<OWLAnnotation> annotations, METADATA metadata) throws UnknownIdentifierException {
		ModelContainer model = checkModelId(modelId);
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
		addFact(modelId, model, property, individual1, individual2, annotations, true, metadata);
		return Arrays.asList(individual1, individual2);
	}

	/**
	 * Convenience wrapper for {@link CoreMolecularModelManager#addFact}
	 * 
	 * @param modelId
	 * @param pid
	 * @param iid
	 * @param jid
	 * @param annotations 
	 * @param metadata
	 * @return relevant individuals
	 * @throws UnknownIdentifierException 
	 */
	public List<OWLNamedIndividual> addFactNonReasoning(String modelId, String pid,	String iid, String jid,
			Set<OWLAnnotation> annotations, METADATA metadata) throws UnknownIdentifierException {
		ModelContainer model = checkModelId(modelId);
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
		addFact(modelId, model, property, individual1, individual2, annotations, false, metadata);
		return Arrays.asList(individual1, individual2);
	}
	
	/**
	 * Convenience wrapper for {@link CoreMolecularModelManager#addFact}
	 * 
	 * @param modelId
	 * @param vocabElement
	 * @param iid
	 * @param jid
	 * @param annotations
	 * @param metadata
	 * @return relevant individuals
	 * @throws UnknownIdentifierException
	 */
	public List<OWLNamedIndividual> addFact(String modelId, OBOUpperVocabulary vocabElement,
			String iid, String jid, Set<OWLAnnotation> annotations, METADATA metadata) throws UnknownIdentifierException {
		ModelContainer model = checkModelId(modelId);
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
		addFact(modelId, model, property, individual1, individual2, annotations, true, metadata);
		return Arrays.asList(individual1, individual2);
	}
	
	/**
	 * @param modelId
	 * @param pid
	 * @param iid
	 * @param jid
	 * @param metadata
	 * @return response info
	 * @throws UnknownIdentifierException 
	 */
	public List<OWLNamedIndividual> removeFact(String modelId, String pid,
			String iid, String jid, METADATA metadata) throws UnknownIdentifierException {
		ModelContainer model = checkModelId(modelId);
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
		removeFact(modelId, model, property, individual1, individual2, true, metadata);
		return Arrays.asList(individual1, individual2);
	}
	
	public Pair<List<OWLNamedIndividual>, Set<IRI>> removeFactNonReasoning(String modelId, String pid,
			String iid, String jid, METADATA metadata) throws UnknownIdentifierException {
		ModelContainer model = checkModelId(modelId);
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
		Set<IRI> iriSet = removeFact(modelId, model, property, individual1, individual2, false, metadata);
		return Pair.of(Arrays.asList(individual1, individual2), iriSet);
	}

	public List<OWLNamedIndividual> addAnnotations(String modelId, String pid, 
			String iid, String jid, Set<OWLAnnotation> annotations, METADATA metadata) throws UnknownIdentifierException {
		ModelContainer model = checkModelId(modelId);
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
		addAnnotations(modelId, model, property, individual1, individual2, annotations, false, metadata);

		return Arrays.asList(individual1, individual2);
	}
	
	public void addAnnotations(String modelId, Set<OWLObjectPropertyAssertionAxiom> axioms, Set<OWLAnnotation> annotations, METADATA metadata) throws UnknownIdentifierException {
		ModelContainer model = checkModelId(modelId);
		for (OWLObjectPropertyAssertionAxiom axiom : axioms) {
			addAnnotations(modelId, model, axiom, annotations, false, metadata);	
		}
	}
	
	public List<OWLNamedIndividual> updateAnnotation(String modelId, String pid, 
			String iid, String jid, OWLAnnotation annotation, METADATA metadata) throws UnknownIdentifierException {
		ModelContainer model = checkModelId(modelId);
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
		updateAnnotation(modelId, model, property, individual1, individual2, annotation, false, metadata);

		return Arrays.asList(individual1, individual2);
	}
	
	public Set<OWLObjectPropertyAssertionAxiom> updateAnnotation(String modelId, Set<OWLObjectPropertyAssertionAxiom> axioms, OWLAnnotation annotation, METADATA metadata) throws UnknownIdentifierException {
		ModelContainer model = checkModelId(modelId);
		Set<OWLObjectPropertyAssertionAxiom> newAxioms = new HashSet<OWLObjectPropertyAssertionAxiom>();
		for (OWLObjectPropertyAssertionAxiom axiom : axioms) {
			OWLObjectPropertyAssertionAxiom newAxiom = 
					updateAnnotation(modelId, model, axiom, annotation, false, metadata);
			if (newAxiom != null) {
				newAxioms.add(newAxiom);
			}
		}
		return newAxioms;
	}
	
	public List<OWLNamedIndividual> removeAnnotations(String modelId, String pid, 
			String iid, String jid, Set<OWLAnnotation> annotations, METADATA metadata) throws UnknownIdentifierException {
		ModelContainer model = checkModelId(modelId);
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
		removeAnnotations(modelId, model, property, individual1, individual2, annotations, false, metadata);

		return Arrays.asList(individual1, individual2);
	}
	
	/**
	 * Convenience wrapper for {@link #addPartOf(String, OWLNamedIndividual, OWLNamedIndividual, Set)}
	 *
	 * @param modelId
	 * @param iid
	 * @param jid
	 * @param annotations 
	 * @throws UnknownIdentifierException
	 */
	@Deprecated
	public void addPartOf(String modelId,  String iid,
			String jid, Set<OWLAnnotation> annotations) throws UnknownIdentifierException {
		ModelContainer model = checkModelId(modelId);
		OWLNamedIndividual individual1 = getIndividual(iid, model);
		if (individual1 == null) {
			throw new UnknownIdentifierException("Could not find a individual for id: "+iid);
		}
		OWLNamedIndividual individual2 = getIndividual(jid, model);
		if (individual2 == null) {
			throw new UnknownIdentifierException("Could not find a individual for id: "+jid);
		}
		addPartOf(modelId, individual1, individual2, annotations);
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
	 * @throws UnknownIdentifierException
	 */
	@Deprecated
	public void addPartOf(String modelId, OWLNamedIndividual i,
			OWLNamedIndividual j, Set<OWLAnnotation> annotations) throws UnknownIdentifierException {
		ModelContainer model = checkModelId(modelId);
		addPartOf(modelId, model, i, j, annotations, true);
	}
	
	@Deprecated
	private void addPartOf(String modelId, ModelContainer model, OWLNamedIndividual i, 
			OWLNamedIndividual j, Set<OWLAnnotation> annotations, boolean flushReasoner) {
		addFact(modelId, model, getObjectProperty(OBOUpperVocabulary.BFO_part_of, model), i, j, annotations, flushReasoner, null);
	}

	/**
	 * This method will check the given model and update the import declarations.
	 * It will add missing IRIs and remove obsolete ones.
	 * 
	 * @param modelId 
	 * @throws UnknownIdentifierException
	 * @see #additionalImports
	 * @see #addImports(Iterable)
	 * @see #obsoleteImports
	 * @see #addObsoleteImports(Iterable)
	 */
	public void updateImports(String modelId) throws UnknownIdentifierException {
		ModelContainer model = checkModelId(modelId);
		updateImports(modelId, model);
	}
	
}
