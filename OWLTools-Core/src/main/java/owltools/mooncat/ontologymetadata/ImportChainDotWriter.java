package owltools.mooncat.ontologymetadata;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;

/**
 * Rudimentary implementation of a DOT writer for the LEGO annotations in OWL.
 * 
 * Uses the 'docs/lego-owl-mappings.txt' file as guide-line.
 */
public class ImportChainDotWriter {

	private final OWLGraphWrapper graph;

	public static class UnExpectedStructureException extends Exception {


		/**
		 * @param message
		 * @param cause
		 */
		public UnExpectedStructureException(String message, Throwable cause) {
			super(message, cause);
		}

		/**
		 * @param message
		 */
		public UnExpectedStructureException(String message) {
			super(message);
		}

	}

	/**
	 * Create a new writer for the given ontology and reasoner.
	 * 
	 * @param graph
	 */
	public ImportChainDotWriter(OWLGraphWrapper graph) {
		super();
		this.graph = graph;
	}

	public void renderDot(OWLOntology ont, String name, String fn, boolean renderKey)
	throws IOException, UnExpectedStructureException {
		renderDot(ont, ont.getImportsClosure(), name, fn, renderKey);
	}
	public void renderDot(Collection<OWLOntology> onts, String name, String fn, boolean renderKey)
	throws IOException, UnExpectedStructureException {
		renderDot(null, onts, name, fn, renderKey);
	}

	/**
	 * Render a dot file for the given ontologies (aka set of annotations).
	 * 
	 * @param rootOnt 
	 * @param onts
	 * @param name name of the graph to be used in the dot file (optional)
	 * @param fn 
	 * @param renderKey
	 * 
	 * @throws IOException
	 * @throws UnExpectedStructureException thrown, if there are unexpected axioms.
	 */
	public void renderDot(OWLOntology rootOnt, Collection<OWLOntology> onts, String name, String fn, boolean renderKey)
	throws IOException, UnExpectedStructureException {

		try {
			open(fn);
			// start dot
			if (name == null) {
				appendLine("digraph {");
			} else {
				appendLine("digraph " + quote(name) + " {");
			}

			// ont nodes
			Map<String, String> legend = new HashMap<String, String>();
			for (OWLOntology ont : onts) {
				renderontsNode(rootOnt, ont, legend);
			}

			// end dot
			appendLine("}");
		} finally {
			close();
		}

	}

	private void renderontsNode(OWLOntology rootOnt, OWLOntology ont, Map<String, String> legend) throws IOException, UnExpectedStructureException {

		OWLPrettyPrinter owlpp = new OWLPrettyPrinter(graph);
		OWLOntology sourceOntology = graph.getSourceOntology();

		Set<OWLAxiom> axioms = ont.getAxioms();
		Set<OWLImportsDeclaration> imports = ont.getImportsDeclarations();
		Set<OWLAnnotation> oAnns = ont.getAnnotations();
		String title = "";
		String desc = null;

		for (OWLAnnotation a : oAnns) {
			OWLAnnotationProperty p = a.getProperty();
			String ps = p.getIRI().toString();
			OWLAnnotationValue av = a.getValue();
			String v;
			if (av instanceof OWLLiteral) {
				v = ((OWLLiteral)av).getLiteral();
				ps = ps.replaceAll(".*/", "");
				if (ps.equals("title"))
					title = v;
				if (ps.equals("description"))
					desc = v;
			}
		}

		OntologyType type = getontType(ont, axioms);
		if (OntologyType.Unknown == type) {
			throw new UnExpectedStructureException("Could not determine lego type for ont: "+ont+" with Axioms: "+axioms);
		}
		else if (OntologyType.StandardOntology == type) {
			// ontology

			String ontologyName = ont.getOntologyID().getOntologyIRI().toString();
			String url = ontologyName.replaceAll(".owl", "");

			ontologyName = ontologyName.replaceAll("http://purl.obolibrary.org/obo/", "");
			List<OWLClassExpression> cellularLocations = new ArrayList<OWLClassExpression>();

			StringBuilder line = new StringBuilder(nodeId(ont));
			line.append(" [shape=plaintext,");
			line.append("href=\""+url+"\",");
			if (desc != null)
				line.append("tooltip=\""+formatText(desc)+"\",");
			line.append("label=");
			line.append('<'); // start HTML markup
			line.append("<TABLE BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\"><TR>");

			if (ontologyName != null) {
				// render activeEntity as box on top of the activity 
				line.append("<TD>").append(ontologyName).append("</TD></TR><TR>");
			}

			int numLogicalAxioms = ont.getLogicalAxiomCount();
			String color = "lightblue";
			if (numLogicalAxioms == 0) {
				// bridge ontology
				color = "pink";
			}

			line.append("<TD BGCOLOR=\""+color+"\" COLSPAN=\"2\">").append(title).append("</TD>");


			if (numLogicalAxioms > 0) {
				int numClasses = ont.getClassesInSignature().size();
				line.append("<TD BGCOLOR=\"yellow\">").append("LogicAxioms: "+numLogicalAxioms+"<br/>"+"ClassesUsed: "+numClasses).append("</TD>");
			}
			else {

			}

			line.append("</TR>");
			if (ont.equals(rootOnt) && desc != null) {
				
				int LEN = 60;
				line.append("<TR>");
				line.append("<TD COLSPAN=\"3\">");
				char[] sAr = desc.toCharArray();

				int start = 0;
				// start with
				for (int i = LEN; i < sAr.length; i++) {
					if (sAr[i] == ' ') {
						line.append(desc.substring(start, i)+"<BR/>");
						start = i+1;
						i += LEN;
					}
				}
				line.append(desc.substring(start)+"<BR/>");
				line.append("</TD>");
				line.append("</TR>");
			}
			line.append("</TABLE>");
			line.append('>'); // end HTML markup
			line.append("];");

			appendLine("");
			appendLine("// ontology", 1);
			appendLine(line, 1);

		}
		else if (OntologyType.BridgeOntology == type) {
			// TODO
		}


		// render links
		for (OWLImportsDeclaration imp : imports) {
			IRI target = imp.getIRI();
			//String linkLabel = graph.getLabelOrDisplayId(property);
			appendLine("");
			appendLine("// edge", 1);
			appendLine(nodeId(ont)+" -> "+nodeId(target)+" [color=\"#C0C0C0\"];", 1);
			if (!legend.containsKey("imports")) {
				legend.put("imports", "[color=\"#C0C0C0\"]");
			}

		}
	}


	private String formatText(String desc) {
		desc = desc.replaceAll("\n", "</BR>");
		return desc;
	}


	private static class Queue<T> {

		private final Set<T> visited = new HashSet<T>();
		private final LinkedList<T> list = new LinkedList<T>();

		public synchronized T pop() {
			return list.removeFirst();
		}

		public synchronized boolean isEmpty() {
			return list.isEmpty();
		}

		public synchronized void addAll(Collection<T> c) {
			for (T t : c) {
				if (!visited.contains(t)) {
					list.add(t);
					visited.add(t);
				}
			}
		}

		public synchronized void add(T t) {
			if (!visited.contains(t)) {
				list.add(t);
				visited.add(t);
			}
		}
	}


	private CharSequence nodeId(OWLOntology ont) {
		return nodeId(ont.getOntologyID().getOntologyIRI().get());
	}

	private CharSequence nodeId(IRI iri) {
		String iriString = iri.toString();
		return quote(iriString);
	}

	private static enum OntologyType {
		StandardOntology,
		BridgeOntology,
		ImporterOntology,
		Unknown
	}

	// TODO
	private OntologyType getontType(OWLOntology ont, Set<OWLAxiom> axioms) {
		boolean hasEnabledBy = false;
		boolean hasLiteral = false;
		return OntologyType.StandardOntology;
	}

	private CharSequence quote(CharSequence cs) {
		StringBuilder sb = new StringBuilder();
		sb.append('"');
		sb.append(cs);
		sb.append('"');
		return sb;
	}

	private void appendLine(CharSequence line, int indent) throws IOException {
		if (indent <= 0) {
			appendLine(line);
		} else {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < indent; i++) {
				sb.append("    ");
			}
			sb.append(line);
			appendLine(sb);
		}
	}

	BufferedWriter fileWriter = null;

	protected void open(String fn) throws IOException {
		fileWriter = new BufferedWriter(new FileWriter(new File(fn)));
	}

	protected void close() {

		IOUtils.closeQuietly(fileWriter);
	}

	protected void appendLine(CharSequence line) throws IOException {
		System.out.println(line);
		fileWriter.append(line).append('\n');
	}

}
