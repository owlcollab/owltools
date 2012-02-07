package owltools.io;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;

import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;

import owltools.OWLToolsTestBasics;
import owltools.io.CatalogXmlIRIMapper;

public class CatalogXmlIRIMapperTest extends OWLToolsTestBasics {

	@Test
	public void testParseCatalogXML() throws Exception {
		File resource = getResource("catalog-v001.xml");
		InputStream inputStream = new FileInputStream(resource);
		File parentFolder = resource.getParentFile();
		Map<IRI, IRI> mappings = CatalogXmlIRIMapper.parseCatalogXml(inputStream, parentFolder);
		assertEquals(4, mappings.size());
	}
	
	@Test
	public void testParseCatalogXML2() throws Exception {
		CatalogXmlIRIMapper m = new CatalogXmlIRIMapper("src/test/resources/test-catalog.xml");
		IRI iri = m.getDocumentIRI(IRI.create("http://purl.obolibrary.org/obo/go.owl"));
		assertNotNull(iri);
	}

}
