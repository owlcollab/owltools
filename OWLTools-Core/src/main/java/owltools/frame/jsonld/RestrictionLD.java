package owltools.frame.jsonld;

import owltools.frame.Restriction;
public abstract class RestrictionLD extends ExpressionLD implements Restriction {


	public abstract BaseLD getFiller();
	
	@Override
	public String getType() {
		return "owl:Restriction";
	}

}
