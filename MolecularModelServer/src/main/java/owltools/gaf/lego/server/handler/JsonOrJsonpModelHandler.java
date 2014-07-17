package owltools.gaf.lego.server.handler;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
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

	private static final Logger logger = Logger.getLogger(JsonOrJsonpModelHandler.class);
	
	public static final String JSONP_DEFAULT_CALLBACK = "jsonp";
	public static final String JSONP_DEFAULT_OVERWRITE = "json.wrf";
	
	private final MolecularModelManager<?> mmm;

	public JsonOrJsonpModelHandler(MolecularModelManager<?> models) {
		super();
		this.mmm = models;
	}

	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3Response m3GetModel(String modelId, boolean help) {
		return errorMsg("Using deactivated m3 method: m3GetModel", null);
	}


	/*
	 * Return all meta-infomation about models in a format that the client can pick apart to help build an interface.
	 * 
	 * @see owltools.gaf.lego.server.handler.M3Handler#m3GetAllModelIds(boolean)
	 */
	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3Response m3GetAllModelIds(boolean help) {
		logger.error("Using deprecated m3 method: m3GetAllModelIds");
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
		logger.error("Using deprecated m3 method: getRelations");
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
	private M3Response information(Object data, MolecularModelManager<?> mmm) {
		M3Response response = new M3Response(M3Response.INFORMATION);
		response.data = data;
		if (mmm != null) {
			// TODO add consistent m3 model to result ?
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
