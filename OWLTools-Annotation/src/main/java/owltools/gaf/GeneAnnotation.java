package owltools.gaf;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import java.util.Collections;

/**
 * Representation of a gene annotation.
 */
public class GeneAnnotation {

	/**
	 * Provide a thread-safe formatter for a GAF date.
	 */
	public static final ThreadLocal<DateFormat> GAF_Date_Format = new ThreadLocal<DateFormat>() {

		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("yyyyMMdd");
		}
		
	};
	private static final String DEFAULT_STRING_VALUE = "";
	
	private String bioentity = DEFAULT_STRING_VALUE; 			// used for c1 and c2
	private Bioentity bioentityObject = null;					// encompass columns 1-3, 10-12
	private boolean isContributesTo = false;
	private boolean isIntegralTo = false;
	private String compositeQualifier = DEFAULT_STRING_VALUE; 	// Col. 4
	private String relation = DEFAULT_STRING_VALUE; 			// implicit relation
	private String cls = DEFAULT_STRING_VALUE; 					// Col. 5
	private String referenceId = DEFAULT_STRING_VALUE;			// Col. 6
	private String evidenceCls = DEFAULT_STRING_VALUE; 			// Col. 7
	private String withExpression = DEFAULT_STRING_VALUE; 		// Col. 8
	private String aspect = DEFAULT_STRING_VALUE; 				// Col. 9
	private String actsOnTaxonId = DEFAULT_STRING_VALUE; 		// Col. 13
	private String lastUpdateDate = DEFAULT_STRING_VALUE; 		// Col. 14 //TODO: convert it to date
	private String assignedBy = DEFAULT_STRING_VALUE; 			// Col. 15
	private String extensionExpression = DEFAULT_STRING_VALUE; 	// Col. 16
	private String geneProductForm = DEFAULT_STRING_VALUE; 		// Col. 17
	
	// derived from c8
	private Collection<WithInfo> withInfoList = null; 
	
	// derived from c16
	private List<List<ExtensionExpression>> extensionExpressionList = null;
	
	// derived from c4
	private Collection<CompositeQualifier> compositeQualifierList = null; 

	// set by parser, optional 
	private transient AnnotationSource annotationSource = null;
	
	// If value of this variable is true then toString is re-calculated
	private volatile boolean isChanged = false;
	
	// cache String representation of this instance
	private volatile String toString = DEFAULT_STRING_VALUE;
	
	private synchronized void setChanged() {
		isChanged = true;
	}
	
	/**
	 * this method generates/updates the tab separated row of a gene annotation.
	 */
	private synchronized void buildRow(){
		if(!isChanged)
			return;
		
		StringBuilder s = new StringBuilder();

		String taxon = DEFAULT_STRING_VALUE;
		CharSequence dbObjectSynonym = DEFAULT_STRING_VALUE;
		String dbObjectName = DEFAULT_STRING_VALUE;
		String dbObjectType = DEFAULT_STRING_VALUE;
		String symbol = DEFAULT_STRING_VALUE;
		
		if(this.bioentityObject!= null){
			taxon = bioentityObject.getNcbiTaxonId();
			if(taxon != null){
				int i = taxon.indexOf(":");
				
				if(i<0)
					i = 0;
				else
					i++;
				
				taxon ="taxon:" + taxon.substring(i);
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
		// intentionally empty
	}
	
	public GeneAnnotation(String bioentity, boolean isContributesTo,
			boolean isIntegralTo, String compositeQualifier, Collection<CompositeQualifier> compositeQualifierList, String cls,
			String referenceId, String evidenceCls, String withExpression, Collection<WithInfo> withInfoList,
			String aspect, String actsOnTaxonId, String lastUpdateDate, String assignedBy,
			String extensionExpression, List<List<ExtensionExpression>> extensionExpressionList,
			String geneProductForm) {

		this.bioentity = bioentity;
		this.isContributesTo = isContributesTo;
		this.isIntegralTo = isIntegralTo;
		this.compositeQualifier = compositeQualifier;
		this.compositeQualifierList = compositeQualifierList;
		this.cls = cls;
		this.referenceId = referenceId;
		this.evidenceCls = evidenceCls;
		this.withExpression = withExpression;
		this.withInfoList = withInfoList;
		this.aspect = aspect;
		this.actsOnTaxonId = actsOnTaxonId;
		this.lastUpdateDate = lastUpdateDate;
		this.assignedBy = assignedBy;
		this.extensionExpression = extensionExpression;
		this.extensionExpressionList = extensionExpressionList;
		this.geneProductForm = geneProductForm;
		setChanged();
	}



	public GeneAnnotation(GeneAnnotation ann) {
		super();
		this.bioentity = ann.bioentity;
		this.bioentityObject = ann.bioentityObject;
		this.isContributesTo = ann.isContributesTo;
		this.isIntegralTo = ann.isIntegralTo;
		this.compositeQualifier = ann.compositeQualifier;
		this.compositeQualifierList = GafObjectsBuilder.parseCompositeQualifier(ann.compositeQualifier);
		this.cls = ann.cls;
		this.referenceId = ann.referenceId;
		this.evidenceCls = ann.evidenceCls;
		this.withExpression = ann.withExpression;
		this.withInfoList = GafObjectsBuilder.parseWithInfo(ann.withExpression);
		this.aspect = ann.aspect;
		this.actsOnTaxonId = ann.actsOnTaxonId;
		this.lastUpdateDate = ann.lastUpdateDate;
		this.assignedBy = ann.assignedBy;
		this.extensionExpression = ann.extensionExpression;
		this.extensionExpressionList = GafObjectsBuilder.parseExtensionExpression(ann.extensionExpression);
		this.geneProductForm = ann.geneProductForm;
		setChanged();
	}

	public String getBioentity() {
		return bioentity;
	}

	public void setBioentity(String bioentity) {
		this.bioentity = bioentity;
		setChanged();
	}

	
	public String getRelation() {
		return relation;
	}

	public void setRelation(String relation) {
		this.relation = relation;
		setChanged();
	}

	public String getCls() {
		return cls;
	}

	public void setCls(String cls) {
		this.cls = cls;
		setChanged();
	}

	public String getReferenceId() {
		return referenceId;
	}

	public void setReferenceId(String referenceId) {
		this.referenceId = referenceId;
		setChanged();
	}

	public String getEvidenceCls() {
		return evidenceCls;
	}

	public void setEvidenceCls(String evidenceCls) {
		this.evidenceCls = evidenceCls;
		setChanged();
	}
	
	public String getActsOnTaxonId() {
		return actsOnTaxonId;
	}

	public void setAspect(String inAspect){
		this.aspect = inAspect;
		setChanged();
	}

	public String getAspect(){
		return aspect;
	}

	public void setActsOnTaxonId(String actsOnTaxonId) {
		this.actsOnTaxonId = actsOnTaxonId;
		setChanged();
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
		setChanged();
	}

	public String getAssignedBy() {
		return assignedBy;
	}

	public void setAssignedBy(String assignedBy) {
		this.assignedBy = assignedBy;
		setChanged();
	}

	public String getExtensionExpression() {
		return extensionExpression;
	}

	public List<List<ExtensionExpression>> getExtensionExpressions(){
		return extensionExpressionList;
	}
	
	public void setExtensionExpressions(List<List<ExtensionExpression>> expressions) {
		this.extensionExpressionList = expressions;
		this.extensionExpression = GafObjectsBuilder.buildExtensionExpression(expressions);
		setChanged();
	}

	public String getGeneProductForm() {
		return geneProductForm;
	}

	public void setGeneProductForm(String geneProductForm) {
		this.geneProductForm = geneProductForm;
		setChanged();
	}

	/**
	 * Retrieve the list of qualifiers. Split the composite string, if
	 * necessary.
	 * 
	 * @return list, never null
	 */
	public List<String> getQualifiers() {
		if (compositeQualifierList != null && compositeQualifierList.isEmpty() == false) {
			List<String> stringQualifiers = new ArrayList<String>(compositeQualifierList.size());
			for (CompositeQualifier qualifier : compositeQualifierList) {
				stringQualifiers.add(qualifier.qualifierObj);
			}
			return stringQualifiers;
		}
		return Collections.emptyList();
	}

	public Bioentity getBioentityObject() {
		return bioentityObject;
	}
	
	public void setBioentityObject(Bioentity bioentityObject) {
		this.bioentityObject = bioentityObject;
		setChanged();
	}
	
	public boolean getIsContributesTo() {
		return isContributesTo;
	}

	public void setIsContributesTo(boolean isContributesTo) {
		this.isContributesTo = isContributesTo;
		setChanged();
	}

	public boolean getIsIntegralTo() {
		return isIntegralTo;
	}

	public void setIsIntegralTo(boolean isIntegralTo) {
		this.isIntegralTo = isIntegralTo;
		setChanged();
	}
	
	public String getWithExpression() {
		return withExpression;
	}

	public void setWithInfos(String withExpression, Collection<WithInfo> withInfoList) {
		this.withExpression = withExpression;
		this.withInfoList = withInfoList;
		setChanged();
	}
	
	public Collection<WithInfo> getWithInfos(){
		return withInfoList;
	}
	
	public void setCompositeQualifiers(String compositeQualifiers, Collection<CompositeQualifier> compositeQualifierList) {
		this.compositeQualifier = compositeQualifiers;
		this.compositeQualifierList = compositeQualifierList;
		setChanged();
	}
	
	public Collection<CompositeQualifier> getCompositeQualifiers(){
		return compositeQualifierList;
	}

	public String getCompositeQualifier() {
		return compositeQualifier;
	}

	public AnnotationSource getSource() {
		return annotationSource;
	}

	void setSource(AnnotationSource annotationSource) {
		this.annotationSource = annotationSource;
		setChanged();
	}

}
