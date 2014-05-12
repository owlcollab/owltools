package owltools.frame.jsonld;

import owltools.frame.Restriction;
public abstract class RestrictionLD extends ExpressionLD implements Restriction {

	// Restrictions
	StubLD onProperty;
	BaseLD someValuesFrom;
	BaseLD allValuesFrom;


	public abstract BaseLD getFiller();
	
	@Override
	public String getType() {
		return "owl:Restriction";
	}

}
