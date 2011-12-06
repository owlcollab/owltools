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

	public GafSolrDocumentLoader(String url) throws MalformedURLException {
		super(url);
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
		}
		addAllAndCommit();
	}

	private OWLObjectProperty getPartOfProperty() {
		OWLObjectProperty p = graph.getOWLObjectPropertyByIdentifier("BFO:0000050");
		return p;
	}

	private void add(Bioentity e) {
		String eid = e.getId();
		String esym = e.getSymbol();
		SolrInputDocument d = new SolrInputDocument();
		d.addField("document_category", "bioentity");
		d.addField("id", eid);
		d.addField("label", esym);
		d.addField("type", e.getTypeCls());
		String taxId = e.getNcbiTaxonId();
		d.addField("taxon", taxId);
		addLabelField(d, "taxon_label", taxId);

		Map<String,SolrInputDocument> aggDocMap = new HashMap<String,SolrInputDocument>();
		
		for (GeneAnnotation a : gafDocument.getGeneAnnotations(e.getId())) {
			String clsId = a.getCls();


			// annotation doc
			SolrInputDocument ad = new SolrInputDocument();
			ad.addField("document_category", "annotation");
			ad.addField("id", eid + clsId); // TODO
			ad.addField("bioentity_id", e.getId());
			ad.addField("bioentity_label", esym);
			ad.addField("taxon", taxId);
			addLabelField(ad, "taxon_label", taxId);

			ad.addField("reference", a.getReferenceId());
			// TODO - ev. closure
			ad.addField("evidence_type", a.getEvidenceCls());
			ad.addField("evidence_with", a.getWithExpression());
			for (WithInfo wi : a.getWithInfos()) {
				//check this
				//ad.addField("evidence_with", wi.getWithXref());
			}

			ad.addField("annotation_class", clsId);
			addLabelField(ad, "annotation_class_label", clsId);

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
				ad.addField("isa_partof_closure", tid);
				addFieldUnique(d, "isa_partof_closure", tid);
				if (tlabel != null) {
					ad.addField("isa_partof_label_closure", tlabel);
					addFieldUnique(d, "isa_partof_label_closure", tlabel);
				}

				// aggregate
				String aggId = eid+"^^^"+clsId;
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
				ad.addField("annotation_extension_class", eeid);	
				addLabelField(ad, "annotation_extension_class_label", eeid);

				if (eObj != null) {
					for (OWLGraphEdge edge : graph.getOutgoingEdgesClosureReflexive(eObj)) {
						OWLObject t = edge.getTarget();
						if (!(t instanceof OWLClass))
							continue;
						ad.addField("annotation_extension_class_closure", graph.getIdentifier(t));
						ad.addField("annotation_extension_class_label_closure", graph.getLabel(edge.getTarget()));
					}
				}
			}

			add(ad);
		}
		add(d);

		for (SolrInputDocument aggDoc : aggDocMap.values()) {
			add(aggDoc);
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
