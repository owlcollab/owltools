package owltools.gaf.rules.go;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.rules.AnnotationRule;
import owltools.gaf.rules.AnnotationRuleViolation;

public class BasicChecksRuleTest extends AbstractEcoRuleTestHelper {

	private static AnnotationRule rule = null;
	
	@BeforeClass
	public static void beforeClass() throws Exception {
		AbstractEcoRuleTestHelper.beforeClass();
		rule = new BasicChecksRule("src/test/resources/rules/GO.xrf_abbs", eco);
	}
	
	@Test
	public void testOutdatedIEAs() throws Exception {
		GafDocument gafdoc = loadGaf("test_out_dated_iea.gaf");
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

	@Test
	public void testMissingPrefixForC16() throws Exception {
		GafDocument gafdoc = loadGaf("id_prefix_c16_valid.gaf");
		List<GeneAnnotation> validAnnotations = gafdoc.getGeneAnnotations();
		assertEquals(1, validAnnotations.size());
		
		Set<AnnotationRuleViolation> ruleViolations = rule.getRuleViolations(validAnnotations.get(0));
		assertTrue(ruleViolations.isEmpty());
		
		
		gafdoc = loadGaf("id_prefix_c16_invalid.gaf");
		List<GeneAnnotation> inValidAnnotations = gafdoc.getGeneAnnotations();
		assertEquals(1, inValidAnnotations.size());
		
		ruleViolations = rule.getRuleViolations(inValidAnnotations.get(0));
		assertEquals(1, ruleViolations.size());	
		AnnotationRuleViolation violation = ruleViolations.iterator().next();
		final String message = violation.getMessage();
		assertTrue(message.contains("The id 'SPCC1682.02c' has no prefix."));
	}
}
