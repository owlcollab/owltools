package owltools.yaml.golrconfig;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import owltools.gaf.io.AbstractXmlWriter;

public class SolrSchemaXMLWriter extends AbstractXmlWriter {

	private ConfigManager config = null;
	
	public SolrSchemaXMLWriter(ConfigManager aconfig) {
		super("  "); // like emacs nXML
		config = aconfig;
	}
	
	/**
	 * Just dump out the fields of our various lists.
	 * 
	 * @param fieldList
	 * @param xml
	 * @throws XMLStreamException
	 */
	//private void outFields(List<? extends GOlrCoreField> fieldList, XMLStreamWriter xml) throws XMLStreamException{
	private void outFields(ConfigManager config, XMLStreamWriter xml) throws XMLStreamException{

		ArrayList<GOlrCoreField> fieldList = config.getFields();
		for( GOlrCoreField field : fieldList ){

			// Output any comments we found as a bunch at the top;
			// this should help clarify things when fields are overloaded.
			ArrayList<String> comments = config.getFieldComments(field.id);
			for( String comment : comments ){
				xml.writeComment(comment);
			}
			
			xml.writeStartElement("field");

			// The main variants.
			xml.writeAttribute("name", field.id);
			xml.writeAttribute("type", field.type);

			// Invariants: we'll always store and index.
			xml.writeAttribute("indexed", "true");
			xml.writeAttribute("stored", "true");

			// ID is the only required field.
			//xml.writeAttribute("required", field.required);
			if( field.id.equals("id") ){
				xml.writeAttribute("required", "true");
			}else{
				xml.writeAttribute("required", "false");
			}

			// Cardinality maps to multivalued.
			if( field.cardinality.equals("single") ){
				xml.writeAttribute("multiValued", "false");
			}else{
				xml.writeAttribute("multiValued", "true");
			}
			
			// Done.
			xml.writeEndElement(); // </field>
		}
	}
	
	/**
	 * Dump the necessary Solr schema blob to STDOUT.
	 */
	public String schema() throws XMLStreamException{
	
		//OutputStream outputStream = System.out;
		OutputStream outputStream = new ByteArrayOutputStream();
		XMLStreamWriter xml = this.createWriter(outputStream);
		
		xml.writeStartDocument();

		///
		/// Opening cruft.
		///
		
		xml.writeStartElement("schema");
		xml.writeAttribute("name", "golr");
		xml.writeAttribute("version", "1.3");

		xml.writeStartElement("types");

		xml.writeStartElement("fieldType");
		xml.writeAttribute("name", "string");
		xml.writeAttribute("class", "solr.StrField");
		xml.writeEndElement(); // </fieldType>		
		
		xml.writeStartElement("fieldType");
		xml.writeAttribute("name", "integer");
		xml.writeAttribute("class", "solr.IntField");
		xml.writeEndElement(); // </fieldType>		

		xml.writeStartElement("fieldType");
		xml.writeAttribute("name", "text_ws");
		xml.writeAttribute("class", "solr.TextField");
		xml.writeAttribute("positionIncrementGap", "100");
		xml.writeEndElement(); // </fieldType>		
		
		xml.writeEndElement(); // </types>		
		
		///
		/// Fields
		///
		
		xml.writeStartElement("fields");
		//xml.writeDefaultNamespace("http://www.w3.org/1999/xhtml");

		// Instructions.
		//xml.writeComment("START");
		//xml.writeComment(" Add this and below to your schema.xml file as your schema and restart Jetty. ");
		//xml.writeComment(" After this schema has been applied for the given config file, purge the index and rerun the loader (with said config file). ");

//		// Single fixed fields--the same every time.
//		outFields(config.getFixedFields(), xml);
//
//		// Dynamic fields.
//		outFields(config.getDynamicFields(), xml);

		// Dynamic fields.
		//outFields(config.getFields(), xml);
		outFields(config, xml);
		
		xml.writeEndElement(); // </fields>

		///
		/// Closing cruft.
		///
		
		xml.writeStartElement("uniqueKey");
		xml.writeCharacters("id");
		xml.writeEndElement(); // </field>
		xml.writeStartElement("defaultSearchField");
		xml.writeCharacters("label");
		xml.writeEndElement(); // </defaultSearchField>
		xml.writeStartElement("solrQueryParser");
		xml.writeAttribute("defaultOperator", "OR");
		xml.writeEndElement(); // </solrQueryParser>
		
		// Special STOP and wrap up.
		//xml.writeComment("STOP");
		xml.writeEndElement(); // </schema>
		xml.writeEndDocument();
		xml.close();
		xml.flush();

		return outputStream.toString();
	}

}
