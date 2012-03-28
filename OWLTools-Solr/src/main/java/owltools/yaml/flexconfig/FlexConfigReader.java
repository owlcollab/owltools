package owltools.yaml.flexconfig;

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

import org.apache.log4j.Logger;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public class FlexConfigReader {

	private static Logger LOG = Logger.getLogger(FlexConfigReader.class);
	private FlexDocConfig config = null;

	/**
	 * Get the flexible document definition from the configuration file.
	 *
 	 * @param location
	 */
	public FlexConfigReader(String location) throws FileNotFoundException {

		// Find the file in question on the filesystem.
		InputStream input = new FileInputStream(new File(location));

		LOG.info("Found flex config: " + location);
		Yaml yaml = new Yaml(new Constructor(FlexDocConfig.class));
		config = (FlexDocConfig) yaml.load(input);
		LOG.info("Dumping flex loader YAML: \n" + yaml.dump(config));
	}

	/**
	 * Get the fixed fields.
	 *
 	 * @returns ArrayList<FlexDocFixedField>
	 */
	public ArrayList<FlexDocFixedField> getFixedFields() {
		return config.fixed;
	}
	
	/**
	 * Get the dynamic fields.
	 *
 	 * @returns ArrayList<FlexDocDynamicField>
	 */
	public ArrayList<FlexDocDynamicField> getDynamicFields() {
		return config.dynamic;
	}

	/**
	 * Dump the necessary Solr schema blob to STDOUT.
	 */
	public void dumpSchemaBlob() throws XMLStreamException {
	
		OutputStream outputStream = System.out;
		XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
		XMLStreamWriter xml = xmlOutputFactory.createXMLStreamWriter(outputStream);

		xml.writeStartDocument();
		xml.writeStartElement("null");
		//xml.writeDefaultNamespace("http://www.w3.org/1999/xhtml");

		// Instructions.
		xml.writeComment("Add this and below to your schema.xml file as your schema and restart Jetty.");
		xml.writeComment("After this schema has been applied for the given config file, purge the index and rerun the loader (with said config file).");

		// Single fixed fields--the same every time.
		for( FlexDocFixedField fixedField : this.getFixedFields() ){
			xml.writeComment(fixedField.description);
			xml.writeStartElement("field");
			xml.writeAttribute("name", fixedField.id);
			xml.writeAttribute("type", fixedField.value);
			xml.writeAttribute("indexed", "true");
			xml.writeAttribute("stored", "true");
			if( fixedField.cardinality.equals("single") ){
				xml.writeAttribute("multiValued", "false");
			}else{
				xml.writeAttribute("multiValued", "true");
			}
			xml.writeAttribute("required", "true");
			xml.writeEndElement(); // </field>
		}
		
		// TODO: dynamic fields
		xml.writeStartElement("field");
		xml.writeAttribute("name", "value"); 
		//xml.writeCharacters("The less-than (<) and greater-than (>) characters are automatically escaped for you");
		xml.writeEndElement(); // </field>

		xml.writeComment("Stop here.");
		xml.writeEndDocument();
		xml.close();
		xml.flush();
	}
}
