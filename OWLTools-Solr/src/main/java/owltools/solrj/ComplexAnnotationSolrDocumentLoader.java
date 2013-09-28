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
//import org.geneontology.lego.json.LegoShuntGraphTool;
import org.geneontology.lego.model.LegoLink;
import org.geneontology.lego.model.LegoNode;
import org.geneontology.lego.model.LegoTools.UnExpectedStructureException;
import org.geneontology.lego.model2.LegoGraph;
import org.geneontology.lego.model2.LegoUnitTools;
import org.geneontology.lego.model2.LegoUnit;
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
import owltools.graph.shunt.OWLShuntEdge;
import owltools.graph.shunt.OWLShuntGraph;
import owltools.graph.shunt.OWLShuntNode;

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

			LegoUnitTools lTools = new LegoUnitTools(graph, graph.getReasoner());
			Set<OWLNamedIndividual> individuals = graph.getSourceOntology().getIndividualsInSignature();

				LegoGraph lUnitTools = null;
				try {
					lUnitTools = lTools.createLegoGraph(individuals);
				} catch (UnExpectedStructureException e) {
					LOG.error("LegoUnitTools did not initialize.");
					return;
				}
				List<LegoLink> links = lUnitTools.getLinks();
				List<LegoNode> nodes = lUnitTools.getNodes();
				List<LegoUnit> units = lUnitTools.getUnits();

				// Store node information for later access during assembly.
				//Map<String, LegoNode> nInfo = new HashMap<String, LegoNode>(); 
				Map<String,LegoNode> nodeInfo = new HashMap<String,LegoNode>(); 
				Map<String,List<String>> nodeLoc = new HashMap<String,List<String>>(); 
				for( LegoNode n : nodes ){

					// Resolve node ID and label.
					if( n != null ){

						String nid = n.getType().asOWLClass().getIRI().toString();
						String nlbl = bestLabel(n.getType());
						LOG.info("\nnode (id): " + nid);
						LOG.info("node (lbl): " + nlbl);
						nodeInfo.put(nid, n);
						
						OWLClass e = n.getActiveEntity();
						if( e != null ){
							String aid = n.getActiveEntity().getIRI().toString();
							String albl = graph.getLabelOrDisplayId(n.getActiveEntity());
							LOG.info("node-a (id): " + aid);
							LOG.info("node-a (lbl): " + albl);
						}
						if( n.isBp() ){
							LOG.info("node is process^");
						}
					
						// Collect cell information if possible.
						List<String> llist = new ArrayList<String>();
						Collection<OWLClassExpression> cell_loc = n.getCellularLocation();
						for( OWLClassExpression cell_loc_cls : cell_loc ){	
							// First, the trivial transfer to the final set.
							String loc_id = graph.getIdentifier(cell_loc_cls);
							String loc_lbl = bestLabel(cell_loc_cls);
							LOG.info("node location: " + loc_lbl + " (" + loc_id + ")");
						
							llist.add(loc_lbl);
							
							//// Ensure 
							//if( ! nodeLoc.containsKey(nid) ){
							//	
							//}
						}
						nodeLoc.put(nid, llist);
					}
				}
					
				// Assemble the group shunt graph from available information.
				// Most of the interesting stuff is happening with the meta-information.
				OWLShuntGraph shuntGraph = new OWLShuntGraph();
				for( LegoUnit u : units ){

					String uid = u.getId().toString();
					String ulbl = u.toString();
					
					LOG.info("\nunit id: " + uid);
					LOG.info("unit lbl: " + ulbl);
					
					OWLShuntNode shuntNode = new OWLShuntNode(uid, ulbl);

					// Try and get some info in there.
					Map<String, String> metadata = new HashMap<String,String>();

					OWLClass oc = u.getEnabledBy();
					if( oc != null ){
						//String iid = graph.getIdentifier(oc);
						String iid = oc.getIRI().toString();
						String ilbl = bestLabel(oc);
						metadata.put("enabled_by", ilbl);
						LOG.info("unit enabled_by (id): " + iid);
						LOG.info("unit enabled_by (lbl): " + ilbl);
					}
					
					OWLClassExpression process_ce = u.getProcess();
					if( process_ce != null ){
						OWLClass ln_oc = process_ce.asOWLClass();
						//String iid = graph.getIdentifier(ln_oc);
						String iid = ln_oc.getIRI().toString();
						String ilbl = bestLabel(ln_oc);
						metadata.put("process", ilbl);
						LOG.info("unit process (id): " + iid);
						LOG.info("unit process (lbl): " + ilbl);
					}
						
					OWLClassExpression activity_ce = u.getActivity();
					if( activity_ce != null ){
						OWLClass ln_oc = activity_ce.asOWLClass();
						//String iid = graph.getIdentifier(ln_oc);
						String iid = ln_oc.getIRI().toString();
						String ilbl = bestLabel(ln_oc);
						metadata.put("activity", ilbl);
						LOG.info("unit activity (id): " + iid);
						LOG.info("unit activity (lbl): " + ilbl);
					}
					
					shuntNode.setMetadata(metadata);
					shuntGraph.addNode(shuntNode);
				}
				
				// 
				for( LegoLink l : links ){

					String sid = l.getSource().getIRI().toString();
					String oid = l.getNamedTarget().getIRI().toString();
					//String pid = l.getRelation().toString();
					String pid = l.getProperty().asOWLObjectProperty().getIRI().toString();
					
					LOG.info("\nlink (sid): " + sid);
					LOG.info("link (sid): " + oid);
					LOG.info("link (sid): " + pid);

					OWLShuntEdge shuntEdge = new OWLShuntEdge(sid, oid, pid);
					shuntGraph.addEdge(shuntEdge);
				}
				
				// Collect the high-level group information: topo graph and group label/id
				//LegoShuntGraphTool shuntTool = new LegoShuntGraphTool();
				//OWLShuntGraph shuntGraph = shuntTool.renderLego(lNodes, graph);
				// TODO: This next bit is all temporary until we get real labels in somehow.
				String groupID = "unknown group";
				String groupLabel = "unknown group label";
				
				// Iterate over the participant nodes and collect the unit information.
				for( LegoUnit u : units ){

					SolrInputDocument doc = collect_unit_info(u, groupID, groupLabel, shuntGraph);

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
			}
			
			// Get the remainder of the docs in.
			LOG.info("Doing clean-up (final) commit at " + current_doc_number + " complex annotation documents...");
			addAllAndCommit();
			LOG.info("Done.");
		}
	
	
	private String bestLabel(OWLObject oc){
		String label = "???";

		if( oc != null ){
			label = graph.getLabel(oc);
			if( label == null ){
				label = graph.getIdentifier(oc);
			}
		}
		
		return label;
	}
	
	/**
	 * Take args and add it index (no commits)
	 * Main wrapping for adding complex annotation documents to GOlr.
	 * @param ln 
	 * @param groupLabel 
	 * @param groupID 
	 * @param shuntGraph 
	 * @param ca_doc 
	 *
	 * @return an input doc for add()
	 */
	public SolrInputDocument collect_unit_info(LegoUnit u, String groupID, String groupLabel, OWLShuntGraph shuntGraph) {

		SolrInputDocument ca_doc = new SolrInputDocument();

		// We'll be using the sam is_a-part_of a lot.
		ArrayList<String> isap = new ArrayList<String>();
		isap.add("BFO:0000050");

		ca_doc.addField("document_category", "complex_annotation");
		
		// annotation_unit
		// annotation_unit_label
		// TODO: This next bit is all temporary until we get real IDs and labels in somehow.
		String unitID = u.getId().toString();
		unitID = StringUtils.replace(unitID, ":", "_");
		unitID = StringUtils.replace(unitID, "/", "_");
		ca_doc.addField("annotation_unit", unitID);
		String unitLabel = u.toString(); // TODO: ???
		//ca_doc.addField("annotation_unit_label", unitLabel);
		ca_doc.addField("annotation_unit_label", "view");
		
		// TODO: This sucks, but live with it for now for testing.
		//ca_doc.addField("id", "???");
		//ca_doc.addField("id", current_doc_number);
		ca_doc.addField("id", unitID);

		// annotation_group(_label)
		ca_doc.addField("annotation_group", groupID);
		ca_doc.addField("annotation_group_label", groupLabel);
		
		// enabled_by(_label)
		OWLClass oc = u.getEnabledBy();
		String oc_id = graph.getIdentifier(oc);
		String oc_lbl = bestLabel(oc);
		ca_doc.addField("enabled_by", oc_id);
		ca_doc.addField("enabled_by_label", oc_lbl);
		
		// process_class(_label)
		// process_class_closure(_label)
		// process_class_closure_map
		OWLClassExpression process_ce = u.getProcess();
		if( process_ce != null ){
			// Get ready for the isa-part_of closure assembly.
			OWLClass ln_oc = process_ce.asOWLClass();
			ca_doc.addField("process_class", graph.getIdentifier(ln_oc));
			ca_doc.addField("process_class_label", bestLabel(ln_oc));
			addClosureToDoc(isap, "process_class_closure", "process_class_closure_label", "process_class_closure_map", ln_oc, ca_doc);
		}
			
		// function_class(_label)
		// function_class_closure(_label)
		// function_class_closure_map
		OWLClassExpression activity_ce = u.getActivity();
		if( activity_ce != null ){
			OWLClass ln_oc = activity_ce.asOWLClass();
			ca_doc.addField("function_class", graph.getIdentifier(ln_oc));
			ca_doc.addField("function_class_label", bestLabel(ln_oc));
			addClosureToDoc(isap, "function_class_closure", "function_class_closure_label", "function_class_closure_map", ln_oc, ca_doc);				
		}

		// location_list(_label)
		// location_list_map
		// location_list_closure(_label)
		// location_list_closure_map

		// Caches for location_list.
		Set<String> locIDSet = new HashSet<String>();
		Set<String> locLabelSet = new HashSet<String>();
		Map<String, String> locMap = new HashMap<String, String>();

		// Caches for location_list_closure.
		Set<String> locClosureIDSet = new HashSet<String>();
		Set<String> locClosureLabelSet = new HashSet<String>();
		Map<String, String> locClosureMap = new HashMap<String, String>();

		// Collect painfully class by class.
		Collection<OWLClass> cell_loc = u.getLocation();
		for( OWLClassExpression cell_loc_cls : cell_loc ){
			
			// First, the trivial transfer to the final set.
			String loc_id = graph.getIdentifier(cell_loc_cls);
			String loc_lbl = bestLabel(cell_loc_cls);
			//String loc_lbl = graph.getLabelOrDisplayId(cell_loc_cls);
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
		
		// topology_graph_json
		//ca_doc.addField("topology_graph_json", shuntGraph.toJSON());
		ca_doc.addField("topology_graph_json", shuntGraph.unsafeToJSON());
		
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
