package owltools.solrj;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
import owltools.gaf.ExtensionExpression;
import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.WithInfo;
import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLQuantifiedProperty;

public class GafSolrDocumentLoader extends AbstractSolrLoader {

	private static Logger LOG = Logger.getLogger(GafSolrDocumentLoader.class);

	GafDocument gafDocument;
	int doc_limit_trigger = 1000; // the number of documents to add before pushing out to solr
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
		LOG.info("Optimizing.");
		server.optimize();
		LOG.info("Done.");
	}

	private OWLObjectProperty getPartOfProperty() {
		OWLObjectProperty p = graph.getOWLObjectPropertyByIdentifier("BFO:0000050");
		return p;
	}

	// Main wrapping for adding non-ontology documents to GOlr.
	// Also see OntologySolrLoader.
	private void add(Bioentity e) {

		String eid = e.getId();
		String esym = e.getSymbol();
		String edb = e.getDb();
		//LOG.info("Adding: " + eid + " " + esym);
		
		SolrInputDocument bioentity_doc = new SolrInputDocument();
		
		// Bioentity document base.
		bioentity_doc.addField("document_category", "bioentity");
		bioentity_doc.addField("id", eid);
		bioentity_doc.addField("label", esym);
		bioentity_doc.addField("db", edb);
		bioentity_doc.addField("type", e.getTypeCls());
		String taxId = e.getNcbiTaxonId();
		bioentity_doc.addField("taxon", taxId);
		addLabelField(bioentity_doc, "taxon_label", taxId);

		Map<String,SolrInputDocument> evAggDocMap = new HashMap<String,SolrInputDocument>();
		
		for (GeneAnnotation a : gafDocument.getGeneAnnotations(e.getId())) {
			// annotation doc
			SolrInputDocument annotation_doc = new SolrInputDocument();

			String clsId = a.getCls();
			String refId = a.getReferenceId();

			// Annotation document base.
			annotation_doc.addField("document_category", "annotation");
			annotation_doc.addField("id", eid + "_:_" + clsId); // TODO - make unique
			annotation_doc.addField("bioentity_id", eid);
			annotation_doc.addField("bioentity_label", esym);
			annotation_doc.addField("source", a.getAssignedBy());
			annotation_doc.addField("date", a.getLastUpdateDate());
			annotation_doc.addField("taxon", taxId);
			addLabelField(annotation_doc, "taxon_label", taxId);

			annotation_doc.addField("reference", refId);
			// TODO - ev. closure
			annotation_doc.addField("evidence_type", a.getEvidenceCls());
			annotation_doc.addField("evidence_with", a.getWithExpression());
			for (WithInfo wi : a.getWithInfos()) {
				//check this
				//ad.addField("evidence_with", wi.getWithXref());
			}

			annotation_doc.addField("annotation_class", clsId);
			addLabelField(annotation_doc, "annotation_class_label", clsId);

			// ------------------------
			// -- isa_partof_closure --
			// ------------------------
			OWLObject c = graph.getOWLObjectByIdentifier(clsId);
			Set<OWLPropertyExpression> ps = Collections.singleton((OWLPropertyExpression)getPartOfProperty());
			Set<OWLObject> ancs = graph.getAncestors(c, ps);
			for (OWLObject t : ancs) {
				if (! (t instanceof OWLClass))
					continue;
				String tid = graph.getIdentifier(t);
				//System.out.println(edge+" TGT:"+tid);
				String tlabel = null;
				if (t != null)
					tlabel = graph.getLabel(t);
				annotation_doc.addField("isa_partof_closure", tid);
				addFieldUnique(bioentity_doc, "isa_partof_closure", tid);
				if (tlabel != null) {
					annotation_doc.addField("isa_partof_label_closure", tlabel);
					addFieldUnique(bioentity_doc, "isa_partof_label_closure", tlabel);
				}

				// Annotation evidence aggregate base.
				String evAggId = eid + "_:ev:_" + clsId;
				SolrInputDocument ev_agg_doc;
				if (evAggDocMap.containsKey(evAggId)) {
					ev_agg_doc = evAggDocMap.get(evAggId);	
				}
				else {
					ev_agg_doc = new SolrInputDocument();
					evAggDocMap.put(evAggId, ev_agg_doc);
					ev_agg_doc.addField("id", evAggId);
					ev_agg_doc.addField("document_category", "annotation_evidence_aggregate");
					ev_agg_doc.addField("bioentity_id", eid);
					ev_agg_doc.addField("bioentity_label", esym);
					ev_agg_doc.addField("annotation_class", tid);
					ev_agg_doc.addField("annotation_class_label", tlabel);
					ev_agg_doc.addField("taxon", taxId);
					addLabelField(ev_agg_doc, "taxon_label", taxId);
				}

				//evidence_type is single valued
				//aggDoc.addField("evidence_type", a.getEvidenceCls());
				String wx = a.getWithExpression();
				if (wx != null && !wx.equals(""))
					ev_agg_doc.addField("evidence_with", wx);

				//aggDoc.getFieldValues(name)
				// TODO:
				ev_agg_doc.addField("evidence_closure", a.getEvidenceCls());
			}

			/*
			for (OWLGraphEdge edge : graph.getOutgoingEdgesClosureReflexive(c)) {
				OWLObject t = edge.getTarget();
				if (! (t instanceof OWLClass))
					continue;
				String tid = graph.getIdentifier(t);
				//System.out.println(edge+" TGT:"+tid);
				String tlabel = null;
				if (t != null)
					tlabel = graph.getLabel(t);
				Set<String> fields = edgeToField(edge);
				// only add to annotation document for now
				for (String field : fields) {
					ad.addField(field+"_closure", tid);
					if (tlabel != null)
						ad.addField(field+"_label_closure", tlabel);
				}

				// aggregate
				// TODO - only do this for 
				String aggId = eid+"^^^"+clsId;
				Map<String,Object> aggFields;
				SolrInputDocument aggDoc;
				if (aggDocMap.containsKey(aggId)) {
					aggDoc = aggDocMap.get(aggId);	
				}
				else {
					aggDoc = new SolrInputDocument();
					aggDocMap.put(aggId, aggDoc);
					aggDoc.addField("id", aggId);
					aggDoc.addField("document_category", "annotation_aggregate");
					aggDoc.addField("bioentity_id", eid);
					aggDoc.addField("bioentity_label", esym);
					aggDoc.addField("annotation_class", tid);
					aggDoc.addField("annotation_class_label", tlabel);
					aggDoc.addField("taxon", taxId);
					addLabelField(aggDoc, "taxon_label", taxId);
				}

				//evidence_type is single valued
				//aggDoc.addField("evidence_type", a.getEvidenceCls());
				String wx = a.getWithExpression();
				if (wx != null && !wx.equals(""))
					aggDoc.addField("evidence_with", wx);

				//aggDoc.getFieldValues(name)
				// TODO:
				aggDoc.addField("evidence_closure", a.getEvidenceCls());
			}
			 */
			
			// c16
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
						annotation_doc.addField("annotation_extension_class_closure", graph.getIdentifier(t));
						annotation_doc.addField("annotation_extension_class_label_closure", graph.getLabel(edge.getTarget()));
					}
				}
			}

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
