package org.bbop.piccolo;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.StringReader;

import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.BoxView;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.HTMLEditorKit;

import org.bbop.util.RootView;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.util.PPaintContext;

public class ViewRenderedStyleText extends PNode {

	// generated
	private static final long serialVersionUID = 2593103842084368338L;

	protected EditorKit editorKit;

	protected ViewFactory viewFactory;

	protected Document document;

	protected View view;

	protected RootView rootView;

	protected String text = "";

	protected boolean useFixedWidth = true;
	
    public static final Object AA_TEXT_PROPERTY_KEY =
        new StringBuffer("AATextPropertyKey");

	public ViewRenderedStyleText() {
		this(new HTMLEditorKit(), null);
	}

	public ViewRenderedStyleText(EditorKit editorKit, ViewFactory viewFactory) {
		this.editorKit = editorKit;
		if (viewFactory == null)
			this.viewFactory = editorKit.getViewFactory();
		else
			this.viewFactory = viewFactory;
		document = editorKit.createDefaultDocument();
		((AbstractDocument) document).setAsynchronousLoadPriority(-1);
		JTextField field = new JTextField();
		field.putClientProperty(AA_TEXT_PROPERTY_KEY,
				Boolean.TRUE);
		rootView = new RootView(field, editorKit);
	}

	public EditorKit getEditorKit() {
		return editorKit;
	}

	public String getLabel() {
		try {
			return document.getText(0, document.getLength());
		} catch (BadLocationException e) {
			return text;
		}
	}

	public String getText() {
		return text;
	}

	public void setText(String text, boolean resizeBounds) {
		try {
			this.text = text;
			// document = editorKit.createDefaultDocument();
			((AbstractDocument) document).setAsynchronousLoadPriority(-1);

			document.remove(0, document.getLength());
			editorKit.read(new StringReader(text), document, 0);

			view = editorKit.getViewFactory().create(
					document.getRootElements()[0]);
			rootView.setView(view);
			if (resizeBounds)
				updateBounds();
			else
				view.setSize((float) getWidth(), (float) getHeight());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@Override
	protected void internalUpdateBounds(double x, double y, double width,
			double height) {
		super.internalUpdateBounds(x, y, width, height);
		if (view != null)
			view.setSize((float) getWidth(), (float) getHeight());
	}

	protected Rectangle rect = new Rectangle();

	protected void updateBounds() {

		if (useFixedWidth && view instanceof BoxView) {
			view.setSize((float) getWidth(), (float) getHeight());
			float startHeight = view.getPreferredSpan(View.Y_AXIS);

			Rectangle2D rect = new Rectangle2D.Double(0, 0, getWidth(),
					startHeight);

			int maxWidth = 0;
			for (int i = 0; i < view.getDocument().getLength(); i++) {
				try {
					Rectangle s = view.modelToView(i, rect).getBounds();
					int maxpos = (int) (s.getX() + s.getWidth());
					if (maxpos > maxWidth)
						maxWidth = maxpos;
				} catch (BadLocationException ex) {
				}
			}

			view.setSize(maxWidth, startHeight);

			setWidth(maxWidth);
			setHeight(startHeight);
		}
	}

	@Override
	protected void paint(PPaintContext paintContext) {
		rect.width = (int) getWidth();
		rect.height = (int) getHeight();
		Graphics2D g = paintContext.getGraphics();
		view.paint(g, rect);
		// setPaint(Color.red);
		// super.paint(paintContext);
	}
}
