package owltools.gaf.rules.go;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.rules.AnnotationRuleViolation;

/**
 * Tests for {@link GoClassReferenceAnnotationRule}.
 */
public class GoClassReferenceAnnotationRuleTest extends AbstractGoRuleTestHelper {

	@Test
	public void test() throws Exception {
		GafDocument gafdoc = loadZippedGaf("gene_association.goa_human.gz");
		
		GoClassReferenceAnnotationRule rule = new GoClassReferenceAnnotationRule(graph, "GO:","CL:");
		
		List<GeneAnnotation> annotations = gafdoc.getGeneAnnotations();
		
		List<AnnotationRuleViolation> allViolations = new ArrayList<AnnotationRuleViolation>();
		for (GeneAnnotation annotation : annotations) {
			Set<AnnotationRuleViolation> violations = rule.getRuleViolations(annotation);
			if (violations != null && !violations.isEmpty()) {
				allViolations.addAll(violations);
				for (AnnotationRuleViolation violation : violations) {
					System.out.println(violation);
				}
			}
		}
		assertEquals(232, allViolations.size());
	}

}
