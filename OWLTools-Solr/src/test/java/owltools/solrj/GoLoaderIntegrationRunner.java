package owltools.solrj;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Collection;
import java.util.Properties;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import owltools.flex.FlexCollection;
import owltools.graph.OWLGraphWrapper;
import owltools.io.CatalogXmlIRIMapper;
import owltools.io.ParserWrapper;
import owltools.yaml.golrconfig.ConfigManager;

/**
 * Uses a system property for the location of go file and catalog xml.
 */
public class GoLoaderIntegrationRunner {

	private static String catalogXml = null;
	private static String goFile = null;
	private static int solrCounter = 0;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Properties properties = System.getProperties();
		catalogXml = properties.getProperty(GoLoaderIntegrationRunner.class.getSimpleName()+".catalog-xml");
		goFile = properties.getProperty(GoLoaderIntegrationRunner.class.getSimpleName()+".go-file");
		assertNotNull("Did not find a catalog xml.", catalogXml);
		assertNotNull("Did not find a go file.", goFile);
	}
	
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		
	}
	
	@Test
	public void testLoadGoTaxon() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		pw.addIRIMapper(new CatalogXmlIRIMapper(catalogXml));
		OWLGraphWrapper g = pw.parseToOWLGraph(goFile);
		
		GafSolrDocumentLoaderIntegrationRunner.printMemoryStats();
		GafSolrDocumentLoaderIntegrationRunner.gc();
		GafSolrDocumentLoaderIntegrationRunner.printMemoryStats();
		
		ConfigManager configManager = new ConfigManager();
		configManager.add("src/test/resources/test-ont-config.yaml");
		
		StopWatch watch = new StopWatch();
		watch.start();
		FlexCollection c = new FlexCollection(configManager, g);
		
		GafSolrDocumentLoaderIntegrationRunner.printMemoryStats();
		GafSolrDocumentLoaderIntegrationRunner.gc();
		GafSolrDocumentLoaderIntegrationRunner.printMemoryStats();
		
		FlexSolrDocumentLoader loader = new FlexSolrDocumentLoader((SolrServer)null, c) {

			@Override
			protected void addToServer(Collection<SolrInputDocument> docs)
					throws SolrServerException, IOException {
				solrCounter += docs.size();
			}
			
		};
		loader.load();
		watch.stop();
		GafSolrDocumentLoaderIntegrationRunner.printMemoryStats();
		System.out.println("Loaded "+solrCounter+" Solr docs in "+watch);
		assertTrue(solrCounter > 0);
	}
}
