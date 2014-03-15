package owltools.gaf;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import owltools.gaf.parser.BuilderTools;

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
	private boolean isNegated = false;
	private String qualifierString = DEFAULT_STRING_VALUE; 		// Col. 4
	private String relation = DEFAULT_STRING_VALUE; 			// implicit relation
	private String cls = DEFAULT_STRING_VALUE; 					// Col. 5
	private List<String> referenceIds = null;					// Col. 6
	private String ecoEvidenceCls;
	private String shortEvidence = DEFAULT_STRING_VALUE;		// Col. 7
//	private String evidenceCls = DEFAULT_STRING_VALUE; 			// Col. 7
	private String withExpression = DEFAULT_STRING_VALUE; 		// Col. 8
	private String aspect = DEFAULT_STRING_VALUE; 				// Col. 9
	private String actsOnTaxonId = DEFAULT_STRING_VALUE; 		// Col. 13
	private String lastUpdateDate = DEFAULT_STRING_VALUE; 		// Col. 14 //TODO: convert it to date
	private String assignedBy = DEFAULT_STRING_VALUE; 			// Col. 15
	private String extensionExpression = DEFAULT_STRING_VALUE; 	// Col. 16
	private String geneProductForm = DEFAULT_STRING_VALUE; 		// Col. 17
	private Map<String, String> properties = null;				// GPAD only
	
	// derived from c8
	private Collection<String> withInfoList = null; 
	
	// derived from c16
	private List<List<ExtensionExpression>> extensionExpressionList = null;
	
	// derived from c4
	private List<String> compositeQualifiers = null; 

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
		
		s.append(qualifierString).append("\t");
		
		s.append(this.cls).append("\t");
		
		if (referenceIds != null) {
			s.append(StringUtils.join(referenceIds, '|'));
		}
		s.append("\t");
		
		s.append(this.shortEvidence).append("\t");
		
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
			boolean isIntegralTo, String compositeQualifier, List<String> compositeQualifiers, String cls,
			List<String> referenceIds, String shortEvidence, String ecoEvidenceCls, String withExpression, Collection<String> withInfoList,
			String aspect, String actsOnTaxonId, String lastUpdateDate, String assignedBy,
			String extensionExpression, List<List<ExtensionExpression>> extensionExpressionList,
			String geneProductForm, Map<String, String> properties) {

		this.bioentity = bioentity;
		this.isContributesTo = isContributesTo;
		this.isIntegralTo = isIntegralTo;
		this.qualifierString = compositeQualifier;
		this.compositeQualifiers = compositeQualifiers;
		this.cls = cls;
		this.referenceIds = referenceIds;
		this.shortEvidence = shortEvidence;
		this.ecoEvidenceCls = ecoEvidenceCls;
		this.withExpression = withExpression;
		this.withInfoList = withInfoList;
		this.aspect = aspect;
		this.actsOnTaxonId = actsOnTaxonId;
		this.lastUpdateDate = lastUpdateDate;
		this.assignedBy = assignedBy;
		this.extensionExpression = extensionExpression;
		this.extensionExpressionList = extensionExpressionList;
		this.geneProductForm = geneProductForm;
		this.properties = properties;
		setChanged();
	}



	public GeneAnnotation(GeneAnnotation ann) {
		super();
		this.bioentity = ann.bioentity;
		this.bioentityObject = ann.bioentityObject;
		this.isContributesTo = ann.isContributesTo;
		this.isIntegralTo = ann.isIntegralTo;
		this.qualifierString = ann.qualifierString;
		this.compositeQualifiers = BuilderTools.parseCompositeQualifier(ann.qualifierString);
		this.cls = ann.cls;
		this.referenceIds = copy(ann.referenceIds);
		this.shortEvidence = ann.shortEvidence;
		this.ecoEvidenceCls = ann.ecoEvidenceCls;
		this.withExpression = ann.withExpression;
		this.withInfoList = BuilderTools.parseWithInfo(ann.withExpression);
		this.aspect = ann.aspect;
		this.actsOnTaxonId = ann.actsOnTaxonId;
		this.lastUpdateDate = ann.lastUpdateDate;
		this.assignedBy = ann.assignedBy;
		this.extensionExpression = ann.extensionExpression;
		this.extensionExpressionList = BuilderTools.parseExtensionExpression(ann.extensionExpression);
		this.geneProductForm = ann.geneProductForm;
		this.properties = copy(ann.properties);
		setChanged();
	}
	
	private static Map<String, String> copy(Map<String, String> source) {
		Map<String, String> copy = null;
		if (source != null) {
			copy = new HashMap<String, String>(source);
		}
		return copy;
	}
	
	private static <T> List<T> copy(List<T> source) {
		List<T> copy = null;
		if (source != null) {
			copy = new ArrayList<T>(source);
		}
		return copy;
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

	public List<String> getReferenceIds() {
		return referenceIds;
	}

	public void addReferenceId(String referenceId) {
		if (referenceIds == null) {
			referenceIds = new ArrayList<String>();
		}
		referenceIds.add(referenceId);
		setChanged();
	}
	
	public void addReferenceIds(Collection<String> referenceIds) {
		if (this.referenceIds == null) {
			this.referenceIds = new ArrayList<String>(referenceIds);
		}
		else {
			this.referenceIds.addAll(referenceIds);
		}
		setChanged();
	}

	public String getEcoEvidenceCls() {
		return ecoEvidenceCls;
	}

	public String getShortEvidence() {
		return shortEvidence;
	}

	public void setEvidence(String shortEvidence, String ecoEvidenceCls) {
		this.shortEvidence = shortEvidence;
		this.ecoEvidenceCls = ecoEvidenceCls;
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
		this.extensionExpression = BuilderTools.buildExtensionExpression(expressions);
		setChanged();
	}

	public String getGeneProductForm() {
		return geneProductForm;
	}

	public void setGeneProductForm(String geneProductForm) {
		this.geneProductForm = geneProductForm;
		setChanged();
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
	
	public void setIsNegated(boolean isNegated) {
		this.isNegated = isNegated;
		setChanged();
	}
	
	public boolean isNegated() {
		return isNegated;
	}
	
	public String getWithExpression() {
		return withExpression;
	}

	public void setWithInfos(String withExpression, Collection<String> withInfoList) {
		this.withExpression = withExpression;
		this.withInfoList = withInfoList;
		setChanged();
	}
	
	public Collection<String> getWithInfos(){
		return withInfoList;
	}
	
	public void setCompositeQualifiers(String qualifierString, List<String> compositeQualifiers) {
		this.qualifierString = qualifierString;
		this.compositeQualifiers = compositeQualifiers;
		setChanged();
	}
	
	public String getQualifierString() {
		return qualifierString;
	}

	/**
	 * Retrieve the list of qualifiers. Split the composite string, if
	 * necessary.
	 * 
	 * @return list, never null
	 */
	public List<String> getCompositeQualifiers() {
		if (compositeQualifiers != null) {
			return compositeQualifiers;
		}
		return Collections.emptyList();
	}

	public AnnotationSource getSource() {
		return annotationSource;
	}

	public void setSource(AnnotationSource annotationSource) {
		this.annotationSource = annotationSource;
		setChanged();
	}

	public void addProperty(String key, String value) {
		if (properties == null) {
			properties = new HashMap<String, String>();
		}
		properties.put(key, value);
	}
	
	public Map<String, String> getProperties() {
		return Collections.unmodifiableMap(properties);
	}
}
