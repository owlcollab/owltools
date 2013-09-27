package org.geneontology.lego.dot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geneontology.lego.model.LegoLink;
import org.geneontology.lego.model.LegoNode;
import org.geneontology.lego.model.LegoTools;
import org.geneontology.lego.model.LegoTools.UnExpectedStructureException;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;

/**
 * Rudimentary implementation of a DOT writer for the LEGO annotations in OWL.
 */
public abstract class LegoDotWriter implements LegoRenderer {

	private static final String MF_COLOR = "lightblue";
	private static final String CMF_COLOR = "lightsalmon";
	private static final String CC_COLOR = "yellow";
	
	private final OWLGraphWrapper graph;
	private final OWLReasoner reasoner;

	/**
	 * Create a new writer for the given ontology and reasoner.
	 * 
	 * @param graph
	 * @param reasoner
	 */
	public LegoDotWriter(OWLGraphWrapper graph, OWLReasoner reasoner) {
		super();
		this.graph = graph;
		this.reasoner = reasoner;
	}
	
	@Override
	public void render(Collection<OWLNamedIndividual> individuals, String name, boolean renderKey)
			throws IOException, UnExpectedStructureException
	{
		LegoTools tools = new LegoTools(graph, reasoner);
		Collection<LegoNode> nodes = tools.createLegoNodes(individuals);
		renderLego(nodes, name, renderKey);
	}
	
	/**
	 * Render the given collection of nodes ({@link LegoNode}) 
	 * 
	 * @param nodes
	 * @param name name of the graph (optional)
	 * @param renderKey
	 * @throws IOException
	 */
	public void renderLego(Collection<LegoNode> nodes, String name, boolean renderKey)
			throws IOException 
	{
		OWLPrettyPrinter owlpp = new OWLPrettyPrinter(graph);
		try {
			open();
			// start dot
			if (name == null) {
				appendLine("digraph {");
			} else {
				appendLine("digraph " + quote(name) + " {");
			}

			// individual nodes
			Map<String, String> legend = new HashMap<String, String>();
			for (LegoNode node : nodes) {
				renderNodeAndLinks(node, owlpp, legend);
			}
			if (!legend.isEmpty() && renderKey) {
				appendLine("");
				appendLine("// Key / Legend",1);
				appendLine("subgraph {", 1);
				List<String> legendKeys = new ArrayList<String>(legend.keySet());
				Collections.sort(legendKeys);
				CharSequence prev = null;
				for(String relName : legendKeys) {
					final CharSequence a = quote("legend_"+relName+"_A");
					final CharSequence b = quote("legend_"+relName+"_B");
					
					appendLine(a+"[shape=plaintext,label=\"\"];", 2);
					appendLine(b+"[shape=plaintext,label="+quote(relName)+"];", 2);
					appendLine(a+" -> "+b+" "+legend.get(relName)+";", 2);
					appendLine("");
					if (prev != null) {
						appendLine("// create invisibe edge for top down order", 2);
						appendLine(prev+" -> "+a+" [style=invis];", 2);
						appendLine("");
					}
					prev = b;
				}
				appendLine("}", 1);
			}

			// end dot
			appendLine("}");
		} finally {
			close();
		}
		
	}
	
	private void renderNodeAndLinks(LegoNode node, OWLPrettyPrinter owlpp, Map<String, String> legend) throws IOException {
		
		// node
		renderLegoNode(node, owlpp);
		
		// render links
		Collection<LegoLink> links = node.getLinks();
		if (links != null) {
			final CharSequence source = nodeId(node.getId());
			for (LegoLink link : links) {
				CharSequence target = nodeId(link.getNamedTarget());
				String linkLabel = graph.getLabelOrDisplayId(link.getProperty());
				if ("directly_inhibits".equals(linkLabel)) {
					appendLine("");
					appendLine("// edge", 1);
					appendLine(source+" -> "+target+" [arrowhead=tee];", 1);
					if (!legend.containsKey("directly_inhibits")) {
						legend.put("directly_inhibits", "[arrowhead=tee]");
					}
				}
				else if ("part_of".equals(linkLabel) || "part of".equals(linkLabel)) {
					appendLine("");
					appendLine("// edge", 1);
					appendLine(source+" -> "+target+" [color=\"#C0C0C0\"];", 1);
					if (!legend.containsKey("part_of")) {
						legend.put("part_of", "[color=\"#C0C0C0\"]");
					}
				}
				else if ("directly_activates".equals(linkLabel)) {
					appendLine("");
					appendLine("// edge", 1);
					appendLine(source+" -> "+target+" [arrowhead=open];", 1);
					if (!legend.containsKey("directly_activates")) {
						legend.put("directly_activates", "[arrowhead=open]");
					}
				}
				else {
					appendLine("");
					appendLine("// edge", 1);
					appendLine(source+" -> "+target+" [label="+quote(linkLabel)+"];", 1);
				}
			}
		}
	}

	private void renderLegoNode(LegoNode node, OWLPrettyPrinter owlpp) throws IOException {
		
		CharSequence label;
		// render node
		if (node.getType() == null) {
			label="?";
		}
		else {
			label = getLabel(node.getType(), owlpp);
		}
		
		StringBuilder line = new StringBuilder(nodeId(node.getId()));
		line.append(" [shape=plaintext,label=");
		line.append('<'); // start HTML markup
		
		line.append("<TABLE BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\">");
		
		OWLClass activeEntity = node.getActiveEntity();
		if (activeEntity != null) {
			// render activeEntity as box on top of the activity 
			line.append("<TR><TD>").append(graph.getLabelOrDisplayId(activeEntity)).append("</TD></TR>");
		}
		if (node.isCmf()) {
			line.append("<TR><TD BGCOLOR=\""+CMF_COLOR+"\" COLSPAN=\"2\">").append(label).append("</TD>");
		}
		else if (node.isMf()) {
			line.append("<TR><TD BGCOLOR=\""+MF_COLOR+"\" COLSPAN=\"2\">").append(label).append("</TD>");
		}
		else {
			line.append("<TR><TD COLSPAN=\"2\">").append(label).append("</TD>");
		}
		
		Collection<OWLClassExpression> cellularLocations = node.getCellularLocation();
		if (cellularLocations != null) {
			for(OWLClassExpression cellularLocation : cellularLocations) {
				String location;
				if (!cellularLocation.isAnonymous()) {
					location = graph.getLabelOrDisplayId(cellularLocation.asOWLClass());
				}
				else {
					location = owlpp.render(cellularLocation);
				}
				line.append("<TD BGCOLOR=\""+CC_COLOR+"\">").append(location).append("</TD>");
			}
		}
		line.append("</TR>");
		Collection<OWLClassExpression> unknowns = node.getUnknowns();
		if (unknowns != null && !unknowns.isEmpty()) {
			line.append("<TR><TD COLSPAN=\"2\">");
			line.append("<TABLE BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\">");
			for (OWLClassExpression expression : unknowns) {
				renderAdditionalNodeExpression(line, expression, owlpp);
			}
			line.append("</TABLE>");
			line.append("</TD></TR>");
		}
		line.append("</TABLE>");
		line.append('>'); // end HTML markup
		line.append("];");
		
		appendLine("");
		if (node.isMf() || node.isCmf()) {
			appendLine("// annoton", 1);
		}
		else {
			appendLine("// context", 1);
		}
		appendLine(line, 1);
		
	}
	
	private void renderAdditionalNodeExpression(StringBuilder line, OWLClassExpression expression, OWLPrettyPrinter owlpp) {
		if (expression instanceof OWLObjectSomeValuesFrom) {
			OWLObjectSomeValuesFrom object = (OWLObjectSomeValuesFrom) expression;
			OWLObjectPropertyExpression property = object.getProperty();
			OWLClassExpression filler = object.getFiller();
			line.append("<TR><TD>");
			line.append(getLabel(property, owlpp));
			line.append("</TD><TD>");
			line.append(getLabel(filler, owlpp));
			line.append("</TD></TR>");
		}
		else {
			line.append("<TR><TD COLSPAN=\"2\">");
			line.append(getLabel(expression, owlpp));
			line.append("</TD></TR>");
		}
	}
	
	private CharSequence getLabel(OWLClassExpression expression, OWLPrettyPrinter owlpp) {
		if (expression instanceof OWLClass) {
			return insertLineBrakes(graph.getLabelOrDisplayId(expression));
		}
		else if (expression instanceof OWLObjectIntersectionOf) {
			StringBuilder sb = new StringBuilder();
			OWLObjectIntersectionOf intersectionOf = (OWLObjectIntersectionOf) expression;
			sb.append("<TABLE>");
			for (OWLClassExpression ce : intersectionOf.getOperands()) {
				sb.append("<TR><TD>");
				if (ce instanceof OWLClass) {
					sb.append(insertLineBrakes(graph.getLabelOrDisplayId((OWLClass)ce)));
				}
				else if (ce instanceof OWLObjectSomeValuesFrom){
					OWLObjectSomeValuesFrom some = (OWLObjectSomeValuesFrom) ce;
					OWLObjectPropertyExpression property = some.getProperty();
					if (property.isAnonymous()) {
						sb.append(owlpp.render(property));
					}
					else {
						sb.append(insertLineBrakes(graph.getLabelOrDisplayId(property.asOWLObjectProperty())));
					}
					sb.append(" <B>some</B> ");
					OWLClassExpression filler = some.getFiller();
					if (filler instanceof OWLClass) {
						sb.append(insertLineBrakes(graph.getLabelOrDisplayId((OWLClass)filler)));
					}
					else {
						sb.append(insertLineBrakes(escape(owlpp.render(filler))));
					}
				}
				else {
					sb.append(insertLineBrakes(escape(ce.toString())));
				}
				sb.append("</TD></TR>");
			}
			sb.append("</TABLE>");
			return sb.toString();
		}
		return insertLineBrakes(escape(owlpp.render(expression)));
	}
	
	private String escape(CharSequence cs) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < cs.length(); i++) {
			char c = cs.charAt(i);
			if (c == '<') {
				sb.append("&lt;");
			}
			else if (c == '>') {
				sb.append("&gt;");
			}
			else if (c == '&') {
				sb.append("&amp;");
			}
			else {
				sb.append(c);
			}
		}
		return sb.toString();
	}
	
	private CharSequence getLabel(OWLObjectPropertyExpression expression, OWLPrettyPrinter owlpp) {
		if (expression.isAnonymous()) {
			return insertLineBrakes(escape(owlpp.render(expression)));
		}
		return insertLineBrakes(graph.getLabelOrDisplayId(expression));
	}

	static final int DEFAULT_LINE_LENGTH = 60;
	
	private CharSequence insertLineBrakes(String s) {
		return StringTools.insertLineBrakes(s, DEFAULT_LINE_LENGTH, "<BR/>");
	}
	
	private CharSequence nodeId(OWLNamedIndividual individual) {
		return nodeId(individual.getIRI());
	}
	
	private CharSequence nodeId(IRI iri) {
		String iriString = iri.toString();
		return quote(iriString);
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

	/**
	 * Overwrite this method to implement a custom open() routine for the writer.
	 * 
	 * @throws IOException
	 */
	protected void open() throws IOException {
		// default: do nothing
	}
	
	/**
	 * Write a line to the file. It is expected that the writer appends the
	 * appropriate newlines. They are not part of the line.
	 * 
	 * @param line
	 * @throws IOException
	 */
	protected abstract void appendLine(CharSequence line) throws IOException;
	
	
	/**
	 * Overwrite this method to implement a custom close() function for the writer.
	 * Guaranteed to be called, also in case of an Exception (try-finally pattern).
	 */
	protected void close() {
		// default: do nothing
	}

}
