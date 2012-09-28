package org.bbop.gui;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.io.IOException;

import javax.swing.Icon;

import org.apache.batik.gvt.GraphicsNode;
import org.apache.log4j.Logger;
import org.bbop.util.SVGUtil;

public class SVGIcon implements Icon {

	//initialize logger
	protected final static Logger logger = Logger.getLogger(SVGIcon.class);

	protected GraphicsNode node;
	protected String uri;
	protected int width = -1;
	protected int height = -1;
	protected double widthScaleFactor = 1;
	protected double heightScaleFactor = 1;

	public SVGIcon(String uri) throws IOException {
		this(uri, -1, -1);
	}

	public SVGIcon(String uri, int dimension) throws IOException {
		node = SVGUtil.getSVG(uri);
		this.uri = uri;
		int width = -1;
		int height = -1;
		Rectangle2D r = node.getBounds();
		if (r.getWidth() < r.getHeight())
			height = dimension;
		else
			width = dimension;
		setDimension(width, height);
	}

	public SVGIcon(String uri, int width, int height) throws IOException {
		node = SVGUtil.getSVG(uri);
		this.uri = uri;
		setDimension(width, height);
	}

	protected void setDimension(int width, int height) {
		Rectangle2D r = node.getBounds();
		widthScaleFactor = 1;
		heightScaleFactor = 1;
		if (height > 0)
			heightScaleFactor = height / r.getHeight();
		if (width > 0)
			widthScaleFactor = width / r.getWidth();
		if (width == -1) {
			widthScaleFactor = heightScaleFactor;
		}
		if (height == -1)
			heightScaleFactor = widthScaleFactor;
		width = (int) (r.getWidth() * widthScaleFactor);
		height = (int) (r.getHeight() * heightScaleFactor);
		this.width = width;
		this.height = height;
	}

	@Override
	public int getIconHeight() {
		return height;
	}

	@Override
	public int getIconWidth() {
		return width;
	}

	@Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		if (g instanceof Graphics2D) {
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
			g2.translate(x, y);
			g2.scale(widthScaleFactor, heightScaleFactor);
			node.paint(g2);
			g2.scale(1 / widthScaleFactor, 1 / heightScaleFactor);
			g2.translate(-x, -y);
		}
	}
	
	@Override
	public String toString() {
		return uri;
	}
}
