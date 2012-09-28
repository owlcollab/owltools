package org.bbop.graph;

import java.awt.Shape;

import org.apache.log4j.Logger;
import org.bbop.graph.LinkDatabase.Link;
import org.bbop.graph.tooltip.TooltipFactory;
import org.semanticweb.owlapi.model.OWLObject;

public class DefaultNodeFactory implements NodeFactory {

	//initialize logger
	protected final static Logger logger = Logger.getLogger(DefaultNodeFactory.class);

	private final TypeIconManager iconManager;
	private final TypeColorManager colorManager;
	private final NodeLabelProvider labelProvider;
	private final TooltipFactory tooltipFactory;

	public DefaultNodeFactory(TypeIconManager iconManager, TypeColorManager colorManager, 
			NodeLabelProvider labelProvider, TooltipFactory tooltipFactory) {
		super();
		this.iconManager = iconManager;
		this.colorManager = colorManager;
		this.labelProvider = labelProvider;
		this.tooltipFactory = tooltipFactory;
	}

	@Override
	public OELink createLink(Link link, Shape s) {
		OELink node = new OELink(link, iconManager, colorManager, s);
		node.setTooltipFactory(tooltipFactory);
		return node;
	}

	@Override
	public OENode createNode(OWLObject obj, Shape s) {
		OENode node = new OENode(obj, labelProvider, s);
		return node;
	}

}
