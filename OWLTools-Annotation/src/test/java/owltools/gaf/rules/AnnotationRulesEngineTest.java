package owltools.gaf.rules;

import static org.junit.Assert.*;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import owltools.OWLToolsTestBasics;
import owltools.gaf.GafDocument;
import owltools.gaf.GafObjectsBuilder;
import owltools.gaf.rules.AnnotationRuleViolation.ViolationType;
import owltools.gaf.rules.AnnotationRulesEngine.AnnotationRulesEngineResult;
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
		engine = new AnnotationRulesEngine(rulesFactory);
	}

	@Test
	public void testValidateAnnotations() throws Exception {
		GafObjectsBuilder builder = new GafObjectsBuilder();
		GafDocument gafdoc = builder.buildDocument(getResource("test_gene_association_mgi.gaf"));			
		AnnotationRulesEngineResult result = engine.validateAnnotations(gafdoc);
		
		if (renderViolations) {
			renderViolations(result);
		}
		// error
		assertTrue(result.hasErrors());
		Map<String, List<AnnotationRuleViolation>> errors = result.getViolations(ViolationType.Error);
		assertEquals(3, errors.size()); // 3 rules with Errors
		assertEquals(2, errors.get("GO_AR:0000001").size());
		assertEquals(7, errors.get("GO_AR:0000013").size());
		assertEquals(1, errors.get("GO_AR:0000014").size());
		
		// warning
		assertTrue(result.hasWarnings());
		Map<String, List<AnnotationRuleViolation>> warnings = result.getViolations(ViolationType.Warning);
		assertEquals(2, warnings.size()); // 4 rules with Warnings
		assertEquals(3, warnings.get("GO_AR:0000013").size());
		assertEquals(1, warnings.get("GO_AR:0000018").size());
		
		// recommendation
		assertFalse(result.hasRecommendations());
	}

	private static void renderViolations(AnnotationRulesEngineResult result) {
		final PrintWriter writer = new PrintWriter(System.out);
		AnnotationRulesEngineResult.renderViolations(result, engine, writer);
		writer.close();
	}
	
}
