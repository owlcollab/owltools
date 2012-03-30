package owltools.yaml.golrconfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;

import org.apache.log4j.Logger;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public class AmiGOConfig {

	private static Logger LOG = Logger.getLogger(AmiGOConfig.class);
	private GOlrConfig config = null;

	/**
	 * Get the flexible document definition from the configuration file.
	 *
 	 * @param location
	 */
	public AmiGOConfig(String location) throws FileNotFoundException {

		// Find the file in question on the filesystem.
		InputStream input = new FileInputStream(new File(location));

		LOG.info("Found flex config: " + location);
		Yaml yaml = new Yaml(new Constructor(GOlrConfig.class));
		config = (GOlrConfig) yaml.load(input);
		LOG.info("Dumping flex loader YAML: \n" + yaml.dump(config));
	}

	/**
	 * Get the fixed fields.
	 *
 	 * @returns ArrayList<GOlrFixedField>
	 */
	public ArrayList<GOlrFixedField> getFixedFields() {
		return config.fixed;
	}
	
	/**
	 * Get the dynamic fields.
	 *
 	 * @returns ArrayList<GOlrDynamicField>
	 */
	public ArrayList<GOlrDynamicField> getDynamicFields() {
		return config.dynamic;
	}
}
