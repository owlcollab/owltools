package owltools.gaf.rules.go;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import owltools.gaf.GafDocument;
import owltools.gaf.GafObjectsBuilder;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.rules.AnnotationRule;
import owltools.gaf.rules.AnnotationRuleViolation;

public class BasicChecksRuleTest extends AbstractRuleTestHelper {

	@Test
	public void testOutdatedIEAs() throws Exception {
		GafObjectsBuilder builder = new GafObjectsBuilder();
		GafDocument gafdoc = builder.buildDocument(getResource("test_out_dated_iea.gaf"));
		AnnotationRule rule = new BasicChecksRule("src/test/resources/rules/GO.xrf_abbs", eco);
		List<GeneAnnotation> annotations = gafdoc.getGeneAnnotations();

		List<AnnotationRuleViolation> allViolations = new ArrayList<AnnotationRuleViolation>();
		for (GeneAnnotation annotation : annotations) {
			Set<AnnotationRuleViolation> violations = rule.getRuleViolations(annotation);
			if (violations != null && !violations.isEmpty()) {
				allViolations.addAll(violations);
			}
		}
		assertEquals(1, allViolations.size());
		AnnotationRuleViolation violation = allViolations.get(0);
		String message = violation.getMessage();
		assertTrue(message.contains("IEA evidence code present with a date more than a year old"));
	}

}
