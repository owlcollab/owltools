package owltools.frame.jsonld;

import owltools.frame.ClassExpression;

public class ClassExpressionLD extends ExpressionLD implements ClassExpression {

	@Override
	public String getType() {
		return "owl:Class";
	}

}
