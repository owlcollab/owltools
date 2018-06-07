package owltools.solrj.loader;

import java.net.MalformedURLException;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.common.SolrInputDocument;

import owltools.flex.FlexCollection;
import owltools.solrj.FlexSolrDocumentLoader;

public class MockFlexSolrDocumentLoader extends FlexSolrDocumentLoader implements MockSolrDocumentLoader {
    
    private static Logger LOG = Logger.getLogger(MockFlexSolrDocumentLoader.class);

    public MockFlexSolrDocumentLoader(FlexCollection c) throws MalformedURLException {
        super((SolrServer)null, c);
    }
   
    final MockSolrDocumentCollection documentCollection = new MockSolrDocumentCollection();
    @Override
    public void add(SolrInputDocument doc) {
        LOG.info("Adding: "+doc);
        documentCollection.add(doc);
    }
    
    /**
     * @return the result
     */
    public MockSolrDocumentCollection getDocumentCollection() {
        return documentCollection;
    }
    
   
}
