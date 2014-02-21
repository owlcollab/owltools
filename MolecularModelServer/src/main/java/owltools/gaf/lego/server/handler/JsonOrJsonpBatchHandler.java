package owltools.gaf.lego.server.handler;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.glassfish.jersey.server.JSONP;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.gaf.lego.LegoModelGenerator;
import owltools.gaf.lego.MolecularModelJsonRenderer;
import owltools.gaf.lego.MolecularModelManager;
import owltools.graph.OWLGraphWrapper;

public class JsonOrJsonpBatchHandler extends JsonOrJsonpModelHandler implements M3BatchHandler {

	public JsonOrJsonpBatchHandler(OWLGraphWrapper graph, MolecularModelManager models) {
		super(graph, models);
	}

	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3BatchResponse m3Batch(String uid, String intention, M3Request[] requests) {
		M3BatchResponse response = new M3BatchResponse(uid, intention);
		final Set<OWLNamedIndividual> relevantIndividuals = new HashSet<OWLNamedIndividual>();
		boolean renderBulk = false;
		String modelId = null;
		try {
			MolecularModelManager m3 = getMolecularModelManager();
			for (M3Request request : requests) {
				requireNotNull(request, "request");
				final String entity = StringUtils.trimToNull(request.entity);
				final String operation = StringUtils.trimToNull(request.operation);
				requireNotNull(request.arguments, "request.arguments");
				final String currentModelId = request.arguments.modelId;
				requireNotNull(currentModelId, "request.arguments.modelId");
				if (modelId == null) {
					modelId = currentModelId;
				}
				else {
					if (modelId.equals(currentModelId) == false) {
						error(response, null, "Using multiple modelIds in one batch call is not supported.");
					}
				}
				
				// individual
				if ("individual".equals(entity)) {
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
						return error(response, null, "Unknown operation: "+operation);
					}
				}
				// edge
				else if ("edge".equals(entity)) {
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
						return error(response, null, "Unknown operation: "+operation);
					}
				}
				//model
				else if ("model".equals(entity)) {
					// get model
					if ("get".equals(operation)){
						renderBulk = true;
					}
					else {
						return error(response, null, "Unknown operation: "+operation);
					}
				}
				else {
					return error(response, null, "Unknown entity: "+entity);
				}
			}
			if (modelId == null) {
				return error(response, null, "Empty batch calls are not supported, at least one request is required.");
			}
			// update reasoner
			// report state
			LegoModelGenerator model = m3.getModel(modelId);
			final OWLReasoner reasoner = model.getReasoner();
			reasoner.flush();
			final boolean isConsistent = reasoner.isConsistent();
			
			// create response.data
			if (renderBulk) {
				// render complete model
				response.data = m3.getModelObject(modelId);
			}
			else {
				// render individuals
				MolecularModelJsonRenderer renderer = new MolecularModelJsonRenderer(model.getAboxOntology());
				response.data = renderer.renderIndividuals(relevantIndividuals);
			}
			
			// add other infos to data
			response.data.put("id", modelId);
			if (!isConsistent) {
				response.data.put("inconsistent_p", Boolean.TRUE);
			}
			return response;
		} catch (Exception e) {
			return error(response, e, "Could not successfully complete batch request.");
		}
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

	private M3BatchResponse error(M3BatchResponse state, Exception e, String msg) {
		state.message_type = "error";
		state.message = msg;
		if (e != null) {
			state.commentary = new HashMap<String, Object>();
			state.commentary.put("exception", e.getClass().getName());
			state.commentary.put("exceptionMsg", e.getMessage());
			StringWriter stacktrace = new StringWriter();
			e.printStackTrace(new PrintWriter(stacktrace));
			state.commentary.put("exceptionTrace", stacktrace.toString());
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
}
