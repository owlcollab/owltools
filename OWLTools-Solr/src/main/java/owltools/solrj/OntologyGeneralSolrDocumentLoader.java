package owltools.solrj;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;
import owltools.graph.OWLGraphWrapper;

/**
 * A highly hard-wired class to specifically load the ontology graph into a set of hard-wired fields for the
 * AmiGO 2/GOlr general-config.yaml schema.
 */
public class OntologyGeneralSolrDocumentLoader extends AbstractSolrLoader {

	private static Logger LOG = Logger.getLogger(OntologyGeneralSolrDocumentLoader.class);
	int doc_limit_trigger = 1000; // the number of documents to add before pushing out to solr
	int current_doc_number;

	public OntologyGeneralSolrDocumentLoader(String url, OWLGraphWrapper graph) throws MalformedURLException {
		super(url);
		setGraph(graph);
		current_doc_number = 0;
	}
	
	@Override
	public void load() throws SolrServerException, IOException {

		LOG.info("Loading general ontology documents...");
		
		if( graph == null ){
			LOG.info("ERROR? OWLGraphWrapper graph is not apparently defined...");
		}else{
			for (OWLObject obj : graph.getAllOWLObjects()) {

				SolrInputDocument doc = collect(obj, graph);
				if( doc != null ){
					add(doc);
				
					// Incremental commits.
					current_doc_number++;
					if( current_doc_number % doc_limit_trigger == 0 ){
						LOG.info("Processed " + doc_limit_trigger + " general ontology docs at " + current_doc_number + " and committing...");
						incrementalAddAndCommit();
					}
				}
			}
			
			// Get the remainder of the docs in.
			LOG.info("Doing clean-up (final) commit at " + current_doc_number + " general ontology documents...");
			addAllAndCommit();
			LOG.info("Done.");
		}
	}

	// Main wrapping for adding ontology documents to GOlr.
	public SolrInputDocument collect(OWLObject obj, OWLGraphWrapper graph) {

		//
		if (obj instanceof OWLClass)
			 return collectClass(graph, (OWLClass)obj);
		else
			return null;
	}

	// Things special for ontology_class.
	private SolrInputDocument collectClass(OWLGraphWrapper graph, OWLClass c) {

		String def = graph.getDef(c);
		String com = graph.getComment(c);
		String syns = StringUtils.join(graph.getOBOSynonymStrings(c, null), " ");
		String subs = StringUtils.join(graph.getSubsets(c), " ");
		
		ArrayList<String> alt_pps = new ArrayList<String>();
		alt_pps.add("alt_id");
		String alts = StringUtils.join(graph.getAnnotationPropertyValues(c, alt_pps), " ");

		ArrayList<String> rep_pps = new ArrayList<String>();
		rep_pps.add("replaced_by");
		String reps = StringUtils.join(graph.getAnnotationPropertyValues(c, rep_pps), " ");

		ArrayList<String> con_pps = new ArrayList<String>();
		con_pps.add("consider");
		String cons = StringUtils.join(graph.getAnnotationPropertyValues(c, con_pps), " ");

		// Okay, pull out all of the variations on the ID for what people might expect in the ontology.
		String gid = graph.getIdentifier(c);
		String gid_no_namespace = StringUtils.substringAfter(gid, ":");
		String gid_no_namespace_or_leading_zeros = StringUtils.stripStart(gid_no_namespace, "0");
		
		// All together now.
		ArrayList<String> all = new ArrayList<String>();
		all.add(gid_no_namespace);
		all.add(gid_no_namespace_or_leading_zeros);
		all.add(def);
		all.add(com);
		all.add(syns);
		all.add(subs);
		all.add(alts);
		all.add(reps);
		all.add(cons);

		// Watch out for "id" collision!
		SolrInputDocument general_doc = new SolrInputDocument();
		general_doc.addField("id", "general_ontology_class_" + gid);
		general_doc.addField("entity", graph.getIdentifier(c));
		general_doc.addField("entity_label", graph.getLabel(c));
		general_doc.addField("document_category", "general");
		general_doc.addField("category", "ontology_class");
		general_doc.addField("general_blob", StringUtils.join(all, " "));
		
		return general_doc;
	}
}
