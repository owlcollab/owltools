package owltools.flex;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphWrapper;
import owltools.yaml.golrconfig.ConfigManager;
import owltools.yaml.golrconfig.GOlrField;

/**
 * Pull defined sources into a middle state for output, loading into Solr, etc.
 * Essentially, a Solr document workalike.
 */
public class FlexCollection implements Iterable<FlexDocument> {
	
	private static Logger LOG = Logger.getLogger(FlexCollection.class);
	protected transient ConfigManager config = null;
	protected transient OWLGraphWrapper graph = null;
	
	/**
	 * More fun init.
	 * 
	 * This does not really work--just for testing some methods.
	 * 
	 * @param in_graph
	 */
	public FlexCollection(OWLGraphWrapper in_graph) {
		graph = in_graph;
	}

	/**
	 * More fun init.
	 * 
	 * @param aconf
	 * @param in_graph
	 */
	public FlexCollection(ConfigManager aconf, OWLGraphWrapper in_graph) {

		graph = in_graph;
		config = aconf;

		if( graph == null ){
			LOG.info("ERROR? OWLGraphWrapper graph is not apparently defined...");
		}
	}

	Object getExtObject(OWLObject oobj, List <String> function_sexpr, Map<String, Object> config){

		Object retval = null;
		
		// Let's tease out the thing that we're going to try and call.
		// As it stands now, the list should behave essentially the same way as a list sexpr.
		if( function_sexpr.size() == 0 ){
			// Nil returns null.
		}else if( function_sexpr.size() > 0 ){
		
			// Pull out the OWLGraphWrapper function.
			String owlfunction = function_sexpr.get(0);

			// Note that this list may be empty ().
			List<String> foo = function_sexpr.subList(1, function_sexpr.size());
			List <String> fargs = new ArrayList<String>(foo);

			// Try to find and invoke method.
			try {
				Class<? extends OWLGraphWrapper> cls = graph.getClass();
				Method[] methods = cls.getMethods();
				Method methodConfigured = null;
				Method methodList = null;
				/*
				 * search for two methods with method name: owlfunction
				 * and parameters either:
				 * 1) OWLObject, List
				 * or
				 * 2) OWLObject, Map
				 */
				for (Method method : methods) {
					if (method.getName().equals(owlfunction)) {
						Class<?>[] parameterTypes = method.getParameterTypes();
						if (parameterTypes.length == 2) {
							Class<?> param1 = parameterTypes[0];
							Class<?> param2 = parameterTypes[1];
							if (OWLObject.class.equals(param1)) {
								if (List.class.equals(param2)) {
									methodList = method;
								}
								else if (Map.class.equals(param2)) {
									methodConfigured = method;
								}
							}
						}
					}
				}
				if (methodConfigured != null) {
					retval = methodConfigured.invoke(graph, oobj,  config);
				}else if (methodList != null){
					retval = methodList.invoke(graph, oobj, fargs);
				} else {
					LOG.info("ERROR: couldn't find method: " + owlfunction);
				}
			} catch (SecurityException e) {
				LOG.info("ERROR: apparently a security problem with: " + owlfunction);
				e.printStackTrace();
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
	 * @see #getExtStringList
	 * @param oobj
	 * @param function_sexpr "s-expression"
	 * @param config
	 * @return a (possibly null) string return value
	 */
	public String getExtString(OWLObject oobj, List <String> function_sexpr, Map<String, Object> config){

		Object retval = getExtObject(oobj, function_sexpr, config);
		if (retval != null) {
			return (String) retval;
		}
		return null;
	}

	/**
	 * Get properly formatted output from the OWLGraphWrapper.
	 * 
	 * @see #getExtString
	 * @param oobj
	 * @param function_sexpr "s-expression"
	 * @param config
	 * @return a (possibly empty) string list of returned values
	 */
	@SuppressWarnings("unchecked")
	public List<String> getExtStringList(OWLObject oobj, List <String> function_sexpr, Map<String, Object> config){

		Object retval = getExtObject(oobj, function_sexpr, config);
		if (retval != null) {
			return (List<String>) retval;
		}
		return null;
	}
	
	/**
	 * Main wrapping for adding ontology documents to GOlr.
	 * Also see GafSolrDocumentLoader for the others.
	 *
	 * TODO: Bad Seth. We have hard-coded document_category here (and the GAF loader).
	 * The proper way would be to pair conf files and the file to be loaded, that is not happening
	 * quite yet, so we punt on this bad thing.
	 *
	 * @param obj owlObject
	 * @param config
	 * @return an input doc for add()
	 */
	public FlexDocument wring(OWLObject obj, ConfigManager config) {

		FlexDocument cls_doc = new FlexDocument();

		///
		/// TODO/BUG: use object to create proper load sequence.
		/// Needs better cooperation from OWLTools to make is truly flexible.
		/// See Chris.
		///
		
		//LOG.info("Trying to load a(n): " + config.id);

		// Special loading for document_category.
		//LOG.info("Add: " + fixedField.id + ":" + fixedField.value);
		//
		
		cls_doc.add(new FlexLine("document_category", "ontology_class"));
					
		// Dynamic fields--have to get dynamic info to cram into the index.
		for( GOlrField field : config.getFields() ){

			String did = field.id;
			List <String> prop_meth_and_args = field.property;
			Map<String, Object> configMap = field.property_config;
			String card = field.cardinality;

			//LOG.info("Add: (" + StringUtils.join(prop_meth_and_args, " ") + ")");
			
			// Select between the single and multi styles.
			if( card.equals("single") ){
				String val = getExtString(obj, prop_meth_and_args, configMap);
				if( val != null ){
					cls_doc.add(new FlexLine(did, val));
				}
			}else{
				List<String> vals = getExtStringList(obj, prop_meth_and_args, configMap);
				if( vals != null && ! vals.isEmpty() ){
					for (String val : vals) {
						cls_doc.add(new FlexLine(did, val));
					}
				}
			}
		}
		
		return cls_doc;
	}

	@Override
	public Iterator<FlexDocument> iterator() {
		final StopWatch timer = new StopWatch();
		Set<OWLObject> allOWLObjects = graph.getAllOWLObjects();
		final int totalCount = allOWLObjects.size();
		timer.start();
		final Iterator<OWLObject> objectIterator = allOWLObjects.iterator();
		
		return new Iterator<FlexDocument>() {
			
			int counter = 0;
			
			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
			
			@Override
			public FlexDocument next() {
				OWLObject obj = objectIterator.next();
				FlexDocument doc = wring(obj, config);
				counter++;
				if( counter % 1000 == 0 ){
					long elapsed = timer.getTime();
					long eta = ((long)totalCount-counter) * (elapsed/((long)counter));
					LOG.info("Loaded: " + Integer.toString(counter) + " of " + Integer.toString(totalCount) 
							+ ", elapsed: "+DurationFormatUtils.formatDurationHMS((elapsed))
							+ ", eta: "+DurationFormatUtils.formatDurationHMS(eta));
				}
				return doc;
			}
			
			@Override
			public boolean hasNext() {
				return objectIterator.hasNext();
			}
		};
	}
}
