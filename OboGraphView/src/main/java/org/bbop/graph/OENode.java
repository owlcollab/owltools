package org.bbop.graph;

import java.awt.Color;
import java.awt.Shape;

import org.bbop.piccolo.Morphable;
import org.bbop.piccolo.PiccoloUtil;
import org.bbop.piccolo.TransitionText;
import org.bbop.piccolo.ViewRenderedStyleText;
import org.bbop.util.ShapeUtil;
import org.semanticweb.owlapi.model.OWLObject;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.activities.PActivity;

public class OENode extends PCNode<OWLObject> implements Morphable {

	// generated
	private static final long serialVersionUID = -1837048236094993743L;

	static final Object KEY_LABEL = "KEY";

	private final ViewRenderedStyleText field = new ViewRenderedStyleText();

	private final int roundingSize = 10;

	private final int x_label_padding = 10;
	private final int y_label_padding = 0;
	private final int x_margin = 0;
	private final int y_margin = 5;

	protected Color nodeBackgroundColor = new Color(230, 230, 230); // very light gray

	public OENode(OWLObject lo, NodeLabelProvider labelProvider, Shape s) {
		this(lo, labelProvider, DefaultNamedChildProvider.getInstance(), s);
	}

	public OENode(OWLObject lo, NodeLabelProvider labelProvider, NamedChildProvider provider, Shape s) {
		field.setWidth(s.getBounds().getWidth());
		field.setHeight(s.getBounds().getHeight());
		s = ShapeUtil.createRoundRectangle(null, roundingSize, (float) s.getBounds().getX(), 
				(float) s.getBounds().getY(),
				(float) s.getBounds().getWidth() + x_margin + x_label_padding, 
				(float) s.getBounds().getHeight() + y_margin + y_label_padding);
		initialize(lo, provider, s);
		setNamedChild(KEY_LABEL, field);
		field.setPickable(false);
		setLabel(labelProvider.getLabel(lo));
		setPaint(nodeBackgroundColor);
	}

	public int getRoundingSize() {
		return roundingSize;
	}

	public String getLabel() {
		return this.field.getText();
	}

	public void setLabel(String field) {
		this.field.setText(field, false);
		updateFieldDimensions();
	}

	@Override
	protected void internalUpdateBounds(double x, double y, double width, double height) {
		super.internalUpdateBounds(x, y, width, height);
		updateFieldDimensions();
	}

	protected void updateFieldDimensions() {
		field.setWidth(getWidth() + x_label_padding);
		field.setHeight(getHeight() + y_label_padding);
		PiccoloUtil.centerInParent(this.field, true, true, false);
	}

	public PActivity animateSetLabel(String text, long duration) {
		if (field instanceof TransitionText)
			return ((TransitionText) field).animateTextChange(text, duration);
		else {
			setLabel(text);
			PActivity act = new PActivity(0);
			return act;
		}
	}

	public int getPreferredHeight() {
		return (int) field.getHeight();
	}

	public int getPreferredWidth() {
		return (int) field.getWidth();

	}

	@Override
	public String toString() {
		return lo.toString();
	}

	@Override
	public boolean doDefaultMorph() {
		return true;
	}

	@Override
	public PActivity morphTo(PNode node, long duration) {
		if (node instanceof OENode) {
			return animateSetLabel(((OENode) node).getLabel(), duration);
		} else {
			return new PActivity(0);
		}
	}

}
