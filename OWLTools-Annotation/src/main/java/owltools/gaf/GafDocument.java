package owltools.gaf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Representation of a gene annotation file (GAF). It holds all relevant
 * {@link GeneAnnotation} and {@link Bioentity} objects.
 * 
 * @see GafObjectsBuilder
 */
public class GafDocument{

	private final String id;
	private final String documentPath;

	private final Map<String, Bioentity> bioentities;
	private List<GeneAnnotation> annotations;
	private final List<String> comments = new ArrayList<String>();
	
	private Map<String, List<GeneAnnotation>> annotationMap = null;

	/**
	 * Create a new document instance.
	 * 
	 * @param id
	 * @param documentPath
	 */
	public GafDocument(String id, String documentPath){
		this.id = id;
		this.documentPath = documentPath;
		bioentities = new HashMap<String, Bioentity>();
		annotations = new ArrayList<GeneAnnotation>();
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
	 * Retrieve the {@link Bioentity} object for the given id.
	 * 
	 * @param id
	 * @return entity or null
	 */
	public Bioentity getBioentity(String id){
		return bioentities.get(id);
	}

	/**
	 * Get all registered {@link Bioentity} objects.
	 * 
	 * @return entities, never null
	 */
	public Collection<Bioentity> getBioentities(){
		return bioentities.values();
	}

	/**
	 * Get all annotations from this document.
	 * 
	 * @return annotations, never
	 */
	public List<GeneAnnotation> getGeneAnnotations(){
		return annotations;
	}

	/**
	 * Get all annotations for a given {@link Bioentity} id. If {@link #index()}
	 * was called, this is a lookup operation, otherwise this is a linear scan
	 * of all annotations.
	 * 
	 * @param bioentity
	 * @return annotations, never null
	 * 
	 * @see #index()
	 */
	public Collection<GeneAnnotation> getGeneAnnotations(String bioentity){
		if (annotationMap != null) {
			return annotationMap.get(bioentity);
		}
		List<GeneAnnotation> anns = new ArrayList<GeneAnnotation>();
		for (GeneAnnotation ann : this.getGeneAnnotations()) {
			if (ann.getBioentity().equals(bioentity))
				anns.add(ann);
		}
		return anns;
	}

	/**
	 * Index all current annotations of this document. Creates an internal cache
	 * for mappings from bioentity-id to set of annotations.
	 * 
	 * @see #getGeneAnnotations(String)
	 */
	public void index() {
		annotationMap = new HashMap<String, List<GeneAnnotation>>();
		for (GeneAnnotation a : getGeneAnnotations()) {
			String eid = a.getBioentity();
			List<GeneAnnotation> entities = annotationMap.get(eid);
			if (entities == null) {
				annotationMap.put(eid, Collections.singletonList(a));
			}
			else if (entities.size() == 1) {
				List<GeneAnnotation> longEntities = new ArrayList<GeneAnnotation>();
				longEntities.add(entities.get(0));
				longEntities.add(a);
				annotationMap.put(eid, longEntities);
			}
			else {
				entities.add(a);
			}
		}
	}

	/**
	 * Search for all annotations with the given cls String.
	 * 
	 * @param cls
	 * @return annotations, never null
	 */
	public List<GeneAnnotation> getGeneAnnotationsByDirectGoCls(String cls){
		List<GeneAnnotation> result = new ArrayList<GeneAnnotation>();
		for (GeneAnnotation annotation : annotations) {
			if(cls.equals(annotation.getCls())) {
				result.add(annotation);
			}
		}
		return result;
	}
	
	/**
	 * Retrieve the (first) annotation for the given line number or null.
	 * 
	 * @param lineNumber
	 * @return annotation or null
	 */
	public GeneAnnotation getGeneAnnotationByLineNumber(int lineNumber) {
		for (GeneAnnotation annotation : annotations) {
			AnnotationSource source = annotation.getSource();
			if (source != null) {
				int current = source.getLineNumber();
				if (lineNumber == current) {
					return annotation;
				}
			}
		}
		return null;
	}
	
	/**
	 * Add a {@link Bioentity} object to the document. Will return the canonical
	 * instance for the entity in this GAF.
	 * 
	 * @param bioentity
	 * @return bioentity
	 */
	public Bioentity addBioentity(Bioentity bioentity){
		Bioentity prev = bioentities.get(bioentity.getId());
		if (prev == null) {
			bioentities.put(bioentity.getId(), bioentity);
			prev = bioentity;
		}
		return prev;
	}

	/**
	 * Add a single annotation to the document.
	 * 
	 * @param ga
	 */
	public void addGeneAnnotation(GeneAnnotation ga){
		annotations.add(ga);
	}
	
	/**
	 * Replace the current set of annotations with the given list.
	 * Also register all the bioentities from the annotations.
	 * 
	 * @param annotations
	 */
	public void setGeneAnnotations(List<GeneAnnotation> annotations) {
		this.annotations = annotations;
		this.bioentities.clear();
		for (GeneAnnotation annotation : annotations) {
			Bioentity bioentity = annotation.getBioentityObject();
			if (bioentity != null) {
				addBioentity(bioentity);
			}
		}
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
