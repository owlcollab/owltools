package owltools.frame.jsonld;

import java.util.Set;

import owltools.frame.ClassFrame;

public class ClassFrameLD extends FrameLD implements ClassFrame {

	public Set<StubLD> directSuperclasses;
	public Set<RestrictionLD> directRestrictions;
	public Set<ExpressionLD> equivalentClasses;
	
	public ClassFrameLD() {
		super();
	}
	
	@Override
	public String getType() {
		return "owl:Class";
	}
	
	
	
	
}
