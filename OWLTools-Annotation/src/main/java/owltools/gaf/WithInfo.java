package owltools.gaf;

public class WithInfo{

	protected String id;
	protected String withXref;
	
	public WithInfo(){
	}

	public WithInfo(String id, String withXref) {
		this();
		this.id = id;
		this.withXref = withXref;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getWithXref() {
		return withXref;
	}

	public void setWithXref(String withXref) {
		this.withXref = withXref;
	}

	public String toString(){
		return "[" + this.id +", " + this.withXref + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((withXref == null) ? 0 : withXref.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (obj instanceof WithInfo == false)
			return false;
		WithInfo other = (WithInfo) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (withXref == null) {
			if (other.withXref != null)
				return false;
		} else if (!withXref.equals(other.withXref))
			return false;
		return true;
	}
	
}
