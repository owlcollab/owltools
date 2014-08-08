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
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.gaf.Bioentity;
import owltools.gaf.EcoTools;
import owltools.gaf.ExtensionExpression;
import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.TaxonTools;
import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLQuantifiedProperty;
import owltools.graph.shunt.OWLShuntEdge;
import owltools.graph.shunt.OWLShuntGraph;
import owltools.graph.shunt.OWLShuntNode;
import owltools.io.OWLPrettyPrinter;

import com.google.gson.*;

/**
 * A very specific class for the specific use case of loading in complex annotations from owl.
 */
public class ComplexAnnotationSolrDocumentLoader extends AbstractSolrLoader {

	private static Logger LOG = Logger.getLogger(ComplexAnnotationSolrDocumentLoader.class);
	int doc_limit_trigger = 1000; // the number of documents to add before pushing out to solr
	int current_doc_number;
	private OWLGraphWrapper currentGraph = null;
	private OWLReasoner currentReasoner = null;
	private String currentGroupID = null;
	private String currentGroupLabel = null;
	private String currentGroupURL = null;
	private Set<OWLNamedIndividual> legoIndividuals = null;
	
	public ComplexAnnotationSolrDocumentLoader(String url, OWLGraphWrapper g, OWLReasoner r, Set<OWLNamedIndividual> individuals, String agID, String agLabel, String agURL) throws MalformedURLException {
		super(url);
		//setGraph(g);
		current_doc_number = 0;
		currentGraph = g;
		currentReasoner  = r;
		legoIndividuals = individuals;
		currentGroupID = agID;
		currentGroupLabel = agLabel;
		currentGroupURL = agURL;
	}
	
	@Override
	public void load() throws SolrServerException, IOException {
		
		LOG.info("Loading complex annotation documents...");
		
		if( currentGraph == null ){
			LOG.info("ERROR? current OWLGraphWrapper graph from Lego is not apparently defined...");
		}else{

			LegoUnitTools lTools = new LegoUnitTools(currentGraph, currentReasoner);
			//Set<OWLNamedIndividual> individuals = legoGraph.getSourceOntology().getIndividualsInSignature();

				LegoGraph lUnitTools = null;
				try {
					//lUnitTools = lTools.createLegoGraph(individuals);
					lUnitTools = lTools.createLegoGraph(legoIndividuals);
				} catch (UnExpectedStructureException e) {
					LOG.error("LegoUnitTools did not initialize.");
					return;
				}
				List<LegoLink> links = lUnitTools.getLinks();
				List<LegoNode> nodes = lUnitTools.getNodes();
				List<LegoUnit> units = lUnitTools.getUnits();
				
				OWLShuntGraph shuntGraph = createShuntGraph(links, nodes);

//				// Store node information for later access during assembly.
//				//Map<String, LegoNode> nInfo = new HashMap<String, LegoNode>(); 
//				Map<String,LegoNode> nodeInfo = new HashMap<String,LegoNode>(); 
//				Map<String,List<String>> nodeLoc = new HashMap<String,List<String>>(); 
//				for( LegoNode n : nodes ){
//
//					// Resolve node ID and label.
//					if( n != null ){
//
//						OWLClassExpression ntype = n.getType();
//						String nid = null;
//						String nlbl = null;
//						if( ! ntype.isAnonymous() ){
//							nid = n.getType().asOWLClass().getIRI().toString();
//							nlbl = bestLabel(n.getType());
//						}else{
//							// TODO: What case is this.
//							nid = ntype.toString();
//							nlbl = ntype.toString();
//						}
//						LOG.info("\nnode (id): " + nid);
//						LOG.info("node (lbl): " + nlbl);
//						nodeInfo.put(nid, n);
//						
//						OWLClass e = n.getActiveEntity();
//						if( e != null ){
//							String aid = n.getActiveEntity().getIRI().toString();
//							String albl = currentGraph.getLabelOrDisplayId(n.getActiveEntity());
//							LOG.info("node-a (id): " + aid);
//							LOG.info("node-a (lbl): " + albl);
//						}
//						if( n.isBp() ){
//							LOG.info("node is process^");
//						}
//					
//						// Collect cell information if possible.
//						List<String> llist = new ArrayList<String>();
//						Collection<OWLClassExpression> cell_loc = n.getCellularLocation();
//						for( OWLClassExpression cell_loc_cls : cell_loc ){	
//							// First, the trivial transfer to the final set.
//							String loc_id = currentGraph.getIdentifier(cell_loc_cls);
//							String loc_lbl = bestLabel(cell_loc_cls);
//							LOG.info("node location: " + loc_lbl + " (" + loc_id + ")");
//						
//							llist.add(loc_lbl);
//							
//							//// Ensure 
//							//if( ! nodeLoc.containsKey(nid) ){
//							//	
//							//}
//						}
//						nodeLoc.put(nid, llist);
//					}
//				}
					
				
				// Collect the high-level group information: topo graph and group label/id
				//LegoShuntGraphTool shuntTool = new LegoShuntGraphTool();
				//OWLShuntGraph shuntGraph = shuntTool.renderLego(lNodes, graph);
				// TODO: This next bit is all temporary until we get real labels in somehow.
				String groupID = currentGroupID;
				String groupLabel = currentGroupLabel;
				String groupURL = currentGroupURL;
				
				// Iterate over the participant nodes and collect the unit information.
				for( LegoUnit u : units ){

					SolrInputDocument doc = collect_unit_info(u, groupID, groupLabel, groupURL, shuntGraph);

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
	
	
	private OWLShuntGraph createShuntGraph(List<LegoLink> links, List<LegoNode> nodes) {
		// Assemble the group shunt graph from available information.
		// Most of the interesting stuff is happening with the meta-information.
		OWLShuntGraph shuntGraph = new OWLShuntGraph();
		OWLPrettyPrinter pp = new OWLPrettyPrinter(currentGraph);
		
		// nodes
		for( LegoNode node : nodes ){

			String uid = node.getId().toString();
			LOG.info("\nunit id: " + uid);
			OWLShuntNode shuntNode = new OWLShuntNode(uid, uid);

			// Try and get some info in there.
			Map<String,Object> metadata = new HashMap<String,Object>();

			OWLClass enabledByClass = node.getActiveEntity();
			if( enabledByClass != null ){
				String iid = enabledByClass.getIRI().toString();
				String ilbl = bestLabel(enabledByClass);
				metadata.put("enabled_by", ilbl);
				LOG.info("unit enabled_by (id): " + iid);
				LOG.info("unit enabled_by (lbl): " + ilbl);
			}
			
			OWLClassExpression type = node.getType();
			if (node.isBp()){
				String processLbl;
				if (type == null ) {
					processLbl = "Unknown Process";
				}else if (type.isAnonymous() == false) {
					OWLClass processClass = type.asOWLClass();
					String iid = processClass.getIRI().toString();
					processLbl = bestLabel(processClass);
					LOG.info("unit process (id): " + iid);
				}else{
					processLbl = pp.render(type);
				}
				metadata.put("process", processLbl);
				LOG.info("unit process (lbl): " + processLbl);
			}
			
			if (node.isMf() || node.isCmf()) { 
				String activityLbl;
				if (type == null) {
					// use custom label or GO:0003674 'molecular function' 
					activityLbl = "Unknown Activity";
				}else if (type.isAnonymous() == false) {
					OWLClass ln_oc = type.asOWLClass();
					String iid = ln_oc.getIRI().toString();
					activityLbl = bestLabel(ln_oc);
					LOG.info("unit activity (id): " + iid);
				}else {
					activityLbl = pp.render(type);
				}
				metadata.put("activity", activityLbl);
				LOG.info("unit activity (lbl): " + activityLbl);
			}
			
			Collection<OWLClassExpression> locations = node.getCellularLocation();
			if (locations != null && !locations.isEmpty()) {
				List<String> locationLabels = new ArrayList<String>();
				for (OWLClassExpression ce : locations) {
					String locationlbl;
					if( ce.isAnonymous() == false ){
						OWLClass locationClass = ce.asOWLClass();
						//String locationId = locationClass.getIRI().toString();
						locationlbl = bestLabel(locationClass);
					}else {
						locationlbl = pp.render(ce);	
					}
					locationLabels.add(locationlbl);
				}
				// Add locationLabels to meta data map
				metadata.put("location", locationLabels);
				LOG.info("unit location (lbl): " + StringUtils.join(locationLabels, ", "));
			}
			
			// TODO decide on if and how to include the other class expressions
			Collection<OWLClassExpression> others = node.getUnknowns();
			if (others != null && ! others.isEmpty()) {
				List<String> unknownLabels = new ArrayList<String>();
				for (OWLClassExpression ce : others) {
					String lbl = null;
					if (ce.isAnonymous() == false) {
						OWLClass otherClass = ce.asOWLClass();
						lbl = bestLabel(otherClass);
					}else {
						lbl = pp.render(ce);
					}
					unknownLabels.add(lbl);
				}
				metadata.put("unknown", unknownLabels);
			}
			
			// Set meta-data and add to node assembly.
			shuntNode.setMetadata(metadata);
			shuntGraph.addNode(shuntNode);
		}
		
		// edges
		for( LegoLink l : links ){

			String sid = l.getSource().getIRI().toString();
			String oid = l.getNamedTarget().getIRI().toString();
			String pid = l.getProperty().asOWLObjectProperty().getIRI().toString();
			
			LOG.info("\nlink (sid): " + sid);
			LOG.info("link (sid): " + oid);
			LOG.info("link (sid): " + pid);

			OWLShuntEdge shuntEdge = new OWLShuntEdge(sid, oid, pid);
			shuntGraph.addEdge(shuntEdge);
		}
		
		return shuntGraph;
	}
	
	private String bestLabel(OWLObject oc){
		String label = "???";

		if( oc != null ){
			label = currentGraph.getLabel(oc);
			if( label == null ){
				label = currentGraph.getIdentifier(oc);
			}
		}
		
		return label;
	}
	
	/**
	 * Convert a (probably url) string into something not terrible to use in real life.
	 * 
	 * @param id
	 * @return
	 */
	private String lessTerribleID(String id){
		String newID = new String(id);
		newID = StringUtils.replace(newID, ":", "_");
		newID = StringUtils.replace(newID, "/", "_");
		newID = StringUtils.replace(newID, "#", "_");
		return newID;
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
	public SolrInputDocument collect_unit_info(LegoUnit u, String groupID, String groupLabel, String groupURL, OWLShuntGraph shuntGraph) {

		SolrInputDocument ca_doc = new SolrInputDocument();

		// We'll be using the sam is_a-part_of a lot.
		ArrayList<String> isap = new ArrayList<String>();
		isap.add("BFO:0000050");

		ca_doc.addField("document_category", "complex_annotation");
		
		// annotation_unit
		// annotation_unit_label
		// TODO: This next bit is all temporary until we get real IDs and labels in somehow.
		String unitID = u.getId().toString();
		unitID = lessTerribleID(unitID);
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
		ca_doc.addField("annotation_group_url", groupURL);
		
		// enabled_by(_label)
		OWLClass oc = u.getEnabledBy();
		String oc_id = currentGraph.getIdentifier(oc);
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
			ca_doc.addField("process_class", currentGraph.getIdentifier(ln_oc));
			ca_doc.addField("process_class_label", bestLabel(ln_oc));
			addClosureToDoc(isap, "process_class_closure", "process_class_closure_label", "process_class_closure_map", ln_oc, ca_doc);
		}
			
		// function_class(_label)
		// function_class_closure(_label)
		// function_class_closure_map
		OWLClassExpression activity_ce = u.getActivity();
		if( activity_ce != null ){
			OWLClass ln_oc = activity_ce.asOWLClass();
			ca_doc.addField("function_class", currentGraph.getIdentifier(ln_oc));
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
			String loc_id = currentGraph.getIdentifier(cell_loc_cls);
			String loc_lbl = bestLabel(cell_loc_cls);
			//String loc_lbl = currentGraph.getLabelOrDisplayId(cell_loc_cls);
			locIDSet.add(loc_id);
			locLabelSet.add(loc_lbl);
			locMap.put(loc_id, loc_lbl);
			
			// Add closures to cache sets
			List<String> loc_id_closure = currentGraph.getRelationIDClosure(cell_loc_cls, isap);
			locClosureIDSet.addAll(loc_id_closure);
			List<String> loc_label_closure = currentGraph.getRelationLabelClosure(cell_loc_cls, isap);
			locClosureLabelSet.addAll(loc_label_closure);
			Map<String, String> loc_closure_map = currentGraph.getRelationClosureMap(cell_loc_cls, isap);
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
		List<String> idClosure = currentGraph.getRelationIDClosure(cls, relations);
		List<String> labelClosure = currentGraph.getRelationLabelClosure(cls, relations);
		solr_doc.addField(closureName, idClosure);
		solr_doc.addField(closureNameLabel, labelClosure);
		for( String tid : idClosure){
			addFieldUnique(solr_doc, closureName, tid);
		}

		// Compile closure maps to JSON.
		Map<String, String> cmap = currentGraph.getRelationClosureMap(cls, relations);
		if( ! cmap.isEmpty() ){
			String jsonized_cmap = gson.toJson(cmap);
			solr_doc.addField(closureMap, jsonized_cmap);
		}
		
		return cmap;
	}

}
