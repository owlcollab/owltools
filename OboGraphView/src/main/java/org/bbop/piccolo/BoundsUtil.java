package org.bbop.piccolo;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.apache.log4j.*;

public class BoundsUtil {

	//initialize logger
	protected final static Logger logger = Logger.getLogger(BoundsUtil.class);
	public static final double DEFAULT_ERR = 30;

	public static boolean nearlyEquals(double a, double b, double err) {
		return Math.abs(a - b) < err;
	}

	public static boolean nearlyEquals(Point2D a, Point2D b, double err) {
		return nearlyEquals(a.getX(), b.getX(), err)
				&& nearlyEquals(a.getY(), b.getY(), err);
	}

	public static boolean nearlyEquals(Rectangle2D a, Rectangle2D b, double err) {
		return nearlyEquals(a.getX(), b.getX(), err)
				&& nearlyEquals(a.getWidth(), b.getWidth(), err)
				&& nearlyEquals(a.getY(), b.getY(), err)
				&& nearlyEquals(a.getHeight(), b.getHeight(), err);
	}

	public static boolean snugFit(Rectangle2D a, Rectangle2D b, double err) {
		return (nearlyEquals(a.getX(), b.getX(), err) && nearlyEquals(a
				.getWidth(), b.getWidth(), err))
				|| (nearlyEquals(a.getY(), b.getY(), err) && nearlyEquals(
						a.getHeight(), b.getHeight(), err));

	}
	
	/*
	 * Returns a rectangle with the dimensions of src, but centered on target's center
	 */
	public static Rectangle2D doFit(Rectangle2D src, Rectangle2D target) {
		Rectangle2D out = new Rectangle2D.Double();
		out.setFrameFromCenter(target.getCenterX(), target.getCenterY(), src.getWidth(), src.getHeight());
		return out;
	}
}
