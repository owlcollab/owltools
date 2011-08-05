package owltools.gaf;

public class Bioentity{

	private String id;
	private String symbol;
	private String fullName;
	private String typeCls;
	private String ncbiTaxonId;
	private String db;
	private String gafDocument;
	
	 public Bioentity(){
	}
	
	public Bioentity(String id, String symbol, String fullName, String typeCls,
			String ncbiTaxonId, String db, String gafDocument) {
		this();
		this.id = id;
		this.symbol = symbol;
		this.fullName = fullName;
		this.typeCls = typeCls;
		this.ncbiTaxonId = ncbiTaxonId;
		this.db = db;
		this.gafDocument = gafDocument;
	}

	// added by Sven so I could dump Collection<Bioentity> objects and remain sane
	public String toString() {
		return this.getId();
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getTypeCls() {
		return typeCls;
	}

	public void setTypeCls(String typeCls) {
		this.typeCls = typeCls;
	}

	public String getNcbiTaxonId() {
		return ncbiTaxonId;
	}

	public void setNcbiTaxonId(String ncbiTaxonId) {
		this.ncbiTaxonId = ncbiTaxonId;
	}

	public String getDb() {
		return db;
	}

	public void setDb(String db) {
		this.db = db;
	}

	public String getGafDocument() {
		return gafDocument;
	}

	public void setGafDocument(String gafDocument) {
		this.gafDocument = gafDocument;
	}

	
}
