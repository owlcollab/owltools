package owltools.gaf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

public class Bioentity{

	private String db; 					// GAF-Col  1	GPI namespace header?
	private String id; 					// GAF-Col  2	GPI-Col 1
	private String symbol; 				// GAF-Col  3	GPI-Col 2
	private String fullName; 			// GAF-Col 10	GPI-Col 3
	private List<String> synonyms; 		// GAF-Col 11	GPI-Col 4
	private String typeCls; 			// GAF-Col 12	GPI-Col 5
	private String ncbiTaxonId; 		// GAF-Col 13	GPI-Col 6
	
	private String parentObjectId = null; 					// GPI-Col 7
	private List<String> dbXrefs = null;  					// GPI-Col 8
	private List<Pair<String, String>> properties = null;	// GPI-Col 9
	
	public Bioentity(){
	}
	
	public Bioentity(String id, String symbol, String fullName, String typeCls,
			String ncbiTaxonId, String db) {
		this();
		this.id = id;
		this.symbol = symbol;
		this.fullName = fullName;
		this.typeCls = typeCls;
		this.ncbiTaxonId = ncbiTaxonId;
		this.db = db;
		this.synonyms = new ArrayList<String>(); // start with something tolerable
	}

	// added by Sven so I could dump Collection<Bioentity> objects and remain sane
	public String toString() {
		return this.getId();
	}
	
	public String getLocalId() {
		return getId().replaceFirst("\\w+:", "");
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

	public void addSynonym(String syn) {
		this.synonyms.add(syn);
	}

	/*
	 * Copy out all of our collected synonyms as an List of Strings.
	 */
	public List<String> getSynonyms() {
		if (this.synonyms == null) {
			return Collections.emptyList();
		}
		return new ArrayList<String>(this.synonyms);
	}

	public String getParentObjectId() {
		return parentObjectId;
	}

	public void setParentObjectId(String parentObjectId) {
		this.parentObjectId = parentObjectId;
	}

	public List<String> getDbXrefs() {
		return dbXrefs;
	}

	public void addDbXref(String dbXref) {
		if (dbXrefs == null) {
			dbXrefs = new ArrayList<String>();
		}
		dbXrefs.add(dbXref);
	}

	public List<Pair<String, String>> getProperties() {
		return properties;
	}

	public void addProperty(String key, String value) {
		if (properties == null) {
			properties = new ArrayList<Pair<String,String>>();
		}
		properties.add(Pair.of(key, value));
	}
}
