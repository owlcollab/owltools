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
	
	
	
}
