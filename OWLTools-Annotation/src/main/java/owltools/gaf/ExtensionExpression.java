package owltools.gaf;


/**
 * 
 * Corresponds to col16 in GAF2.0
 * 
 * @author cjm
 *
 */
public class ExtensionExpression{

	protected String id;
	protected String relation;
	protected String cls;
	
	
	public ExtensionExpression(){
	}
	
	
	public ExtensionExpression(String id, String relation, String cls) {
		this();
		this.id = id;
		this.relation = relation;
		this.cls = cls;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
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
		result = prime * result + ((id == null) ? 0 : id.hashCode());
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
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (relation == null) {
			if (other.relation != null)
				return false;
		} else if (!relation.equals(other.relation))
			return false;
		return true;
	}
	
	public String toString() {
		return id;
	}
	
	
}
