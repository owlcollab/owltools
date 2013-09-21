package owltools.solrj;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.geneontology.lego.model.LegoNode;
import org.geneontology.lego.model.LegoTools;
import org.geneontology.lego.model.LegoTools.UnExpectedStructureException;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
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
 * A very specific class for the specific use case of loading in complex annotations from owl.
 */
public class ComplexAnnotationSolrDocumentLoader extends AbstractSolrLoader {

	private static Logger LOG = Logger.getLogger(ComplexAnnotationSolrDocumentLoader.class);
	int doc_limit_trigger = 1000; // the number of documents to add before pushing out to solr
	int current_doc_number;
	
	
	public ComplexAnnotationSolrDocumentLoader(String url, OWLGraphWrapper g) throws MalformedURLException {
		super(url);
		setGraph(g);
		current_doc_number = 0;
	}
	
	@Override
	public void load() throws SolrServerException, IOException {
		
		LOG.info("Loading complex annotation documents...");
		
		if( graph == null ){
			LOG.info("ERROR? OWLGraphWrapper graph is not apparently defined...");
		}else{

			LegoTools lg = new LegoTools(graph, graph.getReasoner());
			Set<OWLNamedIndividual> individuals = graph.getSourceOntology().getIndividualsInSignature();
			try {

				for( LegoNode ln : lg.createLegoNodes(individuals) ){

					SolrInputDocument doc = collect(ln);

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
							
			} catch (UnExpectedStructureException e) {
				LOG.info("ERROR? Found an unexpected structure apparently!");
				e.printStackTrace();
				LOG.info("But life moves on...");
			}
			
			// Get the remainder of the docs in.
			LOG.info("Doing clean-up (final) commit at " + current_doc_number + " complex annotation documents...");
			addAllAndCommit();
			LOG.info("Done.");
		}
	}
	
	
	/**
	 * Take args and add it index (no commits)
	 * Main wrapping for adding complex annotation documents to GOlr.
	 * @param ln 
	 *
	 * @return an input doc for add()
	 */
	public SolrInputDocument collect(LegoNode ln) {

		SolrInputDocument ca_doc = new SolrInputDocument();

		// We'll be using the sam is_a-part_of a lot.
		ArrayList<String> isap = new ArrayList<String>();
		isap.add("BFO:0000050");

		ca_doc.addField("document_category", "complex_annotation");
		
		// TODO: This sucks--can't do multipass--but live with it for now for testing.
		//ca_doc.addField("id", "???");
		ca_doc.addField("id", current_doc_number);

		// annotation_unit
		// annotation_unit_label
		ln.getIndividual().getIRI().toString();
		ln.getIndividual().getIRI().toString(); // hold until somethin happens here
		
		// TODO: annotation_group(_label)
		// Will need higher-order information.
		
		// bioentity
		// bioentity_label
		OWLClass oc = ln.getActiveEntity();
		String oc_id = graph.getIdentifier(oc);
		String oc_lbl = graph.getLabel(oc);
		ca_doc.addField("bioentity", oc_id);
		ca_doc.addField("bioentity_label", oc_lbl);

		// (do usual)
		
		// TODO: enabled_by(_label)
		// Squeeze label from expression collection.
		Collection<OWLClassExpression> unknowns = ln.getUnknowns();
		
		// TODO: function_class|process_class(_label)
		// function_class_closure|process_class_closure(_label)
		// function_class_closure_map|process_class_closure_map
		OWLClassExpression oce = ln.getType();
		if( oce.isAnonymous() == false ){
			
			// TODO: ID and label.
			
			// Get ready for the isa-part_of closure assembly.
			OWLClass ln_oc = oce.asOWLClass();
			// Either switch f/p--not both.
			if( ln.isBp() ){
				addClosureToDoc(isap, "process_class_closure", "process_class_closure_label", "process_class_closure_map", ln_oc, ca_doc);
			}else{
				addClosureToDoc(isap, "function_class_closure", "function_class_closure_label", "function_class_closure_map", ln_oc, ca_doc);				
			}
			
		}else{

			// TODO: ID and label.
			
			// TODO: Closure.
			Set<OWLClass> cls_for_closure = oce.getClassesInSignature();
			// TODO: Iterate over the collection and build up the ids, labels, and closure map.
		}
		// (do usual -- take hints from json thingy pretty print)

		// TODO: location_list(_label)
		// TODO: location_list_map
		// TODO: location_list_closure(_label)
		// TODO: location_list_closure_map

		// Caches for location_list.
		Set<String> locIDSet = new HashSet<String>();
		Set<String> locLabelSet = new HashSet<String>();
		Map<String, String> locMap = new HashMap<String, String>();

		// Caches for location_list_closure.
		Set<String> locClosureIDSet = new HashSet<String>();
		Set<String> locClosureLabelSet = new HashSet<String>();
		Map<String, String> locClosureMap = new HashMap<String, String>();

		// Collect painfully class by class.
		Collection<OWLClassExpression> cell_loc = ln.getCellularLocation();
		for( OWLClassExpression cell_loc_exp : cell_loc ){
			OWLClass cell_loc_cls = cell_loc_exp.asOWLClass();
			
			// First, the trivial transfer to the final set.
			String loc_id = graph.getIdentifier(cell_loc_cls);
			String loc_lbl = graph.getLabel(cell_loc_cls);
			locIDSet.add(loc_id);
			locLabelSet.add(loc_lbl);
			locMap.put(loc_id, loc_lbl);
			
			// Add closures to cache sets
			List<String> loc_id_closure = graph.getRelationIDClosure(cell_loc_cls, isap);
			locClosureIDSet.addAll(loc_id_closure);
			List<String> loc_label_closure = graph.getRelationLabelClosure(cell_loc_cls, isap);
			locClosureLabelSet.addAll(loc_label_closure);
			Map<String, String> loc_closure_map = graph.getRelationClosureMap(cell_loc_cls, isap);
			locClosureMap.putAll(loc_closure_map);
		}
		
		// Process all collected caches into the document fields.
		ca_doc.addField("location_list", locIDSet);
		ca_doc.addField("location_list_label", locLabelSet);
		ca_doc.addField("location_list_closure", locClosureIDSet);
		ca_doc.addField("location_list_closure_label", locClosureLabelSet);

		// Compile location maps to JSON.
		if( ! locMap.isEmpty() ){
			ca_doc.addField("location_list_map", gson.toJson(locMap));
		}
		if( ! locClosureMap.isEmpty() ){
			ca_doc.addField("location_list_closure_map", gson.toJson(locClosureMap));
		}
		
		// TODO: topology_graph_json
		// Will need higher-order information.
		
		// LATER: panther_family(_label)
		// LATER: taxon(_label)
		// LATER: taxon_closure(_label)
		// LATER: taxon_closure(_map)
		// LATER: owl_blob_json

		return ca_doc;
	}

	/*	
	 * Add specified closure of OWLObject to the doc.
	 */
	private Map<String, String> addClosureToDoc(ArrayList<String> relations, String closureName, String closureNameLabel, String closureMap,
			OWLObject cls, SolrInputDocument solr_doc){
		
		// Add closures to doc; label and id.
		List<String> idClosure = graph.getRelationIDClosure(cls, relations);
		List<String> labelClosure = graph.getRelationLabelClosure(cls, relations);
		solr_doc.addField(closureName, idClosure);
		solr_doc.addField(closureNameLabel, labelClosure);
		for( String tid : idClosure){
			addFieldUnique(solr_doc, closureName, tid);
		}

		// Compile closure maps to JSON.
		Map<String, String> cmap = graph.getRelationClosureMap(cls, relations);
		if( ! cmap.isEmpty() ){
			String jsonized_cmap = gson.toJson(cmap);
			solr_doc.addField(closureMap, jsonized_cmap);
		}
		
		return cmap;
	}

}
