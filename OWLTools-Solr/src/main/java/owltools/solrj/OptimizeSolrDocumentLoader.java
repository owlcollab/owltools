package owltools.solrj;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLPropertyExpression;

import owltools.gaf.Bioentity;
import owltools.gaf.EcoTools;
import owltools.gaf.ExtensionExpression;
import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.TaxonTools;
import owltools.gaf.WithInfo;
import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLQuantifiedProperty;
import owltools.panther.PANTHERForest;
import owltools.panther.PANTHERTree;

import com.google.gson.*;

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
