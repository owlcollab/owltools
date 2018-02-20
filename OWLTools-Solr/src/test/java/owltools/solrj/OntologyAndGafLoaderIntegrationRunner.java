package owltools.solrj;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Properties;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.flex.FlexCollection;
import owltools.gaf.EcoTools;
import owltools.gaf.GafDocument;
import owltools.gaf.TaxonTools;
import owltools.gaf.parser.GafObjectsBuilder;
import owltools.graph.OWLGraphWrapper;
import owltools.io.CatalogXmlIRIMapper;
import owltools.io.ParserWrapper;
import owltools.yaml.golrconfig.ConfigManager;

/**
 * Integration test for loading the full set of ontologies, NOT including NCBI
 * taxonomy full (only slim) and one GAF.
 * <p>
 * Uses system properties for the location of the required files.
 * <p>
 * Ontology loading:
 * <pre>
 * Heap Used Memory:2813 of max 4551 [MB]
 * Loaded 202815 ontology Solr docs in 0:23:17.659
 * </pre>
 * GAF loading:
 * <pre>
 * Heap Used Memory:2955 of max 4771 [MB]
 * Done in 2 minutes for mgi GAF
 * </pre>
 */
public class OntologyAndGafLoaderIntegrationRunner {

	private static String catalogXml = null;
	private static String goFile = null;
	private static String clFile = null;
	private static String uberonFile = null;
	private static String poFile = null;
	private static String soFile = null;
	private static String prFile = null;
	private static String chebiFile = null;
	private static String gafLocation = null;
	
	private static int solrCounter = 0;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Properties properties = System.getProperties();
		catalogXml = properties.getProperty(OntologyAndGafLoaderIntegrationRunner.class.getSimpleName()+".catalog-xml");
		goFile = properties.getProperty(OntologyAndGafLoaderIntegrationRunner.class.getSimpleName()+".go-file");
		clFile = properties.getProperty(OntologyAndGafLoaderIntegrationRunner.class.getSimpleName()+".cl-file");
		uberonFile = properties.getProperty(OntologyAndGafLoaderIntegrationRunner.class.getSimpleName()+".uberon-file");
		poFile = properties.getProperty(OntologyAndGafLoaderIntegrationRunner.class.getSimpleName()+".po-file");
		soFile = properties.getProperty(OntologyAndGafLoaderIntegrationRunner.class.getSimpleName()+".so-file");
		prFile = properties.getProperty(OntologyAndGafLoaderIntegrationRunner.class.getSimpleName()+".pr-file");
		chebiFile = properties.getProperty(OntologyAndGafLoaderIntegrationRunner.class.getSimpleName()+".chebi-file");
		gafLocation = properties.getProperty(OntologyAndGafLoaderIntegrationRunner.class.getSimpleName()+".gaf-folder");
		assertNotNull("Did not find a catalog xml.", catalogXml);
		assertNotNull("Did not find a go file.", goFile);
		assertNotNull("Did not find a cl file.", clFile);
		assertNotNull("Did not find a uberon file.", uberonFile);
		assertNotNull("Did not find a po file.", poFile);
		assertNotNull("Did not find a so file.", soFile);
		assertNotNull("Did not find a pr file.", prFile);
		assertNotNull("Did not find a chebi file.", chebiFile);
		assertNotNull("Did not find a gaf location.", gafLocation);
	}
	
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		
	}
	
	@Test
	public void testLoadOntologiesAndGaf() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		pw.addIRIMapper(new CatalogXmlIRIMapper(catalogXml));
		final OWLGraphWrapper g = pw.parseToOWLGraph(goFile);
		g.mergeOntology(pw.parse(clFile));
		g.mergeOntology(pw.parse(uberonFile));
		g.mergeOntology(pw.parse(poFile));
		g.mergeOntology(pw.parse(soFile));
		g.mergeOntology(pw.parse(prFile));
		g.mergeOntology(pw.parse(chebiFile));
		g.mergeOntology(pw.parse("http://purl.obolibrary.org/obo/go/extensions/gorel.owl"));
		g.mergeOntology(pw.parse(TaxonTools.TAXON_PURL));
		
		EcoTools ecoTools = new EcoTools(pw);
		
		// taxon information
		ElkReasonerFactory factory = new ElkReasonerFactory();
		OWLReasoner taxonReasoner = factory.createReasoner(g.getSourceOntology());
		TaxonTools taxonTools = new TaxonTools(taxonReasoner, true);
		try {

			GafSolrDocumentLoaderIntegrationRunner.printMemoryStats();
			GafSolrDocumentLoaderIntegrationRunner.gc();
			GafSolrDocumentLoaderIntegrationRunner.printMemoryStats();

			ConfigManager configManager = new ConfigManager();
			configManager.add("src/test/resources/test-ont-config.yaml");
			FlexCollection c = new FlexCollection(configManager, g);

			GafSolrDocumentLoaderIntegrationRunner.printMemoryStats();
			GafSolrDocumentLoaderIntegrationRunner.gc();
			GafSolrDocumentLoaderIntegrationRunner.printMemoryStats();

			final StopWatch ontologyWatch = new StopWatch();
			ontologyWatch.start();
			final FlexSolrDocumentLoader ontologyLoader = new FlexSolrDocumentLoader((SolrClient)null, c) {

				@Override
				protected void addToServer(Collection<SolrInputDocument> docs)
						throws SolrServerException, IOException {
					solrCounter += docs.size();
					GafSolrDocumentLoaderIntegrationRunner.printMemoryStats();
					System.out.println("Cache size: "+g.getCurrentEdgesAdvancedCacheSize());
				}

			};
			ontologyLoader.load();
			ontologyWatch.stop();
			GafSolrDocumentLoaderIntegrationRunner.printMemoryStats();
			System.out.println("Loaded "+solrCounter+" ontology Solr docs in "+ontologyWatch);
			assertTrue(solrCounter > 0);

			GafSolrDocumentLoaderIntegrationRunner.printMemoryStats();
			GafSolrDocumentLoaderIntegrationRunner.gc();
			GafSolrDocumentLoaderIntegrationRunner.printMemoryStats();
			solrCounter = 0;

			GafSolrDocumentLoader gafLoader = new GafSolrDocumentLoader(null, 1000) {

				@Override
				protected void addToServer(Collection<SolrInputDocument> docs)
						throws SolrServerException, IOException {
					solrCounter += docs.size();
					GafSolrDocumentLoaderIntegrationRunner.printMemoryStats();
					System.out.println("Cache size: "+g.getCurrentEdgesAdvancedCacheSize());
				}

			};
			gafLoader.setEcoTools(ecoTools);
			gafLoader.setGraph(g);
			gafLoader.setTaxonTools(taxonTools);

			String gaf = new File(gafLocation, "gene_association.mgi.gz").getCanonicalPath();
			GafObjectsBuilder builder = new GafObjectsBuilder();
			GafDocument gafdoc = builder.buildDocument(gaf);
			gafLoader.setGafDocument(gafdoc);

			final StopWatch gafWatch = new StopWatch();
			gafWatch.start();
			gafLoader.load();
			gafWatch.stop();
			GafSolrDocumentLoaderIntegrationRunner.printMemoryStats();
			GafSolrDocumentLoaderIntegrationRunner.gc();
			GafSolrDocumentLoaderIntegrationRunner.printMemoryStats();

			assertTrue(solrCounter > 0);
		}
		finally {
			taxonTools.dispose();
		}
	}
}
