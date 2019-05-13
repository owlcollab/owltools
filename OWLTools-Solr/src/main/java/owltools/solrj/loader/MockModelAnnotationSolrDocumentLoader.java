package owltools.solrj.loader;

import java.net.MalformedURLException;
import java.util.Set;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.solrj.ModelAnnotationSolrDocumentLoader;

public class MockModelAnnotationSolrDocumentLoader extends ModelAnnotationSolrDocumentLoader implements MockSolrDocumentLoader {
    
    
    public MockModelAnnotationSolrDocumentLoader(SolrServer server,
            OWLOntology model, OWLReasoner r, String modelUrl,
            Set<String> modelFilter, boolean skipDeprecatedModels,
            boolean skipTemplateModels) {
        super(server, model, r, modelUrl, modelFilter, skipDeprecatedModels, skipTemplateModels);
        // TODO Auto-generated constructor stub
    }

    public MockModelAnnotationSolrDocumentLoader(String golrUrl,
            OWLOntology model, OWLReasoner r, String modelUrl,
            Set<String> modelFilter, boolean skipDeprecatedModels,
            boolean skipTemplateModels) throws MalformedURLException {
        super((SolrServer)null, model, r, modelUrl, modelFilter, skipDeprecatedModels, skipTemplateModels);
        // TODO Auto-generated constructor stub
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
