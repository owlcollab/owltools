package owltools.gaf.rules.go;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.rules.AnnotationRule;
import owltools.gaf.rules.AnnotationRuleViolation;

/**
 * Tests for {@link GoIPICatalyticActivityRestrictionsRule}.
 */
public class GoIPICatalyticActivityRestrictionsRuleTest extends AbstractGoRuleTestHelper {

	@Test
	public void test() throws Exception {
		GafDocument gafdoc = loadGaf("test_gene_association_mgi.gaf");
		
		AnnotationRule rule = new GoIPICatalyticActivityRestrictionsRule(graph, eco);
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
