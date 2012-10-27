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
import owltools.graph.OWLQuantifiedProperty;

import com.google.gson.*;

public class GafSolrDocumentLoader extends AbstractSolrLoader {

	private static Logger LOG = Logger.getLogger(GafSolrDocumentLoader.class);

	EcoTools eco = null;
	TaxonTools taxo = null;
	PANTHERForest pset = null;

	GafDocument gafDocument;
	int doc_limit_trigger = 1000; // the number of documents to add before pushing out to solr
	//int doc_limit_trigger = 1; // the number of documents to add before pushing out to solr
	int current_doc_number;
	
	public GafSolrDocumentLoader(String url) throws MalformedURLException {
		super(url);
		current_doc_number = 0;
	}

	public GafDocument getGafDocument() {
		return gafDocument;
	}

	public void setGafDocument(GafDocument gafDocument) {
		this.gafDocument = gafDocument;
	}

	public void setEcoTools(EcoTools inEco) {
		this.eco = inEco;
	}
	
	public void setTaxonTools(TaxonTools inTaxo) {
		this.taxo = inTaxo;
	}

	public void setPANTHERSet(PANTHERForest inPSet) {
		this.pset = inPSet;
	}

	@Override
	public void load() throws SolrServerException, IOException {
		gafDocument.index();
		for (Bioentity e : gafDocument.getBioentities()) {
			add(e);
			current_doc_number++;
			if( current_doc_number % doc_limit_trigger == 0 ){
				LOG.info("Processed " + doc_limit_trigger + " bioentities at " + current_doc_number + " and committing...");
				incrementalAddAndCommit();
			}
		}
		LOG.info("Doing cleanup commit.");
		incrementalAddAndCommit(); // pick up anything that we didn't catch
		//LOG.info("Optimizing.");
		//server.optimize();
		LOG.info("Done.");
	}

//	private OWLObjectProperty getPartOfProperty() {
//		OWLObjectProperty p = graph.getOWLObjectPropertyByIdentifier("BFO:0000050");
//		return p;
//	}

	// Main wrapping for adding non-ontology documents to GOlr.
	// Also see OntologySolrLoader.
	private void add(Bioentity e) {

		String eid = e.getId();
		String esym = e.getSymbol();
		String edb = e.getDb();
		//LOG.info("Adding: " + eid + " " + esym);
		
		// We'll need this for serializing later.
		Gson gson = new Gson();
		
		SolrInputDocument bioentity_doc = new SolrInputDocument();
		
		// Bioentity document base.
		bioentity_doc.addField("document_category", "bioentity");
		bioentity_doc.addField("id", eid);
		bioentity_doc.addField("label", esym);
		bioentity_doc.addField("db", edb);
		bioentity_doc.addField("type", e.getTypeCls());

		// Various taxon and taxon closure calculations, including map.
		String etaxid = e.getNcbiTaxonId();
		bioentity_doc.addField("taxon", etaxid);
		addLabelField(bioentity_doc, "taxon_label", etaxid);
		// Add taxon_closure and taxon_closure_label.
		OWLClass tcls = graph.getOWLClassByIdentifier(etaxid);
		Set<OWLClass> taxSuper = taxo.getAncestors(tcls, true);
		// Collect information: ids and labels.
		List<String> taxIDClosure = new ArrayList<String>();
		List<String> taxLabelClosure = new ArrayList<String>();
		Map<String,String> taxon_closure_map = new HashMap<String,String>();
		for( OWLClass ts : taxSuper ){
			String tid = graph.getIdentifier(ts);
			String tlbl = graph.getLabel(ts);
			taxIDClosure.add(tid);
			taxLabelClosure.add(tlbl);
			taxon_closure_map.put(tid, tlbl);
		}
		// Compile closure map to JSON and add to the document.
		String jsonized_taxon_map = null;
		if( ! taxon_closure_map.isEmpty() ){
			jsonized_taxon_map = gson.toJson(taxon_closure_map);
		}
		// Optionally, if there is enough taxon for a map, add the collections to the document.
		if( jsonized_taxon_map != null ){
			bioentity_doc.addField("taxon_closure", taxIDClosure);
			bioentity_doc.addField("taxon_closure_label", taxLabelClosure);
			bioentity_doc.addField("taxon_closure_map", jsonized_taxon_map);
		}

		// Optionally, pull information from the PANTHER file set.
		List<String> pantherFamilies = new ArrayList<String>();
		if( pset != null && pset.getNumberOfFilesInSet() > 0 ){
			Set<PANTHERTree> pTrees = pset.getAssociatedTrees(eid);
			if( pTrees != null ){
				Iterator<PANTHERTree> piter = pTrees.iterator();
				while( piter.hasNext() ){
					PANTHERTree ptree = piter.next();
					pantherFamilies.add(ptree.getTreeName());
				}
			}
		}
		// Optionally, actually /add/ the PANTHER family data to the document.
		if( ! pantherFamilies.isEmpty() ){
			bioentity_doc.addField("family_tag", pantherFamilies);			
		}
		
		// Something that we'll need for the annotation evidence aggregate later.
		Map<String,SolrInputDocument> evAggDocMap = new HashMap<String,SolrInputDocument>();
		
		// Annotation doc
		for (GeneAnnotation a : gafDocument.getGeneAnnotations(e.getId())) {
			SolrInputDocument annotation_doc = new SolrInputDocument();

			String clsId = a.getCls();
			String refId = a.getReferenceId();

			// Annotation document base.
			annotation_doc.addField("document_category", "annotation");
			annotation_doc.addField("bioentity", eid);
			annotation_doc.addField("bioentity_label", esym);
			String asrc = a.getAssignedBy();
			annotation_doc.addField("source", asrc);
			String adate = a.getLastUpdateDate();
			annotation_doc.addField("date", adate);
			annotation_doc.addField("taxon", etaxid);
			addLabelField(annotation_doc, "taxon_label", etaxid);

			annotation_doc.addField("reference", refId);
			String a_ev_type = a.getEvidenceCls();
			annotation_doc.addField("evidence_type", a_ev_type);

			// Optionally, if there is enough taxon for a map, add the collections to the document.
			if( jsonized_taxon_map != null ){
				annotation_doc.addField("taxon_closure", taxIDClosure);
				annotation_doc.addField("taxon_closure_label", taxLabelClosure);
				annotation_doc.addField("taxon_closure_map", jsonized_taxon_map);
			}

			// Optionally, actually /add/ the PANTHER family data to the document.
			if( ! pantherFamilies.isEmpty() ){
				annotation_doc.addField("family_tag", pantherFamilies);			
			}

			// BUG/TODO: Make the ID /really/ unique - ask Chris
			annotation_doc.addField("id", eid + "_:_" + clsId + "_:_" + a_ev_type + "_:_" + asrc + "_:_" + etaxid + "_:_" + adate);

			// Evidence type closure.
			Set<OWLClass> ecoClasses = eco.getClassesForGoCode(a_ev_type);
			Set<OWLClass> ecoSuper = eco.getAncestors(ecoClasses, true);
			List<String> ecoIDClosure = new ArrayList<String>();
			for( OWLClass es : ecoSuper ){
				String itemID = es.toStringID();
				ecoIDClosure.add(itemID);
			}
			addLabelFields(annotation_doc, "evidence_type_closure", ecoIDClosure);

			// Drag in "with" (col 8).
			//annotation_doc.addField("evidence_with", a.getWithExpression());
			for (WithInfo wi : a.getWithInfos()) {
				annotation_doc.addField("evidence_with", wi.getWithXref());
			}

			annotation_doc.addField("annotation_class", clsId);
			addLabelField(annotation_doc, "annotation_class_label", clsId);

			///
			/// isa_partof_closure
			///
			
			OWLObject cls = graph.getOWLObjectByIdentifier(clsId);
			// TODO: This may be a bug workaround, or it may be the way things are.
			// getOWLObjectByIdentifier returns null on alt_ids, so skip them for now.
			if( cls != null ){
				//	System.err.println(clsId);
			
				// Add to annotation and bioentity isa_partof closures; label and id.
				List<String> idClosure = graph.getIsaPartofIDClosure(cls);
				List<String> labelClosure = graph.getIsaPartofLabelClosure(cls);
				annotation_doc.addField("isa_partof_closure", idClosure);
				annotation_doc.addField("isa_partof_closure_label", labelClosure);
				for( String tlabel : labelClosure){
					addFieldUnique(bioentity_doc, "isa_partof_closure_label", tlabel);
				}
				for( String tid : idClosure){
					addFieldUnique(bioentity_doc, "isa_partof_closure", tid);
				}
	
				// Compile closure maps to JSON.
				Map<String, String> isa_partof_map = graph.getIsaPartofClosureMap(cls);
				if( ! isa_partof_map.isEmpty() ){
					String jsonized_isa_partof_map = gson.toJson(isa_partof_map);
					annotation_doc.addField("isa_partof_closure_map", jsonized_isa_partof_map);
				}
	
				// When we cycle, we'll also want to do some stuff to track all of the evidence codes we see.
				List<String> aggEvIDClosure = new ArrayList<String>();
				List<String> aggEvWiths = new ArrayList<String>();

				// Cycle through and pick up all the associated bits for the terms in the closure.
				SolrInputDocument ev_agg_doc = null;
				for( String tid : idClosure ){
	
					String tlabel = isa_partof_map.get(tid);				
					//OWLObject c = graph.getOWLObjectByIdentifier(tid);
	
					// Only have to do the annotation evidence aggregate base once.
					// Otherwise, just skip over and add the multi fields separately.
					String evAggId = eid + "_:ev:_" + clsId;
					if (evAggDocMap.containsKey(evAggId)) {
						ev_agg_doc = evAggDocMap.get(evAggId);	
					} else {
						ev_agg_doc = new SolrInputDocument();
						evAggDocMap.put(evAggId, ev_agg_doc);
						ev_agg_doc.addField("id", evAggId);
						ev_agg_doc.addField("document_category", "annotation_evidence_aggregate");
						ev_agg_doc.addField("bioentity", eid);
						ev_agg_doc.addField("bioentity_label", esym);
						ev_agg_doc.addField("annotation_class", tid);
						ev_agg_doc.addField("annotation_class_label", tlabel);
						ev_agg_doc.addField("taxon", etaxid);
						addLabelField(ev_agg_doc, "taxon_label", etaxid);

						// Optionally, if there is enough taxon for a map, add the collections to the document.
						if( jsonized_taxon_map != null ){
							ev_agg_doc.addField("taxon_closure", taxIDClosure);
							ev_agg_doc.addField("taxon_closure_label", taxLabelClosure);
							ev_agg_doc.addField("taxon_closure_map", jsonized_taxon_map);
						}

						// Optionally, actually /add/ the PANTHER family data to the document.
						if( ! pantherFamilies.isEmpty() ){
							ev_agg_doc.addField("family_tag", pantherFamilies);			
						}
					}
	
					// Drag in "with" (col 8), this time for ev_agg.
					for (WithInfo wi : a.getWithInfos()) {
						aggEvWiths.add(wi.getWithXref());
					}
	
					// Make note for the evidence type closure.
					aggEvIDClosure.add(a.getEvidenceCls());					
				}

				// If there was actually a doc created/there, add the cumulative fields to it.
				if( ev_agg_doc != null ){
					addLabelFields(ev_agg_doc, "evidence_type_closure", aggEvIDClosure);
					addLabelFields(ev_agg_doc, "evidence_with", aggEvWiths);
				}
			}


//			Map<String,String> isa_partof_map = new HashMap<String,String>(); // capture labels/ids
//			OWLObject c = graph.getOWLObjectByIdentifier(clsId);
//			Set<OWLPropertyExpression> ps = Collections.singleton((OWLPropertyExpression)getPartOfProperty());
//			Set<OWLObject> ancs = graph.getAncestors(c, ps);
//			for (OWLObject t : ancs) {
//				if (! (t instanceof OWLClass))
//					continue;
//				String tid = graph.getIdentifier(t);
//				//System.out.println(edge+" TGT:"+tid);
//				String tlabel = null;
//				if (t != null)
//					tlabel = graph.getLabel(t);
//				annotation_doc.addField("isa_partof_closure", tid);
//				addFieldUnique(bioentity_doc, "isa_partof_closure", tid);
//				if (tlabel != null) {
//					annotation_doc.addField("isa_partof_closure_label", tlabel);
//					addFieldUnique(bioentity_doc, "isa_partof_closure_label", tlabel);
//					// Map both ways.
//					// TODO: collisions shouldn't be an issue here?
//					isa_partof_map.put(tid, tlabel);
//					isa_partof_map.put(tlabel, tid);
//				}else{
//					// For the time being at least, I want to ensure that the id and label closures
//					// mirror eachother as much as possible (for facets and mapping, etc.). Without
//					// this, in some cases there is simply nothing returned to drill on.
//					annotation_doc.addField("isa_partof_closure_label", tid);
//					addFieldUnique(bioentity_doc, "isa_partof_closure_label", tid);
//					// Map just the one way I guess--see above.
//					isa_partof_map.put(tid, tid);
//				}
//
//				// Annotation evidence aggregate base.
//				String evAggId = eid + "_:ev:_" + clsId;
//				SolrInputDocument ev_agg_doc;
//				if (evAggDocMap.containsKey(evAggId)) {
//					ev_agg_doc = evAggDocMap.get(evAggId);	
//				}
//				else {
//					ev_agg_doc = new SolrInputDocument();
//					evAggDocMap.put(evAggId, ev_agg_doc);
//					ev_agg_doc.addField("id", evAggId);
//					ev_agg_doc.addField("document_category", "annotation_evidence_aggregate");
//					ev_agg_doc.addField("bioentity", eid);
//					ev_agg_doc.addField("bioentity_label", esym);
//					ev_agg_doc.addField("annotation_class", tid);
//					ev_agg_doc.addField("annotation_class_label", tlabel);
//					ev_agg_doc.addField("taxon", taxId);
//					addLabelField(ev_agg_doc, "taxon_label", taxId);
//				}
//
//				//evidence_type is single valued
//				//aggDoc.addField("evidence_type", a.getEvidenceCls());
//
//				// Drag in "with" (col 8), this time for ev_agg.
//				for (WithInfo wi : a.getWithInfos()) {
//					ev_agg_doc.addField("evidence_with", wi.getWithXref());
//				}
//
//				//aggDoc.getFieldValues(name)
//				// TODO:
//				ev_agg_doc.addField("evidence_type_closure", a.getEvidenceCls());
//			}
			
			// Column 16.
			Map<String,String> ann_ext_map = new HashMap<String,String>(); // capture labels/ids			
			for (ExtensionExpression ee : a.getExtensionExpressions()) {
				ee.getRelation();	// TODO
				String eeid = ee.getCls();
				OWLObject eObj = graph.getOWLObjectByIdentifier(eeid);
				annotation_doc.addField("annotation_extension_class", eeid);	
				addLabelField(annotation_doc, "annotation_extension_class_label", eeid);

				if (eObj != null) {
					for (OWLGraphEdge edge : graph.getOutgoingEdgesClosureReflexive(eObj)) {
						OWLObject t = edge.getTarget();
						if (!(t instanceof OWLClass))
							continue;
						String annExtID = graph.getIdentifier(t);
						String annExtLabel = graph.getLabel(edge.getTarget());
						annotation_doc.addField("annotation_extension_class_closure", annExtID);
						annotation_doc.addField("annotation_extension_class_closure_label", annExtLabel);
						ann_ext_map.put(annExtID, annExtLabel);
						ann_ext_map.put(annExtLabel, annExtID);
					}
				}
			}

			// Add annotation ext closure map to annotation doc.
			if( ! ann_ext_map.isEmpty() ){
				String jsonized_ann_ext_map = gson.toJson(ann_ext_map);
				annotation_doc.addField("annotation_extension_class_closure_map", jsonized_ann_ext_map);
			}
			
			// Finally add doc.
			add(annotation_doc);
		}
		add(bioentity_doc);

		for (SolrInputDocument ev_agg_doc : evAggDocMap.values()) {
			add(ev_agg_doc);
		}
	}
	
	private void addFieldUnique(SolrInputDocument d, String field, String val) {
		if (val == null)
			return;
		Collection<Object> vals = d.getFieldValues(field);
		if (vals != null && vals.contains(val))
			return;
		d.addField(field, val);
	}


	private void addLabelField(SolrInputDocument d, String field, String id) {
		OWLObject obj = graph.getOWLObjectByIdentifier(id);
		if (obj == null)
			return;
		String label = graph.getLabel(obj);
		if (label != null)
			d.addField(field, label);
	}
	
	private void addLabelFields(SolrInputDocument d, String field, List<String> ids) {

		List<String> labelAccumu = new ArrayList<String>();
		
		for( String id : ids ){
			OWLObject obj = graph.getOWLObjectByIdentifier(id);
			if (obj != null){
				String label = graph.getLabel(obj);
				if (label != null){
					labelAccumu.add(label);
				}
			}
		}
		
		if( ! labelAccumu.isEmpty() ){
			d.addField(field, labelAccumu);
		}
	}

	private Set<String> edgeToField(OWLGraphEdge edge) {
		List<OWLQuantifiedProperty> qpl = edge.getQuantifiedPropertyList();
		if (qpl.size() == 0) {
			return Collections.singleton("isa_partof");
		}
		else if (qpl.size() == 1) {
			return qpToFields(qpl.get(0));
		}
		else {
			return Collections.EMPTY_SET;
		}
	}

	private Set<String> qpToFields(OWLQuantifiedProperty qp) {
		if (qp.isSubClassOf()) {
			return Collections.singleton("isa_partof");
		}
		else {
			// TODO
			return Collections.singleton("isa_partof");
		}
		//return Collections.EMPTY_SET;
	}






}
