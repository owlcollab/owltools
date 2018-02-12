package owltools.solrj;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.semanticweb.owlapi.model.OWLObject;

import com.google.gson.Gson;

import owltools.graph.OWLGraphWrapper;

/**
 * Root class for loading ontologies or ontology-related data into a Solr instance
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

	// We'll need this for serializing in various places.
	protected Gson gson = new Gson();

	
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
		this(createDefaultServer(url));
		
	}
	
	public static final SolrServer createDefaultServer(String url) throws MalformedURLException {
		LOG.info("Server at: " + url);
		return new CommonsHttpSolrServer(url);
	}

	/**
	 * 
	 * 
	 * @param d
	 * @param field
	 * @param val
	 * @return true, if value was added to document
	 */
	public boolean addFieldUnique(SolrInputDocument d, String field, String val) {
		if (val == null)
			return false;
		Collection<Object> vals = d.getFieldValues(field);
		if (vals != null && vals.contains(val))
			return false;
		d.addField(field, val);
		return true;
	}

	/**
	 * Resolve the list of id to a label and add it to the doc.
	 * 
	 * @param d
	 * @param field
	 * @param id
	 * @return label or null
	 */
	public String addLabelField(SolrInputDocument d, String field, String id) {
		String retstr = null;
		
		OWLObject obj = graph.getOWLObjectByIdentifier(id);
		if (obj == null)
			return retstr;

		String label = graph.getLabel(obj);
		if (label != null)
			d.addField(field, label);
		
		return label;
	}
	
	/**
	 * Resolve the list of ids to labels and add them to the doc.
	 * 
	 * @param d
	 * @param field
	 * @param ids
	 * @return labels
	 */
	public List<String> addLabelFields(SolrInputDocument d, String field, List<String> ids) {

		List<String> labelAccumu = new ArrayList<String>();
		
		for( String id : ids ){
			OWLObject obj = graph.getOWLObjectByIdentifier(id);
			if (obj != null){
				String label = graph.getLabel(obj);
				if (label != null){
					labelAccumu.add(label);
				}
			}
		}
		
		if( ! labelAccumu.isEmpty() ){
			d.addField(field, labelAccumu);
		}
		
		return labelAccumu;
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
		if( docs.isEmpty() ){
			LOG.warn("Odd: apparently no documents to add?");
		}else{
			LOG.info("adding all docs...");
			addToServer(docs);
			LOG.info("docs committed");
		}
		docs.clear();
	}

	protected void incrementalAddAndCommit() throws SolrServerException, IOException {
		if( docs.isEmpty() ){
			LOG.warn("Odd: apparently no documents to add?");
		}else{
			addToServer(docs);
		}
		docs.clear();
	}

	protected void addToServer(Collection<SolrInputDocument> docs) throws SolrServerException, IOException {
		server.add(docs);
		server.commit();
	}
}
