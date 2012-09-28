package org.bbop.graph;

import java.awt.BasicStroke;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;

import org.bbop.graph.LinkDatabase.Link;
import org.bbop.gui.LineType;
import org.bbop.piccolo.Morphable;
import org.bbop.util.ShapeUtil;
import org.bbop.util.ZigZagStroke;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.activities.PActivity;
import edu.umd.cs.piccolo.nodes.PPath;

public class OELink extends PCNode<LinkDatabase.Link> implements Morphable {

	// generated
	private static final long serialVersionUID = 5003701776218576440L;

	static final int ICON_PANEL_MARGIN = 2;

	static final int ICON_PANEL_HEIGHT = 20;

	static final int ICON_PANEL_WIDTH = 20;

	static final Object KEY_ICON_PANEL = new Object();

	static final Object KEY_ICON = new Object();

	static final Object KEY_ARROWHEAD = new Object();

	private final TypeIconManager iconManager;

	private float arrowheadHeight = 14f;

	private float arrowheadWidth = 12f;

	public OELink(Link link, TypeIconManager iconManager, TypeColorManager colorManager, Shape s) {
		this(link, iconManager, colorManager,  DefaultNamedChildProvider.getInstance(), s);
	}

	public OELink(LinkDatabase.Link link, TypeIconManager iconManager, TypeColorManager colorManager,
			NamedChildProvider provider, Shape s) {
		this.iconManager = iconManager;
		initialize(link, provider, s);

		int weight = 1;
		
		Paint typeColor = colorManager.getColor(link.getProperty());
		LineType type = LineType.SOLID_LINE;

		setLineWeight(2 * weight, type, typeColor);

		PNode iconPanel = createIconPanel();
		PNode arrowhead = createArrowhead();
		iconPanel.setPaint(typeColor);
		arrowhead.setPaint(typeColor);

		setNamedChild(KEY_ARROWHEAD, arrowhead);
		setNamedChild(KEY_ICON_PANEL, iconPanel);

	}

	public PNode createArrowhead() {
		GeneralPath s = new GeneralPath();
		s.moveTo(0, arrowheadHeight);
		s.lineTo(arrowheadWidth / 2, 0);
		s.lineTo(arrowheadWidth, arrowheadHeight);
		s.closePath();
		Point2D attachmentPoint = new Point2D.Double(arrowheadWidth / 2,
				arrowheadHeight);
		PPath arrowhead = new PPath(s);
		arrowhead.setStroke(null);
		double len = ShapeUtil.getLength(getPathDelegate().getPathReference(),
				.5, 8);
		double arrowheadPlacementRatio = (len - arrowheadHeight) / len;
		double[] temp = ShapeUtil.getPosAndAngleAtRatio(getPathDelegate()
				.getPathReference(), arrowheadPlacementRatio, .5, 8);
		arrowhead.setOffset(temp[0] - attachmentPoint.getX(), temp[1]
				- attachmentPoint.getY());
		arrowhead.rotateAboutPoint(temp[2] + Math.PI / 2, attachmentPoint);
		return arrowhead;
	}

	public PNode createIconPanel() {
		Shape s = ShapeUtil.createRoundRectangle(0, 0, ICON_PANEL_WIDTH,
				ICON_PANEL_HEIGHT);
		PPath iconPanel = new PPath(s);
		iconPanel.setChildrenPickable(false);
		iconPanel.setStroke(null);
		Point2D panelLoc = ShapeUtil.getPointAtRatio(getPathDelegate()
				.getPathReference(), .5, .01, 5);
		iconPanel.setOffset(panelLoc.getX() - iconPanel.getWidth() / 2,
				panelLoc.getY() - iconPanel.getHeight() / 2);
		PNode icon = iconManager.getIcon(getLink().getProperty());
		provider.setNamedChild(KEY_ICON, iconPanel, icon);
		icon.centerFullBoundsOnPoint(iconPanel.getWidth() / 2, 
					     iconPanel.getHeight() / 2);

		double zoom = Math.min(ICON_PANEL_WIDTH - ICON_PANEL_MARGIN / 2,
				ICON_PANEL_HEIGHT - ICON_PANEL_MARGIN / 2)
				/ Math.max(icon.getFullBoundsReference().getWidth(), icon
						.getFullBoundsReference().getHeight());
		icon.scaleAboutPoint(zoom,
				icon.getFullBoundsReference().getWidth() / 2, icon
						.getFullBoundsReference().getHeight() / 2);

		return iconPanel;
	}

	public LinkDatabase.Link getLink() {
		return getObject();
	}

	public void setLineWeight(int lineWeight, LineType type, Paint typeColor) {
		Stroke stroke;
		float[] dashArr = null;
		if (type == LineType.DASHED_LINE) {
			dashArr = new float[2];
			dashArr[0] = 2;
			dashArr[1] = 4;
			// Interesting note:  if you try to assign values to dashArr[2] and [3],
			// the Graph Editor spins forever when trying to draw!
		}
		if (type == LineType.ZIGZAG_LINE) {
			stroke = new ZigZagStroke(new BasicStroke(lineWeight,
								  BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f,
								  dashArr, 0f), 
						  ICON_PANEL_HEIGHT / 6,
						  ICON_PANEL_HEIGHT / 4);
			getPathDelegate().setPathTo(stroke.createStrokedShape(getPathReference()));
			getPathDelegate().setStroke(null);
			getPathDelegate().setStrokePaint(null);
			getPathDelegate().setPaint(typeColor);
		} else {
			stroke = new BasicStroke(lineWeight, BasicStroke.CAP_ROUND,
					BasicStroke.JOIN_ROUND, 10f, dashArr, 0f);
			getPathDelegate().setPathTo(getPathReference());
			getPathDelegate().setStroke(stroke);
			getPathDelegate().setStrokePaint(typeColor);
			getPathDelegate().setPaint(null);
		}

	}
	
	@Override
	public String toString() {
		return "OELink["+getLink()+"]";
	}

	@Override
	public boolean doDefaultMorph() {
		return true;
	}

	@Override
	public PActivity morphTo(PNode node, long duration) {
		return new PActivity(0);
	}
}
