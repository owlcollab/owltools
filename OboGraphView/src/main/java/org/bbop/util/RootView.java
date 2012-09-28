package org.bbop.util;

import java.awt.Container;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.Element;
import javax.swing.text.Position;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
/**
 * Root view that acts as a gateway between the component
 * and the View hierarchy.
 */
import org.apache.log4j.*;

public class RootView extends View {

	//initialize logger
	protected final static Logger logger = Logger.getLogger(RootView.class);
	
	protected EditorKit editorKit;
	protected JComponent container;

	public RootView(JComponent c, EditorKit editorKit) {
        super(null);
        setContainer(c);
        this.editorKit = editorKit;
    }
    
    public void setContainer(JComponent container) {
    	if (container == null)
    		container = new JPanel();
    	this.container = container;
    		
    }

    public void setView(View v) {
        View oldView = view;
        view = null;
        if (oldView != null) {
            // get rid of back reference so that the old
            // hierarchy can be garbage collected.
            oldView.setParent(null);
        }
        if (v != null) {
            v.setParent(this);
        }
        view = v;
    }

/**
 * Fetches the attributes to use when rendering.  At the root
 * level there are no attributes.  If an attribute is resolved
 * up the view hierarchy this is the end of the line.
 */
    @Override
	public AttributeSet getAttributes() {
    return null;
}

    /**
     * Determines the preferred span for this view along an axis.
     *
     * @param axis may be either X_AXIS or Y_AXIS
     * @return the span the view would like to be rendered into.
     *         Typically the view is told to render into the span
     *         that is returned, although there is no guarantee.
     *         The parent may choose to resize or break the view.
     */
    @Override
	public float getPreferredSpan(int axis) {
        if (view != null) {
            return view.getPreferredSpan(axis);
        }
        return 10;
    }

    /**
     * Determines the minimum span for this view along an axis.
     *
     * @param axis may be either X_AXIS or Y_AXIS
     * @return the span the view would like to be rendered into.
     *         Typically the view is told to render into the span
     *         that is returned, although there is no guarantee.
     *         The parent may choose to resize or break the view.
     */
    @Override
	public float getMinimumSpan(int axis) {
        if (view != null) {
            return view.getMinimumSpan(axis);
        }
        return 10;
    }

    /**
     * Determines the maximum span for this view along an axis.
     *
     * @param axis may be either X_AXIS or Y_AXIS
     * @return the span the view would like to be rendered into.
     *         Typically the view is told to render into the span
     *         that is returned, although there is no guarantee.
     *         The parent may choose to resize or break the view.
     */
    @Override
	public float getMaximumSpan(int axis) {
    return Integer.MAX_VALUE;
    }

    /**
     * Specifies that a preference has changed.
     * Child views can call this on the parent to indicate that
     * the preference has changed.  The root view routes this to
     * invalidate on the hosting component.
     * <p>
     * This can be called on a different thread from the
     * event dispatching thread and is basically unsafe to
     * propagate into the component.  To make this safe,
     * the operation is transferred over to the event dispatching 
     * thread for completion.  It is a design goal that all view
     * methods be safe to call without concern for concurrency,
     * and this behavior helps make that true.
     *
     * @param child the child view
     * @param width true if the width preference has changed
     * @param height true if the height preference has changed
     */ 
    @Override
	public void preferenceChanged(View child, boolean width, boolean height) {
    	if (container != null)
    		container.revalidate();
    }

    /**
     * Determines the desired alignment for this view along an axis.
     *
     * @param axis may be either X_AXIS or Y_AXIS
     * @return the desired alignment, where 0.0 indicates the origin
     *     and 1.0 the full span away from the origin
     */
    @Override
	public float getAlignment(int axis) {
        if (view != null) {
            return view.getAlignment(axis);
        }
        return 0;
    }

    /**
     * Renders the view.
     *
     * @param g the graphics context
     * @param allocation the region to render into
     */
    @Override
	public void paint(Graphics g, Shape allocation) {
        if (view != null) {
            Rectangle alloc = (allocation instanceof Rectangle) ?
	          (Rectangle)allocation : allocation.getBounds();
	setSize(alloc.width, alloc.height);
            view.paint(g, allocation);
        }
    }
    
    /**
     * Sets the view parent.
     *
     * @param parent the parent view
     */
    @Override
	public void setParent(View parent) {
        throw new Error("Can't set parent on root view");
    }

    /** 
     * Returns the number of views in this view.  Since
     * this view simply wraps the root of the view hierarchy
     * it has exactly one child.
     *
     * @return the number of views
     * @see #getView
     */
    @Override
	public int getViewCount() {
        return 1;
    }

    /** 
     * Gets the n-th view in this container.
     *
     * @param n the number of the view to get
     * @return the view
     */
    @Override
	public View getView(int n) {
        return view;
    }

/**
 * Returns the child view index representing the given position in
 * the model.  This is implemented to return the index of the only
 * child.
 *
 * @param pos the position >= 0
 * @return  index of the view representing the given position, or 
 *   -1 if no view represents that position
 * @since 1.3
 */
    @Override
	public int getViewIndex(int pos, Position.Bias b) {
    return 0;
}

    /**
     * Fetches the allocation for the given child view. 
     * This enables finding out where various views
     * are located, without assuming the views store
     * their location.  This returns the given allocation
     * since this view simply acts as a gateway between
     * the view hierarchy and the associated component.
     *
     * @param index the index of the child
     * @param a  the allocation to this view.
     * @return the allocation to the child
     */
    @Override
	public Shape getChildAllocation(int index, Shape a) {
        return a;
    }

    /**
     * Provides a mapping from the document model coordinate space
     * to the coordinate space of the view mapped to it.
     *
     * @param pos the position to convert
     * @param a the allocated region to render into
     * @return the bounding box of the given position
     */
    @Override
	public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
        if (view != null) {
            return view.modelToView(pos, a, b);
        }
        return null;
    }

/**
 * Provides a mapping from the document model coordinate space
 * to the coordinate space of the view mapped to it.
 *
 * @param p0 the position to convert >= 0
 * @param b0 the bias toward the previous character or the
 *  next character represented by p0, in case the 
 *  position is a boundary of two views. 
 * @param p1 the position to convert >= 0
 * @param b1 the bias toward the previous character or the
 *  next character represented by p1, in case the 
 *  position is a boundary of two views. 
 * @param a the allocated region to render into
 * @return the bounding box of the given position is returned
 * @exception BadLocationException  if the given position does
 *   not represent a valid location in the associated document
 * @exception IllegalArgumentException for an invalid bias argument
 * @see View#viewToModel
 */
@Override
public Shape modelToView(int p0, Position.Bias b0, int p1, Position.Bias b1, Shape a) throws BadLocationException {
    if (view != null) {
	return view.modelToView(p0, b0, p1, b1, a);
    }
    return null;
}

    /**
     * Provides a mapping from the view coordinate space to the logical
     * coordinate space of the model.
     *
     * @param x x coordinate of the view location to convert
     * @param y y coordinate of the view location to convert
     * @param a the allocated region to render into
     * @return the location within the model that best represents the
     *    given point in the view
     */
    @Override
	public int viewToModel(float x, float y, Shape a, Position.Bias[] bias) {
        if (view != null) {
            int retValue = view.viewToModel(x, y, a, bias);
	return retValue;
        }
        return -1;
    }

    /**
     * Provides a way to determine the next visually represented model 
     * location that one might place a caret.  Some views may not be visible,
     * they might not be in the same order found in the model, or they just
     * might not allow access to some of the locations in the model.
     *
     * @param pos the position to convert >= 0
     * @param a the allocated region to render into
     * @param direction the direction from the current position that can
     *  be thought of as the arrow keys typically found on a keyboard.
     *  This may be SwingConstants.WEST, SwingConstants.EAST, 
     *  SwingConstants.NORTH, or SwingConstants.SOUTH.  
     * @return the location within the model that best represents the next
     *  location visual position.
     * @exception BadLocationException
     * @exception IllegalArgumentException for an invalid direction
     */
    @Override
	public int getNextVisualPositionFrom(int pos, Position.Bias b, Shape a, 
                                         int direction,
                                         Position.Bias[] biasRet) 
        throws BadLocationException {
        if( view != null ) {
            int nextPos = view.getNextVisualPositionFrom(pos, b, a,
					     direction, biasRet);
	if(nextPos != -1) {
	    pos = nextPos;
	}
	else {
	    biasRet[0] = b;
	}
        } 
        return pos;
    }

    /**
     * Gives notification that something was inserted into the document
     * in a location that this view is responsible for.
     *
     * @param e the change information from the associated document
     * @param a the current allocation of the view
     * @param f the factory to use to rebuild if the view has children
     */
    @Override
	public void insertUpdate(DocumentEvent e, Shape a, ViewFactory f) {
        if (view != null) {
            view.insertUpdate(e, a, f);
        }
    }
    
    /**
     * Gives notification that something was removed from the document
     * in a location that this view is responsible for.
     *
     * @param e the change information from the associated document
     * @param a the current allocation of the view
     * @param f the factory to use to rebuild if the view has children
     */
    @Override
	public void removeUpdate(DocumentEvent e, Shape a, ViewFactory f) {
        if (view != null) {
            view.removeUpdate(e, a, f);
        }
    }

    /**
     * Gives notification from the document that attributes were changed
     * in a location that this view is responsible for.
     *
     * @param e the change information from the associated document
     * @param a the current allocation of the view
     * @param f the factory to use to rebuild if the view has children
     */
    @Override
	public void changedUpdate(DocumentEvent e, Shape a, ViewFactory f) {
        if (view != null) {
            view.changedUpdate(e, a, f);
        }
    }

    /**
     * Returns the document model underlying the view.
     *
     * @return the model
     */
    @Override
	public Document getDocument() {
    	if (view != null)
    		return view.getDocument();
    	else
    		return null;
    }
    
    /**
     * Returns the starting offset into the model for this view.
     *
     * @return the starting offset
     */
    @Override
	public int getStartOffset() {
        if (view != null) {
            return view.getStartOffset();
        }
        return getElement().getStartOffset();
    }

    /**
     * Returns the ending offset into the model for this view.
     *
     * @return the ending offset
     */
    @Override
	public int getEndOffset() {
        if (view != null) {
            return view.getEndOffset();
        }
        return getElement().getEndOffset();
    }

    /**
     * Gets the element that this view is mapped to.
     *
     * @return the view
     */
    @Override
	public Element getElement() {
        if (view != null) {
            return view.getElement();
        }
        return getDocument().getDefaultRootElement();
    }

    /**
     * Breaks this view on the given axis at the given length.
     *
     * @param axis may be either X_AXIS or Y_AXIS
     * @param len specifies where a break is desired in the span
     * @param the current allocation of the view
     * @return the fragment of the view that represents the given span
     *   if the view can be broken, otherwise null
     */
    public View breakView(int axis, float len, Shape a) {
        throw new Error("Can't break root view");
    }

    /**
     * Determines the resizability of the view along the
     * given axis.  A value of 0 or less is not resizable.
     *
     * @param axis may be either X_AXIS or Y_AXIS
     * @return the weight
     */
    @Override
	public int getResizeWeight(int axis) {
        if (view != null) {
            return view.getResizeWeight(axis);
        }
        return 0;
    }

    /**
     * Sets the view size.
     *
     * @param width the width
     * @param height the height
     */
    @Override
	public void setSize(float width, float height) {
        if (view != null) {
            view.setSize(width, height);
        }
    }

    /**
     * Fetches the container hosting the view.  This is useful for
     * things like scheduling a repaint, finding out the host 
     * components font, etc.  The default implementation
     * of this is to forward the query to the parent view.
     *
     * @return the container
     */
    @Override
	public Container getContainer() {
        return container;
    }
    
    /**
     * Fetches the factory to be used for building the
     * various view fragments that make up the view that
     * represents the model.  This is what determines
     * how the model will be represented.  This is implemented
     * to fetch the factory provided by the associated
     * EditorKit unless that is null, in which case this
     * simply returns the BasicTextUI itself which allows
     * subclasses to implement a simple factory directly without
     * creating extra objects.  
     *
     * @return the factory
     */
    @Override
	public ViewFactory getViewFactory() {
        return editorKit.getViewFactory();
    }

    private View view;

}
