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
	}
	
	public static enum Operation {
		get("get"),
		create("create"),
		addType("add-type"),
		removeType("remove-type"),
		add("add"),
		remove("remove"),
		addAnnotation("add-annotation"),
		removeAnnotation("remove-annotation"),
		generate("generate"),
		generateBlank("generate-blank"),
		exportModel("export"),
		importModel("import"),
		allModelIds("all-model-ids");
		
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
	}
	
	public static class M3Argument {
		String modelId;
		String subject;
		String object;
		String predicate;
		String individual;
		String db;
		String importModel;
		
		M3Expression[] expressions;
		M3Pair[] values;
	}
	
	public static class M3Pair {
		String key;
		String value;
	}
	
	public static class M3Expression {
		String type;
		String onProp;
		String literal;
//		Expression expression; // use in the future for recursive expression
	}
	
	public static class M3BatchResponse {
		final String uid; // pass-through
		/*
		 * pass-through; model:
		 * "information", "action" //, "location"
		 */
		final String intention;
		
		/*
		 * "merge", "rebuild", "meta" //, "location"?
		 */
		String signal;
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
		 */
		public M3BatchResponse(String uid, String intention) {
			this.uid = uid;
			this.intention = intention;
		}
		
	}
	
	
	public M3BatchResponse m3Batch(String uid, String intention, M3Request[] requests);
	
	@Path("m3Batch")
	@POST
	@Consumes("application/x-www-form-urlencoded")
	public M3BatchResponse m3BatchPost(
			@FormParam("uid") String uid,
			@FormParam("intention") String intention,
			@FormParam("requests") String requests);
	
	
	@Path("m3Batch")
	@GET
	public M3BatchResponse m3BatchGet(
			@QueryParam("uid") String uid,
			@QueryParam("intention") String intention,
			@QueryParam("requests") String requests);
}
