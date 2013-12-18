package owltools.tr;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyLoaderListener;

import owltools.OWLToolsTestBasics;
import owltools.io.CatalogXmlIRIMapper;
import owltools.io.ParserWrapper;
import owltools.util.MinimalModelGeneratorTest;

public class TemplatedTransformerTest extends OWLToolsTestBasics {

	private static Logger LOG = Logger.getLogger(TemplatedTransformerTest.class);
	private static boolean verbose = false;

	@Test
	public void testTr() throws Exception {
		CatalogXmlIRIMapper m = new CatalogXmlIRIMapper("src/test/resources/catalog-v001.xml");
		ParserWrapper p = new ParserWrapper();
		OWLOntology owlOntology = p.parse(getResourceIRIString("test-tr.owl"));
		TemplatedTransformer tt = new TemplatedTransformer(owlOntology);
		Set<OWLOntologyChange> chgs = tt.tr();
		for (OWLOntologyChange chg : chgs) {
			LOG.info(chg);
		}
		assertNotNull(owlOntology);
	}


}
