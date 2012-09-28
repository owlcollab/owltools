package org.bbop.piccolo;

import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.bbop.util.ShapeUtil;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.activities.PActivity;
import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolox.nodes.PComposite;

public class WordBubbleNode extends PComposite {

	// generated
	private static final long serialVersionUID = 8789958382828814345L;
	
	protected PPath bubble = new PPath(ShapeUtil.createRoundRectangle(0, 0, 150, 150));
	protected PPath flare = new PPath();
	protected PNode contents;
	protected Point2D pointsAt = new Point2D.Float();
	protected float margins = 10;
	protected boolean tooltipFlareVisible;
	protected boolean tooltipBubbleVisible;

	protected float bubbleTransparency;
	protected Paint bubblePaint;

	public void setMargins(float margins) {
		this.margins = margins;
	}

	protected float getFlareBaseRatio() {
		return 1f;
	}

	public WordBubbleNode() {
		addChild(bubble);
		addChild(flare);
		bubble.setPickable(false);
		flare.setPickable(false);
		setBubbleTransparency(.9f);
		setBubblePaint(new Color(200, 200, 255));
		flare.setStroke(null);
		bubble.setStroke(null);
		flare.setVisible(tooltipFlareVisible);
	}

	public void setContents(PNode contents) {
		if (this.contents != null) {
			removeChild(this.contents);
		}
		this.contents = contents;
		addChild(contents);
		bubble.setPathTo(getBubbleShape());
		PiccoloUtil.normalizePath(bubble);
		contents.setOffset(bubble.getXOffset() + margins, bubble.getYOffset()
				+ margins);
		flare.setPathTo(getFlareShape());
		PiccoloUtil.normalizePath(flare);
		contents.moveToFront();
	}

	protected Shape getBubbleShape() {
		return ShapeUtil.createRoundRectangle(null, 30, 0, 0, (float) contents
				.getFullBoundsReference().getWidth()
				+ getMargins(), (float) contents.getFullBoundsReference()
				.getHeight()
				+ getMargins());
	}

	public PActivity animateSetContents(PNode contents) {
		// animate current contents fade out

		// animate bubble resize
		// animate flare transform
		// animate new contents fade
		return null;
	}

	public Rectangle2D getBubbleBounds() {
		return bubble.getBounds();
	}

	public PActivity animateScaleInFromPoint(long duration) {
		PCompoundActivity a = new PCompoundActivity();
		long shortDuration = duration / 2;
		if (shortDuration < 1)
			shortDuration = 1;
		a.addActivity(flare.animateToPositionScaleRotation(flare.getXOffset(),
				flare.getYOffset(), 1, 0, shortDuration));
		a.addActivity(bubble.animateToPositionScaleRotation(
				bubble.getXOffset(), bubble.getYOffset(), 1, 0, shortDuration));
		a.addActivity(contents.animateToPositionScaleRotation(contents
				.getXOffset(), contents.getYOffset(), 1, 0, shortDuration));
		a.addActivity(flare.animateToTransparency(getBubbleTransparency(),
				duration));
		a.addActivity(bubble.animateToTransparency(getBubbleTransparency(),
				duration));
		a.addActivity(contents.animateToTransparency(1, duration));

		// shrink everything down to prep
		flare.setTransparency(0);
		bubble.setTransparency(0);
		contents.setTransparency(0);
		flare.setScale(.001);
		bubble.setScale(.001);
		contents.setScale(.001);
		flare.setOffset(pointsAt);
		bubble.setOffset(pointsAt);
		contents.setOffset(pointsAt);

		addActivity(a);
		return a;
	}

	public void setBubbleOffset(Point2D bubbleOffset) {
		bubble.setOffset(bubbleOffset.getX() - getXOffset(), bubbleOffset
				.getY()
				- getYOffset());
		flare.setPathTo(getFlareShape());
		contents.setOffset(bubble.getXOffset() + getMargins() / 2, bubble
				.getYOffset()
				+ getMargins() / 2);
		PiccoloUtil.normalizePath(flare);
	}

	protected Shape getFlareShape() {
		GeneralPath out = new GeneralPath();
		double bubbleWidth = bubble.getWidth();
		double bubbleHeight = bubble.getHeight();
		float flareBaseSize = (float) Math.min(bubbleWidth, bubbleHeight)
				* getFlareBaseRatio();
		double bubbleCenterX = bubble.getXOffset() + bubbleWidth / 2;
		double bubbleCenterY = bubble.getYOffset() + bubbleHeight / 2;

		double theta = Math.PI
				/ 2
				- Math.atan((pointsAt.getY() - bubbleCenterY)
						/ (pointsAt.getX() - bubbleCenterX));
		double baseXOffset = flareBaseSize * Math.cos(theta) / 2;
		double baseYOffset = flareBaseSize * Math.sin(theta) / 2;
		out.moveTo((float) (bubbleCenterX + baseXOffset),
				(float) (bubbleCenterY + baseYOffset));
		out.lineTo((float) pointsAt.getX(), (float) pointsAt.getY());
		out.lineTo((float) (bubbleCenterX - baseXOffset),
				(float) (bubbleCenterY - baseYOffset));
		out.closePath();
		Area a = new Area(out);
		a.subtract(new Area(bubble.getPathReference().createTransformedShape(
				AffineTransform.getTranslateInstance(bubble.getXOffset(),
						bubble.getYOffset()))));
		return a;
	}

	public PActivity animateSetBubbleOffset(Point2D bubbleOffset) {
		// animate bubble move
		// animate flare transform
		return null;
	}

	public PActivity animateSetPointsAt(Point2D pointsAt) {
		// animate flare transform
		return null;
	}

	public void setPointsAt(Point2D pointsAt) {
		this.pointsAt.setLocation(pointsAt);
		flare.setPathTo(getFlareShape());
		PiccoloUtil.normalizePath(flare);
	}

	public Paint getBubblePaint() {
		return bubblePaint;
	}

	public void setBubblePaint(Paint bubblePaint) {
		this.bubblePaint = bubblePaint;
		bubble.setPaint(bubblePaint);
		flare.setPaint(bubblePaint);
	}

	public float getBubbleTransparency() {
		return bubbleTransparency;
	}

	public void setBubbleTransparency(float bubbleTransparency) {
		this.bubbleTransparency = bubbleTransparency;
		bubble.setTransparency(bubbleTransparency);
		flare.setTransparency(bubbleTransparency);
	}

	public float getMargins() {
		return margins;
	}
	
	//Only the flare visibility is set as part of the WordBubbleNode
	//Tooltips can be made invisible just by not adding tooltipBehavior at all. 
	public void setTooltipFlareVisible(boolean tooltipFlareVisible){
		flare.setVisible(tooltipFlareVisible);
	}

	public boolean getTooltipFlareVisible(){
		return tooltipFlareVisible;
	}


	

}