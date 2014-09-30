package owltools.solrj;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Properties;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.gaf.EcoTools;
import owltools.gaf.GafDocument;
import owltools.gaf.TaxonTools;
import owltools.gaf.parser.GafObjectsBuilder;
import owltools.graph.OWLGraphWrapper;
import owltools.io.CatalogXmlIRIMapper;
import owltools.io.ParserWrapper;

@SuppressWarnings("deprecation")
public class GafSolrDocumentLoaderIntegrationRunner {

	private static int solrCount = 0;
	private static OWLGraphWrapper graph;
	private static TaxonTools taxonTools = null;
	private static GafSolrDocumentLoader loader;
	
	private static String gafLocation = null;
	private static String catalogXmlFile = null;
	
	static void printMemoryStats() {
		int mb = 1024*1024;
        Runtime runtime = Runtime.getRuntime();
        System.out.println("Heap Used Memory:"
            + ((runtime.totalMemory() - runtime.freeMemory()) / mb)
            + " of max "
            + (runtime.maxMemory() / mb)
            + " [MB]");
	}
	
	static void gc() {
		Runtime runtime = Runtime.getRuntime();
		runtime.gc();
		runtime.gc();
	}
	
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		// check runtime for parameters
		Properties properties = System.getProperties();
		gafLocation = properties.getProperty(GafSolrDocumentLoaderIntegrationRunner.class.getSimpleName()+".gaf-folder");
		catalogXmlFile = properties.getProperty(GafSolrDocumentLoaderIntegrationRunner.class.getSimpleName()+".catalog-xml");
		assertNotNull("Did not find a catalog-xml file.", catalogXmlFile);
		assertNotNull("Did not find a gaf-folder file.", gafLocation);
		
		// setup solr loader
		loader = new GafSolrDocumentLoader(null, 1000) {

			@Override
			protected void addToServer(Collection<SolrInputDocument> docs)
					throws SolrServerException, IOException {
				solrCount += docs.size();
				GafSolrDocumentLoaderIntegrationRunner.printMemoryStats();
				System.out.println("Cache size: "+graph.getCurrentEdgesAdvancedCacheSize());
			}
			
		};
		ParserWrapper pw = new ParserWrapper();
		pw.addIRIMapper(new CatalogXmlIRIMapper(catalogXmlFile));
		// ontology
		graph = new OWLGraphWrapper(pw.parse("http://purl.obolibrary.org/obo/go/extensions/go-plus.owl"));
		graph.mergeOntology(pw.parse("http://purl.obolibrary.org/obo/go/extensions/gorel.owl"));
		loader.setGraph(graph);
		
		// eco
		EcoTools ecoTools = new EcoTools(pw);
		loader.setEcoTools(ecoTools);
		
		// taxon information
		OWLOntology taxonOwl = pw.parseOWL(TaxonTools.TAXON_PURL);
		ElkReasonerFactory factory = new ElkReasonerFactory();
		OWLReasoner taxonReasoner = factory.createReasoner(taxonOwl);
		taxonTools = new TaxonTools(taxonReasoner, true);
		loader.setTaxonTools(taxonTools);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		if (taxonTools != null) {
			taxonTools.dispose();
		}
	}

	@Test
	public void test() throws Exception {
		assertNotNull(graph);
		assertNotNull(loader);
		assertNotNull(taxonTools);
		
		String gaf = new File(gafLocation, "gene_association.mgi.gz").getCanonicalPath();
		
		gc();
		printMemoryStats();
		
		for(int i=0;i<10;i++) {
			System.out.println("Iteration start: "+(i+1));
			System.out.println("Loading GAF: "+gaf);

			GafObjectsBuilder builder = new GafObjectsBuilder();
			GafDocument gafdoc = builder.buildDocument(gaf);
			loader.setGafDocument(gafdoc);
			loader.load();

			System.out.println("Loaded solr documents: "+solrCount);
			System.out.println("Iteration end: "+(i+1));
			printMemoryStats();
			gc();
			printMemoryStats();
		}
		
		gc();
		printMemoryStats();
	}

}
