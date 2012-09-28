package org.bbop.graph;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.nodes.PImage;
import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.nodes.PText;

import org.apache.log4j.*;

public class IconBuilderUtil {

	//initialize logger
	protected final static Logger logger = Logger.getLogger(IconBuilderUtil.class);

	public static PNode createIcon(Shape backgroundShape,
			Paint backgroundPaint, Stroke backgroundStroke, Paint strokePaint,
			Font font, String foregroundText, Paint textPaint) {
		PPath path = new PPath(backgroundShape);
		path.setPaint(backgroundPaint);
		path.setStroke(backgroundStroke);
		path.setStrokePaint(strokePaint);
		PText text = new PText(foregroundText);
		text.setGreekThreshold(0);
		text.setConstrainHeightToTextHeight(true);
		text.setFont(font);
		text.setConstrainWidthToTextWidth(true);
		text.setTextPaint(textPaint);
		PNode out = new PNode();
		out.addChild(path);
		out.addChild(text);
		text.moveToFront();
		double textScaleY = path.getHeight() / text.getHeight();
		double textScaleX = path.getWidth() / text.getWidth();
		text.centerBoundsOnPoint(path.getBoundsReference().getCenterX(), path
				.getBoundsReference().getCenterY());
		text.scaleAboutPoint(Math.min(textScaleX, textScaleY), path
				.getBoundsReference().getCenter2D());
		return out;
	}

	public static PNode createImageIcon(Shape backgroundShape,
			Paint backgroundPaint, Stroke backgroundStroke, Paint strokePaint,
			Font font, String foregroundText, Paint textPaint, int size) {
		double MARGIN = 2;
		BufferedImage out = new BufferedImage(size, size,
				BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();
		double shapeScale = size
				/ Math.max(backgroundShape.getBounds2D().getWidth(),
						backgroundShape.getBounds2D().getHeight());
		GeneralPath p = new GeneralPath();
		p.append(backgroundShape.getPathIterator(AffineTransform
				.getScaleInstance(shapeScale, shapeScale)), false);
		g.setPaint(backgroundPaint);
		g.fill(p);
		if (backgroundStroke != null && strokePaint != null) {
			g.setStroke(backgroundStroke);
			g.setPaint(strokePaint);
			g.draw(backgroundShape);
		}
		Shape initialStringShape = font.createGlyphVector(g.getFontRenderContext(), foregroundText.toCharArray()).getOutline();
		GeneralPath stringShape = new GeneralPath();
		double textScale = size
		/ Math.max(backgroundShape.getBounds2D().getWidth()-MARGIN,
				backgroundShape.getBounds2D().getHeight()-MARGIN);
		stringShape.append(initialStringShape.getPathIterator(AffineTransform
				.getScaleInstance(textScale, textScale)), false);
		g.setPaint(textPaint);
		g.fill(stringShape);
		return new PImage(out);
	}
}
