package owltools.gaf.lego.server.handler;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
import org.coode.owlapi.obo.parser.OBOOntologyFormat;
import org.glassfish.jersey.server.JSONP;
import org.semanticweb.owlapi.io.OWLFunctionalSyntaxOntologyFormat;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import owltools.gaf.lego.LegoModelGenerator;
import owltools.gaf.lego.MolecularModelManager;
import owltools.gaf.lego.MolecularModelManager.OWLOperationResponse;
import owltools.graph.OWLGraphWrapper;

/**
 * Implementation of the {@link M3Handler}. Uses the build in function to render
 * generic object into JSON or JSONP.<br>
 * <br>
 * {@link Exception} are caught during the execution and reported in json
 * format. {@link Throwable} are not caught and will trigger a default error
 * page (and non standard status code for HTTP, i.e. 500).
 */
@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
public class JsonOrJsonpModelHandler implements M3Handler {

	private static Logger LOG = Logger.getLogger(JsonOrJsonpModelHandler.class);

	static final String JSONP_DEFAULT_CALLBACK = "eval";
	static final String JSONP_DEFAULT_OVERWRITE = "jsonpCallback";
	
	private final OWLGraphWrapper graph;
	private MolecularModelManager models = null;

	public JsonOrJsonpModelHandler(OWLGraphWrapper graph, MolecularModelManager models) {
		super();
		this.graph = graph;
		this.models = models;
	}

	private synchronized MolecularModelManager getMolecularModelManager() throws OWLOntologyCreationException {
		if (models == null) {
			LOG.info("Creating m3 object");
			models = new MolecularModelManager(graph);
		}
		return models;
	}

	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3Response m3GenerateMolecularModel(String classId, String db, boolean help) {
		if (help) {
			return helpMsg("generates Minimal Model augmented with GO associations");
		}
		try {
			MolecularModelManager mmm = getMolecularModelManager();
			String mid = mmm.generateModel(classId, db);
			return success(Collections.singletonMap("id", mid), mmm);
		} catch (Exception exception) {
			return errorMsg("Could not generate model", exception);
		}
	}
	
	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3Response m3preloadGaf(String db, boolean help) {
		if (help) {
			return helpMsg("loads a GAF into memory (saves parsing time later on)");
		}
		try {
			MolecularModelManager mmm = getMolecularModelManager();
			mmm.loadGaf(db);
			return success(Collections.singletonMap("db", db), mmm);
		} catch (Exception exception) {
			return errorMsg("Could not preload gaf", exception);
		}
	}
	
	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3Response m3CreateIndividual(String modelId, String classId, boolean help) {
		if (help) {
			return helpMsg("generates a new individual");
		}
		try {
			System.out.println("mod: " + modelId);
			System.out.println("cls: " + classId);
			MolecularModelManager mmm = getMolecularModelManager();
			String id = mmm.createIndividual(modelId, classId);
			return success(Collections.singletonMap("id", id), mmm);
		} catch (Exception exception) {
			return errorMsg("Could not create individual in model", exception);
		}
	}
	
	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3Response m3AddType(String modelId, String individualId, String classId, boolean help) {
		if (help) {
			return helpMsg("generates ClassAssertion (named class)");
		}
		try {
			MolecularModelManager mmm = getMolecularModelManager();
			OWLOperationResponse resp = mmm.addType(modelId, individualId, classId);
			return response(resp, mmm);
		} catch (OWLOntologyCreationException exception) {
			return errorMsg("Could not add type to model", exception);
		}
	}
	
	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3Response m3AddTypeExpression(String modelId, String individualId, String propertyId,
					String classId, boolean help) {
		if (help) {
			return helpMsg("generates ClassAssertion (anon class expression)");
		}
		try {
			MolecularModelManager mmm = getMolecularModelManager();
			OWLOperationResponse resp = mmm.addType(modelId, individualId, propertyId, classId);
			return response(resp, mmm);
		} catch (Exception exception) {
			return errorMsg("Could not add type expression to model", exception);
		}
	}

	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3Response m3AddFact(String modelId, String propertyId, String individualId,
					String fillerId, boolean help) {
		if (help) {
			return helpMsg("generates ObjectPropertyAssertion");
		}
		try {
			MolecularModelManager mmm = getMolecularModelManager();
			OWLOperationResponse resp = mmm.addFact(modelId, propertyId, individualId, fillerId);
			return response(resp, mmm);
		} catch (Exception exception) {
			return errorMsg("Could not add fact to model", exception);
		}
	}
	
	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3Response m3RemoveFact(String propertyId, String modelId, String individualId,
					String fillerId, boolean help) {
		if (help) {
			return helpMsg("generates ObjectPropertyAssertion");
		}
		try {
			MolecularModelManager mmm = getMolecularModelManager();
			OWLOperationResponse resp = mmm.removeFact(propertyId, modelId, individualId, fillerId);
			return response(resp, mmm);
		} catch (Exception exception) {
			return errorMsg("Could not remove fact from model", exception);
		}
	}

	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3Response m3GetModel(String modelId, boolean help) {
		if (help) {
			return helpMsg("fetches molecular model json");
		}
		try {
			MolecularModelManager mmm = getMolecularModelManager();
			Map<String, Object> obj = mmm.getModelObject(modelId);
			return success(obj, mmm);
		} catch (Exception exception) {
			return errorMsg("Could not retrieve model", exception);
		}
	}

	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3Response m3ExportModel(String modelId, String format, boolean help) {
		if (help) {
			return helpMsg("Export the current content of the model");
		}
		try {
			MolecularModelManager mmm = getMolecularModelManager();
			LegoModelGenerator model = mmm.getModel(modelId);
			OWLOntology ont = model.getAboxOntology();
			OWLOntologyManager ontologyManager = ont.getOWLOntologyManager();
			
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			
			OWLOntologyFormat ontologyFormat = getOWLOntologyFormat(format);
			if (ontologyFormat != null) {
				ontologyManager.saveOntology(ont, ontologyFormat, outputStream);
			}
			else {
				ontologyManager.saveOntology(ont, outputStream);
			}
			String modelString = outputStream.toString();
			return success(Collections.singletonMap("export", modelString), null);
		} catch (Exception exception) {
			return errorMsg("Could not export model", exception);
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
	 * @return REST response, never null
	 */
	private M3Response success(Object data, MolecularModelManager mmm) {
		M3Response response = new M3Response(M3Response.SUCCESS);
		response.data = data;
		if (mmm != null) {
			// TODO add consistent m3 model to result
		}
		return response;
	}
	
	/**
	 * @param resp
	 * @param mmm
	 * @return REST response, never null
	 */
	private M3Response response(OWLOperationResponse resp, MolecularModelManager mmm) {
		M3Response response;
		if (resp.isResultsInInconsistency()) {
			response = new M3Response(M3Response.INCONSISTENT);
		}
		else if (resp.isSuccess()) {
			response = new M3Response(M3Response.SUCCESS);
		}
		else {
			response = new M3Response(M3Response.ERROR);
		}
		if (mmm != null) {
			// TODO add consistent m3 model to result
		}
		return response;
	}
	
	private OWLOntologyFormat getOWLOntologyFormat(String fmt) {
		OWLOntologyFormat ofmt = null;
		if (fmt != null) {
			fmt = fmt.toLowerCase();
			if (fmt.equals("rdfxml"))
				ofmt = new RDFXMLOntologyFormat();
			else if (fmt.equals("owl"))
				ofmt = new RDFXMLOntologyFormat();
			else if (fmt.equals("rdf"))
				ofmt = new RDFXMLOntologyFormat();
			else if (fmt.equals("owx"))
				ofmt = new OWLXMLOntologyFormat();
			else if (fmt.equals("owf"))
				ofmt = new OWLFunctionalSyntaxOntologyFormat();
			else if (fmt.equals("obo"))
				ofmt = new OBOOntologyFormat();
		}
		return ofmt;
	}


}
