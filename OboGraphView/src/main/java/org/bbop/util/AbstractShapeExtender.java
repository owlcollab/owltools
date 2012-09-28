package org.bbop.util;

import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;

public abstract class AbstractShapeExtender implements ShapeExtender {

	// generated
	private static final long serialVersionUID = -1778794178016173251L;

	@Override
	public Shape[] extend(Shape a, Shape b) {
		Shape[] out = new Shape[2];
		PathOpList source = new PathOpList(a, false);
		PathOpList target = new PathOpList(b, false);
		/*
		 * logger.info("original source = "+source.toString());
		 * logger.info("original target = "+target.toString());
		 */
		int[] sourceIndices = source.getSubpathIndices(false);
		int[] targetIndices = target.getSubpathIndices(false);

		int i;
		for (i = 0; i < sourceIndices.length && i < targetIndices.length; i++) {
			int endSourceIndex;
			if (i >= sourceIndices.length - 1)
				endSourceIndex = source.size() - 1;
			else
				endSourceIndex = sourceIndices[i + 1] - 1;

			int endTargetIndex;
			if (i >= targetIndices.length - 1)
				endTargetIndex = target.size() - 1;
			else
				endTargetIndex = targetIndices[i + 1] - 1;

			PathOp endTargetOp = target.getSegment(endTargetIndex);
			PathOp endSourceOp = source.getSegment(endSourceIndex);
			if (endTargetOp.getOp() == PathIterator.SEG_CLOSE
					&& endTargetOp.getOp() != endSourceOp.getOp()) {
				source.addPendingOp(endSourceIndex + 1, new PathOp(
						PathIterator.SEG_CLOSE, null));
			} else if (endSourceOp.getOp() == PathIterator.SEG_CLOSE
					&& endTargetOp.getOp() != endSourceOp.getOp()) {
				PathOp sourceStartOp = source.getSegment(sourceIndices[i]);
				PathOp newEndOp;
				if (sourceStartOp.getOp() == PathIterator.SEG_MOVETO) {
					newEndOp = new PathOp(PathIterator.SEG_LINETO,
							sourceStartOp.getCoords());
				} else {
					float[] coords = { 0, 0 };
					newEndOp = new PathOp(PathIterator.SEG_LINETO, coords);
				}
				source.resetOp(endSourceIndex, newEndOp);
			}
		}
		source.flushPendingOps();
		target.flushPendingOps();

		sourceIndices = source.getSubpathIndices(true);
		targetIndices = target.getSubpathIndices(true);

		for (i = 0; i < sourceIndices.length && i < targetIndices.length; i++) {
			int endSourceIndex;
			if (i >= sourceIndices.length - 1)
				endSourceIndex = source.size() - 1;
			else
				endSourceIndex = sourceIndices[i + 1] - 1;

			int endTargetIndex;
			if (i >= targetIndices.length - 1)
				endTargetIndex = target.size() - 1;
			else
				endTargetIndex = targetIndices[i + 1] - 1;

			addPoints(source, sourceIndices[i], endSourceIndex, target,
					targetIndices[i], endTargetIndex);
		}
		source.flushPendingOps();
		target.flushPendingOps();

		if (i < sourceIndices.length) {
			appendEmptySubpaths(target, source, sourceIndices,
					sourceIndices.length - targetIndices.length);
		} else if (i < targetIndices.length) {
			appendEmptySubpaths(source, target, targetIndices,
					targetIndices.length - sourceIndices.length);
		}
		/*
		 * logger.info("morphed source = "+source.toString());
		 * logger.info("morphed target = "+target.toString());
		 */
		out[0] = source.getShape();
		out[1] = target.getShape();
		
		return out;
	}

	protected void appendEmptySubpaths(PathOpList target, PathOpList source,
			int[] sourceIndices, int subpathCount) {

		Point2D.Float origin;
		if (target.size() == 0)
			origin = new Point2D.Float();
		else
			origin = target.getSegmentEndpoint(target.size() - 1, null);

		int startIndex = sourceIndices[sourceIndices.length - subpathCount];
		for (int i = startIndex; i < source.size(); i++) {
			PathOp op = source.getSegment(i);
			PathOp newOp;
			if (op.op == PathIterator.SEG_MOVETO) {
				float[] coords = { origin.x, origin.y };
				newOp = new PathOp(op.op, coords);
				target.addPendingOp(-1, newOp);
			} else if (op.op == PathIterator.SEG_CLOSE) {
				newOp = new PathOp(op.op, new float[0]);
				// maybe this isn't a good idea?
				target.addPendingOp(-1, newOp);
			} else {
				float[] coords = { origin.x, origin.y };
				newOp = new PathOp(PathIterator.SEG_LINETO, coords);
				target.addPendingOp(-1, newOp);
			}
		}
		try {
			target.flushPendingOps();
		} catch (Throwable ex) {
			ex.printStackTrace();
		}
	}

	protected abstract void addPoints(PathOpList source, int i,
			int endSourceIndex, PathOpList target, int j, int endTargetIndex);
}
