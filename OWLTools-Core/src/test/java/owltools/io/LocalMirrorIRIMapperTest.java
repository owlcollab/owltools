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

public class LocalMirrorIRIMapperTest extends OWLToolsTestBasics {

	private static boolean verbose = false;
	
	@Test
	public void testParseCatalogXML() throws Exception {
		File resource = getResource("owl-mirror.txt");
		InputStream inputStream = new FileInputStream(resource);
		File parentFolder = resource.getParentFile();
		Map<IRI, IRI> mappings = LocalMirrorIRIMapper.parseDirectoryMappingFile(inputStream, parentFolder);
		assertTrue(mappings.size() == 2);
	}
	
	@Test
	public void testParseCatalogXML2() throws Exception {
		LocalMirrorIRIMapper m = new LocalMirrorIRIMapper("src/test/resources/owl-mirror.txt");
		IRI iri = m.getDocumentIRI(IRI.create("http://purl.obolibrary.org/obo/go.owl"));
		assertNotNull(iri);
	}
	
		
	@Test
	public void testParseCatalogXML4() throws Exception {
		LocalMirrorIRIMapper m = new LocalMirrorIRIMapper("src/test/resources/owl-mirror.txt");
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
