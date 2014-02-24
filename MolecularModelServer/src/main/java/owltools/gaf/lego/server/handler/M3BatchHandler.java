package owltools.gaf.lego.server.handler;

import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;


public interface M3BatchHandler {

	public static class M3Request {
		String entity;
		String operation;
		M3Argument arguments;
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
		final String intention; // pass-through
		
		/*
		 * "merge", "rebuild", "meta"
		 */
		String signal;
		/*
		 * "error", "success", //"warning"
		 */
		String message_type;
		String message;
		Map<String, Object> commentary = null;
		
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
	
	@Path("m3Batch")
	@POST
	@Consumes({"application/json"})
	public M3BatchResponse m3Batch(String uid, String intention, M3Request[] requests);
	
	
	@Path("m3Batch")
	@GET
	public M3BatchResponse m3BatchGet(
			@QueryParam("uid") String uid,
			@QueryParam("intention") String intention,
			@QueryParam("requests") String requests);
}
