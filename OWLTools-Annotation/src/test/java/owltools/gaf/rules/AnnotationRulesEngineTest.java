package owltools.gaf.rules;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import owltools.OWLToolsTestBasics;
import owltools.gaf.GafDocument;
import owltools.gaf.GafObjectsBuilder;
import owltools.gaf.rules.go.GoAnnotationRulesFactoryImpl;

public class AnnotationRulesEngineTest extends OWLToolsTestBasics {

	private static boolean renderViolations = false;
	private static final String LOCATION = "src/test/resources/rules/";
	private static AnnotationRulesEngine engine = null;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		String qcfile = LOCATION + "annotation_qc.xml";
		String xrfabbslocation = LOCATION + "GO.xrf_abbs";
		List<String> taxonomylocation = Arrays.asList(LOCATION + "taxon/gene_ontology_ext.obo", 
				LOCATION + "taxon/ncbi_taxon_slim.obo",
				LOCATION + "taxon/taxon_go_triggers.obo", 
				LOCATION + "taxon/taxon_union_terms.obo");
		
		String ecolocation = getResourceIRIString("eco.obo");
		
		AnnotationRulesFactory rulesFactory = new GoAnnotationRulesFactoryImpl(
				qcfile, xrfabbslocation, taxonomylocation, ecolocation);
		engine = new AnnotationRulesEngine(-1, rulesFactory);
	}

	@Test
	public void testValidateAnnotations() throws Exception {
		GafObjectsBuilder builder = new GafObjectsBuilder();
		GafDocument gafdoc = builder.buildDocument(getResource("test_gene_association_mgi.gaf"));			
		Map<String, List<AnnotationRuleViolation>> allViolations = engine.validateAnnotations(gafdoc);
		
		if (renderViolations) {
			renderViolations(allViolations);
		}
		assertEquals(4, allViolations.size()); // 4 types of rule violations
		assertEquals(2, allViolations.get("GO_AR:0000001").size());
		assertEquals(10, allViolations.get("GO_AR:0000013").size());
		assertEquals(1, allViolations.get("GO_AR:0000014").size());
		assertEquals(1, allViolations.get("GO_AR:0000018").size());
	}

	private static void renderViolations(Map<String, List<AnnotationRuleViolation>> allViolations) {
		System.out.println("------------");
		List<String> ruleIds = new ArrayList<String>(allViolations.keySet());
		Collections.sort(ruleIds);
		for (String ruleId : ruleIds) {
			List<AnnotationRuleViolation> violationList = allViolations.get(ruleId);
			System.out.println(ruleId + "  count: "+ violationList.size());
			for (AnnotationRuleViolation violation : violationList) {
				StringBuilder sb = new StringBuilder("Line ");
				sb.append(violation.getLineNumber());
				sb.append(": ");
				sb.append(violation.getMessage());
				System.out.println(sb);
			}
			System.out.println("------------");
		}
	}

}
