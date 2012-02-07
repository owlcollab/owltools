package owltools.io;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;

import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyLoaderListener;

import owltools.OWLToolsTestBasics;
import owltools.io.CatalogXmlIRIMapper;

public class CatalogXmlIRIMapperTest extends OWLToolsTestBasics {

	private static boolean verbose = false;
	
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
	
	@Test
	public void testParseCatalogXML3() throws Exception {
		CatalogXmlIRIMapper m = new CatalogXmlIRIMapper("src/test/resources/catalog-v001.xml");
		ParserWrapper p = new ParserWrapper();
		p.addIRIMapper(m);
		if (verbose) {
			p.manager.addOntologyLoaderListener(new PrintingOntologLoaderListener());
		}
		OWLOntology owlOntology = p.parse(getResourceIRIString("wine.owl"));
		assertNotNull(owlOntology);
	}
	
	@Test
	public void testParseCatalogXML4() throws Exception {
		CatalogXmlIRIMapper m = new CatalogXmlIRIMapper("src/test/resources/catalog-v001.xml");
		ParserWrapper p = new ParserWrapper();
		p.addIRIMapper(m);
		if (verbose) {
			p.manager.addOntologyLoaderListener(new PrintingOntologLoaderListener());
		}
		OWLOntology owlOntology = p.parse(getResourceIRIString("mutual-import-1.owl"));
		assertNotNull(owlOntology);
	}

	public static final class PrintingOntologLoaderListener implements
			OWLOntologyLoaderListener {
		@Override
		public void startedLoadingOntology(LoadingStartedEvent event) {
			System.out.println("Loading: "+event.getDocumentIRI());
			
		}
	
		@Override
		public void finishedLoadingOntology(LoadingFinishedEvent event) {
			System.out.println("Finished: "+event.getDocumentIRI());
		}
	}
	
}
