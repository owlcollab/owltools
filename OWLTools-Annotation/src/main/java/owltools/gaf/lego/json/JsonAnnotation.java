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
}