package owltools.gaf.lego.server.handler;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.glassfish.jersey.server.JSONP;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.gaf.lego.MolecularModelManager.UnknownIdentifierException;
import owltools.gaf.lego.UndoAwareMolecularModelManager;
import owltools.gaf.lego.UndoAwareMolecularModelManager.UndoMetadata;
import owltools.gaf.lego.json.JsonAnnotation;
import owltools.gaf.lego.json.JsonModel;
import owltools.gaf.lego.json.JsonOwlFact;
import owltools.gaf.lego.json.JsonOwlIndividual;
import owltools.gaf.lego.json.MolecularModelJsonRenderer;
import owltools.gaf.lego.server.external.ExternalLookupService;
import owltools.gaf.lego.server.external.ExternalLookupService.LookupEntry;
import owltools.gaf.lego.server.handler.M3BatchHandler.M3BatchResponse.ResponseDataKey;
import owltools.util.ModelContainer;

import com.google.common.reflect.TypeToken;

public class JsonOrJsonpBatchHandler extends OperationsImpl implements M3BatchHandler {

	public static final String JSONP_DEFAULT_CALLBACK = "jsonp";
	public static final String JSONP_DEFAULT_OVERWRITE = "json.wrf";
	
	
	public static boolean USE_USER_ID = true;
	public static boolean USE_CREATION_DATE = true;
	public static boolean ADD_INFERENCES = true;
	public static boolean VALIDATE_BEFORE_SAVE = true;
	public static boolean ENFORCE_EXTERNAL_VALIDATE = false;
	public boolean CHECK_LITERAL_IDENTIFIERS = true; // TODO remove the temp work-around
	
	private static final Logger logger = Logger.getLogger(JsonOrJsonpBatchHandler.class);
	
	public JsonOrJsonpBatchHandler(UndoAwareMolecularModelManager models, 
			Set<OWLObjectProperty> importantRelations,
			ExternalLookupService externalLookupService) {
		super(models, importantRelations, externalLookupService);
	}

	private final Type requestType = new TypeToken<M3Request[]>(){

		// generated
		private static final long serialVersionUID = 5452629810143143422L;
		
	}.getType();
	
	@Override
	boolean enforceExternalValidate() {
		return ENFORCE_EXTERNAL_VALIDATE;
	}

	@Override
	boolean checkLiteralIdentifiers() {
		return CHECK_LITERAL_IDENTIFIERS;
	}

	@Override
	boolean validateBeforeSave() {
		return VALIDATE_BEFORE_SAVE;
	}

	@Override
	boolean useUserId() {
		return USE_USER_ID;
	}

	@Override
	boolean useCreationDate() {
		return USE_CREATION_DATE;
	}

	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3BatchResponse m3BatchGet(String intention, String packetId, String requestString) {
		return m3Batch(null, intention, packetId, requestString, false);
	}
	
	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3BatchResponse m3BatchGetPrivileged(String uid, String intention, String packetId, String requestString) {
		return m3Batch(uid, intention, packetId, requestString, true);
	}

	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3BatchResponse m3BatchPost(String intention, String packetId, String requestString) {
		return m3Batch(null, intention, packetId, requestString, false);
	}
	
	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3BatchResponse m3BatchPostPrivileged(String uid, String intention, String packetId, String requestString) {
		return m3Batch(uid, intention, packetId, requestString, true);
	}

	private static String checkPacketId(String packetId) {
		if (packetId == null) {
			packetId = PacketIdGenerator.generateId();
		}
		return packetId;
	}
	
	@Override
	public M3BatchResponse m3Batch(String uid, String intention, String packetId, M3Request[] requests, boolean isPrivileged) {
		M3BatchResponse response = new M3BatchResponse(uid, intention, checkPacketId(packetId));
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
	
	private M3BatchResponse m3Batch(String uid, String intention, String packetId, String requestString, boolean isPrivileged) {
		M3BatchResponse response = new M3BatchResponse(uid, intention, checkPacketId(packetId));
		try {
			M3Request[] requests = MolecularModelJsonRenderer.parseFromJson(requestString, requestType);
			return m3Batch(response, requests, uid, isPrivileged);
		} catch (Exception e) {
			return error(response, "Could not successfully handle batch request.", e);
		} catch (Throwable t) {
			logger.error("A critical error occured.", t);
			return error(response, "An internal error occured at the server level.", t);
		}
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
		response.data = new HashMap<ResponseDataKey, Object>();
		MolecularModelJsonRenderer renderer = createModelRenderer(model, externalLookupService);
		if (values.renderBulk) {
			// render complete model
			JsonModel jsonModel = renderer.renderModel();
			initMap(jsonModel, response.data, values.renderModelAnnotations);
			response.signal = M3BatchResponse.SIGNAL_REBUILD;
			if (ADD_INFERENCES) {
				response.data.put(ResponseDataKey.individuals_i, renderer.renderModelInferences(reasoner));
			}
		}
		else {
			// render individuals
			Pair<JsonOwlIndividual[],JsonOwlFact[]> pair = renderer.renderIndividuals(values.relevantIndividuals);
			response.data.put(ResponseDataKey.individuals, pair.getLeft());
			response.data.put(ResponseDataKey.facts, pair.getRight());
			response.signal = M3BatchResponse.SIGNAL_MERGE;
			if (ADD_INFERENCES) {
				response.data.put(ResponseDataKey.individuals_i, renderer.renderInferences(values.relevantIndividuals, reasoner));
			}
			// add model annotations
			if (values.renderModelAnnotations) {
				List<JsonAnnotation> anObjs = MolecularModelJsonRenderer.renderModelAnnotations(model.getAboxOntology());
				response.data.put(ResponseDataKey.annotations, anObjs);
			}
		}
		
		// add other infos to data
		response.data.put(ResponseDataKey.id, values.modelId);
		if (!isConsistent) {
			response.data.put(ResponseDataKey.inconsistent_p, Boolean.TRUE);
		}
		// These are required for an "okay" response.
		response.message_type = M3BatchResponse.MESSAGE_TYPE_SUCCESS;
		if( response.message == null ){
			response.message = "success";
		}
		return response;
	}

	public static void initMap(JsonModel jsonModel, Map<ResponseDataKey, Object> map, boolean addAnnotations) {
		map.put(ResponseDataKey.individuals, jsonModel.individuals);
		map.put(ResponseDataKey.facts, jsonModel.facts);
		map.put(ResponseDataKey.properties, jsonModel.properties);
		if (addAnnotations) {
			map.put(ResponseDataKey.annotations, jsonModel.annotations);
		}
	}
	
	/**
	 * @param model
	 * @param externalLookupService
	 * @return renderer
	 */
	static MolecularModelJsonRenderer createModelRenderer(
			final ModelContainer model, 
			final ExternalLookupService externalLookupService) {
		
		MolecularModelJsonRenderer renderer;
		if (externalLookupService != null) {
			renderer = new MolecularModelJsonRenderer(model.getAboxOntology()) {

				@Override
				protected String getLabel(OWLNamedObject i, String id) {
					String label = super.getLabel(i, id);
					if (label == null ) {
						// TODO get taxon for now take the first one
						// externalLookupService.lookup(id, taxon);
						List<LookupEntry> lookup = externalLookupService.lookup(id);
						if (lookup != null && !lookup.isEmpty()) {
							LookupEntry entry = lookup.iterator().next();
							label = entry.label;
						}
					}
					return label;
				}

			};
		}
		else {
			renderer = new MolecularModelJsonRenderer(model.getAboxOntology());
		}
		return renderer;
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
	
	protected void checkPermissions(Entity entity, Operation operation, boolean isPrivileged) throws InsufficientPermissionsException {
		// TODO make this configurable
		if (isPrivileged == false) {
			switch (operation) {
			case get:
			case exportModel:
			case exportModelLegacy:
			case allModelIds:
			case allModelMeta:
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
}
