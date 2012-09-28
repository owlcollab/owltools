package org.bbop.util;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author jrichter
 * 
 */
import org.apache.log4j.*;

public class ShapeUtil {

	//initialize logger
	protected final static Logger logger = Logger.getLogger(ShapeUtil.class);

	protected static final float[] scratch = new float[6];

	private ShapeUtil() {
	}

	public static double distance(double x, double y, double z, double a,
			double b, double c) {
		return Math.sqrt(Math.pow(x - a, 2) + Math.pow(y - b, 2)
				+ Math.pow(z - c, 2));
	}

	public static Shape createRoundRectangle(float x, float y, float width,
			float height) {
		return createRoundRectangle(50, x, y, width, height);
	}

	public static Shape createRoundRectangle(int roundingPercentage, float x,
			float y, float width, float height) {
		float roundingSize = Math.min(width, height) * roundingPercentage
				/ 100f;
		return createRoundRectangle(null, roundingSize, x, y, width, height);
	}

	public static Shape createRoundRectangle(GeneralPath out,
			float roundingSize, float x, float y, float width, float height) {
		if (out == null)
			out = new GeneralPath();
		else
			out.reset();
		if (roundingSize > width / 2)
			roundingSize = width / 2;
		if (roundingSize > height / 2)
			roundingSize = height / 2;

		// start at leftmost corner of top left curve
		out.moveTo(x, (y + roundingSize));

		// draw top left curve
		out.curveTo(x, y, x, y, x + roundingSize, y);

		// draw line across top
		out.lineTo(x + width - roundingSize, y);

		// draw top right curve
		out.curveTo(x + width, y, x + width, y, x + width, y + roundingSize);

		// draw line down right edge
		out.lineTo(x + width, y + height - roundingSize);

		// draw bottom right curve
		out.curveTo(x + width, y + height, x + width, y + height, x + width
				- roundingSize, y + height);

		// draw bottom line
		out.lineTo(x + roundingSize, y + height);

		// draw bottom left curve
		out.curveTo(x, y + height, x, y + height, x, y + height - roundingSize);

		// draw left line
		out.lineTo(x, y + roundingSize);
		out.closePath();

		return out;
	}

	public static boolean hasSubpaths(Shape shape) {
		PathIterator iterator = shape.getPathIterator(null);
		while (!iterator.isDone()) {
			int type = iterator.currentSegment(scratch);
			if (type == PathIterator.SEG_MOVETO)
				return true;
		}
		return false;
	}

	public static Shape getMaximumOutline(Shape shape) {
		Area out = new Area();
		Iterator<Shape> it = getSubpaths(shape, null).iterator();
		while (it.hasNext()) {
			Shape s = it.next();
			out.add(new Area(s));
		}
		return out;
	}

	/**
	 * Approximates the perimeter of all the subpaths of a shape. The distance
	 * between SEG_MOVETOs is not included in the total length. If a
	 * targetLength is provided that is >= 0, the method will halt and return
	 * the point at that targetLength. The contents are returned in the out
	 * parameter.
	 * 
	 * @param shape
	 *            The shape whose perimeter we're finding
	 * @param flatness
	 *            The allowable flattening error
	 * @param limit
	 *            The recursion depth limit when flattening
	 * @param targetLength
	 *            If >= 0, return the point at the target length
	 * @param out
	 *            If targetLength >=0, return the point at the target length
	 *            through this parameter
	 * @return The perimeter of the shape. If targetLength >=0, return
	 *         targetLength
	 */
	protected static double calculateLength(Shape shape, double flatness,
			int limit, double targetLength, double[] out) {
		FlatteningPathIterator it = new FlatteningPathIterator(shape
				.getPathIterator(null), flatness, limit);
		double len = 0;
		double lastLen = 0;
		Point2D.Double lastPoint = new Point2D.Double();
		Point2D.Double endPoint = new Point2D.Double();
		Point2D.Double lastMoveTo = new Point2D.Double();
		double[] coords = new double[6];
		while (!it.isDone()) {
			lastLen = len;
			lastPoint.setLocation(endPoint);
			int op = it.currentSegment(coords);
			if (op == PathIterator.SEG_MOVETO) {
				endPoint.setLocation(coords[0], coords[1]);
				lastMoveTo.setLocation(endPoint);
			} else if (op == PathIterator.SEG_LINETO) {
				len += Point2D.distance(endPoint.x, endPoint.y, coords[0],
						coords[1]);
				endPoint.setLocation(coords[0], coords[1]);
			} else if (op == PathIterator.SEG_CLOSE) {
				len += Point2D.distance(endPoint.x, endPoint.y, lastMoveTo.x,
						lastMoveTo.y);
				endPoint.setLocation(lastMoveTo);
			}
			if (targetLength >= 0 && len > targetLength) {
				if (out != null) {
					double currentSegLen = len - lastLen;
					double segRatio = (targetLength - lastLen) / currentSegLen;
					double xpos = lastPoint.x + segRatio
							* (endPoint.x - lastPoint.x);
					double ypos = lastPoint.y + segRatio
							* (endPoint.y - lastPoint.y);
					out[0] = xpos;
					out[1] = ypos;
					if (out.length > 2)
						out[2] = Math.atan2(endPoint.y - lastPoint.y,
								endPoint.x - lastPoint.x);
				}
				return targetLength;
			}
			it.next();
		}
		return len;
	}

	public static double getLength(Shape shape, double flatness, int limit) {
		return calculateLength(shape, flatness, limit, -1, null);
	}

	/**
	 * Returns the point at some ratio along the perimiter of a shape. For
	 * example, ratio were .5, this method would return the midpoint of the
	 * shape's perimeter.
	 * 
	 * This method uses a flattening path iterator to approximate the length of
	 * curved shapes. The flatness and limit parameters are used to determine
	 * the amount of allowable error in the approximation
	 * 
	 * @param shape
	 *            The shape along which to calculate
	 * @param ratio
	 *            The ratio value from 0 to 1 to calculate
	 * @param flatness
	 *            The maximum allowable distance from any point along the
	 *            original curve to the flattened approximation
	 * @param limit
	 *            The maximum number of recursions allowed during approximation
	 * @return The (approximate) point along the shape at the given ratio
	 */
	public static Point2D getPointAtRatio(Shape shape, double ratio,
			double flatness, int limit) {
		double[] arr = getPosAndAngleAtRatio(shape, ratio, flatness, limit);
		return new Point2D.Double(arr[0], arr[1]);
	}

	public static double[] getPosAndAngleAtRatio(Shape shape, double ratio,
			double flatness, int limit) {
		double[] out = new double[3];
		double length = calculateLength(shape, flatness, limit, -1, out);
		calculateLength(shape, flatness, limit, length * ratio, out);
		return out;

	}

	public static Collection<Line2D> getLines(FlatteningPathIterator it) {
		Collection<Line2D> out = new LinkedList<Line2D>();
		double[] coords = new double[2];
		double[] lastCoords = new double[2];
		double[] lastMove = new double[2];
		while (!it.isDone()) {
			int type = it.currentSegment(coords);
			if (type == PathIterator.SEG_LINETO) {
				out.add(new Line2D.Double(lastCoords[0], lastCoords[1],
						coords[0], coords[1]));
			} else if (type == PathIterator.SEG_MOVETO) {
				lastMove[0] = coords[0];
				lastMove[1] = coords[1];
			} else if (type == PathIterator.SEG_CLOSE) {
				out.add(new Line2D.Double(lastMove[0], lastMove[1], coords[0],
						coords[1]));
			}
			lastCoords[0] = coords[0];
			lastCoords[1] = coords[1];
		}
		return out;
	}

	/** Indicates no intersection between shapes */
	public static final int NO_INTERSECTION = 0;

	/** Indicates intersection between shapes */
	public static final int COINCIDENT = -1;

	/** Indicates two lines are parallel */
	public static final int PARALLEL = -2;

	/**
	 * Compute the intersection of two line segments.
	 * 
	 * @param a
	 *            the first line segment
	 * @param b
	 *            the second line segment
	 * @param intersect
	 *            a Point in which to store the intersection point
	 * @return the intersection code. One of {@link #NO_INTERSECTION},
	 *         {@link #COINCIDENT}, or {@link #PARALLEL}.
	 */
	public static int intersectLineLine(Line2D a, Line2D b, double[] intersect) {
		double a1x = a.getX1(), a1y = a.getY1();
		double a2x = a.getX2(), a2y = a.getY2();
		double b1x = b.getX1(), b1y = b.getY1();
		double b2x = b.getX2(), b2y = b.getY2();
		return intersectLineLine(a1x, a1y, a2x, a2y, b1x, b1y, b2x, b2y,
				intersect);
	}

	public static double getAngle(Line2D line) {
		return Math.atan2(line.getY2() - line.getY1(), line.getX2()
				- line.getY2());
	}

	/**
	 * Compute the intersection of two line segments.
	 * 
	 * @param a1x
	 *            the x-coordinate of the first endpoint of the first line
	 * @param a1y
	 *            the y-coordinate of the first endpoint of the first line
	 * @param a2x
	 *            the x-coordinate of the second endpoint of the first line
	 * @param a2y
	 *            the y-coordinate of the second endpoint of the first line
	 * @param b1x
	 *            the x-coordinate of the first endpoint of the second line
	 * @param b1y
	 *            the y-coordinate of the first endpoint of the second line
	 * @param b2x
	 *            the x-coordinate of the second endpoint of the second line
	 * @param b2y
	 *            the y-coordinate of the second endpoint of the second line
	 * @param intersect
	 *            a Point in which to store the intersection point
	 * @return the intersection code. One of {@link #NO_INTERSECTION},
	 *         {@link #COINCIDENT}, or {@link #PARALLEL}.
	 */
	public static int intersectLineLine(double a1x, double a1y, double a2x,
			double a2y, double b1x, double b1y, double b2x, double b2y,
			double[] intersect) {
		double ua_t = (b2x - b1x) * (a1y - b1y) - (b2y - b1y) * (a1x - b1x);
		double ub_t = (a2x - a1x) * (a1y - b1y) - (a2y - a1y) * (a1x - b1x);
		double u_b = (b2y - b1y) * (a2x - a1x) - (b2x - b1x) * (a2y - a1y);

		if (u_b != 0) {
			double ua = ua_t / u_b;
			double ub = ub_t / u_b;

			if (0 <= ua && ua <= 1 && 0 <= ub && ub <= 1) {
				intersect[0] = a1x + ua * (a2x - a1x);
				intersect[1] = a1y + ua * (a2y - a1y);
				return 1;
			} else {
				return NO_INTERSECTION;
			}
		} else {
			return (ua_t == 0 || ub_t == 0 ? COINCIDENT : PARALLEL);
		}
	}

	/**
	 * Returns the angle of the first point in a shape.
	 * 
	 * @param shape
	 * @return angle in radians
	 */
	public static double[] getStartPointAndAngle(Shape shape) {
		double[] startCoords = { 0, 0, 0 };
		double[] endCoords = { 0, 0 };
		double[] coords = new double[6];
		PathIterator it = shape.getPathIterator(null);
		while (!it.isDone()) {
			int type = it.currentSegment(coords);
			it.next();
			if (type == PathIterator.SEG_MOVETO) {
				startCoords[0] = coords[0];
				startCoords[1] = coords[1];
			} else if (type == PathIterator.SEG_CLOSE) {
				// do nothing; if we get here it means that the
				// path immediately closes, so we ignore this pointless
				// zero-length
				// segment at the beginning of the shape
			} else {
				endCoords[0] = coords[0];
				endCoords[1] = coords[1];
				break;
			}
		}
		startCoords[2] = Math.atan2(endCoords[1] - startCoords[1], endCoords[0]
				- startCoords[0]);
		return startCoords;
	}

	public static Shape reverseShape(Shape shape, GeneralPath out) {
		if (out == null)
			out = new GeneralPath();
		else
			out.reset();
		out.append(new ReversePathIterator(shape.getPathIterator(null)), false);
		return out;
	}

	public static Shape extendShape(Shape shape, GeneralPath out,
			Point2D newStart, Point2D newEnd) {
		if (out == null)
			out = new GeneralPath();
		else
			out.reset();
		double[] coords = new double[6];
		double[] lastCoords = new double[2];
		double[] lastMove = new double[2];
		double outAngle = 0;
		PathIterator it = shape.getPathIterator(null);
		boolean firstOp = true;
		if (newStart != null) {
			lastMove[0] = newStart.getX();
			lastMove[1] = newStart.getY();
			out.moveTo((float) lastMove[0], (float) lastMove[1]);
		}
		while (!it.isDone()) {
			int type = it.currentSegment(coords);
			it.next();
			if (firstOp && newStart != null) {
				double[] start = getStartPointAndAngle(shape);
				double len = Point2D.distance(newStart.getX(), newStart.getY(),
						start[0], start[1]) / 2;
				double ctrlx = len * Math.sin(start[2]);
				double ctrly = len * Math.cos(start[2]);
				out.quadTo((float) ctrlx, (float) ctrly, (float) start[0],
						(float) start[1]);
				outAngle = start[2];
			}
			if (type == PathIterator.SEG_MOVETO) {
				if (firstOp && newStart != null) {
					// do nothing, we already took care of it above
				} else {
					out.moveTo((float) coords[0], (float) coords[1]);
				}
				lastMove[0] = coords[0];
				lastMove[1] = coords[1];
				lastCoords[0] = coords[0];
				lastCoords[1] = coords[1];
			} else if (type == PathIterator.SEG_CLOSE) {
				out.closePath();
				lastCoords[0] = lastMove[0];
				lastCoords[1] = lastMove[1];
			} else if (type == PathIterator.SEG_LINETO) {
				out.lineTo((float) coords[0], (float) coords[1]);
				outAngle = Math.atan2(coords[1] - lastCoords[1], coords[0]
						- lastCoords[0]);
				lastCoords[0] = coords[0];
				lastCoords[1] = coords[1];
			} else if (type == PathIterator.SEG_CUBICTO) {
				out.curveTo((float) coords[0], (float) coords[1],
						(float) coords[2], (float) coords[3],
						(float) coords[4], (float) coords[5]);
				lastCoords[0] = coords[4];
				lastCoords[1] = coords[5];
				outAngle = Math.atan2(coords[1] - lastCoords[1], coords[0]
						- lastCoords[0]);
			} else if (type == PathIterator.SEG_QUADTO) {
				out.quadTo((float) coords[0], (float) coords[1],
						(float) coords[2], (float) coords[3]);
				lastCoords[0] = coords[2];
				lastCoords[1] = coords[3];
				outAngle = Math.atan2(coords[1] - lastCoords[1], coords[0]
						- lastCoords[0]);
			}
			firstOp = false;
		}
		if (newEnd != null) {
			double len = Point2D.distance(lastCoords[0], lastCoords[1], newEnd
					.getX(), newEnd.getY()) / 2;
			double ctrlx = len * Math.sin(outAngle);
			double ctrly = len * Math.cos(outAngle);
			out.quadTo((float) ctrlx, (float) ctrly, (float) newEnd.getX(),
					(float) newEnd.getY());
		}
		return out;
	}

	public static Collection<double[]> getPointsAndAnglesOfIntersection(
			Shape a, Shape b, double flatness, int limit, int maxPoints) {
		Collection<double[]> out = new LinkedList<double[]>();
		Collection<Line2D> shapeaLines = getLines(new FlatteningPathIterator(a
				.getPathIterator(null), flatness, limit));
		Collection<Line2D> shapebLines = getLines(new FlatteningPathIterator(a
				.getPathIterator(null), flatness, limit));
		double[] scratch = new double[2];
		for (Line2D linea : shapeaLines) {
			for (Line2D lineb : shapebLines) {
				int code = intersectLineLine(linea, lineb, scratch);
				if (code == COINCIDENT) {
					double[] p = { scratch[0], scratch[1], getAngle(linea),
							getAngle(lineb) };
					out.add(p);
					if (maxPoints > 0 && out.size() >= maxPoints)
						return out;
				}
			}
		}
		return out;
	}

	/**
	 * Translates a given shape such that the top-left corner of the shape's
	 * bounding box will be at coordinates (0,0). The amount of translation that
	 * needs to be applied to the resulting shape to return it to its original
	 * position is returned.
	 * 
	 * @param shape
	 *            The shape to translate
	 * @param out
	 *            The GeneralPath used to store the translated shape (this must
	 *            not be the same object as the shape parameter!)
	 * @return A point representing the amount of translation applied
	 */
	public static Point2D normalize(Shape shape, GeneralPath out) {
		Rectangle2D b = shape.getBounds2D();
		out.reset();
		out.append(shape.getPathIterator(AffineTransform.getTranslateInstance(
				-b.getX(), -b.getY())), false);
		return new Point2D.Double(b.getX(), b.getY());
	}

	public static List<Shape> getSubpaths(Shape shape, List<Shape> out) {
		if (out == null)
			out = new ArrayList<Shape>();
		if (!hasSubpaths(shape)) {
			out.add(shape);
			return out;
		}
		GeneralPath currentPath = null;
		PathIterator iterator = shape.getPathIterator(null);
		while (!iterator.isDone()) {
			int type = iterator.currentSegment(scratch);
			if (type == PathIterator.SEG_MOVETO) {
				if (currentPath != null)
					out.add(currentPath);
				currentPath = new GeneralPath();
				currentPath.moveTo(scratch[0], scratch[1]);
			} else if (type == PathIterator.SEG_CLOSE) {
				currentPath.closePath();
			} else if (type == PathIterator.SEG_CUBICTO) {
				currentPath.curveTo(scratch[0], scratch[1], scratch[2],
						scratch[3], scratch[4], scratch[5]);
			} else if (type == PathIterator.SEG_LINETO) {
				currentPath.lineTo(scratch[0], scratch[1]);
			} else if (type == PathIterator.SEG_QUADTO) {
				currentPath.quadTo(scratch[0], scratch[1], scratch[2],
						scratch[3]);
			}
			iterator.next();
		}
		if (currentPath != null)
			out.add(currentPath);
		return out;
	}

	public static int getArrayUse(int op) {
		if (op == PathIterator.SEG_CLOSE)
			return 0;
		else if (op == PathIterator.SEG_CUBICTO)
			return 6;
		else if (op == PathIterator.SEG_LINETO)
			return 2;
		else if (op == PathIterator.SEG_MOVETO)
			return 2;
		else if (op == PathIterator.SEG_QUADTO)
			return 4;
		else
			return 0;
	}

}
