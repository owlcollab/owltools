package owltools.yaml.golrconfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;

import org.apache.log4j.Logger;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public class ConfigManager {

	private static Logger LOG = Logger.getLogger(ConfigManager.class);
	//private GOlrConfig config = null;
	
	private ArrayList<GOlrFixedField> fixed_fields = new ArrayList<GOlrFixedField>();
	private ArrayList<GOlrDynamicField> dynamic_fields = new ArrayList<GOlrDynamicField>();
	private HashMap<String, GOlrCoreField> unique_fields = new HashMap<String, GOlrCoreField>();
	private HashMap<String, ArrayList<String>> collected_comments = new HashMap<String, ArrayList<String>>();
	
	/**
	 * Constructor.
	 */
	public ConfigManager() {
		// Nobody here.
	}

	private void addFieldToBook (GOlrCoreField field) {
		// Ensure presence of item; only take the first one.
		if( ! unique_fields.containsKey(field.id) ){
			unique_fields.put(field.id, field);
		}
		
		// Ensure presence of comments (description) list.
		if( ! collected_comments.containsKey(field.id) ){
			collected_comments.put(field.id, new ArrayList<String>());
		}
		// And add to it if there is an available description.
		if( field.description != null ){
			ArrayList<String> comments = collected_comments.get(field.id);
			comments.add(field.description);
			collected_comments.put(field.id, comments);
		}
	}
	
	/**
	 * Work with a flexible document definition from a configuration file.
	 *
 	 * @param location
	 */
	public void add(String location) throws FileNotFoundException {

		// Find the file in question on the filesystem.
		InputStream input = new FileInputStream(new File(location));

		LOG.info("Found flex config: " + location);
		Yaml yaml = new Yaml(new Constructor(GOlrConfig.class));
		GOlrConfig config = (GOlrConfig) yaml.load(input);
		LOG.info("Dumping flex loader YAML: \n" + yaml.dump(config));

		// Plonk them all in to our bookkeeping.
		for( GOlrFixedField field : config.fixed ){
			addFieldToBook(field);
			fixed_fields.add(field);
		}
		for( GOlrDynamicField field : config.dynamic ){
			addFieldToBook(field);
			dynamic_fields.add(field);
		}
	}

	/**
	 * Get all the fields.
	 * 
	 * @return
	 */
	public ArrayList<GOlrCoreField> getFields() {

		ArrayList<GOlrCoreField> collection = new ArrayList<GOlrCoreField>();
		
		// Plonk them all in to our bookkeeping.
		for( GOlrCoreField field : unique_fields.values() ){
			collection.add(field);
		}
		
		return collection;
	}
	
	/**
	 * Return the comments associated with the GOlrCoreField id; empty list if there weren't any.
	 * 
	 * @return
	 */
	public ArrayList<String> getFieldComments(String id) {

		ArrayList<String> collection = new ArrayList<String>();
		
		// Plonk them all in to our bookkeeping.	private HashMap<String, ArrayList<String>> collected_comments = new HashMap<String, ArrayList<String>>();
		if( collected_comments.containsKey(id) ){
			collection = collected_comments.get(id);
		}
		
		return collection;
	}
	
	/**
	 * Get the fixed fields.
	 *
 	 * @returns ArrayList<GOlrFixedField>
	 */
	public ArrayList<GOlrFixedField> getFixedFields() {
		return fixed_fields;
	}
	
	/**
	 * Get the dynamic fields.
	 *
 	 * @returns ArrayList<GOlrDynamicField>
	 */
	public ArrayList<GOlrDynamicField> getDynamicFields() {
		return dynamic_fields;
	}
}
