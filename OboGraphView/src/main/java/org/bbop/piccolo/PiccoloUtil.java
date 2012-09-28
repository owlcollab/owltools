package org.bbop.piccolo;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.activities.PActivity;
import edu.umd.cs.piccolo.activities.PActivity.PActivityDelegate;
import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.util.PAffineTransform;
import edu.umd.cs.piccolo.util.PPickPath;
import edu.umd.cs.piccolo.util.PStack;
import edu.umd.cs.piccolox.nodes.PStyledText;

import org.apache.log4j.*;

public class PiccoloUtil {

	//initialize logger
	protected final static Logger logger = Logger.getLogger(PiccoloUtil.class);

	// public static final long ANIMATION_DURATION = 1000;

	protected static HTMLEditorKit htmlEditorKit;

	public static void setHTML(PStyledText node, String html) {
		if (htmlEditorKit == null)
			htmlEditorKit = new HTMLEditorKit();
		Document doc;
		if (node.getDocument() instanceof HTMLDocument) {
			doc = node.getDocument();
		} else {
			doc = htmlEditorKit.createDefaultDocument();
		}
		try {
			doc.remove(0, doc.getLength());
			htmlEditorKit.read(new StringReader(html), doc, 0);
			logger.info("stylesheet = "
					+ ((HTMLDocument) doc).getStyleSheet());
			node.setDocument(doc);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}

	public static Shape createSilhouette(PNode node) {
		PNode parent = node.getParent();
		return createSilhouette(parent, node, 0, 0);
	}

	public static Shape createSilhouette(PNode parent, PNode node,
			double xoffset, double yoffset) {
		Area out = new Area();
		if (node instanceof PPath) {
			PPath pnode = (PPath) node;
			GeneralPath shape = new GeneralPath();
			shape.append(pnode.getPathReference().getPathIterator(
					AffineTransform.getTranslateInstance(xoffset, yoffset)),
					false);
			out.add(new Area(shape));
			if (pnode.getStroke() != null && pnode.getStrokePaint() != null) {
				out.add(new Area(pnode.getStroke().createStrokedShape(shape)));
			}
		}
		Iterator<?> it = node.getChildrenIterator();
		while (it.hasNext()) {
			PNode c = (PNode) it.next();
			out.add(new Area(createSilhouette(parent, c, c.getXOffset()
					+ xoffset, c.getYOffset() + yoffset)));
		}
		return out;
	}

	public static void centerInParent(PNode node, boolean horz, boolean vert) {
		centerInParent(node, horz, vert, false);
	}

	public static void centerInParent(PNode node, boolean horz, boolean vert,
			boolean fullbounds) {
		if (node.getParent() == null)
			return;
		double x;
		double y;
		if (horz) {
			if (fullbounds)
				x = (node.getParent().getFullBoundsReference().getWidth() - node
						.getFullBoundsReference().getWidth()) / 2;
			else
				x = (node.getParent().getWidth() - node.getWidth()) / 2;
		} else
			x = node.getXOffset();
		if (vert) {
			if (fullbounds)
				y = (node.getParent().getFullBoundsReference().getHeight() - node
						.getFullBoundsReference().getHeight()) / 2;
			else
				y = (node.getParent().getHeight() - node.getHeight()) / 2;

		} else
			y = node.getYOffset();
		node.setOffset(x, y);
	}

	public static void normalizePath(PPath node) {
		Rectangle2D shapeBounds = node.getPathReference().getBounds2D();
		if (node.getParent() != null) {
			Point2D parentOffset = node.getParent().getGlobalBounds()
					.getOrigin();
			node.getPathReference().transform(
					AffineTransform.getTranslateInstance(-shapeBounds.getX(),
							-shapeBounds.getY()));
			node.setOffset(shapeBounds.getX() - parentOffset.getX(),
					shapeBounds.getY() - parentOffset.getY());
		} else {
			Point2D oldOffset = node.getOffset();

			node.getPathReference().transform(
					AffineTransform.getTranslateInstance(-shapeBounds.getX(),
							-shapeBounds.getY()));
			node.setOffset(shapeBounds.getX() + oldOffset.getX(), shapeBounds
					.getY()
					+ oldOffset.getY());
		}

		node.updateBoundsFromPath();
	}

	public static PActivity animateScaleFactorAboutPoint(PNode node, double x,
			double y, double scale, long duration) {
		PAffineTransform a = node.getTransform();
		a.scaleAboutPoint(scale, x, y);
		return node.animateToTransform(a, duration);
	}

	public static PActivity animateAbsoluteScaleAboutPoint(PNode node,
			double x, double y, double scale, long duration) {
		PAffineTransform a = node.getTransform();
		a.scaleAboutPoint(scale / a.getScale(), x, y);
		return node.animateToTransform(a, duration);
	}

	public static PActivity animateScaleFactorAboutCenter(PNode node,
			double scale, long duration) {
		PAffineTransform a = node.getTransform();
		double x = node.getFullBoundsReference().getWidth() / 2;
		double y = node.getFullBoundsReference().getHeight() / 2;
		a.scaleAboutPoint(scale, x, y);
		return node.animateToTransform(a, duration);
	}

	public static PActivity animateRotateAboutPoint(PNode node, double x,
			double y, double theta, long duration) {
		PAffineTransform a = node.getTransform();
		a.rotate(theta, x, y);
		return node.animateToTransform(a, duration);
	}

	public static PActivity animateAbsoluteScaleAboutCenter(PNode node,
			double scale, long duration) {
		PAffineTransform a = node.getTransform();
		double x = node.getBoundsReference().getWidth() / 2;
		double y = node.getBoundsReference().getHeight() / 2;
		a.scaleAboutPoint(scale / a.getScale(), x, y);
		return node.animateToTransform(a, duration);
	}

	@SuppressWarnings("unchecked")
	public static <T> T getNodeOfClass(PPickPath path, Class<T> targetClass) {
		PStack stack = path.getNodeStackReference();
		Iterator<?> it = stack.iterator();
		while (it.hasNext()) {
			PNode n = (PNode) it.next();
			if (targetClass.isAssignableFrom(n.getClass())) {
				return (T) n;
			}
		}
		return null;
	}

	public static PNode getNodeOfClass(PPickPath path, Class<?>... targetClasses) {
		for (Class<?> targetClass : targetClasses) {
			PStack stack = path.getNodeStackReference();
			Iterator<?> it = stack.iterator();
			while (it.hasNext()) {
				PNode n = (PNode) it.next();
				if (targetClass.isAssignableFrom(n.getClass())) {
					return n;
				}
			}
		}
		return null;
	}

	public static PActivity animateTransitions(
			final Map<Object, PNode> rectMap,
			final Map<Object, PNode> newRectMap, final PNode parent,
			final Morpher morpher, final long duration) {
		PCompoundActivity relayoutActivity = new PCompoundActivity();

		Collection<Object> allKeys = new HashSet<Object>();
		allKeys.addAll(rectMap.keySet());
		allKeys.addAll(newRectMap.keySet());
		final Collection<PNode> addSet = new LinkedHashSet<PNode>();
		final Collection<PNode> delSet = new LinkedHashSet<PNode>();

		for (Object key : allKeys) {
			PNode oldNode = rectMap.get(key);
			PNode newNode = newRectMap.get(key);
			relayoutActivity.addActivity(morpher.morph(oldNode, newNode,
					duration));
			if (oldNode == null)
				addSet.add(newNode);
			if (newNode == null)
				delSet.add(oldNode);
		}
		relayoutActivity.setDelegate(new PActivityDelegate() {

			@Override
			public void activityFinished(PActivity arg0) {
				for (PNode delNode : delSet)
					delNode.removeFromParent();
			}

			@Override
			public void activityStarted(PActivity arg0) {
				for (PNode child : addSet) {
					parent.addChild(child);
				}
			}

			@Override
			public void activityStepped(PActivity arg0) {
			}

		});
		return relayoutActivity;
	}

	public static final Object NO_RIGHT_CLICK_MENU = new Object() {
		@Override
		public String toString() {
			return "NO_RIGHT_CLICK_MENU";
		};
	};

	public static boolean rightClickMenusDisabled(PNode node) {
		return node.getBooleanAttribute(NO_RIGHT_CLICK_MENU, false);
	}
}
