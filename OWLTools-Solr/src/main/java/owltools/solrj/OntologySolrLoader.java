package owltools.solrj;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLProperty;

import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLGraphWrapper.ISynonym;

public class OntologySolrLoader extends AbstractSolrLoader {

	private static Logger LOG = Logger.getLogger(OntologySolrLoader.class);

	public OntologySolrLoader(String url, OWLGraphWrapper graph) throws MalformedURLException {
		super(url);
		setGraph(graph);
	}
	
	@Override
	public void load() throws SolrServerException, IOException {
		if( graph == null ){
			LOG.info("ERROR? OWLGraphWrapper graph is not apparently defined...");
		}else{
			for (OWLObject obj : graph.getAllOWLObjects()) {
				add(collect(obj, graph));
			}	
			addAllAndCommit();
		}
	}

	// Main wrapping for adding ontology documents to GOlr.
	// Also see GafSolrDocumentLoader for the others.
	public SolrInputDocument collect(OWLObject obj, OWLGraphWrapper graph) {

		SolrInputDocument cls_doc = new SolrInputDocument();

		// General for all ontology objects.
		cls_doc.addField("id", graph.getIdentifier(obj));
		cls_doc.addField("label", graph.getLabel(obj));
		cls_doc.addField("description", graph.getDef(obj));
		
		if (obj instanceof OWLClass)
			 collectClass(cls_doc, graph, (OWLClass)obj);
		else if (obj instanceof OWLIndividual)
			 collectIndividual(cls_doc, (OWLIndividual)obj);
		else if (obj instanceof OWLProperty)
			 collectProperty(cls_doc, (OWLProperty)obj);
		return cls_doc; 
	}

	// Things special for ontology_class.
	private void collectClass(SolrInputDocument cls_doc, OWLGraphWrapper graph, OWLClass c) {

		// Single fields.
		cls_doc.addField("document_category", "ontology_class");
		cls_doc.addField("source", graph.getNamespace(c));
		cls_doc.addField("is_obsolete", graph.getIsObsolete(c));
		cls_doc.addField("comment", graph.getComment(c));
	
		// Term synonym gathering.
		java.util.List<ISynonym> syns = graph.getOBOSynonyms(c);
		if( syns != null && !syns.isEmpty() ){	
			for( ISynonym s : syns ){
				String synLabel = s.getLabel();

				// Standard neutral synonym.
				cls_doc.addField("synonym", synLabel); // can add multiples

				// // EXPERIMENTAL: scoped synonym label.
				// String synScope = s.getScope();
				// String synScopeName = "synonym_label_with_scope_" + synScope.toLowerCase();
				// cls_doc.addField(synScopeName, synLabel);
			}
		}
	
		// Add alternate ids, subsets, and definition xrefs.
		cramString(cls_doc, "alternate_id", graph.getAltIds(c));
		cramString(cls_doc, "subset", graph.getSubsets(c));
		cramString(cls_doc, "definition_xref", graph.getDefXref(c));
	}

	// Private helper to load multiple fields when the list return type is of dubious quality.
	private void cramString(SolrInputDocument cls_doc, String name, Collection<String> inList) {
		if( inList != null && ! inList.isEmpty()) {
			for (String string : inList) {
				cls_doc.addField(name, string);
			}
		}
	}
	
	private void collectProperty(SolrInputDocument d, OWLProperty obj) {
	}

	private void collectIndividual(SolrInputDocument d, OWLIndividual obj) {
	}

}
