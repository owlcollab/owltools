package owltools.gaf;

import java.util.ArrayList;
import java.util.List;

public class BioentityDocument {

	private final String id;
	private final String documentPath;

	private List<Bioentity> bioentities;
	private final List<String> comments = new ArrayList<String>();
	
	public BioentityDocument(String id, String documentPath) {
		this(id, documentPath, new ArrayList<Bioentity>());
	}
	
	public BioentityDocument(String id, String documentPath, List<Bioentity> bioentities) {
		this.id = id;
		this.documentPath = documentPath;
		this.bioentities = bioentities;
	}
	
	/**
	 * @return documentId
	 */
	public String getId() {
		return id;
	}

	/**
	 * Retrieve the source path for this document
	 * 
	 * @return path or null.
	 */
	public String getDocumentPath() {
		return documentPath;
	}
	
	/**
	 * Add a {@link Bioentity} object to the document.
	 * 
	 * @param bioentity
	 */
	public void addBioentity(Bioentity bioentity){
		bioentities.add(bioentity);
	}
	
	/**
	 * Get all {@link Bioentity} objects.
	 * 
	 * @return entities, never null
	 */
	public List<Bioentity> getBioentities(){
		return bioentities;
	}
	
	/**
	 * Get the current set of comments.
	 * 
	 * @return comments, never null
	 */
	public List<String> getComments() {
		return comments;
	}

	/**
	 * Add a comment line.
	 * 
	 * @param c
	 */
	public void addComment(String c) {
		this.comments.add(c);
		
	}
}
