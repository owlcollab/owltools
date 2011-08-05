package owltools.gaf;

public class WithInfo{

	private String id;
	private String withXref;
	
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

	
	
}
