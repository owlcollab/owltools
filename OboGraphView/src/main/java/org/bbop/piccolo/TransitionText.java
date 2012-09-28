package org.bbop.piccolo;

import javax.swing.text.ViewFactory;
import javax.swing.text.html.HTMLEditorKit;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.activities.PActivity;
import edu.umd.cs.piccolo.util.PPaintContext;

public class TransitionText extends ViewRenderedStyleText implements Morphable {

	// generated
	private static final long serialVersionUID = 8780013655699499121L;

	protected boolean disablePaint = false;
	protected ViewRenderedStyleText dummyOld;
	protected ViewRenderedStyleText dummyNew;

	public TransitionText() {
		super();
	}
	
	@Override
	public boolean doDefaultMorph() {
		return false;
	}
	
	public TransitionText(HTMLEditorKit editorKit, ViewFactory factory) {
		super(editorKit, factory);
		
		// setPaint(Color.black);
	}

	public PActivity animateTextChange(final String newText, long duration) {
		dummyOld = new ViewRenderedStyleText(getEditorKit(), viewFactory);
		dummyNew = new ViewRenderedStyleText(getEditorKit(), viewFactory);
		dummyOld.setTransparency(1);
		dummyNew.setTransparency(0);
		dummyOld.setPaint(getPaint());
		dummyNew.setPaint(getPaint());

		PCompoundActivity activity = new PCompoundActivity() {

			@Override
			protected void activityStarted() {
				super.activityStarted();
				disablePaint = true;
				setText(newText, false);

				dummyOld.setWidth(getWidth());
				dummyNew.setWidth(getWidth());
				dummyOld.setText(getText(), true);
				dummyNew.setText(newText, true);

				addChild(dummyOld);
				addChild(dummyNew);
			}

			@Override
			protected void activityFinished() {
				removeChild(dummyOld);
				removeChild(dummyNew);
				disablePaint = false;
				super.activityFinished();
			}
		};
		activity.addActivity(dummyOld.animateToTransparency(0, duration));
		activity.addActivity(dummyNew.animateToTransparency(1, duration));
		return activity;
		
	}

	@Override
	protected void paint(PPaintContext arg0) {
		if (!disablePaint)
			super.paint(arg0);
	}

	@Override
	public PActivity morphTo(PNode node, long duration) {
		if (node instanceof ViewRenderedStyleText) {
			return animateTextChange(((ViewRenderedStyleText) node).getLabel(),
					duration);
		}
		return null;
	}
}
