package owltools.frame.jsonld;

import owltools.frame.Annotation;

public class AnnotationLD extends BaseLD implements Annotation {
	PropertyStubLD property;
	Object value;
	
	public AnnotationLD() {
		super();
	}

	public AnnotationLD(PropertyStubLD p, Object object) {
		super();
		this.property = p;
		this.value = object;
	}


}
