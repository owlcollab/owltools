package owltools.gaf.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.StringUtils;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import owltools.gaf.Bioentity;
import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLGraphWrapper.ISynonym;
import owltools.graph.OWLQuantifiedProperty;

/**
 * Java implementation to create a legacy pseudo RDF XML file for GO 
 * terms and gene annotations.
 */
public class PseudoRdfXmlWriter extends AbstractXmlWriter {
	
	/**
	 * DTD location for the GO RDF XML format
	 */
	static final String GO_RDF_XML_DTD = "http://www.geneontology.org/dtds/go.dtd";
	
	static final String RDF_NAMESPACE_URI = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	static final String GO_NAMESPACE_URI = GO_RDF_XML_DTD+"#";
	static final String DEFAULT_INDENT = "    "; // 4 space chars as indent
	
	private ProgressReporter progressReporter;
	
	public PseudoRdfXmlWriter() {
		super(DEFAULT_INDENT);
	}

	public void setProgressReporter(ProgressReporter reporter) {
		this.progressReporter = reporter;
	}
	
	public static interface ProgressReporter {
		
		public void report(int count, int total);
	}
	
	/**
	 * Write a pseudo RDF XML for the given ontology and gene annotations.
	 * 
	 * @param stream
	 * @param graph the ontology
	 * @param gafs (optional) list of gaf documents or null
	 * @throws IOException
	 */
	public void write(OutputStream stream, OWLGraphWrapper graph, List<GafDocument> gafs) throws IOException {
		try {
			XMLStreamWriter writer = createWriter(stream);
			writer.writeStartDocument();
			writer.writeDTD("\n<!DOCTYPE go:go PUBLIC \"-//Gene Ontology//Custom XML/RDF Version 2.0//EN\" \""+GO_RDF_XML_DTD+"\">\n");
			
			writer.writeStartElement("go:go");
			writer.writeNamespace("go", GO_NAMESPACE_URI);
			writer.writeNamespace("rdf", RDF_NAMESPACE_URI);
			
			writer.writeStartElement(RDF_NAMESPACE_URI, "RDF");
			
			writeTerms(writer, graph, gafs);
			
			writer.writeEndElement(); // RDF
			writer.writeEndElement(); // go:go
			
			writer.writeEndDocument();
			writer.flush();
		} catch (XMLStreamException e) {
			throw new IOException(e);
		}
	}

	private void writeTerms(XMLStreamWriter writer, final OWLGraphWrapper graph, List<GafDocument> gafs)
			throws XMLStreamException {
		
		// get all classes and sort them according to the lexical order of their OBO identifiers
		List<OWLClass> allClasses = new ArrayList<OWLClass>();
		for(OWLObject owlObject : graph.getAllOWLObjects()) {
			if (owlObject instanceof OWLClass && graph.getIdentifier(owlObject) != null) {
				allClasses.add((OWLClass) owlObject);
			}
		}
		Collections.sort(allClasses, new Comparator<OWLClass>() {

			@Override
			public int compare(OWLClass c1, OWLClass c2) {
				String id1 = graph.getIdentifier(c1);
				String id2 = graph.getIdentifier(c2);
				return id1.compareTo(id2);
			}
		});
		
		// write each term and corresponding gene annotations
		int total = allClasses.size();
		int count = 0;
		for (OWLClass owlClass : allClasses) {
			writeTerm(writer, owlClass, graph, gafs);
			if (progressReporter != null) {
				count += 1;
				progressReporter.report(count, total);
			}
		}
	}
	
	private void writeTerm(XMLStreamWriter writer, OWLClass c, OWLGraphWrapper graph, List<GafDocument> gafs) throws XMLStreamException {
		writer.writeStartElement(GO_NAMESPACE_URI, "term");
		String accession = graph.getIdentifier(c);
		writer.writeAttribute(RDF_NAMESPACE_URI, "about", "http://www.geneontology.org/go#"+accession);
		
		// go:accession
		writeGoTag(writer, "accession", accession);
		
		// go:name
		writeGoTag(writer, "name", graph.getLabel(c));
		
		// go:synonym*
		writeSynonyms(writer, c, graph);
		
		// go:definition?
		writeGoTag(writer, "definition", graph.getDef(c));
		
		// go:comment*
		writeGoTag(writer, "comment", graph.getComment(c));
		
		// (go:part_of | go:is_a | go:negatively_regulates | go:positively_regulates | go:regulates)*
		writeRelations(writer, c, graph);
		
		// go:dbxref*
		List<String> xrefs = graph.getXref(c);
		if (xrefs != null && !xrefs.isEmpty()) {
			for(String xref : xrefs) {
				writeDbXref(writer, xref);
			}
		}
		
		
		// go:association*
		if(gafs != null && !gafs.isEmpty()) {
			writeAssociations(writer, c, accession, graph, gafs);
		}
		
		// go:history*
		// NOT used
		
		// go:is_obsolete*
		// WARNING: primary marker for obsolete terms is a fake relation to an obsolete node
		// adding explicit tag as extra help, since it is already defined in the DTD
		boolean obsolete = graph.isObsolete(c);
		if (obsolete) {
			writeGoTag(writer, "is_obsolete", Boolean.TRUE.toString());
		}
		
		writer.writeEndElement(); // term
	}
	
	private void writeSynonyms(XMLStreamWriter writer, OWLClass c, OWLGraphWrapper graph) throws XMLStreamException {
		List<ISynonym> synonyms = graph.getOBOSynonyms(c);
		if (synonyms != null && !synonyms.isEmpty()) {
			// sort synonym labels and avoid duplicates
			SortedSet<String> strings = new TreeSet<String>();
			for (ISynonym synonym : synonyms) {
				strings.add(synonym.getLabel());
			}
			for (String string : strings) {
				writeGoTag(writer, "synonym", string);
			}
		}
		
	}
	
	void writeRelations(XMLStreamWriter writer, OWLClass c, OWLGraphWrapper graph) throws XMLStreamException {
		// go:part_of | go:is_a | go:negatively_regulates | go:positively_regulates | go:regulates
		
		// sort relations to provide a deterministic output file
		SortedSet<String> is_a_rels = new TreeSet<String>();
		SortedSet<String> part_of_rels = new TreeSet<String>();
		SortedSet<String> negatively_regulates_rels = new TreeSet<String>();
		SortedSet<String> positively_regulates_rels = new TreeSet<String>();
		SortedSet<String> regulates_rels = new TreeSet<String>();
		
		
		Set<OWLGraphEdge> edges = graph.getOutgoingEdges(c);
		for (OWLGraphEdge owlGraphEdge : edges) {
			OWLObject target = owlGraphEdge.getTarget();
			
			List<OWLQuantifiedProperty> propertyList = owlGraphEdge.getQuantifiedPropertyList();
			if ("GO:0006417".equals(graph.getIdentifier(c))) {
				System.out.println();
			}
			boolean is_a = false;
			boolean part_of = false;
			boolean negatively_regulates = false;
			boolean positively_regulates = false;
			boolean regulates = false;
			for (OWLQuantifiedProperty property : propertyList) {
				OWLObjectProperty objectProperty = property.getProperty();
				if (objectProperty != null) {
					String label = graph.getLabel(objectProperty);
					if ("part_of".equals(label) || "part of".equals(label)) {
						part_of = true;
						break;
					}
					else if ("negatively_regulates".equals(label) || "negatively regulates".equals(label)) {
						negatively_regulates = true;
						break;
					}
					else if ("positively_regulates".equals(label) || "positively regulates".equals(label)) {
						positively_regulates = true;
						break;
					}
					else if ("regulates".equals(label)) {
						regulates = true;
						break;
					}
				}
				else {
					is_a =  property.isSubClassOf();
				}
			}
			if (negatively_regulates) {
				negatively_regulates_rels.add(graph.getIdentifier(target));
			}
			else if (positively_regulates) {
				positively_regulates_rels.add(graph.getIdentifier(target));
			}
			else if (regulates) {
				regulates_rels.add(graph.getIdentifier(target));
			}
			else if (part_of) {
				part_of_rels.add(graph.getIdentifier(target));
			}
			else if (is_a) {
				is_a_rels.add(graph.getIdentifier(target));
			}
		}
		
		// sort by type of relation
		writeRelation(writer, "is_a", is_a_rels);
		writeRelation(writer, "part_of", part_of_rels);
		writeRelation(writer, "regulates", regulates_rels);
		writeRelation(writer, "positively_regulates", positively_regulates_rels);
		writeRelation(writer, "negatively_regulates", negatively_regulates_rels);
		
		// Fake obsolete marker via relation ship to go-namespace specific obsolete nodes
		
		// <go:is_a rdf:resource="http://www.geneontology.org/go#obsolete_biological_process" />
		// <go:is_a rdf:resource="http://www.geneontology.org/go#obsolete_molecular_function" />
		// <go:is_a rdf:resource="http://www.geneontology.org/go#obsolete_cellular_component" />
		
		if (graph.isObsolete(c)) {
			String namespace = graph.getNamespace(c);
			if (namespace != null) {
				namespace = namespace.toLowerCase();
				if (namespace.equals("biological_process") 
						|| namespace.equals("molecular_function") 
						|| namespace.equals("cellular_component")) {
					writeRelation(writer, "is_a", "obsolete_" + namespace);
				}
			}
		}
	}
	
	private void writeRelation(XMLStreamWriter writer, String rel, Iterable<String> targetIds) throws XMLStreamException {
		for (String targetId : targetIds) {
			writeRelation(writer, rel, targetId);
		}
	}
	
	private void writeRelation(XMLStreamWriter writer, String rel, String targetId) throws XMLStreamException {
		writer.writeEmptyElement(GO_NAMESPACE_URI, rel);
		writer.writeAttribute(RDF_NAMESPACE_URI, "resource", "http://www.geneontology.org/go#" + targetId);
	}

	private void writeDbXref(XMLStreamWriter writer, String xref) throws XMLStreamException {
		if (xref == null || xref.isEmpty()) {
			// ignore empty xref
			return;
		}
		int pos = xref.indexOf(':');
		if (pos > 0 && (pos + 2) < xref.length()) {
			String dataBaseSymbol = xref.substring(0, pos);
			String reference = xref.substring(pos + 1);
			
			writeDbXref(writer, dataBaseSymbol, reference);
		}
	}

	private void writeDbXref(XMLStreamWriter writer, String dataBaseSymbol, String reference) throws XMLStreamException {
		writer.writeStartElement(GO_NAMESPACE_URI, "dbxref");
		writer.writeAttribute(RDF_NAMESPACE_URI, "parseType", "Resource");

		writeGoTag(writer, "database_symbol", dataBaseSymbol);
		writeGoTag(writer, "reference", reference);

		writer.writeEndElement();
	}
	
	private static final Set<String> EVIDENCE_CODES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
			"IEA", "IMP", "IGI", "IPI", "ISS", "IDA", "IEP", "TAS",  "NAS", "IC", "ND", "NR", "RCA", "NULL")));
	
	private void writeAssociations(XMLStreamWriter writer, OWLClass c, String accession, final OWLGraphWrapper graph, List<GafDocument> gafs)
			throws XMLStreamException {
		
		// get corresponding gene annotations for the term
		for(GafDocument gaf : gafs) {
			for(GeneAnnotation ann : gaf.getGeneAnnotationsByDirectGoCls(accession)) {
				writer.writeStartElement(GO_NAMESPACE_URI, "association");
				writer.writeAttribute(RDF_NAMESPACE_URI, "parseType", "Resource");
				
				//go:evidence+
				String evidenceCls = ann.getShortEvidence();
				if (evidenceCls == null) {
					evidenceCls = "NULL";
				}
				else {
					evidenceCls = evidenceCls.toUpperCase();
					if(!EVIDENCE_CODES.contains(evidenceCls)) {
						evidenceCls = "NULL";
					}
				}
				String xref = "";
				List<String> referenceIds = ann.getReferenceIds();
				if (referenceIds != null && !referenceIds.isEmpty()) {
					xref = StringUtils.join(referenceIds, '|');
				}
				
				writer.writeStartElement(GO_NAMESPACE_URI, "evidence");
				writer.writeAttribute("evidence_code", evidenceCls);
				writeDbXref(writer, xref);
				writer.writeEndElement(); // evidence
				
				//go:gene_product
				Bioentity bioentity = ann.getBioentityObject();
				writer.writeStartElement(GO_NAMESPACE_URI, "gene_product");
				writer.writeAttribute(RDF_NAMESPACE_URI, "parseType", "Resource");
				
				writeGoTag(writer, "name", bioentity.getFullName());
				writeDbXref(writer, bioentity.getId());
				
				writer.writeEndElement(); // gene_product
				
				writer.writeEndElement(); // association
			}
		}
		
	}

	private static void writeGoTag(XMLStreamWriter writer, String tag, String value) throws XMLStreamException {
		if (value != null) {
			writer.writeStartElement(GO_NAMESPACE_URI, tag);
			writer.writeCharacters(value);
			writer.writeEndElement();
		}
	}
	
}
