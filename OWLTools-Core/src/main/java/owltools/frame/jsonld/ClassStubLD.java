package owltools.frame.jsonld;

import owltools.frame.ClassStub;

public class ClassStubLD extends StubLD implements ClassStub {
	
	public ClassStubLD() {
		super();
	}

	public ClassStubLD(String id, String label) {
		super();
		this.id = id;
		this.label = label;
	}

	@Override
	public String getType() {
		return "owl:Class";
	}


}
