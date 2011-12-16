package owltools.sim;

import org.semanticweb.owlapi.model.OWLObject;

public class OWLObjectPair {
	private final OWLObject a;
	private final OWLObject b;
	private transient final int hash;

	public OWLObjectPair(OWLObject a, OWLObject b) {
		this(a,b,false);
	}
	
	public OWLObjectPair(OWLObject a, OWLObject b, boolean isSymmetric) {
		super();
		if (isSymmetric && b.compareTo(a) < 0) {
			this.b = a;
			this.a = b;
		}
		else {
			this.a = a;
			this.b = b;			
		}
		hash = (this.a == null? 0 : this.a.hashCode() * 31)+(this.b == null? 0 : this.b.hashCode());
	}
	@Override
	public int hashCode()
	{
		return hash;
	}
	public boolean equals(Object x) {
		if (!(x instanceof OWLObjectPair))
			return false;
		return ((OWLObjectPair)x).getA().equals(a) &&
		((OWLObjectPair)x).getB().equals(b);

	}
	public OWLObject getA() {
		return a;
	}
	public OWLObject getB() {
		return b;
	} 

	public String toString() {
		return "PAIR:{"+a.toString()+","+b.toString()+"}";
	}

}
