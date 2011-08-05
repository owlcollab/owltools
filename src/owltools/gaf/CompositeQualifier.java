package owltools.gaf;

public class CompositeQualifier {

	private String id;
	private String qualifierObj;
	
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
	
}
