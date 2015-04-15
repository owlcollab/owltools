package owltools.gaf.lego.json;

import owltools.gaf.lego.IdStringManager.AnnotationShorthand;

public class JsonAnnotation {
	public String key;
	public String value;
	
	
	public static JsonAnnotation create(AnnotationShorthand key, String value) {
		return create(key.name(), value);
	}
	
	public static JsonAnnotation create(String key, String value) {
		JsonAnnotation a = new JsonAnnotation();
		a.key = key;
		a.value = value;
		return a;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		JsonAnnotation other = (JsonAnnotation) obj;
		if (key == null) {
			if (other.key != null) {
				return false;
			}
		} else if (!key.equals(other.key)) {
			return false;
		}
		if (value == null) {
			if (other.value != null) {
				return false;
			}
		} else if (!value.equals(other.value)) {
			return false;
		}
		return true;
	}
}