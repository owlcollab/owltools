package org.bbop.util;

import java.awt.geom.PathIterator;

import org.apache.log4j.*;

public class PathOp {

	//initialize logger
	protected final static Logger logger = Logger.getLogger(PathOp.class);
	public int op;
	public float [] coords;
	public int pendingIndex = -1;
	
	public PathOp(int op, float [] coords) {
		this.op = op;
		this.coords = coords;
		if (this.coords == null)
			this.coords = new float[0];
	}
	
	@Override
	public String toString() {
		StringBuffer out = new StringBuffer();
		if (op == PathIterator.SEG_CLOSE)
			out.append("SEG_CLOSE");
		else if (op == PathIterator.SEG_MOVETO)
			out.append("SEG_MOVETO");
		else if (op == PathIterator.SEG_LINETO)
			out.append("SEG_LINETO");
		else if (op == PathIterator.SEG_CUBICTO)
			out.append("SEG_CUBICTO");
		else if (op == PathIterator.SEG_CLOSE)
			out.append("SEG_CLOSE");
		else if (op == PathIterator.SEG_QUADTO)
			out.append("SEG_QUADTO");
		else
			out.append("????");
		out.append(":");
		for(int i=0; i < coords.length; i++) {
			if (i > 0)
				out.append(",");
			out.append(coords[i]);
		}
		return out.toString();
	}
	
	public void setPendingIndex(int pendingIndex) {
		this.pendingIndex = pendingIndex;
	}
	
	public int getPendingIndex() {
		return pendingIndex;
	}

	public float[] getCoords() {
		return coords;
	}

	public void setCoords(float[] coords) {
		this.coords = coords;
	}

	public int getOp() {
		return op;
	}

	public void setOp(int op) {
		this.op = op;
	}
	
	
}
