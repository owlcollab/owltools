package owltools.gaf.lego.server.handler;

import static owltools.gaf.lego.server.handler.M3BatchHandler.Entity.*;
import static owltools.gaf.lego.server.handler.M3BatchHandler.Operation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.glassfish.jersey.server.JSONP;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.gaf.lego.LegoModelGenerator;
import owltools.gaf.lego.MolecularModelJsonRenderer;
import owltools.gaf.lego.MolecularModelJsonRenderer.KEY;
import owltools.gaf.lego.MolecularModelManager;
import owltools.gaf.lego.MolecularModelManager.LegoAnnotationType;
import owltools.gaf.lego.MolecularModelManager.UnknownIdentifierException;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class JsonOrJsonpBatchHandler implements M3BatchHandler {

	public static final String JSONP_DEFAULT_CALLBACK = "jsonp";
	public static final String JSONP_DEFAULT_OVERWRITE = "json.wrf";
	
	
	public static boolean USE_USER_ID = false;
	
	private static final Logger logger = Logger.getLogger(JsonOrJsonpBatchHandler.class);
	
	private final MolecularModelManager m3;
	private final Set<String> relevantRelations;

	public JsonOrJsonpBatchHandler(MolecularModelManager models, Set<String> relevantRelations) {
		super();
		this.m3 = models;
		this.relevantRelations = relevantRelations;
	}

	private final GsonBuilder gsonBuilder = new GsonBuilder();
	private final Type type = new TypeToken<M3Request[]>(){

		// generated
		private static final long serialVersionUID = 5452629810143143422L;
		
	}.getType();
	
	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3BatchResponse m3BatchGet(String uid, String intention, String requestString) {
		M3BatchResponse response = new M3BatchResponse(uid, intention);
		try {
			Gson gson = gsonBuilder.create();
			M3Request[] requests = gson.fromJson(requestString, type);
			return m3Batch(response, requests, uid);
		} catch (Exception e) {
			return error(response, "Could not successfully handle batch request.", e);
		} catch (Throwable t) {
			logger.error("A critical error occured.", t);
			return error(response, "An internal error occured at the server level.", t);
		}
	}

	@Override
	public M3BatchResponse m3Batch(String uid, String intention, M3Request[] requests) {
		M3BatchResponse response = new M3BatchResponse(uid, intention);
		try {
			return m3Batch(response, requests, uid);
		} catch (Exception e) {
			return error(response, "Could not successfully complete batch request.", e);
		} catch (Throwable t) {
			logger.error("A critical error occured.", t);
			return error(response, "An internal error occured at the server level.", t);
		}
	}
	
	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3BatchResponse m3BatchPost(String uid, String intention, String requestString) {
		return m3BatchGet(uid, intention, requestString);
	}

	private M3BatchResponse m3Batch(M3BatchResponse response, M3Request[] requests, String userId) throws Exception {
		// TODO add userId to relevant requests (i.e. contributor)
		userId = StringUtils.trimToNull(userId);
		final Set<OWLNamedIndividual> relevantIndividuals = new HashSet<OWLNamedIndividual>();
		boolean renderBulk = false;
		boolean renderModelAnnotations = false;
		boolean nonMeta = false;
		String modelId = null;
		for (M3Request request : requests) {
			requireNotNull(request, "request");
			final String entity = StringUtils.trimToNull(request.entity);
			final String operation = StringUtils.trimToNull(request.operation);

			// individual
			if (match(Entity.individual, entity)) {
				nonMeta = true;
				requireNotNull(request.arguments, "request.arguments");
				modelId = checkModelId(modelId, request);

				// get info, no modification
				if (match(Operation.get, operation)) {
					requireNotNull(request.arguments.individual, "request.arguments.individual");
					OWLNamedIndividual i = m3.getNamedIndividual(modelId, request.arguments.individual);
					relevantIndividuals.add(i);
					
				}
				// create from class
				else if (match(Operation.create, operation)) {
					// required: subject
					// optional: expressions, values
					requireNotNull(request.arguments.subject, "request.arguments.subject");
					Collection<Pair<String, String>> annotations = extract(request.arguments.values, userId);
					Pair<String, OWLNamedIndividual> individualPair = m3.createIndividualNonReasoning(modelId, request.arguments.subject, annotations);
					relevantIndividuals.add(individualPair.getValue());

					if (request.arguments.expressions != null) {
						for(M3Expression expression : request.arguments.expressions) {
							OWLClassExpression cls = M3ExpressionParser.parse(modelId, expression, m3);
							m3.addTypeNonReasoning(modelId, individualPair.getKey(), cls);
						}
					}
				}
				// create individuals for subject and object,
				// add object as fact to subject with given property
				else if (match(Operation.createComposite, operation)) {
					// required: subject, predicate, object
					// optional: expressions, values
					requireNotNull(request.arguments.subject, "request.arguments.subject");
					requireNotNull(request.arguments.predicate, "request.arguments.predicate");
					requireNotNull(request.arguments.object, "request.arguments.object");
					Collection<Pair<String, String>> annotations = extract(request.arguments.values, userId);
					Pair<String, OWLNamedIndividual> individual1Pair = m3.createIndividualNonReasoning(modelId, request.arguments.subject, annotations);
					relevantIndividuals.add(individual1Pair.getValue());

					if (request.arguments.expressions != null) {
						for(M3Expression expression : request.arguments.expressions) {
							OWLClassExpression cls = M3ExpressionParser.parse(modelId, expression, m3);
							m3.addTypeNonReasoning(modelId, individual1Pair.getKey(), cls);
						}
					}
					Pair<String, OWLNamedIndividual> individual2Pair = m3.createIndividualNonReasoning(modelId, request.arguments.object, annotations);
					relevantIndividuals.add(individual2Pair.getValue());
					
					m3.addFact(modelId, request.arguments.predicate, individual1Pair.getLeft(), individual2Pair.getLeft(), annotations);
				}
				// remove individual (and all axioms using it)
				else if (match(Operation.remove, operation)){
					// required: modelId, individual
					requireNotNull(request.arguments.individual, "request.arguments.individual");
					m3.deleteIndividual(modelId, request.arguments.individual);
					renderBulk = true;
				}				
				// add type / named class assertion
				else if (match(Operation.addType, operation)){
					// required: individual, expressions
					requireNotNull(request.arguments.individual, "request.arguments.individual");
					requireNotNull(request.arguments.expressions, "request.arguments.expressions");
					for(M3Expression expression : request.arguments.expressions) {
						OWLClassExpression cls = M3ExpressionParser.parse(modelId, expression, m3);
						// TODO evidence and contributor information for types
						OWLNamedIndividual i = m3.addTypeNonReasoning(modelId, request.arguments.individual, cls);
						relevantIndividuals.add(i);
					}
				}
				// remove type / named class assertion
				else if (match(Operation.removeType, operation)){
					// required: individual, expressions
					requireNotNull(request.arguments.individual, "request.arguments.individual");
					requireNotNull(request.arguments.expressions, "request.arguments.expressions");
					for(M3Expression expression : request.arguments.expressions) {
						OWLClassExpression cls = M3ExpressionParser.parse(modelId, expression, m3);
						OWLNamedIndividual i = m3.removeTypeNonReasoning(modelId, request.arguments.individual, cls);
						relevantIndividuals.add(i);
					}
				}
				// add annotation
				else if (match(Operation.addAnnotation, operation)){
					// required: individual, values
					requireNotNull(request.arguments.individual, "request.arguments.individual");
					requireNotNull(request.arguments.values, "request.arguments.values");

					OWLNamedIndividual i = m3.addAnnotations(modelId, request.arguments.individual,
							extract(request.arguments.values, userId));
					relevantIndividuals.add(i);
				}
				// remove annotation
				else if (match(Operation.removeAnnotation, operation)){
					// required: individual, values
					requireNotNull(request.arguments.individual, "request.arguments.individual");
					requireNotNull(request.arguments.values, "request.arguments.values");

					OWLNamedIndividual i = m3.removeAnnotations(modelId, request.arguments.individual,
							extract(request.arguments.values, null));
					relevantIndividuals.add(i);
				}
				else {
					return error(response, "Unknown operation: "+operation, null);
				}
			}
			// edge
			else if (match(Entity.edge, entity)) {
				nonMeta = true;
				requireNotNull(request.arguments, "request.arguments");
				modelId = checkModelId(modelId, request);
				// required: subject, predicate, object
				requireNotNull(request.arguments.subject, "request.arguments.subject");
				requireNotNull(request.arguments.predicate, "request.arguments.predicate");
				requireNotNull(request.arguments.object, "request.arguments.object");

				// add edge
				if (match(Operation.add, operation)){
					// optional: values
					List<OWLNamedIndividual> individuals = m3.addFactNonReasoning(modelId,
							request.arguments.predicate, request.arguments.subject,
							request.arguments.object, extract(request.arguments.values, userId));
					relevantIndividuals.addAll(individuals);
				}
				// remove edge
				else if (match(Operation.remove, operation)){
					List<OWLNamedIndividual> individuals = m3.removeFactNonReasoning(modelId,
							request.arguments.predicate, request.arguments.subject,
							request.arguments.object);
					relevantIndividuals.addAll(individuals);
				}
				// add annotation
				else if (match(Operation.addAnnotation, operation)){
					requireNotNull(request.arguments.values, "request.arguments.values");

					List<OWLNamedIndividual> individuals = m3.addAnnotations(modelId,
							request.arguments.predicate, request.arguments.subject,
							request.arguments.object, extract(request.arguments.values, userId));
					relevantIndividuals.addAll(individuals);
				}
				// remove annotation
				else if (match(Operation.removeAnnotation, operation)){
					requireNotNull(request.arguments.values, "request.arguments.values");

					List<OWLNamedIndividual> individuals = m3.removeAnnotations(modelId,
							request.arguments.predicate, request.arguments.subject,
							request.arguments.object, extract(request.arguments.values, null));
					relevantIndividuals.addAll(individuals);
				}
				else {
					return error(response, "Unknown operation: "+operation, null);
				}
			}
			//model
			else if (match(Entity.model, entity)) {
				// get model
				if (match(Operation.get, operation)){
					nonMeta = true;
					requireNotNull(request.arguments, "request.arguments");
					modelId = checkModelId(modelId, request);
					renderBulk = true;
				}
				else if (match(Operation.generate, operation)) {
					nonMeta = true;
					requireNotNull(request.arguments, "request.arguments");
					requireNotNull(request.arguments.db, "request.arguments.db");
					requireNotNull(request.arguments.subject, "request.arguments.subject");
					renderBulk = true;
					modelId = m3.generateModel(request.arguments.subject, request.arguments.db);
					
					Collection<Pair<String, String>> annotations = extract(request.arguments.values, userId);
					if (annotations != null) {
						m3.addAnnotations(modelId, annotations);
					}
				}
				else if (match(Operation.generateBlank, operation)) {
					nonMeta = true;
					renderBulk = true;
					// db is optional
					String db = null;
					Collection<Pair<String, String>> annotations = null;
					if (request.arguments != null) {
						db = request.arguments.db;
						annotations = extract(request.arguments.values, userId);
					}
					modelId = m3.generateBlankModel(db);
					
					if (annotations != null) {
						m3.addAnnotations(modelId, annotations);
					}
				}
				else if (match(Operation.addAnnotation, operation)) {
					nonMeta = true;
					requireNotNull(request.arguments, "request.arguments");
					requireNotNull(request.arguments.values, "request.arguments.values");
					modelId = checkModelId(modelId, request);
					Collection<Pair<String, String>> annotations = extract(request.arguments.values, userId);
					if (annotations != null) {
						m3.addAnnotations(modelId, annotations);
					}
					renderModelAnnotations = true;
				}
				else if (match(Operation.removeAnnotation, operation)) {
					nonMeta = true;
					requireNotNull(request.arguments, "request.arguments");
					requireNotNull(request.arguments.values, "request.arguments.values");
					modelId = checkModelId(modelId, request);
					Collection<Pair<String, String>> annotations = extract(request.arguments.values, null);
					if (annotations != null) {
						m3.removeAnnotations(modelId, annotations);
					}
					renderModelAnnotations = true;
				}
				else if (match(Operation.exportModel, operation)) {
					if (nonMeta) {
						// can only be used with other "meta" operations in batch mode, otherwise it would lead to conflicts in the returned signal
						return error(response, "Export model can only be combined with other meta operations.", null);
					}
					requireNotNull(request.arguments, "request.arguments");
					modelId = checkModelId(modelId, request);
					export(response, modelId, m3);
				}
				else if (match(Operation.importModel, operation)) {
					nonMeta = true;
					requireNotNull(request.arguments, "request.arguments");
					requireNotNull(request.arguments.importModel, "request.arguments.importModel");
					modelId = m3.importModel(request.arguments.importModel);
					
					Collection<Pair<String, String>> annotations = extract(request.arguments.values, userId);
					if (annotations != null) {
						m3.addAnnotations(modelId, annotations);
					}
					renderBulk = true;
				}
				else if (match(Operation.storeModel, operation)) {
					requireNotNull(request.arguments, "request.arguments");
					modelId = checkModelId(modelId, request);
					Collection<Pair<String, String>> annotations = extract(request.arguments.values, userId);
					save(response, modelId, annotations, m3);
				}
				else if (match(Operation.allModelIds, operation)) {
					if (nonMeta) {
						// can only be used with other "meta" operations in batch mode, otherwise it would lead to conflicts in the returned signal
						return error(response, operation+" cannot be combined with other operations.", null);
					}
					getAllModelIds(response, m3);
				}
				else if (match(Operation.search, operation)) {
					if (nonMeta) {
						// can only be used with other "meta" operations in batch mode, otherwise it would lead to conflicts in the returned signal
						return error(response, operation+" cannot be combined with other operations.", null);
					}
					requireNotNull(request.arguments, "request.arguments");
					requireNotNull(request.arguments.values, "request.arguments.values");
					Collection<Pair<String, String>> values = extract(request.arguments.values, null);
					List<String> searchIds = new ArrayList<String>();
					if (values != null) {
						for (Pair<String, String> pair : values) {
							String key = pair.getKey();
							String val = pair.getValue();
							if ("id".equals(key) && val != null) {
								searchIds.add(val);
							}
						}
					}
					if (!searchIds.isEmpty()) {
						searchModels(response, searchIds, m3);
					}
					else {
						return error(response, "No query identifiers found in the request.", null);
					}
				}
				else {
					return error(response, "Unknown operation: "+operation, null);
				}
			}
			// relations
			else if (match(Entity.relations, entity)) {
				if (match(Operation.get, operation)){
					if (nonMeta) {
						// can only be used with other "meta" operations in batch mode, otherwise it would lead to conflicts in the returned signal
						return error(response, "Get Relations can only be combined with other meta operations.", null);
					}
					getRelations(response, m3);
				}
				else {
					return error(response, "Unknown operation: "+operation, null);
				}
			}
			// evidence
			else if (match(Entity.evidence, entity)) {
				if (match(Operation.get, operation)){
					if (nonMeta) {
						// can only be used with other "meta" operations in batch mode, otherwise it would lead to conflicts in the returned signal
						return error(response, "Get Evidences can only be combined with other meta operations.", null);
					}
					getEvidence(response, m3);
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
		if (modelId == null) {
			return error(response, "Empty batch calls are not supported, at least one request is required.", null);
		}
		// get model
		final LegoModelGenerator model = m3.getModel(modelId);
		// update reasoner
		// report state
		final OWLReasoner reasoner = model.getReasoner();
		reasoner.flush();
		final boolean isConsistent = reasoner.isConsistent();

		// create response.data
		if (renderBulk) {
			// render complete model
			response.data = m3.getModelObject(modelId);
			response.signal = M3BatchResponse.SIGNAL_REBUILD;
		}
		else {
			// render individuals
			MolecularModelJsonRenderer renderer = new MolecularModelJsonRenderer(model.getAboxOntology());
			response.data = renderer.renderIndividuals(relevantIndividuals);
			response.signal = M3BatchResponse.SIGNAL_MERGE;
		}
		
		// add model annotations
		if (renderModelAnnotations) {
			List<Object> anObjs = MolecularModelJsonRenderer.renderModelAnnotations(model.getAboxOntology());
			response.data.put(KEY.annotations.name(), anObjs);
		}

		// add other infos to data
		response.data.put("id", modelId);
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

	private void getAllModelIds(M3BatchResponse response, MolecularModelManager m3) throws IOException {
		Set<String> allModelIds = m3.getAvailableModelIds();
		//Set<String> scratchModelIds = mmm.getScratchModelIds();
		//Set<String> storedModelIds = mmm.getStoredModelIds();
		//Set<String> memoryModelIds = mmm.getCurrentModelIds();

		if (response.data == null) {
			response.data = new HashMap<Object, Object>();
			response.message_type = M3BatchResponse.MESSAGE_TYPE_SUCCESS;
			response.message = "success: " + response.data.size();
			response.signal = M3BatchResponse.SIGNAL_META;
		}
		
		response.data.put("model_ids", allModelIds);
		//response.data.put("models_memory", memoryModelIds);
		//response.data.put("models_stored", storedModelIds);
		//response.data.put("models_scratch", scratchModelIds);
	}
	
	private void searchModels(M3BatchResponse response, List<String> ids, MolecularModelManager m3) throws IOException {
		Set<String> allModelIds = m3.searchModels(ids);
		if (response.data == null) {
			response.data = new HashMap<Object, Object>();
			response.message_type = M3BatchResponse.MESSAGE_TYPE_SUCCESS;
			response.message = "success: " + response.data.size();
			response.signal = M3BatchResponse.SIGNAL_META;
		}
		response.data.put("model_ids", allModelIds);
	}

	private void getRelations(M3BatchResponse response, MolecularModelManager m3) throws OWLOntologyCreationException {
		List<Map<Object,Object>> relList = MolecularModelJsonRenderer.renderRelations(m3, relevantRelations);
		if (response.data == null) {
			response.data = new HashMap<Object, Object>();
			response.message_type = M3BatchResponse.MESSAGE_TYPE_SUCCESS;
			response.message = "success: " + response.data.size();
			response.signal = M3BatchResponse.SIGNAL_META;
		}
		response.data.put(Entity.relations.name(), relList);
	}
	
	private void getEvidence(M3BatchResponse response, MolecularModelManager m3) throws OWLException, IOException {
		List<Map<Object,Object>> evidencesList = MolecularModelJsonRenderer.renderEvidences(m3);
		if (response.data == null) {
			response.data = new HashMap<Object, Object>();
			response.message_type = M3BatchResponse.MESSAGE_TYPE_SUCCESS;
			response.message = "success: " + response.data.size();
			response.signal = M3BatchResponse.SIGNAL_META;
		}
		response.data.put(Entity.evidence.name(), evidencesList);
	}
	
	private void export(M3BatchResponse response, String modelId, MolecularModelManager m3) throws OWLOntologyStorageException {
		String exportModel = m3.exportModel(modelId);
		if (response.data == null) {
			response.data = new HashMap<Object, Object>();
			response.message_type = M3BatchResponse.MESSAGE_TYPE_SUCCESS;
			response.message = "success";
			response.signal = M3BatchResponse.SIGNAL_META;
		}
		response.data.put(Operation.exportModel.getLbl(), exportModel);
	}
	
	private void save(M3BatchResponse response, String modelId, Collection<Pair<String,String>> annotations, MolecularModelManager m3) throws OWLOntologyStorageException, OWLOntologyCreationException, IOException, UnknownIdentifierException {
		m3.saveModel(modelId, annotations);
		if (response.data == null) {
			response.data = new HashMap<Object, Object>();
			response.message_type = M3BatchResponse.MESSAGE_TYPE_SUCCESS;
			response.message = "success: " + response.data.size();
			response.signal = M3BatchResponse.SIGNAL_META;
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
	
	private Collection<Pair<String, String>> extract(M3Pair[] values, String userId) {
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
				result = new ArrayList<Pair<String,String>>(1);
				result.add(Pair.of(LegoAnnotationType.contributor.name(), userId));
			}
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
