package owltools.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLProperty;

import owltools.graph.OWLGraphWrapper;

/**
 * reads in a stanza-formatted file (e.g. http://www.geneontology.org/doc/GO.xrf_abbs)
 * and generates OWL objects for each entry based on a custom mapping
 * 
 * The syntax of the various stanza files used by GO are loose but conform somewhat to
 * the following structure:
 * 
 * (*) Lines beginning with "!"s are ignored (comments)
 * (*) Each stanza is separated by en empty line (in practice the separator line may contain whitespace, and there may be multiple empty lines separating stanzas)
 * (*) Each stanza represents a distinct entity
 * (*) Each stanza is a list of key-value pairs, separated by a ':', usually followed by a single whitespace
 * (*) The key has no whitespace, and generally consists only of alphanumeric characters plus underscore plus '-'
 * 
 * Each stanza is translated to an OWLObject (currently: OWLNamedIndividuals)
 * Each key-val pair is translated into an OWLAxiom (currently: OWLObjectPropertyAssertion, i.e. triple)
 * 
 * The mapping is configurable, but only partially implemented. Each key is associated with a Mapping,
 * which determines whether the key-val pair is translated to a data, object or annotation assertion.
 * 
 * In future, we may allow the mapping to be specified in the file itself (in !comments). For example:
 * 
 * <pre>
 * !Map: database -> http://purl.org/dc/elements/1.1/description
 * </pre>
 * 
 * @author cjm
 *
 */
public class StanzaToOWLConverter {

	private static Logger LOG = Logger.getLogger(StanzaToOWLConverter.class);

	/**
	 * incomplete. Specifies how a key is to be mapped
	 * 
	 * @author cjm
	 *
	 */
	public class Mapping {
		public boolean isToObject = false; // if true, ObjectPropertyAssertion axioms are generated
		public boolean isAnnotation = false; // TODO - implement
		public OWLLiteral literalPrototype; // template for the literal type - TODO - implement
		public OWLProperty property; // property to use in mapping
		public String toString() {
			if (property != null)
				return "P="+property.toString();
			else
				return super.toString();
		}
	}
	
	/**
	 * 
	 * 
	 * @author cjm
	 *
	 */
	public class Config {
		public boolean isStrict = false; // throws errors if parse problems found
		public boolean isOboIdentifiers = true; // true if any IDs to be resolved are OBO-style
		public String defaultPrefix = null; // can be URI prefix or OBO short prefix (if latter, use trailing ':')
		public String idField = null; // no standard key for ID. If not set, assume first
		public Map<String,Mapping> keyMap = new HashMap<String,Mapping>(); // how to treat each key in the stanza file
		public OWLOntology targetOntology; // new axioms are added here
	}
	
	/**
	 * A stanza is set of key-value pairs
	 * E.g
	 * <pre>
	 * abbreviation: AgBase
	 * 	database: AgBase resource for functional analysis of agricultural plant and animal gene products
	 * generic_url: http://www.agbase.msstate.edu/
	 * url_syntax: http://www.agbase.msstate.edu/cgi-bin/getEntry.pl?db_pick=[ChickGO/MaizeGO]&uid=[example_id]
	 * <pre>
	 * 
	 * @author cjm
	 *
	 */
	public class Stanza {
		Map<String,String> keyValMap = new HashMap<String,String>();
	}

	public Config config = new Config();
	public OWLGraphWrapper graph;



	public StanzaToOWLConverter(OWLGraphWrapper graph) {
		super();
		this.graph = graph;
		setDefaults();
	}
	
	public void setDefaults() {
		config.targetOntology = graph.getSourceOntology();
		Map<String, Mapping> km = config.keyMap;
	}

	/**
	 * parses a stanza file and generates axioms in targetOntology
	 * 
	 * @param fn
	 * @throws IOException
	 */
	public void parse(String fn) throws IOException {
		File myFile = new File(fn);
		parse(myFile);
	}
	
	public void parse(File myFile) throws IOException {
		Set<Stanza> stanzas = new HashSet<Stanza>();
		Stanza currentStanza = new Stanza();
		
		FileReader fileReader = new FileReader(myFile);
		BufferedReader reader = new BufferedReader(fileReader);
		String line;
		while ((line = reader.readLine()) != null) {
			if (line.startsWith("!"))
				continue;
			line = line.replaceAll("\\s+$", ""); // remove trailing newline
			if (line.length() == 0) {
				stanzas.add(currentStanza);
				currentStanza = new Stanza();
				LOG.info("Parsed stanza");
				continue;
			}
			int pos = line.indexOf(":");
			
			if (pos < 0) {
				warn("Cannot parse: '"+line+"'");
				continue;
			}
			String k = line.substring(0, pos);
			String v = line.substring(pos+1);
			v = v.replaceAll("^\\s+", ""); // remove leading whitespace
			if (config.idField == null && currentStanza.keyValMap.size() == 0) {
				// assume first line has ID
				config.idField = k;
				LOG.info("Using "+k+" for ID field");
			}
			currentStanza.keyValMap.put(k, v);
		}
		if (currentStanza.keyValMap.size() > 0)
			stanzas.add(currentStanza);

		for (Stanza s : stanzas) {
			translate(s);
		}

	}


	/**
	 * Translates a stanza to axioms (triples) and adds these to the targetOntology
	 * 
	 * @param s
	 */
	public void translate(Stanza s) {
		// TODO Auto-generated method stub
		Map<String, String> kvm = s.keyValMap;
		Map<String, Mapping> km = config.keyMap;
		if (kvm.size() == 0) {
			LOG.info("empty stanza");
			return;
		}
		String idField = config.idField;
		OWLDataFactory df = graph.getDataFactory();
		Set<OWLAxiom> axs = new HashSet<OWLAxiom>();

		OWLNamedIndividual obj = resolveIndividual(kvm.get(idField));
		axs.add(df.getOWLDeclarationAxiom(obj));
		
		for (String k : kvm.keySet()) {
			if (k.equals(idField)) {
				continue;
			}
			String v = kvm.get(k);
			Mapping m = null;
			if (!km.containsKey(k)) {
				m = new Mapping();
			}
			else {
				m = km.get(k);
			}
			LOG.info("Mapping: "+m);
			
			Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
			if (m.isToObject) {
				if (m.property == null)
					m.property = df.getOWLObjectProperty( this.resolveIRI(k) );

				OWLNamedIndividual val = this.resolveIndividual(v);
				axioms.add(df.getOWLObjectPropertyAssertionAxiom((OWLObjectPropertyExpression) m.property, obj, val));
				
			}
			else {
				if (m.property == null)
					m.property = df.getOWLDataProperty( this.resolveIRI(k) );

				// TODO - allow different types of literal
				OWLLiteral val = df.getOWLLiteral(v);
				axioms.add(df.getOWLDataPropertyAssertionAxiom((OWLDataPropertyExpression) m.property, obj, val));
			}
			graph.getManager().addAxioms(config.targetOntology, axioms);
		}	
	}

	
	private IRI resolveIRI(String id) {
		IRI iri;
		if (id.indexOf(":") < 0 && config.defaultPrefix != null) {
			id = config.defaultPrefix + id;
		}
		if (config.isOboIdentifiers && !id.startsWith("http:/")) {
			iri = graph.getIRIByIdentifier(id);
		}
		else {
			iri = IRI.create(id);
		}
		return iri;
	}


	// translates id to IRI if required.
	// always returns an OWLIndividual, even if not previously declared
	private OWLNamedIndividual resolveIndividual(String id) {
		IRI iri = resolveIRI(id);
		OWLNamedIndividual c= graph.getDataFactory().getOWLNamedIndividual(iri);
		return c;
	}
	
	private void warn(String message) throws IOException {
		LOG.warn(message);
		if (config.isStrict) {
			throw new IOException(message);
		}
		
	}

	
}
