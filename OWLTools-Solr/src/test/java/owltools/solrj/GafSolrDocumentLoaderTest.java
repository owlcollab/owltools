package owltools.solrj;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Collection;

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

public class GafSolrDocumentLoaderTest {

	private static int solrCount = 0;
	private static OWLGraphWrapper graph;
	private static TaxonTools taxonTools = null;
	private static GafSolrDocumentLoader loader;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		loader = new GafSolrDocumentLoader(null, 1000) {

			@Override
			protected void addToServer(Collection<SolrInputDocument> docs)
					throws SolrServerException, IOException {
				solrCount += docs.size();
			}
			
		};
		ParserWrapper pw = new ParserWrapper();
		pw.addIRIMapper(new CatalogXmlIRIMapper("../OWLTools-Annotation/src/test/resources/rules/ontology/extensions/catalog-v001.xml"));
		// ontology
		graph = new OWLGraphWrapper(pw.parse("http://purl.obolibrary.org/obo/go.owl"));
		graph.mergeOntology(pw.parse("http://purl.obolibrary.org/obo/go/extensions/gorel.owl"));
		OWLOntology taxonOwl = pw.parseOWL(TaxonTools.TAXON_PURL);
		graph.mergeOntology(taxonOwl);
		loader.setGraph(graph);
		
		// eco
		EcoTools ecoTools = new EcoTools(pw);
		loader.setEcoTools(ecoTools);
		
		// taxon information
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
		
		String gaf = "../OWLTools-Annotation/src/test/resources/gene_association.goa_human.gz";

		GafObjectsBuilder builder = new GafObjectsBuilder();
		GafDocument gafdoc = builder.buildDocument(gaf);
		loader.setGafDocument(gafdoc);
		loader.load();

		assertTrue(solrCount > 0);
	}

}
