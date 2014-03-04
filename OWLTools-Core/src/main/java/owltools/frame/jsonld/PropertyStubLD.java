package owltools.frame.jsonld;

import owltools.frame.PropertyStub;

public class PropertyStubLD extends StubLD implements PropertyStub {
	
	public PropertyStubLD() {
		super();
	}

	public PropertyStubLD(String id, String label) {
		super();
		this.id = id;
		this.label = label;
	}



	@Override
	public String getType() {
		return "owl:ObjectProperty"; // TODO
	}


}
