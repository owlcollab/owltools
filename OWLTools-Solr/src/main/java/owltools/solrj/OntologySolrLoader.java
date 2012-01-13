package owltools.solrj;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLProperty;

import owltools.graph.OWLGraphWrapper;

public class OntologySolrLoader extends AbstractSolrLoader {

	private static Logger LOG = Logger.getLogger(OntologySolrLoader.class);

	public OntologySolrLoader(String url) throws MalformedURLException {
		super(url);
	}
	
	@Override
	public void load() throws SolrServerException, IOException {
		// TODO Auto-generated method stub		
	}

	public void load(OWLGraphWrapper graph) throws SolrServerException, IOException {
		if( graph == null ){
			LOG.info("ERROR? OWLGraphWrapper graph is not apparently defined...");
		}else{
			for (OWLObject obj : graph.getAllOWLObjects()) {
				add(collect(obj, graph));
			}	
			addAllAndCommit();
		}
	}

	// Main wrapping for adding ontology documents to GOlr.
	// Also see GafSolrDocumentLoader for the others.
	public SolrInputDocument collect(OWLObject obj, OWLGraphWrapper graph) {

		SolrInputDocument cls_doc = new SolrInputDocument();
		
		cls_doc.addField("id", graph.getIdentifier(obj));
		cls_doc.addField("label", graph.getLabel(obj));

		// BUG: Apparently no "def" in real life...
		String obj_def = graph.getDef(obj);
		if( obj_def == null ){
			LOG.info("ERROR? No \"def\" for: " + graph.getIdentifier(obj));
		}else{
			cls_doc.addField("def", graph.getDef(obj));
		}
		
		if (obj instanceof OWLClass)
			 collectClass(cls_doc, (OWLClass)obj);
		else if (obj instanceof OWLIndividual)
			 collectIndividual(cls_doc, (OWLIndividual)obj);
		else if (obj instanceof OWLProperty)
			 collectProperty(cls_doc, (OWLProperty)obj);
		return cls_doc; 
	}

	private void collectProperty(SolrInputDocument d, OWLProperty obj) {
	}


	private void collectIndividual(SolrInputDocument d, OWLIndividual obj) {
	}


	private void collectClass(SolrInputDocument cls_doc, OWLClass c) {
		cls_doc.addField("document_category", "ontology_class");
	}

}
