package owltools.gaf;

public class CompositeQualifier {

	protected String id;
	protected String qualifierObj;
	
	public CompositeQualifier(){
	}

	public CompositeQualifier(String id, String qualifierObj) {
		this();
		this.id = id;
		this.qualifierObj = qualifierObj;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getQualifierObj() {
		return qualifierObj;
	}

	public void setQualifierObj(String qualifierObj) {
		this.qualifierObj = qualifierObj;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((qualifierObj == null) ? 0 : qualifierObj.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (obj instanceof CompositeQualifier == false)
			return false;
		CompositeQualifier other = (CompositeQualifier) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (qualifierObj == null) {
			if (other.qualifierObj != null)
				return false;
		} else if (!qualifierObj.equals(other.qualifierObj))
			return false;
		return true;
	}
	
}
