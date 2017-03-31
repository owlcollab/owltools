package owltools.solrj.loader;

import org.apache.solr.common.SolrInputDocument;

import owltools.solrj.GafSolrDocumentLoader;

public class MockGafSolrDocumentLoader extends GafSolrDocumentLoader implements MockSolrDocumentLoader {
    
    public MockGafSolrDocumentLoader() {
        super(null, 100);
    }
    
    final MockSolrDocumentCollection documentCollection = new MockSolrDocumentCollection();
    @Override
    protected void add(SolrInputDocument doc) {
        documentCollection.add(doc);
    }
    
    /**
     * @return the result
     */
    public MockSolrDocumentCollection getDocumentCollection() {
        return documentCollection;
    }
    
   
}
