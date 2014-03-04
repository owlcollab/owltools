package owltools.frame;

import owltools.frame.jsonld.PropertyStubLD;

public interface Frame extends Stub {
	
	public void addAnnotation(PropertyStubLD p, Object v);

}
