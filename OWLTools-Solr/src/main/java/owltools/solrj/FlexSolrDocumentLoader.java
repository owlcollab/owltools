package owltools.solrj;

import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.log4j.Logger;
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
	
	public FlexSolrDocumentLoader(String url, FlexCollection c) throws MalformedURLException {
		super(url);
		collection = c;
	}
	
	@Override
	public void load() throws SolrServerException, IOException {

		//		//GOlrConfig config = getConfig();
		//		LOG.info("Trying to load with config: " + config.id);

		for( FlexDocument d : collection ){
			add(collect(d));
		}	
		addAllAndCommit();
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
