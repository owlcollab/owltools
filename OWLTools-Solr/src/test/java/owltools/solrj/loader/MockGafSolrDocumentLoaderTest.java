package owltools.solrj.loader;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrInputDocument;
import org.junit.Test;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import owltools.gaf.EcoTools;
import owltools.gaf.GafDocument;
import owltools.gaf.TaxonTools;
import owltools.gaf.parser.GafObjectsBuilder;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;
import owltools.solrj.GafSolrDocumentLoader;

public class MockGafSolrDocumentLoaderTest {

    @Test
    public void test() throws Exception {
        GafObjectsBuilder b = new GafObjectsBuilder();
        final GafDocument gaf = b.buildDocument("src/test/resources/not_bioentity_closure/mini-test.gaf");
        
        ParserWrapper pw = new ParserWrapper();
        OWLOntology testOwl = pw.parse(new File("src/test/resources/not_bioentity_closure/mini-test.obo").getCanonicalPath());
        final OWLGraphWrapper g = new OWLGraphWrapper(testOwl);
        
        OWLOntology slimOWL = pw.parse(TaxonTools.TAXON_PURL);
        OWLReasonerFactory rf = new ReasonerFactory();
        
        MockGafSolrDocumentLoader l = new MockGafSolrDocumentLoader();
         
        OWLReasoner r = null;
        try {
            r = rf.createReasoner(slimOWL);

            l.setGafDocument(gaf);
            l.setGraph(g);
            l.setEcoTools(new EcoTools(pw));
            l.setTaxonTools(new TaxonTools(r, true));
            l.load();
        }
        finally {
            if (r != null) {
                r.dispose();
            }
        }
        
        List<Map<String, Object>> docs = l.getDocumentCollection().getDocuments();
        assertFalse(docs.size() == 0);
        
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        for (Map<String, Object> doc : docs) {
            String json = gson.toJson(doc);
            System.out.println(json);
            
        }
    }

}
