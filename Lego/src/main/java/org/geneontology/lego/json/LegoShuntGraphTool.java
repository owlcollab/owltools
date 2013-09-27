package org.geneontology.lego.json;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.geneontology.lego.model.LegoLink;
import org.geneontology.lego.model.LegoNode;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;

import owltools.graph.OWLGraphWrapper;
import owltools.graph.shunt.OWLShuntEdge;
import owltools.graph.shunt.OWLShuntGraph;
import owltools.graph.shunt.OWLShuntNode;
import owltools.io.OWLPrettyPrinter;

/**
 * Helper to create a shut graph for a given set of lego nodes.<br>
 * <br>
 * This creates nodes and edges, where the nodes are the individuals with a
 * simple label. The html label and additional information is provided as meta
 * data. The edges are relations between the individuals and usually have edge
 * label.
 */
public class LegoShuntGraphTool {

	private static final String MF_COLOR = "lightblue";  // molecular function
	private static final String CMF_COLOR = "lightsalmon"; // complex molecular function 
	private static final String CC_COLOR = "yellow"; // cellular component(s)
	
	/**
	 * Render the given collection of nodes ({@link LegoNode}) 
	 * 
	 * @param nodes
	 * @param graph 
	 * @return shuntGraph
	 */
	public OWLShuntGraph renderLego(Collection<LegoNode> nodes, OWLGraphWrapper graph) {
		OWLPrettyPrinter owlpp = new OWLPrettyPrinter(graph);
		OWLShuntGraph shuntGraph = new OWLShuntGraph();
		for (LegoNode node : nodes) {
			// node
			renderLegoNode(node, owlpp, graph, shuntGraph);
			
			// render links
			Collection<LegoLink> links = node.getLinks();
			if (links != null) {
				final String source = nodeId(node.getId());
				for (LegoLink link : links) {
					String target = nodeId(link.getNamedTarget());
					String linkLabel = graph.getLabelOrDisplayId(link.getProperty());
					OWLShuntEdge e = new OWLShuntEdge(source, target, linkLabel);
					shuntGraph.addEdge(e);
				}
			}
		}
		return shuntGraph;
	}
	
	private void renderLegoNode(LegoNode node, OWLPrettyPrinter owlpp, OWLGraphWrapper graph, OWLShuntGraph shuntGraph) {
		
		OWLShuntNode shuntNode = new OWLShuntNode(nodeId(node.getId()));
		String label;
		// render node
		if (node.getType() == null) {
			label="?";
		}
		else {
			label = getLabel(node.getType(), owlpp, graph);
		}
		shuntNode.setLabel(label);
		Map<String, String> metaData = new HashMap<String, String>();
		
		StringBuilder line = new StringBuilder();
		
		line.append("<TABLE BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\">");
		
		OWLClass activeEntity = node.getActiveEntity();
		if (activeEntity != null) {
			// render activeEntity as box on top of the activity
			String activeEntityLabel = graph.getLabelOrDisplayId(activeEntity);
			line.append("<TR><TD>").append(activeEntityLabel).append("</TD></TR>");
			metaData.put("activeEntity", graph.getIdentifier(activeEntity));
			metaData.put("activeEntityLabel", activeEntityLabel);
		}
		if (node.isCmf()) {
			// composed molecular function
			line.append("<TR><TD BGCOLOR=\""+CMF_COLOR+"\" COLSPAN=\"2\">").append(label).append("</TD>");
			metaData.put("isCmf", Boolean.toString(true));
		}
		else if (node.isMf()) {
			// molecular function
			line.append("<TR><TD BGCOLOR=\""+MF_COLOR+"\" COLSPAN=\"2\">").append(label).append("</TD>");
			metaData.put("isMf", Boolean.toString(true));
		}
		else {
			line.append("<TR><TD COLSPAN=\"2\">").append(label).append("</TD>");
		}
		
		Collection<OWLClassExpression> cellularLocations = node.getCellularLocation();
		if (cellularLocations != null) {
			int count = 0;
			for(OWLClassExpression cellularLocation : cellularLocations) {
				String location;
				if (!cellularLocation.isAnonymous()) {
					location = graph.getLabelOrDisplayId(cellularLocation.asOWLClass());
				}
				else {
					location = owlpp.render(cellularLocation);
				}
				line.append("<TD BGCOLOR=\""+CC_COLOR+"\">").append(location).append("</TD>");
				metaData.put("cellularLocation"+count, location);
				count += 1;
			}
			metaData.put("cellularLocationCount", Integer.toString(count));
		}
		line.append("</TR>");
		Collection<OWLClassExpression> unknowns = node.getUnknowns();
		if (unknowns != null && !unknowns.isEmpty()) {
			line.append("<TR><TD COLSPAN=\"2\">");
			line.append("<TABLE BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\">");
			for (OWLClassExpression expression : unknowns) {
				renderAdditionalNodeExpression(line, expression, owlpp, graph);
			}
			line.append("</TABLE>");
			line.append("</TD></TR>");
		}
		line.append("</TABLE>");
		
		metaData.put("html", line.toString());
		
		if (node.isMf() || node.isCmf()) {
			metaData.put("isAnnoton", Boolean.TRUE.toString());
		}
		else {
			metaData.put("isContext", Boolean.TRUE.toString());
		}
		
		
		shuntNode.setMetadata(metaData);
		shuntGraph.addNode(shuntNode);
		
	}
	
	private void renderAdditionalNodeExpression(StringBuilder line, OWLClassExpression expression, OWLPrettyPrinter owlpp, OWLGraphWrapper graph) {
		if (expression instanceof OWLObjectSomeValuesFrom) {
			OWLObjectSomeValuesFrom object = (OWLObjectSomeValuesFrom) expression;
			OWLObjectPropertyExpression property = object.getProperty();
			OWLClassExpression filler = object.getFiller();
			line.append("<TR><TD>");
			line.append(getLabel(property, owlpp, graph));
			line.append("</TD><TD>");
			line.append(getLabel(filler, owlpp, graph));
			line.append("</TD></TR>");
		}
		else {
			line.append("<TR><TD COLSPAN=\"2\">");
			line.append(getLabel(expression, owlpp, graph));
			line.append("</TD></TR>");
		}
	}

	private String getLabel(OWLClassExpression expression, OWLPrettyPrinter owlpp, OWLGraphWrapper graph) {
		if (expression instanceof OWLClass) {
			return graph.getLabelOrDisplayId(expression);
		}
		else if (expression instanceof OWLObjectIntersectionOf) {
			StringBuilder sb = new StringBuilder();
			OWLObjectIntersectionOf intersectionOf = (OWLObjectIntersectionOf) expression;
			sb.append("<TABLE>");
			for (OWLClassExpression ce : intersectionOf.getOperands()) {
				sb.append("<TR><TD>");
				if (ce instanceof OWLClass) {
					sb.append(graph.getLabelOrDisplayId((OWLClass)ce));
				}
				else if (ce instanceof OWLObjectSomeValuesFrom){
					OWLObjectSomeValuesFrom some = (OWLObjectSomeValuesFrom) ce;
					OWLObjectPropertyExpression property = some.getProperty();
					if (property.isAnonymous()) {
						sb.append(owlpp.render(property));
					}
					else {
						sb.append(graph.getLabelOrDisplayId(property.asOWLObjectProperty()));
					}
					sb.append(" <B>some</B> ");
					OWLClassExpression filler = some.getFiller();
					if (filler instanceof OWLClass) {
						sb.append(graph.getLabelOrDisplayId((OWLClass)filler));
					}
					else {
						sb.append(owlpp.render(filler));
					}
				}
				else {
					sb.append(ce.toString());
				}
				sb.append("</TD></TR>");
			}
			sb.append("</TABLE>");
			return sb.toString();
		}
		return owlpp.render(expression);
	}
	
	private CharSequence getLabel(OWLObjectPropertyExpression expression, OWLPrettyPrinter owlpp, OWLGraphWrapper graph) {
		if (expression.isAnonymous()) {
			return owlpp.render(expression);
		}
		return graph.getLabelOrDisplayId(expression);
	}
	
	
	private String nodeId(OWLNamedObject obj) {
		return obj.getIRI().toString();
	}
	
	private String nodeId(IRI iri) {
		return iri.toString();
	}
	
}
