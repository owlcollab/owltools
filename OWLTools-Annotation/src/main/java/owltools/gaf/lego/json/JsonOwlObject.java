package owltools.gaf.lego.json;

import java.util.List;

public final class JsonOwlObject extends JsonAnnotatedObject {
	
	public static enum JsonOwlObjectType {
		SomeValueFrom("svf"),
		ObjectProperty("property"),
		Class("class"),
		IntersectionOf("intersection"),
		UnionOf("union");
		
		private final String lbl;
		
		private JsonOwlObjectType(String lbl) {
			this.lbl = lbl;
		}
		
		public String getLbl() {
			return lbl;
		}
	}
	
	public JsonOwlObject.JsonOwlObjectType type;
	public String id;
	public String label;
	public JsonOwlObject[] expressions; // union, intersection
	public String onProperty;
	public JsonOwlObject filler;

	static JsonOwlObject createCls(String id, String label) {
		JsonOwlObject json = new JsonOwlObject();
		json.type = JsonOwlObjectType.Class;
		json.id = id;
		json.label = label;
		return json;
	}
	
	static JsonOwlObject createProperty(String id, String label) {
		JsonOwlObject json = new JsonOwlObject();
		json.type = JsonOwlObjectType.ObjectProperty;
		json.id = id;
		json.label = label;
		return json;
	}

	public static JsonOwlObject createIntersection(List<JsonOwlObject> expressions) {
		JsonOwlObject json = new JsonOwlObject();
		json.type = JsonOwlObjectType.IntersectionOf;
		if (expressions != null && !expressions.isEmpty()) {
			json.expressions = expressions.toArray(new JsonOwlObject[expressions.size()]);
		}
		return json;
	}
	
	public static JsonOwlObject createUnion(List<JsonOwlObject> expressions) {
		JsonOwlObject json = new JsonOwlObject();
		json.type = JsonOwlObjectType.UnionOf;
		if (expressions != null && !expressions.isEmpty()) {
			json.expressions = expressions.toArray(new JsonOwlObject[expressions.size()]);
		}
		return json;
	}

	public static JsonOwlObject createSvf(String prop, JsonOwlObject filler) {
		JsonOwlObject json = new JsonOwlObject();
		json.type = JsonOwlObjectType.SomeValueFrom;
		json.onProperty = prop;
		json.filler = filler;
		return json;
	}
}