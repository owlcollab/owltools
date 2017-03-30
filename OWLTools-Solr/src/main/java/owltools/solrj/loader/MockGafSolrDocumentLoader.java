package owltools.solrj.loader;

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.common.SolrInputDocument;

import owltools.solrj.GafSolrDocumentLoader;

public class MockGafSolrDocumentLoader extends GafSolrDocumentLoader {
    
    public MockGafSolrDocumentLoader() {
        super(null, 100);
    }
    
    final List<SolrInputDocument> documents = new ArrayList<SolrInputDocument>();
    @Override
    protected void add(SolrInputDocument doc) {
        documents.add(doc);
    }
    
    /**
     * @return the result
     */
    public List<SolrInputDocument> getDocuments() {
        return documents;
    }
    
    
}
