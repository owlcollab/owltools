package owltools.solrj;

import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;

/**
 * A hard-wired optimize command for after we've done everything that we wanted to load.
 * May not need to be used in real life.
 */
public class OptimizeSolrDocumentLoader extends AbstractSolrLoader {

	private static Logger LOG = Logger.getLogger(OptimizeSolrDocumentLoader.class);

	public OptimizeSolrDocumentLoader(String url) throws MalformedURLException {
		super(url);
	}

	@Override
	public void load() throws SolrServerException, IOException {

		LOG.info("Optimizing index. This may take a while...");
		server.optimize();
		LOG.info("Done.");
	}
}
