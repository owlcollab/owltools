package owltools.gaf.lego.server.handler;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.glassfish.jersey.server.JSONP;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.gaf.lego.MolecularModelJsonRenderer;
import owltools.gaf.lego.MolecularModelJsonRenderer.KEY;
import owltools.gaf.lego.MolecularModelManager;
import owltools.gaf.lego.MolecularModelManager.LegoAnnotationType;
import owltools.gaf.lego.MolecularModelManager.UnknownIdentifierException;
import owltools.gaf.lego.UndoAwareMolecularModelManager;
import owltools.gaf.lego.UndoAwareMolecularModelManager.ChangeEvent;
import owltools.gaf.lego.UndoAwareMolecularModelManager.UndoMetadata;
import owltools.gaf.lego.server.validation.BeforeSaveModelValidator;
import owltools.util.ModelContainer;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class JsonOrJsonpBatchHandler implements M3BatchHandler {

	public static final String JSONP_DEFAULT_CALLBACK = "jsonp";
	public static final String JSONP_DEFAULT_OVERWRITE = "json.wrf";
	
	
	public static boolean USE_USER_ID = true;
	public static boolean USE_CREATION_DATE = true;
	public static boolean ADD_INFERENCES = true;
	public static boolean VALIDATE_BEFORE_SAVE = true;
	
	private static final Logger logger = Logger.getLogger(JsonOrJsonpBatchHandler.class);
	
	private final UndoAwareMolecularModelManager m3;
	private final Set<String> relevantRelations;
	private final BeforeSaveModelValidator beforeSaveValidator;

	public JsonOrJsonpBatchHandler(UndoAwareMolecularModelManager models, Set<String> relevantRelations) {
		super();
		this.m3 = models;
		this.relevantRelations = relevantRelations;
		this.beforeSaveValidator = new BeforeSaveModelValidator();
	}

	private final GsonBuilder gsonBuilder = new GsonBuilder();
	private final Type type = new TypeToken<M3Request[]>(){

		// generated
		private static final long serialVersionUID = 5452629810143143422L;
		
	}.getType();
	
	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3BatchResponse m3BatchGet(String intention, String requestString) {
		return m3Batch(null, intention, requestString, false);
	}
	
	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3BatchResponse m3BatchGetPrivileged(String uid, String intention, String requestString) {
		return m3Batch(uid, intention, requestString, true);
	}

	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3BatchResponse m3BatchPost(String intention, String requestString) {
		return m3Batch(null, intention, requestString, false);
	}
	
	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3BatchResponse m3BatchPostPrivileged(String uid, String intention, String requestString) {
		return m3Batch(uid, intention, requestString, true);
	}

	@Override
	public M3BatchResponse m3Batch(String uid, String intention, M3Request[] requests, boolean isPrivileged) {
		M3BatchResponse response = new M3BatchResponse(uid, intention);
		try {
			return m3Batch(response, requests, uid, isPrivileged);
		} catch (InsufficientPermissionsException e) {
			return error(response, e.getMessage(), null);
		} catch (Exception e) {
			return error(response, "Could not successfully complete batch request.", e);
		} catch (Throwable t) {
			logger.error("A critical error occured.", t);
			return error(response, "An internal error occured at the server level.", t);
		}
	}
	
	private M3BatchResponse m3Batch(String uid, String intention, String requestString, boolean isPrivileged) {
		M3BatchResponse response = new M3BatchResponse(uid, intention);
		try {
			Gson gson = gsonBuilder.create();
			M3Request[] requests = gson.fromJson(requestString, type);
			return m3Batch(response, requests, uid, isPrivileged);
		} catch (Exception e) {
			return error(response, "Could not successfully handle batch request.", e);
		} catch (Throwable t) {
			logger.error("A critical error occured.", t);
			return error(response, "An internal error occured at the server level.", t);
		}
	}
	
	private static class BatchHandlerValues {
		
		final Set<OWLNamedIndividual> relevantIndividuals = new HashSet<OWLNamedIndividual>();
		boolean renderBulk = false;
		boolean renderModelAnnotations = false;
		boolean nonMeta = false;
		String modelId = null;
	}
	
	private M3BatchResponse m3Batch(M3BatchResponse response, M3Request[] requests, String userId, boolean isPrivileged) throws InsufficientPermissionsException, Exception {
		userId = normalizeUserId(userId);
		UndoMetadata token = new UndoMetadata(userId);
		
		final BatchHandlerValues values = new BatchHandlerValues();
		for (M3Request request : requests) {
			requireNotNull(request, "request");
			final Entity entity = Entity.get(StringUtils.trimToNull(request.entity));
			if (entity == null) {
				throw new MissingParameterException("No valid value for entity type: "+request.entity);
			}
			final Operation operation = Operation.get(StringUtils.trimToNull(request.operation));
			if (operation == null) {
				throw new MissingParameterException("No valid value for operation type: "+request.operation);
			}
			checkPermissions(entity, operation, isPrivileged);

			// individual
			if (Entity.individual == entity) {
				String error = handleRequestForIndividual(request, operation, userId, token, values);
				if (error != null) {
					return error(response, "Unknown operation: "+operation, null);
				}
			}
			// edge
			else if (Entity.edge == entity) {
				String error = handleRequestForEdge(request, operation, userId, token, values);
				if (error != null) {
					return error(response, error, null);
				}
			}
			//model
			else if (Entity.model == entity) {
				String error = handleRequestForModel(request, response, operation, userId, token, values);
				if (error != null) {
					return error(response, "Unknown operation: "+operation, null);
				}
			}
			// relations
			else if (Entity.relations == entity) {
				if (Operation.get == operation){
					if (values.nonMeta) {
						// can only be used with other "meta" operations in batch mode, otherwise it would lead to conflicts in the returned signal
						return error(response, "Get Relations can only be combined with other meta operations.", null);
					}
					getRelations(response, userId);
				}
				else {
					return error(response, "Unknown operation: "+operation, null);
				}
			}
			// evidence
			else if (Entity.evidence == entity) {
				if (Operation.get == operation){
					if (values.nonMeta) {
						// can only be used with other "meta" operations in batch mode, otherwise it would lead to conflicts in the returned signal
						return error(response, "Get Evidences can only be combined with other meta operations.", null);
					}
					getEvidence(response, userId);
				}
				else {
					return error(response, "Unknown operation: "+operation, null);
				}
			}
			else {
				return error(response, "Unknown entity: "+entity, null);
			}
		}
		if (M3BatchResponse.SIGNAL_META.equals(response.signal)) {
			return response;
		}
		if (values.modelId == null) {
			return error(response, "Empty batch calls are not supported, at least one request is required.", null);
		}
		// get model
		final ModelContainer model = m3.getModel(values.modelId);
		if (model == null) {
			throw new UnknownIdentifierException("Could not retrieve a model for id: "+values.modelId);
		}
		// update reasoner
		// report state
		final OWLReasoner reasoner = model.getReasoner();
		reasoner.flush();
		final boolean isConsistent = reasoner.isConsistent();

		// create response.data
		if (values.renderBulk) {
			// render complete model
			response.data = m3.getModelObject(values.modelId);
			response.signal = M3BatchResponse.SIGNAL_REBUILD;
			if (ADD_INFERENCES) {
				MolecularModelJsonRenderer renderer = new MolecularModelJsonRenderer(model.getAboxOntology());
				renderer.renderModelInferences(response.data, reasoner);
			}
		}
		else {
			// render individuals
			MolecularModelJsonRenderer renderer = new MolecularModelJsonRenderer(model.getAboxOntology());
			response.data = renderer.renderIndividuals(values.relevantIndividuals);
			response.signal = M3BatchResponse.SIGNAL_MERGE;
			if (ADD_INFERENCES) {
				renderer.renderInferences(values.relevantIndividuals, response.data, reasoner);
			}
		}
		
		// add model annotations
		if (values.renderModelAnnotations) {
			List<Object> anObjs = MolecularModelJsonRenderer.renderModelAnnotations(model.getAboxOntology());
			response.data.put(KEY.annotations.name(), anObjs);
		}

		// add other infos to data
		response.data.put("id", values.modelId);
		if (!isConsistent) {
			response.data.put("inconsistent_p", Boolean.TRUE);
		}
		// These are required for an "okay" response.
		response.message_type = M3BatchResponse.MESSAGE_TYPE_SUCCESS;
		if( response.message == null ){
			response.message = "success";
		}
		return response;
	}

	/**
	 * Normalize the userId.
	 * 
	 * @param userId
	 * @return normalized id or null
	 */
	private String normalizeUserId(String userId) {
		if (userId != null) {
			userId = StringUtils.trimToNull(userId);
			// quick hack, may be removed once all users are required to have a user id.
			if ("anonymous".equalsIgnoreCase(userId)) {
				return null;
			}
		}
		return userId;
	}
	
	private String handleRequestForIndividual(M3Request request, Operation operation, String userId, UndoMetadata token, BatchHandlerValues values) throws Exception {
		values.nonMeta = true;
		requireNotNull(request.arguments, "request.arguments");
		values.modelId = checkModelId(values.modelId, request);

		// get info, no modification
		if (Operation.get == operation) {
			requireNotNull(request.arguments.individual, "request.arguments.individual");
			OWLNamedIndividual i = m3.getNamedIndividual(values.modelId, request.arguments.individual);
			values.relevantIndividuals.add(i);
			
		}
		// create from class
		else if (Operation.create == operation) {
			// required: subject
			// optional: expressions, values
			requireNotNull(request.arguments.subject, "request.arguments.subject");
			Collection<Pair<String, String>> annotations = extract(request.arguments.values, userId, true);
			Pair<String, OWLNamedIndividual> individualPair = m3.createIndividualNonReasoning(values.modelId, request.arguments.subject, annotations, token);
			values.relevantIndividuals.add(individualPair.getValue());

			if (request.arguments.expressions != null) {
				for(M3Expression expression : request.arguments.expressions) {
					OWLClassExpression cls = M3ExpressionParser.parse(values.modelId, expression, m3);
					m3.addTypeNonReasoning(values.modelId, individualPair.getKey(), cls, token);
				}
			}
			addContributor(values.modelId, userId, token, m3);
		}
		// create individuals for subject and object,
		// add object as fact to subject with given property
		else if (Operation.createComposite == operation) {
			// required: subject, predicate, object
			// optional: expressions, values
			requireNotNull(request.arguments.subject, "request.arguments.subject");
			requireNotNull(request.arguments.predicate, "request.arguments.predicate");
			requireNotNull(request.arguments.object, "request.arguments.object");
			Collection<Pair<String, String>> annotations = extract(request.arguments.values, userId, true);
			Pair<String, OWLNamedIndividual> individual1Pair = m3.createIndividualNonReasoning(values.modelId, request.arguments.subject, annotations, token);
			values.relevantIndividuals.add(individual1Pair.getValue());

			if (request.arguments.expressions != null) {
				for(M3Expression expression : request.arguments.expressions) {
					OWLClassExpression cls = M3ExpressionParser.parse(values.modelId, expression, m3);
					m3.addTypeNonReasoning(values.modelId, individual1Pair.getKey(), cls, token);
				}
			}
			Pair<String, OWLNamedIndividual> individual2Pair = m3.createIndividualNonReasoning(values.modelId, request.arguments.object, annotations, token);
			values.relevantIndividuals.add(individual2Pair.getValue());
			
			m3.addFact(values.modelId, request.arguments.predicate, individual1Pair.getLeft(), individual2Pair.getLeft(), annotations, token);
			addContributor(values.modelId, userId, token, m3);
		}
		// remove individual (and all axioms using it)
		else if (Operation.remove == operation){
			// required: modelId, individual
			requireNotNull(request.arguments.individual, "request.arguments.individual");
			m3.deleteIndividual(values.modelId, request.arguments.individual, token);
			addContributor(values.modelId, userId, token, m3);
			values.renderBulk = true;
		}				
		// add type / named class assertion
		else if (Operation.addType == operation){
			// required: individual, expressions
			requireNotNull(request.arguments.individual, "request.arguments.individual");
			requireNotNull(request.arguments.expressions, "request.arguments.expressions");
			for(M3Expression expression : request.arguments.expressions) {
				OWLClassExpression cls = M3ExpressionParser.parse(values.modelId, expression, m3);
				// TODO evidence and contributor information for types
				OWLNamedIndividual i = m3.addTypeNonReasoning(values.modelId, request.arguments.individual, cls, token);
				values.relevantIndividuals.add(i);
			}
			addContributor(values.modelId, userId, token, m3);
		}
		// remove type / named class assertion
		else if (Operation.removeType == operation){
			// required: individual, expressions
			requireNotNull(request.arguments.individual, "request.arguments.individual");
			requireNotNull(request.arguments.expressions, "request.arguments.expressions");
			for(M3Expression expression : request.arguments.expressions) {
				OWLClassExpression cls = M3ExpressionParser.parse(values.modelId, expression, m3);
				OWLNamedIndividual i = m3.removeTypeNonReasoning(values.modelId, request.arguments.individual, cls, token);
				values.relevantIndividuals.add(i);
			}
			addContributor(values.modelId, userId, token, m3);
		}
		// add annotation
		else if (Operation.addAnnotation == operation){
			// required: individual, values
			requireNotNull(request.arguments.individual, "request.arguments.individual");
			requireNotNull(request.arguments.values, "request.arguments.values");

			OWLNamedIndividual i = m3.addAnnotations(values.modelId, request.arguments.individual,
					extract(request.arguments.values, userId, false), token);
			values.relevantIndividuals.add(i);
			addContributor(values.modelId, userId, token, m3);
		}
		// remove annotation
		else if (Operation.removeAnnotation == operation){
			// required: individual, values
			requireNotNull(request.arguments.individual, "request.arguments.individual");
			requireNotNull(request.arguments.values, "request.arguments.values");

			OWLNamedIndividual i = m3.removeAnnotations(values.modelId, request.arguments.individual,
					extract(request.arguments.values, null, false), token);
			values.relevantIndividuals.add(i);
			addContributor(values.modelId, userId, token, m3);
		}
		else {
			return "Unknown operation: "+operation;
		}
		return null;
	}
	
	private String handleRequestForEdge(M3Request request, Operation operation, String userId, UndoMetadata token, BatchHandlerValues values) throws Exception {
		values.nonMeta = true;
		requireNotNull(request.arguments, "request.arguments");
		values.modelId = checkModelId(values.modelId, request);
		// required: subject, predicate, object
		requireNotNull(request.arguments.subject, "request.arguments.subject");
		requireNotNull(request.arguments.predicate, "request.arguments.predicate");
		requireNotNull(request.arguments.object, "request.arguments.object");

		// add edge
		if (Operation.add == operation){
			// optional: values
			List<OWLNamedIndividual> individuals = m3.addFactNonReasoning(values.modelId,
					request.arguments.predicate, request.arguments.subject,
					request.arguments.object, extract(request.arguments.values, userId, true), token);
			values.relevantIndividuals.addAll(individuals);
			addContributor(values.modelId, userId, token, m3);
		}
		// remove edge
		else if (Operation.remove == operation){
			List<OWLNamedIndividual> individuals = m3.removeFactNonReasoning(values.modelId,
					request.arguments.predicate, request.arguments.subject,
					request.arguments.object, token);
			values.relevantIndividuals.addAll(individuals);
			addContributor(values.modelId, userId, token, m3);
		}
		// add annotation
		else if (Operation.addAnnotation == operation){
			requireNotNull(request.arguments.values, "request.arguments.values");

			List<OWLNamedIndividual> individuals = m3.addAnnotations(values.modelId,
					request.arguments.predicate, request.arguments.subject,
					request.arguments.object, extract(request.arguments.values, userId, false), token);
			values.relevantIndividuals.addAll(individuals);
		}
		// remove annotation
		else if (Operation.removeAnnotation == operation){
			requireNotNull(request.arguments.values, "request.arguments.values");

			List<OWLNamedIndividual> individuals = m3.removeAnnotations(values.modelId,
					request.arguments.predicate, request.arguments.subject,
					request.arguments.object, extract(request.arguments.values, null, false), token);
			values.relevantIndividuals.addAll(individuals);
		}
		else {
			return "Unknown operation: "+operation;
		}
		return null;
	}
	
	private String handleRequestForModel(M3Request request, M3BatchResponse response, Operation operation, String userId, UndoMetadata token, BatchHandlerValues values) throws Exception {
		// get model
		if (Operation.get == operation){
			values.nonMeta = true;
			requireNotNull(request.arguments, "request.arguments");
			values.modelId = checkModelId(values.modelId, request);
			values.renderBulk = true;
		}
		else if (Operation.updateImports == operation){
			values.nonMeta = true;
			requireNotNull(request.arguments, "request.arguments");
			values.modelId = checkModelId(values.modelId, request);
			m3.updateImports(values.modelId);
			values.renderBulk = true;
		}
		else if (Operation.generate == operation) {
			values.nonMeta = true;
			requireNotNull(request.arguments, "request.arguments");
			requireNotNull(request.arguments.db, "request.arguments.db");
			requireNotNull(request.arguments.subject, "request.arguments.subject");
			values.renderBulk = true;
			values.modelId = m3.generateModel(request.arguments.subject, request.arguments.db, token);
			
			Collection<Pair<String, String>> annotations = extract(request.arguments.values, userId, true);
			if (annotations != null) {
				m3.addAnnotations(values.modelId, annotations, token);
			}
			addContributor(values.modelId, userId, token, m3);
		}
		else if (Operation.generateBlank == operation) {
			values.nonMeta = true;
			values.renderBulk = true;
			// db and taxonId are both optional
			String db = null;
			String taxonId = null;
			Collection<Pair<String, String>> annotations = null;
			if (request.arguments != null) {
				db = request.arguments.db;
				taxonId = request.arguments.taxonId;
				annotations = extract(request.arguments.values, userId, true);
			}
			else {
				annotations = extract(null, userId, true);
			}
			if (taxonId != null) {
				values.modelId = m3.generateBlankModelWithTaxon(taxonId, token);
			}
			else {
				values.modelId = m3.generateBlankModel(db, token);
			}
			
			if (annotations != null) {
				m3.addAnnotations(values.modelId, annotations, token);
			}
			addContributor(values.modelId, userId, token, m3);
		}
		else if (Operation.addAnnotation == operation) {
			values.nonMeta = true;
			requireNotNull(request.arguments, "request.arguments");
			requireNotNull(request.arguments.values, "request.arguments.values");
			values.modelId = checkModelId(values.modelId, request);
			Collection<Pair<String, String>> annotations = extract(request.arguments.values, userId, false);
			if (annotations != null) {
				m3.addAnnotations(values.modelId, annotations, token);
			}
			values.renderModelAnnotations = true;
		}
		else if (Operation.removeAnnotation == operation) {
			values.nonMeta = true;
			requireNotNull(request.arguments, "request.arguments");
			requireNotNull(request.arguments.values, "request.arguments.values");
			values.modelId = checkModelId(values.modelId, request);
			Collection<Pair<String, String>> annotations = extract(request.arguments.values, null, false);
			if (annotations != null) {
				m3.removeAnnotations(values.modelId, annotations, token);
			}
			values.renderModelAnnotations = true;
		}
		else if (Operation.exportModel == operation) {
			if (values.nonMeta) {
				// can only be used with other "meta" operations in batch mode, otherwise it would lead to conflicts in the returned signal
				return "Export model can only be combined with other meta operations.";
			}
			requireNotNull(request.arguments, "request.arguments");
			values.modelId = checkModelId(values.modelId, request);
			export(response, values.modelId, userId);
		}
		else if (Operation.exportModelLegacy == operation) {
			if (values.nonMeta) {
				// can only be used with other "meta" operations in batch mode, otherwise it would lead to conflicts in the returned signal
				return "Export legacy model can only be combined with other meta operations.";
			}
			requireNotNull(request.arguments, "request.arguments");
			values.modelId = checkModelId(values.modelId, request);
			exportLegacy(response, values.modelId, request.arguments.format, userId);
		}
		else if (Operation.importModel == operation) {
			values.nonMeta = true;
			requireNotNull(request.arguments, "request.arguments");
			requireNotNull(request.arguments.importModel, "request.arguments.importModel");
			values.modelId = m3.importModel(request.arguments.importModel);
			
			Collection<Pair<String, String>> annotations = extract(request.arguments.values, userId, false);
			if (annotations != null) {
				m3.addAnnotations(values.modelId, annotations, token);
			}
			addContributor(values.modelId, userId, token, m3);
			values.renderBulk = true;
		}
		else if (Operation.storeModel == operation) {
			requireNotNull(request.arguments, "request.arguments");
			values.modelId = checkModelId(values.modelId, request);
			Collection<Pair<String, String>> annotations = extract(request.arguments.values, userId, false);
			if (VALIDATE_BEFORE_SAVE) {
				List<String> issues = beforeSaveValidator.validateBeforeSave(values.modelId, m3);
				if (issues != null && !issues.isEmpty()) {
					StringBuilder commentary = new StringBuilder();
					for (Iterator<String> it = issues.iterator(); it.hasNext();) {
						String issue = it.next();
						commentary.append(issue);
						if (it.hasNext()) {
							commentary.append('\n');
						}
					}
					response.commentary = commentary.toString();
					return "Save model failed due to a failed validation of the model";			
				}
			}
			save(response, values.modelId, annotations, userId, token);
		}
		else if (Operation.allModelIds == operation) {
			if (values.nonMeta) {
				// can only be used with other "meta" operations in batch mode, otherwise it would lead to conflicts in the returned signal
				return operation+" cannot be combined with other operations.";
			}
			getAllModelIds(response, userId);
		}
		else if (Operation.allModelMeta == operation) {
			if (values.nonMeta) {
				// can only be used with other "meta" operations in batch mode, otherwise it would lead to conflicts in the returned signal
				return operation+" cannot be combined with other operations.";
			}
			getAllModelMeta(response, userId);
		}
		else if (Operation.search == operation) {
			if (values.nonMeta) {
				// can only be used with other "meta" operations in batch mode, otherwise it would lead to conflicts in the returned signal
				return operation+" cannot be combined with other operations.";
			}
			requireNotNull(request.arguments, "request.arguments");
			requireNotNull(request.arguments.values, "request.arguments.values");
			Collection<Pair<String, String>> extractedValues = extract(request.arguments.values, null, false);
			List<String> searchIds = new ArrayList<String>();
			if (extractedValues != null) {
				for (Pair<String, String> pair : extractedValues) {
					String key = pair.getKey();
					String val = pair.getValue();
					if ("id".equals(key) && val != null) {
						searchIds.add(val);
					}
				}
			}
			if (!searchIds.isEmpty()) {
				searchModels(response, searchIds, userId);
			}
			else {
				return "No query identifiers found in the request.";
			}
		}
		else if (Operation.undo == operation) {
			values.nonMeta = true;
			m3.undo(values.modelId, userId);
			values.renderBulk = true;
		}
		else if (Operation.redo == operation) {
			values.nonMeta = true;
			m3.redo(values.modelId, userId);
			values.renderBulk = true;
		}
		else if (Operation.getUndoRedo == operation) {
			if (values.nonMeta) {
				// can only be used with other "meta" operations in batch mode, otherwise it would lead to conflicts in the returned signal
				return operation+" cannot be combined with other operations.";
			}
			getCurrentUndoRedoForModel(response, values.modelId, userId);
		}
		else {
			return "Unknown operation: "+operation;
		}
		return null;
	}

	private void getCurrentUndoRedoForModel(M3BatchResponse response, String modelId, String userId) {
		Pair<List<ChangeEvent>,List<ChangeEvent>> undoRedoEvents = m3.getUndoRedoEvents(modelId);
		initMetaResponse(response);
		List<Map<Object, Object>> undos = new ArrayList<Map<Object,Object>>();
		List<Map<Object, Object>> redos = new ArrayList<Map<Object,Object>>();
		final long currentTime = System.currentTimeMillis();
		for(ChangeEvent undo : undoRedoEvents.getLeft()) {
			Map<Object, Object> data = new HashMap<Object, Object>(3);
			data.put("user-id", undo.getUserId());
			data.put("time", Long.valueOf(currentTime-undo.getTime()));
			// TODO add a summary of the change? axiom count?
			undos.add(data);
		}
		for(ChangeEvent redo : undoRedoEvents.getRight()) {
			Map<Object, Object> data = new HashMap<Object, Object>(3);
			data.put("user-id", redo.getUserId());
			data.put("time", Long.valueOf(currentTime-redo.getTime()));
			// TODO add a summary of the change? axiom count?
			redos.add(data);
		}
		response.data.put("undo", undos);
		response.data.put("redo", redos);
	}
	
	private void getAllModelIds(M3BatchResponse response, String userId) throws IOException {
		Set<String> allModelIds = m3.getAvailableModelIds();
		//Set<String> scratchModelIds = mmm.getScratchModelIds();
		//Set<String> storedModelIds = mmm.getStoredModelIds();
		//Set<String> memoryModelIds = mmm.getCurrentModelIds();

		initMetaResponse(response);
		
		response.data.put("model_ids", allModelIds);
		//response.data.put("models_memory", memoryModelIds);
		//response.data.put("models_stored", storedModelIds);
		//response.data.put("models_scratch", scratchModelIds);
	}
	
	/*
	 * A newer version of getAllModelIds that tries to supply additional meta information like labels.
	 * Meant to eventually completely replace it.
	 * 
	 * TODO/BUG: Will obviously clobber top-level properties with more than one entry.
	 */
	private void getAllModelMeta(M3BatchResponse response, String userId) throws IOException {

		Map<String,Map<String,String>> retMap = new HashMap<String, Map<String,String>>();
			
		// Jimmy out what information we can cycling directly through all the models.
		Set<String> allModelIds = m3.getAvailableModelIds();
		for( String mid : allModelIds ){

			retMap.put(mid, new HashMap<String,String>());
			Map<String, String> modelMap = retMap.get(mid);
			
			// Iterate through the model's a.
			ModelContainer m = m3.getModel(mid);
			OWLOntology o = m.getAboxOntology();
			Set<OWLAnnotation> annotations = o.getAnnotations();
			for( OWLAnnotation an : annotations ){
				
				// See if we can match them up.
				for( LegoAnnotationType anntype : LegoAnnotationType.values() ){
					IRI foo = anntype.getAnnotationProperty();
					IRI bar = an.getProperty().getIRI();
					if( foo.equals(bar) ){
						OWLAnnotationValue v = an.getValue();
						modelMap.put(anntype.toString(), v.toString());
					}
				}				
			}
		}

		// Sending the actual response.
		initMetaResponse(response);
		response.data.put("models_meta", retMap);
	}
	
	private void initMetaResponse(M3BatchResponse response) {
		if (response.data == null) {
			response.data = new HashMap<Object, Object>();
			response.message_type = M3BatchResponse.MESSAGE_TYPE_SUCCESS;
			response.message = "success: " + response.data.size();
			response.signal = M3BatchResponse.SIGNAL_META;
		}
	}
	
	private void searchModels(M3BatchResponse response, List<String> ids, String userId) throws IOException {
		Set<String> allModelIds = m3.searchModels(ids);
		initMetaResponse(response);
		response.data.put("model_ids", allModelIds);
	}

	private void getRelations(M3BatchResponse response, String userId) throws OWLOntologyCreationException {
		List<Map<Object,Object>> relList = MolecularModelJsonRenderer.renderRelations(m3, relevantRelations);
		initMetaResponse(response);
		response.data.put(Entity.relations.name(), relList);
	}
	
	private void getEvidence(M3BatchResponse response, String userId) throws OWLException, IOException {
		List<Map<Object,Object>> evidencesList = MolecularModelJsonRenderer.renderEvidences(m3);
		initMetaResponse(response);
		response.data.put(Entity.evidence.name(), evidencesList);
	}
	
	private void export(M3BatchResponse response, String modelId, String userId) throws OWLOntologyStorageException, UnknownIdentifierException {
		String exportModel = m3.exportModel(modelId);
		initMetaResponse(response);
		response.data.put(Operation.exportModel.getLbl(), exportModel);
	}
	
	private void exportLegacy(M3BatchResponse response, String modelId, String format, String userId) throws UnknownIdentifierException, IOException {
		String exportModel = m3.exportModelLegacy(modelId, format);
		initMetaResponse(response);
		response.data.put(Operation.exportModel.getLbl(), exportModel);
	}
	
	private void save(M3BatchResponse response, String modelId, Collection<Pair<String,String>> annotations, String userId, UndoMetadata token) throws OWLOntologyStorageException, OWLOntologyCreationException, IOException, UnknownIdentifierException {
		m3.saveModel(modelId, annotations, token);
		initMetaResponse(response);
	}
	
	private void addContributor(String modelId, String userId, UndoMetadata token, MolecularModelManager<UndoMetadata> m3) throws UnknownIdentifierException {
		if (USE_USER_ID && userId != null) {
			Collection<Pair<String, String>> pairs = new ArrayList<Pair<String,String>>(1);
			pairs.add(Pair.of(LegoAnnotationType.contributor.name(), userId));
			m3.addAnnotations(modelId, pairs, token);
		}
	}


	/**
	 * @param modelId
	 * @param request
	 * @return modelId
	 * @throws MissingParameterException
	 * @throws MultipleModelIdsParameterException
	 */
	public String checkModelId(String modelId, M3Request request) 
			throws MissingParameterException, MultipleModelIdsParameterException {
		
		if (modelId == null) {
			final String currentModelId = request.arguments.modelId;
			requireNotNull(currentModelId, "request.arguments.modelId");
			modelId = currentModelId;
		}
		else {
			final String currentModelId = request.arguments.modelId;
			if (currentModelId != null && modelId.equals(currentModelId) == false) {
				throw new MultipleModelIdsParameterException("Using multiple modelIds in one batch call is not supported.");
			}
		}
		return modelId;
	}
	
	private Collection<Pair<String, String>> extract(M3Pair[] values, String userId, boolean addDate) {
		Collection<Pair<String, String>> result = null;
		if (values != null && values.length > 0) {
			result = new ArrayList<Pair<String,String>>();
			for (M3Pair m3Pair : values) {
				if (m3Pair.key != null && m3Pair.value != null) {
					result.add(Pair.of(m3Pair.key, m3Pair.value));
				}
			}
		}
		if (USE_USER_ID && userId != null) {
			if (result == null) {
				result = new ArrayList<Pair<String,String>>(2);
			}
			result.add(Pair.of(LegoAnnotationType.contributor.name(), userId));
		}
		if (USE_CREATION_DATE && addDate) {
			if (result == null) {
				result = new ArrayList<Pair<String,String>>(1);
			}
			String dateString = MolecularModelManager.LegoAnnotationTypeDateFormat.get().format(new Date());
			result.add(Pair.of(LegoAnnotationType.date.name(), dateString));
		}
		return result;
	}

	/*
	 * commentary is now to be a string, not an unknown multi-leveled object.
	 */
	private M3BatchResponse error(M3BatchResponse state, String msg, Throwable e) {
		state.message_type = "error";
		state.message = msg;
		if (e != null) {

			// Add in the exception name if possible.
			String ename = e.getClass().getName();
			if( ename != null ){
				state.message = state.message + " Exception: " + ename + ".";
			}
			
			// And the exception message.
			String emsg = e.getMessage();
			if( emsg != null ){
				state.message = state.message + " " + emsg;
			}
			
			// Add the stack trace as commentary.
			StringWriter stacktrace = new StringWriter();
			e.printStackTrace(new PrintWriter(stacktrace));
			state.commentary = stacktrace.toString();
		}
		return state;
	}
	
	private void requireNotNull(Object value, String msg) throws MissingParameterException {
		if (value == null) {
			throw new MissingParameterException("Expected non-null value for: "+msg);
		}
	}
	
	protected void checkPermissions(Entity entity, Operation operation, boolean isPrivileged) throws InsufficientPermissionsException {
		// TODO make this configurable
		if (isPrivileged == false) {
			switch (operation) {
			case get:
			case exportModel:
			case exportModelLegacy:
			case allModelIds:
			case allModelMeta:
			case search:
				// positive list, all other operation require a privileged call
				break;
			default :
				throw new InsufficientPermissionsException("Insufficient permissions for the operation "+operation.getLbl()+" on entity: "+entity);
			}
		}
	}
	
	static class InsufficientPermissionsException extends Exception {
		
		private static final long serialVersionUID = -3751573576960618428L;

		InsufficientPermissionsException(String msg) {
			super(msg);
		}
	}
	
	static class MissingParameterException extends Exception {

		private static final long serialVersionUID = 4362299465121954598L;

		/**
		 * @param message
		 */
		MissingParameterException(String message) {
			super(message);
		}
		
	}
	
	private static class MultipleModelIdsParameterException extends Exception {

		private static final long serialVersionUID = 4362299465121954598L;

		/**
		 * @param message
		 */
		MultipleModelIdsParameterException(String message) {
			super(message);
		}
		
	}
}
