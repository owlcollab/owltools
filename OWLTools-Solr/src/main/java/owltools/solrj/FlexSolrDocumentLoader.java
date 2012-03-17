package owltools.solrj;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import owltools.gaf.Bioentity;
import owltools.gaf.ExtensionExpression;
import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.WithInfo;
import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLGraphWrapper.ISynonym;
import owltools.graph.OWLQuantifiedProperty;
import owltools.yaml.flexdoc.FlexDocConfig;
import owltools.yaml.flexdoc.FlexDocDynamicField;
import owltools.yaml.flexdoc.FlexDocFixedField;

public class FlexSolrDocumentLoader extends AbstractSolrLoader {

	private static Logger LOG = Logger.getLogger(FlexSolrDocumentLoader.class);
	private String schema_mangle = "ext"; // same as appears in geneontology/java/gold/conf/schema.xml
	private String ns_mangle = "unknown";

	public FlexSolrDocumentLoader(String url, OWLGraphWrapper graph) throws MalformedURLException {
		super(url);
		setGraph(graph);
	}

	/*
	 * Get the flexible document definition from the configuration file.
	 *
 	 * @param
	 * @return config
	 */
	private FlexDocConfig getConfig() throws FileNotFoundException {

		// Find the file in question on the filesystem.
		String rsrc = "flex-loader.yaml";
		ClassLoader floader = FlexSolrDocumentLoader.class.getClassLoader();
		URL yamlURL = floader.getResource(rsrc);
		if( yamlURL == null ){
			LOG.info("Couldn't access \"" + rsrc + "\" in: " + getClass().getResource("").toString());
			return null;
		}
	
		// Generate the config from the file input text.
		InputStream input = null;
		try {
			input = new FileInputStream(new File(yamlURL.toURI()));
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		LOG.info("Found flex config: " + yamlURL.toString());
		Yaml yaml = new Yaml(new Constructor(FlexDocConfig.class));
		FlexDocConfig config = (FlexDocConfig) yaml.load(input);
		LOG.info("Dumping flex loader YAML: \n" + yaml.dump(config));

		return config;
	}
	
	@Override
	public void load() throws SolrServerException, IOException {

		FlexDocConfig config = getConfig();
		ns_mangle = config.id;
		
		if( graph == null ){
			LOG.info("ERROR? OWLGraphWrapper graph is not apparently defined...");
		}else{
			for (OWLObject obj : graph.getAllOWLObjects()) {
				add(collect(obj, graph, config));
			}	
			addAllAndCommit();
		}
	}

	/*
	 * Take args and add it index (no commits)
	 * Main wrapping for adding ontology documents to GOlr.
	 * Also see GafSolrDocumentLoader for the others.
	 *
	 * @param owlObject, graph, and a config.
	 * @return an input doc for add()
	 */
	public SolrInputDocument collect(OWLObject obj, OWLGraphWrapper graph, FlexDocConfig config) {

		SolrInputDocument cls_doc = new SolrInputDocument();

		///
		/// TODO/BUG: use object to create load sequence.
		/// Needs better cooperation from OWLTools to make is truly flexible.
		/// See Chris.
		///
		
		LOG.info("Trying to load a(n): " + config.id);

		// Single fixed fields--the same every time.
		for( FlexDocFixedField fixedField : config.fixed ){
			//LOG.info("Add: " + fixedField.id + ":" + fixedField.value);
			cls_doc.addField(fixedField.id, fixedField.value);
		}
		// TODO/BUG: Needs to be removed--just here so I don't have to juggle muliple schema.xml on Solr during testing
		// and benchmarking. "id" is actually the only one that is *required* as it stands now.
		//cls_doc.addField("id", "nil");
					
		// Dynamic fields--have to get dynamic info to cram into the index.
		//LOG.info("Add?: " + fixedField.id + ":" + fixedField.property + " " + OWLRDFVocabulary.RDFS_LABEL.getIRI());
		//cls_doc.addField("id", graph.getIdentifier(obj));
		for( FlexDocDynamicField dynamicField : config.dynamic ){
			//LOG.info("Add?: (" + dynamicField.type + ") " + dynamicField.id + ":" + dynamicField.property + ":" + dynamicField.cardinality);
//			ArrayList<String> inputList = tempLoader(obj, graph, dynamicField.property);
//			cramAll(cls_doc, dynamicField.id, inputList);
			// TODO/BUG: This "id" is special for now.
			if( dynamicField.id.equals("id") ){
				cls_doc.addField("id", graph.getIdentifier(obj));
			}
			if( dynamicField.type.equals("string") || dynamicField.type.equals("text") ){
				ArrayList<String> inputList = tempStringLoader(obj, graph, dynamicField.property);
				cramString(cls_doc, dynamicField.type, dynamicField.id, inputList, dynamicField.cardinality);
			}else if( dynamicField.type.equals("integer") ){
				ArrayList<Integer> inputList = tempIntegerLoader(obj, graph, dynamicField.property);
				cramInteger(cls_doc, dynamicField.type, dynamicField.id, inputList, dynamicField.cardinality);
			}else{
				LOG.info("No input methods for: " + dynamicField.type);
			}
		}
		
		return cls_doc;
	}

	/*
	 * Jimmy interesting bits out of the OWLObject for use in loading the GOlr index.
	 * 
	 * WARNING: This is a temporary function until a proper flex mapper can be built into the OWLGraphWrapper,
	 * and that	will take some consultation with Chris.
	 * 
	 * @param owlObject and string property to identify the part of the owl object that we want
	 * @return ArrayList<String>
	 */
	@Deprecated
	private ArrayList<String> tempStringLoader(OWLObject obj, OWLGraphWrapper graph, String property){
	//private ArrayList<String> tempLoader(OWLObject obj, OWLGraphWrapper graph, String property){

		ArrayList<String> fields = new ArrayList<String>();
		
		if( property.equals("id") ){
			fields.add(graph.getIdentifier(obj));
//			// BUG
//			String foo_id = graph.getIdentifier(obj);
//			if( foo_id.equals("GO:0022008")){
//				LOG.info("Found: " + foo_id);
//			}
		}else if( property.equals("label") ){
			fields.add(graph.getLabel(obj));
		}else if( property.equals("description") ){
			fields.add(graph.getDef(obj));
		}else if( property.equals("source") ){
			fields.add(graph.getNamespace(obj));
		}else if( property.equals("comment") ){
			fields.add(graph.getComment(obj));
		}else if( property.equals("synonym") ){
			// Term synonym gathering rather more irritating.
			java.util.List<ISynonym> syns = graph.getOBOSynonyms(obj);
			if( syns != null && !syns.isEmpty() ){	
				for( ISynonym s : syns ){
					String synLabel = s.getLabel();

					// Standard neutral synonym.
					//cls_doc.addField("synonym", synLabel); // can add multiples
					fields.add(synLabel);

					// EXPERIMENTAL: scoped synonym label.
					//String synScope = s.getScope();
					//String synScopeName = "synonym_label_with_scope_" + synScope.toLowerCase();
					//cls_doc.addField(synScopeName, synLabel);
				}
			}	
		}else if( property.equals("alternate_id") ){
			fields = ensureArrayList(graph.getAltIds(obj));
		}else if( property.equals("subset") ){
			fields = ensureArrayList(graph.getSubsets(obj));
		}else if( property.equals("definition_xref") ){
			fields = ensureArrayList(graph.getDefXref(obj));
		}
			
		return fields;
	}
	// Same as above
	@Deprecated
	private ArrayList<Integer> tempIntegerLoader(OWLObject obj, OWLGraphWrapper graph, String property){

		ArrayList<Integer> fields = new ArrayList<Integer>();
		
		if( property.equals("is_obsolete") ){
			Boolean obs = graph.getIsObsolete(obj);
			if( obs ){
				fields.add(1);
			}else{
				fields.add(0);
			}
		}
		
		return fields;
	}
	
//	/*
//	 * Private helper to load our always assumed multiple fields.
//	 * Since everything is the same on the backend, go ahead and ignore incoming types.
//	 * Probably long-term deprecated since we'll eventually use some built through the OWLGraphWrapper.
//	 */
//	@Deprecated
//	private void cramAll(SolrInputDocument cls_doc, String name, ArrayList<String> inList) {
//		if( inList != null && ! inList.isEmpty()) {
//			for (String thing : inList) {
//				cls_doc.addField(name + "_" + ns_mangle + "_" + schema_mangle, thing);
//			}
//		}
//	}
	
	/*
	 * Private helper to load our always assumed multiple fields.
	 */
	@Deprecated
	private void cramString(SolrInputDocument cls_doc, String type, String name, ArrayList<String> inList, String cardinality) {
		if( inList != null && ! inList.isEmpty()) {
			for (String thing : inList) {
				cls_doc.addField(name + "_" + ns_mangle + "_" + schema_mangle + "_" + type + "_" + cardinality, thing);
			}
		}
	}
		
	@Deprecated
	private void cramInteger(SolrInputDocument cls_doc, String type, String name, ArrayList<Integer> inList, String cardinality) {
		if( inList != null && ! inList.isEmpty() ){
			for (Integer thing : inList) {
				cls_doc.addField(name + "_" + ns_mangle + "_" + schema_mangle + "_" + type + "_" + cardinality, thing);
			}
		}
	}
	
	// We want to ensure at least an empty array on these callbacks.
	private ArrayList<String> ensureArrayList (Collection<String> inList) {

		ArrayList<String> outList = new ArrayList<String>();
		
		if( inList != null && ! inList.isEmpty()) {
			outList = new ArrayList<String>(inList);
		}
		return outList;
	}
}
