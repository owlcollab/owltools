package owltools.sim2;

import org.semanticweb.owlapi.model.OWLClassExpression;

public class ClassExpressionPair {
	public OWLClassExpression c1;
	public OWLClassExpression c2;
	public ClassExpressionPair(OWLClassExpression c1,
			OWLClassExpression c2) {
		super();
		this.c1 = c1;
		this.c2 = c2;
	}
	public int hashCode() {
		int n1 = c1.hashCode();
		int n2 = c2.hashCode();

		return (n1 + n2) * n2 + n1;
	}
	public boolean equals(Object other) {
		if (other instanceof ClassExpressionPair) {
			ClassExpressionPair otherPair = (ClassExpressionPair) other;
			return this.c1 == otherPair.c1 && this.c2 == otherPair.c2;
		}

		return false;
	}

}