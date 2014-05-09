package owltools.gaf.lego.server.handler;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.server.JSONP;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyDocumentAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import owltools.gaf.lego.MolecularModelJsonRenderer;
import owltools.gaf.lego.MolecularModelManager;
import owltools.gaf.lego.MolecularModelManager.OWLOperationResponse;
import owltools.graph.OWLGraphWrapper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Implementation of the {@link M3Handler}. Uses the build in function to render
 * generic object into JSON or JSONP.<br>
 * <br>
 * {@link Exception} are caught during the execution and reported in json
 * format. {@link Throwable} are not caught and will trigger a default error
 * page (and non standard status code for HTTP, i.e. 500).
 * 
 * @deprecated use {@link JsonOrJsonpBatchHandler} instead.
 */
@Deprecated
@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
public class JsonOrJsonpModelHandler implements M3Handler {

	public static final String JSONP_DEFAULT_CALLBACK = "jsonp";
	public static final String JSONP_DEFAULT_OVERWRITE = "json.wrf";
	
	private final MolecularModelManager mmm;

	public JsonOrJsonpModelHandler(MolecularModelManager models) {
		super();
		this.mmm = models;
	}

	/*
	 * Builder: {"id": <id>, "instances": [...]}
	 */
	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3Response m3GetModel(String modelId, boolean help) {
		if (help) {
			return helpMsg("fetches molecular model json");
		}
		try {
			return bulk(modelId, mmm, M3Response.INSTANTIATE);
		} catch (Exception exception) {
			return errorMsg("Could not retrieve model", exception);
		}
	}

	/*
	 * Builder: {"id": <id>, "instances": [...]}
	 */
	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3Response m3GenerateMolecularModel(String classId, String db, boolean help) {
		if (help) {
			return helpMsg("generates Minimal Model augmented with GO associations");
		}
		try {
			System.out.println("db: " + db);
			System.out.println("cls: " + classId);
			String mid = mmm.generateModel(classId, db);
			return bulk(mid, mmm, M3Response.INSTANTIATE);
		} catch (Exception exception) {
			return errorMsg("Could not generate model", exception);
		}
	}
	
	/*
	 * Builder: {"id": <id>, "instances": [...]}
	 */
	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3Response m3GenerateBlankMolecularModel(String db, boolean help) {
		if (help) {
			return helpMsg("generates Minimal Model augmented with GO associations");
		}
		try {
			System.out.println("db: " + db);
			String mid = mmm.generateBlankModel(db);
			return bulk(mid, mmm, M3Response.INSTANTIATE);
		} catch (Exception exception) {
			return errorMsg("Could not generate model", exception);
		}
	}
	
	/*
	 * Info: {"message_type": "success", ..., "data: {"db": <db>}}
	 */
	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3Response m3preloadGaf(String db, boolean help) {
		if (help) {
			return helpMsg("loads a GAF into memory (saves parsing time later on)");
		}
		try {
			mmm.loadGaf(db);
			return success(Collections.singletonMap("db", db), mmm);
		} catch (Exception exception) {
			return errorMsg("Could not preload gaf", exception);
		}
	}
	
	/*
	 * Individiuals: [...]
	 */
	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3Response m3CreateIndividual(String modelId, String classId, boolean help) {
		if (help) {
			return helpMsg("generates a new individual");
		}
		try {
			OWLOperationResponse resp = mmm.createIndividual(modelId, classId, null);
			return response(resp, mmm, null);
		} catch (Exception exception) {
			return errorMsg("Could not create individual in model", exception);
		}
	}
	
	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3Response m3DeleteIndividual(String modelId, String individualId, boolean help) {
		if (help) {
			return helpMsg("delete the given individual");
		}
		try {
			OWLOperationResponse resp = mmm.deleteIndividual(modelId, individualId);
			return bulk(modelId, mmm, M3Response.INCONSISTENT); // TODO for now return the whole thing
		} catch (Exception exception) {
			return errorMsg("Could not create individual in model", exception);
		}
	}
	
	/*
	 * Individiuals: [...]
	 */
	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3Response m3AddType(String modelId, String individualId, String classId, boolean help) {
		if (help) {
			return helpMsg("generates ClassAssertion (named class)");
		}
		try {
			OWLOperationResponse resp = mmm.addType(modelId, individualId, classId);
			return response(resp, mmm, M3Response.MERGE);
		} catch (Exception exception) {
			return errorMsg("Could not add type to model", exception);
		}
	}
	
	/*
	 * Individiuals: [...]
	 */
	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3Response m3AddTypeExpression(String modelId, String individualId, String propertyId,
					String classId, boolean help) {
		if (help) {
			return helpMsg("generates ClassAssertion (anon class expression)");
		}
		try {
			OWLOperationResponse resp = mmm.addType(modelId, individualId, propertyId, classId);
			return response(resp, mmm, M3Response.MERGE);
		} catch (Exception exception) {
			return errorMsg("Could not add type expression to model", exception);
		}
	}

	/*
	 * Individiuals: [...]
	 */
	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3Response m3AddFact(String modelId, String propertyId, String individualId,
					String fillerId, boolean help) {
		if (help) {
			return helpMsg("generates ObjectPropertyAssertion");
		}
		try {
			//System.out.println("mod: " + modelId);
			//System.out.println("fil: " + fillerId);
			//System.out.println("ind: " + individualId);
			//System.out.println("rel: " + propertyId);
			OWLOperationResponse resp = mmm.addFact(modelId, propertyId, individualId, fillerId, null);
			M3Response response = response(resp, mmm, M3Response.MERGE);
			//printJsonResponse(response);
			return response;
		} catch (Exception exception) {
			return errorMsg("Could not add fact to model", exception);
		}
	}
	
	/*
	 * Individiuals: [...]
	 */
	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3Response m3RemoveFact(String propertyId, String modelId, String individualId,
					String fillerId, boolean help) {
		if (help) {
			return helpMsg("generates ObjectPropertyAssertion");
		}
		try {
			System.out.println("mod: " + modelId);
			System.out.println("rel: " + propertyId);
			System.out.println("ind: " + individualId);
			System.out.println("fil: " + fillerId);
			OWLOperationResponse resp = mmm.removeFact(modelId, propertyId, individualId, fillerId);
			return response(resp, mmm, M3Response.MERGE);
		} catch (Exception exception) {
			return errorMsg("Could not remove fact from model", exception);
		}
	}

	/*
	 * Individiuals: [...]
	 */
	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3Response m3CreateSimpleCompositeIndividual(String modelId, String classId, String enabledById, String occursInId, boolean help) {
		if (help) {
			return helpMsg("generates a new simple composite individual");
		}
		try {
			System.out.println("mod: " + modelId); // necessatry
			System.out.println("act: " + classId); // necessatry
			System.out.println("enb: " + enabledById); // optional
			System.out.println("occ: " + occursInId); // optional

			// Create base instance, along with any simples optionals that are along for the ride.
			OWLOperationResponse resp = mmm.addCompositeIndividual(modelId, classId,
																   StringUtils.stripToNull(enabledById),
																   StringUtils.stripToNull(occursInId));
			
			return response(resp, mmm, M3Response.MERGE);
		} catch (Exception exception) {
			return errorMsg("Could not create individual in model", exception);
		}
	}
	
	/*
	 * Other.
	 */
	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3Response m3ExportModel(String modelId, boolean help) {
		if (help) {
			return helpMsg("Export the current content of the model");
		}
		try {
			String model = mmm.exportModel(modelId);
			return success(Collections.singletonMap("export", model), null);
		} catch (Exception exception) {
			return errorMsg("Could not export model", exception);
		}
	}
	
	/*
	 * @see owltools.gaf.lego.server.handler.M3Handler#m3ImportModel
	 */
	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3Response m3ImportModel(String model, boolean help) {
		if (help) {
			return helpMsg("Import the model into the server.");
		}
		try {
			String modelId = mmm.importModel(model);
			return bulk(modelId, mmm, M3Response.INSTANTIATE);
		} catch (Exception exception) {
			return errorMsg("Could not import model", exception);
		}
	}

	/*
	 * Return all meta-infomation about models in a format that the client can pick apart to help build an interface.
	 * 
	 * @see owltools.gaf.lego.server.handler.M3Handler#m3GetAllModelIds(boolean)
	 */
	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3Response m3GetAllModelIds(boolean help) {
		if (help) {
			return helpMsg("Get the current available model ids.");
		}
		try {
			// Get the different kinds of model IDs for the client.

			Set<String> allModelIds = mmm.getAvailableModelIds();
			//Set<String> scratchModelIds = mmm.getScratchModelIds();
			//Set<String> storedModelIds = mmm.getStoredModelIds();
			//Set<String> memoryModelIds = mmm.getCurrentModelIds();

			Map<String, Object> map = new HashMap<String, Object>();
			map.put("models_all", allModelIds);
			//map.put("models_memory", memoryModelIds);
			//map.put("models_stored", storedModelIds);
			//map.put("models_scratch", scratchModelIds);
			
			return information(map, mmm);
		} catch (Exception exception) {
			return errorMsg("Could not retrieve all available model ids", exception);
		}
	}
	
	/*
	 * @see owltools.gaf.lego.server.handler.M3Handler#m3SaveModel
	 */
	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3Response m3StoreModel(String modelId, boolean help) {
		if (help) {
			return helpMsg("Persist the given model on the server.");
		}
		try {
			mmm.saveModel(modelId);
			M3Response response = new M3Response(M3Response.SUCCESS);
			response.message = "The model has been saved on the server.";
			return response;
		} catch (Exception exception) {
			return errorMsg("Could not save the model on the server.", exception);
		}
	}
	
	// ----------------------------------------
	// END OF COMMANDS
	// ----------------------------------------

	/**
	 * Get all relations/object properties (e.g. ro)
	 * TODO provide this also in Batch mode
	 * 
	 * @return response
	 */
	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3Response getRelations() {
		/*
		 * data: {
		 *   relations: [{
		 * 	   id: {String}
		 *     label: {String}
		 *     ?color: {String} // TODO in the future
		 *     ?glyph: {String} // TODO in the future
		 *   }]
		 * }
		 */
		try {
			// retrieve (or load) all ontologies
			// put in a new wrapper
			OWLGraphWrapper wrapper = new OWLGraphWrapper(mmm.getOntology());
			Collection<IRI> imports = mmm.getImports();
			OWLOntologyManager manager = wrapper.getManager();
			for (IRI iri : imports) {
				OWLOntology ontology = manager.getOntology(iri);
				if (ontology == null) {
					// only try to load it, if it isn't already loaded
					try {
						ontology = manager.loadOntology(iri);
					} catch (OWLOntologyDocumentAlreadyExistsException e) {
						IRI existing = e.getOntologyDocumentIRI();
						ontology = manager.getOntology(existing);
					} catch (OWLOntologyAlreadyExistsException e) {
						OWLOntologyID id = e.getOntologyID();
						ontology = manager.getOntology(id);
					}
				}
				if (ontology != null) {
					wrapper.addSupportOntology(ontology);
				}
			}
			
			// get all properties from all loaded ontologies
			Set<OWLObjectProperty> properties = new HashSet<OWLObjectProperty>();
			Set<OWLOntology> allOntologies = wrapper.getAllOntologies();
			for(OWLOntology o : allOntologies) {
				properties.addAll(o.getObjectPropertiesInSignature());
			}
			// sort properties
			List<OWLObjectProperty> propertyList = new ArrayList<OWLObjectProperty>(properties);
			Collections.sort(propertyList);
			
			// retrieve id and label for all properties
			List<Map<String, String>> relList = new ArrayList<Map<String,String>>();
			for (OWLObjectProperty p : propertyList) {
				if (p.isBuiltIn()) {
					// skip owl:topObjectProperty
					continue;
				}
				String identifier = MolecularModelJsonRenderer.getId(p, wrapper);
				String label = wrapper.getLabel(p);
				Map<String, String> entry = new HashMap<String, String>();
				entry.put("id", identifier);
				entry.put("label", label);
				relList.add(entry);
			}
			
			// create response
			M3Response resp = new M3Response(M3Response.SUCCESS);
			Map<Object, Object> relData = new HashMap<Object, Object>();
			relData.put("relationsCount", relList.size());
			relData.put("relations", relList);
			resp.data = relData;
			return resp ;
		} catch (OWLOntologyCreationException exception) {
			return errorMsg("Could not retrieve relations on the server.", exception);
		}
		
	}
	
	// ----------------------------------------
	// END OF COMMANDS
	// ----------------------------------------

	// UTIL
	private M3Response helpMsg(String msg) {
		M3Response response = new M3Response(M3Response.SUCCESS);
		response.message = msg;
		return response;
	}
	
	private M3Response warningMsg(String msg) {
		M3Response response = new M3Response(M3Response.WARNING);
		response.message = msg;
 		return response;
	}
	
	private M3Response errorMsg(String msg, Exception e) {
		M3Response response = new M3Response(M3Response.ERROR);
		response.message = msg;
		if (e != null) {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("exception", e.getClass().getName());
			map.put("exceptionMsg", e.getMessage());
			StringWriter stacktrace = new StringWriter();
			e.printStackTrace(new PrintWriter(stacktrace));
			map.put("exceptionTrace", stacktrace.toString());
			response.commentary = map;
		}
		return response;
	}
	
	/**
	 * @param data
	 * @param mmm
	 * @return REST response, never null
	 */
	private M3Response success(Object data, MolecularModelManager mmm) {
		M3Response response = new M3Response(M3Response.SUCCESS);
		response.data = data;
		if (mmm != null) {
			// TODO add consistent m3 model to result ?
		}
		return response;
	}
	
	/**
	 * @param data
	 * @param mmm
	 * @return REST response, never null
	 */
	private M3Response information(Object data, MolecularModelManager mmm) {
		M3Response response = new M3Response(M3Response.INFORMATION);
		response.data = data;
		if (mmm != null) {
			// TODO add consistent m3 model to result ?
		}
		return response;
	}
	
	/**
	 * @param modelId
	 * @param mmm 
	 * @param respCat
	 * @return REST response, never null
	 */
	private M3Response bulk(String modelId, MolecularModelManager mmm, String respCat) {
		M3Response response = new M3Response(respCat);
		if (mmm != null) {
			// TODO add consistent m3 model to result ?
			Map<Object, Object> obj = mmm.getModelObject(modelId);
			obj.put("id", modelId);
			response.data = obj;
		}
		return response;
	}
	
	/**
	 * @param resp
	 * @param mmm
	 * @param intention
	 * @return REST response, never null
	 */
	private M3Response response(OWLOperationResponse resp, MolecularModelManager mmm, String intention) {
		M3Response response;
		if (resp.isResultsInInconsistency()) {
			response = new M3Response(M3Response.INCONSISTENT);
			response.message = "unintentional inconsistency";
		}
		else if ( ! resp.isSuccess()) {
			response = new M3Response(M3Response.ERROR);
		}
		else {
			//response = new M3Response(M3Response.SUCCESS);
			response = new M3Response(intention);
		}
		if (resp.getModelData() != null) {
			response.data = resp.getModelData();
		}
		return response;
	}
	
	/**
	 * Function for debugging: print JSON representation of a response to System.out.
	 * 
	 * @param response
	 */
	static void printJsonResponse(M3Response response) {
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.setPrettyPrinting();
		Gson gson = gsonBuilder.create();
		String json = gson.toJson(response);
		System.out.println("json: " + json);
	}
}
