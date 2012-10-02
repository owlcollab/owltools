package org.bbop.graph.collapse;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;
import org.bbop.graph.LinkDatabase;
import org.bbop.graph.LinkDatabase.Link;
import org.bbop.graph.OENode;
import org.bbop.graph.tooltip.TooltipFactory;
import org.bbop.gui.GraphCanvas;
import org.bbop.gui.ViewBehavior;
import org.bbop.piccolo.PiccoloUtil;
import org.bbop.util.AbstractTaskDelegate;
import org.bbop.util.BackgroundEventQueue;
import org.bbop.util.ShapeUtil;
import org.semanticweb.owlapi.model.OWLObject;

import edu.umd.cs.piccolo.event.PBasicInputEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.nodes.PPath;

public class LinkButtonBehavior implements ViewBehavior {

	//initialize logger
	protected final static Logger logger = Logger.getLogger(LinkButtonBehavior.class);

	public static enum ButtonLocations {
		EXPAND_PARENTS(true, false, false), 
		EXPAND_CHILDREN(true, true, false), 
		COLLAPSE_PARENTS(false, false, false),
		COLLAPSE_CHILDREN(false, true, false),
		CLOSE(false, false, true);

		protected int height = 30;

		protected int width = 30;

		protected boolean expand;

		protected boolean children;

		protected boolean close;

		private ButtonLocations(boolean expand, boolean children, boolean close) {
			this.expand = expand;
			this.children = children;
			this.close = close;
		}

		public int getWidth() {
			return width;
		}

		public int getHeight() {
			return height;
		}

		public boolean isExpand() {
			return expand;
		}

		public boolean isChildren() {
			return children;
		}

		public boolean isClose() {
			return close;
		}
	};

	protected GraphCanvas canvas;

	protected OENode currentNode;

	protected BackgroundEventQueue queue;

	public LinkButtonBehavior() {
	}

	@Override
	public void install(GraphCanvas canvas) {
		queue = new BackgroundEventQueue();
		this.canvas = canvas;
		canvas.addInputEventListener(new PBasicInputEventHandler() {
			@Override
			public void mouseMoved(PInputEvent event) {
				if (LinkButtonBehavior.this.canvas.isLayingOut())
					return;
				OENode oldNode = currentNode;
				currentNode = PiccoloUtil.getNodeOfClass(event.getPath(), OENode.class);
				if (oldNode == null || oldNode.equals(currentNode) == false) {
					queue.cancelAll();
					if (oldNode != null) {
						removeButtons(oldNode);
					}
					if (currentNode != null) {
						addButtons(currentNode);
					}
				}
				super.mouseMoved(event);
			}
		});
	}

	@Override
	public void uninstall(GraphCanvas canvas) {
		this.canvas = null;
		queue.die();
	}

	public void removeButtons(OENode oenode) {
		for (ButtonLocations loc : ButtonLocations.values()) {
			oenode.setNamedChild(loc, null);
		}

	}

	public void addButtons(OENode oenode) {
		for (ButtonLocations loc : ButtonLocations.values()) {
			queue.scheduleTask(new ButtonPlacementTask(loc, oenode));
		}
	}

	protected static Shape getPlusShape(float size) {
		GeneralPath s = new GeneralPath();
		s.moveTo(2, 0);
		s.lineTo(5, 3);
		s.lineTo(8, 0);
		s.lineTo(10, 2);
		s.lineTo(7, 5);
		s.lineTo(10, 8);
		s.lineTo(8, 10);
		s.lineTo(5, 7);
		s.lineTo(2, 10);
		s.lineTo(0, 8);
		s.lineTo(3, 5);
		s.lineTo(0, 2);
		s.closePath();
		double scale = size / s.getBounds2D().getHeight();
		return s.createTransformedShape(AffineTransform.getScaleInstance(scale, scale));
	}

	protected PBasicInputEventHandler buttonListener = new PBasicInputEventHandler() {
		@Override
		public void mouseClicked(PInputEvent event) {

			PPath button = (PPath) event.getPickedNode();
			OWLObject lo = (OWLObject) button.getAttribute("node");
			final ButtonLocations loc = (ButtonLocations) button.getAttribute("buttonType");
			if (loc.isClose()) {
				if (event.isLeftMouseButton())
					canvas.removeVisibleObjects(Collections.singleton(lo));
				return;
			}

			if (event.isRightMouseButton()) {
//				MouseEvent me = (MouseEvent) event.getSourceSwingEvent();
//				JPanel panel = new JPanel() {
//
//					@Override
//					public void paint(Graphics g) {
//						super.paint(g);
//					}
//
//					@Override
//					protected void paintChildren(Graphics g) {
//						super.paintChildren(g);
//					}
//
//					@Override
//					public void paintComponent(Graphics g) {
//						Graphics2D g2 = (Graphics2D) g;
//						Composite c = g2.getComposite();
//						g2.setComposite(AlphaComposite.getInstance(
//								AlphaComposite.SRC_OVER, .8f));
//						super.paintComponent(g);
//						g2.setComposite(c);
//					}
//				};
//				panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
//				List<Link> showThese = new ArrayList<Link>(getLinksToShow(lo, loc.isChildren()));
//				Collections.sort(showThese, new Comparator<Link>() {
//
//					@Override
//					public int compare(Link o1, Link o2) {
//						OWLObject lo1;
//						OWLObject lo2;
//						if (loc.isChildren()) {
//							lo1 = o1.getChild();
//							lo2 = o2.getChild();
//						} else {
//							lo1 = o1.getParent();
//							lo2 = o2.getParent();
//						}
//						return lo1.getName().compareToIgnoreCase(lo2.getName());
//					}
//
//				});
//				for (Link link : showThese) {
//					OWLObject obj;
//					if (loc.isChildren())
//						obj = link.getSource();
//					else
//						obj = link.getTarget();
//					final OWLObject finalObj = obj;
//					final JCheckBox checkBox = new JCheckBox(finalObj.toString()
//							+ " (via " + link.getType() + ")", canvas.getVisibleObjects().contains(link));
//					checkBox.setOpaque(false);
//					checkBox.addActionListener(new ActionListener() {
//
//						public void actionPerformed(ActionEvent e) {
//							if (checkBox.isSelected()) {
//								canvas.addVisibleObjects(Collections
//										.singleton(finalObj));
//							} else
//								canvas.removeVisibleObjects(Collections
//										.singleton(finalObj));
//						}
//					});
//					panel.add(checkBox);
//				}
//				canvas.popupInFrame(panel, "Select "
//						+ (loc.isChildren() ? "child" : "parent")
//						+ " nodes to display", me.getX(), me.getY());

				// right click do nothing for now !
				
			} else if (loc.isExpand()) {
				canvas.addVisibleObjects(getLinksToShow(lo, loc.isChildren()));
			} else {
				Collection<OWLObject> removeUs = new LinkedList<OWLObject>();
				if (loc.isChildren()) {
					removeUs = canvas.getLinkDatabase().getDescendants(lo, false);
				} else {
					removeUs = canvas.getLinkDatabase().getAncestors(lo, false);
				}
				canvas.removeVisibleObjects(removeUs);

			}
		}
	};

	protected Collection<Link> getLinksToShow(OWLObject lo, boolean isChildren) {
		Collection<Link> showThese;
		if (isChildren)
			showThese = canvas.getLinkDatabase().getChildren(lo);
		else
			showThese = canvas.getLinkDatabase().getParents(lo);
		return showThese;
	}

	protected class ButtonPlacementTask extends AbstractTaskDelegate<Void> {

		protected ButtonLocations loc;

		protected OENode oenode;

		public ButtonPlacementTask(ButtonLocations loc, OENode node) {
			this.loc = loc;
			this.oenode = node;
		}

		@Override
		public void execute() throws Exception {
			// This method is always called 5 times in a row for the 5 different button types.
			int buttonSize = 12;
			int width = buttonSize;
			int height = buttonSize;
			final PPath button = new PPath(new Ellipse2D.Double(0, 0, width, height));
			button.addAttribute(PiccoloUtil.NO_RIGHT_CLICK_MENU, true);
			Shape iconShape;
			if (!loc.isClose()) {
				iconShape = getTriangle(height * 2 / 3, loc.isChildren() != loc.isExpand());
			} else {
				iconShape = getPlusShape(height * 2 / 3);
			}
			PPath icon = new PPath(iconShape);
			icon.setPaint(Color.white);
			icon.setStroke(null);
			button.addChild(icon);
			icon.setPickable(false);
			PiccoloUtil.centerInParent(icon, true, true);
			button.addAttribute(TooltipFactory.KEY, SimpleTooltipFactory.getInstance());
			button.addAttribute("buttonType", loc);
			button.addAttribute("node", oenode.getObject());
			button.addInputEventListener(buttonListener);
			button.setStroke(null);
			button.setPaint(Color.blue);
			button.setTransparency(.5f);
			if (!loc.isClose()) {
//				boolean grayedOut = true;  // not used
				Collection<OWLObject> expandThese = new LinkedList<OWLObject>();
				Collection<Link> links = null;

				// boolean tooManyLinks = canvas.getReasoner() != null
				// && ((loc.isChildren() && canvas.getReasoner()
				// .getChildren((LinkedObject) oenode.getObject())
				// .size() > 100) || (!loc.isChildren() && canvas
				// .getReasoner().getParents(
				// (LinkedObject) oenode.getObject())
				// .size() > 100));
				boolean tooManyLinks = false;
				if (!tooManyLinks) {
					if (loc.isChildren()) {
						if (isCancelled())
							return;
						LinkDatabase db = canvas.getLinkDatabase();
						OWLObject obj = oenode.getObject();
						links = db.getChildren(obj);
						if (links.isEmpty()) {
							button.setVisible(false);
							return;
						}
						for (Link link : links) {
							expandThese.add(link.getSource());
						}
					} else {
						if (isCancelled())
							return;
						links = canvas.getLinkDatabase().getParents(oenode.getObject());
						if (links.isEmpty()) {
							button.setVisible(false);
							return;
						}
						for (Link link : links) {
							expandThese.add(link.getTarget());
						}
					}
				}
				// This next block decides whether there are in fact any children that could be expanded
				// or collapsed, so that we can then decide whether to gray out the expand or collapse arrow button.
 				if (loc.isExpand()) {
					Collection<OWLObject> visible = canvas.getVisibleObjects();
					// Remove already visible objects from expandThese; see later if there are any left.
					Iterator<OWLObject> it = expandThese.iterator();
					while (it.hasNext()) {
						OWLObject lo = it.next();
						if (isCancelled())
							return;
						if (visible.contains(lo))
							it.remove();
					}
				} else if (loc.isClose()) { // ?
					// Here we are doing it again--could we cache visible?
					Collection<OWLObject> visible = canvas.getVisibleObjects();
					Iterator<OWLObject> it = expandThese.iterator();
					while (it.hasNext()) {
						OWLObject lo = it.next();
						if (isCancelled())
							return;
						if (!visible.contains(lo))
							it.remove();
					}
				}
				if (!tooManyLinks && expandThese.isEmpty())
					button.setPaint(Color.gray);
				else {
					String connectingWord;
					if (tooManyLinks) {
						connectingWord = " many ";
					} else if (expandThese.size() == links.size()) {
						if (expandThese.size() == 1)
							connectingWord = " 1 ";
						else if (expandThese.size() == 2)
							connectingWord = " both ";
						else
							connectingWord = " all " + expandThese.size() + " ";
					} else if (loc.isExpand()) {
						connectingWord = " " + expandThese.size()
								+ " hidden (of " + links.size() + " total) ";
					} else {
						connectingWord = " " + expandThese.size()
								+ " visible (of " + links.size() + " total) ";
					}
					String tooltip;
					if (tooManyLinks)
						tooltip = (loc.isExpand() ? "Expand" : "Collapse")
								+ " "
								+ (loc.isChildren() ? "children" : "parents");
					else
						tooltip = (loc.isExpand() ? "Expand" : "Collapse")
								+ connectingWord
								+ (loc.isChildren() ? (expandThese.size() == 1 ? "child"
										: "children")
										: (expandThese.size() == 1 ? "parent"
												: "parents"));
					button.addAttribute(TooltipFactory.TEXT_KEY, tooltip);
				}
			}

			if (loc.isClose()) {
				button.setOffset((oenode.getWidth() - width) / 2, oenode
						.getHeight()
						- height);
				button.setPaint(Color.red);
				button.addAttribute(TooltipFactory.TEXT_KEY,
						"Hide only this term");
			} else {
				if (loc.isChildren())
					button.setOffset(0, oenode.getHeight() - height);
				else
					button.setOffset(0, 0);
				if (loc.isExpand())
					button.setOffset(oenode.getWidth() - width, button
							.getYOffset());
			}
			SwingUtilities.invokeLater(new Runnable() {
				
				@Override
				public void run() {
					if (!isCancelled())
						oenode.setNamedChild(loc, button);
				}
			});
		}
	}

	protected void placeButton(ButtonLocations loc, final OENode oenode) {
		// int buttonSize = (int) (Math.min(oenode.getFullBoundsReference()
		// .getWidth(), oenode.getFullBoundsReference().getHeight()) / 3);
	}

	protected static Shape getTriangle(float size, boolean up) {
		GeneralPath s = new GeneralPath();
		s.moveTo(0, 0);
		s.lineTo(10, 0);
		s.lineTo(5, 5);
		s.closePath();
		AffineTransform t = new AffineTransform();
		if (up)
			t.rotate(Math.PI);
		double scale = size / s.getBounds2D().getWidth();
		t.scale(scale, scale);
		s.transform(t);
		ShapeUtil.normalize((Shape) s.clone(), s);
		return s;
	}

	public boolean onlyDecorateAfterLayout() {
		return true;
	}
}
