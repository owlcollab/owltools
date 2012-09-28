package org.bbop.util;

import java.awt.Shape;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;

import org.apache.log4j.Logger;

public class ShapeMorpher {

	//initialize logger
	protected final static Logger logger = Logger.getLogger(ShapeMorpher.class);
	protected Shape source;
	protected Shape target;
	protected Shape massagedSource;
	protected Shape massagedTarget;
	protected float[] sp = new float[6];
	protected float[] tp = new float[6];
	protected Point2D.Float lastPoint = new Point2D.Float(0, 0);

	public ShapeMorpher(Shape source, Shape target, double flatness, int limit,
			ShapeExtender extender) {
		this.source = source;
		this.target = target;
		int padAmt = 50;
		GeneralPath simplifiedSource = new GeneralPath();
		GeneralPath simplifiedTarget = new GeneralPath();
		simplifiedSource.append(new FlatteningPathIterator(source
				.getPathIterator(null), flatness, limit), false);
		simplifiedTarget.append(new FlatteningPathIterator(target
				.getPathIterator(null), flatness, limit), false);

		Shape[] shapes = extender.extend(simplifiedSource, simplifiedTarget);
		massagedSource = shapes[0];
		massagedTarget = shapes[1];

		PathIterator sourceIterator = massagedSource.getPathIterator(null);
		PathIterator targetIterator = massagedTarget.getPathIterator(null);
		lastPoint.setLocation(0, 0);
		int pos = 0;
		while (!sourceIterator.isDone()) {
			int stype = -111 , ttype = -111, outtype = -111;
			try {
				stype = sourceIterator.currentSegment(sp);
				ttype = targetIterator.currentSegment(tp);
				outtype = matchCoords(stype, sp, ttype, tp);
				sourceIterator.next();
				targetIterator.next();
				if (targetIterator.isDone() != sourceIterator.isDone())
					logger.info("Aha!");
				pos++;
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

	public Shape getShapeAtTime(float time, GeneralPath out) {
		if (time == 0)
			return source;
		if (time == 1)
			return target;

		if (out == null)
			out = new GeneralPath();
		else
			out.reset();

		PathIterator sourceIterator = massagedSource.getPathIterator(null);
		PathIterator targetIterator = massagedTarget.getPathIterator(null);
		lastPoint.setLocation(0, 0);
		while (!sourceIterator.isDone()) {
			int stype = sourceIterator.currentSegment(sp);
			int ttype = targetIterator.currentSegment(tp);
			int outtype = matchCoords(stype, sp, ttype, tp);
			interpolateCoords(outtype, sp, tp, time);
			addOp(out, outtype, sp, lastPoint);
			sourceIterator.next();
			targetIterator.next();
		}

		return out;
	}

	public static void addOp(GeneralPath out, int op, float[] sp2,
			Point2D.Float lastPoint) {
		if (op == PathIterator.SEG_CLOSE)
			out.closePath();
		else if (op == PathIterator.SEG_CUBICTO) {
			out.curveTo(sp2[0], sp2[1], sp2[2], sp2[3], sp2[4], sp2[5]);
			lastPoint.x = sp2[4];
			lastPoint.y = sp2[5];
		} else if (op == PathIterator.SEG_LINETO) {
			out.lineTo(sp2[0], sp2[1]);
			lastPoint.x = sp2[0];
			lastPoint.y = sp2[1];
		} else if (op == PathIterator.SEG_MOVETO) {
			out.moveTo(sp2[0], sp2[1]);
			lastPoint.x = sp2[0];
			lastPoint.y = sp2[1];
		} else if (op == PathIterator.SEG_QUADTO) {
			out.quadTo(sp2[0], sp2[1], sp2[2], sp2[3]);
			lastPoint.x = sp2[2];
			lastPoint.y = sp2[3];
		} else
			throw new IllegalArgumentException(
					"bad path iterator segment type " + op);

	}

	protected void interpolateCoords(int outtype, float[] sp2, float[] tp2,
			float time) {
		int count = ShapeUtil.getArrayUse(outtype);
		for (int i = 0; i < count; i++)
			sp2[i] = sp2[i] + (tp2[i] - sp2[i]) * time;
	}

	protected int lineToCubic(float[] lessComplexCoords) {
		lessComplexCoords[4] = lessComplexCoords[0];
		lessComplexCoords[5] = lessComplexCoords[1];
		lessComplexCoords[2] = lessComplexCoords[0];
		lessComplexCoords[3] = lessComplexCoords[1];
		lessComplexCoords[0] = lastPoint.x;
		lessComplexCoords[1] = lastPoint.y;
		return PathIterator.SEG_CUBICTO;
	}

	protected int quadToCubic(float[] moreComplexCoords) {
		throw new UnsupportedOperationException(
				"We don't yet support cubic to quad");
	}

	protected int quadToLine(double[] coords) {
		coords[0] = coords[2];
		coords[1] = coords[3];
		return PathIterator.SEG_LINETO;
	}

	protected int cubicToLine(double[] coords) {
		coords[0] = coords[4];
		coords[1] = coords[5];
		return PathIterator.SEG_LINETO;
	}

	protected boolean convertCurveToLine() {
		return false;
	}

	protected static boolean isMoreComplex(int op, int op2) {
		if (op == op2)
			return false;
		if (op == PathIterator.SEG_CUBICTO)
			return true;
		if (op == PathIterator.SEG_QUADTO && op2 == PathIterator.SEG_LINETO)
			return true;
		if (op == PathIterator.SEG_LINETO && op2 == PathIterator.SEG_CLOSE)
			return true;
		return false;
	}

	protected int matchCoords(int stype, float[] sp2, int ttype, float[] tp2) {
		if (stype == ttype)
			return stype;
		int moreComplexType;
		int lessComplexType;
		float[] moreComplexCoords;
		float[] lessComplexCoords;
		if (isMoreComplex(stype, ttype)) {
			moreComplexType = stype;
			lessComplexType = ttype;
			moreComplexCoords = sp2;
			lessComplexCoords = tp2;
		} else {
			moreComplexType = ttype;
			lessComplexType = stype;
			moreComplexCoords = tp2;
			lessComplexCoords = sp2;

		}
		if (lessComplexType == PathIterator.SEG_QUADTO) {
			quadToCubic(lessComplexCoords);
			return PathIterator.SEG_CUBICTO;
		} else if (lessComplexType == PathIterator.SEG_LINETO) {
			lineToCubic(lessComplexCoords);
			if (moreComplexType == PathIterator.SEG_QUADTO)
				quadToCubic(moreComplexCoords);
			return PathIterator.SEG_CUBICTO;
		} else if (lessComplexType == PathIterator.SEG_CLOSE
				&& moreComplexType == PathIterator.SEG_CLOSE) {
			return PathIterator.SEG_CLOSE;
		}
		// we shouldn't be able to get here if the other parts are doing their
		// jobs
		return -1;
	}
}
