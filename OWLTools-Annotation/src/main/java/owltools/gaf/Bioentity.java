package owltools.gaf;

import java.util.ArrayList;
import java.util.List;

public class Bioentity{

	protected String db; // Col. 1
	protected String id; // Col. 2
	protected String symbol; // Col. 3
	protected String fullName; // Col. 10(?)
	protected List<String> synonyms; // Col. 11
	protected String typeCls; // Col. 12
	protected String ncbiTaxonId; // Col. 13(?)
	protected String gafDocument;
	
	public Bioentity(){
	}
	
	/*
	 * This constructor assumes that there are no
	 */
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
		this.synonyms = new ArrayList<String>(); // start with something tolerable
	}

	// added by Sven so I could dump Collection<Bioentity> objects and remain sane
	public String toString() {
		return this.getId();
	}
	
	/*
	 * Should return "column 1" + "column 2".
	 */
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
	
	/*
	 * Should return "column 1".
	 */
	public String getDb() {
		return db;
	}

	/*
	 * Return the ID in the database (the "column 2" in the usual id: "column 1" + "column 2").
	 */
	public String getDBID(){
		int len = getDb().length();
		return getId().substring(len + 1); // +1 to take care of the ':'
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

	public void addSynonym(String syn) {
		this.synonyms.add(syn);
	}

	/*
	 * Copy out all of our collected synonyms as an List of Strings.
	 */
	public List<String> getSynonyms() {
		return new ArrayList<String>(this.synonyms);
	}
}
