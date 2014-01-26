package owltools.gaf.lego.server.handler;

import java.util.Map;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import owltools.gaf.lego.MolecularModelManager;

/**
 * Define methods and paths for the REST api of the {@link MolecularModelManager}.
 */
@Path("/")
public interface M3Handler {

	// General purpose arguments,
	static final String PARAM_HELP = "help";
	static final String PARAM_DB = "db";
	static final String PARAM_CLASSID = "classId";
	static final String PARAM_MODELID = "modelId";
	static final String PARAM_INDIVIDIALID = "individualId";
	static final String PARAM_PROPERTYID = "propertyId";
	static final String PARAM_FILLERID = "fillerId";
	static final String PARAM_FORMAT = "format";
	
	// Arguments for composite functions.
	static final String PARAM_SIMPLE_ENABLED_BY = "enabledById";
	static final String PARAM_SIMPLE_OCCURS_IN = "occursInId";
	
	public static class M3Response {
		public static final String ERROR = "error"; // non-update response (informational)
		public static final String WARNING = "warning"; // non-update response (informational)
		public static final String SUCCESS = "success"; // non-update response (informational)
		public static final String INFORMATION = "information"; // update meta information about server
		public static final String INCONSISTENT = "inconsistent"; // full likely necessary for client
		public static final String MERGE = "merge"; // attempt to update client
		
		public final String message_type; // due to introspection, not camelCase here
		public String message = null;
		//public String signal = null;
		public Map<String, Object> commentary = null;
		public Object data;
		
		public M3Response(String messageType) {
			this.message_type = messageType;
			this.message = messageType; // can't have a message type without a message
			//this.signal = signal;
		}
		public M3Response(String messageType, String message) {
			this.message_type = messageType;
			this.message = message;
		}
	}
	
	@Path("m3GenerateMolecularModel")
	@GET
	public M3Response m3GenerateMolecularModel(
			@QueryParam(PARAM_CLASSID) String classId,
			@QueryParam(PARAM_DB) String db,
			@DefaultValue("false") @QueryParam(PARAM_HELP) boolean help);

	@Path("m3preloadGaf")
	@GET
	public M3Response m3preloadGaf(
			@QueryParam(PARAM_DB) String db,
			@DefaultValue("false") @QueryParam(PARAM_HELP) boolean help);

	@Path("m3CreateIndividual")
	@GET
	public M3Response m3CreateIndividual(
			@QueryParam(PARAM_MODELID) String modelId,
			@QueryParam(PARAM_CLASSID) String classId,
			@DefaultValue("false") @QueryParam(PARAM_HELP) boolean help);

	@Path("m3AddType")
	@GET
	public M3Response m3AddType(
			@QueryParam(PARAM_MODELID) String modelId,
			@QueryParam(PARAM_INDIVIDIALID) String individualId,
			@QueryParam(PARAM_CLASSID) String classId, // typically MF or BP
			@DefaultValue("false") @QueryParam(PARAM_HELP) boolean help);

	/**
	 * Always generates a restriction.
	 * 
	 * @param modelId
	 * @param individualId
	 * @param propertyId
	 * @param classId
	 * @param help
	 * @return response
	 */
	@Path("m3AddTypeExpression")
	@GET
	public M3Response m3AddTypeExpression(
			@QueryParam(PARAM_MODELID) String modelId,
			@QueryParam(PARAM_INDIVIDIALID) String individualId,
			@QueryParam(PARAM_PROPERTYID) String propertyId, // occurs_in, enabled_by
			@QueryParam(PARAM_CLASSID) String classId,
			@DefaultValue("false") @QueryParam(PARAM_HELP) boolean help);

	@Path("m3AddFact")
	@GET
	public M3Response m3AddFact(
			@QueryParam(PARAM_MODELID) String modelId,
			@QueryParam(PARAM_PROPERTYID) String propertyId, // Relation
			@QueryParam(PARAM_INDIVIDIALID) String individualId, // Subject
			@QueryParam(PARAM_FILLERID) String fillerId, // Object
			@DefaultValue("false") @QueryParam(PARAM_HELP) boolean help);

	@Path("m3RemoveFact")
	@GET
	public M3Response m3RemoveFact(
			@QueryParam(PARAM_PROPERTYID) String propertyId,
			@QueryParam(PARAM_MODELID) String modelId,
			@QueryParam(PARAM_INDIVIDIALID) String individualId,
			@QueryParam(PARAM_FILLERID) String fillerId,
			@DefaultValue("false") @QueryParam(PARAM_HELP) boolean help);

	@Path("m3CreateSimpleCompositeIndividual")
	@GET
	public M3Response m3CreateSimpleCompositeIndividual(
			@QueryParam(PARAM_MODELID) String modelId,
			@QueryParam(PARAM_CLASSID) String classId,
			@QueryParam(PARAM_SIMPLE_ENABLED_BY) String enabledById,
			@QueryParam(PARAM_SIMPLE_OCCURS_IN) String occursInId,
			@DefaultValue("false") @QueryParam(PARAM_HELP) boolean help);
	
	@Path("m3GetModel")
	@GET
	public M3Response m3GetModel(
			@QueryParam(PARAM_MODELID) String modelId,
			@DefaultValue("false") @QueryParam(PARAM_HELP) boolean help);

	@Path("m3ExportModel")
	@GET
	public M3Response m3ExportModel(
			@QueryParam(PARAM_MODELID) String modelId,
			@QueryParam(PARAM_FORMAT) String format,
			@DefaultValue("false") @QueryParam(PARAM_HELP) boolean help);
	
	@Path("m3ImportModel")
	@GET
	public M3Response m3ImportModel(
			@QueryParam(PARAM_MODELID) String modelId,
			@QueryParam(PARAM_MODELID) String model,
			@DefaultValue("false") @QueryParam(PARAM_HELP) boolean help);

	@Path("m3GetAllModelIds")
	@GET
	public M3Response m3GetAllModelIds(@DefaultValue("false") @QueryParam(PARAM_HELP) boolean help);
	
	@Path("m3StoreModel")
	@GET
	public M3Response m3StoreModel(
			@QueryParam(PARAM_MODELID) String modelId,
			@DefaultValue("false") @QueryParam(PARAM_HELP) boolean help);
}
