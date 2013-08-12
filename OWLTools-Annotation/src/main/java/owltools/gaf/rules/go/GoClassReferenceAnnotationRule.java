package owltools.gaf.rules.go;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import owltools.gaf.ExtensionExpression;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.rules.AbstractAnnotationRule;
import owltools.gaf.rules.AnnotationRuleViolation;
import owltools.gaf.rules.AnnotationRuleViolation.ViolationType;
import owltools.graph.OWLGraphWrapper;

/**
 * Checks to see if an annotation uses a class that is not in the current ontology, and that
 * the class has not been obsoleted
 */
public class GoClassReferenceAnnotationRule extends AbstractAnnotationRule {
	
	/**
	 * The string to identify this class in the annotation_qc.xml and related factories.
	 * This is not supposed to be changed. 
	 */
	public static final String PERMANENT_JAVA_ID = "org.geneontology.gold.rules.GoClassReferenceAnnotationRule";

	private final OWLGraphWrapper graph;
	private final Map<String, OWLObject> owlObjectsByAltId;
	private final Map<String, OWLObjectProperty> relByAltId;
	
	private final List<String> c16PrefixWhiteList;

	public GoClassReferenceAnnotationRule(OWLGraphWrapper wrapper, String...idPrefixes){
		this.graph = wrapper; 
		owlObjectsByAltId = graph.getAllOWLObjectsByAltId();
		relByAltId = new HashMap<String, OWLObjectProperty>();
		for(String altId : owlObjectsByAltId.keySet()) {
			OWLObject owlObject = owlObjectsByAltId.get(altId);
			if (owlObject instanceof OWLObjectProperty) {
				OWLObjectProperty rel = (OWLObjectProperty) owlObject;
				relByAltId.put(altId, rel);
			}
		}
		c16PrefixWhiteList = Arrays.asList(idPrefixes);
	}

	@Override
	public Set<AnnotationRuleViolation> getRuleViolations(GeneAnnotation a) {

		HashSet<AnnotationRuleViolation> set = new HashSet<AnnotationRuleViolation>();
		String id = a.getCls();
		OWLClass owlClass = graph.getOWLClassByIdentifier(id);

		// check cls
		if (owlClass == null) {
			OWLObject owlObject = owlObjectsByAltId.get(id);
			if (owlObject == null) {
				set.add(new AnnotationRuleViolation(getRuleId(),
					"The id '"+id+"' in the annotation is a dangling reference", a));
			}
			else {
				String mainId = graph.getIdentifier(owlObject);
				set.add(new AnnotationRuleViolation(getRuleId(), 
					"The id '"+id+"' in the annotation is an alternate id for: "+mainId, a, ViolationType.Warning));
			}
		}
		else {
			boolean isObsolete = graph.isObsolete(owlClass);
			if (isObsolete) {
				StringBuilder msg = new StringBuilder();
				msg.append("The id '").append(id).append("' in the annotation is an obsolete class");
				List<String> replacedBy = graph.getReplacedBy(owlClass);
				if (replacedBy != null && !replacedBy.isEmpty()) {
					msg.append(", suggested replacements:");
					for (String replace : replacedBy) {
						msg.append(' ').append(replace);
					}
				}
				set.add(new AnnotationRuleViolation(getRuleId(), msg.toString(), a));
			}
		}
		
		// check c16
		List<List<ExtensionExpression>> groupedExpressions = a.getExtensionExpressions();
		if (groupedExpressions != null && !groupedExpressions.isEmpty()) {
			for (List<ExtensionExpression> expressions : groupedExpressions) {
				for (ExtensionExpression expression : expressions) {
					// check c16 cls only if it is in the whitelist
					String exId = expression.getCls();
					boolean doCheck = false;
					for(String idPrefix : c16PrefixWhiteList) {
						if (exId.startsWith(idPrefix)) {
							doCheck = true;
							break;
						}
					}
					if (doCheck) {
						OWLClass exCls = graph.getOWLClassByIdentifier(exId);
						if (exCls == null) {
							OWLObject owlObject = owlObjectsByAltId.get(exId);
							if (owlObject == null) {
								set.add(new AnnotationRuleViolation(getRuleId(),
										"The id '"+exId+"' in the c16 annotation extension is a dangling reference", a));
							}
							else {
								String mainId = graph.getIdentifier(owlObject);
								set.add(new AnnotationRuleViolation(getRuleId(), 
										"The id '"+exId+"' in the c16 annotation extension is an alternate id for: "+mainId, a, ViolationType.Warning));
							}
						}
						else {
							boolean isObsolete = graph.isObsolete(exCls);
							if (isObsolete) {
								StringBuilder msg = new StringBuilder();
								msg.append("The id '").append(exId).append("' in the c16 annotation extension is an obsolete class");
								List<String> replacedBy = graph.getReplacedBy(exCls);
								if (replacedBy != null && !replacedBy.isEmpty()) {
									msg.append(", suggested replacements:");
									for (String replace : replacedBy) {
										msg.append(' ').append(replace);
									}
								}
								set.add(new AnnotationRuleViolation(getRuleId(), msg.toString(), a));
							}
						}
					}

					// check c16 relation
					String exRel = expression.getRelation();
					OWLObjectProperty rel = graph.getOWLObjectPropertyByIdentifier(exRel);
					if (rel == null) {
						rel = relByAltId.get(rel);
						if (rel == null) {
							set.add(new AnnotationRuleViolation(getRuleId(),
									"The relation '"+exRel+"' in the c16 annotation extension is a dangling reference", a));
						}
						else {
							String mainId = graph.getIdentifier(rel);
							set.add(new AnnotationRuleViolation(getRuleId(), 
									"The relation '"+exRel+"' in the c16 annotation extension is an alternate id for: "+mainId, a, ViolationType.Warning));
						}
					}
					else {
						boolean isObsolete = graph.isObsolete(rel);
						if (isObsolete) {
							StringBuilder msg = new StringBuilder();
							msg.append("The relation '").append(exRel).append("' in the c16 annotation extension is an obsolete relation");
							List<String> replacedBy = graph.getReplacedBy(rel);
							if (replacedBy != null && !replacedBy.isEmpty()) {
								msg.append(", suggested replacements:");
								for (String replace : replacedBy) {
									msg.append(' ').append(replace);
								}
							}
							set.add(new AnnotationRuleViolation(getRuleId(), msg.toString(), a));
						}
					}
				}
			}
		}
		return set;
	}

}
