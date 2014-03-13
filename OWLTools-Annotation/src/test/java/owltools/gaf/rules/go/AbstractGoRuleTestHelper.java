package owltools.gaf.rules.go;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;

import owltools.gaf.GafDocument;
import owltools.gaf.parser.GafObjectsBuilder;
import owltools.graph.OWLGraphWrapper;
import owltools.io.CatalogXmlIRIMapper;
import owltools.io.ParserWrapper;

public abstract class AbstractGoRuleTestHelper extends AbstractEcoRuleTestHelper {

	protected static OWLGraphWrapper graph = null;
	
	private static Level owltoolsLogLevel = null;
	private static Logger owltoolsLogger = null;
	
	protected GafDocument loadZippedGaf(String file) throws Exception {
		GafObjectsBuilder builder = new GafObjectsBuilder();
		GafDocument gafdoc = builder.buildDocument(getResource(file).getAbsolutePath());
		return gafdoc;
	}
	
	@BeforeClass
	public static void beforeClass() throws Exception {
		owltoolsLogger = Logger.getLogger("owltools");
		owltoolsLogLevel = owltoolsLogger.getLevel();
		owltoolsLogger.setLevel(Level.ERROR);
		AbstractEcoRuleTestHelper.beforeClass();
		if (graph == null) {
			ParserWrapper p = new ParserWrapper();
			OWLOntologyIRIMapper mapper = new CatalogXmlIRIMapper(getResource("rules/ontology/extensions/catalog-v001.xml"));
			p.addIRIMapper(mapper);
			OWLOntology goTaxon = p.parse("http://purl.obolibrary.org/obo/go/extensions/go-plus.owl");
			OWLOntology gorel = p.parse("http://purl.obolibrary.org/obo/go/extensions/gorel.owl");
			
			graph = new OWLGraphWrapper(goTaxon);
			graph.addImport(gorel);
		}
	}
	
	@AfterClass
	public static void afterClass() throws Exception {
		if (graph != null) {
			graph = null;
		}
		if (owltoolsLogLevel != null && owltoolsLogger != null) {
			owltoolsLogger.setLevel(owltoolsLogLevel);
			owltoolsLogger = null;
			owltoolsLogLevel = null;
		}
		AbstractEcoRuleTestHelper.afterClass();
	}
}
