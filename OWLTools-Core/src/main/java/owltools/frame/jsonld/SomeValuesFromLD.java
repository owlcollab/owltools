package owltools.frame.jsonld;

import owltools.frame.SomeValuesFrom;

public class SomeValuesFromLD extends RestrictionLD implements SomeValuesFrom {
	
	public SomeValuesFromLD(StubLD p, StubLD f) {
		super();
		this.onProperty = p;
		this.someValuesFrom = f;
	}

	@Override
	public BaseLD getFiller() {
		return someValuesFrom;
	}



}
