package owltools.solrj;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collection;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLProperty;

import owltools.graph.OWLGraphWrapper;

public class OntologySolrLoader extends AbstractSolrLoader {

	
	public OntologySolrLoader(String url) throws MalformedURLException {
		super(url);
	}
	

	@Override
	public void load() throws SolrServerException, IOException {
		for (OWLObject obj : graph.getAllOWLObjects()) {
			add(collect(obj));
		}
		addAllAndCommit();
	}

	public SolrInputDocument collect(OWLObject obj) {
		SolrInputDocument d = new SolrInputDocument();
		d.addField("id", graph.getIdentifier(obj));
		d.addField("label", graph.getLabel(obj));
		d.addField("def", graph.getDef(obj));
		if (obj instanceof OWLClass)
			 collectClass(d, (OWLClass)obj);
		else if (obj instanceof OWLIndividual)
			 collectIndividual(d, (OWLIndividual)obj);
		else if (obj instanceof OWLProperty)
			 collectProperty(d, (OWLProperty)obj);
		return d; 
	}

	private void collectProperty(SolrInputDocument d, OWLProperty obj) {
	}



	private void collectIndividual(SolrInputDocument d, OWLIndividual obj) {
	}



	private void collectClass(SolrInputDocument d, OWLClass c) {
	}

	
	

}
