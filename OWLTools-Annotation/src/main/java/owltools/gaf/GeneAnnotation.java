package owltools.gaf;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import java.util.Collections;

/**
 * 
 * @author Shahid Manzoor
 *
 */
public class GeneAnnotation {

	/**
	 * Provide a thread-safe formatter for a GAF date.
	 */
	protected static final ThreadLocal<DateFormat> GAF_Date_Format = new ThreadLocal<DateFormat>() {

		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("yyyyMMdd");
		}
		
	};
	
	protected String bioentity; // used for c1 and c2
	protected Bioentity bioentityObject; // encompass columns 1-3, 10-12
	protected boolean isContributesTo;
	protected boolean isIntegralTo;
	protected String compositeQualifier; // Col. 4
	protected String relation; // implicit relation
	protected String cls; // Col. 5
	protected String referenceId;	// Col. 6
	protected String evidenceCls; // Col. 7
	protected String withExpression; // Col. 8
	protected String aspect; // Col. 9
	protected String actsOnTaxonId; // Col. 13
	protected String lastUpdateDate; // Col. 14 //TODO: convert it to date
	protected String assignedBy; // Col. 15
	protected String extensionExpression; // Col. 16
	protected String geneProductForm; // Col. 17
	
	protected String gafDocument; // parent document id
	
	protected Collection<WithInfo> withInfoList; // derived from c8
	protected Collection<ExtensionExpression> extensionExpressionList; // derived from c16
	protected Collection<CompositeQualifier> compositeQualifierList; // derived from c4

	protected transient GafDocument gafDocumentObject; // parent document
	protected transient AnnotationSource annotationSource;
	
	/**
	 * If value of this variable is true then toString is re-calculated
	 */
	protected boolean isChanged;
	
	protected String toString;
	
	/**
	 * this method generates/updates the tab separated row of a gene annotation.
	 */
	protected void buildRow(){
		if(!isChanged)
			return;
		
		StringBuilder s = new StringBuilder();

		String taxon = "";
		CharSequence dbObjectSynonym = "";
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
			List<String> synonyms = this.bioentityObject.getSynonyms();
			if (synonyms != null && !synonyms.isEmpty()) {
				StringBuilder synonymBuilder = new StringBuilder();
				for (int i = 0; i < synonyms.size(); i++) {
					if (i > 0) {
						synonymBuilder.append('|');
					}
					synonymBuilder.append(synonyms.get(i));
				}
				dbObjectSynonym = synonymBuilder;
			}
			
			symbol = this.bioentityObject.getSymbol();
		}
		
		if(this.bioentity != null){
			int i = bioentity.indexOf(":");
			if(i>-1){
				s.append(bioentity.substring(0, i)).append("\t").append(bioentity.substring(i+1)).append("\t");
			}else{
				s.append(bioentity).append("\t");
			}
		}else{
			s.append("\t\t");
		}
			
		
		s.append(symbol).append("\t");
		
		s.append(compositeQualifier).append("\t");
		
		s.append(this.cls).append("\t");
		
		s.append(this.referenceId).append("\t");
		
		s.append(this.evidenceCls).append("\t");
		
		s.append(this.withExpression).append("\t");
		
		s.append(this.aspect).append("\t");
		
		s.append(dbObjectName).append("\t");
		
		s.append(dbObjectSynonym).append("\t");
		
		s.append(dbObjectType).append("\t");
		
		if(this.actsOnTaxonId != null && this.actsOnTaxonId.length()>0){
			int i = actsOnTaxonId.indexOf(":");
			if(i<0)
				i = 0;
			else 
				i++;
			
			taxon += "|taxon:" + actsOnTaxonId.substring(i);
		}
		
		s.append(taxon).append("\t");
		
		s.append(this.lastUpdateDate).append("\t");
		
		s.append(this.assignedBy).append("\t");
		
		s.append(this.extensionExpression).append("\t");
		
		s.append(this.geneProductForm);
		
		this.isChanged = false;
		
		this.toString = s.toString();

	}
	
	public String toString(){
		buildRow();
		
		return toString;
	}
	
	
	public GeneAnnotation(){
		this("", false, false, "", "", "", "", "", "", "", "", "", "", "", "");
	}
	
	void setGafDocumetObject(GafDocument gafDocumentObject){
		this.gafDocumentObject = gafDocumentObject;
	}
	
	public GeneAnnotation(String bioentity, boolean isContributesTo,
			boolean isIntegralTo, String compositeQualifier, String cls,
			String referenceId, String evidenceCls, String withExpression,
			String aspect, String actsOnTaxonId, String lastUpdateDate, String assignedBy,
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
		this.aspect = aspect;
		this.actsOnTaxonId = actsOnTaxonId;
		this.lastUpdateDate = lastUpdateDate;
		this.assignedBy = assignedBy;
		this.extensionExpression = extensionExpression;
		this.geneProductForm = geneProductForm;
		this.gafDocument = gafDocument;
		this.isChanged = true;
	}



	public GeneAnnotation(GeneAnnotation ann) {
		super();
		this.bioentity = ann.bioentity;
		this.bioentityObject = ann.bioentityObject;
		this.isContributesTo = ann.isContributesTo;
		this.isIntegralTo = ann.isIntegralTo;
		this.compositeQualifier = ann.compositeQualifier;
		this.cls = ann.cls;
		this.referenceId = ann.referenceId;
		this.evidenceCls = ann.evidenceCls;
		this.withExpression = ann.withExpression;
		this.aspect = ann.aspect;
		this.actsOnTaxonId = ann.actsOnTaxonId;
		this.lastUpdateDate = ann.lastUpdateDate;
		this.assignedBy = ann.assignedBy;
		this.extensionExpression = ann.extensionExpression;
		this.geneProductForm = ann.geneProductForm;
		this.gafDocument = ann.gafDocument;
		this.isChanged = true;
	}

	public String getBioentity() {
		return bioentity;
	}

	public void setBioentity(String bioentity) {
		this.bioentity = bioentity;
		
		this.isChanged = true;
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

	public void setAspect(String inAspect){
		this.aspect = inAspect;
	}

	public String getAspect(){
		return aspect;
	}

	public void setActsOnTaxonId(String actsOnTaxonId) {
		this.actsOnTaxonId = actsOnTaxonId;
		this.isChanged = true;

	}

	public String getLastUpdateDate() {
		return lastUpdateDate;
	}

	public void setLastUpdateDate(Date date) {
		if (date != null) {
			String dateString = GAF_Date_Format.get().format(date);
			setLastUpdateDate(dateString);
		}
		else {
			setLastUpdateDate("");
		}
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
	
	public void setExtensionExpressionList(Collection<ExtensionExpression> xs) {
		extensionExpressionList = xs;
		this.setExtensionExpression(StringUtils.join(xs, ","));
	}

	public Collection<ExtensionExpression> getExtensionExpressions(){
		if(extensionExpressionList == null){
			
			if(gafDocumentObject != null){
				extensionExpressionList = gafDocumentObject.getExpressions(getExtensionExpression());
				
				if(extensionExpressionList == null)
					extensionExpressionList = Collections.emptyList();
			}
		}
		
		return extensionExpressionList;
	}
	
	public Collection<WithInfo> getWithInfos(){
		if(withInfoList == null){
			
			if(gafDocumentObject != null)
				withInfoList = gafDocumentObject.getWithInfos(getWithExpression());
			
			
			if(withInfoList == null){
				withInfoList = Collections.emptyList();
			}
		}
		
		return withInfoList;
	}
	
	public Collection<CompositeQualifier> getCompositeQualifiers(){
		if(compositeQualifierList == null){
			if(gafDocumentObject != null){
				compositeQualifierList = gafDocumentObject.getCompositeQualifiers(getCompositeQualifier());
			}

			if(compositeQualifierList == null)
				compositeQualifierList = Collections.emptyList();
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
