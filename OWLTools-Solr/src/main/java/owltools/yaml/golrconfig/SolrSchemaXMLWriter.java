package owltools.yaml.golrconfig;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import owltools.gaf.io.AbstractXmlWriter;

public class SolrSchemaXMLWriter extends AbstractXmlWriter {
	
	//private static Logger LOG = Logger.getLogger(ConfigManager.class);
	private ConfigManager config = null;
	
	public SolrSchemaXMLWriter(ConfigManager aconfig) {
		super("  "); // like emacs nXML
		config = aconfig;
	}
	
	/**
	 * Automatically add fields to the schema depending on qualities in the current GOlrField.
	 * 
	 * @param GOlrField
	 * @param xml
	 * @throws XMLStreamException
	 */
	private void generateAutomaticFields(GOlrField field, XMLStreamWriter xml) throws XMLStreamException{

		// Detect whether we need to automatically add _label_closure mapping information.
		Pattern p = Pattern.compile("(.*)_closure_label$");
		Matcher fmatch = p.matcher(field.id);
		if( fmatch.matches() ){
			
			// A map for:
			String baseName = fmatch.group(1);
			
			// NOTE: See comments below.
			xml.writeComment(" Automatically created to capture mapping information ");
			xml.writeComment(" between " + baseName + "_closure and " + baseName + "_closure_label.");
			xml.writeComment(" It is not indexed for searching, but may be useful to the client. ");
			xml.writeStartElement("field"); // <field>
			xml.writeAttribute("name", baseName + "_closure_map");
			xml.writeAttribute("type", "string");
			xml.writeAttribute("required", "false");
			xml.writeAttribute("multiValued", "false");
			xml.writeAttribute("indexed", "false");
			xml.writeAttribute("stored", "true");
			xml.writeEndElement(); // </field>
		}
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

		ArrayList<GOlrField> fieldList = config.getFields();
		for( GOlrField field : fieldList ){

			// Output any comments we found as a bunch at the top;
			// this should help clarify things when fields are overloaded.
			ArrayList<String> comments = config.getFieldComments(field.id);
			for( String comment : comments ){
				xml.writeComment(comment);
			}
			
			// Gather things up first.
			String f_id = field.id;
			String f_type = field.type;
			// ID is the only required field.
			String f_required = "false";
			if( field.id.equals("id") ){
				f_required = "true";
			}
			// Cardinality maps to multivalued.
			String f_multi = "true";
			if( field.cardinality.equals("single") ){
				f_multi = "false";
			}
			String f_indexed = field.indexed;
			
			// Write out the "main" field declaration.
			xml.writeStartElement("field");
			// The main variants.
			xml.writeAttribute("name", f_id);
			xml.writeAttribute("type", f_type);
			xml.writeAttribute("required", f_required);
			xml.writeAttribute("multiValued", f_multi);
			xml.writeAttribute("indexed", f_indexed);
			// Invariants: we'll always store.
			xml.writeAttribute("stored", "true");
			// Done.
			xml.writeEndElement(); // </field>

			// If searchable is true, create an additional field that mirrors
			// the main one, but using the tokenizer (needed for edismax, etc.).
			String f_searchable = field.searchable;
			//LOG.info("field.searchable: " + f_searchable);
			if( f_searchable.equals("true") ){

				//String munged_id = f_id + config.getSearchableExtension();
				String munged_id = f_id + "_searchable";

				xml.writeComment("An easily searchable (TextField tokenized) version of " + f_id + ".");
				xml.writeStartElement("field");
				// The main variants.
				xml.writeAttribute("name", munged_id);
				xml.writeAttribute("type", "text_searchable");
				xml.writeAttribute("required", f_required);
				xml.writeAttribute("multiValued", f_multi);
				// Invariants: we'll always store and index.
				xml.writeAttribute("indexed", "true");
				xml.writeAttribute("stored", "true");
				// Done.
				xml.writeEndElement(); // </field>				

				// Also, add the field copy soe we don't have to worry about manually loading it.
				xml.writeStartElement("copyField");
				//  <copyField source="body" dest="teaser" maxChars="300"/>
				xml.writeAttribute("source", f_id);
				xml.writeAttribute("dest", munged_id);
				xml.writeEndElement(); // </copyField>
			}
			
			// Add any automatically generated fields if necessary.
			generateAutomaticFields(field, xml);
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
		xml.writeAttribute("version", "3.6");

		xml.writeStartElement("types");

		// NOTE: See comments below.
		xml.writeComment("Unsplit string for when text needs to be dealt with atomically.");
		xml.writeComment("For example, faceted querying.");
		xml.writeStartElement("fieldType");
		xml.writeAttribute("name", "string");
		xml.writeAttribute("class", "solr.StrField");
		xml.writeAttribute("sortMissingLast", "true");
		xml.writeEndElement(); // </fieldType>		

		// NOTE: See comments below.
		xml.writeComment("Any string with spaces that needs to be treated for searching purposes.");
		xml.writeComment("This will be automatically used in cases where \"searchable: true\" has been");
		xml.writeComment("specified in the YAML.");
		xml.writeStartElement("fieldType");
		xml.writeAttribute("name", "text_searchable");
		xml.writeAttribute("class", "solr.TextField");
		xml.writeAttribute("positionIncrementGap", "100");
		xml.writeAttribute("sortMissingLast", "true");
		xml.writeStartElement("analyzer");
		xml.writeStartElement("tokenizer");
		xml.writeAttribute("class", "solr.StandardTokenizerFactory");
		xml.writeEndElement(); // </tokenizer>		
		xml.writeStartElement("filter");
		xml.writeAttribute("class", "solr.LowerCaseFilterFactory");
		xml.writeEndElement(); // </filter>		
		xml.writeEndElement(); // </analyzer>		
		xml.writeEndElement(); // </fieldType>		

		// Integer.
		xml.writeStartElement("fieldType");
		xml.writeAttribute("name", "integer");
		xml.writeAttribute("class", "solr.TrieIntField");
		xml.writeAttribute("precisionStep", "0");
		xml.writeAttribute("positionIncrementGap", "0");
		xml.writeAttribute("sortMissingLast", "true");
		xml.writeEndElement(); // </fieldType>		

		// True boolean.
		xml.writeStartElement("fieldType");
		xml.writeAttribute("name", "boolean");
		xml.writeAttribute("class", "solr.BoolField");
		xml.writeAttribute("sortMissingLast", "true");
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

		// Write out the special required "document_category" field declaration.
		xml.writeComment(" A special static/fixed (by YAML conf file) field all documents have. ");
		xml.writeStartElement("field"); // <field>
		xml.writeAttribute("name", "document_category");
		xml.writeAttribute("type", "string");
		xml.writeAttribute("required", "false");
		xml.writeAttribute("multiValued", "false");
		xml.writeAttribute("indexed", "true");
		xml.writeAttribute("stored", "true");
		xml.writeEndElement(); // </field>
		
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
		xml.writeEndElement(); // </uniqueKey>

		// These are now declared in the search string:
		// defaultSearchOperator defaults to OR and can be changed with "q.op".
		// defaultSearchField is deprecated for using "df" in request handler.
		//xml.writeStartElement("defaultSearchField");
		//xml.writeCharacters("label");
		//xml.writeEndElement(); // </defaultSearchField>
		//xml.writeStartElement("solrQueryParser");
		//xml.writeAttribute("defaultOperator", "OR");
		//xml.writeEndElement(); // </solrQueryParser>
		
		// Special STOP and wrap up.
		//xml.writeComment("STOP");
		xml.writeEndElement(); // </schema>
		xml.writeEndDocument();
		xml.close();

		return outputStream.toString();
	}

}
