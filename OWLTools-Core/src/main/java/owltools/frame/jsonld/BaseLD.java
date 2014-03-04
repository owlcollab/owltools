package owltools.frame.jsonld;

import owltools.frame.Base;

public abstract class BaseLD implements Base {

	// Base
	String type;
	


	
	public BaseLD() {
		super();
		type = getType();
	}
	



	public String getType() {
		return type;
	}
}
