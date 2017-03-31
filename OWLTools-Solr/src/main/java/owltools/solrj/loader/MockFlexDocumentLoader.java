package owltools.solrj.loader;

import java.net.MalformedURLException;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.common.SolrInputDocument;

import owltools.flex.FlexCollection;
import owltools.solrj.FlexSolrDocumentLoader;

public class MockFlexDocumentLoader extends FlexSolrDocumentLoader implements MockSolrDocumentLoader {
    
    public MockFlexDocumentLoader(FlexCollection flex) throws MalformedURLException {
        super((SolrServer)null, flex);
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
