package owltools.gaf.lego.server.handler;

import static owltools.gaf.lego.server.handler.JsonOrJsonpModelHandler.*;

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
import org.glassfish.jersey.server.JSONP;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.gaf.lego.LegoModelGenerator;
import owltools.gaf.lego.MolecularModelJsonRenderer;
import owltools.gaf.lego.MolecularModelManager;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class JsonOrJsonpBatchHandler implements M3BatchHandler {

	private final MolecularModelManager m3;

	public JsonOrJsonpBatchHandler(MolecularModelManager models) {
		super();
		this.m3 = models;
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
			return m3Batch(response, requests);
		} catch (Exception e) {
			return error(response, "Could not successfully handle batch request.", e);
		}
	}

	@Override
	public M3BatchResponse m3Batch(String uid, String intention, M3Request[] requests) {
		M3BatchResponse response = new M3BatchResponse(uid, intention);
		try {
			return m3Batch(response, requests);
		} catch (Exception e) {
			return error(response, "Could not successfully complete batch request.", e);
		}
	}
	
	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3BatchResponse m3BatchPost(String uid, String intention, String requestString) {
		return m3BatchGet(uid, intention, requestString);
	}

	private M3BatchResponse m3Batch(M3BatchResponse response, M3Request[] requests) throws Exception {
		final Set<OWLNamedIndividual> relevantIndividuals = new HashSet<OWLNamedIndividual>();
		boolean renderBulk = false;
		boolean nonMeta = false;
		String modelId = null;
		for (M3Request request : requests) {
			requireNotNull(request, "request");
			final String entity = StringUtils.trimToNull(request.entity);
			final String operation = StringUtils.trimToNull(request.operation);
			

			// individual
			if ("individual".equals(entity)) {
				nonMeta = true;
				requireNotNull(request.arguments, "request.arguments");
				modelId = checkModelId(modelId, request);

				// get info, no modification
				if ("get".equals(operation)) {
					requireNotNull(request.arguments.individual, "request.arguments.individual");
					OWLNamedIndividual i = m3.getNamedIndividual(modelId, request.arguments.individual);
					relevantIndividuals.add(i);
					
				}
				// create from class
				else if ("create".equals(operation)) {
					// required: subject
					// optional: expressions, values
					requireNotNull(request.arguments.subject, "request.arguments.subject");
					Collection<Pair<String, String>> annotations = extract(request.arguments.values);
					Pair<String, OWLNamedIndividual> individualPair = m3.createIndividualNonReasoning(modelId, request.arguments.subject, annotations);
					relevantIndividuals.add(individualPair.getValue());

					for(M3Expression expression : request.arguments.expressions) {
						requireNotNull(expression.type, "expression.type");
						requireNotNull(expression.literal, "expression.literal");
						if ("class".equals(expression.type)) {
							m3.addTypeNonReasoning(modelId, individualPair.getKey(), expression.literal);
						}
						else if ("svf".equals(expression.type)) {
							requireNotNull(expression.onProp, "expression.onProp");
							m3.addTypeNonReasoning(modelId, individualPair.getKey(), expression.onProp, expression.literal);
						}
					}
				}
				// remove individual (and all axioms using it)
				else if ("remove".equals(operation)){
					// required: modelId, individual
					requireNotNull(request.arguments.individual, "request.arguments.individual");
					m3.deleteIndividual(modelId, request.arguments.individual);
					renderBulk = true;
				}				
				// add type / named class assertion
				else if ("add-type".equals(operation)){
					// required: individual, expressions
					requireNotNull(request.arguments.individual, "request.arguments.individual");
					requireNotNull(request.arguments.expressions, "request.arguments.expressions");
					for(M3Expression expression : request.arguments.expressions) {
						requireNotNull(expression.type, "expression.type");
						requireNotNull(expression.literal, "expression.literal");
						if ("class".equals(expression.type)) {
							OWLNamedIndividual i = m3.addTypeNonReasoning(modelId, 
									request.arguments.individual, expression.literal);
							relevantIndividuals.add(i);
						}
						else if ("svf".equals(expression.type)) {
							requireNotNull(expression.onProp, "expression.onProp");
							OWLNamedIndividual i = m3.addTypeNonReasoning(modelId,
									request.arguments.individual, expression.onProp, expression.literal);
							relevantIndividuals.add(i);
						}
					}
				}
				// remove type / named class assertion
				else if ("remove-type".equals(operation)){
					// required: individual, expressions
					requireNotNull(request.arguments.individual, "request.arguments.individual");
					requireNotNull(request.arguments.expressions, "request.arguments.expressions");
					for(M3Expression expression : request.arguments.expressions) {
						requireNotNull(expression.type, "expression.type");
						requireNotNull(expression.literal, "expression.literal");
						if ("class".equals(expression.type)) {
							OWLNamedIndividual i = m3.removeTypeNonReasoning(modelId,
									request.arguments.individual, expression.literal);
							relevantIndividuals.add(i);
						}
						else if ("svf".equals(expression.type)) {
							requireNotNull(expression.onProp, "expression.onProp");
							OWLNamedIndividual i = m3.removeTypeNonReasoning(modelId,
									request.arguments.individual, expression.onProp, expression.literal);
							relevantIndividuals.add(i);
						}
					}
				}
				// add annotation
				else if ("add-annotation".equals(operation)){
					// required: individual, values
					requireNotNull(request.arguments.individual, "request.arguments.individual");
					requireNotNull(request.arguments.values, "request.arguments.values");

					OWLNamedIndividual i = m3.addAnnotations(modelId, request.arguments.individual,
							extract(request.arguments.values));
					relevantIndividuals.add(i);
				}
				// remove annotation
				else if ("remove-annotation".equals(operation)){
					// required: individual, values
					requireNotNull(request.arguments.individual, "request.arguments.individual");
					requireNotNull(request.arguments.values, "request.arguments.values");

					OWLNamedIndividual i = m3.removeAnnotations(modelId, request.arguments.individual,
							extract(request.arguments.values));
					relevantIndividuals.add(i);
				}
				else {
					return error(response, "Unknown operation: "+operation, null);
				}
			}
			// edge
			else if ("edge".equals(entity)) {
				nonMeta = true;
				requireNotNull(request.arguments, "request.arguments");
				modelId = checkModelId(modelId, request);
				// required: subject, predicate, object
				requireNotNull(request.arguments.subject, "request.arguments.subject");
				requireNotNull(request.arguments.predicate, "request.arguments.predicate");
				requireNotNull(request.arguments.object, "request.arguments.object");

				// add edge
				if ("add".equals(operation)){
					// optional: values
					List<OWLNamedIndividual> individuals = m3.addFactNonReasoning(modelId,
							request.arguments.predicate, request.arguments.subject,
							request.arguments.object, extract(request.arguments.values));
					relevantIndividuals.addAll(individuals);
				}
				// remove edge
				else if ("remove".equals(operation)){
					List<OWLNamedIndividual> individuals = m3.removeFactNonReasoning(modelId,
							request.arguments.predicate, request.arguments.subject,
							request.arguments.object);
					relevantIndividuals.addAll(individuals);
				}
				// add annotation
				else if ("add-annotation".equals(operation)){
					requireNotNull(request.arguments.values, "request.arguments.values");

					List<OWLNamedIndividual> individuals = m3.addAnnotations(modelId,
							request.arguments.predicate, request.arguments.subject,
							request.arguments.object, extract(request.arguments.values));
					relevantIndividuals.addAll(individuals);
				}
				// remove annotation
				else if ("remove-annotation".equals(operation)){
					requireNotNull(request.arguments.values, "request.arguments.values");

					List<OWLNamedIndividual> individuals = m3.removeAnnotations(modelId,
							request.arguments.predicate, request.arguments.subject,
							request.arguments.object, extract(request.arguments.values));
					relevantIndividuals.addAll(individuals);
				}
				else {
					return error(response, "Unknown operation: "+operation, null);
				}
			}
			//model
			else if ("model".equals(entity)) {
				requireNotNull(request.arguments, "request.arguments");
				// get model
				if ("get".equals(operation)){
					nonMeta = true;
					modelId = checkModelId(modelId, request);
					renderBulk = true;
				}
				else if ("generate".equals(operation)) {
					nonMeta = true;
					requireNotNull(request.arguments.db, "request.arguments.db");
					requireNotNull(request.arguments.subject, "request.arguments.subject");
					renderBulk = true;
					modelId = m3.generateModel(request.arguments.subject, request.arguments.db);
				}
				else if ("generate-blank".equals(operation)) {
					nonMeta = true;
					renderBulk = true;
					requireNotNull(request.arguments.db, "request.arguments.db");
					modelId = m3.generateBlankModel(request.arguments.db);
				}
				else if ("export".equals(operation)) {
					if (nonMeta) {
						// can only be used with other "meta" operations in batch mode, otherwise it would lead to conflicts in the returned signal
						return error(response, "Export model can only be combined with other meta operations.", null);
					}
					modelId = checkModelId(modelId, request);
					export(response, modelId, m3);
				}
				else if ("import".equals(operation)) {
					nonMeta = true;
					requireNotNull(request.arguments.importModel, "request.arguments.importModel");
					modelId = m3.importModel(request.arguments.importModel);
					renderBulk = true;
				}
				else if ("all-modelIds".equals(operation)) {
					if (nonMeta) {
						// can only be used with other "meta" operations in batch mode, otherwise it would lead to conflicts in the returned signal
						return error(response, operation+" cannot be combined with other operations.", null);
					}
					getAllModelIds(response, m3);
				}
				else {
					return error(response, "Unknown operation: "+operation, null);
				}
			}
			// relations
			else if ("relations".equals(entity)) {
				if ("get".equals(operation)){
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
			// evidences
			else if ("evidences".equals(entity)) {
				if ("get".equals(operation)){
					if (nonMeta) {
						// can only be used with other "meta" operations in batch mode, otherwise it would lead to conflicts in the returned signal
						return error(response, "Get Evidences can only be combined with other meta operations.", null);
					}
					getEvidences(response, m3);
				}
				else {
					return error(response, "Unknown operation: "+operation, null);
				}
			}
			else {
				return error(response, "Unknown entity: "+entity, null);
			}
		}
		if ("meta".equals(response.signal)) {
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
			response.signal = "rebuild";
		}
		else {
			// render individuals
			MolecularModelJsonRenderer renderer = new MolecularModelJsonRenderer(model.getAboxOntology());
			response.data = renderer.renderIndividuals(relevantIndividuals);
			response.signal = "merge";
		}

		// add other infos to data
		response.data.put("id", modelId);
		if (!isConsistent) {
			response.data.put("inconsistent_p", Boolean.TRUE);
		}
		response.message_type = "success";
		return response;
	}

	private void getAllModelIds(M3BatchResponse response, MolecularModelManager m3) throws IOException {
		Set<String> allModelIds = m3.getAvailableModelIds();
		//Set<String> scratchModelIds = mmm.getScratchModelIds();
		//Set<String> storedModelIds = mmm.getStoredModelIds();
		//Set<String> memoryModelIds = mmm.getCurrentModelIds();

		if (response.data == null) {
			response.data = new HashMap<Object, Object>();
			response.message_type = "success";
			response.signal = "meta";
		}
		
		response.data.put("models_all", allModelIds);
		//response.data.put("models_memory", memoryModelIds);
		//response.data.put("models_stored", storedModelIds);
		//response.data.put("models_scratch", scratchModelIds);
	}

	private void getRelations(M3BatchResponse response, MolecularModelManager m3) throws OWLOntologyCreationException {
		List<Map<Object,Object>> relList = MolecularModelJsonRenderer.renderRelations(m3);
		if (response.data == null) {
			response.data = new HashMap<Object, Object>();
			response.message_type = "success";
			response.signal = "meta";
		}
		response.data.put("relations", relList);
	}
	
	private void getEvidences(M3BatchResponse response, MolecularModelManager m3) throws OWLException, IOException {
		List<Map<Object,Object>> evidencesList = MolecularModelJsonRenderer.renderEvidences(m3);
		if (response.data == null) {
			response.data = new HashMap<Object, Object>();
			response.message_type = "success";
			response.signal = "meta";
		}
		response.data.put("evidences", evidencesList);
	}
	
	private void export(M3BatchResponse response, String modelId, MolecularModelManager m3) throws OWLOntologyStorageException {
		String exportModel = m3.exportModel(modelId);
		if (response.data == null) {
			response.data = new HashMap<Object, Object>();
			response.message_type = "success";
			response.signal = "meta";
		}
		response.data.put("export", exportModel);
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
		final String currentModelId = request.arguments.modelId;
		requireNotNull(currentModelId, "request.arguments.modelId");
		if (modelId == null) {
			modelId = currentModelId;
		}
		else {
			if (modelId.equals(currentModelId) == false) {
				throw new MultipleModelIdsParameterException("Using multiple modelIds in one batch call is not supported.");
			}
		}
		return modelId;
	}
	
	private Collection<Pair<String, String>> extract(M3Pair[] values) {
		Collection<Pair<String, String>> result = null;
		if (values != null && values.length > 0) {
			result = new ArrayList<Pair<String,String>>();
			for (M3Pair m3Pair : values) {
				if (m3Pair.key != null && m3Pair.value != null) {
					result.add(Pair.of(m3Pair.key, m3Pair.value));
				}
			}
		}
		return result;
	}

	/*
	 * commentary is now to be a string, not an unknown multi-leveled object.
	 */
	private M3BatchResponse error(M3BatchResponse state, String msg, Exception e) {
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
	
	private static class MissingParameterException extends Exception {

		private static final long serialVersionUID = 4362299465121954598L;

		/**
		 * @param message
		 */
		public MissingParameterException(String message) {
			super(message);
		}
		
	}
	
	private static class MultipleModelIdsParameterException extends Exception {

		private static final long serialVersionUID = 4362299465121954598L;

		/**
		 * @param message
		 */
		public MultipleModelIdsParameterException(String message) {
			super(message);
		}
		
	}
}
