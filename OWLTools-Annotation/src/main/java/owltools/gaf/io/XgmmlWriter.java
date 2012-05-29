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

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLProperty;

import owltools.gaf.Bioentity;
import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLGraphWrapper.ISynonym;
import owltools.graph.OWLQuantifiedProperty;

/**
 * Generates Xgmml for import into Cytoscape
 */
public class XgmmlWriter extends AbstractXmlWriter {

	static final String RDF_NAMESPACE_URI = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	static final String DC = "http://purl.org/dc/elements/1.1/";
	static final String DEFAULT_INDENT = "    "; // 4 space chars as indent

	public XgmmlWriter() {
		super(DEFAULT_INDENT);
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

			writer.writeStartElement("graph");
			writer.writeAttribute("id","1");
			writer.writeAttribute("label","test");


			//xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
			//xmlns:ns1="http://www.w3.org/1999/xlink" 
			//xmlns:dc="http://purl.org/dc/elements/1.1/" 
			//xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" 
			//xmlns="http://www.cs.rpi.edu/XGMML"

			writer.writeNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
			writer.writeNamespace("ns1", "http://www.w3.org/1999/xlink");
			writer.writeNamespace("dc", DC);
			writer.writeNamespace("rdf", RDF_NAMESPACE_URI);
			writer.writeNamespace("", "http://www.cs.rpi.edu/XGMML");

			/*
			writer.writeStartElement("att");
			writer.writeAttribute("name","networkMetadata");


			writer.writeStartElement(RDF_NAMESPACE_URI, "RDF");
			writer.writeStartElement(RDF_NAMESPACE_URI, "Description");
			writer.writeAttribute(RDF_NAMESPACE_URI,"about",graph.getSourceOntology().getOntologyID().getOntologyIRI().toString());
			writeTag(writer, DC, "type", "ontology");
			writeTag(writer, DC, "format", "Cytoscape-XGMML");
			writer.writeEndElement(); // Description		

			writer.writeEndElement(); // RDF
			writer.writeEndElement(); // att
			 */
			// TODO <att value="#eeffcc" name="backgroundColor"/>
			writeNodes(writer, graph, gafs);
			writeEdges(writer, graph, gafs);
			//			writeEdges();

			writer.writeEndElement(); // graph

			writer.writeEndDocument();
			writer.flush();
		} catch (XMLStreamException e) {
			throw new IOException(e);
		}
	}

	private void writeNodes(XMLStreamWriter writer, final OWLGraphWrapper graph, List<GafDocument> gafs)
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
		for (OWLClass owlClass : allClasses) {
			writeNode(writer, owlClass, allClasses, graph, gafs);
		}
	}
	
	private void writeEdges(XMLStreamWriter writer, final OWLGraphWrapper graph, List<GafDocument> gafs)
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
		for (OWLClass owlClass : allClasses) {
			writeEdges(writer, owlClass, allClasses, graph, gafs);
		}
	}


	public String getId(String in) {
		if (in == null)
			return null;
		return in.replaceAll(":", "_");
	}

	private void writeNode(XMLStreamWriter writer, OWLClass c, List<OWLClass> allClasses, OWLGraphWrapper graph, List<GafDocument> gafs) throws XMLStreamException {

		boolean obsolete = graph.isObsolete(c);
		if (obsolete) {
			return;
		}

		String id = getId(graph.getIdentifier(c));
		String label = graph.getLabel(c);
		if (label == null) {
			label = id;
		}

		writer.writeStartElement("node");
		writer.writeAttribute("id", id);
		writer.writeAttribute("label", label);
		//writer.writeAttribute("name", "base");
		writer.writeStartElement("att");
		writer.writeAttribute("name","size");
		writer.writeAttribute("type","integer");
		writer.writeAttribute("value","24");
		writer.writeEndElement(); // att

		writer.writeStartElement("att");
		writer.writeAttribute("name","label");
		writer.writeAttribute("type","string");
		writer.writeAttribute("value",label);
		writer.writeEndElement(); // att


		// TODO - custom 

		writer.writeEndElement(); // node
	}
	private void writeEdges(XMLStreamWriter writer, OWLClass c, List<OWLClass> allClasses, OWLGraphWrapper graph, List<GafDocument> gafs) throws XMLStreamException {

		String id = getId(graph.getIdentifier(c));
		Set<OWLGraphEdge> edges = graph.getOutgoingEdges(c);
		for (OWLGraphEdge owlGraphEdge : edges) {
			OWLObject target = owlGraphEdge.getTarget();
			if (!allClasses.contains(target))
				continue;
			String tid = getId(graph.getIdentifier(target));
			OWLQuantifiedProperty qp = owlGraphEdge.getSingleQuantifiedProperty();
			OWLProperty p = qp.getProperty();
			String pid;
			if (p == null) {
				pid = qp.getQuantifier().toString();
			}
			else {
				pid = getId(graph.getIdentifier(p));
				if (pid == null) {
					pid = "UNK";
				}
			}
			String eid = id+" ("+pid+") "+tid;
			writer.writeStartElement("edge");
			writer.writeAttribute("id", eid);
			writer.writeAttribute("label", eid);
			writer.writeAttribute("source", id);
			writer.writeAttribute("target", tid);
			writer.writeStartElement("att");
			writer.writeAttribute("name","interaction");
			writer.writeAttribute("value",pid);
			writer.writeEndElement(); // att

			writer.writeEndElement(); // edge

		}
	}

	private static void writeTag(XMLStreamWriter writer, String ns, String tag, String value) throws XMLStreamException {
		if (value != null) {
			writer.writeStartElement(ns, tag);
			writer.writeCharacters(value);
			writer.writeEndElement();
		}
	}

	private static void writeTag(XMLStreamWriter writer, String tag, String value) throws XMLStreamException {
		if (value != null) {
			writer.writeStartElement(tag);
			writer.writeCharacters(value);
			writer.writeEndElement();
		}
	}

}
