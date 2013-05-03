package owltools.gaf.rules.go;

import static org.junit.Assert.*;

import java.util.Set;

import org.junit.Test;

import owltools.gaf.GafDocument;
import owltools.gaf.rules.AnnotationRule;
import owltools.gaf.rules.AnnotationRuleViolation;

public class GOReciprocalAnnotationRuleTest extends AbstractGoRuleTestHelper {

	@Test
	public void test() throws Exception {
		GafDocument gafdoc = loadGaf("test_gene_association_mgi.gaf");
		
		AnnotationRule rule = new GOReciprocalAnnotationRule(graph, eco);
		
		Set<AnnotationRuleViolation> violations = rule.getRuleViolations(gafdoc);
		assertEquals(5, violations.size());
	}

}
