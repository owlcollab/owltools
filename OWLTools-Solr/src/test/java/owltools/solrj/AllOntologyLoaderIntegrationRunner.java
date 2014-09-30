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
 * Integration test for loading the full set of ontologies, inlucing NCBI taxonomy OWL file.
 * Will take about 2-3 hours to load with 6500M (better 7G or more) of main memory.
 * <p>
 * Uses system properties for the location of the required files.
 * <p>
 * Latest result:
 * <pre>
 * Heap Used Memory:6358 of max 6473 [MB]
 * Loaded 1373907 Solr docs in 1:50:03.928
 * </pre>
 */
public class AllOntologyLoaderIntegrationRunner {

	private static String catalogXml = null;
	private static String goFile = null;
	private static String clFile = null;
	private static String uberonFile = null;
	private static String poFile = null;
	private static String soFile = null;
	private static String prFile = null;
	private static String chebiFile = null;
	private static String taxonFile = null;
	private static int solrCounter = 0;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Properties properties = System.getProperties();
		catalogXml = properties.getProperty(AllOntologyLoaderIntegrationRunner.class.getSimpleName()+".catalog-xml");
		goFile = properties.getProperty(AllOntologyLoaderIntegrationRunner.class.getSimpleName()+".go-file");
		clFile = properties.getProperty(AllOntologyLoaderIntegrationRunner.class.getSimpleName()+".cl-file");
		uberonFile = properties.getProperty(AllOntologyLoaderIntegrationRunner.class.getSimpleName()+".uberon-file");
		poFile = properties.getProperty(AllOntologyLoaderIntegrationRunner.class.getSimpleName()+".po-file");
		soFile = properties.getProperty(AllOntologyLoaderIntegrationRunner.class.getSimpleName()+".so-file");
		prFile = properties.getProperty(AllOntologyLoaderIntegrationRunner.class.getSimpleName()+".pr-file");
		chebiFile = properties.getProperty(AllOntologyLoaderIntegrationRunner.class.getSimpleName()+".chebi-file");
		taxonFile = properties.getProperty(AllOntologyLoaderIntegrationRunner.class.getSimpleName()+".taxon-file");
		assertNotNull("Did not find a catalog xml.", catalogXml);
		assertNotNull("Did not find a go file.", goFile);
		assertNotNull("Did not find a cl file.", clFile);
		assertNotNull("Did not find a uberon file.", uberonFile);
		assertNotNull("Did not find a po file.", poFile);
		assertNotNull("Did not find a so file.", soFile);
		assertNotNull("Did not find a pr file.", prFile);
		assertNotNull("Did not find a chebi file.", chebiFile);
		assertNotNull("Did not find a taxon file.", taxonFile);
	}
	
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		
	}
	
	@Test
	public void testLoadAllOntologies() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		pw.addIRIMapper(new CatalogXmlIRIMapper(catalogXml));
		final OWLGraphWrapper g = pw.parseToOWLGraph(goFile);
		g.mergeOntology(pw.parse(clFile));
		g.mergeOntology(pw.parse(uberonFile));
		g.mergeOntology(pw.parse(poFile));
		g.mergeOntology(pw.parse(soFile));
		g.mergeOntology(pw.parse(prFile));
		g.mergeOntology(pw.parse(chebiFile));
		g.mergeOntology(pw.parse(taxonFile));
		g.setEdgesAdvancedCacheSize(1000);
		
		GafSolrDocumentLoaderIntegrationRunner.printMemoryStats();
		GafSolrDocumentLoaderIntegrationRunner.gc();
		GafSolrDocumentLoaderIntegrationRunner.printMemoryStats();
		
		ConfigManager configManager = new ConfigManager();
		configManager.add("src/test/resources/test-ont-config.yaml");
		FlexCollection c = new FlexCollection(configManager, g);
		
		GafSolrDocumentLoaderIntegrationRunner.printMemoryStats();
		GafSolrDocumentLoaderIntegrationRunner.gc();
		GafSolrDocumentLoaderIntegrationRunner.printMemoryStats();
		
		StopWatch watch = new StopWatch();
		watch.start();
		FlexSolrDocumentLoader loader = new FlexSolrDocumentLoader((SolrServer)null, c) {

			@Override
			protected void addToServer(Collection<SolrInputDocument> docs)
					throws SolrServerException, IOException {
				solrCounter += docs.size();
				GafSolrDocumentLoaderIntegrationRunner.printMemoryStats();
				System.out.println("Cache size: "+g.getCurrentEdgesAdvancedCacheSize());
			}
			
		};
		loader.load();
		watch.stop();
		GafSolrDocumentLoaderIntegrationRunner.printMemoryStats();
		System.out.println("Loaded "+solrCounter+" Solr docs in "+watch);
		assertTrue(solrCounter > 0);
	}
}
