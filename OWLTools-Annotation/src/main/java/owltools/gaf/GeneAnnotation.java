package owltools.gaf;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import owltools.gaf.parser.BuilderTools;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

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

	private String bioentity = DEFAULT_STRING_VALUE;            // used for c1 and c2
	private Bioentity bioentityObject = null;                    // encompass columns 1-3, 10-12
	private String relation = DEFAULT_STRING_VALUE;            // implicit relation
	private String cls = DEFAULT_STRING_VALUE;                    // Col. 5
	private List<String> referenceIds = null;                    // Col. 6
	private String ecoEvidenceCls;                                // GPAD only
	private String shortEvidence = DEFAULT_STRING_VALUE;        // Col. 7
	private String aspect = DEFAULT_STRING_VALUE;                // Col. 9
    private String synonyms = DEFAULT_STRING_VALUE;              // Col. 11 synonyms
	private Pair<String, String> actsOnTaxonId = null;            // Col. 13
	private String lastUpdateDate = DEFAULT_STRING_VALUE;        // Col. 14 //TODO: convert it to date
	private String assignedBy = DEFAULT_STRING_VALUE;            // Col. 15
	private String geneProductForm = DEFAULT_STRING_VALUE;        // Col. 17
	private List<Pair<String, String>> properties = null;        // GPAD only

	private boolean is_MRC;
	private boolean is_DirectNot;

	private int qualifier_flags;
	public static final int CONTRIBUTES_TO_MASK = 1;    // 2^^0    000...00000001
	public static final int COLOCALIZES_MASK = 2;    // 2^^1    000...00000010
	public static final int INTEGRAL_TO_MASK = 4;    // 2^^2    000...00000100
	public static final int NOT_MASK = 8;    // 2^^3    000...00001000
	public static final int CUT_MASK = 16;   // 2^^4    000...00010000

	// derived from c8
	private Collection<String> withInfoList = null; // col 8

	// derived from c16
	private List<List<ExtensionExpression>> extensionExpressionList = null; // col 16

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
	private synchronized void buildRow() {
		if (!isChanged)
			return;

		StringBuilder s = new StringBuilder();

		String taxon = null;
		String dbObjectSynonym = null;
		String dbObjectName = null;
		String dbObjectType = null;
		String symbol = null;

		if (this.bioentityObject != null) {
			taxon = bioentityObject.getNcbiTaxonId();
			if (taxon != null) {
				taxon = "taxon:" + BuilderTools.removePrefix(taxon, ':');
			}

			dbObjectName = this.bioentityObject.getFullName();
			dbObjectType = this.bioentityObject.getTypeCls();
			List<String> synonyms = this.bioentityObject.getSynonyms();
			if (synonyms != null && !synonyms.isEmpty()) {
				dbObjectSynonym = StringUtils.join(synonyms, '|');
			}
			symbol = this.bioentityObject.getSymbol();
		}

		if (this.bioentity != null) {
			int i = bioentity.indexOf(":");
			if (i > -1) {
				s.append(bioentity.substring(0, i)).append("\t").append(bioentity.substring(i + 1)).append("\t");
			} else {
				s.append(bioentity).append("\t");
			}
		} else {
			s.append("\t\t");
		}

		append(symbol, s);
		append(BuilderTools.buildGafQualifierString(this), s);
		append(cls, s);
		append(BuilderTools.buildReferenceIdsString(referenceIds), s);
		append(shortEvidence, s);
		append(BuilderTools.buildWithString(withInfoList), s);
		append(aspect, s);
		append(dbObjectName, s);
		append(dbObjectSynonym, s);
		append(dbObjectType, s);
		append(BuilderTools.buildTaxonString(taxon, actsOnTaxonId), s);
		append(lastUpdateDate, s);
		append(assignedBy, s);

		append(BuilderTools.buildExtensionExpression(extensionExpressionList), s);
		append(geneProductForm, s);

		this.isChanged = false;
		this.toString = s.toString();
	}

	private static void append(CharSequence s, StringBuilder builder) {
		if (s != null) {
			builder.append(s);
		}
		builder.append('\t');
	}

	public String toString() {
		buildRow();
		return toString;
	}

	public GeneAnnotation() {
		// intentionally empty
	}

	public GeneAnnotation(GeneAnnotation ann) {
		super();
		this.bioentity = ann.bioentity;
		this.bioentityObject = ann.bioentityObject;
		this.setIsColocatesWith(ann.isColocatesWith());
		this.setIsContributesTo(ann.isContributesTo());
		this.cls = ann.cls;
		this.referenceIds = copy(ann.referenceIds);
		this.shortEvidence = ann.shortEvidence;
		this.ecoEvidenceCls = ann.ecoEvidenceCls;
		this.withInfoList = copy(ann.withInfoList);
		this.aspect = ann.aspect;
		this.actsOnTaxonId = ann.actsOnTaxonId;
		this.lastUpdateDate = ann.lastUpdateDate;
		this.assignedBy = ann.assignedBy;
		this.extensionExpressionList = copyExpr(ann.extensionExpressionList);
		this.geneProductForm = ann.geneProductForm;
		this.properties = copy(ann.properties);
		this.relation = ann.relation;
		setChanged();
	}

	private static <T> List<T> copy(Collection<T> source) {
		List<T> copy = null;
		if (source != null) {
			copy = new ArrayList<T>(source);
		}
		return copy;
	}

	private static List<List<ExtensionExpression>> copyExpr(List<List<ExtensionExpression>> source) {
		List<List<ExtensionExpression>> copy = null;
		if (source != null) {
			copy = new ArrayList<List<ExtensionExpression>>(source.size());
			for (List<ExtensionExpression> exprList : source) {
				copy.add(new ArrayList<ExtensionExpression>(exprList));
			}
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
		} else {
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

	public Pair<String, String> getActsOnTaxonId() {
		return actsOnTaxonId;
	}

	public void setAspect(String inAspect) {
		this.aspect = inAspect;
		setChanged();
	}

	public String getAspect() {
		return aspect;
	}

	public void setActsOnTaxonId(String actsOnTaxonId) {
		setActsOnTaxonId(Pair.of(actsOnTaxonId, relation));
	}

	public void setActsOnTaxonId(Pair<String, String> taxonRelPair) {
		this.actsOnTaxonId = taxonRelPair;
		setChanged();
	}

	public String getLastUpdateDate() {
		return lastUpdateDate;
	}

	public void setLastUpdateDate(Date date) {
		if (date != null) {
			String dateString = GAF_Date_Format.get().format(date);
			setLastUpdateDate(dateString);
		} else {
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

	public List<List<ExtensionExpression>> getExtensionExpressions() {
		return extensionExpressionList;
	}

	public void setExtensionExpressions(List<List<ExtensionExpression>> expressions) {
		this.extensionExpressionList = expressions;
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

	public boolean isContributesTo() {
		return (qualifier_flags & this.CONTRIBUTES_TO_MASK) == this.CONTRIBUTES_TO_MASK;
	}

	public void setIsContributesTo(boolean isContributesTo) {
		if (isContributesTo)
			qualifier_flags |= this.CONTRIBUTES_TO_MASK;
		else
			qualifier_flags &= ~this.CONTRIBUTES_TO_MASK;
		setChanged();
	}

	public boolean isColocatesWith() {
		return (qualifier_flags & this.COLOCALIZES_MASK) == this.COLOCALIZES_MASK;
	}

	public void setIsColocatesWith(boolean isColocatesWith) {
		if (isColocatesWith)
			qualifier_flags |= this.COLOCALIZES_MASK;
		else
			qualifier_flags &= ~this.COLOCALIZES_MASK;
		setChanged();
	}

	public boolean isIntegralTo() {
		return (qualifier_flags & this.INTEGRAL_TO_MASK) == this.INTEGRAL_TO_MASK;
	}

	public void setIsIntegralTo(boolean isIntegralTo) {
		if (isIntegralTo)
			qualifier_flags |= this.INTEGRAL_TO_MASK;
		else
			qualifier_flags &= ~this.INTEGRAL_TO_MASK;
		setChanged();
	}

	public void setIsNegated(boolean isNegated) {
		if (isNegated)
			qualifier_flags |= this.NOT_MASK;
		else
			qualifier_flags &= ~this.NOT_MASK;
		setChanged();
	}

	public boolean isNegated() {
		return (qualifier_flags & this.NOT_MASK) == this.NOT_MASK;
	}

	public void setIsCut(boolean isCut) {
		if (isCut)
			qualifier_flags |= this.CUT_MASK;
		else
			qualifier_flags &= ~this.CUT_MASK;
		setChanged();
	}

	public boolean isCut() {
		return (qualifier_flags & this.CUT_MASK) == this.CUT_MASK;
	}

	public void setWithInfos(Collection<String> withInfoList) {
		this.withInfoList = withInfoList;
		setChanged();
	}

	public Collection<String> getWithInfos() {
		return withInfoList;
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
			properties = new ArrayList<Pair<String, String>>();
		}
		properties.add(Pair.of(key, value));
	}

	public List<Pair<String, String>> getProperties() {
		return properties;
	}

	public boolean hasQualifiers() {
		return this.qualifier_flags > 0;
	}

	public void setQualifiers(int qualifiers) {
		this.qualifier_flags = qualifiers;
	}

	public int getQualifiers() {
		return qualifier_flags;
	}

	public boolean isMRC() {
		return is_MRC;
	}

	public void setDirectMRC(boolean is_MRC) {
		this.is_MRC = is_MRC;
	}

	public boolean isDirectNot() {
		return is_DirectNot;
	}

	public void setDirectNot(boolean isDirectNot) {
		is_DirectNot = isDirectNot;
	}

}
