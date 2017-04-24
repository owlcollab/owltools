package owltools.solrj.loader;

import java.net.MalformedURLException;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.common.SolrInputDocument;

import owltools.flex.FlexCollection;
import owltools.solrj.FlexSolrDocumentLoader;

public class MockFlexSolrDocumentLoader extends FlexSolrDocumentLoader implements MockSolrDocumentLoader {
    
    public MockFlexSolrDocumentLoader(FlexCollection c) throws MalformedURLException {
        super((SolrServer)null, c);
    }
   
    final MockSolrDocumentCollection documentCollection = new MockSolrDocumentCollection();
    @Override
    public void add(SolrInputDocument doc) {
        documentCollection.add(doc);
    }
    
    /**
     * @return the result
     */
    public MockSolrDocumentCollection getDocumentCollection() {
        return documentCollection;
    }
    
   
}
