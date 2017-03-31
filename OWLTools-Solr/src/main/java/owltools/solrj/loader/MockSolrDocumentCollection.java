package owltools.solrj.loader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrInputDocument;

public class MockSolrDocumentCollection {

    final List<Map<String,Object>> documents = new ArrayList<>();
    public void add(SolrInputDocument doc) {
        documents.add(convert(doc));
    }
    
    
    
    /**
     * @return the documents
     */
    public List<Map<String, Object>> getDocuments() {
        return documents;
    }



    public boolean isEmpty() {
        return documents.size() == 0;
    }
    
    private Map<String,Object> convert(SolrInputDocument doc) {
        Map<String,Object> m = new HashMap<>();
        for (String f : doc.getFieldNames()) {
            Collection<Object> vs = doc.getFieldValues(f);
            m.put(f, vs);
        }
        return m;
    }

}
