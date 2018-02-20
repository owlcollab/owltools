package owltools.solrj;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Collection;
import java.util.Properties;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import owltools.flex.FlexCollection;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;
import owltools.yaml.golrconfig.ConfigManager;

/**
 * Integration test for loading the full NCBI taxonomy OWL file.
 * Will take about 45 minutes to load with 5GB of main memory.
 * 
 * Uses a system property for the location of the taxonomy.owl file.
 */
public class FullTaxonIntegrationRunner {

	private static String taxonFile = null;
	private static int solrCounter = 0;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Properties properties = System.getProperties();
		taxonFile = properties.getProperty(FullTaxonIntegrationRunner.class.getSimpleName()+".taxon-file");
		assertNotNull("Did not find a taxon file.", taxonFile);
	}
	
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		
	}
	
	@Test
	public void testLoadFullTaxon() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		final OWLGraphWrapper g = pw.parseToOWLGraph(taxonFile);
		
		GafSolrDocumentLoaderIntegrationRunner.printMemoryStats();
		GafSolrDocumentLoaderIntegrationRunner.gc();
		GafSolrDocumentLoaderIntegrationRunner.printMemoryStats();
		
		ConfigManager configManager = new ConfigManager();
		configManager.add("src/test/resources/test-ont-config.yaml");
		FlexCollection c = new FlexCollection(configManager, g);
		
		GafSolrDocumentLoaderIntegrationRunner.printMemoryStats();
		GafSolrDocumentLoaderIntegrationRunner.gc();
		GafSolrDocumentLoaderIntegrationRunner.printMemoryStats();
		
		FlexSolrDocumentLoader loader = new FlexSolrDocumentLoader((SolrClient)null, c) {

			@Override
			protected void addToServer(Collection<SolrInputDocument> docs)
					throws SolrServerException, IOException {
				solrCounter += docs.size();
				GafSolrDocumentLoaderIntegrationRunner.printMemoryStats();
				System.out.println("Cache size: "+g.getCurrentEdgesAdvancedCacheSize());
			}
			
		};
		loader.load();
		assertTrue(solrCounter > 0);
	}
}
