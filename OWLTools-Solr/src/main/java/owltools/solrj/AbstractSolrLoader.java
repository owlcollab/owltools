package owltools.solrj;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.log4j.Logger;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.common.SolrInputDocument;

import owltools.graph.OWLGraphWrapper;

/**
 * root class for loading ontologies or ontology-related data into a solr server
 * using web services and the solrj library
 * 
 * @author cjm
 *
 */
public abstract class AbstractSolrLoader {
	
	private static Logger LOG = Logger.getLogger(AbstractSolrLoader.class);
	
    protected SolrServer server;
    
    private Collection<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();

	protected OWLGraphWrapper graph;

	public OWLGraphWrapper getGraph() {
		return graph;
	}

	public void setGraph(OWLGraphWrapper graph) {
		this.graph = graph;
	}

	public AbstractSolrLoader(SolrServer server) {
		super();
		this.server = server;
	}

	public AbstractSolrLoader(String url) throws MalformedURLException {
		super();
		LOG.info("Server at: " + url);
		this.server = new CommonsHttpSolrServer(url);
	}

	public abstract void load() throws SolrServerException, IOException;
	
	protected void add(SolrInputDocument doc) {
		if (doc != null)
			docs.add(doc);
	}
	protected void addAll(Collection<SolrInputDocument> dl) {
		docs.addAll(dl);
	}

	protected void addAllAndCommit() throws SolrServerException, IOException {
		LOG.info("adding all docs...");
		server.add(docs);
		LOG.info("committing docs...");
		server.commit();
		LOG.info("docs committed");
	}

	protected void incrementalAddAndCommit() throws SolrServerException, IOException {
		//LOG.info("adding some docs...");
		server.add(docs);
		//LOG.info("committing some docs...");
		server.commit();
		//LOG.info("some docs committed");
	    docs.clear();
		//LOG.info("added, committed, and purged some docs");
	}
}
