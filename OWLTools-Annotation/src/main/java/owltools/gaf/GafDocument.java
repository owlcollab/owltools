package owltools.gaf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GafDocument{

	protected String id;
	protected String documentPath;

	protected transient Map<String, Bioentity> bioentities;
	protected transient Map<String, Set<WithInfo>> withInfos;
	protected transient Map<String, Set<CompositeQualifier>> compositeQualifiers; 
	protected transient List<GeneAnnotation> annotations;
	protected transient List<String> comments = new ArrayList<String>();
	
	private Map<String,Set<GeneAnnotation>> annotationMap = null;


	public GafDocument(){
		bioentities = new Hashtable<String, Bioentity>();
		withInfos = new HashMap<String, Set<WithInfo>>();
		compositeQualifiers = new HashMap<String, Set<CompositeQualifier>>();
		annotations = new ArrayList<GeneAnnotation>();
	}

	public void index() {
		annotationMap = new HashMap<String,Set<GeneAnnotation>>();
		for (GeneAnnotation a : getGeneAnnotations()) {
			String eid = a.getBioentity();
			if (!annotationMap.containsKey(eid))
				annotationMap.put(eid, new HashSet<GeneAnnotation>());
			annotationMap.get(eid).add(a);
		}
	}

	public GafDocument(String id, String documentPath) {
		this();
		this.id = id;
		this.documentPath = documentPath;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDocumentPath() {
		return documentPath;
	}

	public void setDocumentPath(String documentPath) {
		this.documentPath = documentPath;
	}

	public Bioentity getBioentity(String id){
		return bioentities.get(id);
	}

	public Collection<Bioentity> getBioentities(){

		return bioentities.values();
	}


	public List<GeneAnnotation> getGeneAnnotations(){

		return annotations;
	}

	// TODO - improve efficiency, cache?
	public Set<GeneAnnotation> getGeneAnnotations(String bioentity){
		if (annotationMap != null) {
			return annotationMap.get(bioentity);
		}
		Set<GeneAnnotation> anns = new HashSet<GeneAnnotation>();
		for (GeneAnnotation ann : this.getGeneAnnotations()) {
			if (ann.getBioentity().equals(bioentity))
				anns.add(ann);
		}
		return anns;
	}

	public List<GeneAnnotation> getGeneAnnotationsByDirectGoCls(String cls){
		List<GeneAnnotation> result = new ArrayList<GeneAnnotation>();
		for (GeneAnnotation annotation : annotations) {
			if(cls.equals(annotation.cls)) {
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
	
	public void addBioentity(Bioentity bioentity){
		bioentity.setGafDocument(this.getId());
		bioentities.put(bioentity.getId(), bioentity);
	}

	public void addCompositeQualifier(CompositeQualifier compositeQualifier){
		Set<CompositeQualifier> set = compositeQualifiers.get(compositeQualifier.getId());
		if(set == null){
			set = new HashSet<CompositeQualifier>();
			compositeQualifiers.put(compositeQualifier.getId(), set);
		}
		set.add(compositeQualifier);
	}

	public Set<String> getCompositeQualifiersIds(){
		return compositeQualifiers.keySet();
	}

	public Collection<CompositeQualifier> getCompositeQualifiers(String id){
		Set<CompositeQualifier> set = compositeQualifiers.get(id);
		return set;
	}

	public void addWithInfo(WithInfo withInfo){

		Set<WithInfo> list = withInfos.get(withInfo.getId());
		if(list == null){
			list = new HashSet<WithInfo>();
			withInfos.put(withInfo.getId(), list);
		}
		list.add(withInfo);
	}

	public Set<String> getWithInfosIds(){
		return withInfos.keySet();
	}

	public Collection<WithInfo> getWithInfos(String id){
		Set<WithInfo> set = withInfos.get(id);
		return set;
	}

	public void addGeneAnnotation(GeneAnnotation ga){
		ga.setGafDocumetObject(this);
		ga.setGafDocument(this.getId());
		annotations.add(ga);
	}

	public void setGeneAnnotations(List<GeneAnnotation> newAnns) {
		annotations = newAnns;
	}

	public List<String> getComments() {
		return comments;
	}

	public void setComments(List<String> comments) {
		this.comments = comments;
	}

	public void addComment(String c) {
		this.comments.add(c);
		
	}

}
