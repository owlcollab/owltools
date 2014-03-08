package owltools.gaf.bioentities;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.lang3.StringUtils;

import owltools.gaf.bioentities.QuestForOrthologsSeqXMLParser.MoreInfoProteinListener.MoreInfo;

/**
 * This is an event based parser for the seq XML produced by the Quest for
 * Orthologs project.<br>
 * <br>
 * Add listeners to retrieve the information. There are two types of listeners.
 * The {@link ProteinListener} gives a primary id and some labels. The
 * {@link MoreInfoProteinListener} will provide additionally all available
 * alternative identifiers and sequence via the {@link MoreInfo} object.
 * 
 * @see ProteinListener
 * @see MoreInfoProteinListener
 */
public class QuestForOrthologsSeqXMLParser {

	private final List<ProteinListener> proteinListeners = new ArrayList<ProteinListener>();
	private final List<MoreInfoProteinListener> moreListeners = new ArrayList<MoreInfoProteinListener>(); 
	
	/**
	 * Listener for {@link QuestForOrthologsSeqXMLParser}. Provide minimal information for an protein.
	 */
	public static interface ProteinListener {
		
		/**
		 * @param db db name, never null
		 * @param id db-specific id, never null
		 * @param name name, can be null
		 * @param uniqueName unique name
		 * @param comment long label or comment, can be null
		 * @param taxonId corresponding ncbi taxon id, can be null
		 */
		public void handleProtein(String db, String id, String name, String uniqueName, String comment, String taxonId);
	}
	
	/**
	 * Listener of {@link QuestForOrthologsSeqXMLParser}. Provide all available information via {@link MoreInfo}.
	 */
	public static interface MoreInfoProteinListener {
		
		/**
		 * Additional data about a protein.
		 */
		public static interface MoreInfo {
			
			/**
			 * Retrieve the databases for which there are identifiers for the protein.
			 * 
			 * @return unmodifiable set of databases, never null, but may be empty
			 */
			public Set<String> getDBs();
			
			/**
			 * Retrieve all identifier of a protein for the given database. Only
			 * expects db which are obtained via {@link #getDBs()}.
			 * 
			 * @param db
			 * @return unmodifiable set, null if no entries exist for the given
			 *         db
			 * @see #getDBs()
			 */
			public Set<String> getIds(String db);
			
			/**
			 * @return sequence string or null
			 */
			public String getSequence();
		}
		
		/**
		 * @param db db name, never null
		 * @param id db-specific id, never null
		 * @param name name, can be null
		 * @param uniqueName unique name
		 * @param comment long label or comment, can be null
		 * @param taxonId corresponding ncbi taxon id, can be null
		 * @param more more information, never null
		 */
		public void handleProtein(String db, String id, String name, String uniqueName, String comment, String taxonId, MoreInfo more);
	}
	
	/**
	 * Add a {@link ProteinListener}.
	 * 
	 * @param listener
	 */
	public void addListener(ProteinListener listener) {
		proteinListeners.add(listener);
	}
	
	/**
	 * Add a {@link MoreInfoProteinListener}.
	 * 
	 * @param listener
	 */
	public void addListener(MoreInfoProteinListener listener) {
		moreListeners.add(listener);
	}
	
	private static final String TAG_seqXML = "seqXML";
	private static final String TAG_entry = "entry";
	private static final String TAG_AAseq ="AAseq";
	private static final String TAG_description = "description";
	private static final String TAG_DBRef = "DBRef";
	private static final String TAG_property = "property";
	
	private static final String ATTR_source= "source";
	private static final String ATTR_id = "id";
	private static final String ATTR_name = "name";
	private static final String ATTR_value = "value";
	
	private static final String KEY_GN = "GN";
	private static final String KEY_NCBI_TaxID = "NCBI_TaxID";
	private static final String KEY_UniProtKB_ID = "UniProtKB-ID";
	
	/**
	 * Parse the content of the stream with an event-based XML parser.<br>
	 * Content is only provided to registered listeners.
	 * 
	 * @param inputStream
	 * @throws XMLStreamException
	 */
	public void parse(InputStream inputStream) throws XMLStreamException {
		XMLStreamReader parser = null;
		try {
			XMLInputFactory factory = XMLInputFactory.newInstance();
			parser = factory.createXMLStreamReader(inputStream);

			boolean readSeqXML = false;
			for (int event = parser.next(); event != XMLStreamConstants.END_DOCUMENT; event = parser.next()) {
				if (event == XMLStreamConstants.START_ELEMENT) {
					String element = parser.getLocalName();
					if (TAG_seqXML.equals(element)) {
						if (readSeqXML) {
							error("Multiple " + TAG_seqXML + " tags found", parser);
						}
						readSeqXML = true;
					}
					else if (TAG_entry.equals(element)) {
						parseEntryTag(parser);
					}
				}
			}
		} finally {
			if (parser != null) {
				parser.close();	
			}
		}
	}
	
	private static class Collector {
		String db = null;
		String id = null;
		String comment = null;
		String uniqueName = null;
		String name = null;
		String taxonId = null;
		MoreInfoImpl more = null;
	}
	
	private static class MoreInfoImpl implements MoreInfo {
		
		String sequence = null;
		final Map<String, Set<String>> ids = new HashMap<String, Set<String>>();

		void addId(String db, String id) {
			Set<String> set = ids.get(db);
			if (set == null) {
				set = Collections.singleton(id);
				ids.put(db, set);
			}
			else if (set.size() == 1) {
				set = new HashSet<String>(set);
				set.add(id);
				ids.put(db, set);
			}
			else {
				set.add(id);
			}
		}
		
		@Override
		public Set<String> getDBs() {
			return Collections.unmodifiableSet(ids.keySet());
		}

		@Override
		public Set<String> getIds(String db) {
			Set<String> set = ids.get(db);
			if (set != null) {
				set = Collections.unmodifiableSet(set);
			}
			return set;
		}

		@Override
		public String getSequence() {
			return sequence;
		}
		
	}
	
	private void parseEntryTag(XMLStreamReader parser) throws XMLStreamException {
		Collector c = new Collector();
		c.db = getAttribute(parser, ATTR_source);
		c.id = getAttribute(parser, ATTR_id);
		
		boolean requireMoreInfo = !moreListeners.isEmpty();
		if (requireMoreInfo) {
			c.more = new MoreInfoImpl();
		}
		
		// read 
		while (true) {
			int event = parser.next();
			if (event == XMLStreamConstants.END_ELEMENT) {
				String element = parser.getLocalName();
				if (TAG_entry.equals(element)) {
					break;
				}
			}
			if (event == XMLStreamConstants.START_ELEMENT) {
				String element = parser.getLocalName();
				if (TAG_description.equals(element)) {
					c.comment = parseElementText(parser, TAG_description);
				}
				else if (TAG_AAseq.equals(element)) {
					if (requireMoreInfo) {
						c.more.sequence = parseElementText(parser, TAG_AAseq);
					}
					else {
						skip(parser, TAG_AAseq);
					}
				}
				else if (TAG_DBRef.equals(element)) {
					parseDBRef(parser, c, requireMoreInfo);
				}
				else if (TAG_property.equals(element)) {
					parseProperty(parser, c);
				}
				else {
					skip(parser, element);
				}
			}
		}
		// notify listeners
		for (ProteinListener listener : proteinListeners) {
			listener.handleProtein(c.db, c.id, c.name, c.uniqueName, c.comment, c.taxonId);
		}
		
		if (requireMoreInfo) {
			for (MoreInfoProteinListener listener : moreListeners) {
				listener.handleProtein(c.db, c.id, c.name, c.uniqueName, c.comment, c.taxonId, c.more);
			}
		}
	}
	
	private void parseDBRef(XMLStreamReader parser, Collector c, boolean requireMoreInfo) {
		String source = getAttribute(parser, ATTR_source);
		String id = getAttribute(parser, ATTR_id);
		
		if (KEY_NCBI_TaxID.equals(source)) {
			c.taxonId = id;
		}
		else if (KEY_UniProtKB_ID.equals(source)) {
			c.uniqueName = id;
		}
		else if (requireMoreInfo) {
			c.more.addId(source, id);
		}
	}

	private void parseProperty(XMLStreamReader parser, Collector c) {
		String name = getAttribute(parser, ATTR_name);
		if (KEY_GN.equals(name)) {
			c.name = getAttribute(parser, ATTR_value);
		}
	}

	private String parseElementText(XMLStreamReader parser, String tag) throws XMLStreamException {
		String text = null;
		while (true) {
			switch (parser.next()) {
				case XMLStreamConstants.END_ELEMENT:
					String element = parser.getLocalName();
					if (tag.equals(element)) {
						text = StringUtils.trimToNull(text);
						return text;
					}
					break;
				case XMLStreamConstants.CHARACTERS:
				case XMLStreamConstants.CDATA:
					text = parser.getText();
					break;
				case XMLStreamConstants.START_ELEMENT:
					error("Unexpected element: " + parser.getLocalName(), parser);
					break;
			}
		}
	}
	
	private String getAttribute(XMLStreamReader parser, String attrName) {
		String value = parser.getAttributeValue(null, attrName);
		if (value == null) {
			error("Missing Attribute: " + attrName, parser);
		}
		else if (value.isEmpty()) {
			error("Empty Attribute: " + attrName, parser);
		}
		return value;
	}
	
	private void skip(XMLStreamReader parser, String tag) throws XMLStreamException {
		int count = 1;
		while (true) {
			int event = parser.next();
			if (event == XMLStreamConstants.END_ELEMENT) {
				String element = parser.getLocalName();
				if (tag.equals(element)) {
					count -= 1;
					if (count == 0) {
						return;
					}
				}
			}
			else if (event == XMLStreamConstants.START_ELEMENT) {
				String element = parser.getLocalName();
				if (tag.equals(element)) {
					count += 1;
				}
			}
		}
	}
	
	private void error(String message, XMLStreamReader parser) {
		StringBuilder sb = new StringBuilder();
		sb.append(message);
		Location location = parser.getLocation();
		if (location != null) {
			int lineNumber = location.getLineNumber();
			if (lineNumber >= 0) {
				sb.append(" at line number: ");
				sb.append(lineNumber);
			}
		}
		throw new RuntimeException(sb.toString());
	}
	
}
