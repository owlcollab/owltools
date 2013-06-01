package owltools.graph;

import org.semanticweb.owlapi.model.OWLObjectInverseOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;

public class OWLQuantifiedProperty {
	
	// TODO - change name to predicate?
	public enum Quantifier {
		SOME, ONLY, CARDINALITY, SUBCLASS_OF, INSTANCE_OF, PROPERTY_ASSERTION, IDENTITY, VALUE, EQUIVALENT
	}
	
	private OWLObjectProperty property;
	private Quantifier quantifier;
	private boolean isInverseOf = false;
	private boolean isNegated = false;
	private boolean isInferred = false;
	private Integer minCardinality;
	private Integer maxCardinality;

	public OWLQuantifiedProperty(OWLObjectPropertyExpression p, Quantifier q) {
		if (p != null) {
			if (p instanceof OWLObjectInverseOf) {
				isInverseOf = true;
				p = ((OWLObjectInverseOf)p).getInverse();
			}
			property = p.asOWLObjectProperty();
		}
		this.quantifier = q;
	}
	
	public OWLQuantifiedProperty() {
		// TODO Auto-generated constructor stub
	}

	public OWLQuantifiedProperty(Quantifier q) {
		this.quantifier = q;
	}

	public boolean isInverseOf() {
		return isInverseOf;
	}

	public void setInverseOf(boolean isInverseOf) {
		this.isInverseOf = isInverseOf;
	}

	public boolean isInferred() {
		return isInferred;
	}

	public void setInferred(boolean isInferred) {
		this.isInferred = isInferred;
	}

	public OWLObjectProperty getProperty() {
		return property;
	}
	public void setProperty(OWLObjectProperty property) {
		this.property = property;
	}
	public boolean hasProperty() {
		return property != null;
	}
	
	public Quantifier getQuantifier() {
		return quantifier;
	}
	public boolean isQuantified() {
		return property != null;
	}
	public void setQuantifier(Quantifier quantifier) {
		this.quantifier = quantifier;
	}
	public Integer getMinCardinality() {
		return minCardinality;
	}
	public void setMinCardinality(Integer minCardinality) {
		this.minCardinality = minCardinality;
	}
	public Integer getMaxCardinality() {
		return maxCardinality;
	}
	public void setMaxCardinality(Integer maxCardinality) {
		this.maxCardinality = maxCardinality;
	}
	
	@Override
	public String toString() {
		return getPropertyId()+" "+quantifier;
	}

	
	public String getPropertyId() {
		if (!hasProperty())
			return "-";
		return property.getIRI().toString();
	}

	// TODO - overload quantifier?
	public boolean isSubClassOf() {
		return quantifier != null && quantifier == Quantifier.SUBCLASS_OF;
	}
	public boolean isInstanceOf() {
		return quantifier != null && quantifier == Quantifier.INSTANCE_OF;
	}
	public boolean isIdentity() {
		return quantifier != null && quantifier == Quantifier.IDENTITY;
	}


	public boolean isSomeValuesFrom() {
		return quantifier != null && quantifier == Quantifier.SOME;
	}

	public boolean isPropertyAssertion() {
		return quantifier != null && quantifier == Quantifier.PROPERTY_ASSERTION;
	}

	public boolean isAllValuesFrom() {
		return quantifier != null && quantifier == Quantifier.ONLY;
	}
	
	public boolean isCardinality() {
		return quantifier != null && quantifier == Quantifier.CARDINALITY;
	}

	public boolean isHasValue() {
		return quantifier != null && quantifier == Quantifier.VALUE;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
//		result = prime * result + (isInferred ? 1231 : 1237);
//		result = prime * result + (isInverseOf ? 1231 : 1237);
//		result = prime * result + (isNegated ? 1231 : 1237);
		result = prime * result
				+ ((maxCardinality == null) ? 0 : maxCardinality.hashCode());
		result = prime * result
				+ ((minCardinality == null) ? 0 : minCardinality.hashCode());
		result = prime * result
				+ ((property == null) ? 0 : property.hashCode());
		result = prime * result
				+ ((quantifier == null) ? 0 : quantifier.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OWLQuantifiedProperty other = (OWLQuantifiedProperty) obj;
//		if (isInferred != other.isInferred)
//			return false;
//		if (isInverseOf != other.isInverseOf)
//			return false;
//		if (isNegated != other.isNegated)
//			return false;
		if (maxCardinality == null) {
			if (other.maxCardinality != null)
				return false;
		} else if (!maxCardinality.equals(other.maxCardinality))
			return false;
		if (minCardinality == null) {
			if (other.minCardinality != null)
				return false;
		} else if (!minCardinality.equals(other.minCardinality))
			return false;
		if (property == null) {
			if (other.property != null)
				return false;
		} else if (!property.equals(other.property))
			return false;
		if (quantifier != other.quantifier)
			return false;
		return true;
	}

	/**
	 * true if this is equal to other. Nulls are considered to match
	 * @param other
	 * @return boolean
	 */
	public boolean subsumes(OWLQuantifiedProperty other) {
		if ((quantifier == null || quantifier == other.getQuantifier())
				&&
				(getProperty() == null ||
						getProperty().equals(other.getProperty())))
			return true;
		return false;
	}
	
}
