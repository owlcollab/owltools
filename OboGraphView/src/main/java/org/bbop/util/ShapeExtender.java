package org.bbop.util;

import java.awt.Shape;
import java.io.Serializable;

public interface ShapeExtender extends Serializable {

	public Shape[] extend(Shape source, Shape target);
}
