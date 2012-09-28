package org.bbop.piccolo;

import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;

import javax.swing.Timer;

import org.apache.log4j.Logger;
import org.bbop.util.EndpointShapeExtender;
import org.bbop.util.ShapeExtender;
import org.bbop.util.ShapeUtil;

import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.activities.PActivity;
import edu.umd.cs.piccolo.activities.PActivity.PActivityDelegate;
import edu.umd.cs.piccolo.event.PBasicInputEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.nodes.PText;

public class StatusMessageDisplayer {

	//initialize logger
	protected final static Logger logger = Logger.getLogger(StatusMessageDisplayer.class);
	protected PCamera camera;
	protected Paint messageBoxPaint = Color.blue;
	
	protected PNode currentMessage;

	protected PCompoundActivity statusBoxActivity;
	
	protected ShapeExtender extender = new EndpointShapeExtender();

	protected Timer messageTimer;

	protected PPath statusBox = new PPath(ShapeUtil.createRoundRectangle(0, 0,
			1, 1));
	
	protected Font messageFont = new Font("Dialog", Font.BOLD, 16);
	protected Paint messagePaint = Color.blue;
	protected float messageBoxTransparency = .5f;
	protected ActionListener hideMessageActionListener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			hideStatusMessage();
		}
	};

	protected static final double STATUS_BOX_MARGIN = 10;


	public StatusMessageDisplayer(PCamera camera) {
		this.camera = camera;
		camera.addInputEventListener(new PBasicInputEventHandler() {
			@Override
			public void keyPressed(PInputEvent event) {
				if (event.getKeyCode() == KeyEvent.VK_ESCAPE) {
					hideStatusMessage();
				}
			}
		});
	}

	public synchronized void hideStatusMessage() {
		statusBoxActivity = new PCompoundActivity();
		if (currentMessage != null) {
			PActivity disappear = currentMessage.animateToTransparency(0, 500);
			disappear.setDelegate(new PActivityDelegate() {

				@Override
				public void activityFinished(PActivity activity) {
					camera.removeChild(currentMessage);
					camera.removeChild(statusBox);
					statusBox.setPathTo(ShapeUtil.createRoundRectangle(0, 0, 1,
							1));
					currentMessage = null;
				}

				@Override
				public void activityStarted(PActivity activity) {
				}

				@Override
				public void activityStepped(PActivity activity) {
				}

			});
			statusBoxActivity.addActivity(disappear);
		}
		statusBoxActivity.addActivity(statusBox.animateToTransparency(0, 500));
		camera.addActivity(statusBoxActivity);
	}

	public void showStatusMessage(String message, long duration) {
		PText text = new PText(message);
		text.setFont(getMessageFont());
		text.setTextPaint(Color.white);
		showStatusMessage(text, duration);
	}

	public synchronized void showStatusMessage(final PNode node, long duration) {
		if (!camera.getChildrenReference().contains(statusBox)) {
			statusBox.setTransparency(0);
			camera.addChild(statusBox);
			Point2D center = camera.getBounds().getCenter2D();
			statusBox.setOffset(center.getX() - statusBox.getWidth() / 2,
					center.getY() - statusBox.getHeight() / 2);
		}
		if (messageTimer != null)
			messageTimer.stop();
		statusBox.setPickable(false);
		statusBox.setStroke(null);
		statusBox.setPaint(getMessageBoxPaint());
		node.setTransparency(0);
		node.setPickable(false);
		camera.addChild(node);
		node.setOffset(camera.getBounds().getWidth() / 2
				- node.getFullBoundsReference().getWidth() / 2, camera
				.getBounds().getHeight()
				/ 2 - node.getFullBoundsReference().getHeight() / 2);
		if (statusBoxActivity != null)
			statusBoxActivity.terminate(PActivity.TERMINATE_WITHOUT_FINISHING);
		statusBoxActivity = new PCompoundActivity();
		final PNode currentMessage = this.currentMessage;
		if (camera.getChildrenReference().contains(currentMessage)) {
			PActivity disappear = currentMessage.animateToTransparency(0, 100);
			disappear.setDelegate(new PActivityDelegate() {

				@Override
				public void activityFinished(PActivity activity) {
					camera.removeChild(currentMessage);
				}

				@Override
				public void activityStarted(PActivity activity) {
				}

				@Override
				public void activityStepped(PActivity activity) {
				}

			});
			statusBoxActivity.addActivity(disappear);

		}
		this.currentMessage = node;
		statusBoxActivity.addActivity(statusBox.animateToTransparency(
				getMessageBoxTransparency(), 500));
		statusBoxActivity.addActivity(node.animateToTransparency(1, 500));

		double newWidth = Math.min(camera.getBounds().getWidth(), node
				.getFullBoundsReference().getWidth())
				+ STATUS_BOX_MARGIN;
		double newHeight = Math.min(camera.getBounds().getHeight(), node
				.getFullBoundsReference().getHeight())
				+ STATUS_BOX_MARGIN;
		double newX = (camera.getBounds().getWidth() - newWidth) / 2;
		double newY = (camera.getBounds().getHeight() - newHeight) / 2;
		statusBoxActivity.addActivity(new MorphActivity(statusBox,
				ShapeUtil.createRoundRectangle(0, 0, (float) newWidth,
						(float) newHeight), extender, 500));
		statusBoxActivity.addActivity(statusBox.animateToPositionScaleRotation(
				newX, newY, 1, 0, 500));
		statusBoxActivity.addActivity(node.animateToPositionScaleRotation(node
				.getXOffset(), node.getYOffset(), 1, 0, 500));
		node.setOffset(node.getFullBoundsReference().getCenterX(), node
				.getFullBoundsReference().getCenterY());
		node.setScale(.01);
		camera.addActivity(statusBoxActivity);
		if (messageTimer == null) {
			messageTimer = new Timer((int) duration, hideMessageActionListener );
			messageTimer.start();
		} else {
			messageTimer.setInitialDelay((int) duration);
			messageTimer.restart();
		}
	}

	public Paint getMessageBoxPaint() {
		return messageBoxPaint;
	}

	public void setMessageBoxPaint(Paint messageBoxPaint) {
		this.messageBoxPaint = messageBoxPaint;
	}

	public Font getMessageFont() {
		return messageFont;
	}

	public void setMessageFont(Font messageFont) {
		this.messageFont = messageFont;
	}

	public float getMessageBoxTransparency() {
		return messageBoxTransparency;
	}

	public void setMessageBoxTransparency(float messageBoxTransparency) {
		this.messageBoxTransparency = messageBoxTransparency;
	}

	public Paint getMessagePaint() {
		return messagePaint;
	}

	public void setMessagePaint(Paint messagePaint) {
		this.messagePaint = messagePaint;
	}

}
