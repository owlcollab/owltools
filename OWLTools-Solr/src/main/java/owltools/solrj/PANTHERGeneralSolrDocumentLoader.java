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
 * A hard-wired loader for the general-config.yaml profile.
 */
public class PANTHERGeneralSolrDocumentLoader extends AbstractSolrLoader {

	private static Logger LOG = Logger.getLogger(PANTHERGeneralSolrDocumentLoader.class);

	PANTHERForest pset = null;

	GafDocument gafDocument;
	int doc_limit_trigger = 100; // the number of documents to add before pushing out to solr
	int current_doc_number;
		
	public PANTHERGeneralSolrDocumentLoader(String url) throws MalformedURLException {
		super(url);
		current_doc_number = 0;
	}

	public void setPANTHERSet(PANTHERForest inPSet) {
		this.pset = inPSet;
	}

	@Override
	public void load() throws SolrServerException, IOException {

		LOG.info("Loading PANTHER (general) documents (" + pset.getTreeIDSet().size() + " total)...");

		// Cycle through all of the trees in the forest.
		for( String tree_id : pset.getTreeIDSet() ){
			PANTHERTree ptree = pset.getTreeByID(tree_id);
			if( ptree != null ){

				//LOG.info("Loading PANTHER tree: " + ptree.getTreeID());

				// Now repeat some of the same to help populate the "general" index.
				SolrInputDocument general_doc = new SolrInputDocument();
				// Watch out for "id" collision!
				general_doc.addField("id", "general_family_" + ptree.getPANTHERID());
				general_doc.addField("entity", ptree.getPANTHERID());
				general_doc.addField("entity_label", ptree.getTreeLabel());
				general_doc.addField("document_category", "general");
				general_doc.addField("category", "family");
				//general_doc.addField("general_blob", "");
				add(general_doc);
				
				// Incremental commits.
				current_doc_number++;
				if( current_doc_number % doc_limit_trigger == 0 ){
					LOG.info("Processed " + doc_limit_trigger + " trees at " + current_doc_number + " and committing...");
					incrementalAddAndCommit();
				}
			}
		}
		
		// Get the remainder of the docs in.
		LOG.info("Doing clean-up (final) commit at " + current_doc_number + " trees...");
		addAllAndCommit();
		LOG.info("Done.");
	}
}
