package org.bbop.piccolo;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import javax.swing.Icon;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.activities.PActivity;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PPaintContext;

public class IconNode extends PNode implements Morphable {

	// generated
	private static final long serialVersionUID = -7277431814123210010L;

	protected Icon icon;

	public IconNode(Icon icon) {
		setIcon(icon);
	}

	public void setIcon(Icon icon) {
		this.icon = icon;
		setBounds(0, 0, icon.getIconWidth(), icon.getIconHeight());
	}
	
	public Icon getIcon() {
		return icon;
	}

	@Override
	public boolean setBounds(double arg0, double arg1, double arg2, double arg3) {
		return super.setBounds(arg0, arg1, arg2, arg3);
	}

	@Override
	protected void paint(PPaintContext paintContext) {
		Rectangle2D svgBounds = new Rectangle2D.Float(0, 0,
				icon.getIconWidth(), icon.getIconHeight());
		PBounds b = getBoundsReference();
		Graphics2D g2 = paintContext.getGraphics();

		if (b.x != 0 || b.y != 0 || b.width != svgBounds.getWidth()
				|| b.height != svgBounds.getHeight()) {
			g2.translate(b.x, b.y);
			g2.scale(b.width / svgBounds.getWidth(), b.height
					/ svgBounds.getHeight());
			icon.paintIcon(null, g2, 0, 0);
			// g2.drawImage(image, 0, 0, null);
			g2.scale(svgBounds.getWidth() / b.width, svgBounds.getHeight()
					/ b.height);
			g2.translate(-b.x, -b.y);
		} else {
			icon.paintIcon(null, g2, 0, 0);
		}
	}

	@Override
	public boolean doDefaultMorph() {
		return false;
	}

	@Override
	public PActivity morphTo(PNode node, long duration) {
		if (node instanceof IconNode) {
			setIcon(((IconNode) node).getIcon());
			return new PActivity(0);
		}
		return null;
	}

}
