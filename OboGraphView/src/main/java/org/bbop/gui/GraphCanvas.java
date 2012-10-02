package org.bbop.gui;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.bbop.graph.DefaultNodeFactory;
import org.bbop.graph.DefaultNodeLabelProvider;
import org.bbop.graph.DefaultTypeColorManager;
import org.bbop.graph.GraphLayout;
import org.bbop.graph.HTMLNodeLabelProvider;
import org.bbop.graph.LabelBasedNodeSizeProvider;
import org.bbop.graph.LinkDatabase;
import org.bbop.graph.LinkDatabase.Link;
import org.bbop.graph.LinkDatabaseLayoutEngine;
import org.bbop.graph.NamedChildProvider;
import org.bbop.graph.NodeDecorator;
import org.bbop.graph.NodeLabelProvider;
import org.bbop.graph.NodeSizeProvider;
import org.bbop.graph.OELink;
import org.bbop.graph.OENode;
import org.bbop.graph.PCNode;
import org.bbop.graph.RelayoutListener;
import org.bbop.graph.RightClickMenuBehavior;
import org.bbop.graph.RightClickMenuFactory;
import org.bbop.graph.SingleCameraPanHandler;
import org.bbop.graph.bounds.BoundsGuarantor;
import org.bbop.graph.bounds.ZoomToAllGuarantor;
import org.bbop.graph.collapse.CollapsibleLinkDatabase;
import org.bbop.graph.collapse.DefaultLinkDatabase;
import org.bbop.graph.collapse.ExpandCollapseListener;
import org.bbop.graph.collapse.ExpansionEvent;
import org.bbop.graph.collapse.LinkButtonBehavior;
import org.bbop.graph.focus.FocusPicker;
import org.bbop.graph.focus.FocusedNodeListener;
import org.bbop.graph.tooltip.LinkTooltipFactory;
import org.bbop.graph.tooltip.TooltipBehavior;
import org.bbop.graph.zoom.ZoomWidgetBehavior;
import org.bbop.piccolo.ExtensibleCanvas;
import org.bbop.piccolo.ExtensibleRoot;
import org.bbop.piccolo.FullPaintCamera;
import org.bbop.piccolo.NamedChildMorpher;
import org.bbop.piccolo.PiccoloUtil;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.graph.OWLGraphWrapper;
import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.PRoot;
import edu.umd.cs.piccolo.activities.PActivity;
import edu.umd.cs.piccolo.activities.PActivity.PActivityDelegate;
import edu.umd.cs.piccolo.activities.PInterpolatingActivity;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PPickPath;

/*** This class does most of the work for the Graph Editor component */

public class GraphCanvas extends ExtensibleCanvas implements RightClickMenuProvider {

	// generated
	private static final long serialVersionUID = 3863061306003913893L;
	
	private static final Object CURRENT_DECORATOR_ANIMATIONS = new Object();
	private static final long DEFAULT_LAYOUT_DURATION = 750;
	
	private static final Comparator<Object> LAYOUT_ORDERING_COMPARATOR = new Comparator<Object>() {

		@Override
		public int compare(Object o1, Object o2) {
			if (o1 instanceof PNode && o2 instanceof PNode) {
				PNode n1 = (PNode) o1;
				PNode n2 = (PNode) o2;
				n1.invalidatePaint();
				n2.invalidatePaint();
				if (o1 instanceof OENode && o2 instanceof OELink)
					return 1;
				else if (o2 instanceof OENode && o1 instanceof OELink)
					return -1;
				else
					return 0;
			} else
				return 0;
		}

	};

	@SuppressWarnings("unchecked")
	static void decorateNode(PRoot root, PNode canvas, Collection<NodeDecorator> decorators, 
			boolean noAnimation, boolean postLayout)
	{
		Collection<PActivity> currentActivities = (Collection<PActivity>) canvas.getAttribute(CURRENT_DECORATOR_ANIMATIONS);
		if (currentActivities == null) {
			currentActivities = new LinkedList<PActivity>();
			canvas.addAttribute(CURRENT_DECORATOR_ANIMATIONS, currentActivities);
		} else {
			for (PActivity activity : currentActivities) {
				activity.terminate(PActivity.TERMINATE_WITHOUT_FINISHING);
			}
		}
		for (int i = 0; i < canvas.getChildrenCount(); i++) {
			PNode node = canvas.getChild(i);
			for (NodeDecorator decorator : decorators) {
				if (postLayout || !decorator.onlyDecorateAfterLayout()) {
					PActivity activity = decorator.decorate(node, noAnimation);
					if (activity != null) {
						root.addActivity(activity);
						currentActivities.add(activity);
					}
				}
			}
		}
	}

	private boolean isLayingOut = false;

	private OWLObject focus = null;

	private PNode newLayer;
	private PActivity relayoutActivity;

    private static class CanvasConfig {
    	
    	static boolean useFocusPicker = false; 		// does not make sense for this application
    	static boolean useToolTip = true; 			// tooltips are always nice
    	static boolean useZoomWidget = true;		// allow to 
    	static boolean useBoundsGuarantor = false;	// the bounds generator refocuses on the current nodes,
    												// this is a bit too dynamic if you allow also collapsible nodes
    	
    	NodeLabelProvider nodeLabelProvider;
    	NodeSizeProvider nodeSizeProvider;
    	NamedChildMorpher morpher = new NamedChildMorpher();

    	LinkDatabaseLayoutEngine layoutEngine;
    	
    	KeyListener keyListener;
    	MouseListener mouseListener;

    	MouseMotionListener mouseMotionListener;

    	MouseWheelListener mouseWheelListener;
    	
    	RightClickMenuFactory menuFactory;
    	RightClickMenuBehavior rightClickBehavior = new RightClickMenuBehavior();

    	List<ViewBehavior> viewBehaviors = new LinkedList<ViewBehavior>();

    	private long layoutDuration = DEFAULT_LAYOUT_DURATION;
    	boolean disableAnimations = false;

    	Collection<NodeDecorator> decorators = new LinkedList<NodeDecorator>();
    	
    	DefaultNodeFactory nodeFactory;

    	Collection<FocusedNodeListener> focusedNodeListeners = new ArrayList<FocusedNodeListener>();
    	
    	Collection<RelayoutListener> layoutListeners = new ArrayList<RelayoutListener>();
    	
    	LinkDatabase database;
    	CollapsibleLinkDatabase collapsibleDatabase;
    }
    
    private final CanvasConfig config;


	public GraphCanvas(GraphLayout graphLayout, OWLGraphWrapper graph, OWLReasoner reasoner) {
		super();
		config = new CanvasConfig();
		
		config.nodeLabelProvider = new HTMLNodeLabelProvider("<center><font face='Arial'>$name$</font></center>", new DefaultNodeLabelProvider(graph));
		config.nodeSizeProvider = new LabelBasedNodeSizeProvider(config.nodeLabelProvider);
		
		if (CanvasConfig.useFocusPicker) {
			addViewBehavior(new FocusPicker());
		}
		if (CanvasConfig.useToolTip) {
			addViewBehavior(new TooltipBehavior());
		}
		if (CanvasConfig.useZoomWidget) {
			addViewBehavior(new ZoomWidgetBehavior(8, 20));
		}
		if (CanvasConfig.useBoundsGuarantor) {
			addViewBehavior(new BoundsGuarantor() {
				@Override
				protected void installDefaultCyclers() {
					addBoundsGuarantor(new ZoomToAllGuarantor(canvas));
				}
			});
		}
		addViewBehavior(new LinkButtonBehavior());
		
		
		DefaultTypeColorManager typeManager = new DefaultTypeColorManager(graph);
		config.nodeFactory = new DefaultNodeFactory(typeManager, typeManager, config.nodeLabelProvider, new LinkTooltipFactory(graph));
		
		config.database = new DefaultLinkDatabase(graph, reasoner);
		config.collapsibleDatabase = new CollapsibleLinkDatabase(config.database);
		config.collapsibleDatabase.addListener(new ExpandCollapseListener() {
			
			@Override
			public void expandStateChanged(ExpansionEvent e) {
				relayout();
			}
		});
		config.layoutEngine = new LinkDatabaseLayoutEngine(config.collapsibleDatabase , graphLayout, config.nodeFactory, config.nodeSizeProvider, config.nodeLabelProvider);
		
		setPanEventHandler(new SingleCameraPanHandler());
		getPanEventHandler().setAutopan(false);
		setAutoscrolls(false);
		
		installListeners();
	}

	@Override
	protected PCamera createCamera() {
		return new FullPaintCamera();
	}

	public void decorate() {
		decorateNode(getRoot(), getLayer(), config.decorators, false, false);
	}

	public void dim() {
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	}

	@Override
	public void fillInMenu(MouseEvent e, JPopupMenu menu) {
		PInputEvent event = new PInputEvent(getRoot().getDefaultInputManager(), e);
		event.setPath(getCamera().pick(e.getX(), e.getY(), 1));
		Collection<JMenuItem> factories = config.menuFactory.getMenuItems(this, event);
		if (factories != null) {
			for (JMenuItem item : factories) {
				if (item == null)
					continue;
				if (item == RightClickMenuFactory.SEPARATOR_ITEM)
					menu.addSeparator();
				else
					menu.add(item);
			}
		}
	}

	public PBounds getBounds(Collection<OWLObject> pcs) {
		PBounds bounds = null;
		for (OWLObject pc : pcs) {
			if (pc == null)
				continue;
			PNode node = getNode(pc);
			if (node == null)
				continue;
			if (bounds == null) {
				bounds = new PBounds(node.getXOffset(), node.getYOffset(), node
						.getWidth(), node.getHeight());
			} else
				bounds.add(new PBounds(node.getXOffset(), node.getYOffset(),
						node.getWidth(), node.getHeight()));
		}
		return bounds;
	}

	public boolean getDisableAnimations() {
		return config.disableAnimations;
	}

	public PNode getFinalLayoutVersion(Object key) {
		if (!isLayingOut())
			return null;
		return config.morpher.getProvider().getNamedChild(key, newLayer);
	}

	public long getLayoutDuration() {
		return config.layoutDuration;
	}

	public LinkDatabase getLinkDatabase() {
		return config.database;
	}
	
	public CollapsibleLinkDatabase getCollapsibleLinkDatabase() {
		return config.collapsibleDatabase;
	}

	public float getMaxZoom() {
		return 1.5f;
	}

	public float getMinZoom() {
		PBounds zoomDim = getLayer().getFullBounds();

		float viewWidth = (float) getCamera().getWidth();
		float zoomWidth = (float) zoomDim.getWidth();
		float viewHeight = (float) getCamera().getHeight();
		float zoomHeight = (float) zoomDim.getHeight();
		float minZoom = Math.min(Math.min(viewWidth / zoomWidth, viewHeight
				/ zoomHeight), 1f);
		return minZoom;
	}

	public NamedChildMorpher getMorpher() {
		return config.morpher;
	}

	public NamedChildProvider getNamedChildProvider() {
		return config.morpher.getProvider();
	}

	public PNode getNewLayer() {
		if (!isLayingOut())
			throw new IllegalStateException(
					"getNewLayer() can only be called while the canvas is laying out");
		return newLayer;
	}

	public PCNode<?> getNode(int x, int y) {
		PPickPath path = getCamera().pick(x, y, 1);
		PCNode<?> node = (PCNode<?>) PiccoloUtil.getNodeOfClass(path, OENode.class,
				OELink.class);
		return node;
	}

	public OENode getNode(OWLObject pc) {
		NamedChildProvider provider = getNamedChildProvider();
		return (OENode) provider.getNamedChild(pc, getLayer());
	}

	public NodeLabelProvider getNodeLabelProvider() {
		return config.nodeLabelProvider;
	}

	public RightClickMenuBehavior getRightClickBehavior() {
		return config.rightClickBehavior;
	}

	private void addViewBehavior(ViewBehavior viewBehavior) {
		config.viewBehaviors.add(viewBehavior);
		viewBehavior.install(this);
	}
	
	public void addDecorator(NodeDecorator decorator) {
		config.decorators.add(decorator);
	}
	
	public void addFocusedNodeListener(FocusedNodeListener listener) {
		config.focusedNodeListeners.add(listener);
	}
	
	public void addRelayoutListener(RelayoutListener listener) {
		config.layoutListeners.add(listener);
	}
	
	public void removeDecorator(NodeDecorator decorator) {
		config.decorators.remove(decorator);
	}
	
	public void removeFocusedNodeListener(FocusedNodeListener listener) {
		config.focusedNodeListeners.remove(listener);
	}
	
	public void removeRelayoutListener(RelayoutListener listener) {
		config.layoutListeners.remove(listener);
	}
	
	/**
	 * This method installs mouse and key listeners on the canvas that forward
	 * those events to piccolo.
	 */
	@Override
	protected void installInputSources() {
		if (config.mouseListener == null) {
			config.mouseListener = new MouseListener() {
				
				private boolean isButton1Pressed;

				private boolean isButton2Pressed;

				private boolean isButton3Pressed;

				@Override
				public void mouseClicked(MouseEvent e) {
					sendInputEventToInputManager(e, MouseEvent.MOUSE_CLICKED);
				}

				@Override
				public void mouseEntered(MouseEvent e) {
					MouseEvent simulated = null;

					if ((e.getModifiersEx() & (InputEvent.BUTTON1_DOWN_MASK
							| InputEvent.BUTTON2_DOWN_MASK | InputEvent.BUTTON3_DOWN_MASK)) != 0) {
						simulated = new MouseEvent((Component) e.getSource(),
								MouseEvent.MOUSE_DRAGGED, e.getWhen(), e
										.getModifiers(), e.getX(), e.getY(), e
										.getClickCount(), e.isPopupTrigger(), e
										.getButton());
					} else {
						simulated = new MouseEvent((Component) e.getSource(),
								MouseEvent.MOUSE_MOVED, e.getWhen(), e
										.getModifiers(), e.getX(), e.getY(), e
										.getClickCount(), e.isPopupTrigger(), e
										.getButton());
					}

					sendInputEventToInputManager(e, MouseEvent.MOUSE_ENTERED);
					sendInputEventToInputManager(simulated, simulated.getID());
				}

				@Override
				public void mouseExited(MouseEvent e) {

				}

				@Override
				public void mousePressed(MouseEvent e) {
					requestFocus();

					boolean shouldBalanceEvent = false;

					if (e.getButton() == MouseEvent.NOBUTTON) {
						if ((e.getModifiers() & MouseEvent.BUTTON1_MASK) == MouseEvent.BUTTON1_MASK) {
							e = new MouseEvent((Component) e.getSource(),
									MouseEvent.MOUSE_PRESSED, e.getWhen(), e
											.getModifiers(), e.getX(),
									e.getY(), e.getClickCount(), e
											.isPopupTrigger(),
									MouseEvent.BUTTON1);
						} else if ((e.getModifiers() & MouseEvent.BUTTON2_MASK) == MouseEvent.BUTTON2_MASK) {
							e = new MouseEvent((Component) e.getSource(),
									MouseEvent.MOUSE_PRESSED, e.getWhen(), e
											.getModifiers(), e.getX(),
									e.getY(), e.getClickCount(), e
											.isPopupTrigger(),
									MouseEvent.BUTTON2);
						} else if ((e.getModifiers() & MouseEvent.BUTTON3_MASK) == MouseEvent.BUTTON3_MASK) {
							e = new MouseEvent((Component) e.getSource(),
									MouseEvent.MOUSE_PRESSED, e.getWhen(), e
											.getModifiers(), e.getX(),
									e.getY(), e.getClickCount(), e
											.isPopupTrigger(),
									MouseEvent.BUTTON3);
						}
					}

					switch (e.getButton()) {
					case MouseEvent.BUTTON1:
						if (isButton1Pressed) {
							shouldBalanceEvent = true;
						}
						isButton1Pressed = true;
						break;

					case MouseEvent.BUTTON2:
						if (isButton2Pressed) {
							shouldBalanceEvent = true;
						}
						isButton2Pressed = true;
						break;

					case MouseEvent.BUTTON3:
						if (isButton3Pressed) {
							shouldBalanceEvent = true;
						}
						isButton3Pressed = true;
						break;
					}

					if (shouldBalanceEvent) {
						MouseEvent balanceEvent = new MouseEvent((Component) e
								.getSource(), MouseEvent.MOUSE_RELEASED, e
								.getWhen(), e.getModifiers(), e.getX(), e
								.getY(), e.getClickCount(), e.isPopupTrigger(),
								e.getButton());
						sendInputEventToInputManager(balanceEvent,
								MouseEvent.MOUSE_RELEASED);
					}

					sendInputEventToInputManager(e, MouseEvent.MOUSE_PRESSED);
				}

				@Override
				public void mouseReleased(MouseEvent e) {
					boolean shouldBalanceEvent = false;

					if (e.getButton() == MouseEvent.NOBUTTON) {
						if ((e.getModifiers() & MouseEvent.BUTTON1_MASK) == MouseEvent.BUTTON1_MASK) {
							e = new MouseEvent((Component) e.getSource(),
									MouseEvent.MOUSE_RELEASED, e.getWhen(), e
											.getModifiers(), e.getX(),
									e.getY(), e.getClickCount(), e
											.isPopupTrigger(),
									MouseEvent.BUTTON1);
						} else if ((e.getModifiers() & MouseEvent.BUTTON2_MASK) == MouseEvent.BUTTON2_MASK) {
							e = new MouseEvent((Component) e.getSource(),
									MouseEvent.MOUSE_RELEASED, e.getWhen(), e
											.getModifiers(), e.getX(),
									e.getY(), e.getClickCount(), e
											.isPopupTrigger(),
									MouseEvent.BUTTON2);
						} else if ((e.getModifiers() & MouseEvent.BUTTON3_MASK) == MouseEvent.BUTTON3_MASK) {
							e = new MouseEvent((Component) e.getSource(),
									MouseEvent.MOUSE_RELEASED, e.getWhen(), e
											.getModifiers(), e.getX(),
									e.getY(), e.getClickCount(), e
											.isPopupTrigger(),
									MouseEvent.BUTTON3);
						}
					}

					switch (e.getButton()) {
					case MouseEvent.BUTTON1:
						if (!isButton1Pressed) {
							shouldBalanceEvent = true;
						}
						isButton1Pressed = false;
						break;

					case MouseEvent.BUTTON2:
						if (!isButton2Pressed) {
							shouldBalanceEvent = true;
						}
						isButton2Pressed = false;
						break;

					case MouseEvent.BUTTON3:
						if (!isButton3Pressed) {
							shouldBalanceEvent = true;
						}
						isButton3Pressed = false;
						break;
					}

					if (shouldBalanceEvent) {
						MouseEvent balanceEvent = new MouseEvent((Component) e
								.getSource(), MouseEvent.MOUSE_PRESSED, e
								.getWhen(), e.getModifiers(), e.getX(), e
								.getY(), e.getClickCount(), e.isPopupTrigger(),
								e.getButton());
						sendInputEventToInputManager(balanceEvent,
								MouseEvent.MOUSE_PRESSED);
					}

					sendInputEventToInputManager(e, MouseEvent.MOUSE_RELEASED);
				}
			};
			addMouseListener(config.mouseListener);
		}

		if (config.mouseMotionListener == null) {
			config.mouseMotionListener = new MouseMotionListener() {
				@Override
				public void mouseDragged(MouseEvent e) {
					sendInputEventToInputManager(e, MouseEvent.MOUSE_DRAGGED);
				}

				@Override
				public void mouseMoved(MouseEvent e) {
					sendInputEventToInputManager(e, MouseEvent.MOUSE_MOVED);
				}
			};
			addMouseMotionListener(config.mouseMotionListener);
		}

		if (config.mouseWheelListener == null) {
			config.mouseWheelListener = new MouseWheelListener() {
				@Override
				public void mouseWheelMoved(MouseWheelEvent e) {
					sendInputEventToInputManager(e, e.getScrollType());
					if (!e.isConsumed() && getParent() != null) {
						getParent().dispatchEvent(e);
					}
				}
			};
			addMouseWheelListener(config.mouseWheelListener);
		}

		if (config.keyListener == null) {
			config.keyListener = new KeyListener() {
				@Override
				public void keyPressed(KeyEvent e) {
					sendInputEventToInputManager(e, KeyEvent.KEY_PRESSED);
				}

				@Override
				public void keyReleased(KeyEvent e) {
					sendInputEventToInputManager(e, KeyEvent.KEY_RELEASED);
				}

				@Override
				public void keyTyped(KeyEvent e) {
					sendInputEventToInputManager(e, KeyEvent.KEY_TYPED);
				}
			};
			addKeyListener(config.keyListener);
		}
	}

	protected void installListeners() {
		addRelayoutListener(new RelayoutListener() {

			@Override
			public void relayoutComplete() {
				getZoomEventHandler().setMaxScale(getMaxZoom());
				float minZoom = getMinZoom();
				getZoomEventHandler().setMinScale(minZoom);
			}

			@Override
			public void relayoutStarting() {
			}

		});
		getCamera().addPropertyChangeListener(PCamera.PROPERTY_VIEW_TRANSFORM,
				new PropertyChangeListener() {

					@Override
					public void propertyChange(PropertyChangeEvent evt) {
						getZoomEventHandler().setMaxScale(getMaxZoom());
						float minZoom = getMinZoom();
						getZoomEventHandler().setMinScale(minZoom);
					}

				});
	}

	public Collection<OENode> getVisibleNodes() {
		Collection<OENode> out = new ArrayList<OENode>();
		for(OWLObject obj : config.collapsibleDatabase.getObjects()) {
			OENode node = getNode(obj);
			if (node != null) {
				out.add(node);
			}
		}
		return out;
	}

	public boolean isLayingOut() {
		return isLayingOut;
	}

	public void panToObjects() {
		PBounds centerBounds = getLayer().getFullBoundsReference();
		getCamera().animateViewToCenterBounds(centerBounds, false,
				getLayoutDuration());
	}

	public void relayout() {
		if (config.collapsibleDatabase == null)
			return;
		isLayingOut = true;
		dim();
		if (getDisableAnimations()) {
			int width = getWidth();
			int height = getHeight();
			if (width < 1)
				width = 1;
			if (height < 1)
				height = 1;
			((ExtensibleRoot) getRoot()).setDisableUpdates(true);
			repaint(); // and the component still works normally.
		}
		if (relayoutActivity != null) {
			relayoutActivity.terminate(PActivity.TERMINATE_WITHOUT_FINISHING);
			relayoutActivity = null;
		}

		newLayer = config.layoutEngine.getNewLayer();
		decorateNode(getRoot(), newLayer, config.decorators, true, true);

		config.morpher.setNewNodeOriginNode(getFocusedNode());
		relayoutActivity = config.morpher.morph(getLayer(), newLayer, getLayoutDuration());
		if (relayoutActivity instanceof PInterpolatingActivity) {
			((PInterpolatingActivity) relayoutActivity).setSlowInSlowOut(false);
		}
		relayoutActivity.setDelegate(new PActivityDelegate() {

			@SuppressWarnings("unchecked")
			@Override
			public void activityFinished(PActivity activity) {
				isLayingOut = false;
				newLayer = null;

				Collections.sort(getLayer().getChildrenReference(), LAYOUT_ORDERING_COMPARATOR);

				fireRelayoutCompleteEvent();
				decorateNode(getRoot(), getLayer(), config.decorators, true, true);
				undim();
				repaint(); // This line stops the graph from appearing in a small box at the bottom righthand corner.
			}
			
			
			@Override
			public void activityStarted(PActivity activity) {
				fireRelayoutStartingEvent();
			}

			@Override
			public void activityStepped(PActivity activity) {
			}

		});
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				if (relayoutActivity == null)
					return;
				getRoot().addActivity(relayoutActivity);
				((ExtensibleRoot) getRoot()).setDisableUpdates(false);
			}
		});
	}

	public OENode getFocusedNode() {
		if (focus == null)
			return null;
		else
			return getNode(focus);
	}
	
	public void setFocusedObject(OWLObject focusedObject) {
		OWLObject oldFocused = this.focus;
		this.focus = focusedObject;
		fireFocusedNodeChanged(oldFocused, focusedObject);
	}
	
	private void fireFocusedNodeChanged(OWLObject oldNode, OWLObject newNode) {
		for (FocusedNodeListener listener : config.focusedNodeListeners) {
			listener.focusedChanged(oldNode, newNode);
		}
	}
	
	private void fireRelayoutCompleteEvent() {
		List<RelayoutListener> defensiveCopy = new ArrayList<RelayoutListener>(config.layoutListeners);
		for (RelayoutListener listener : defensiveCopy) {
			listener.relayoutComplete();
		}
	}

	private void fireRelayoutStartingEvent() {
		List<RelayoutListener> defensiveCopy = new ArrayList<RelayoutListener>(config.layoutListeners);
		for (RelayoutListener listener : defensiveCopy) {
			listener.relayoutStarting();
		}
	}
	
	public void show(Collection<OWLObject> pcs, boolean zoom) {
		PBounds b = getBounds(pcs);
		getCamera().animateViewToCenterBounds(b, zoom, getLayoutDuration());
	}

	public void undim() {
		setCursor(Cursor.getDefaultCursor());
	}

	public void zoomToObjects() {
		PBounds centerBounds = getLayer().getFullBoundsReference();
		getCamera().animateViewToCenterBounds(centerBounds, true, getLayoutDuration());
	}
	
	public void redraw() {
		relayout();		
	}

	public void removeVisibleObjects(Collection<OWLObject> visible) {
		Collection<OWLObject> current = new HashSet<OWLObject>();
		for (OWLObject io : config.collapsibleDatabase.getObjects()) {
			current.add(io);
		}
		current.removeAll(getLinkedObjectCollection(visible));
		config.collapsibleDatabase.setVisibleObjects(current, false);
	}

	public void addVisibleObjects(Collection<Link> visible) {
		Collection<OWLObject> current = new HashSet<OWLObject>();
		for (OWLObject io : config.collapsibleDatabase.getObjects()) {
			current.add(io);
		}
		Collection<OWLObject> loCol = getLinkedObjectCollection(visible);
		current.addAll(loCol);
		config.collapsibleDatabase.setVisibleObjects(current, false);
	}

	protected Collection<OWLObject> getLinkedObjectCollection(Collection<?> pcs) {
		Collection<OWLObject> out = new HashSet<OWLObject>();
		for (Object pc : pcs) {
			if (pc instanceof OWLObject) {
				OWLObject node = (OWLObject) pc;
				out.add(node);
			}
			else if (pc instanceof Link) {
				Link link = (Link) pc;
				OWLObject source = link.getSource();
				if (source != null) {
					out.add(source);
				}
				OWLObject target = link.getTarget();
				if (target != null) {
					out.add(target);
				}
			}
		}
		return out;
	}
	
	public Collection<OWLObject> getVisibleObjects() {
		Collection<OWLObject> out = new HashSet<OWLObject>();
		for (OWLObject io : config.collapsibleDatabase.getObjects()) {
			out.add(io);
		}
		return out;
	}
	
	public void reset() {
		config.collapsibleDatabase.setVisibleObjects(config.database.getRoots(), false);
		relayout();
	}
}
