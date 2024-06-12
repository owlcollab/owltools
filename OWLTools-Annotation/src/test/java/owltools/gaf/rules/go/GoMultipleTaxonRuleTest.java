package owltools.gaf.rules.go;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.rules.AnnotationRule;
import owltools.gaf.rules.AnnotationRuleViolation;

public class GoMultipleTaxonRuleTest extends AbstractGoRuleTestHelper {

	@Ignore
	@Test
	public void test() throws Exception {
		GafDocument gafdoc = loadZippedGaf("gene_association.PAMGO_Mgrisea.gz");
		
		AnnotationRule rule = new GoMultipleTaxonRule(graph);
		
		List<AnnotationRuleViolation> allViolations = new ArrayList<AnnotationRuleViolation>();
		List<GeneAnnotation> annotations = gafdoc.getGeneAnnotations();
		for (GeneAnnotation annotation : annotations) {
			Set<AnnotationRuleViolation> violations = rule.getRuleViolations(annotation);
			if (violations != null && !violations.isEmpty()) {
				allViolations.addAll(violations);
			}
		}
		assertEquals(89, allViolations.size());
	}

}
