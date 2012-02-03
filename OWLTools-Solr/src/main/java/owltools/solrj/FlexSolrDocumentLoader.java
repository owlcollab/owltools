package owltools.solrj;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
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
import org.yaml.snakeyaml.Yaml;

import owltools.gaf.Bioentity;
import owltools.gaf.ExtensionExpression;
import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.WithInfo;
import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLGraphWrapper.ISynonym;
import owltools.graph.OWLQuantifiedProperty;

public class FlexSolrDocumentLoader extends AbstractSolrLoader {

	private static Logger LOG = Logger.getLogger(FlexSolrDocumentLoader.class);

	public FlexSolrDocumentLoader(String url, OWLGraphWrapper graph) throws MalformedURLException {
		super(url);
		setGraph(graph);
	}

	// Get the inputs from the configuration file.
	private Object getConfig() throws FileNotFoundException {

		// ...
		String rsrc = "flex-loader.yaml";
		ClassLoader floader = FlexSolrDocumentLoader.class.getClassLoader();
		URL yamlURL = floader.getResource(rsrc);
		if( yamlURL == null ){
			LOG.info("Couldn't access \"" + rsrc + "\" in: " + getClass().getResource("").toString());
			return null;
		}
	
		// ...
		InputStream input = null;
		try {
			input = new FileInputStream(new File(yamlURL.toURI()));
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		LOG.info("Found flex config: " + yamlURL.toString());
		//String input = yamlURL.toString();
		Yaml yaml = new Yaml();
		Object config = yaml.load(input);
		LOG.info("Dumping flex loader YAML: " + yaml.dump(config));

		return config;
		//return null;
	}
	
	@Override
	public void load() throws SolrServerException, IOException {

		Object config = getConfig();
		
		if( graph == null ){
			LOG.info("ERROR? OWLGraphWrapper graph is not apparently defined...");
		}else{
			for (OWLObject obj : graph.getAllOWLObjects()) {
				add(collect(obj, graph, config));
			}	
			addAllAndCommit();
		}
	}

	// Main wrapping for adding ontology documents to GOlr.
	// Also see GafSolrDocumentLoader for the others.
	public SolrInputDocument collect(OWLObject obj, OWLGraphWrapper graph, Object config) {

		SolrInputDocument cls_doc = new SolrInputDocument();

//		// TODO: use object to create load sequence.
//		config.
//		for(  ){
//			
//		}
		
//		// General for all ontology objects.
//		cls_doc.addField("id", graph.getIdentifier(obj));
//		cls_doc.addField("label", graph.getLabel(obj));
//		cls_doc.addField("description", graph.getDef(obj));
//		
//		// Single fields.
//		cls_doc.addField("document_category", "ontology_class");
//		cls_doc.addField("source", graph.getNamespace(obj));
//		cls_doc.addField("is_obsolete", graph.getIsObsolete(obj));
//		cls_doc.addField("comment", graph.getComment(obj));
//	
//		// Term synonym gathering.
//		java.util.List<ISynonym> syns = graph.getOBOSynonyms(obj);
//		if( syns != null && !syns.isEmpty() ){	
//			for( ISynonym s : syns ){
//				String synLabel = s.getLabel();
//				String synScope = s.getScope();
//
//				// Standard neutral synonym.
//				cls_doc.addField("synonym", synLabel); // can add multiples
//
//				// EXPERIMENTAL: scoped synonym label.
//				String synScopeName = "synonym_label_with_scope_" + synScope.toLowerCase();
//				cls_doc.addField(synScopeName, synLabel);
//			}
//		}
//	
//		// Add alternate ids, subsets, and definition xrefs.
//		cramString(cls_doc, "alternate_id", graph.getAltIds(obj));
//		cramString(cls_doc, "subset", graph.getSubsets(obj));
//		cramString(cls_doc, "definition_xref", graph.getDefXref(obj));
		
		return cls_doc;
	}

	// Private helper to load multiple fields when the list return type is of dubious quality.
	private void cramString(SolrInputDocument cls_doc, String name, Collection<String> inList) {
		if( inList != null && ! inList.isEmpty()) {
			for (String string : inList) {
				cls_doc.addField(name, string);
			}
		}
	}
}
