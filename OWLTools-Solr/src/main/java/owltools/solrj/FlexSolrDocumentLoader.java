package owltools.solrj;

import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;

import owltools.flex.FlexCollection;
import owltools.flex.FlexDocument;
import owltools.flex.FlexLine;

/**
 * A slightly generic class for loading a Solr index compatible with the constraints defined by a BBOP-JS Solr index.
 * The load is entirely defined by a YAML file and functions in the OWLGraphWrapper {@link owltools.graph.OWLGraphWrapper}.
 */
public class FlexSolrDocumentLoader extends AbstractSolrLoader {

	private static Logger LOG = Logger.getLogger(FlexSolrDocumentLoader.class);
	private FlexCollection collection = null;
	int doc_limit_trigger = 1000; // the number of documents to add before pushing out to solr
	int current_doc_number;

	public FlexSolrDocumentLoader(String url, FlexCollection c) throws MalformedURLException {
		super(url);
		collection = c;
		current_doc_number = 0;
	}
	
	protected FlexSolrDocumentLoader(SolrServer server, FlexCollection c) {
		super(server);
		collection = c;
		current_doc_number = 0;
	}
	
	@Override
	public void load() throws SolrServerException, IOException {

		for( FlexDocument d : collection ){
			add(collect(d));
			
			// Incremental commits.
			current_doc_number++;
			if( current_doc_number % doc_limit_trigger == 0 ){
				LOG.info("Processed " + doc_limit_trigger + " flex ontology docs at " + current_doc_number + " and committing...");
				incrementalAddAndCommit();
			}
		}	

		// Get the remainder of the docs in.
		LOG.info("Doing clean-up (final) commit at " + current_doc_number + " ontology documents...");
		addAllAndCommit();
		LOG.info("Done.");
	}
	
	/**
	 * Take args and add it index (no commits)
	 * Main wrapping for adding ontology documents to GOlr.
	 * Also see GafSolrDocumentLoader for the others.
	 *
	 * @param f
	 * @return an input doc for add()
	 */
	public SolrInputDocument collect(FlexDocument f) {

		SolrInputDocument cls_doc = new SolrInputDocument();

		for( FlexLine l : f ){
			String fieldName = l.field();
			for( String val : l.values() ){
				cls_doc.addField(fieldName, val);
			}
		}
		
		return cls_doc;
	}
}
