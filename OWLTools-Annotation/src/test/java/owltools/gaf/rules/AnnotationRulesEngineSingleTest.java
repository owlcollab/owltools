package owltools.gaf.rules;

import static org.junit.Assert.*;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;

import owltools.OWLToolsTestBasics;
import owltools.gaf.GafDocument;
import owltools.gaf.eco.EcoMapperFactory;
import owltools.gaf.eco.TraversingEcoMapper;
import owltools.gaf.parser.GafObjectsBuilder;
import owltools.gaf.rules.AnnotationRuleViolation.ViolationType;
import owltools.gaf.rules.AnnotationRulesEngine.AnnotationRulesEngineResult;
import owltools.gaf.rules.go.GoAnnotationRulesFactoryImpl;
import owltools.graph.OWLGraphWrapper;
import owltools.io.CatalogXmlIRIMapper;
import owltools.io.ParserWrapper;

/**
 * Tests for {@link AnnotationRulesEngine}.
 *
 * TODO currently this is excluded from the test suite due to incomplete check for taxon violations.
 */
public class AnnotationRulesEngineSingleTest extends OWLToolsTestBasics {

	private static boolean renderViolations = false;
	private static final String LOCATION = "src/test/resources/rules/";
	private static TraversingEcoMapper eco = null;
	private static AnnotationRulesEngine engine = null;

	private static Level elkLogLevel = null;
	private static Logger elkLogger = null;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		elkLogger = Logger.getLogger("org.semanticweb.elk");
		elkLogLevel = elkLogger.getLevel();
		elkLogger.setLevel(Level.ERROR);
		
		String qcfile = LOCATION + "annotation_qc.xml";
		String xrfabbslocation = LOCATION + "GO.xrf_abbs";
		
		ParserWrapper p = new ParserWrapper();
		
		OWLOntologyIRIMapper mapper = new CatalogXmlIRIMapper(getResource("rules/ontology/extensions/catalog-v001.xml"));
		p.addIRIMapper(mapper);
		
		OWLOntology goTaxon = p.parse("http://purl.obolibrary.org/obo/go/extensions/go-plus.owl");
		OWLOntology gorel = p.parse("http://purl.obolibrary.org/obo/go/extensions/gorel.owl");
		OWLGraphWrapper graph = new OWLGraphWrapper(goTaxon);
		graph.addImport(gorel);
		eco = EcoMapperFactory.createTraversingEcoMapper(p, getResourceIRIString("eco.obo")).getMapper();
		
		AnnotationRulesFactory rulesFactory = new GoAnnotationRulesFactoryImpl(
				qcfile, xrfabbslocation, graph, eco, null);
		engine = new AnnotationRulesEngine(rulesFactory, true, false);
	}

	@Ignore
	@Test
	public void testValidateAnnotations() throws Exception {
		GafObjectsBuilder builder = new GafObjectsBuilder();
		GafDocument gafdoc = builder.buildDocument(getResource("test_gene_association_mgi_single.gaf"));			
		AnnotationRulesEngineResult result = engine.validateAnnotations(gafdoc);
		
		if (renderViolations) {
			renderViolations(result);
		}
		// error
		assertTrue(result.hasErrors());
		Map<String, List<AnnotationRuleViolation>> errors = result.getViolations(ViolationType.Error);
		assertEquals(1, errors.size()); // 1 rules with Errors
		assertEquals(1, errors.get("GO_AR:0000013").size());
		
		// warning
		assertFalse(result.hasWarnings());
		
		// recommendation
		assertFalse(result.hasRecommendations());
	}

	private static void renderViolations(AnnotationRulesEngineResult result) {
		AnnotationRulesReportWriter.renderViolations(result, engine, new PrintWriter(System.out));
	}
	
	@AfterClass
	public static void afterClass() throws Exception {
		if (engine != null) {
			engine = null;
		}
		if (eco != null) {
			eco.dispose();
			eco = null;
		}
		if (elkLogLevel != null && elkLogger != null) {
			elkLogger.setLevel(elkLogLevel);
			elkLogger = null;
			elkLogLevel = null;
		}
	}
}
