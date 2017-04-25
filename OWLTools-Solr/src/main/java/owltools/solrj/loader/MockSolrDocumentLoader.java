package owltools.solrj.loader;

import org.apache.solr.common.SolrInputDocument;

public interface MockSolrDocumentLoader {
    void add(SolrInputDocument doc);
    public MockSolrDocumentCollection getDocumentCollection();
}
