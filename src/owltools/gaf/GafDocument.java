package owltools.gaf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import org.apache.log4j.Logger;

public class GafDocument{

	private static Logger LOG = Logger.getLogger(GafDocument.class);
	
	private String id;
	private String documentPath;
	
	private transient Hashtable<String, Bioentity> bioentities;
	private transient Hashtable<String, List<WithInfo>> withInfos;
	private transient Hashtable<String, List<ExtensionExpression>> extensionExpressions;
	private transient Hashtable<String, List<CompositeQualifier>> compositeQualifiers; 
	private transient List<GeneAnnotation> annotations;
	
	
	public GafDocument(){
		
		
		bioentities = new Hashtable<String, Bioentity>();
		withInfos = new Hashtable<String, List<WithInfo>>();
		extensionExpressions = new Hashtable<String, List<ExtensionExpression>>();
		compositeQualifiers = new Hashtable<String, List<CompositeQualifier>>();
		annotations = new ArrayList<GeneAnnotation>();
	}
	
	
	void setHibernateLoad(){
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
	
	public Set<GeneAnnotation> getGeneAnnotations(String bioentity){
		return null;
	}
	
	public Set<GeneAnnotation> getGeneAnnotationsByGoCls(String cls){
		return null;
	}
	
	public void addBioentity(Bioentity bioentity){
		bioentity.setGafDocument(this.getId());
		bioentities.put(bioentity.getId(), bioentity);
	}
	
	public void addCompositeQualifier(CompositeQualifier compositeQualifier){
		List<CompositeQualifier> list = compositeQualifiers.get(compositeQualifier.getId());
		if(list == null){
			list = new ArrayList<CompositeQualifier>();
			compositeQualifiers.put(compositeQualifier.getId(), list);
		}
		
		if(!list.contains(compositeQualifier))
			list.add(compositeQualifier);
	}
	
	public Set<String> getCompositeQualifiersIds(){
		return compositeQualifiers.keySet();
	}
	
	public List<CompositeQualifier> getCompositeQualifiers(String id){
		List<CompositeQualifier> list = compositeQualifiers.get(id);
		
		
		return list;
	}
	
	public void addWithInfo(WithInfo withInfo){
		
		List<WithInfo> list = withInfos.get(withInfo.getId());
		if(list == null){
			list = new ArrayList<WithInfo>();
			withInfos.put(withInfo.getId(), list);
		}
		
		if(!list.contains(withInfo))
			list.add(withInfo);
		
	}
	
	public Set<String> getWithInfosIds(){
		return withInfos.keySet();
	}
	
	public List<WithInfo> getWithInfos(String id){
		List<WithInfo> list = withInfos.get(id);
		
		
		return list;
	}
	
	
	public List<ExtensionExpression> getExpressions(String id){
		List<ExtensionExpression> list = extensionExpressions.get(id);
		
		
		return list;
	}
	
	public Set<String> getExtensionExpressionIds(){
		return extensionExpressions.keySet();
	}
	
	public void addExtensionExpression(ExtensionExpression extensionExpression){
		List<ExtensionExpression> list = extensionExpressions.get(extensionExpression.getId());
		if(list == null){
			list = new ArrayList<ExtensionExpression>();
			extensionExpressions.put(extensionExpression.getId(), list);
		}
		
		if(!list.contains(extensionExpression))
			list.add(extensionExpression);
	}
	
	public void addGeneAnnotation(GeneAnnotation ga){
		ga.setGafDocumetObject(this);
		ga.setGafDocument(this.getId());
		annotations.add(ga);
	}
	
}
