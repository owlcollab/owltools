package owltools.gaf.lego.server.handler;

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

	static final String PARAM_HELP = "help";
	static final String PARAM_DB = "db";
	static final String PARAM_CLASSID = "classId";
	static final String PARAM_MODELID = "modelId";
	static final String PARAM_INDIVIDIALID = "individualId";
	static final String PARAM_PROPERTYID = "propertyId";
	static final String PARAM_FILLERID = "fillerId";
	static final String PARAM_FORMAT = "format";
	
	@Path("m3GenerateMolecularModel")
	@GET
	public Object m3GenerateMolecularModel(
			@QueryParam(PARAM_CLASSID) String classId,
			@QueryParam(PARAM_DB) String db,
			@DefaultValue("false") @QueryParam(PARAM_HELP) boolean help);

	@Path("m3preloadGaf")
	@GET
	public Object m3preloadGaf(
			@QueryParam(PARAM_DB) String db,
			@DefaultValue("false") @QueryParam(PARAM_HELP) boolean help);

	@Path("m3CreateIndividual")
	@GET
	public Object m3CreateIndividual(
			@QueryParam(PARAM_MODELID) String modelId,
			@QueryParam(PARAM_CLASSID) String classId,
			@DefaultValue("false") @QueryParam(PARAM_HELP) boolean help);

	@Path("m3AddType")
	@GET
	public Object m3AddType(
			@QueryParam(PARAM_MODELID) String modelId,
			@QueryParam(PARAM_INDIVIDIALID) String individualId,
			@QueryParam(PARAM_CLASSID) String classId,
			@DefaultValue("false") @QueryParam(PARAM_HELP) boolean help);

	@Path("m3AddTypeExpression")
	@GET
	public Object m3AddTypeExpression(
			@QueryParam(PARAM_MODELID) String modelId,
			@QueryParam(PARAM_INDIVIDIALID) String individualId,
			@QueryParam(PARAM_PROPERTYID) String propertyId,
			@QueryParam(PARAM_CLASSID) String classId,
			@DefaultValue("false") @QueryParam(PARAM_HELP) boolean help);

	@Path("m3AddFact")
	@GET
	public Object m3AddFact(
			@QueryParam(PARAM_MODELID) String modelId,
			@QueryParam(PARAM_PROPERTYID) String propertyId,
			@QueryParam(PARAM_INDIVIDIALID) String individualId,
			@QueryParam(PARAM_FILLERID) String fillerId,
			@DefaultValue("false") @QueryParam(PARAM_HELP) boolean help);

	@Path("m3RemoveFact")
	@GET
	public Object m3RemoveFact(
			@QueryParam(PARAM_PROPERTYID) String propertyId,
			@QueryParam(PARAM_MODELID) String modelId,
			@QueryParam(PARAM_INDIVIDIALID) String individualId,
			@QueryParam(PARAM_FILLERID) String fillerId,
			@DefaultValue("false") @QueryParam(PARAM_HELP) boolean help);

	@Path("m3GetModel")
	@GET
	public Object m3GetModel(
			@QueryParam(PARAM_MODELID) String modelId,
			@DefaultValue("false") @QueryParam(PARAM_HELP) boolean help);

	@Path("m3ExportModel")
	@GET
	public Object m3ExportModel(
			@QueryParam(PARAM_MODELID) String modelId,
			@QueryParam(PARAM_FORMAT) String format,
			@DefaultValue("false") @QueryParam(PARAM_HELP) boolean help);

}
