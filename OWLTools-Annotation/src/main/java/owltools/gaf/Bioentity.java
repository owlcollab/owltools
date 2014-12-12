package owltools.gaf;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Bioentity{

	private String db; 					// GAF-Col  1	GPI namespace header?
	private String id; 					// GAF-Col  1+2	GPI-Col 1
	private String symbol; 				// GAF-Col  3	GPI-Col 2
	private String fullName; 			// GAF-Col 10	GPI-Col 3
	private List<String> synonyms; 		// GAF-Col 11	GPI-Col 4
	private String typeCls; 			// GAF-Col 12	GPI-Col 5
	private String ncbiTaxonId; 		// GAF-Col 13	GPI-Col 6

	private String geneId = null; 					// GPI-Col 7
	private List<String> dbXrefs = null;  					// GPI-Col 8
	private List<Pair<String, String>> properties = null;	// GPI-Col 9
	private String species_label;

	private List<GeneAnnotation> annotations;

	private String seq_db = "";
	private String seq_id = "";

	// From PANTHER DB or some protein family tree DB
	private String persistantNodeID;

	private Bioentity parent = null;
	private List<Bioentity> children;
	private List<Bioentity> originalChildrenOrder;
	private float distanceFromParent;
	// --Commented out by Inspection (12/9/14, 9:32 AM):private float distanceFromRoot;
	private String type;
	private String paint_id;

	private boolean is_leaf = true;
	private boolean pruned;

	// --Commented out by Inspection (12/9/14, 9:32 AM):private static final String NODE_TYPE_DUPLICATION="1>0";

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
		this.is_leaf = true;
	}

	// added by Sven so I could dump Collection<Bioentity> objects and remain sane
	public String toString() {
		return this.getId();
	}

	public String getLocalId() {
		if (getId() == null)
			return "";
		else
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

	public void setSymbol(String label) {
		if (label != null && label.length() > 0) {
			String previous = this.symbol;
			this.symbol = label;
			if (previous != null && !previous.equals(label))
				addSynonym(previous);
		}
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
		if (ncbiTaxonId.startsWith("NCBITaxon"))
			this.ncbiTaxonId = ncbiTaxonId.substring(4).toLowerCase();
		else
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
		return getLocalId();
	}

	public void setDb(String db) {
		this.db = db;
	}

	public void addSynonym(String syn) {
		if (this.synonyms == null) {
			this.synonyms = new ArrayList<String>();
		}
		boolean addit = this.symbol == null || (!this.symbol.equals(syn));
		for (String s : synonyms) {
			addit &= !s.equals(syn);
		}
		if (addit)
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

	public void setSynonyms(List<String> synonym) {
		if (synonym != null) {
			this.synonyms = synonym;
		}
	}

	// In cases where the bioentity of a particular isoform/alternate transcript, this is the ID for the parent Gene
	public String getGeneId() {
		return geneId;
	}

	public void setGeneId(String parentObjectId) {
		this.geneId = parentObjectId;
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

	public String getSeqId() {
		return seq_id;
	}

	public String getSeqDb() {
		return seq_db;
	}

	public void setSeqId(String seqdb, String acc) {
		if (acc != null && acc.length() > 0 && seqdb != null && seqdb.length() > 0) {
			this.seq_db = seqdb;
			this.seq_id = acc;
		}
	}

	public String getPersistantNodeID() {
		return persistantNodeID;
	}

	public void setPersistantNodeID(String treedb, String ptn) {
		if (ptn != null) {
			if (persistantNodeID == null) {
				persistantNodeID = ptn;
				if (db == null || db.length() == 0) {
					this.setDb(treedb);
					this.setId(db + ':' + ptn);
				}
				if (getDb().equals(treedb)) {
					if (getSymbol() == null || getSymbol().length() == 0) {
						setSymbol(ptn);
					}
				} else {
					this.addSynonym(ptn);
				}
			}
		}
	}

	public void setParent(Bioentity parent) {
		this.parent = parent;
	}

	public Bioentity getParent() {
		return parent;
	}

	public boolean isLeaf() {
		return is_leaf;
	}

	public boolean isTerminus() {
		return is_leaf || (!is_leaf && pruned);
	}

	public boolean isPruned() {
		return pruned;
	}

	public void setPrune(boolean prune) {
		this.pruned = prune;
	}

	public void setDistanceFromParent(float dist) {
		distanceFromParent = dist;
	}

// --Commented out by Inspection START (12/9/14, 9:31 AM):
//	public float getDistanceFromParent() {
//		return distanceFromParent;
//	}
// --Commented out by Inspection STOP (12/9/14, 9:31 AM)

// --Commented out by Inspection START (12/9/14, 9:31 AM):
//	public void setDistanceFromRoot(float dist) {
//		distanceFromRoot = dist;
//	}
// --Commented out by Inspection STOP (12/9/14, 9:31 AM)

// --Commented out by Inspection START (12/9/14, 9:31 AM):
//	public float getDistanceFromRoot() {
//		return distanceFromRoot;
//	}
// --Commented out by Inspection STOP (12/9/14, 9:31 AM)

	// Setter/Getter methods
	private boolean initChildren(List<Bioentity> children) {
		if (children == null) {
			this.children = null;
			is_leaf = true;
			return true;
		}
		if (children.isEmpty()) {
			return false;
		}

		this.children = children;
		is_leaf = false;
		return true;
	}

	public boolean setChildren(List<Bioentity> children) {
		if (null == originalChildrenOrder) {
			if (initChildren(children)) {
				setOriginalChildrenToCurrentChildren();
				return true;
			}
		}
		boolean returnVal = initChildren(children);
		setOriginalChildrenToCurrentChildren();
		return returnVal;
	}

	public List<Bioentity> getChildren() {
		return children;
	}

// --Commented out by Inspection START (12/9/14, 9:31 AM):
//	void getTermini(List<Bioentity> leaves) {
//		if (leaves != null) {
//			if (this.isTerminus())
//				leaves.add(this);
//			else
//				for (int i = 0; i < children.size(); i++) {
//					Bioentity child = children.get(i);
//					child.getTermini(leaves);
//				}
//		}
//	}
// --Commented out by Inspection STOP (12/9/14, 9:31 AM)

	public void setOriginalChildrenToCurrentChildren() {
		if (children == null) {
			return;
		}
		if (originalChildrenOrder == null) {
			originalChildrenOrder = new ArrayList<Bioentity> ();
		}
		originalChildrenOrder.clear();
		originalChildrenOrder.addAll(children);
	}

	public void setType(String s) {
		this.type = s;
	}

// --Commented out by Inspection START (12/9/14, 9:32 AM):
//	public String getType() {
//		return type;
//	}
// --Commented out by Inspection STOP (12/9/14, 9:32 AM)

// --Commented out by Inspection START (12/9/14, 9:32 AM):
//	public boolean isDuplicationNode() {
//		if (null == type) {
//			return false;
//		}
//		int index = type.indexOf(NODE_TYPE_DUPLICATION);
//		return index >= 0;
//	}
// --Commented out by Inspection STOP (12/9/14, 9:32 AM)

	public String getPaintId() {
		return paint_id;
	}

	public void setPaintId(String an_number) {
		if (an_number != null && an_number.length() > 0) {
			this.paint_id = an_number;
		}
	}

// --Commented out by Inspection START (12/9/14, 9:32 AM):
//	public String getSpeciesLabel() {
//		return (species_label == null ? "" : species_label);
//	}
// --Commented out by Inspection STOP (12/9/14, 9:32 AM)

	public void addSpeciesLabel(String species) {
		species.trim();
		if (species.length() > 0) {
			species_label = species;
		}
	}

	public List<GeneAnnotation> getAnnotations() {
		return annotations;
	}

	public void setAnnotations(List<GeneAnnotation> annotations) {
		this.annotations = annotations;
	}

	public void addAnnotation(GeneAnnotation assoc) {
		if (annotations == null) {
			annotations = new ArrayList<GeneAnnotation>();
		}
		annotations.add(assoc);
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
