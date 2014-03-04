package owltools.frame.jsonld;

import java.util.Set;
import owltools.frame.IntersectionOf;

public class IntersectionOfLD extends NaryBooleanClassExpressionLD implements IntersectionOf {

	public IntersectionOfLD(Set<ExpressionLD> ops) {
		intersectionOf = ops;
	}
	
}
