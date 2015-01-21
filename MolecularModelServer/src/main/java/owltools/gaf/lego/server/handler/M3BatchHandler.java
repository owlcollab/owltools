package owltools.gaf.lego.server.handler;

import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

@Path("/")

public interface M3BatchHandler {

	public static class M3Request {
		String entity;
		String operation;
		M3Argument arguments;
	}
	
	public static enum Entity {
		individual,
		edge,
		model,
		relations,
		evidence;
		
		public static boolean match(Entity e, String s) {
			return e.name().equals(s);
		}
		
		public static Entity get(String s) {
			if (s != null) {
				for(Entity e : Entity.values()) {
					if (e.name().equals(s)) {
						return e;
					}
				}
			}
			return null;
		}
	}
	
	public static enum Operation {
		get("get"),
		create("create"),
		@Deprecated // hard coded assumptions about the data model, use variables instead
		createComposite("create-composite"), // aka create annaton 
		addType("add-type"),
		removeType("remove-type"),
		add("add"),
		remove("remove"),
		addAnnotation("add-annotation"),
		removeAnnotation("remove-annotation"),
		generate("generate"),
		generateBlank("generate-blank"),
		exportModel("export"),
		exportModelLegacy("export-legacy"),
		importModel("import"),
		storeModel("store"),
		allModelIds("all-model-ids"),
		allModelMeta("all-model-meta"),
		search("search"),
		updateImports("update-imports"),
		
		// undo operations for models
		undo("undo"), // undo the latest op
		redo("redo"), // redo the latest undo
		getUndoRedo("get-undo-redo"); // get a list of all currently available undo and redo for a model
		
		private final String lbl;
		
		private Operation(String lbl) {
			this.lbl = lbl;
		}
		
		public String getLbl() {
			return lbl;
		}
		
		public static boolean match(Operation op, String s) {
			return op.lbl.equals(s);
		}
		
		public static Operation get(String s) {
			if (s != null) {
				for(Operation op : Operation.values()) {
					if (op.lbl.equals(s)) {
						return op;
					}
				}
			}
			return null;
		}
	}
	
	public static class M3Argument {
		String modelId;
		String subject;
		String object;
		String predicate;
		String individual;
		String db; // TODO deprecate db, should use taxonId instead
		String taxonId;
		String importModel;
		String format;
		String assignToVariable;
		
		M3Expression[] expressions;
		M3Pair[] values;
	}
	
	public static class M3Pair {
		String key;
		String value;
	}
	
	public static class M3Expression {
		String type; // class, svf, intersection, union
		String onProp;
		String literal;
		M3Expression[] expressions;
	}
	
	public static enum M3ExpressionType {
		
		clazz("class"), 
		svf("svf"),
		intersection("intersection"),
		union("union");
		
		private final String lbl;
		
		private M3ExpressionType(String lbl) {
			this.lbl = lbl;
		}
		
		public String getLbl() {
			return lbl;
		}
	}
	
	public static class M3BatchResponse {
		final String packet_id; // generated or pass-through
		final String uid; // pass-through
		/*
		 * pass-through; model:
		 * "query", "action" //, "location"
		 */
		final String intention;
		
		public static final String SIGNAL_MERGE = "merge";
		public static final String SIGNAL_REBUILD = "rebuild";
		public static final String SIGNAL_META = "meta";
		/*
		 * "merge", "rebuild", "meta" //, "location"?
		 */
		String signal;
		
		public static final String MESSAGE_TYPE_SUCCESS = "success";
		public static final String MESSAGE_TYPE_ERROR = "error";
		/*
		 * "error", "success", //"warning"
		 */
		String message_type;
		/*
		 * "e.g.: server done borked"
		 */
		String message;
		/*
		 * Now degraded to just a String, not an Object.
		 */
		//Map<String, Object> commentary = null;
		String commentary;
		
		/*
		 * {
		 * 	 inconsistent_p: boolean
		 * 	 modelId: String
		 *   relations: [] (optional)
		 *   individuals: []
		 *   ...
		 * }
		 */
		Map<Object, Object> data;

		/**
		 * @param uid
		 * @param intention
		 * @param packet_id
		 */
		public M3BatchResponse(String uid, String intention, String packet_id) {
			this.uid = uid;
			this.intention = intention;
			this.packet_id = packet_id;
		}
		
	}
	
	
	/**
	 * Process a batch request. The parameters uid and intention are round-tripped for the JSONP.
	 * 
	 * @param uid user id, JSONP relevant
	 * @param intention JSONP relevant
	 * @param packetId response relevant, may be null
	 * @param requests batch request
	 * @param isPrivileged true, if the access is privileged
	 * @return response object, never null
	 */
	public M3BatchResponse m3Batch(String uid, String intention, String packetId, M3Request[] requests, boolean isPrivileged);
	
	/**
	 * Jersey REST method for POST with three form parameters.
	 * 
	 * @param intention JSONP relevant
	 * @param packetId
	 * @param requests JSON string of the batch request
	 * @return response convertible to JSON(P)
	 */
	@Path("m3Batch")
	@POST
	@Consumes("application/x-www-form-urlencoded")
	public M3BatchResponse m3BatchPost(
			@FormParam("intention") String intention,
			@FormParam("packet-id") String packetId,
			@FormParam("requests") String requests);
	
	/**
	 * Jersey REST method for POST with three form parameters with privileged rights.
	 * 
	 * @param uid user id, JSONP relevant
	 * @param intention JSONP relevant
	 * @param packetId
	 * @param requests JSON string of the batch request
	 * @return response convertible to JSON(P)
	 */
	@Path("m3BatchPrivileged")
	@POST
	@Consumes("application/x-www-form-urlencoded")
	public M3BatchResponse m3BatchPostPrivileged(
			@FormParam("uid") String uid,
			@FormParam("intention") String intention,
			@FormParam("packet-id") String packetId,
			@FormParam("requests") String requests);
	
	
	/**
	 * Jersey REST method for GET with three query parameters.
	 * 
	 * @param intention JSONP relevant
	 * @param packetId 
	 * @param requests JSON string of the batch request
	 * @return response convertible to JSON(P)
	 */
	@Path("m3Batch")
	@GET
	public M3BatchResponse m3BatchGet(
			@QueryParam("intention") String intention,
			@QueryParam("packet-id") String packetId,
			@QueryParam("requests") String requests);
	
	/**
	 * Jersey REST method for GET with three query parameters with privileged rights.
	 * 
	 * @param uid user id, JSONP relevant
	 * @param intention JSONP relevant
	 * @param packetId 
	 * @param requests JSON string of the batch request
	 * @return response convertible to JSON(P)
	 */
	@Path("m3BatchPrivileged")
	@GET
	public M3BatchResponse m3BatchGetPrivileged(
			@QueryParam("uid") String uid,
			@QueryParam("intention") String intention,
			@QueryParam("packet-id") String packetId,
			@QueryParam("requests") String requests);
}
