package owltools.gaf.lego.json;

import java.util.Arrays;
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
	public String property;
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
		json.property = prop;
		json.filler = filler;
		return json;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Arrays.hashCode(expressions);
		result = prime * result + ((filler == null) ? 0 : filler.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime * result
				+ ((property == null) ? 0 : property.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		JsonOwlObject other = (JsonOwlObject) obj;
		if (!Arrays.equals(expressions, other.expressions)) {
			return false;
		}
		if (filler == null) {
			if (other.filler != null) {
				return false;
			}
		} else if (!filler.equals(other.filler)) {
			return false;
		}
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		if (label == null) {
			if (other.label != null) {
				return false;
			}
		} else if (!label.equals(other.label)) {
			return false;
		}
		if (property == null) {
			if (other.property != null) {
				return false;
			}
		} else if (!property.equals(other.property)) {
			return false;
		}
		if (type != other.type) {
			return false;
		}
		return true;
	}
}