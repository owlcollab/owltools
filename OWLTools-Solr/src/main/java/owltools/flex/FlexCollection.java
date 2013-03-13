package owltools.flex;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
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
	
	protected ArrayList<FlexDocument> docs = null;
	
	/**
	 * More fun init.
	 * 
	 * This does not really work--just for testing some methods.
	 * 
	 * @param in_graph
	 */
	public FlexCollection(OWLGraphWrapper in_graph) {
		graph = in_graph;
		docs = new ArrayList<FlexDocument>();
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

		docs = new ArrayList<FlexDocument>();
//		//GOlrConfig config = getConfig();
//		LOG.info("Trying to load with config: " + config.id);

		if( graph == null ){
			LOG.info("ERROR? OWLGraphWrapper graph is not apparently defined...");
		}else{
			int c = 0;
			int t = graph.getAllOWLObjects().size();
			LOG.info("Loading collection with: " + t + " objects.");
			for (OWLObject obj : graph.getAllOWLObjects()) {
				docs.add(wring(obj, config));

				// Status.
				c++;
				if( c % 1000 == 0 ){
					LOG.info("Loaded: " + Integer.toString(c) + " of " + Integer.toString(t) + ".");
				}
			}	
		}
	}

//	/**
//	 * Try and pull out right OWLGraphWrapper function, one that takes an OWLObject and some (String?) args as an argument.
//	 * 
//	 * @param owlfunction
//	 * @return
//	 */
//	private Method getOWLGraphWrapperMethod(String owlfunction, Object... function_additional_args){
//
//		// Gather all of our argument classes together for the method search.
//		ArrayList<Class<?>> arg_classes = new ArrayList<Class<?>>();
//		arg_classes.add(OWLObject.class); // this will always be the first argument
//		for( Object o : function_additional_args){
//			arg_classes.add(o.getClass());
//		}
//		
//		// Try and hunt down our method.
//		java.lang.reflect.Method method = null;
//		try {
//			method = graph.getClass().getMethod(owlfunction, (Class<?>[]) arg_classes.toArray());
//		} catch (SecurityException e) {
//			LOG.info("ERROR: apparently a security problem with: " + owlfunction);
//			e.printStackTrace();
//		} catch (NoSuchMethodException e) {
//			LOG.info("ERROR: couldn't find method: " + owlfunction);
//			e.printStackTrace();
//		}
//
//		return method;
//	}
	
	/**
	 * Get properly formatted output from the OWLGraphWrapper.
	 * 
	 * @see #getExtStringList
	 * @param oobj
	 * @param owlfunction "s-expression"
	 * @return a (possibly null) string return value
	 */
	//private String getExtString(OWLObject oobj, ArrayList <String> function_sexpr){
	public String getExtString(OWLObject oobj, ArrayList <String> function_sexpr){

		String retval = null;
		
		// Let's tease out the thing that we're going to try and call.
		// As it stands now, the list should behave essentially the same way as a list sexpr.
		if( function_sexpr.size() == 0 ){
			// Nil returns null.
		}else if( function_sexpr.size() > 0 ){
		
			// Pull out the OWLGraphWrapper function.
			String owlfunction = function_sexpr.get(0);

			// Note that this list may be empty ().
			List<String> foo = function_sexpr.subList(1, function_sexpr.size());
			ArrayList <String> fargs = new ArrayList<String>(foo);

//			LOG.info("1: " + owlfunction);
//			LOG.info("2: " + fargs);
//			LOG.info("3: " + fargs.size());
//			LOG.info("4: " + OWLObject.class.toString());
//			LOG.info("5: " + fargs.getClass().toString());

			// Try to invoke said method.
			try {
				java.lang.reflect.Method method = graph.getClass().getMethod(owlfunction, OWLObject.class, fargs.getClass());
				retval = (method != null) ? (String) method.invoke(graph, oobj, fargs) : null;
			} catch (SecurityException e) {
				LOG.info("ERROR: apparently a security problem with: " + owlfunction);
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				LOG.info("ERROR: couldn't find method: " + owlfunction);
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
	 * @see #getExtString
	 * @param oobj
	 * @param owlfunction "s-expression"
	 * @return a (possibly empty) string list of returned values
	 */
	@SuppressWarnings("unchecked")
	//private List<String> getExtStringList(OWLObject oobj, ArrayList <String> function_sexpr){
	public List<String> getExtStringList(OWLObject oobj, ArrayList <String> function_sexpr){

		ArrayList<String> retvals = new ArrayList<String>();

		// First, let's tease out the thing that we're going to try and call.
		// As it stands now, the list should behave essentially the same way as a list sexpr.
		if( function_sexpr.size() == 0 ){
			// Nil returns null.
		}else if( function_sexpr.size() > 0){
		
			// Pull out the OWLGraphWrapper function.
			String owlfunction = function_sexpr.get(0);

			// Note that this list may be empty ().
			List<String> foo = function_sexpr.subList(1, function_sexpr.size());
			ArrayList <String> fargs = new ArrayList<String>(foo);

			// Try to invoke said method.
			try {
				java.lang.reflect.Method method = graph.getClass().getMethod(owlfunction, OWLObject.class, fargs.getClass());
				retvals = (method != null) ? (ArrayList<String>) method.invoke(graph, oobj, fargs) : new ArrayList<String>();
			} catch (SecurityException e) {
				LOG.info("ERROR: apparently a security problem with: " + owlfunction);
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				LOG.info("ERROR: couldn't find method: " + owlfunction);
				e.printStackTrace();
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
	
//	/**
//	 * Private helper to take care of the annoying busywork.
//	 * 
//	 * @param car
//	 * @param cdr
//	 * @return
//	 */
//	private ArrayList<String> joinLine(String car, String cdr) {
//		ArrayList<String> c = new ArrayList<String>();
//		c.add(car);
//		c.add(cdr);
//		return c;
//	}
	
	/**
	 * Main wrapping for adding ontology documents to GOlr.
	 * Also see GafSolrDocumentLoader for the others.
	 *
	 * TODO: Bad Seth. We have hard-coded document_category here (and the GAF loader).
	 * The proper way would be to pair conf files and the file to be loaded, that is not happening
	 * quite yet, so we punt on this bad thing.
	 *
	 * @param owlObject, graph, and a config.
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
			ArrayList <String> prop_meth_and_args = field.property;
			String card = field.cardinality;

			//LOG.info("Add: (" + StringUtils.join(prop_meth_and_args, " ") + ")");
			
			// Select between the single and multi styles.
			if( card.equals("single") ){
				String val = getExtString(obj, prop_meth_and_args);
				if( val != null ){
					cls_doc.add(new FlexLine(did, val));
				}
			}else{
				List<String> vals = getExtStringList(obj, prop_meth_and_args);
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
		return docs.iterator();
	}
}
