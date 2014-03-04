package owltools.frame.jsonld;

import java.util.HashSet;
import java.util.Set;

import owltools.frame.Frame;

public class FrameLD extends StubLD implements Frame {
	Set<AnnotationLD> annotations;

	
	@Override
	public void addAnnotation(PropertyStubLD p, Object v) {
		AnnotationLD a = new AnnotationLD(p,v);
		if (annotations == null)
			annotations = new HashSet<AnnotationLD>();
		annotations.add(a);
	}


}
