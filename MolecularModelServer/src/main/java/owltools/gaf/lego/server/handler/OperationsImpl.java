package owltools.gaf.lego.server.handler;

import static owltools.gaf.lego.server.handler.OperationsTools.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import owltools.gaf.lego.IdStringManager;
import owltools.gaf.lego.IdStringManager.AnnotationShorthand;
import owltools.gaf.lego.MolecularModelManager;
import owltools.gaf.lego.MolecularModelManager.UnknownIdentifierException;
import owltools.gaf.lego.UndoAwareMolecularModelManager;
import owltools.gaf.lego.UndoAwareMolecularModelManager.ChangeEvent;
import owltools.gaf.lego.UndoAwareMolecularModelManager.UndoMetadata;
import owltools.gaf.lego.json.JsonAnnotation;
import owltools.gaf.lego.json.JsonEvidenceInfo;
import owltools.gaf.lego.json.JsonOwlObject;
import owltools.gaf.lego.json.JsonRelationInfo;
import owltools.gaf.lego.json.MolecularModelJsonRenderer;
import owltools.gaf.lego.server.external.ExternalLookupService;
import owltools.gaf.lego.server.handler.M3BatchHandler.M3BatchResponse;
import owltools.gaf.lego.server.handler.M3BatchHandler.M3BatchResponse.MetaResponse;
import owltools.gaf.lego.server.handler.M3BatchHandler.M3BatchResponse.ResponseData;
import owltools.gaf.lego.server.handler.M3BatchHandler.M3Request;
import owltools.gaf.lego.server.handler.M3BatchHandler.Operation;
import owltools.gaf.lego.server.handler.OperationsTools.MissingParameterException;
import owltools.gaf.lego.server.validation.BeforeSaveModelValidator;

/**
 * Separate the actual calls to the {@link UndoAwareMolecularModelManager} from the
 * request, error and response handling.
 * 
 * @see JsonOrJsonpBatchHandler
 */
abstract class OperationsImpl {

	final UndoAwareMolecularModelManager m3;
	final Set<OWLObjectProperty> importantRelations;
	final BeforeSaveModelValidator beforeSaveValidator;
	final ExternalLookupService externalLookupService;

	OperationsImpl(UndoAwareMolecularModelManager models, 
			Set<OWLObjectProperty> importantRelations,
			ExternalLookupService externalLookupService) {
		super();
		this.m3 = models;
		this.importantRelations = importantRelations;
		this.externalLookupService = externalLookupService;
		this.beforeSaveValidator = new BeforeSaveModelValidator();
	}

	abstract boolean enforceExternalValidate();

	abstract boolean checkLiteralIdentifiers();
	
	abstract boolean validateBeforeSave();
	
	abstract boolean useUserId();
	
	static class BatchHandlerValues {
		
		final Set<OWLNamedIndividual> relevantIndividuals = new HashSet<OWLNamedIndividual>();
		boolean renderBulk = false;
		boolean renderModelAnnotations = false;
		boolean nonMeta = false;
		String modelId = null;
		Map<String, Pair<String, OWLNamedIndividual>> individualVariables = new HashMap<String, Pair<String, OWLNamedIndividual>>();
		
		public boolean notVariable(String id) {
			return individualVariables.containsKey(id) == false;
		}
		
		public String getVariableValueId(String id) throws UnknownIdentifierException {
			if (individualVariables.containsKey(id)) {
				Pair<String, OWLNamedIndividual> pair = individualVariables.get(id);
				if (pair == null) {
					throw new UnknownIdentifierException("Variable "+id+" has a null value.");
				}
				return pair.getKey();
			}
			else {
				return id;
			}
		}
	}
	

	/**
	 * Handle the request for an operation regarding an individual.
	 * 
	 * @param request
	 * @param operation
	 * @param userId
	 * @param token
	 * @param values
	 * @return error or null
	 * @throws Exception 
	 */
	String handleRequestForIndividual(M3Request request, Operation operation, String userId, UndoMetadata token, BatchHandlerValues values) throws Exception {
		values.nonMeta = true;
		requireNotNull(request.arguments, "request.arguments");
		values.modelId = checkModelId(values.modelId, request);

		// get info, no modification
		if (Operation.get == operation) {
			requireNotNull(request.arguments.individual, "request.arguments.individual");
			// check for that the request.arguments.individual is not a variable
			if (values.notVariable(request.arguments.individual)) {
				OWLNamedIndividual i = m3.getNamedIndividual(values.modelId, request.arguments.individual);
				values.relevantIndividuals.add(i);
				if (request.arguments.assignToVariable != null) {
					values.individualVariables.put(request.arguments.assignToVariable, 
							Pair.of(request.arguments.individual, i));
				}
			}
			else {
				Pair<String, OWLNamedIndividual> pair = values.individualVariables.get(request.arguments.individual);
				if (pair != null) {
					values.relevantIndividuals.add(pair.getRight());
				}
			}
		}
		// create individual (look-up variable first) and add type
		else if (Operation.add == operation) {
			// required: expression
			// optional: more expressions, values
			requireNotNull(request.arguments.expressions, "request.arguments.expressions");
			Set<OWLAnnotation> annotations = extract(request.arguments.values, userId, true, values, values.modelId);
			Pair<String, OWLNamedIndividual> individualPair;
			List<OWLClassExpression> clsExpressions = new ArrayList<OWLClassExpression>(request.arguments.expressions.length);
			for(JsonOwlObject expression : request.arguments.expressions) {
				OWLClassExpression cls = parseM3Expression(expression, values);
				clsExpressions.add(cls);
			}
			if (values.notVariable(request.arguments.individual)) {
				if (clsExpressions.isEmpty() == false) {
					// create individual for given class expression
					OWLClassExpression head = clsExpressions.get(0);
					individualPair = m3.createIndividualNonReasoning(values.modelId, head, annotations, token);
					// add to render list and set variable
					values.relevantIndividuals.add(individualPair.getValue());
					if (request.arguments.assignToVariable != null) {
						values.individualVariables.put(request.arguments.assignToVariable, individualPair);
					}
					// add types for the remaining expressions
					if (clsExpressions.size() > 1) {
						List<OWLClassExpression> tail = clsExpressions.subList(1, clsExpressions.size());
						for (OWLClassExpression ce : tail) {
							m3.addTypeNonReasoning(values.modelId, individualPair.getKey(), ce, token);
						}
					}
				}
			}
			else {
				individualPair = values.individualVariables.get(request.arguments.individual);
				for (OWLClassExpression clsExpression : clsExpressions) {
					m3.addTypeNonReasoning(values.modelId, individualPair.getKey(), clsExpression, token);
				}
			}
			addContributor(values.modelId, userId, token, m3);
		}
		// remove individual (and all axioms using it)
		else if (Operation.remove == operation){
			// required: modelId, individual
			requireNotNull(request.arguments.individual, "request.arguments.individual");
			Set<IRI> annotationIRIs;
			if (values.notVariable(request.arguments.individual)) {
				annotationIRIs = m3.deleteIndividual(values.modelId, request.arguments.individual, token);
			}
			else {
				Pair<String, OWLNamedIndividual> pair = values.individualVariables.get(request.arguments.individual);
				annotationIRIs = m3.deleteIndividual(values.modelId, pair.getKey(), token);
			}
			handleRemovedAnnotationIRIs(annotationIRIs, values.modelId, token);
			addContributor(values.modelId, userId, token, m3);
			values.renderBulk = true;
		}				
		// add type / named class assertion
		else if (Operation.addType == operation){
			// required: individual, expressions
			requireNotNull(request.arguments.individual, "request.arguments.individual");
			requireNotNull(request.arguments.expressions, "request.arguments.expressions");
			
			Set<OWLAnnotation> annotations = createGeneratedAnnotations(values.modelId, userId);
			
			for(JsonOwlObject expression : request.arguments.expressions) {
				OWLClassExpression cls = parseM3Expression(expression, values);
				if (values.notVariable(request.arguments.individual)) {
					OWLNamedIndividual i = m3.addTypeNonReasoning(values.modelId, request.arguments.individual, cls, token);
					values.relevantIndividuals.add(i);

					if (request.arguments.assignToVariable != null) {
						values.individualVariables.put(request.arguments.assignToVariable, 
								Pair.of(request.arguments.individual, i));
					}
					
					m3.addAnnotations(values.modelId, request.arguments.individual, annotations, token);
				}
				else {
					Pair<String, OWLNamedIndividual> pair = values.individualVariables.get(request.arguments.individual);
					OWLNamedIndividual i = m3.addTypeNonReasoning(values.modelId, pair.getKey(), cls, token);
					values.relevantIndividuals.add(i);
					m3.addAnnotations(values.modelId, pair.getLeft(), annotations, token);
				}
			}
			addContributor(values.modelId, userId, token, m3);
		}
		// remove type / named class assertion
		else if (Operation.removeType == operation){
			// required: individual, expressions
			requireNotNull(request.arguments.individual, "request.arguments.individual");
			requireNotNull(request.arguments.expressions, "request.arguments.expressions");
			
			Set<OWLAnnotation> annotations = createGeneratedAnnotations(values.modelId, userId);
			
			for(JsonOwlObject expression : request.arguments.expressions) {
				OWLClassExpression cls = parseM3Expression(expression, values);
				if (values.notVariable(request.arguments.individual)) {
					OWLNamedIndividual i = m3.removeTypeNonReasoning(values.modelId, request.arguments.individual, cls, token);
					values.relevantIndividuals.add(i);

					if (request.arguments.assignToVariable != null) {
						values.individualVariables.put(request.arguments.assignToVariable, 
								Pair.of(request.arguments.individual, i));
					}
					m3.addAnnotations(values.modelId, request.arguments.individual, annotations, token);
				}
				else {
					Pair<String, OWLNamedIndividual> pair = values.individualVariables.get(request.arguments.individual);
					OWLNamedIndividual i = m3.removeTypeNonReasoning(values.modelId, pair.getKey(), cls, token);
					values.relevantIndividuals.add(i);
					m3.addAnnotations(values.modelId, pair.getLeft(), annotations, token);
				}
			}
			addContributor(values.modelId, userId, token, m3);
		}
		// add annotation
		else if (Operation.addAnnotation == operation){
			// required: individual, values
			requireNotNull(request.arguments.individual, "request.arguments.individual");
			requireNotNull(request.arguments.values, "request.arguments.values");

			Set<OWLAnnotation> annotations = extract(request.arguments.values, userId, false, values, values.modelId);
			if(values.notVariable(request.arguments.individual)) {
				OWLNamedIndividual i = m3.addAnnotations(values.modelId, request.arguments.individual,
						annotations, token);
				values.relevantIndividuals.add(i);

				if (request.arguments.assignToVariable != null) {
					values.individualVariables.put(request.arguments.assignToVariable, 
							Pair.of(request.arguments.individual, i));
				}
			}
			else {
				Pair<String, OWLNamedIndividual> pair = values.individualVariables.get(request.arguments.individual);
				OWLNamedIndividual i = m3.addAnnotations(values.modelId, pair.getKey(),
						annotations, token);
				values.relevantIndividuals.add(i);
			}
			addContributor(values.modelId, userId, token, m3);
		}
		// remove annotation
		else if (Operation.removeAnnotation == operation){
			// required: individual, values
			requireNotNull(request.arguments.individual, "request.arguments.individual");
			requireNotNull(request.arguments.values, "request.arguments.values");

			Set<OWLAnnotation> annotations = extract(request.arguments.values, null, false, values, values.modelId);
			Set<IRI> usedIRIs = MolecularModelManager.extractIRIValues(annotations);
			if(values.notVariable(request.arguments.individual)) {
				OWLNamedIndividual i = m3.removeAnnotations(values.modelId, request.arguments.individual,
						annotations, token);
				values.relevantIndividuals.add(i);

				if (request.arguments.assignToVariable != null) {
					values.individualVariables.put(request.arguments.assignToVariable, 
							Pair.of(request.arguments.individual, i));
				}
			}
			else {
				Pair<String, OWLNamedIndividual> pair = values.individualVariables.get(request.arguments.individual);
				OWLNamedIndividual i = m3.removeAnnotations(values.modelId, pair.getKey(),
						annotations, token);
				values.relevantIndividuals.add(i);
			}
			handleRemovedAnnotationIRIs(usedIRIs, values.modelId, token);
			addContributor(values.modelId, userId, token, m3);
		}
		else {
			return "Unknown operation: "+operation;
		}
		return null;
	}
	
	private void handleRemovedAnnotationIRIs(Set<IRI> iriSets, String modelId, UndoMetadata token) throws UnknownIdentifierException {
		if (iriSets != null) {
			for (IRI iri : iriSets) {
				m3.deleteIndividualNonReasoning(modelId, iri, token);
			}
		}
	}

	private OWLClassExpression parseM3Expression(JsonOwlObject expression, BatchHandlerValues values)
			throws MissingParameterException, UnknownIdentifierException, OWLException {
		M3ExpressionParser p = new M3ExpressionParser(checkLiteralIdentifiers());
		if (enforceExternalValidate()) {
			return p.parse(values.modelId, expression, m3, externalLookupService);
		}
		else {
			return p.parse(values.modelId, expression, m3, null);
		}
	}
	
	/**
	 * Handle the request for an operation regarding an edge.
	 * 
	 * @param request
	 * @param operation
	 * @param userId
	 * @param token
	 * @param values
	 * @return error or null
	 * @throws Exception
	 */
	String handleRequestForEdge(M3Request request, Operation operation, String userId, UndoMetadata token, BatchHandlerValues values) throws Exception {
		values.nonMeta = true;
		requireNotNull(request.arguments, "request.arguments");
		values.modelId = checkModelId(values.modelId, request);
		// required: subject, predicate, object
		requireNotNull(request.arguments.subject, "request.arguments.subject");
		requireNotNull(request.arguments.predicate, "request.arguments.predicate");
		requireNotNull(request.arguments.object, "request.arguments.object");
		// check for variables
		final String subject = values.getVariableValueId(request.arguments.subject);
		final String object = values.getVariableValueId(request.arguments.object);
		
		// add edge
		if (Operation.add == operation){
			// optional: values
			List<OWLNamedIndividual> individuals = m3.addFactNonReasoning(values.modelId,
					request.arguments.predicate, subject, object,
					extract(request.arguments.values, userId, true, values, values.modelId), token);
			values.relevantIndividuals.addAll(individuals);
			addContributor(values.modelId, userId, token, m3);
		}
		// remove edge
		else if (Operation.remove == operation){
			Pair<List<OWLNamedIndividual>, Set<IRI>> pair = m3.removeFactNonReasoning(values.modelId,
					request.arguments.predicate, subject, object, token);
			values.relevantIndividuals.addAll(pair.getLeft());
			handleRemovedAnnotationIRIs(pair.getRight(), values.modelId, token);
			addContributor(values.modelId, userId, token, m3);
		}
		// add annotation
		else if (Operation.addAnnotation == operation){
			requireNotNull(request.arguments.values, "request.arguments.values");

			List<OWLNamedIndividual> individuals = m3.addAnnotations(values.modelId,
					request.arguments.predicate, subject, object,
					extract(request.arguments.values, userId, false, values, values.modelId), token);
			values.relevantIndividuals.addAll(individuals);
		}
		// remove annotation
		else if (Operation.removeAnnotation == operation){
			requireNotNull(request.arguments.values, "request.arguments.values");

			Set<OWLAnnotation> annotations = extract(request.arguments.values, null, false, values, values.modelId);
			Set<IRI> iriSet = MolecularModelManager.extractIRIValues(annotations);
			List<OWLNamedIndividual> individuals = m3.removeAnnotations(values.modelId,
					request.arguments.predicate, subject, object, annotations, token);
			values.relevantIndividuals.addAll(individuals);
			handleRemovedAnnotationIRIs(iriSet, values.modelId, token);
		}
		else {
			return "Unknown operation: "+operation;
		}
		return null;
	}
	
	/**
	 * Handle the request for an operation regarding a model.
	 * 
	 * @param request
	 * @param response
	 * @param operation
	 * @param userId
	 * @param token
	 * @param values
	 * @return error or null
	 * @throws Exception
	 */
	String handleRequestForModel(M3Request request, M3BatchResponse response, Operation operation, String userId, UndoMetadata token, BatchHandlerValues values) throws Exception {
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
		// add an empty model
		else if (Operation.add == operation) {
			values.nonMeta = true;
			values.renderBulk = true;
			
			Set<OWLAnnotation> annotations = null;
			if (request.arguments != null && request.arguments.taxonId != null) {
				values.modelId = m3.generateBlankModelWithTaxon(request.arguments.taxonId, token);
				annotations = extract(request.arguments.values, userId, true, values, values.modelId);
			}
			else {
				values.modelId = m3.generateBlankModel(null, token);
				annotations = extract(null, userId, true, values, values.modelId);
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
			Set<OWLAnnotation> annotations = extract(request.arguments.values, userId, false, values, values.modelId);
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
			Set<OWLAnnotation> annotations = extract(request.arguments.values, null, false, values, values.modelId);
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
			
			Set<OWLAnnotation> annotations = extract(request.arguments.values, userId, false, values, values.modelId);
			if (annotations != null) {
				m3.addAnnotations(values.modelId, annotations, token);
			}
			addContributor(values.modelId, userId, token, m3);
			values.renderBulk = true;
		}
		else if (Operation.storeModel == operation) {
			requireNotNull(request.arguments, "request.arguments");
			values.modelId = checkModelId(values.modelId, request);
			Set<OWLAnnotation> annotations = extract(request.arguments.values, userId, false, values, values.modelId);
			if (validateBeforeSave()) {
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
		else if (Operation.undo == operation) {
			values.nonMeta = true;
			requireNotNull(request.arguments, "request.arguments");
			values.modelId = checkModelId(values.modelId, request);
			m3.undo(values.modelId, userId);
			values.renderBulk = true;
		}
		else if (Operation.redo == operation) {
			values.nonMeta = true;
			requireNotNull(request.arguments, "request.arguments");
			values.modelId = checkModelId(values.modelId, request);
			m3.redo(values.modelId, userId);
			values.renderBulk = true;
		}
		else if (Operation.getUndoRedo == operation) {
			if (values.nonMeta) {
				// can only be used with other "meta" operations in batch mode, otherwise it would lead to conflicts in the returned signal
				return operation+" cannot be combined with other operations.";
			}
			requireNotNull(request.arguments, "request.arguments");
			values.modelId = checkModelId(values.modelId, request);
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
		response.data.undo = undos;
		response.data.redo = redos;
	}
	
	private void initMetaResponse(M3BatchResponse response) {
		if (response.data == null) {
			response.data = new ResponseData();
			response.messageType = M3BatchResponse.MESSAGE_TYPE_SUCCESS;
			response.message = "success: 0";
			response.signal = M3BatchResponse.SIGNAL_META;
		}
	}
	
	/**
	 * Handle the request for the meta properties.
	 * 
	 * @param response
	 * @param userId
	 * @throws IOException 
	 * @throws OWLException 
	 */
	void getMeta(M3BatchResponse response, String userId) throws IOException, OWLException {
		// init
		initMetaResponse(response);
		if (response.data.meta == null) {
			response.data.meta = new MetaResponse();
		}
		
		// relations
		final List<JsonRelationInfo> relList = MolecularModelJsonRenderer.renderRelations(m3, importantRelations);
		if (relList != null) {
			response.data.meta.relations = relList.toArray(new JsonRelationInfo[relList.size()]);
		}
		
		// evidence
		final List<JsonEvidenceInfo> evidencesList = MolecularModelJsonRenderer.renderEvidences(m3);
		if (evidencesList != null) {
			response.data.meta.evidence = evidencesList.toArray(new JsonEvidenceInfo[evidencesList.size()]);	
		}
		
		// model ids
		final Set<String> allModelIds = m3.getAvailableModelIds();
		response.data.meta.modelIds = allModelIds;
		
		Map<String,Map<String,String>> retMap = new HashMap<String, Map<String,String>>();
		
		// get model annotations
		for( String mid : allModelIds ){

			retMap.put(mid, new HashMap<String,String>());
			Map<String, String> modelMap = retMap.get(mid);
			
			// Iterate through the model's a.
			OWLOntology o = m3.getModelAbox(mid);
			Set<OWLAnnotation> annotations = o.getAnnotations();
			for( OWLAnnotation an : annotations ){
				// see if the annotation is a shorthand
				AnnotationShorthand shorthand = AnnotationShorthand.getShorthand(an.getProperty().getIRI());
				if (shorthand != null) {
					// add shorthand and string value
					modelMap.put(shorthand.name(), 
							MolecularModelJsonRenderer.getAnnotationStringValue(an.getValue()));
				}
				else {
					// TODO handle non short hand annotations?
				}
			}
		}
		response.data.meta.modelsMeta = retMap;
	}
	
	
	private void export(M3BatchResponse response, String modelId, String userId) throws OWLOntologyStorageException, UnknownIdentifierException {
		String exportModel = m3.exportModel(modelId);
		initMetaResponse(response);
		response.data.exportModel = exportModel;
	}
	
	private void exportLegacy(M3BatchResponse response, String modelId, String format, String userId) throws UnknownIdentifierException, IOException {
		String exportModel = m3.exportModelLegacy(modelId, format);
		initMetaResponse(response);
		response.data.exportModel = exportModel;
	}
	
	private void save(M3BatchResponse response, String modelId, Set<OWLAnnotation> annotations, String userId, UndoMetadata token) throws OWLOntologyStorageException, OWLOntologyCreationException, IOException, UnknownIdentifierException {
		m3.saveModel(modelId, annotations, token);
		initMetaResponse(response);
	}
	
	private void addContributor(String modelId, String userId, UndoMetadata token, MolecularModelManager<UndoMetadata> m3) throws UnknownIdentifierException {
		if (useUserId() && userId != null) {
			Set<OWLAnnotation> annotations = new HashSet<OWLAnnotation>();
			final OWLDataFactory f = m3.getOWLDataFactory(modelId);
			annotations.add(create(f, AnnotationShorthand.contributor, userId));
			m3.addAnnotations(modelId, annotations, token);
		}
	}
	
	private static OWLAnnotation create(OWLDataFactory f, AnnotationShorthand s, String literal) {
		return create(f, s, f.getOWLLiteral(literal));
	}
	
	private static OWLAnnotation create(OWLDataFactory f, AnnotationShorthand s, OWLAnnotationValue v) {
		final OWLAnnotationProperty p = f.getOWLAnnotationProperty(s.getAnnotationProperty());
		return f.getOWLAnnotation(p, v);
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
	
	private Set<OWLAnnotation> extract(JsonAnnotation[] values, String userId, boolean addDate, BatchHandlerValues batchValues, String modelId) throws UnknownIdentifierException {
		Set<OWLAnnotation> result = new HashSet<OWLAnnotation>();
		OWLDataFactory f = m3.getOWLDataFactory(modelId);
		if (values != null) {
			for (JsonAnnotation m3Pair : values) {
				if (m3Pair.key != null && m3Pair.value != null) {
					AnnotationShorthand shorthand = AnnotationShorthand.getShorthand(m3Pair.key);
					if (shorthand != null) {
						if (AnnotationShorthand.evidence == shorthand) {
							OWLAnnotationValue evidenceValue;
							if (batchValues.individualVariables.containsKey(m3Pair.value)) {
								Pair<String, OWLNamedIndividual> pair = batchValues.individualVariables.get(m3Pair.value);
								if (pair == null) {
									throw new UnknownIdentifierException("Variable "+m3Pair.value+" has a null value.");
								}
								evidenceValue = pair.getRight().getIRI();
							}
							else {
								evidenceValue = IdStringManager.getIRI(m3Pair.value);
							}
							result.add(create(f, shorthand, evidenceValue));
						}
						else {
							result.add(create(f, shorthand, m3Pair.value));
						}
					}
					else {
						IRI pIRI = IRI.create(m3Pair.key);
						result.add(f.getOWLAnnotation(f.getOWLAnnotationProperty(pIRI), f.getOWLLiteral(m3Pair.value)));
					}
				}
			}
		}
		addGeneratedAnnotations(userId, addDate, result, f);
		return result;
	}
	
	private void addGeneratedAnnotations(String userId, boolean addDate, Set<OWLAnnotation> annotations, OWLDataFactory f) {
		if (useUserId() && userId != null) {
			annotations.add(create(f, AnnotationShorthand.contributor, userId));
		}
		if (addDate) {
			String dateString = generateDateString();
			annotations.add(create(f, AnnotationShorthand.date, dateString));
		}
	}

	/**
	 * separate method, intended to be overridden during test.
	 * 
	 * @return date string, never null
	 */
	protected String generateDateString() {
		String dateString = MolecularModelJsonRenderer.AnnotationTypeDateFormat.get().format(new Date());
		return dateString;
	}
	
	private Set<OWLAnnotation> createGeneratedAnnotations(String modelId, String userId) {
		Set<OWLAnnotation> annotations = new HashSet<OWLAnnotation>();
		OWLDataFactory f = m3.getOWLDataFactory(modelId);
		addGeneratedAnnotations(userId, true, annotations, f);
		return annotations;
	}
	
	static class MultipleModelIdsParameterException extends Exception {

		private static final long serialVersionUID = 4362299465121954598L;

		/**
		 * @param message
		 */
		MultipleModelIdsParameterException(String message) {
			super(message);
		}
		
	}
}
