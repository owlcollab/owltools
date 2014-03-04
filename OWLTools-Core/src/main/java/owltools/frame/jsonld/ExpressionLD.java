package owltools.frame.jsonld;

import java.util.Set;
import owltools.frame.Expression;


public abstract class ExpressionLD extends BaseLD implements Expression {

	// Stub
	String id;
	String iri;
	String label;

	// Restrictions
	StubLD onProperty;
	BaseLD someValuesFrom;
	BaseLD allValuesFrom;
	
	// N-ary
	Set<ExpressionLD> intersectionOf;
	Set<ExpressionLD> unionOf;
}
