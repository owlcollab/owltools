package owltools.gaf;


/**
 * 
 * Corresponds to col16 in GAF2.0
 * 
 * @author cjm
 *
 */
public class ExtensionExpression{

	protected String relation;
	protected String cls;
	
	
	public ExtensionExpression(){
	}
	
	
	public ExtensionExpression(String relation, String cls) {
		this();
		this.relation = relation;
		this.cls = cls;
	}
	public String getRelation() {
		return relation;
	}
	public void setRelation(String relation) {
		this.relation = relation;
	}
	public String getCls() {
		return cls;
	}
	public void setCls(String cls) {
		this.cls = cls;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((cls == null) ? 0 : cls.hashCode());
		result = prime * result
				+ ((relation == null) ? 0 : relation.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (obj instanceof ExtensionExpression == false)
			return false;
		ExtensionExpression other = (ExtensionExpression) obj;
		if (cls == null) {
			if (other.cls != null)
				return false;
		} else if (!cls.equals(other.cls))
			return false;
		if (relation == null) {
			if (other.relation != null)
				return false;
		} else if (!relation.equals(other.relation))
			return false;
		return true;
	}
	
	public String toString() {
		return relation+" "+cls;
	}
	
	
}
