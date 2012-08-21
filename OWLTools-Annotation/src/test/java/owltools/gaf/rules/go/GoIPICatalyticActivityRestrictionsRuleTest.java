package owltools.gaf.rules.go;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import owltools.OWLToolsTestBasics;
import owltools.gaf.GafDocument;
import owltools.gaf.GafObjectsBuilder;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.rules.AnnotationRule;
import owltools.gaf.rules.AnnotationRuleViolation;

/**
 * Tests for {@link GoIPICatalyticActivityRestrictionsRule}.
 */
public class GoIPICatalyticActivityRestrictionsRuleTest extends OWLToolsTestBasics {

	@Test
	public void test() throws Exception {
		GafObjectsBuilder builder = new GafObjectsBuilder();
		GafDocument gafdoc = builder.buildDocument(getResource("test_gene_association_mgi.gaf"));
		AnnotationRule rule = new GoIPICatalyticActivityRestrictionsRule(getGraph("rules/taxon/gene_ontology_ext.obo"), getGraph("eco.obo"));
		List<GeneAnnotation> annotations = gafdoc.getGeneAnnotations();
		
		List<AnnotationRuleViolation> allViolations = new ArrayList<AnnotationRuleViolation>();
		for (GeneAnnotation annotation : annotations) {
			Set<AnnotationRuleViolation> violations = rule.getRuleViolations(annotation);
			if (violations != null && !violations.isEmpty()) {
				allViolations.addAll(violations);
			}
		}
		assertEquals(0, allViolations.size());
	}

}
