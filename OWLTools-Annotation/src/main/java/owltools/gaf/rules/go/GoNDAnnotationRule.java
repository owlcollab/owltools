package owltools.gaf.rules.go;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import owltools.gaf.GeneAnnotation;
import owltools.gaf.eco.TraversingEcoMapper;
import owltools.gaf.rules.AbstractAnnotationRule;
import owltools.gaf.rules.AnnotationRuleViolation;
import owltools.gaf.rules.AnnotationRuleViolation.ViolationType;

/**
 * GO_AR:0000011
 */
public class GoNDAnnotationRule extends AbstractAnnotationRule {

	/**
	 * The string to identify this class in the annotation_qc.xml and related factories.
	 * This is not supposed to be changed. 
	 */
	public static final String PERMANENT_JAVA_ID = "org.geneontology.rules.GO_AR_0000011";
	
	private static final String MESSAGE = "ND annotations to root nodes only";
	
	private final String message;
	private final ViolationType violationType;
	
	private final Set<String> evidences;
	private final Set<String> rootIds;
	private final Set<String> ndXrefs;
	
	public GoNDAnnotationRule(TraversingEcoMapper eco) {
		this.message = MESSAGE;
		this.violationType = ViolationType.Error;
		
		evidences = eco.getAllValidEvidenceIds("ND", true);
		
		rootIds = new HashSet<String>();
		rootIds.addAll(Arrays.asList("GO:0005575", "GO:0003674", "GO:0008150"));

		ndXrefs = new HashSet<String>();
		ndXrefs.addAll(Arrays.asList(
				"GO_REF:0000015", 
				"FB:FBrf0159398",
				"ZFIN:ZDB-PUB-031118-1",
				"dictyBase_REF:9851",
				"MGI:MGI:2156816",
				"SGD_REF:S000069584", 
				"CGD_REF:CAL0125086",
				"RGD:1598407",
				"TAIR:Communication:1345790",
				"TAIR:Communication:501683652",
				"AspGD_REF:ASPL0000111607"));
					
	}
	
	@Override
	public Set<AnnotationRuleViolation> getRuleViolations(GeneAnnotation a) {
		String evidenceCls = a.getEvidenceCls();
		if (evidenceCls != null && evidences.contains(evidenceCls)) {
			String cls = a.getCls();
			if (rootIds.contains(cls) == false) {
				AnnotationRuleViolation violation = new AnnotationRuleViolation(getRuleId(), message+", but was: "+cls, a, violationType);
				return Collections.singleton(violation);
			}
			List<String> referenceIds = a.getReferenceIds();
			boolean hasRef = false;
			for (String referenceId : referenceIds) {
				if (ndXrefs.contains(referenceId)) {
					hasRef = true;
					break;
				}
			}
			if (hasRef == false) {
				AnnotationRuleViolation violation = new AnnotationRuleViolation(getRuleId(), "ND annotations to root require reference ID", a, ViolationType.Warning);
				return Collections.singleton(violation);
			}
		}
		else {
			String cls = a.getCls();
			if (rootIds.contains(cls)) {
				AnnotationRuleViolation violation = new AnnotationRuleViolation(getRuleId(), message+", but was root node: "+cls+" with evidence code: "+evidenceCls, a, violationType);
				return Collections.singleton(violation);
			}
		}
		return null;
	}
}
