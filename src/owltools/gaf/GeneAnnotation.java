package owltools.gaf;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Shahid Manzoor
 *
 */
public class GeneAnnotation {

	private String bioentity;
	private transient Bioentity bioentityObject;
	private boolean isContributesTo;
	private boolean isIntegralTo;
	private String compositeQualifier;
	private String cls;
	private String referenceId;	
	private String evidenceCls;
	private String withExpression;
	private String actsOnTaxonId;
	private String lastUpdateDate; //TODO: convert it to date
	private String assignedBy;
	private String extensionExpression;
	private String geneProductForm;
	private String gafDocument;
	
	private List<WithInfo> withInfoList;
	private List<ExtensionExpression> extensionExpressionList;
	private List<CompositeQualifier> compositeQualifierList;

	
	private static List<WithInfo> withInfoEmptyList = new ArrayList<WithInfo>();
	private static List<ExtensionExpression> extensionExpressionEmptyList = new ArrayList<ExtensionExpression>();
	private static List<CompositeQualifier> compositeQualifierEmptyList = new ArrayList<CompositeQualifier>();
	
	private transient GafDocument gafDocumentObject;

	private transient AnnotationSource annotationSource;
	
	/**
	 * If value of this variable is true then toString is re-calculated
	 */
	private boolean isChanged;
	
	private String toString;
	
	/**
	 * this method generate a tab separated row of a gene annotation
	 * @return
	 */
	private void buildRow(){
		if(!isChanged)
			return;
		
		String s = "";

		String taxon = "";
		String dbObjectSynonym = "";
		String dbObjectName = "";
		String dbObjectType = "";
		String symbol = "";
		
		if(this.bioentityObject!= null){
			taxon = bioentityObject.getNcbiTaxonId();
			if(taxon != null){
				int i = taxon.indexOf(":");
				
				if(i<0)
					i = 0;
				else
					i++;
				
				taxon ="taxon:" + bioentityObject.getNcbiTaxonId().substring(i);
			}

			dbObjectName = this.bioentityObject.getFullName();
			dbObjectType = this.bioentityObject.getTypeCls();
			symbol = this.bioentityObject.getSymbol();
		}
		
		if(this.bioentity != null){
			int i = bioentity.indexOf(":");
			if(i>-1){
				s += bioentity.substring(0, i) + "\t" + bioentity.substring(i+1) + "\t";
			}else{
				s += bioentity + "\t";
			}
		}else{
			s += "\t\t";
		}
			
		
		s += symbol + "\t";
		
		s+= compositeQualifier + "\t";
		
		s+= this.cls + "\t";
		
		s += this.referenceId + "\t";
		
		s += this.evidenceCls + "\t";
		
		s += this.withExpression + "\t";
		
		s += "\t";
		
		s += dbObjectName+ "\t";
		
		s += dbObjectSynonym + "\t";
		
		s += dbObjectType+ "\t";
		
		if(this.actsOnTaxonId != null && this.actsOnTaxonId.length()>0){
			int i = actsOnTaxonId.indexOf(":");
			if(i<0)
				i = 0;
			else 
				i++;
			
			taxon += "|taxon:" + actsOnTaxonId.substring(i);
		}
		
		s+= taxon + "\t";
		
		s += this.lastUpdateDate + "\t";
		
		s += this.assignedBy + "\t";
		
		s += this.extensionExpression + "\t";
		
		s += this.geneProductForm;
		
		this.isChanged = false;
		
		this.toString = s;

	}
	
	public String toString(){
		buildRow();
		
		return toString;
	}
	
	
	public GeneAnnotation(){
		this("", false, false, "", "", "", "", "", "", "", "", "", "", "");
	}
	
	
	void setGafDocumetObject(GafDocument gafDocumentObject){
		this.gafDocumentObject = gafDocumentObject;
	}
	
	
	public GeneAnnotation(String bioentity, boolean isContributesTo,
			boolean isIntegralTo, String compositeQualifier, String cls,
			String referenceId, String evidenceCls, String withExpression,
			String actsOnTaxonId, String lastUpdateDate, String assignedBy,
			String extensionExpression, String geneProductForm,
			String gafDocument) {

		this.bioentity = bioentity;
		this.isContributesTo = isContributesTo;
		this.isIntegralTo = isIntegralTo;
		this.compositeQualifier = compositeQualifier;
		this.cls = cls;
		this.referenceId = referenceId;
		this.evidenceCls = evidenceCls;
		this.withExpression = withExpression;
		this.actsOnTaxonId = actsOnTaxonId;
		this.lastUpdateDate = lastUpdateDate;
		this.assignedBy = assignedBy;
		this.extensionExpression = extensionExpression;
		this.geneProductForm = geneProductForm;
		this.gafDocument = gafDocument;
		this.isChanged = true;
	}



	public String getBioentity() {
		return bioentity;
	}

	public void setBioentity(String bioentity) {
		this.bioentity = bioentity;
		
		this.isChanged = true;
	}

	public String getCls() {
		return cls;
	}

	public void setCls(String cls) {
		this.cls = cls;
		this.isChanged = true;
	
	}

	public String getReferenceId() {
		return referenceId;
	}

	public void setReferenceId(String referenceId) {
		this.referenceId = referenceId;
		this.isChanged = true;

	}

	public String getEvidenceCls() {
		return evidenceCls;
	}

	public void setEvidenceCls(String evidenceCls) {
		this.evidenceCls = evidenceCls;
		this.isChanged = true;

	}
	
	public String getWithExpression() {
		return withExpression;
	}

	public void setWithExpression(String withExpression) {
		this.withExpression = withExpression;
		this.isChanged = true;

	}

	public String getActsOnTaxonId() {
		return actsOnTaxonId;
	}

	public void setActsOnTaxonId(String actsOnTaxonId) {
		this.actsOnTaxonId = actsOnTaxonId;
		this.isChanged = true;

	}

	public String getLastUpdateDate() {
		return lastUpdateDate;
	}

	public void setLastUpdateDate(String lastUpdateDate) {
		this.lastUpdateDate = lastUpdateDate;
		this.isChanged = true;

	}

	public String getAssignedBy() {
		return assignedBy;
	}

	public void setAssignedBy(String assignedBy) {
		this.assignedBy = assignedBy;
		this.isChanged = true;

	}

	public String getExtensionExpression() {
		return extensionExpression;
	}

	public void setExtensionExpression(String extensionExpression) {
		this.extensionExpression = extensionExpression;
		this.isChanged = true;

	}

	public String getGeneProductForm() {
		return geneProductForm;
	}

	public void setGeneProductForm(String geneProductForm) {
		this.geneProductForm = geneProductForm;
		this.isChanged = true;

	}


	public String getCompositeQualifier() {
		return compositeQualifier;
	}

	public void setCompositeQualifier(String compositeQualifier) {
		this.compositeQualifier = compositeQualifier;
		this.isChanged = true;

	}

	public Bioentity getBioentityObject() {
		
		return bioentityObject;
	}

	
	public void setBioentityObject(Bioentity bioentityObject) {
		this.bioentityObject = bioentityObject;
		this.isChanged = true;
	}
	
	
	public String getGafDocument() {
		return gafDocument;
	}

	public void setGafDocument(String gafDocument) {
		this.gafDocument = gafDocument;
	}

	public boolean getIsContributesTo() {
		return isContributesTo;
	}

	public void setIsContributesTo(boolean isContributesTo) {
		this.isContributesTo = isContributesTo;
	}

	public boolean getIsIntegralTo() {
		return isIntegralTo;
	}

	public void setIsIntegralTo(boolean isIntegralTo) {
		this.isIntegralTo = isIntegralTo;
	}

	public List<ExtensionExpression> getExtensionExpressions(){
		if(extensionExpressionList == null){
			
			if(gafDocumentObject != null){
				extensionExpressionList = gafDocumentObject.getExpressions(getExtensionExpression());
				
				if(extensionExpressionList == null)
					extensionExpressionList = extensionExpressionEmptyList;
			}
		}
		
		return extensionExpressionList;
	}
	
	public List<WithInfo> getWithInfos(){
		if(withInfoList == null){
			
			if(gafDocumentObject != null)
				withInfoList = gafDocumentObject.getWithInfos(getWithExpression());
			
			
			if(withInfoList == null){
				withInfoList = withInfoEmptyList;
			}
		}
		
		return withInfoList;
	}
	
	public List<CompositeQualifier> getCompositeQualifiers(){
		if(compositeQualifierList == null){
			if(gafDocumentObject != null){
				compositeQualifierList = gafDocumentObject.getCompositeQualifiers(getCompositeQualifier());
			}

			if(compositeQualifierList == null)
				compositeQualifierList = compositeQualifierEmptyList;
		}
		return compositeQualifierList;
	}

	public AnnotationSource getSource() {
		return annotationSource;
	}

	void setSource(AnnotationSource annotationSource) {
		this.annotationSource = annotationSource;
		this.toString = annotationSource.getRow();
		isChanged = false;
	}
	

	

}
