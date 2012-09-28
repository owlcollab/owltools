package org.bbop.graph.tooltip;

import org.bbop.graph.LinkDatabase;
import org.bbop.graph.OELink;
import org.bbop.piccolo.ViewRenderedStyleText;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import owltools.graph.OWLGraphWrapper;
import edu.umd.cs.piccolo.PNode;

public class LinkTooltipFactory extends AbstractTooltipFactory {

	private final OWLGraphWrapper graph;

	public LinkTooltipFactory(OWLGraphWrapper graph) {
		this.graph = graph;
	}

	ViewRenderedStyleText text = new ViewRenderedStyleText();

	@Override
	public PNode getTooltip(PNode node) {
		if (node instanceof OELink) {
			LinkDatabase.Link link = ((OELink) node).getLink();
			if (link != null) {
				StringBuilder html = new StringBuilder();
				html.append("<html>\n<body>\n");
				html.append("<table>");
				OWLObject source = link.getSource();
				html.append("<tr><td><b>Source</b></td><td>");
				html.append(graph.getLabelOrDisplayId(source));
				html.append("</td>");
				
				OWLObject target = link.getTarget();
				html.append("<tr><td><b>Target</b></td><td>");
				html.append(graph.getLabelOrDisplayId(target));
				html.append("</td>");
				
				OWLObjectProperty property = link.getProperty();
				html.append("<tr><td><b>Type</b></td><td>");
				if (property == null) {
					html.append("subClassOf");
				}
				else {
					html.append(graph.getLabelOrDisplayId(property));
				}
				html.append("</td>");
				
				html.append("</table></body></html>");
//				text.setWidth(canvas.getWidth() * .6);
				text.setText(html.toString(), true);
				return text;
			}
		}
		return null;
	}

}
