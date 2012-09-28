package org.bbop.graph.zoom;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.geom.Ellipse2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.LinkedList;

import org.bbop.gui.GraphCanvas;
import org.bbop.util.ShapeUtil;

import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PBasicInputEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.event.PPanEventHandler;
import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.util.PAffineTransform;

public class ZoomWidget extends PNode {

	// generated
	private static final long serialVersionUID = 3780750336738904263L;

	public static final int ZOOM_VALUE_PROPERTY_CODE = 1;

	protected PPath outerPath;

	protected PPath bubble;

	protected float height;

	protected GraphCanvas canvas;
	
	protected Collection<ZoomWidgetListener> zoomListeners =
		new LinkedList<ZoomWidgetListener>();

	protected PropertyChangeListener cameraListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			float minZoom = canvas.getMinZoom();
			float maxZoom = canvas.getMaxZoom();
			float actualZoom = (float) ((PAffineTransform) evt.getNewValue())
					.getScale();
			float zoomVal = (float) Math.sqrt((actualZoom - minZoom) / (maxZoom - minZoom));
			updateBubblePos(zoomVal);
		}
	};

	public void cleanup() {
		canvas.getCamera().removePropertyChangeListener(
				PCamera.PROPERTY_VIEW_TRANSFORM, cameraListener);
	}

	public ZoomWidget(final GraphCanvas canvas, float width, float height) {
		setTransparency(.7f);
		outerPath = new PPath(ShapeUtil.createRoundRectangle(0, 0, width,
				height));
		outerPath.setStrokePaint(defaultButtonColor());
		bubble = new PPath(new Ellipse2D.Float(0, 0, width, width));
		bubble.setStroke(null);
		bubble.setPickable(false);
		bubble.setPaint(defaultButtonColor());
		outerPath.setPickable(true);
		outerPath.setStroke(new BasicStroke(2));
		Color background = canvas.getBackground();
		outerPath.setPaint(background);
		addChild(outerPath);
		addChild(bubble);
		this.height = height - width;
		this.canvas = canvas;
		canvas.getCamera().addPropertyChangeListener(
				PCamera.PROPERTY_VIEW_TRANSFORM, cameraListener);
		PBasicInputEventHandler handler = new PBasicInputEventHandler() {

			protected PPanEventHandler panHandler; // spare some change?

			@Override
			public void mousePressed(PInputEvent event) {
				panHandler = canvas.getPanEventHandler();
				canvas.setPanEventHandler(null);
				super.mousePressed(event);
			}

			@Override
			public void mouseDragged(PInputEvent event) {
				float newPos = (float) Math.min(Math.max(event
						.getPositionRelativeTo(outerPath).getY(), 0),
						ZoomWidget.this.height);
				setZoomValue(newPos / ZoomWidget.this.height);
			}

			@Override
			public void mouseReleased(PInputEvent event) {
				float newPos = (float) Math.min(Math.max(event
						.getPositionRelativeTo(outerPath).getY(), 0),
						ZoomWidget.this.height);
				setZoomValue(newPos / ZoomWidget.this.height);
				canvas.setPanEventHandler(panHandler);
			}
		};
		outerPath.addInputEventListener(handler);
		bubble.addInputEventListener(handler);
	}

	public void setZoomValue(float zoomValue) {
		float oldZoom = getZoomValue();
		updateBubblePos(zoomValue);
		firePropertyChange(ZOOM_VALUE_PROPERTY_CODE, "ZoomWidgetValue",
				oldZoom, zoomValue);
		fireZoomChange(zoomValue);
	}
	
	public void addZoomListener(ZoomWidgetListener listener) {
		zoomListeners.add(listener);
	}
	
	public void removeZoomListener(ZoomWidgetListener listener) {
		zoomListeners.remove(listener);
	}

	protected void fireZoomChange(float zoomValue) {
		for(ZoomWidgetListener listener : zoomListeners) {
			listener.zoom(zoomValue);
		}		
	}

	protected void updateBubblePos(float zoomValue) {
		bubble.setOffset(bubble.getXOffset(), height * zoomValue);
	}

	protected float getZoomValue() {
		return (float) bubble.getYOffset() / height;
	}

	public static Color defaultButtonColor() {
		return new Color(100, 149, 237);
	}
}
