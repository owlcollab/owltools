package owltools.solrj;

import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;

import owltools.gaf.GafDocument;
import owltools.panther.PANTHERForest;
import owltools.panther.PANTHERTree;

/**
 * A hard-wired loader for the family-config.yaml profile.
 */
public class PANTHERSolrDocumentLoader extends AbstractSolrLoader {

	private static Logger LOG = Logger.getLogger(PANTHERSolrDocumentLoader.class);

	PANTHERForest pset = null;

	GafDocument gafDocument;
	int doc_limit_trigger = 100; // the number of documents to add before pushing out to solr
	int current_doc_number;
		
	public PANTHERSolrDocumentLoader(String url) throws MalformedURLException {
		super(url);
		current_doc_number = 0;
	}

	public void setPANTHERSet(PANTHERForest inPSet) {
		this.pset = inPSet;
	}

	@Override
	public void load() throws SolrServerException, IOException {

		LOG.info("Loading PANTHER documents (" + pset.getTreeIDSet().size() + " total)...");

		// Cycle through all of the trees in the forest.
		for( String tree_id : pset.getTreeIDSet() ){
			PANTHERTree ptree = pset.getTreeByID(tree_id);
			if( ptree != null ){

				//LOG.info("Loading PANTHER tree: " + ptree.getTreeID());

				// Create the panther family document.
				SolrInputDocument family_doc = new SolrInputDocument();

				// Base information.
				family_doc.addField("document_category", "family");
				family_doc.addField("id", ptree.getPANTHERID());
				family_doc.addField("panther_family", ptree.getPANTHERID());
				family_doc.addField("panther_family_label", ptree.getTreeLabel());

				// Add in the bioentities and maps.
				family_doc.addField("bioentity_list", ptree.getAssociatedGeneProductIDs());
				family_doc.addField("bioentity_list_label", ptree.getAssociatedGeneProductLabels());
				family_doc.addField("bioentity_list_map", ptree.getAssociatedGeneProductJSONMap());
						
				// Okay, push into loader.
				add(family_doc);
								
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
