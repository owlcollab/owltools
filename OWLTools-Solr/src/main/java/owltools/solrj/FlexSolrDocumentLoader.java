package owltools.solrj;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
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
import owltools.yaml.golrconfig.ConfigManager;
import owltools.yaml.golrconfig.GOlrConfig;
import owltools.yaml.golrconfig.GOlrDynamicField;
import owltools.yaml.golrconfig.GOlrFixedField;

public class FlexSolrDocumentLoader extends AbstractSolrLoader {

	private static Logger LOG = Logger.getLogger(FlexSolrDocumentLoader.class);
	private ConfigManager config = null;
	
	public FlexSolrDocumentLoader(String url, ConfigManager aconf, OWLGraphWrapper graph) throws MalformedURLException {
		super(url);
		setGraph(graph);
		config = aconf;
	}

//	/*
//	 * Get the flexible document definition from the configuration file.
//	 *
// 	 * @param
//	 * @return config
//	 */
//	private GOlrConfig getConfig() throws FileNotFoundException {
//
//		// Find the file in question on the filesystem.
//		String rsrc = "amigo-config.yaml";
//		ClassLoader floader = FlexSolrDocumentLoader.class.getClassLoader();
//		URL yamlURL = floader.getResource(rsrc);
//		if( yamlURL == null ){
//			LOG.info("Couldn't access \"" + rsrc + "\" in: " + getClass().getResource("").toString());
//			return null;
//		}
//	
//		// Generate the config from the file input text.
//		InputStream input = null;
//		try {
//			input = new FileInputStream(new File(yamlURL.toURI()));
//		} catch (URISyntaxException e) {
//			e.printStackTrace();
//		}
//		LOG.info("Found flex config: " + yamlURL.toString());
//		Yaml yaml = new Yaml(new Constructor(GOlrConfig.class));
//		GOlrConfig config = (GOlrConfig) yaml.load(input);
//		LOG.info("Dumping flex loader YAML: \n" + yaml.dump(config));
//
//		return config;
//	}
	
	@Override
	public void load() throws SolrServerException, IOException {

//		//GOlrConfig config = getConfig();
//		LOG.info("Trying to load with config: " + config.id);

		if( graph == null ){
			LOG.info("ERROR? OWLGraphWrapper graph is not apparently defined...");
		}else{
			for (OWLObject obj : graph.getAllOWLObjects()) {
				add(collect(obj, graph, config));
			}	
			addAllAndCommit();
		}
	}

	/**
	 * Try and pull out right OWLGraphWrapper function.
	 * 
	 * @param owlfunction
	 * @return
	 */
	private Method getExtMethod(String owlfunction){

		java.lang.reflect.Method method = null;
		try {
			method = graph.getClass().getMethod(owlfunction, OWLObject.class);
		} catch (SecurityException e) {
			LOG.info("ERROR: apparently a security problem with: " + owlfunction);
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			LOG.info("ERROR: couldn't find method: " + owlfunction);
			e.printStackTrace();
		}

		return method;
	}
	
	/**
	 * Get properly formatted output from the OWLGraphWrapper.
	 * 
	 * @param oobj
	 * @param owlfunction
	 * @return a (possibly null) string return value
	 */
	private String getExtString(OWLObject oobj, String owlfunction){

		String retval = null;
		
		// Try and pull out right OWLGraphWrapper function.
		java.lang.reflect.Method method = getExtMethod(owlfunction);
		
		// Try to invoke said method.
		if( method != null ){
			try {
				retval = (String) method.invoke(graph, oobj);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();				
			}
		}
		
		return retval;
	}

	/**
	 * Get properly formatted output from the OWLGraphWrapper.
	 * 
	 * @param oobj
	 * @param owlfunction
	 * @return a (possibly empty) string list of returned values
	 */
	@SuppressWarnings("unchecked")
	private List<String> getExtStringList(OWLObject oobj, String owlfunction){

		List<String> retvals = new ArrayList<String>();

		// Try and pull out right OWLGraphWrapper function.
		java.lang.reflect.Method method = getExtMethod(owlfunction);
		
		// Try to invoke said method.
		if( method != null ){
			try {
				// TODO: anybody got a better idea about this?
				retvals = (List<String>) method.invoke(graph, oobj);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();				
			}
		}
		
		return retvals;
	}
	
	/**
	 * Take args and add it index (no commits)
	 * Main wrapping for adding ontology documents to GOlr.
	 * Also see GafSolrDocumentLoader for the others.
	 *
	 * @param owlObject, graph, and a config.
	 * @return an input doc for add()
	 */
	public SolrInputDocument collect(OWLObject obj, OWLGraphWrapper graph, ConfigManager config) {

		SolrInputDocument cls_doc = new SolrInputDocument();

		///
		/// TODO/BUG: use object to create proper load sequence.
		/// Needs better cooperation from OWLTools to make is truly flexible.
		/// See Chris.
		///
		
		//LOG.info("Trying to load a(n): " + config.id);

		// Single fixed fields--the same every time.
		for( GOlrFixedField fixedField : config.getFixedFields() ){
			//LOG.info("Add: " + fixedField.id + ":" + fixedField.value);
			cls_doc.addField(fixedField.id, fixedField.property);
		}
					
		// Dynamic fields--have to get dynamic info to cram into the index.
		for( GOlrDynamicField dynamicField : config.getDynamicFields() ){

			String did = dynamicField.id;
			String prop_meth = dynamicField.property;
			String card = dynamicField.cardinality;

			// Select between the single and multi styles.
			if( card.equals("single") ){
				String val = getExtString(obj, prop_meth);
				if( val != null ){
					cls_doc.addField(did, val);
				}
			}else{
				List<String> vals = getExtStringList(obj, prop_meth);
				if( vals != null && ! vals.isEmpty() ){
					for (String val : vals) {
						cls_doc.addField(did, val);
					}
				}
			}
		}
		
		return cls_doc;
	}
}
