package org.bbop.util;

import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

public class PathOpList {

	//initialize logger
	protected final static Logger logger = Logger.getLogger(PathOpList.class);

	protected float[] scratch = new float[6];
	protected int[] subpathIndices = null;
	protected List<PathOp> list = new ArrayList<PathOp>();
	protected List<PathOp> pendingOps = new ArrayList<PathOp>();
	protected int subpathCount = 0;
	protected int windingRule;

	public PathOpList(Shape shape, boolean finishClose) {
		this(shape.getPathIterator(null), finishClose);
	}

	public int getSubpathCount() {
		return subpathCount;
	}

	public void duplicateEndPoint(int segmentIndex) {
		PathOp op = list.get(segmentIndex);
		if (op.op == PathIterator.SEG_CLOSE) {
			// draw a line from the previous point to MOVE_TO
			Point2D.Float startShapePoint = getPathStartPointForSegIndex(
					segmentIndex, null);
			float[] coords = { startShapePoint.x, startShapePoint.y };
			PathOp newOp = new PathOp(PathIterator.SEG_LINETO, coords);
			addPendingOp(segmentIndex, newOp);
		} else {
			// draw a line from the previous endpoint to the previous endpoint
			Point2D.Float endPoint = getSegmentEndpoint(segmentIndex, null);
			float[] coords = { endPoint.x, endPoint.y };
			PathOp newOp = new PathOp(PathIterator.SEG_LINETO, coords);
			addPendingOp(segmentIndex + 1, newOp);
		}
	}

	public Point2D.Float getPathStartPointForSegIndex(int segIndex,
			Point2D.Float out) {
		for (int i = segIndex; i >= 0; i--) {
			PathOp op = list.get(i);
			if (op.op == PathIterator.SEG_MOVETO) {
				return getSegmentEndpoint(i, out);
			}
		}
		out.setLocation(0, 0);
		return out;
	}

	public int[] getSubpathIndices(boolean rebuild) {
		if (subpathIndices == null || rebuild)
			rebuildSubpathIndex();
		return subpathIndices;
	}

	protected void rebuildSubpathIndex() {
		subpathIndices = new int[subpathCount];
		Iterator<PathOp> it = list.iterator();
		int subpathIndex = 0;
		for (int i = 0; it.hasNext(); i++) {
			PathOp op = it.next();
			if (op.op == PathIterator.SEG_MOVETO)
				subpathIndices[subpathIndex++] = i;
		}
	}

	protected void addSubpathIndex(int arrIndex, int subpathIndex) {
		int[] newArr = new int[subpathIndices.length + 1];
		for (int i = 0; i < newArr.length; i++) {
			if (i < arrIndex)
				newArr[i] = subpathIndices[i];
			else if (i == arrIndex)
				newArr[i] = subpathIndex;
			else if (i > arrIndex)
				newArr[i] = subpathIndices[i - 1] + 1;
		}
		subpathIndices = newArr;
	}

	public PathOpList(PathIterator it, boolean finishClose) {
		windingRule = it.getWindingRule();
		Point2D.Float lastMoveTo = new Point2D.Float();
		Point2D.Float lastPoint = new Point2D.Float();
		while (!it.isDone()) {
			int op = it.currentSegment(scratch);
			int coordCount = ShapeUtil.getArrayUse(op);
			float[] coords = new float[coordCount];
			for (int i = 0; i < coordCount; i++) {
				coords[i] = scratch[i];
				if (i == coordCount - 1)
					lastPoint.y = coords[i];
				else if (i == coordCount - 2)
					lastPoint.x = coords[i];
			}
			if (op == PathIterator.SEG_MOVETO)
				lastMoveTo.setLocation(coords[0], coords[1]);
			if (op == PathIterator.SEG_CLOSE && finishClose) {
				if (!lastPoint.equals(lastMoveTo)) {
					float[] pt = new float[2];
					pt[0] = lastMoveTo.x;
					pt[1] = lastMoveTo.y;
					addOp(PathIterator.SEG_LINETO, pt);
					lastPoint.setLocation(lastMoveTo);
				}
			}

			addOp(op, coords);
			it.next();
		}
	}

	public Point2D.Float getSegmentStartPoint(int index, Point2D.Float out) {
		if (out == null)
			out = new Point2D.Float();
		if (index == 0) {
			out.setLocation(0, 0);
			return out;
		} else {
			return getSegmentEndpoint(index - 1, out);
		}
	}

	public Point2D.Float getSegmentEndpoint(int index, Point2D.Float out) {
		if (out == null)
			out = new Point2D.Float();
		PathOp op = (PathOp) list.get(index);
		if (op.op == PathIterator.SEG_CLOSE) {
			getPathStartPointForSegIndex(index, out);
		} else {
			out.x = op.coords[op.coords.length - 2];
			out.y = op.coords[op.coords.length - 1];
		}
		return out;
	}

	public void addOp(int op, float[] coords) {
		addOp(list.size(), op, coords);
	}

	public void addOp(int index, int op, float[] coords) {
		addOp(index, new PathOp(op, coords));
	}

	public int size() {
		return list.size();
	}
	
	public void addOp(PathOp op) {
		addOp(list.size(), op);
	}

	public void addOp(int index, PathOp op) {
		subpathIndices = null;
		if (op.op == PathIterator.SEG_MOVETO) {
			subpathCount++;
		}
		list.add(index, op);
	}

	public void addPendingOp(int index, PathOp op) {
		op.setPendingIndex(index);
		pendingOps.add(op);
	}
	
	public void resetOp(int index, PathOp op) {
		list.set(index, op);
	}

	public void flushPendingOps() {
		Collections.sort(pendingOps, new Comparator<PathOp>() {
			@Override
			public int compare(PathOp o1, PathOp o2) {
				return o1.getPendingIndex() - o2.getPendingIndex();
			}
		});
		int addedSoFar = 0;
		Iterator<PathOp> it = pendingOps.iterator();
		while (it.hasNext()) {
			PathOp op = it.next();
			it.remove();
			int newIndex = op.getPendingIndex();
			if (newIndex < 0)
				newIndex = list.size();
			else
				newIndex = newIndex + addedSoFar;
			addOp(newIndex, op);
			addedSoFar++;
		}
	}
	
	@Override
	public String toString() {
		return list.toString();
	}

	protected void incrementPendingIndices(int lastIndex) {
		Iterator<PathOp> it = pendingOps.iterator();
		while (it.hasNext()) {
			PathOp op = it.next();
			if (op.getPendingIndex() >= lastIndex)
				op.setPendingIndex(op.getPendingIndex() + 1);
		}
	}

	public Shape getShape() {
		GeneralPath out = new GeneralPath();
		out.append(getPathIterator(), false);
		return out;
	}

	public PathIterator getPathIterator() {
		return new PathIterator() {
			protected int index = 0;

			@Override
			public boolean isDone() {
				return index >= list.size();
			}

			@Override
			public int getWindingRule() {
				return windingRule;
			}

			@Override
			public void next() {
				index++;
			}

			@Override
			public int currentSegment(float[] coords) {
				PathOp op = (PathOp) list.get(index);
				for (int i = 0; i < op.coords.length; i++)
					coords[i] = op.coords[i];
				return op.op;
			}

			@Override
			public int currentSegment(double[] coords) {
				PathOp op = (PathOp) list.get(index);
				for (int i = 0; i < op.coords.length; i++)
					coords[i] = op.coords[i];
				return op.op;
			}
		};
	}

	public Iterator<PathOp> iterator() {
		return list.iterator();
	}

	public PathOp getSegment(int i) {
		return (PathOp) list.get(i);
	}

}
