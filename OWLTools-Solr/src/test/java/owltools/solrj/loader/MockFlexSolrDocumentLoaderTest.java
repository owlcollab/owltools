package owltools.solrj.loader;

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLOntology;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import owltools.flex.FlexCollection;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;
import owltools.yaml.golrconfig.ConfigManager;

public class MockFlexSolrDocumentLoaderTest {

    private ConfigManager aconf = null;

    public List<Map<String, Object>> mockLoad(String path) throws Exception {
 
        aconf = new ConfigManager();
        aconf.add("src/test/resources/test-ont-config.yaml");

        ParserWrapper pw = new ParserWrapper();
        OWLOntology testOwl = pw.parse(new File(path).getCanonicalPath());
        final OWLGraphWrapper g = new OWLGraphWrapper(testOwl);
        FlexCollection flex = new FlexCollection(aconf, g);

        ElkReasonerFactory rf = new ElkReasonerFactory();

        MockFlexSolrDocumentLoader l = new MockFlexSolrDocumentLoader(flex);
        l.load();


        List<Map<String, Object>> docs = l.getDocumentCollection().getDocuments();

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        for (Map<String, Object> doc : docs) {
            String json = gson.toJson(doc);
            System.out.println(json);

        }    
        return docs;
    }
    
    @Test
    public void testMini() throws Exception {
        
        List<Map<String, Object>> docs = mockLoad("src/test/resources/not_bioentity_closure/mini-test.obo");
        assertEquals(10, docs.size());
        for (Map<String, Object> doc : docs) {
            assertTrue(checkDocForFilteredClasses(doc));
        }
 
    }
    
    @Test
    public void testFilterOwlNothing() throws Exception {
        
        List<Map<String, Object>> docs = mockLoad("src/test/resources/with-nothing.owl");
        assertEquals(2, docs.size());
        for (Map<String, Object> doc : docs) {
            assertTrue(checkDocForFilteredClasses(doc));
        }
 
    }
    
    // TODO - provide more extensive tests
    private boolean checkDocForFilteredClasses(Map<String, Object> doc) {
        Object tg = doc.get("topology_graph_json");
        System.out.println("TESTING: "+tg.toString());
        return !tg.toString().contains("Thing") && !tg.toString().contains("Nothing");
    }
}
