package org.bbop.graph;

import java.awt.Dimension;

import org.apache.log4j.Logger;
import org.bbop.piccolo.ViewRenderedStyleText;
import org.semanticweb.owlapi.model.OWLObject;

public class LabelBasedNodeSizeProvider implements NodeSizeProvider {

	//initialize logger
	protected final static Logger logger = Logger.getLogger(LabelBasedNodeSizeProvider.class);

	protected ViewRenderedStyleText text = null;
	protected int initialNodeWidth = 200;
	private final NodeLabelProvider labelProvider;
	protected int xmargin = 0;
	protected int ymargin = 0;

	public LabelBasedNodeSizeProvider(NodeLabelProvider labelProvider) {
		this(0, 0, labelProvider);
	}
	
	public LabelBasedNodeSizeProvider(int xmargin, int ymargin, NodeLabelProvider labelProvider) {
		super();
		this.xmargin = xmargin;
		this.ymargin = ymargin;
		this.labelProvider = labelProvider;
	}

	@Override
	public Dimension getSize(OWLObject io) {
		if (text == null)
			text = new ViewRenderedStyleText();
		text.setWidth(initialNodeWidth);
		text.setText(labelProvider.getLabel(io), true);
		return new Dimension((int) text.getWidth() + xmargin, (int) text.getHeight() + ymargin);
	}

	public int getXmargin() {
		return xmargin;
	}

	public void setXmargin(int xmargin) {
		this.xmargin = xmargin;
	}

	public int getYmargin() {
		return ymargin;
	}

	public void setYmargin(int ymargin) {
		this.ymargin = ymargin;
	}
}
